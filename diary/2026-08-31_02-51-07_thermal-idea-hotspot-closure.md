# Thermal IDEA hotspot closure

- Time: `2026-08-31 02:51:07 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `material profile lookup and pre-runtime section-owner fallback`

## Completed

- Analyzed `IdeaSnapshots/BootstrapLauncher_2026_08_31_023223.jfr`; its
  effective event window is `185.8s`.
- Replaced boxed material profile/contact-pattern maps with dense, direct-index
  arrays matching the existing `1..N` profile construction contract.
- Made the first dormant query for a pre-runtime loaded section attach that
  section's existing chunk/section owner. Later queries no longer repeat
  `ServerChunkCache.getChunkNow`.

## Decisions

- No Page admission scan, chunk enumeration, cache, counter, pool, solver
  change, or persistence-format change was added.
- Material IDs are validated once at registry construction; Brick compilation
  performs only bounds checks and array loads.

## Validation

- Production, JUnit, and GameTest source compilation passed on Java 17.
- Focused thermal JUnit passed: `99/99`.
- Forge GameTest passed: `14/14` required tests.
- `git diff --check` passed.

## Remaining

- Repeat the same IDEA profiler workload before claiming measured percentage
  reductions.
