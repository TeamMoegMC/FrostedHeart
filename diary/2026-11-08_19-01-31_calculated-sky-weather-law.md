# Calculated Sky Ice From The Weather

- Time: `2026-11-08 19:01:31 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`IceWeatherLaw`, `IceWeatherDepth`, `IceDepthGrid`, `CalculatedSkyScreenshots`), `WorldClimate`, `FHClimatePacket`, `ClientClimateData`, GameTests

## Completed

- The ice optical depth follows the weather law when `iceFollowsWeather` is on, the default ([Ice optical depth from the weather](../docs/climate/calculated-sky.md#ice-optical-depth-from-the-weather)), from the snow hours that `WorldClimate` searches `24` climate hours each way and `FHClimatePacket` carries to the client; `IceDepthGrid` holds `22` depths from `0` to `3`, the two around the drawn depth kept and blended.
- The harness has a variant key `iceFollowsWeather`; GameTests `IceWeatherGameTests` (the law, the grid, the blend, the ground under ice) and `SnowHistoryGameTests` cover it. The multiple scattering table scatters through diffuse totals of `SkyAtmosphere` with only `1 - g` of the delta scaled ice ([Forward scattering in the multiple scattering table](../docs/climate/calculated-sky.md#forward-scattering-in-the-multiple-scattering-table)).

## Decisions

- A DeepSeek session run by empireu fixed the failing GameTests first: a cone of `10 degrees` around the sun, left out of the blend test, hid a real failure of `2.39` display steps, so the test now leaves out only the sun's square widened by `0.5 degrees`, and the grid step went from `0.25` to `0.2`.
- empireu found too much light reaching the ground under thick ice and asked for a fix of the brightness only; scaling `G` and `S` as well, as first proposed, made every case too dark against the path tracer.
- Under a high sun the mod stays up to `11%` above the path tracer, which only knows a generic phase; with the mod's crystals the preview gives up to `8%` more and matches the mod within `0.3%`, so the gap comes from the oriented crystals; the GameTest allows `12%`.
- A gloomy day before snow would need lower and thicker clouds than ice at depth `3`, which empireu kept out of this change.

## Validation

- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky`: all `16` tests passed; the blend test measured `1.34` display steps at worst and the ground test `10.7%`.
- empireu checked the law in `runClient` with humidity `0` and snow from about `71000` ticks: depth `0.089` at `53000`, `2.996` at `75000`, and `0.046` at `105000`, `5` hours after the snow ended; all match the law.
- empireu took the set `v1` at eight times, at drawn depths `0.020` to `2.995` (`v1-2026-11-08_18-41-21` to `18-46-36`): against the runs before the multiple scattering change, the gray sky at sunrise and sunset under depth `3` is lighter and a high sun keeps some blue.

## Remaining

- Not yet checked in game: the recovery after snow watched live, a relog during it, and popping as the depth crosses grid depths.
- Under a high sun, thick ice brightens the ground by up to about `10%` over clear, from the oriented crystals; how much is real has not been traced.
- Lower clouds for gloom before snow, diamond dust, and aurorae are separate features; `CalculatedSkyScreenshots.ENABLED` goes to `false` before a pull request.
