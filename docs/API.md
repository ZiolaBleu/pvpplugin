# Service protocol v1

JSON over HTTP (loopback) / HTTPS (remote). Origin-only base URL. All mutable
endpoints use POST and `Content-Type: application/json`. Authenticated requests
use `Authorization: Bearer TOKEN`. Error responses are `{ "error": "message" }`
with a non-2xx status. No CORS support is needed by the Java client.

| Method | Path | Request | Permission |
| --- | --- | --- | --- |
| GET | `/health` | none | public |
| POST | `/v1/tournaments` | `title`, optional integer `capacity` (2–64) | public |
| POST | `/v1/tournaments/CODE/join` | `name` (1–12 ASCII game-name characters) | code |
| GET | `/v1/tournaments/CODE` | none | member or organizer |
| POST | `/v1/tournaments/CODE/register` | boolean `registered` | member |
| POST | `/v1/tournaments/CODE/start` | `{}` | organizer |
| POST | `/v1/tournaments/CODE/result` | `match`, `winner` player ID, integer `revision` | organizer |
| POST | `/v1/tournaments/CODE/cancel` | `{}` | organizer |

Create and join return `{ "token": "...", "tournament": { ... } }`. All other
tournament endpoints return the snapshot directly. Codes are case-sensitive
uppercase on the wire. The plugin normalizes pasted join codes.

Snapshots contain `code`, `title`, `capacity`, `status`, `revision`, `players`,
`rounds`, nullable `champion`, `organizer` and `memberId`. `memberId` is `host`
for organizer credentials. Players contain `id`, `name`, `registered`. Rounds
are arrays of matches with `id` (`zero-based-round-index`), nullable `a`, `b`,
`winner`, and boolean `complete`, `bye`. Match IDs use `round-match`, e.g. `0-1`.

Status transitions: `REGISTRATION` → `ACTIVE` → `COMPLETE`. An organizer may
cancel registration or active play, producing terminal `CANCELLED`. Byes advance
automatically. Result writes require the exact snapshot revision; a competing
write fails with 409 and must be refreshed. Completed matches cannot be changed.
SQLite immediate transactions serialize simultaneous writes, including joins
and registration closure. Repeat register requests are safe; create/join are
not idempotent and clients should not automatically retry a lost POST response.
