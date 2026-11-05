# Add PvP Tournament

Adds a RuneLite sidebar for private tournament codes, opt-in registration,
single-elimination brackets for 2–64 competitors, automatic byes, and
organizer-confirmed match results through a tournament champion.

Players connect explicitly to a trusted self-hosted HTTPS service. The reference
Python service and SQLite persistence are included in the source repository;
localhost is for development. No public service is provisioned. Names are
self-declared, and the organizer adjudicates results. No combat automation,
kill detection or game-account authentication is included.

The plugin sends only entered tournament data to the selected service after
connection confirmation. Credentials are held in memory and may be manually
backed up. Privacy, hosting and participant trust are documented in the source.

Validation completed: Java 11-targeted plugin build on JDK 17; 2 Java tests;
5 service tests including all bracket sizes 2–64, conflicting result writes,
authorization, persistence and HTTP requests. Offscreen bracket render inspected.

Before requesting review, record these pending checks:

- Two RuneLite clients: join/register, seed, refresh progress and final champion.
- Five-player bracket with byes and a 64-player scrollable bracket.
- Session restore, permission failures, service restart and plugin disable.
- Two computers using the shared HTTPS service.

This is a prepared description, not evidence that a pull request was opened.

Generated-by: OpenAI Codex
