# Research Known-Risks Remediation

- Time: `2026-08-23 00:45:26 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `research catalog, persistence, networking, ownership, UI, automated validation, and companion research definitions`

## Completed

- Replaced incremental research-definition mutation with validated, deterministic candidate catalogs for startup, reload, and client definition synchronization. Invalid startup data is rejected with aggregated diagnostics; invalid reload candidates leave the prior catalog, listeners, team state, and client view intact.
- Added stable formal IDs and migration-only `legacyIds` for research, clues, and effects. Team saves now use a versioned string active ID, preserve deleted-definition orphans, and migrate legacy integer active selections through `fhregistries.dat` without discarding unrelated progress.
- Migrated committed points to `long`, made submission and progress calculations overflow-safe, fixed zero-based registry lookup, preserved custom clue NBT over the network, and moved full/incremental packets to bounded stable string keys and nonces.
- Enforced the shared 128-character stable-ID bound during catalogue validation, made registry/attribute NBT packet decoding defensive, and changed stale active-selection packets to be discarded rather than clearing a valid client selection.
- Added machine ownership and authorization contracts. Mechanical calculators persist the placing team, allow only the first real player to claim a legacy ownerless machine, reject FakePlayer and cross-team extraction, and share one authorization path for direct and rubbing interactions.
- Removed the global Create research context in favor of nested, exception-safe, thread-local execution ownership. Routed the existing player, IE, Create, campfire, and generator integration paths through explicit player/team or persisted-machine ownership checks, with ownerless locked automation failing closed.
- Removed the obsolete Frosted Research FTB Teams event path in favor of Chorda's bridge, added optional JEI/FTB class-loading boundaries, and replaced FTB sidebar state mutation with archive-scoped rendering/input suppression.
- Added editor-aware hidden-node graph snapshots, complete hidden edges in editor mode, transactional archive refresh after full state replacement, and real-bounds `fitToVisible` with 24-pixel padding and zoom clamping.
- Changed the companion definitions `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra` to explicit root research entries.
- Updated the living research documentation and `docs/research/known-risks.md`. Added a separate ready plan for the remaining third-party automation ownership audit; BROWSE and EXPERIMENT remain documented future features.

## Decisions

- Immersive Engineering and Create remain required dependencies; missing-dependency startup combinations are intentionally outside validation.
- The companion pack remains the sole production research-definition source. The Gradle catalog task performs registry-independent structural preflight, while Forge startup and reload perform full production codec and registry validation.
- Formal IDs always win when both a formal key and one of its legacy aliases exist; the alias record is retained as an orphan and reported instead of being merged or re-awarded.
- Old ownerless machines may be claimed only by direct interaction from a real server player. Unknown third-party execution paths are not claimed to be covered by the new contract and are tracked in the follow-up audit plan.

## Validation

- `./gradlew test` passed after the final implementation changes.
- `./gradlew runGameTestServer` passed all required Forge GameTests, covering calculator claim/authorization/partial extraction, transactional invalid hot-reload fallback, two-team kill/advancement/`always` listener lifecycle, reload/removal cleanup, orphan preservation, non-current completion, and reversible effect reset.
- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` passed the same dedicated-server GameTests without FTB Teams or JEI.
- `./gradlew test -PwithoutFtb -PwithoutJei` passed the unit suite with optional integrations absent.
- `./gradlew validateResearchCatalog -PresearchCatalogDir=<companion>/config/fhresearches` validated all 81 companion definitions.
- Main-repository scoped and companion-repository `git diff --check` results are recorded in the task handoff; the main repository contains an unrelated concurrent edit in `discussion/research-idea.md` that is intentionally untouched.

## Remaining

- Execute the destructive migration, two-client/two-team, UI scale, FTB sidebar, and optional-dependency client scenarios from `docs/research/known-risks.md` in disposable interactive worlds before release sign-off.
- Complete `plans/2026-08-23_00-27-05_research-third-party-automation-ownership-audit.md` for third-party recipe execution paths beyond the integrations changed here.
- BROWSE and EXPERIMENT have no entry point or data model in this change and remain future features.
