# House GUI inventory and scrolling

- Time: 2026-07-27 21:03:06 +0800
- Author: Codex `/root`

## Context

The first House GUI iteration used the whole container background for town information, which covered the player's inventory. Dense overview and resident information also had no usable scrolling, and several labels exposed intermediate model details rather than the values useful to players.

## Changes

- Restored the player's inventory and hotbar to the House menu at the same positions used by the warehouse screen.
- Reduced both House information panels to the upper 130 pixels so they no longer overlap the inventory area.
- Added a right-side vertical scrollbar to the overview and resident-detail panels. Both support mouse-wheel scrolling, track clicks, and thumb dragging.
- Kept the resident selector independently scrollable on the left.
- Simplified resident details:
  - Four attributes display as rounded integers out of 100.
  - Mining and hunting proficiency display as rounded integers out of 100.
  - The next settlement forecast displays only the resulting health and mental deltas.
- Expanded the overview's non-workable state into specific unmet conditions:
  - not initialized;
  - occupied town space overlap;
  - invalid structure;
  - insufficient area;
  - insufficient volume;
  - temperature outside the valid working range, including the required range.
- Simplified the latest settlement section:
  - removed the duplicate settled-resident count;
  - renamed food to food consumption;
  - removed the redundant nutrition recovery multiplier.
- Added a qualitative label after effective temperature: too low, comfortable, or too high.

## Decisions

- Temperature workability and temperature comfort remain separate concepts. Workability uses the hard valid range configured by the House model. The descriptive label uses the point where the temperature comfort rating reaches 50%, so a building may remain operational while still being described as too cold or too hot.
- No incremental synchronization or new packet was added. The screen continues to consume the full town data already synchronized every tick.

## Validation

- `./gradlew compileJava --offline` passed.
- `./gradlew build --offline` passed.
- Both edited localization JSON files passed `jq empty`.
- `git diff --check` passed.
- Removed localization keys have no remaining source references.

## Remaining work

- Visually verify the upper-panel height, scrollbar hit areas, and text density in game at the GUI scales commonly used by players.
