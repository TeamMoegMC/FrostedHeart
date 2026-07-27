# Town GUI text wrapping

- Time: `2026-07-27 23:25:34 +0800`
- Author: `Codex /root; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town GUI shared text layout, information panels, resident/workforce panels`

## Completed

- Added `TownTextLayout` as the shared rule set for wrapping styled `Component` text and truncating selector labels with an explicit ellipsis.
- Replaced `TownInfoPanel`'s silent `plainSubstrByWidth` clipping with Minecraft's styled `Font.split` wrapping.
- Flattened logical information rows into visual rows before rendering and scrollbar calculation, so wrapped continuations can be reached by wheel or scrollbar drag.
- Kept an item icon only on the first visual line while aligning every continuation line with the first line's text.
- Applied the same visual-line wrapping and scroll calculations to `TownWorkforcePanel`, `HouseResidentPanel`, and legacy `BuildingInfoElement`.
- Kept resident selector names single-line and changed their truncation to a visible `…`.
- Preserved explicit blank rows and rich component styles during wrapping.

## Decisions

- Information panels always reserve scrollbar width during layout. This keeps wrapping stable when content changes between fitting and overflowing.
- Selector rows remain fixed-height and do not wrap because wrapping names would break selection geometry.
- Wrapping operates on `Component` rather than `Component#getString`, preserving translated content and nested styles.

## Validation

- A repository search confirms `plainSubstrByWidth` remains only inside the intentional ellipsis helper.
- `git diff --check` passes.
- `./gradlew compileJava` passes.
- `./gradlew build` passes. The repository-wide license-report task continues to emit its existing non-fatal list.

## Remaining

- Visually verify Chinese punctuation wrapping and long English localization at the supported GUI scales.
- Confirm wheel and scrollbar movement feel natural when one logical row expands to several visual lines.
