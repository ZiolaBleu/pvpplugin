# Shared service hosting

All players must use the same deployed service. No paid hosting is required for
local development. Public deployment needs an operator-controlled machine and
domain with a valid TLS certificate. This package does not create either.

Run Python 3.11+ under a service supervisor as a dedicated unprivileged user:

```sh
python3 server/server.py --bind 127.0.0.1 --port 8080 --db /var/lib/pvp-tournament/tournaments.sqlite3
```

The database directory must exist or be creatable by that user. Protect its
permissions and backups. Restart the service automatically on failure. Back up
SQLite using its backup API rather than copying a live file without its WAL.

Terminate HTTPS at an updated reverse proxy such as Caddy. Example Caddyfile
(replace the domain with your own):

```text
tournaments.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

Keep the Python port private. Configure clients to use the HTTPS origin without
a path. `/health` returns a JSON health response. Request bodies are capped at
8 KiB; tokens are sent through Authorization headers, never query parameters.
The reference server has bounded concurrent workers and a global token-bucket
rate limit. For public operation add per-IP request/connection limits at the
proxy and monitor disk, memory and backups. Do not trust forwarded IP headers
from arbitrary clients. The reference service is intended for small clan events,
not large anonymous hosting; authenticated room creation and abuse controls may
be needed for an open public service. Maximum 10,000 stored rooms; the operator
must prune rooms under a documented retention policy.

Creation and joining are open, subject to service and room limits. Anyone with
the code can occupy a slot or claim an unverified display name. Keep codes within
your event and verify participants yourself. There is no global lobby directory,
account recovery, token rotation, reconnect without a backup, result correction,
or automatic combat verification. Cancellation is available to the organizer.
