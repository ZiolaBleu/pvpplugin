# PvP Tournament for RuneLite

[Source on GitHub](https://github.com/ZiolaBleu/pvpplugin)

Create an eight-character tournament code, join a private lobby, register to
compete, and follow a single-elimination bracket to its **#1 PvP champion**.
Supports 2–64 registered competitors, random seeding, automatic byes, five-second
progress polling, and an organizer-controlled result workflow. The champion is
the winner of that tournament, not a global skill ranking.

## Status

This is source for a development plugin plus a self-hosted reference service.
It is not installed by copying the ZIP or JAR into RuneLite. Plugin Hub listing
requires RuneLite maintainers to review and merge a submission. No public
tournament service is provisioned by this package. In-game testing is still
required before a public listing; see `docs/TESTING.md`.

## Start locally

Requirements: JDK 11 or 17, Python 3.11+, and internet for Gradle dependencies.
In one terminal, from the project directory:

```powershell
python server/server.py
```

In a second terminal:

```powershell
.\gradlew.bat test build
.\gradlew.bat run
```

On macOS/Linux use `chmod +x gradlew` and `./gradlew` instead. Use the official
[Jagex Accounts development instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
to sign in if needed. Enable **PvP Tournament**, then open the trophy sidebar.
The default service is `http://127.0.0.1:8080`, which works only on the server's
own computer. For remote players, deploy the service behind HTTPS and configure
every client with that same origin (see `docs/HOSTING.md`).

## Organize and play

1. Create a tournament. Approve connection to your trusted service, then copy
   the displayed code to your friends.
2. Immediately click **Private session backup**, copy the JSON and save it
   privately. The organizer token controls results. Sessions are kept in memory
   only: restarting/disabling RuneLite discards them unless you saved a backup.
3. Competitors choose **Join with code**, enter a display name, then click
   **Register to compete**. Joining the lobby alone does not register a player.
   Registration can be withdrawn before the organizer starts.
4. The organizer closes registration to seed the bracket. At least two
   registered players are required. Names and registration then lock.
5. Play each fight under rules agreed by the organizer. The organizer selects
   and confirms the winner of each ready match. Results are final. Wrong
   results require cancelling and creating a new tournament.
6. Open the scrollable **visual bracket** to see connected rounds and highlighted
   winners. Each bracket window is a snapshot; reopen it after progress changes.
   The sidebar updates every five seconds. The last winner becomes champion.

An organizer can also compete: save the organizer session, disconnect, join the
same code as a player, register, save that separate session, then resume the
organizer session to manage the bracket. To resume, paste saved JSON into
**Resume private session**. Never send session JSON to other players or commit it.

## Trust and privacy

Display names are self-declared; this service does not verify game-account
ownership. The organizer adjudicates match results. It does not detect kills,
move the player, control combat, inspect passwords, or communicate with Jagex.
Only entered names, registration, room information and confirmed results are
sent to the configured service. Like any network service, its operator receives
your IP address. No network traffic occurs until you create, join or resume.
Do not use a service whose operator you do not trust.

Room codes grant the ability to join while registration is open. Separate
unpredictable bearer tokens grant member/organizer permissions. Server storage
contains token hashes; private backups contain the actual token. Lost tokens
cannot be recovered. Do not publish backups, production databases or logs.
See `PRIVACY.md` for storage and retention details.

## Tests and source layout

```powershell
python -m unittest discover -s server -v
.\gradlew.bat test build
```

- `src/main/java/com/pvptournament`: RuneLite integration, HTTP client, sidebar
  and Java2D bracket renderer. Java 11 bytecode; RuneLite-provided dependencies.
- `src/test/java`: development client launcher and offline plugin tests.
- `server`: Python standard-library HTTP service, SQLite persistence, tests.
- `docs`: hosting, manual acceptance and GitHub/Plugin Hub publication steps.
- `.github/workflows`: Java build and service tests on pushes/pull requests.

BSD 2-Clause licensed. This is an independent community project.
