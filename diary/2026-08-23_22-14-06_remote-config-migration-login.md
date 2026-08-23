# Safe remote server configuration migration

- Time: `2026-08-23 22:14:06 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `FHConfig server-config migration and Cluster Server remote login`

## Completed

- Restricted nutrition-scale and resident-attribute migrations to file-backed `CommentedFileConfig` instances.
- Skipped Forge's in-memory `SimpleCommentedConfig` used for vanilla fallback login and synchronized remote server configuration.
- Consolidated both version migrations behind one persistence guard and one final save.
- Added regression coverage distinguishing remote in-memory configuration from persistent world server configuration.
- Updated the nutrition living documentation with the server-owned migration boundary.

## Decisions

- Persistence capability, not physical distribution or thread identity, determines whether migration may run. This preserves old-world migration on dedicated and integrated servers while allowing the Cluster Server login endpoint to use Forge's vanilla fallback path safely.
- In-memory remote configuration is never migrated client-side; the client consumes the authoritative configuration supplied by the destination server.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.infrastructure.config.FHConfigMigrationTest --tests com.teammoeg.frostedheart.content.health.nutrition.NutritionScaleMigrationTest` passed.
- `./gradlew test` passed.
- Scoped `git diff --check` passed before this diary entry was added.

## Remaining

- Rebuild and deploy the same Frosted Heart JAR to the client and backend server, then verify login through Cluster Server port `15020` reaches the test server without a `ClientboundGameProfilePacket` processing error.
