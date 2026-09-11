# White curtain V1 atmospheric wall and render-path reduction

- Time: `2026-08-31 05:20:46 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `partial`
- Scope: `SpatialWeatherRenderer`, `WeatherQualityProfile`, white-curtain texture, focused renderer tests, living weather documentation and V1 plan

## Completed

- Replaced the straight wall front with a connected deterministic strip. Adjacent segments share boundary offsets, producing at most `2.5/2 blocks` of Fast/Fancy front variation without cracks or additional quads.
- Added `48 blocks` smooth endpoint fading, deterministic per-boundary opacity, independent bounded U/V layer flow and a lower top alpha. The wall keeps the existing shader and one wall batch.
- Reduced Fast/Fancy wall geometry from `4x16 / 8x32` to `3x12 / 5x20`: `64/256` became `36/100` maximum wall quads per frame, with fewer full-screen translucent layers.
- Added one wall-level frustum test before segment construction while retaining per-segment distance culling for very wide corridors.
- Cached the active snow lattice's world coordinates and hashes in fixed primitive arrays. The cache rebuilds only when the camera crosses a lattice cell or the quality profile changes.
- Moved radial rejection before previous/current grid precipitation sampling, so the square lattice's outside-circle cells no longer pay the expensive sample path.
- Replaced `white_curtain.png` with a deterministic seamless `64x256 RGBA` wind texture. After user screenshots showed that the first crossing-diagonal version looked like fabric, it was replaced by fine-scale cloud snow with only one wind direction.
- Removed the remaining night-time black holes by compressing texture alpha to `[51,193]`. Added one per-frame sky-darkness scalar shared by wall and near-snow vertex RGB, limited wall height to camera-relative `-64/+96 blocks`, and reduced top alpha to `8%`; this corrects the unlit shader appearance without a lightmap or block-light sampling.
- Raised the typical night brightness from `34.4%` to `64%` after manual feedback showed that full darkening made the curtain unreadable. Height, top-alpha and fog-distance limits remain responsible for containing glow.
- Unified wall visibility with Vanilla terrain fog by switching wall and near snow to the built-in `particle` shader and vertex format. The shader consumes the already configured fog uniforms and light texture; CPU culling remains a fixed work cap rather than a dynamic visibility boundary.
- Reframed the accepted V1 target as a snow version of a dust storm. The outside wall now uses alpha `[154,235]` and roughly `85%` three-layer composite opacity; inside whiteout caps both Fast and Fancy terrain fog at `16 blocks`, after which the wall is culled and near snow remains.
- Removed the later asymmetric Y-margin workaround after it still produced visible threshold changes. Geometry returned to fixed camera-relative `-64/+96 blocks`; the same particle fog shader as Vanilla precipitation now performs all final X/Y/Z visibility fading.
- Fixed a live-only static initialization regression found during the first world entry: `INSTANCE` had been initialized before `MAX_SNOW_COLUMNS`, creating zero-length lattice arrays and quarantining the renderer on its first snow frame. Static constants now initialize before the singleton, with no runtime guard or resize path.
- Updated the living rendering document and V1 plan with current formulas, limits, ownership, validation and remaining release gates.

## Decisions

- Keep V1 on the Vanilla position/texture/color shader with no framebuffer, temporal history, custom shader, terrain-conforming wall or worker thread.
- Spend existing segments on front silhouette variation and endpoint fading rather than adding more translucent layers.
- The built-in image generation tool was unavailable in this session. A deterministic local bitmap generator was used because repeatability, alpha distribution and exact edge continuity are more important for this texture than unconstrained image synthesis.

## Validation

- Texture: `64x256`, mean alpha `0.7753`, alpha range `[154,235]`; opposite-edge continuity is enforced by the focused resource test.
- Java 17 focused validation: `ClientWeatherStateTest`, `ClientWeatherFrameTest`, and `SpatialWeatherRendererTest`; `3 suites / 20 tests / 0 failures / 0 errors`.
- Java 17 `jar` and `reobfJar` passed; `frostedheart-1.20.1-0.7.7.jar` contains `assets/frostedheart/textures/environment/white_curtain.png`.
- Development client compiled and launched. Its first world entry exposed the zero-length lattice regression through a direct `prepareSnowLattice` stack trace; the initialization order was corrected. The user reserved screenshot and visual judgment for manual testing, so no automated UI screenshot was captured.
- Three user screenshots of the first V1.1 visual pass rejected its crossing-diagonal texture and coarse front displacement: the layers read as fabric overhead and produced large translucent ground polygons. Those parameters were removed; the final candidate uses single-direction cloud snow, `2.5/2 blocks` roughness and `6/4.5 blocks` layer spacing. A new user capture is still required.
- A later night screenshot found black alpha holes and a wall that stayed self-lit far above the player. The texture floor, sky-brightness modulation and camera-relative vertical fade now directly cover those observations; another night capture remains required.
- User testing first found that the wall ignored reduced whiteout range, then that CPU far-plane/Y-axis culling made it disappear abruptly. Inspection confirmed `position_tex_color` had no fog code while Vanilla precipitation used `particle` with `linear_fog`; the renderer now follows the Vanilla path. Another inside-whiteout capture remains required.

## Remaining

- User manual screenshots from the clear side, corridor endpoint, close front and storm interior in Fast and Fancy.
- Profile render-thread/GPU P50/P95/P99 and allocation at 1080p/4K and 60/144/240 FPS, including Embeddium/Oculus.
- Persistent VBO/staging reuse and terrain/depth-aware effects remain measurement-gated; they were not added speculatively.
