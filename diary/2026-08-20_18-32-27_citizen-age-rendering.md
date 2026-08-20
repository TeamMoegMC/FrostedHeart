# Citizen age rendering

- Time: `2026-08-20 18:32:27 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `Resident age presentation through Citizen sync, FakeEntity, CPU batch, and Flywheel rendering`

## Completed

- Mirrored authoritative `Resident.age` into two transient bits of the existing `CitizenSim.presentationFlags` array.
- Added the age byte to `S2CCitizenSpawnPacket`; low-frequency growth transitions reuse the same packet as an in-place metadata refresh and leave movement batches unchanged.
- Applied infant `0.4`, child `0.5`, and adult/elder `1.0` scale to detailed fake entities, CPU Body/Billboard, Flywheel Body/Billboard, culling bounds, light sampling, and picking.
- Reused the final byte of the existing 58 B Flywheel instance flags instead of increasing the stride.
- Updated the town simulation and rendering living documentation.

## Decisions

- Keep the implementation inside existing classes and packets. No age utility, appearance packet, renderer subtype, or test-only production abstraction was added.
- Preserve zero-filled transient state as adult so old saves and unmanaged Citizen rows keep their previous visual default.
- Treat age as presentation metadata only; it does not change Citizen movement, behavior, LOD distance, or server collision.

## Validation

- `./gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.CitizenRenderCoordinatorTest --tests com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenCullBoxTest --tests com.teammoeg.frostedheart.content.town.citizen.client.FlywheelCitizenBackendTest --tests com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimPersistenceTest --console=plain`: passed (`BUILD SUCCESSFUL`).
- `git diff --check`: passed.
- Source search found no remaining temporary appearance packet/class, shared age presentation class, `applyAppearance`, or Flywheel `reserved` field.

## Remaining

- Verify actual infant/child proportions in game across detailed FakeEntity, CPU Body/Billboard, and Flywheel Body/Billboard, including standing and sleeping views. Automated tests do not prove final shader/model appearance.
