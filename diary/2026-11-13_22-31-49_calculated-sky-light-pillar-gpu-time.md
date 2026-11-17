# Calculated Sky Light Pillar GPU Time

- Time: `2026-11-13 22:31:49 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`LightPillarOptics`, `LightPillarRenderer`, `CalculatedSkyClientCommand`), `light_pillars.glsl`, GameTests, `docs/climate/calculated-sky.md`. After `2026-11-13_19-46-03_calculated-sky-light-pillar-glitter.md` (`5d02380dc`).

## Completed

- Ribbons hold glints only from `GLINT_VALUE` on, eight display steps, with `RIBBON_GLINT_CHANCE` at `1%`, and hold the smooth pillar down to `VISIBLE_VALUE`; `leastVisibleCount` counts a crystal as reaching the glint value from `10%` below it (`RIBBON_GLINT_MARGIN`). The rule is in the docs under [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).
- The integral of `pillarRadiance` is rewritten for the GPU without changing what it computes, at `3` exponentials per point instead of `8`, as described under [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).
- `/calculatedSky pillars fill` switches a fill view that marks every pixel a ribbon covers green where its pillar shows and red where the fragment stage runs for nothing.

## Decisions

- empireu measured `1.34 ms` for `44` slots of a small torch field at about `1080` pixels, against the plan's `0.5 ms`, so the vertex stage is not worth changing; the readings are in the docs under [Validation And Performance](../docs/climate/calculated-sky.md#5-validation-and-performance).
- A probe put the cost of glitter in the ribbon area: with every glint held to half a step at `0.2%`, torch ribbons `8` to `32` blocks away covered `2.45` times the area of smooth ones. empireu first chose four display steps, then eight steps and `1%` after reading `0.45 ms` at `75` micrometers, where glitter still cost about half.
- empireu asked for the fill view, to see how many pixels are computed for nothing, and to keep it in the final code behind the command.

## Validation

- A port of the rewritten shader math to Java matched `dustRadiance`, `dustCrystalCount`, and `widestLateralSpread` within `5e-10` over `200000` random cases, and in single precision matched the old shader's errors. Both shader stages pass `glslangValidator`.
- A read-only review found that a `pillarNearFade` below about `4e-39`, a denormal float, made the near weight NaN; the offset is now the constant `-1/3`.
- `51` of `51` GameTests, after a first run failed `lightPillarRibbonsHoldTheGlints` where one crystal showed `1.00` to `1.03` times the glint value. The chance is now at most `0.87%` away from a ribbon's ends and `4.1%` anywhere.
- In game empireu read `0.37 ms` at `75` micrometers, against `0.45 ms` before, and saw no visible change. The fill view showed the torch field's ribbons stacking to yellow and near pillars red with green specks.

## Remaining

- The pillars alone take most of the `0.5 ms`, so glints and the dust in the sky pass need their own budget or further cuts here.
- The remaining items of the last entry.
