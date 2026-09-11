# State transition recipe profile activation

- Time: `2026-08-25 19:39:39 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `StateTransitionData hot-side profile compilation, gameplay phase mutation ownership, reload lifecycle, documentation, and validation`

## Completed

- Added a hot-side transition view to `StateTransitionData` and compiled automatically trusted static BlockStates into shared `PHASE_RESERVOIR` profiles during gameplay startup.
- Reused the existing recipe target BlockState for main-thread mutation, connected ownership only for exact candidates installed in an applied Page, and left cold-side, dynamic-shape, empty-contact, and Page-miss cases on the legacy path.
- Added `APPLY_STATE_TRANSITION_RECIPE`, recipe-reload invalidation, and the `ICE_DO_NOT_SMELT` mutation gate without adding another data format, per-mod compatibility scan, or runtime profile table.
- Updated the climate living documentation and active thermal plan.

## Decisions

- Existing `StateTransitionData` recipes remain the gameplay authority. The first reached hot-side threshold becomes one latent-energy stage; the replacement state supplies any later stage, while equal thresholds retain legacy gas priority.
- Gameplay calibration is currently `20 W/K` per full face and `38,000 J * heat_capacity` per transition unit. The old `heat_capacity` field remains a relative pacing value, not physical heat capacity.
- Protected ice-biome mutations return retry without changing the block or committing reserved latent energy.

## Validation

- Java 17 compilation passed.
- Repository JUnit executed `817` tests: `816` passed; the only failure was the existing missing external fixture in `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`.
- Thermal JUnit passed `238/238`, player radiation conversion passed `1/1`, and `StateTransitionDataTest` passed `3/3`.
- Forge GameTest passed all `19/19` required tests. Production startup compiled `707` hot-side BlockStates into `6` shared profiles and `4` contact patterns; `1` state retained legacy handling because it had no conservative material contact.

## Remaining

- Calibrate transition conductance and energy against real in-save ice, snow, permafrost, and magma chains. Cold-side phase authority and non-phase gameplay material profiles remain separate future work.
