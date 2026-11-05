"""PvP Tournament reference service. Python 3.11+, standard library only."""
import argparse
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import threading
import time
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class Problem(Exception):
    def __init__(self, status, message):
        self.status, self.message = status, message


def require(condition, message, status=409):
    if not condition:
        raise Problem(status, message)


def digest(token):
    return hashlib.sha256(token.encode()).hexdigest()


def text(value, label, maximum):
    require(isinstance(value, str), label + ' must be text', 400)
    value = value.strip()
    require(0 < len(value) <= maximum and not any(ord(c) < 32 for c in value),
            label + ' is invalid', 400)
    return value


class Store:
    def __init__(self, path):
        self.path = str(path)
        with self.connect() as db:
            db.execute('PRAGMA journal_mode=WAL')
            db.execute('CREATE TABLE IF NOT EXISTS rooms (code TEXT PRIMARY KEY, data TEXT NOT NULL)')

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.path, timeout=15)
        try:
            with db:
                yield db
        finally:
            db.close()

    @contextmanager
    def transaction(self):
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            yield db

    def load(self, db, code):
        row = db.execute('SELECT data FROM rooms WHERE code=?', (code,)).fetchone()
        require(row is not None, 'Tournament not found', 404)
        return json.loads(row[0])

    def save(self, db, room):
        room['revision'] += 1
        db.execute('UPDATE rooms SET data=? WHERE code=?', (json.dumps(room), room['code']))

    def authorize(self, room, token, host=False):
        hashed = digest(token)
        if hmac.compare_digest(hashed, room['hostHash']):
            return 'host'
        if not host:
            for member in room['members']:
                if hmac.compare_digest(hashed, member['tokenHash']):
                    return member['id']
        raise Problem(403, 'Invalid credentials or organizer permission required')

    def public(self, room, identity):
        players = [{k: p[k] for k in ('id', 'name', 'registered')} for p in room['members']]
        return {**{k: room[k] for k in ('code', 'title', 'status', 'revision', 'capacity', 'rounds', 'champion')},
                'players': players, 'organizer': identity == 'host', 'memberId': identity}

    def create(self, body):
        title = text(body.get('title'), 'Title', 60)
        capacity = body.get('capacity', 32)
        require(type(capacity) is int and 2 <= capacity <= 64, 'Capacity must be 2 to 64', 400)
        token = secrets.token_urlsafe(32)
        with self.transaction() as db:
            require(db.execute('SELECT count(*) FROM rooms').fetchone()[0] < 10000,
                    'Service capacity reached; contact operator', 503)
            while True:
                code = ''.join(secrets.choice('ABCDEFGHJKLMNPQRSTUVWXYZ23456789') for _ in range(8))
                if not db.execute('SELECT 1 FROM rooms WHERE code=?', (code,)).fetchone():
                    break
            room = dict(code=code, title=title, capacity=capacity, status='REGISTRATION',
                        revision=1, members=[], rounds=[], champion=None, hostHash=digest(token),
                        created=int(time.time()), audit=[])
            db.execute('INSERT INTO rooms VALUES (?,?)', (code, json.dumps(room)))
        return dict(token=token, tournament=self.public(room, 'host'))

    def join(self, code, body):
        name = text(body.get('name'), 'RuneScape name', 12)
        require(re.fullmatch(r'[A-Za-z0-9 _-]+', name), 'Invalid RuneScape name', 400)
        token = secrets.token_urlsafe(32)
        with self.transaction() as db:
            room = self.load(db, code)
            require(room['status'] == 'REGISTRATION', 'Registration is closed')
            require(len(room['members']) < room['capacity'], 'Tournament is full')
            normalized = name.replace('_', ' ').replace('-', ' ').casefold()
            require(not any(p['normalized'] == normalized for p in room['members']), 'Name already joined')
            member = dict(id=secrets.token_hex(8), name=name, normalized=normalized,
                          tokenHash=digest(token), registered=False)
            room['members'].append(member)
            self.save(db, room)
        return dict(token=token, tournament=self.public(room, member['id']))

    def get(self, code, token):
        with self.connect() as db:
            room = self.load(db, code)
            return self.public(room, self.authorize(room, token))

    def action(self, code, token, action, body):
        with self.transaction() as db:
            room = self.load(db, code)
            identity = self.authorize(room, token, action in ('start', 'result', 'cancel'))
            if action == 'register':
                require(room['status'] == 'REGISTRATION', 'Registration is closed')
                require(identity != 'host', 'Join separately to compete', 400)
                require(type(body.get('registered')) is bool, 'registered must be boolean', 400)
                next(p for p in room['members'] if p['id'] == identity)['registered'] = body['registered']
            elif action == 'start':
                require(room['status'] == 'REGISTRATION', 'Tournament already started or cancelled')
                players = [p['id'] for p in room['members'] if p['registered']]
                require(len(players) >= 2, 'At least two registered players required')
                secrets.SystemRandom().shuffle(players)
                size = 1 << (len(players) - 1).bit_length()
                # Distribute byes so the first round never contains an empty match.
                byes = size - len(players)
                slots = []
                for _ in range(byes):
                    slots.extend([players.pop(), None])
                slots.extend(players)
                room['rounds'] = []
                r = 0
                while size > 1:
                    room['rounds'].append([dict(id=f'{r}-{i}', a=slots[i * 2] if r == 0 else None,
                                               b=slots[i * 2 + 1] if r == 0 else None,
                                               winner=None, complete=False, bye=False)
                                           for i in range(size // 2)])
                    r += 1
                    size //= 2
                room['status'] = 'ACTIVE'
                self.advance(room)
            elif action == 'result':
                require(room['status'] == 'ACTIVE', 'Tournament is not active')
                require(type(body.get('revision')) is int and body['revision'] == room['revision'],
                        'Bracket changed; refresh before confirming')
                match = next((m for rnd in room['rounds'] for m in rnd if m['id'] == body.get('match')), None)
                require(match is not None, 'Match not found', 404)
                require(not match['complete'] and match['a'] and match['b'], 'Match is not ready')
                require(body.get('winner') in (match['a'], match['b']), 'Winner must be in this match', 400)
                match['winner'], match['complete'] = body['winner'], True
                room['audit'].append(dict(match=match['id'], winner=match['winner'], at=int(time.time())))
                self.advance(room)
            elif action == 'cancel':
                require(room['status'] in ('REGISTRATION', 'ACTIVE'), 'Tournament already finished')
                room['status'] = 'CANCELLED'
            else:
                raise Problem(404, 'Unknown action')
            self.save(db, room)
            return self.public(room, identity)

    @staticmethod
    def advance(room):
        for r, matches in enumerate(room['rounds']):
            for i, match in enumerate(matches):
                if r:
                    previous = room['rounds'][r - 1][i * 2:i * 2 + 2]
                    if not all(m['complete'] for m in previous):
                        continue
                    match['a'], match['b'] = (m['winner'] for m in previous)
                if not match['complete'] and (not match['a'] or not match['b']):
                    match.update(winner=match['a'] or match['b'], complete=True, bye=True)
        final = room['rounds'][-1][0]
        if final['complete']:
            room['status'], room['champion'] = 'COMPLETE', final['winner']


class Api(BaseHTTPRequestHandler):
    server_version = 'Tournament/1.0'

    def log_message(self, *_):
        pass  # No access logs containing room codes or credentials.

    def do_GET(self):
        self.handle_request('GET')

    def do_POST(self):
        self.handle_request('POST')

    def handle_request(self, method):
        try:
            self.connection.settimeout(10)
            # Bounded global rate limiter: does not trust spoofable proxy headers.
            with self.server.rate_lock:
                now = time.monotonic()
                self.server.credits = min(200, self.server.credits + (now - self.server.last) * 20)
                self.server.last = now
                require(self.server.credits >= 1, 'Too many requests; try again later', 429)
                self.server.credits -= 1
            if method == 'GET' and self.path == '/health':
                return self.respond(200, {'status': 'ok'})
            body = {}
            if method == 'POST':
                require(self.headers.get('Content-Type', '').split(';')[0] == 'application/json',
                        'Use application/json', 415)
                length = int(self.headers.get('Content-Length', '0'))
                require(0 < length <= 8192, 'Invalid request length', 413)
                body = json.loads(self.rfile.read(length))
                require(isinstance(body, dict), 'Expected JSON object', 400)
            if method == 'POST' and self.path == '/v1/tournaments':
                return self.respond(201, self.server.store.create(body))
            route = re.fullmatch(r'/v1/tournaments/([A-Z2-9]{8})(?:/(join|register|start|result|cancel))?', self.path)
            require(route is not None, 'Not found', 404)
            code, action = route.groups()
            auth = self.headers.get('Authorization', '')
            token = auth[7:] if auth.startswith('Bearer ') else ''
            if method == 'GET' and action is None:
                result = self.server.store.get(code, token)
            elif method == 'POST' and action == 'join':
                result = self.server.store.join(code, body)
            elif method == 'POST' and action:
                result = self.server.store.action(code, token, action, body)
            else:
                raise Problem(405, 'Method not allowed')
            self.respond(200, result)
        except Problem as error:
            self.respond(error.status, {'error': error.message})
        except (ValueError, UnicodeError):
            self.respond(400, {'error': 'Invalid JSON or request'})
        except (TimeoutError, ConnectionError):
            self.close_connection = True
        except Exception:
            self.respond(500, {'error': 'Service error; contact operator'})

    def respond(self, status, data):
        encoded = json.dumps(data).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(encoded)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.end_headers()
        self.wfile.write(encoded)


class Server(ThreadingHTTPServer):
    daemon_threads = True
    request_queue_size = 32

    def __init__(self, address, store):
        super().__init__(address, Api)
        self.store = store
        self.rate_lock = threading.Lock()
        self.credits, self.last = 200, time.monotonic()
        self.slots = threading.BoundedSemaphore(64)

    def process_request(self, request, address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, address)
        except Exception:
            self.slots.release()
            raise

    def process_request_thread(self, request, address):
        try:
            super().process_request_thread(request, address)
        finally:
            self.slots.release()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--bind', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8080)
    parser.add_argument('--db', default='data/tournaments.sqlite3')
    args = parser.parse_args()
    Path(args.db).resolve().parent.mkdir(parents=True, exist_ok=True)
    server = Server((args.bind, args.port), Store(args.db))
    print(f'Tournament service on {args.bind}:{args.port}', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
