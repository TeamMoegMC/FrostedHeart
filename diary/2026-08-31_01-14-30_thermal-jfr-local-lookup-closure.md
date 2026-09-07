# Thermal JFR local lookup closure

- Time: `2026-08-31 01:14:30 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `dormant fallback lookup, same-Page topology planning, and phase ownership allocation`

## Completed

- Analyzed `run/thermal-dormant-storage-20260831-005633-120s.jfr`. Dormant
  computation/packing was absent from CPU/allocation hot sites, but all 22
  dormant Server-thread samples passed through `ServerChunkCache.getChunkNow`.
- Added one `LevelChunk` reference to the existing loaded-section `SectionOwner`
  and reused `ownersBySection`; active-runtime dormant fallback now bypasses
  chunk-future/Optional lookup without a new map or lifecycle.
- Passed current `PageState` and `nextSignatures` through Brick material
  compilation. Same-Page Air adjacency now uses direct references, while genuine
  cross-Page access retains `TopologyView` hash lookup. Removed the unused old
  overload.
- Removed redundant `HeatingTransition` construction from phase ownership;
  `MinecraftThermalProfiles.phaseProfileId` remains the eligibility authority.

## Decisions

- No persistent cache, counter, probe, Page/source traversal, or compatibility
  path was added. SectionOwner releases the chunk reference on normal unload.
- The no-runtime bootstrap fallback still uses `getChunkNow` because no
  `MinecraftPageManager` exists at that boundary.

## Validation

- Production, JUnit, and GameTest source compilation: passed on Java 17.
- Focused thermal JUnit: `99/99` passed.
- Forge GameTest: all `14/14` required tests passed.
- Removed-overload search and `git diff --check`: passed.

## Remaining

- Repeat the same 120-second workload to measure the post-fix reduction. No
  percentage is claimed from pre-fix evidence.
