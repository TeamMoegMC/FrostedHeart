# Generative Research V2 Phase 1 result foundation

- Time: `2026-08-25 21:05:23 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Frosted Research V2 result codecs/catalogue, team knowledge authority, access projections, prototype shell, commands, synchronization, integrations, tests, plan, and docs/research`

## Completed

- Added the five named `ResearchResult` branches: Finding, Design, Construction, Procedure, and Prototype. Construction contains only multiblock formation IDs; Procedure contains only right-click usable block IDs.
- Added the minimal format-3 topic and format-1 prototype-profile datapack loaders, aggregated validation, immutable last-known-good snapshots, monotonic catalogue revisions, and managed recipe/multiblock/block universes. No formal topic or companion-pack content was added.
- Added the independent `frostedresearch:knowledge` Chorda component with four idempotent acquired-ID sets and orphan preservation, plus service/facade APIs and administrator grant commands.
- Added the non-stackable physical `frostedresearch:upgrade_prototype` shell with frozen profile revision, serial, and owner-team identity. Prototype fabrication does not create a team entitlement and uses the existing inventory-or-nearby-drop delivery path.
- Added `KnowledgeProjection`, `TechnologyAccessProjection`, `AccessDecision`, exact new/legacy provenance, and default-open Boolean adapters. Legacy `EffectCrafting`, `EffectBuilding`, and `EffectUse` remain unchanged and project only into Design, Construction, and Procedure channels respectively.
- Routed recipe execution, IE formation, right-click block use, generator formation, and JEI through the same resolver. Added full knowledge snapshot synchronization for login, team changes, grants, relevant legacy changes, and catalogue reloads.
- Updated the living research documentation and the V2 plan. The plan is now `in-progress`, records Phase 1 completion, uses five-result terminology throughout, and corrects the obsolete `ITownBuilding.CODEC` ordering claim.

## Decisions

- Construction is a direct, narrow multiblock formation entitlement. Procedure is a direct, narrow right-click block entitlement; neither is a protocol, machine stat, automation capability, or general operating-knowledge wrapper.
- Raw `ResourceLocation` identities remain in definitions and projections; runtime objects are resolved only during catalogue validation or at an explicit execution boundary.
- Prototype identity is physical from Phase 1 onward. There is no temporary `acquiredPrototypeIds` state and no host/socket/contribution behavior before Phase 5.
- The V2 result catalogue coexists with the legacy config catalogue. A rejected V2 reload retains its previous snapshot, and a rejected legacy reload retains its original early-return behavior while still synchronizing any successfully installed V2 snapshot.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed `73` focused tests.
- `./gradlew test` passed.
- `./gradlew runGameTestServer` passed all `18` required GameTests, including real RecipeManager, IE multiblock, RightClickBlock, legacy provenance/reset, and full-inventory prototype delivery paths.
- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` passed the same `18` required GameTests.
- `./gradlew validateResearchCatalog -PresearchCatalogDir="<companion>/config/fhresearches"` validated all `81` unchanged companion definitions.
- `git diff --check` passed after implementation.

## Remaining

- Phase 2 and later still own evidence/idea workflow, formal V2 topics, stable companion recipe IDs, research institute/experiment content, and prototype host/socket/install/contribution behavior.
- Finding view-handler semantics and the full topic/profile workflow schema remain intentionally deferred to their consuming phases.
