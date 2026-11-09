# Agent client bridge: step-by-step driving of a real dev client

- Time: `2026-11-09 02:34:24 -0600`
- Author: `Claude; Anthropic/claude-opus-5-5; dev tooling`
- Status: `completed`
- Scope: `src/gametest/java/com/teammoeg/frostedheart/devtools/AgentClientBridge.java`, `Scripts/dev_client_bridge.init.gradle`, `Scripts/agent_client.py`, `README.md`

## Completed

- Added `AgentClientBridge`, a client-only class in the `gameTest` source set, so it is excluded from production JARs. It is enabled only with `-Dfrostedheart.agentBridge=true`.
  - It takes one command at a time from `<gameDir>/agent/inbox/*.cmd`, runs it on the client thread, and writes a JSON result (plus a `state` snapshot) to `agent/outbox/`.
  - Commands: `state`, `connect`, `world`, `newworld`, `disconnect`, `command`, `messages`, `look`, `lookat`, `key`, `type`, `click`, `scroll`, `move`, `screen`, `screenshot`, `wait`, `quit`. `Scripts/agent_client.py` documents them.
- `screen` reports vanilla widgets, container slots, and the CUI element tree (`CUIScreen#getPrimaryLayer`), all in absolute GUI coordinates that `click` accepts directly.
- `Scripts/dev_client_bridge.init.gradle` adds the `gameTest` source set and the property to `runClient`, and fixes the window at 1280×720. `build.gradle` is unchanged.
- Added a short entry to `README.md`, next to the dedicated-server/RCON notes from [the previous entry](2026-11-09_02-06-34_dev-server-companion-mods-rcon.md).

## Decisions

- Input goes through vanilla's own GLFW callback targets: `KeyboardHandler#keyPress` (public), and `MouseHandler#onPress/onScroll/onMove` and `KeyboardHandler#charTyped` (private, called by reflection under their official names; dev only). Forge input events therefore fire as for real input. FH hotkeys are handled in `InputEvent.Key` (`FHKeyHandler`), so a bare `KeyMapping.click` would not open, for example, the nutrition screen.
- Mouse buttons in the world use `KeyMapping.click`. A real `onPress` would grab the OS cursor while the user may be working.
- `click`/`scroll` move the cursor, wait for two rendered frames, then press. CUI resolves a press against the hover state of the last rendered frame. Pressing in the same tick as the move clicked whatever was under the previous position (each house tab opened the previously hovered tab's panel).
- Commands start 40 ticks after the loading overlay closes. `connect` retries a failed join up to twice and lists each failure under `retries`. The very first join after one client start never received the server's login queries and timed out after 30 s; the root cause is unknown, and it did not recur in later starts.
- Transport is plain files (atomic rename on both sides): it needs no ports or extra dependencies, and stale state is easy to inspect.

## Validation

All runs were in an isolated worktree of HEAD `00cea8393` with these files copied in (see Remaining for why). Each check was done with a dedicated server (`runServer -I Scripts/dev_server_nogui.init.gradle`) plus a bridge client:

- `connect localhost:25565` joined; RCON `list` showed `Dev`, and RCON `op`/`gamemode`/`fill` affected the connected player.
- `key e` opened `CreativeModeInventoryScreen`, and `screen` listed its 54 slots. `key h` opened FH `HealthStatScreen` through `InputEvent.Key`; the screenshot showed the nutrition rings. `key escape` closed both.
- `command /town name` returned `Dev's Town` via chat capture.
- Player placement: `/item replace … hotbar.0 with frostedheart:house`, `key 1`, `lookat`, `key key.mouse.right` placed a house. RCON `data get block` showed it registered to the team town.
  - Placing it also opened an FTB `ScreenWrapper` (the quest book). Its widgets are FTB's own and not visible to `screen`.
- Right-clicking the house opened `CUIMenuScreenWrapper`/`HouseScreen`. After the hover fix, `click` on the three tabs produced `HouseOverviewElement`, `HouseMealElement` and `HouseResidentPanel` respectively. `move` showed the tab tooltip in a screenshot.
- `disconnect` → `newworld agent-bridge-test flat 42` → `command /time query daytime` → `screenshot` → `disconnect` → `quit` all succeeded. `quit` ended the Gradle task with BUILD SUCCESSFUL.
- Screenshots are 2560×1440 on this Retina Mac at guiScale 5. A pixel equals GUI coordinate × `guiScale`.

## Remaining

- The dev server and client share `run/` (and `run/logs/latest.log`) in the same checkout. The client needs `run/kubejs` and `run/config` to match the server's registries.
- Dev runs load classes straight from `build/classes`. During testing, another contributor's build in the main checkout failed to compile (`KnowledgeView.java`) and left `build/classes/java/main` half empty, which crashed both running instances with `ClassNotFoundException`. When other builds may run in the same checkout, run agent tests from a separate worktree.
- `screen` cannot see inside FTB Library GUIs, and only reports CUI element types, titles and rectangles, not label text. Screenshots cover the rest.
