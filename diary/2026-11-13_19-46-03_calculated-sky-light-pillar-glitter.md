# Calculated Sky Light Pillar Glitter

- Time: `2026-11-13 19:46:03 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarOptics`, `LightPillarRenderer`, `SkySetting`), `shaders/light_pillars.glsl`, GameTests, `docs/climate/calculated-sky.md`

## Completed

- Light pillars glitter and twinkle instead of showing still bands, after `2026-11-13_19-01-53_calculated-sky-light-pillar-overhangs-near-fade.md` (`7f464cbee`). The crystal count, the cells, the twinkle clock, the ribbon rule, and the settings `pillarCrystalSize`, `pillarTwinkleSeconds`, and `pillarNearFade` are in [Light pillars](../docs/climate/calculated-sky.md#light-pillars) and [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).

## Decisions

- empireu asked on `2026-11-13` for believable glint and grain on the bands, after research on real pillars and diamond dust. Real pillars are the summed glints of single crystals, so one Poisson count per cell gives both the grain of far pillars and the sparse glints of near ones from one physical size, neither strength being a free parameter.
- Rejected the first ribbon rule, holding the center's value times `sparkleCap`: the count falls off center while each crystal keeps its value, so it left glints of about the visible value outside the ribbon in up to `41%` of pixels at the edge. The rule by chance replaced it, and `1%` instead of `0.2%` saved a third of the extra area but left `8` to `11%` near the zenith.
- The twinkle period of `0.25 s` and the `0.25` blocks per period while moving are chosen by eye, to be tuned in game; a crystal `50` blocks away crosses a pixel in about `0.07 s`.
- Left out bokeh, flutter modes, and wind-driven tilts: bokeh is a camera effect, the zigzag and spiral fall modes belong to plates of millimeters, and diamond dust plates fall at a Reynolds number around `1`.

## Validation

- Probes outside the game keep the mean value and never exceed the cap, and ribbons cover about `64%` more sky than without glitter.
- GameTests: all `51` pass. The first run failed two new checks, a blended value one rounding step above its cap and the count of wobbling plates at `3 degrees`, now with a tolerance of `25%`. New: `lightPillarCrystalCountMatchesTheRayIntegral` (within `1.2%` for plates and `23%` for wobbling plates), `lightPillarSparkleKeepsTheMeanValue`, and `lightPillarRibbonsHoldTheGlints`.
- Both shader stages pass `glslangValidator`. empireu tuned `pillarCrystalSize` in game and chose `65` micrometers, the default. The full `frostedheart` namespace was not run.

## Remaining

- In game: `pillarTwinkleSeconds`, turning the head, walking, pausing, and the GPU time.
- Whether `pillarNearFade` should come down now that the crystal count dims near crystals by itself.
- The remaining items of the last entry: the in-game checks, the docs for the scan, the shadows, and the overhangs, the zenith leak of ribbon edges, `lightPillarLightPassesBelowTheViewRayBeforeTheLight`, the accumulation target, and `CalculatedSkyClientCommand.ENABLED` before a pull request.
