# Acceptance checks

Automated service tests play every bracket size from 2 through 64 and check
exactly N−1 contested results before a champion, persistence, permissions,
duplicate names, withdrawal, cancellation, stale revisions, simultaneous results
and real HTTP requests. Java tests validate endpoint restrictions and render the
bracket offscreen. These checks do not establish in-game behavior.

Before public distribution, manually run two development RuneLite clients:

1. Create a room in client A. Save organizer credentials. Join in B, register,
   and verify A receives changes within about five seconds.
2. Add another competitor and start. Confirm bracket connections, ready matches,
   winner buttons and final champion. Repeat with 3 and 5 players to check byes.
3. Verify a competitor cannot start/cancel/report results, and cannot join or
   change registration after the start. Verify joined but unregistered users
   are not seeded. Test duplicate/empty/long display names and incorrect codes.
4. Restore both session types after disabling/re-enabling the plugin. Verify
   invalid backups are rejected. Back up organizer credentials before switching
   to a competitor session.
5. Stop the service; the client should show an error and remain responsive.
   Restart it and verify persistence and subsequent refresh. Change the configured
   URL during an active session: requests stay bound to that session's original
   service until you disconnect and explicitly connect again.
6. Verify the bracket scrolls horizontally/vertically at 64 players. Reopen a
   bracket snapshot after reporting results. Disable the plugin and verify its
   sidebar and polling stop. Close any separate bracket windows.
7. Repeat from a second computer against the shared HTTPS deployment.

No automated interaction with the game is included or needed. Fight and verify
game behavior manually. A clean build alone does not establish listing readiness.
