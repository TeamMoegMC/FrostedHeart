# Polar bear clothing fits lining slots

- Time: `2026-11-04 17:18:10 +02:00`
- Author: `empireu; Anthropic Claude Opus 5.5; outside contributor`
- Status: `completed`
- Scope: `FHItems` polar bear armor, `frostedheart:inner_linning` item tag

## Completed

- Added `FHTags.Items.INNER_LINNING` to `polar_bear_fur_socks`, `polar_bear_fur_hat`, `polar_bear_fur_jacket`, and `polar_bear_fur_pants`, and regenerated `inner_linning.json`. This answers [issue #169](https://github.com/TeamMoegMC/TheWinterRescue/issues/169).
- Documentation impact: none. No living document describes the lining tag or polar bear clothing.

## Decisions

- Treated the missing tags as an oversight. The pack's `lining_upgrade` research unlocks all five polar bear recipes, and every other clothing set has the tag. No design document says this set is outer-only. The gloves are not evidence either way, because hands have no armor slot and gloves need the tag to be worn at all.
- Kept the existing polar bear values. This shifts balance, because the set is the warmest by far. By `BodyPartData` weights, the warmest torso goes from 540 (polar bear armor and three wool linings) to 900 (four polar bear pieces). These are calculated, not measured. If outer-only is intended, revert this and add the tooltip the issue suggests.
- Left unrelated `runData` output out of the change. On `61eddf706` it also rewrites 30 generated files, and parsed JSON comparison shows no content difference (key order and one trailing newline only).

## Validation

- Before the change, in `runClient` started from the Gradle task: lining slots refused the four pieces, the torso armor slot accepted the jacket, and lining slots accepted the polar bear gloves and fox jacket.
- `runData` succeeded. `inner_linning.json` gained exactly the four entries. `git diff --check` is clean.
- After the change: the first in-game run crashed when the linings passed weight 10. This is an existing Heavy Load bug that this tag only makes easier to reach, and [its own entry](2026-11-04_17-50-16_heavy-load-first-crossing-crash.md) covers it.
- After the change, with the Heavy Load fix in place: lining slots accepted the four pieces on their own body part and refused the jacket on the head. The torso armor slot still accepted the jacket, and lining slots still accepted the polar bear gloves and fox jacket. Linings survived a save and reload, and the wardrobe behaved the same.

## Remaining

- This change should land after the Heavy Load fix, because a polar bear jacket, pants and hat in the linings already weigh 10.8.
- The balance effect has not been playtested. The change has not been tested in the full pack.
