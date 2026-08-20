# House and Warehouse Texture Swap

- Time: `2026-08-20 23:20:29 +08:00`
- Author: `Codex; GPT-5; coding agent`
- Status: `completed`
- Scope: `Block texture assets under src/main/resources/assets/frostedheart/textures/block/`

## Completed

- Copied the previous `warehouse.png` texture to `house.png`.
- Replaced the block `warehouse.png` texture with the new root-level `warehouse.png` source asset.

## Decisions

- Kept existing model and registry references unchanged because they already resolve the same texture paths.
- Preserved the root-level source image; it remains available as an untracked user-provided file.

## Validation

- Confirmed all three images are `16x16` PNGs.
- Confirmed `house.png` matches the previous warehouse texture SHA-256 and the block `warehouse.png` matches the new source SHA-256.

## Remaining

- None.
