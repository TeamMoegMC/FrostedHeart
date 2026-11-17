# Infrared: missing values untinted, translucent surfaces read the block behind them

- Time: `2026-11-15 12:37:24 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `implemented; shader compiled against the local OpenGL driver; not checked in game`
- Scope: `src/main/resources/assets/frostedheart/shaders/infrared_view.fsh`, `InfraredPhysicalE2eClient`, infrared and
  world-climate documentation
- Follows: [unpublished field](2026-11-15_12-22-38_infrared-unpublished-field.md)

## Report

After the new-world fix the natural base showed, but glass was violet, and frozen rivers were a violet ground with
chaotic blue streaks that flickered wildly whenever the view moved (owner's screenshot
`run/screenshots/2026-11-15_12.03.35.png`). The owner asked for missing values to stay untinted.

## Cause

Only SOLID and CUTOUT terrain goes through the capture pass and its exact block owner. Ice, water, translucent glass
and entities fall back to reconstructing a camera-relative position from depth and reading the 144³ texture at
`floor(position)`. A translucent surface lies exactly on a block boundary, so depth precision flips `floor()` between
its own block (with a natural value, blue) and the air in front of it (missing, violet), pixel by pixel and frame by
frame.

## Completed

- The fallback reads `SURFACE_DEPTH = 0.05` blocks behind the surface, along the screen-space normal
  `cross(dFdx(world), dFdy(world))` turned away from the camera, or along the view ray when the normal is degenerate.
  The derivatives are taken before any `discard`, where they are defined.
- A missing value (`INVALID`) is discarded: the pixel keeps the scene's own colour, so violet now means a real
  −50 °C or colder.
- `InfraredPhysicalE2eClient` expects an untinted frame for a missing value.

## Validation

- The shader compiles in an offscreen OpenGL 3.2 core context on this machine (GL 4.1 Metal), with the checker from
  the colour-scale entry.
- `compileJava compileGameTestJava` passes. No GameTest covers the shader.

## Documentation

- `docs/climate/infrared-view.md`: the colour section notes untinted missing values; new section on non-terrain pixels;
  "blue/violet" section rewritten as "untinted"; diagnostic table.
- `docs/climate/world-climate-and-temperature.md`: missing values untinted; entity and translucent pixels step behind
  the surface.

## Remaining

- Not checked in game; silhouette edges, where a 2×2 pixel quad spans two surfaces, can still pick a wrong cell.
- The real-client E2E (`InfraredE2eClient`, `InfraredPhysicalE2eClient`, `Scripts/run-infrared-network-e2e.py`) has
  been out of date since the natural base: it reads the GPU texture and expects it to equal the server's wire values
  and to become INVALID after a material retires, while the texture now shows the natural temperature there. It was
  not run or reworked here.
