# Thermal functional package layout

- Time: `2026-08-31 02:20:42 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal Java package locations, imports, visibility boundaries, and core ownership comments`

## Completed

- Reduced `thermal.runtime.minecraft` to the public gameplay facade and Forge
  lifecycle entry point.
- Grouped input capture, immutable messages, dimension execution, asynchronous
  transport, topology, persistence, profiles, sources, radiation, fields, and
  queries by functional responsibility.
- Moved Brick kernels, Page topology state, and the
  `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter` transaction
  into `thermal.topology`; no topology implementation remains under a generic
  worker package.
- Added concise class Javadocs for ownership, thread boundaries, transaction
  order, and persistence scope. No `package-info.java` files were retained.
- Updated the living runtime architecture document with the current source map.

## Decisions

- Package names describe function rather than the thread that happens to execute
  the code.
- Method bodies, algorithms, state meanings, and execution order were not
  changed. Visibility was widened only where an existing call now crosses a
  functional package boundary.
- Git contains no rename metadata; Git/GitHub detect these moves by content
  similarity after the new paths are staged.

## Validation

- Production, JUnit, and GameTest source compilation passed on Java 17.
- Focused thermal JUnit passed: `99/99`.
- Forge GameTest passed: `14/14` required tests.
- Package declarations match source paths; stale old package references,
  thermal `package-info.java` files, and whitespace errors are absent.

## Remaining

- Before committing, stage the complete move and inspect rename detection with
  `git diff --cached --summary -M`.
