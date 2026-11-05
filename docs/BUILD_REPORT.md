# Validation report — November 4, 2026 (America/Chicago)

- Final Gradle 8.10 `--offline --no-daemon test build`: **BUILD SUCCESSFUL**.
- Microsoft OpenJDK 17.0.21; Java compilation targets release 11.
- `PluginUnitTest`: **2 tests, 0 failures, 0 errors**. Covers endpoint restrictions
  and offscreen bracket rendering. `docs/bracket-preview.png` in the ZIP is the
  actual Java2D render from the test, visually inspected during creation.
- `python -m unittest discover -s server -v`: **5 tests passed** on Python 3.13,
  including a complete simulation for every bracket size 2–64, permissions,
  private-data exclusion, simultaneous conflicting results, stale revisions,
  SQLite persistence, registration withdrawal, cancellation and HTTP requests.
- Tests used temporary databases and ephemeral loopback ports. No production
  tournament data or user credentials are included in this archive.

The first sandboxed HTTP test was denied loopback networking; the complete suite
then passed with local networking allowed. A database connection cleanup issue
found in the initial run was fixed before the passing suite.

The plugin was compiled against dependencies resolved from RuneLite's Maven
repository. `latest.release` follows RuneLite's official example-plugin guidance;
future builds may resolve newer RuneLite versions and require revalidation.

Not performed: interactive in-game acceptance, live fights or a multi-computer
tournament. No maintainer approval or Plugin Hub listing is claimed.
After the initial package was produced, the maintainer provided the public
repository `https://github.com/ZiolaBleu/pvpplugin` for source publication.
All 32 initial package files were uploaded and verified on `main` at commit
`2c0306adb0bc594c6ca2c80665bcf74b3282ee05`. A subsequent documentation update
adds the prepared Plugin Hub manifest and review description.

## Submission and runtime verification

- Plugin Hub submission: https://github.com/runelite/plugin-hub/pull/20151
- The upstream Plugin Hub `build` check passed for manifest commit
  `3369e4c3f7a3c1cc62c45f4c2180f0119b28a162`.
- The maintainer completed the final PR creation after the connector returned
  HTTP 403 for the upstream repository. The connector can read its build status.
- Local reference service started; local and public HTTPS health checks passed.
- A temporary Cloudflare development tunnel completed an HTTP smoke check:
  three synthetic players joined, registered, advanced through a bracket with
  a bye and reached a champion. The synthetic room was removed afterward.
- A development RuneLite client launched using JDK 11. Its logs confirmed
  `TournamentPlugin is now running` and completed initialization. This confirms
  startup only; actual player interaction remains for the user to verify.

The temporary endpoint is local runtime state, not a permanent hosted service.
It is not committed to the public repository. See `START_HERE.md`.

Follow `TESTING.md` before public submission. The compiled JAR is included under
`dist/` for inspection; it requires RuneLite and is not a standalone installer.
