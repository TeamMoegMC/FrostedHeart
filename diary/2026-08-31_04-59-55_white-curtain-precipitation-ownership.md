# White curtain precipitation ownership correction

- Time: `2026-08-31 04:59:55 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `ClientWeatherState`, weather ownership regression tests, V1 weather documentation and plan outcome

## Completed

- Separated global snow/blizzard grid values from descriptor-produced white-curtain precipitation ownership. Ordinary weather no longer cancels Vanilla `renderSnowAndRain` in spatial V1 modes.
- Kept global climate as a grid base only while a visible white curtain needs the spatial grid, so entering a curtain still has one precipitation owner without dropping concurrent global weather.
- Added zero-work exits when there are no white-curtain kernels or no near/wall candidates. Ordinary weather now skips V1 grid rebuilds and snow-column rendering.
- Reused the existing grid boolean as `hasSpatialPrecipitation`; no packet, allocation, render batch, server work or new runtime abstraction was added.
- Removed the newly added duplicate state test after review. The regression remains covered by one frame ownership test and assertions added to an existing state test.

## Decisions

- Ownership is triggered only by unattenuated `WhiteCurtainDescriptor` field contribution. Global climate may affect custom intensities after ownership is already necessary, but cannot acquire ownership by itself.
- A distant wall uses `WALL_ONLY`, preserving ordinary Vanilla/legacy blizzard precipitation while drawing only the white-curtain wall.

## Validation

- Java 17 targeted validation: `ClientWeatherStateTest` and `ClientWeatherFrameTest`, `2 suites / 16 tests / 0 failures / 0 errors`.
- Compilation completed through the targeted Gradle test task.

## Remaining

- Manual in-world check: ordinary snow and blizzard in Fast/Fancy must use the legacy precipitation appearance; a distant curtain must overlay its wall without replacing that weather; entering the curtain must switch only then to spatial near-field precipitation.
- Existing V1 GPU/JFR and shader compatibility release gates remain open.
