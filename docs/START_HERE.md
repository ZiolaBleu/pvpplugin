# Use the plugin while review is pending

The Plugin Hub submission is [PR #20151](https://github.com/runelite/plugin-hub/pull/20151).
Its upstream build passed. It will not appear in the Hub until maintainers review
and approve it. You can use the development client before then.

## First session

1. Start the Python service and development client as shown in the README.
   Prefer JDK 11 for the game client. The build also supports JDK 17; its stricter
   reflective-access rules can produce a RuneLite startup warning. The creation
   session launched the game client with JDK 11 and confirmed plugin startup.
2. Sign in yourself. For a Jagex Account, open the Jagex authentication sidebar,
   select **Add Jagex Account**, complete login, select a character, then choose
   **Play Now**. See the official
   [Jagex Account instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
3. Find **PvP Tournament** in plugin settings. For testing on one computer,
   leave its service URL as `http://127.0.0.1:8080`.
4. For players on different computers, everyone must use the same reachable
   HTTPS service origin. Set that value in **Tournament service URL** before
   creating/joining a session. Disconnect old sessions before changing origins.
5. Open the trophy sidebar, create a tournament, save a private session backup,
   and share the eight-character room code with your competitors. Competitors
   must click **Register to compete** after joining.
6. Close registration, play fights manually, and confirm winners in the organizer
   panel. Open the visual bracket to inspect progression. Reopen the bracket
   snapshot after new results. The last match determines this tournament's champion.

Until Hub acceptance, each participant needs to run the development plugin.
Installing ordinary RuneLite alone does not load this unpublished plugin.

## Temporary shared service used during setup

A temporary Cloudflare Quick Tunnel was started on the maintainer's PC so other
computers can reach the local service over HTTPS. Its current origin is saved
locally in `data/public-service-url.txt`; this runtime state is excluded from Git
and the ZIP, along with logs and the tournament database.

The PC, Python service and tunnel must remain running. The hostname changes on
restart. Quick Tunnels are for development and have no uptime guarantee; use a
stable HTTPS deployment from `HOSTING.md` for a regular community service.
See [Cloudflare's Quick Tunnel documentation](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

Do not share your session backup JSON. Share the service origin and room code.
The organizer token is separate from the code and authorizes final results.
