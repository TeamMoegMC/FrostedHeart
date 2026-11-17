# Labor dispatch datagen fix

- Author: Codex
- Date: 2026-11-13
- Status: complete

Reproduced `runData` failure: Registrate default blockstate generation requested missing texture `frostedheart:block/labor_dispatch`. Added explicit `FHBlockStateGen.simpleCubeAll("transport_station")` to `FHBlocks.LABOR_DISPATCH`, matching the existing runtime placeholder texture. Included generated agency blockstate, block/item models, loot, language entries and tags. Preserved unrelated existing language ordering and upside-down research institute text.

Validation: `bash gradlew --offline runData --console=plain` succeeded in 3m 48s (exit 0). Logs: `build/labor-dispatch-datagen.log` (reproduction), `build/labor-dispatch-datagen-fixed.log` (success). `git diff --check` passed. Updated `docs/town/labor-dispatch.md` with the datagen contract. No gameplay changes.
