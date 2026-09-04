# Thermal uniform signature and lazy mutation memory closure

- Time: `2026-08-30 22:33:43 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Page signature payloads, section mutation inbox memory, Page mutation buffers, tests, plan, and living thermal documentation`

## Completed

- Encoded uniform compact and wide Brick signatures as `char[1]` and `int[1]`.
  Nonuniform payloads remain `char[64]`/`int[64]`, and all reads remain direct
  O(1) indexed lookups.
- Replaced each mutated section's eager geometry bitmap with a non-geometry
  exception bitmap allocated only after a source-only event. Owner and manager
  scratch arrays exchange references under the existing lock, so normal door
  cuts add no copy or word traversal.
- Made `PageEntry` center/signature buffers absent until first mutation and
  reduced initial capacity from 32 to 8 while preserving geometric growth and
  the existing 32-center bitmap promotion.

## Decisions

- Length-1 primitive arrays reuse the existing immutable payload type contract;
  no scalar wrapper, interning cache, palette, or persistent-tree branch was
  introduced.
- The exception bitmap is required only to distinguish source-only positions
  from geometry positions in a mixed cut. A temporary per-owner word summary
  was removed because direct scratch-array exchange is both simpler and faster.
- Existing behavior tests were reused. One replacement value was changed to a
  wide signature ID so the same test covers compact and wide uniform payloads;
  no new implementation-detail test was added.

## Validation

- Java 17 production, test, and GameTest compilation: passed.
- Thermal JUnit: `96/96` passed.
- Forge GameTest: all `14/14` required tests passed.
- Residual searches found no `pendingGeometry`, temporary non-geometry word
  summary, or eager 32-entry Page mutation buffers.
- `git diff --check`: passed.

## Documentation

- Updated the living thermal architecture with uniform signature payloads and
  lazy section/Page mutation storage.
- Corrected the active plan's signature memory contract and marked all four
  representation-closure items complete.

## Remaining

- Measure retained Page-signature memory and current door/block CPU/allocation
  with controlled heap/JFR workloads before assigning an observed percentage
  improvement.
