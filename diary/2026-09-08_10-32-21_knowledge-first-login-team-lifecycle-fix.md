# Knowledge first-login team lifecycle fix

- Time: `2026-09-08 10:32:21 +0800`
- Author: `Codex; OpenAI GPT-6; primary implementation agent, with independent lifecycle investigation`
- Status: `completed`
- Scope: `KnowledgeRuntime`, focused first-login GameTest, GameTest namespace selection, docs/knowledge lifecycle
  documentation

## Completed

- Fixed the new-world “Invalid player data” disconnect reported by the user. The supplied stack trace identifies
  `KnowledgeRuntime.datapackSync` calling `KnowledgeDataAPI.getData` before the new player's FTB team exists.
- Skip team lookup in the player-targeted `OnDatapackSyncEvent`; perform initial knowledge initialization and explicit
  synchronization at `PlayerLoggedInEvent` priority `LOWEST` instead.
- Preserve the reload event's online-team synchronization and one-time knowledge initialization.
- Updated [knowledge lifecycle documentation](../docs/knowledge/state-and-api.md) and
  added [KnowledgeLoginGameTests](../src/gametest/java/com/teammoeg/frostedresearch/knowledge/KnowledgeLoginGameTests.java).
- Added optional `gameTestNamespaces` Gradle property so the focused login test can run independently; default GameTests
  also include its namespace.

## Decisions

- Correct the lifecycle boundary rather than treating an absent pre-login team as a new data format or creating a
  temporary replacement team.
- Local Forge source confirms `PlayerList.placeNewPlayer` fires targeted datapack sync before logged-in events.
  Architectury forwards player join at `HIGH`, and FTB Teams creates a previously unknown player's personal team there.
  Knowledge initialization must execute later.
- Use the real `PlayerList.placeNewPlayer` path with a random new player profile and real FTB Teams. An in-memory Netty
  `EmbeddedChannel` supplies the channel required by Forge login hooks; Minecraft's bare mock-player helper omits it.

## Validation

- `./gradlew -PgameTestNamespaces=frostedresearch_knowledge_login runGameTestServer` passed the required first-login
  GameTest and compiled main/GameTest sources successfully.
- The test observes no FTB team at the early datapack event, completes actual player login, verifies team creation and
  initialized knowledge, and checks reload sync does not mutate initialized state.
- `git diff --check` passed.
- Test execution used the repository's dedicated `run-gametest` world; no player save or companion-pack data was edited.

## Remaining

- User can restart the updated development client and repeat new-world entry in their instance. The headless Forge/FTB
  login path is verified.
