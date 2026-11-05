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

Not performed: interactive RuneLite acceptance, live fights, a multi-computer
HTTPS deployment or Plugin Hub submission. No review or listing is claimed.
After the initial package was produced, the maintainer provided the public
repository `https://github.com/ZiolaBleu/pvpplugin` for source publication.

Follow `TESTING.md` before public submission. The compiled JAR is included under
`dist/` for inspection; it requires RuneLite and is not a standalone installer.
