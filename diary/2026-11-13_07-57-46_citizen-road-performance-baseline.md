# Citizen road performance baseline

- Author: Codex
- Date: 2026-11-13 UTC
- Status: completed; settings restored and client closed
- Scope: development-only population stimulus, fixed-camera timing and JFR sampling

## Changes

- Added TownRoadPerformance and dispatched roads perf commands from TownRoadClientTools; no production source changes.
- Added reproducible matrix and JFR analysis scripts under Scripts/.
- Added docs/town/citizen-road-performance.md plus JSON/CSV summaries under docs/town/validation/ and linked the report from the town README.

## Validation

- Test tool compiled successfully; a real development client completed all seven stages with a single fixed camera.
- Production baseline 86eedc23a. One logical CPU, llvmpipe software rasterizer, 1280x720, view distance 6, about 1.73 GiB maximum heap.
- 1000-person moving cases averaged 919–929 actual position changes per server tick. Tick p95 about 5–6ms; full-frame FPS about 12–15 on this software renderer.
- JFR identified sampled allocation hotspots in RoadPresentation.surface/clearBody (about 73.1% combined estimated steady-window allocation weight) and two SerialOld pauses about 1.28s each. First allocation samples and record boundaries were excluded from weighted attribution.
- Complete data, sampling definitions, limited repeat count and hardware limits are in the report. No GPU-hardware conclusion, dedicated-server-only throughput result, or optimized-code speedup claimed.
- All commands succeeded, including restore and quit. Log confirms all dimensions saved and BUILD SUCCESSFUL. No fresh functional GameTest run; production behavior was unchanged.
- Script syntax parsed successfully and git diff --check passed. Changes are left uncommitted because this request was performance testing and reporting.
