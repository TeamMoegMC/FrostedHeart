# Citizen shared Body/Billboard LOD hysteresis

- Time: `2026-08-20 02:33:34 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `CitizenRenderCoordinator`, `CitizenRenderOwnership`, `FlywheelCitizenBackend`, CPU batch ownership, tests, and town rendering documentation

## Completed

- Restored the visual 68/72-block hysteresis instead of using a stateless 64-block cutoff.
- Added a client-only `CitizenRenderCoordinator` map from citizen id to the current batch owner. CPU and M3 both consume and update this owner; the map is cleared on despawn, benchmark removal, world transition, and out-of-range eviction.
- Removed M3's private batch-owner resolver and its backend-local LOD policy. `FlywheelCitizenBackend.Entry.owner` remains only the GPU instance's current allocation, while the coordinator owns the cross-backend policy state.
- Preserved the 96-block batch limit and detailed-entity priority. No server packet, persistence field, or synchronization contract changed.
- Updated living town documentation and added boundary/state-reset tests.

## Decisions

- Body enters below `68` blocks and remains through `72`; Billboard remains at or beyond `68` until it leaves the `96`-block AOI. Exact threshold behavior is centralized in `CitizenRenderOwnership`.
- Keep this state client-local because it depends on camera-relative presentation and must survive CPU/M3 backend replacement, not network synchronization.
- Supersede the earlier 64-block unification decision; that version removed useful hysteresis and was inferior for visual stability.

## Validation

- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.client.*" --console=plain`: passed.
- `./gradlew.bat test --console=plain`: passed (`BUILD SUCCESSFUL`).
- Tests cover entry/exit hysteresis, shared coordinator state across backend calls, 96-block eviction, and re-entry without stale Body ownership.

## Remaining

- In-game Oculus/CPU/M3 switching still requires the existing visual regression pass; automated tests cover ownership continuity, not GPU output.
