# Data handling

The plugin contacts only the service origin chosen in settings or explicitly
confirmed when restoring a session. Remote service connections require HTTPS;
HTTP is accepted for loopback development only. Redirects are disabled.

The service stores tournament titles, codes, timestamps, self-declared display
names, registration flags, match results and an internal result audit trail in
SQLite. It stores SHA-256 hashes of high-entropy permission tokens. API snapshots
exclude hashes and the audit trail. The reference server disables request logs.
Reverse proxies and hosting providers may separately log IP addresses.

No credentials or game account identifiers are read from RuneLite. Session
tokens exist only in plugin memory unless the user manually copies a backup.
Backups contain credentials; users are responsible for protecting those copies.
Disconnecting clears the active session but does not delete server data.

Retention: reference data persists until the operator deletes it. Before using a
public deployment, the operator must publish its identity, contact address,
retention period and deletion procedure. To remove a tournament, stop the server,
back up the database securely and delete the matching room through SQLite using
a parameterized query. Purge associated backups under the published policy.
Deleting a room invalidates all of its credentials. No analytics are included.
