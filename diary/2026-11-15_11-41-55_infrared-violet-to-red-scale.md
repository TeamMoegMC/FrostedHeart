# Infrared colour scale: violet to red over −50..50 °C, comfortable temperatures green

- Time: `2026-11-15 11:41:55 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `implemented; shader compiled against the local OpenGL driver; not checked in game`
- Scope: `src/main/resources/assets/frostedheart/shaders/infrared_view.fsh`, `InfraredPhysicalE2eClient`,
  `InfraredE2eClient`, infrared and world-climate documentation
- Follows: [infrared natural base](2026-11-15_10-48-51_infrared-natural-base.md)

## Why

The old scale ran −20..20 °C through blue → orange → yellow → red. Its hue turned back, so −6.8 °C and +13.2 °C were
the same orange, and with the natural base most of the world (about −50 to −10 °C) fell into the first third, whose
middle is a greyish purple. The owner asked for violet through red over −50..50 °C, with comfortable temperatures
(20–30 °C) roughly green.

## Completed

- `temperatureToColor` interpolates linearly between stops: violet (0.5, 0, 1) at −50, blue at −30, cyan at 0, green
  at 25, yellow at 37.5, red at 50 °C; outside the range it keeps the end colours. Neighbouring stops are one 60° hue
  sector apart, so each linear mix is a hue sweep: the hue is monotonic and saturation constant. 20 °C is
  (0, 1, 0.2) and 30 °C (0.4, 1, 0), both green. INVALID still takes the bottom of the scale, now violet. The 0.43
  premultiplied blend is unchanged.
- A single even hue sweep would have put 25 °C on yellow, so green is pinned at 25 °C and the cold side gets three of
  the five segments.
- `InfraredPhysicalE2eClient.color` follows the new scale and compares the heated frame against the violet
  placeholder (`placeholderError`, previously `bluePlaceholderError`, not read by the script). `InfraredE2eClient`
  counts heat as `r > b + 64 && r >= g`, because 40 °C is now amber (1, 0.8, 0), which the old `r > g + 64` test
  missed.

## Validation

- The shader compiles in an offscreen OpenGL 3.2 core context on this machine (GL 4.1 Metal, GLSL 4.10), using a small
  C checker in the session scratchpad; the same checker rejects a copy with a deliberate syntax error.
- `compileJava compileGameTestJava` passes. No GameTest covers shader colours.
- The real-client E2E scripts (`Scripts/run-infrared-network-e2e.py`) were not run.

## Documentation

- `docs/climate/infrared-view.md`: new "色标" section with the stops; the "blue" section is now about violet; the
  diagnostic table uses −50. The 2026-11-08 validation records keep their blue wording, as written then.
- `docs/climate/world-climate-and-temperature.md`: placeholder colour and range in the infrared paragraphs.

## Remaining

- Not checked in game. A 43 % violet or blue overlay is dark; brightness was not tuned.
- Entities and other pixels without a value are violet, as they were blue before.
