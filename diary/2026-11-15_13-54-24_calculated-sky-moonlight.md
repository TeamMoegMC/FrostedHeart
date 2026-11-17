# Calculated Sky Moonlight

- Time: `2026-11-15 13:54:24 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `in progress` (the moon's brightness is still to be chosen)
- Scope: `content/climate/render/sky` (new `MoonLight`; `SkyLight`, `SkyState`, `SkyBuilder`, `SkyViewSet`, `CalculatedSkyRenderer`, `SkySetting`), `calculated_sky.fsh`, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `b87493b16`.

## Completed

- **Moonlight.** The moon stands opposite the sun and lights the sky from the same tables, with `MoonLight.chooseLight` picking the light every frame and a smoothstep fade on each side of the switch ([Moon](../docs/climate/calculated-sky.md#moon)).
- **The kind rule.** `SkyState` records the light kind it was built for, so a switch builds again and reuses the halo maps and views on the same grid indices, and `SkyLight.radianceScale` is `0` until that build lands; the scale goes in before the tone map in the shader, the fog color, and the daylight ([Runtime](../docs/climate/calculated-sky.md#2-runtime)).

## Decisions

- The plan's colors were replaced after a literature check empireu ran with an outside agent: `(1.15, 1.00, 0.85)` was too weak, and the blue `(1.05, 0.97, 1.27)` I remembered from Jensen et al. is Durand and Dorsey's mild shift, so the end at `1` is Jensen's full scotopic hue.
- The moon's reddening away from full and the opposition surge are left out: quarter moons display nearly black, and the full moon's brightness is a gain set by eye.
- The terrain keeps vanilla's night light: the plan's floor in `daylightColor` only stops darkening, and a full moon high would light the terrain about twice as bright as vanilla's night light.
- While the sky is not drawn (blindness, darkness, a boss's world fog) no build is requested, so the fog stays black after a switch until the sky draws again; blindness and darkness darken vanilla's fog anyway.

## Validation

- A temporary GameTest (`SkyMoonlightSwitchGameTests`, by a Subordinate, deleted after) measured the switch elevation, tabulated in the docs. All `75` tests of `frostedheart_calculated_sky` passed, and the `109` hashes of `SkyBaselineHashGameTests` still equal those from before step 1.
- New GameTests by a Subordinate, which I reviewed: `moonPhaseBrightnessFollowsThePhaseAngle`, `sunAndMoonFadeToBlackAtTheSwitch`, `chooseLightTakesTheSunOrTheMoon`, `moonColorBlendsFromItsOwnColorToBlue`, `skyLightScalesOnlyTheKindItWasBuiltFor`, `scaledFogAndDaylightFollowTheLightScale`, `sunSkyIsDarkAtTheMoonlightSwitch`, and `builderBuildsAgainForTheOtherLight`; all `83` tests then passed.
- empireu took the sets `step2` (`21` shots), `night` (`50`), and `switch` (`76`), all settled, and found the switch, the ring, and the moon dogs fine. The pillars are faint next to the moon at brightness `0.025`, which looks too bright: the sky near it reads like clear mountain air.

## Remaining

- Choose `moonlightBrightness` from the `night` shots (the full moon's zenith near `0.10` sRGB), then test the moon's zenith and horizon display; empireu suggested a weather law, to decide after the measurement.
- The pillars' fade under a moonlit fog: keep it, or base it on the sun's part only.
- The moon shows through snow while the sun does not, since vanilla's sprite only fades by `1 - rainLevel` and ignores the ice; step 5 plans to dim the sprite by the ice, and whether both should show faintly is empireu's call.
- Not yet checked in game: snow at dusk (`FogModification`), relog at night, `/time set` across a phase change, F3+T, Esc while the moon fades, the frame rate, and the build times at the switch.
- `SkyBaselineHashGameTests` stays untracked until the review of this step is done.
