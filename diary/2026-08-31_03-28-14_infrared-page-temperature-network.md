# Infrared Page temperature network and texture implementation

- Time: `2026-08-31 03:28:14 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `infrared Page/query publication, client-carried presence network, GL_R16I rendering, legacy HeatArea deletion, tests, and climate documentation`

## Completed

- Replaced analytic/source HeatArea payloads with solver-published Air
  temperatures at one signed-short texel per 4x4x4 thermal Brick.
- Added Page temperature change IDs, an 80-tick dimension activity window,
  coherent multi-slot read cursors, fixed Page-memory admission, and
  topology-owned worker Page identities.
- Reused `MinecraftPageManager.pagesByChunk` for a 9x9 horizontal bucket scan
  plus actual Page entries; transient geometry gaps retain last coherent Page
  presence.
- Reworked the existing two packet classes in place. Stable clients poll every
  40 ticks with entity-ID staggering, exact 729-bit presence, and their last
  change ID; unchanged state emits no S2C payload.
- Replaced the 512-entry fragment loop and UBO with a 36x36x36 GL_R16I texture,
  one integer fetch per valid fragment, one flat CPU mirror, and full/delta Page
  updates.
- Deleted infrared-only source/analytic encoders, UBO config, shader structs,
  and dead update overloads.
- Updated the climate behavior, runtime architecture, lifecycle documentation,
  and implementation plan. Existing dormant/hotspot worktree changes were
  preserved.

## Decisions

- Keep one 25 KiB Page change-ID array per active dimension instead of
  per-player state, hashes, payload caches, Brick revisions, or an observer
  manager.
- Use 0.25degC quantization, exact client-carried presence, 40-tick polling,
  and an 80-tick tracking window.
- Prefer current Page publication and fall back to the last coherent worker cut
  during bounded geometry mutation; retirement alone clears presence.
- Pixel upload follows Oculus `TextureUploadHelper`: reset unpack row length,
  skip rows, skip pixels, and alignment to `0/0/0/4`, then restore the previous
  3D texture binding. No PBO path or speculative extra state wrapper is kept.
- Flatten the CPU mirror in OpenGL X-fastest, then Y, then Z order. Bias the
  depth-reconstructed temperature sample `1/2048` of the camera ray toward the
  camera so geometry on a 4-block texel boundary selects the visible-side Air
  texel without another texture fetch.

## Validation

- `compileJava compileTestJava compileGameTestJava`: passed on Java 17 after
  forcing one stale incremental main-class output to rebuild.
- Focused thermal and infrared packet JUnit: `103/103` passed.
- Forge GameTest server: all `14/14` required tests passed.
- Maximum 729-Page response codec stays below 96 KiB.
- Client smoke reached the existing world, loaded
  `frostedheart:infrared_view` successfully, and delivered a full response to
  the render thread.
- Two client uploads exposed the same native NVIDIA crash in
  `glTexSubImage3D` (`hs_err_pid24508.log` and `hs_err_pid31568.log`). The
  second run already bound `GL_PIXEL_UNPACK_BUFFER=0`, disproving the first
  PBO hypothesis.
- Local dependency inspection found that Embeddium interpolation calls Vanilla
  `NativeImage.upload`; `NativeImage._upload` leaves global unpack row-length
  and skip state. Oculus has a dedicated `TextureUploadHelper` that resets
  exactly row length, skip rows, skip pixels, and alignment before custom
  texture uploads. The final implementation mirrors that contract.
- Live visual inspection exposed two independent mapping defects after the
  upload crash was removed: the CPU mirror initially interchanged OpenGL Y/Z,
  and an unbiased `floor` let depth reconstruction error alternate between Air
  texels on `4n` block faces. The mirror order is corrected and the shader now
  applies one constant camera-ray multiply/add before its existing single
  `texelFetch`; post-fix live visual validation remains pending.
- Crouching exposed an inherited camera-origin mismatch: the depth/view matrix
  used Minecraft's smoothed main `Camera`, while the infrared uniform used the
  entity's immediate eye position. The uniform now reads the same main Camera
  position, also correcting third-person reconstruction without new state.
- Campfire smoke uses Minecraft's translucent particle type but still writes
  depth, so the unified post-process samples the Air texel crossed by the
  particle. A particle-exclusion/two-stage prototype was investigated and
  reverted at the user's direction; the final path keeps the single
  `AFTER_LEVEL` pass and the existing no-hand/no-translucent depth contract.
- `git diff --check` passed; only existing LF/CRLF conversion warnings remain.

## Remaining

- Repeat the live client toggle after the pixel-store fix and visually verify valid,
  invalid, movement, and Oculus depth paths. Computer Use was stopped by the
  user before this rerun.
- Run the single planned inactive/active JFR and 100-player non-overlap request
  fixture before recording measured performance percentages.
