# Town manager wide layout

- Time: `2026-08-14 20:42:46 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Mayor's Seal frame and town-manager tab layouts`

## Completed

- Expanded the Mayor's Seal frame from `176x222` to `264x222`, increasing the inner content width from 160 to 248 pixels.
- Split the original background into fixed left/right borders and a horizontally stretched center so the frame edges and tab texture are not distorted.
- Expanded the building and resident list columns from 62 to 96 pixels, giving both names and right-side details more room.
- Expanded the staffing building-name area and capacity/target bar to use the new width; overview, events, and statistics already derive their layout from the shared content width.

## Decisions

- Keep the existing height and tab geometry because the reported problem is horizontal text pressure and the screen does not need an inventory area.
- Use exactly 1.5 times the original frame width as requested, without creating or modifying the texture asset.

## Validation

- `./gradlew compileJava processResources` passed with existing repository warnings only.
- `./gradlew test` passed.
- `git diff --check` passed before adding this entry.

## Remaining

- Reopen all six Mayor's Seal tabs in game and visually verify the widened background seams, long English labels, resident name editors, and staffing drag/target hit areas.
