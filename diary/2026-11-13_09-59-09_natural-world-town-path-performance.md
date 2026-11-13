# Natural-world town path performance

- Author: Codex
- Date: 2026-11-13 UTC
- Status: nine valid timed rounds complete; final game saved and closed
- Scope: actual TownConnectivity on generated terrain; development-only probes, no production algorithm edits

## Changes

- Add opt-in TownPathProbe, dedicated pathmixin package/resources and profile init script; dispatch natural-world preparation/build/arm/barrier/seal/status commands through the development bridge.
- Add reproducible Scripts/run-town-path-performance.py and document the profile configuration. The default profile heap is 4G and can be overridden with townPathHeap.
- Add docs/town/town-path-world-performance.md, JSON/CSV validation summaries and a town README entry.

## Validation

- Development code compiled and the real client ran normal-preset seed 12345, coast/dark forest/frozen peaks, building floor heights Y62–160.
- Real 8/16/32-controller houses with scanned doors, unflattened terrain between houses, 221 preloaded chunks and zero residents. All measured section requests stayed in loaded chunks; no tick served over two sections.
- Nine worker tasks completed successfully: first/repeated 8/16/32, wall, reloaded 32-control and sealed previously reachable entrance. Added an idle 32-building control.
- Default-heap 32 recompute: 49.46s elapsed, 336ms worker CPU, 1876ms main CPU, 44.72s explicit snapshot waits; 1278 requests / 253 unique sections (about 80.2% repeated).
- Wall-only placement did not change 14 witness edges / largest cluster 8, so it was not treated as proof of new disconnection.
- Same fixed-4G session control/seal: 43.33s / 42.17s; witness edges 14 -> 13, components 18 -> 19. Largest size stayed 8; the automation initially over-constrained this, reported an assertion after valid measurement, and was corrected in the replay/analysis to validate forest components. Original diagnostic retained.
- Probe package mistake during the first startup and an OOM during default-heap reload were excluded. The OOM client failed to terminate normally and was explicitly stopped; fixed-4G retry completed and saved normally.
- Both effective sessions restored options and released 221 forced chunks. Final client log confirms stopping and all dimensions saved at 09:48:10 UTC, BUILD SUCCESSFUL.
- Report scopes CPU versus wall/overlapping waits and discloses single-core llvmpipe, different heap sessions, background stalls and probe reference overhead. No production optimization speedup claimed.
- Replay Python syntax and JSON/CSV consistency checked; git diff --check passed. No new functional GameTest suite was run for profiling-only code. Changes remain uncommitted.
