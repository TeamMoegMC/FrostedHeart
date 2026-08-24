# White curtain V1 visual density and stable precipitation lattice

- Time: `2026-08-24 08:09:21 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `partial`
- Scope: `SpatialWeatherRenderer`, white-curtain client texture, renderer regression tests, climate weather documentation and V1 plan

## Completed

- Confirmed the reported Fast-mode defects in source and resources. The old snow-column origin used `floor(camera)` with `2-block` spacing, so a one-block move replaced the entire parity set. Vanilla `textures/environment/snow.png` contains only `129/16384` opaque pixels, yet the wall multiplied it by vertex alpha `0.10..0.28`.
- Anchored Fast/Fancy precipitation to world cells. Positions remain identical while the camera stays in a cell; crossing a cell retains the interior set and replaces only the radial-fade edge. A deterministic per-cell hash adds horizontal jitter without frame or camera dependence, including at negative coordinates.
- Added `assets/frostedheart/textures/environment/white_curtain.png`, a seamless `64x256 RGBA` dense snow-veil texture. Existing wall geometry now binds it with per-slice UV offsets and `a = (0.24 + 0.30s)d`; near snow keeps the Vanilla flake texture.
- Preserved the existing Fast/Fancy wall, snow-column and draw limits. The change adds no render pass, shader, framebuffer, quad, packet or server work. Per-column division and per-quad clock/UV work were moved outside hot loops.
- Updated `docs/climate/weather-rendering.md` and the V1 implementation plan. No lifecycle, persistence, configuration or network document changed because their contracts are unchanged.

## Decisions

- Use a dedicated deterministic tile texture in the existing wall batch instead of another full-screen blend pass or a custom shader. This makes the wall continuous without extra overdraw or a shader compatibility surface.
- Keep local snow streaks on the Vanilla atlas and solve their instability at coordinate ownership, rather than hiding the pop with extra particles or interpolation.
- Treat automated alpha/edge/composite checks as resource regression evidence only. They do not replace an in-world visual pass.

## Validation

- Before the fix, `SpatialWeatherRendererTest` produced `3 tests, 3 failures`: both Fast anchoring assertions and the missing dense wall resource failed.
- Java 17 targeted white-curtain suite: `9 suites, 38 tests, 0 failures, 0 errors`.
- Java 17 full suite: `153 suites, 588 tests, 0 failures, 0 errors`.
- Java 17 `jar`/`reobfJar`: passed; the packaged mod JAR contains `assets/frostedheart/textures/environment/white_curtain.png`.
- Wall texture inspection: `64x256`, alpha range `99..236`, mean alpha `151.67/255`, opposite-edge mean RGBA delta `6.70` horizontally and `0.80` vertically.

## Remaining

- Restart the development client and repeat the same Fast path across individual block and two-block boundaries; verify that only the invisible outer band changes and no interior snow column rephases.
- Recheck wall density from clear side, at the snow front and inside the core in both Fast and Fancy. Tune the dedicated texture/vertex alpha only from matched screenshots, not by adding geometry.
- Capture render-thread/GPU P50/P95/P99 and allocation with Embeddium/Oculus; automated work caps prove unchanged scaling but not an absolute millisecond result.
