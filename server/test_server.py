import concurrent.futures
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path
from server import Store, Server, Problem


class TournamentTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.store = Store(Path(self.tmp.name) / 'test.sqlite')
        host = self.store.create({'title': 'Test cup', 'capacity': 64})
        self.code = host['tournament']['code']
        self.host = host['token']

    def tearDown(self):
        self.tmp.cleanup()

    def register(self, count):
        members = []
        for i in range(count):
            p = self.store.join(self.code, {'name': f'Player {i}'})
            self.store.action(self.code, p['token'], 'register', {'registered': True})
            members.append(p)
        return members

    def test_all_sizes_and_byes(self):
        for size in range(2, 65):
            created = self.store.create({'title': f'Cup {size}', 'capacity': 64})
            self.code, self.host = created['tournament']['code'], created['token']
            self.register(size)
            room = self.store.action(self.code, self.host, 'start', {})
            decisions = 0
            while room['status'] == 'ACTIVE':
                ready = [m for rnd in room['rounds'] for m in rnd
                         if not m['complete'] and m['a'] and m['b']]
                self.assertTrue(ready)
                m = ready[0]
                room = self.store.action(self.code, self.host, 'result',
                                         {'match': m['id'], 'winner': m['a'], 'revision': room['revision']})
                decisions += 1
            self.assertEqual(size - 1, decisions)
            self.assertIsNotNone(room['champion'])

    def test_authorization_and_privacy(self):
        member = self.register(2)[0]
        with self.assertRaises(Problem):
            self.store.get(self.code, 'wrong')
        with self.assertRaises(Problem):
            self.store.action(self.code, member['token'], 'start', {})
        public = json.dumps(self.store.get(self.code, member['token']))
        self.assertNotIn('tokenHash', public)
        self.assertNotIn('hostHash', public)
        self.assertNotIn(self.host, public)
        room = self.store.action(self.code, self.host, 'start', {})
        with self.assertRaises(Problem):
            self.store.join(self.code, {'name': 'Late'})
        m = room['rounds'][0][0]
        with self.assertRaises(Problem):
            self.store.action(self.code, self.host, 'result', {'match': m['id'], 'winner': 'other', 'revision': room['revision']})
        with self.assertRaises(Problem):
            self.store.action(self.code, self.host, 'result', {'match': m['id'], 'winner': m['a'], 'revision': 0})

    def test_concurrent_conflicting_results(self):
        self.register(4)
        room = self.store.action(self.code, self.host, 'start', {})
        m = room['rounds'][0][0]
        def submit(winner):
            try:
                self.store.action(self.code, self.host, 'result', {'match': m['id'], 'winner': winner, 'revision': room['revision']})
                return True
            except Problem:
                return False
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            self.assertEqual(1, sum(pool.map(submit, [m['a'], m['b']])))

    def test_persistence_and_registration(self):
        p = self.store.join(self.code, {'name': 'Some_Name'})
        with self.assertRaises(Problem):
            self.store.join(self.code, {'name': 'some name'})
        self.store.action(self.code, p['token'], 'register', {'registered': True})
        self.store.action(self.code, p['token'], 'register', {'registered': False})
        self.assertFalse(Store(self.store.path).get(self.code, p['token'])['players'][0]['registered'])
        with self.assertRaises(Problem):
            self.store.action(self.code, self.host, 'start', {})
        self.store.action(self.code, self.host, 'cancel', {})
        self.assertEqual('CANCELLED', self.store.get(self.code, self.host)['status'])
        with self.assertRaises(Problem):
            self.store.action(self.code, self.host, 'start', {})

    def test_http(self):
        service = Server(('127.0.0.1', 0), self.store)
        thread = threading.Thread(target=service.serve_forever, daemon=True)
        thread.start()
        base = f'http://127.0.0.1:{service.server_port}'
        def call(path, body=None, token=''):
            headers = {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}
            req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
            return json.load(urllib.request.urlopen(req))
        try:
            self.assertEqual('ok', call('/health')['status'])
            created = call('/v1/tournaments', {'title': 'HTTP cup'})
            code = created['tournament']['code']
            player = call(f'/v1/tournaments/{code}/join', {'name': 'HTTP Player'})
            room = call(f'/v1/tournaments/{code}/register', {'registered': True}, player['token'])
            self.assertTrue(room['players'][0]['registered'])
            with self.assertRaises(urllib.error.HTTPError) as err:
                call(f'/v1/tournaments/{code}/start', {}, player['token'])
            self.assertEqual(403, err.exception.code)
            with self.assertRaises(urllib.error.HTTPError) as err:
                call('/v1/tournaments', [])
            self.assertEqual(400, err.exception.code)
        finally:
            service.shutdown(); service.server_close(); thread.join()


if __name__ == '__main__':
    unittest.main()
