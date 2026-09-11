# Player radiation gameplay connection

- Time: `2026-08-25 18:17:25 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `MinecraftThermalInput production radiation startup/static occlusion, TemperatureUpdate body consumption, climate documentation and thermal implementation plan`

## Completed

- Enabled the existing `RadiationService` when the player gameplay runtime starts and replayed live Campfire/Generator radiation sources through the existing physical-source manager.
- The loaded-only ray path now reads `BlockState` directly: `hasDynamicShape=true` is air, while automatically trusted static states use `BlockState.canOcclude()` as transparent/full-block opaque. It does not allocate resolver snapshots or positions and adds no per-mod checks, partial-shape raster, new runtime class, or periodic compatibility scan.
- Reused the existing caller-owned `MutableEnvironmentSample` in `TemperatureUpdate`; radiant flux now becomes absorbed energy and a five-body-part temperature delta at the existing update cadence, plus an equivalent radiant-temperature contribution to the existing HUD feeling value.
- Updated the living climate docs and implementation plan to distinguish active player air/radiation from still-dormant material radiation and non-player authority.

## Decisions

- Production radiation is bounded to `16` blocks, `0.1 W/m2`, `64` candidate visits, top `8` sources, `24` rays, and `256` quarter-block DDA steps. Its projected optional reservation is `729,408 B` per dimension.
- Body conversion is `q * 0.7 m2 * 0.8 * seconds / 5,000 J/K`; HUD feeling uses `q * 0.8 / 6 W/m2/K` without adding body energy twice. Source radiation remains a single declared loss and receiver observation never writes the Air Mesh or source ledger.

## Validation

- Targeted Java 17 JUnit: `239/239` passed (`238` thermal plus `1` player radiation conversion test).
- Forge GameTest: `19/19` required passed. The radiation test used production parameters/classification and verified visible Campfire flux, stone-wall blocking, restoration after removal, cache reuse, and unchanged source watermark.
- Full repository JUnit compiled and ran `814` tests: `813` passed; the only failure was the existing missing external fixture in `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`.

## Remaining

- Calibrate the provisional body-response constants and production caps in a real multiplayer save workload.
- Material radiation, surface composition, approved FarField profiles, and non-player gameplay authority remain separate work.
