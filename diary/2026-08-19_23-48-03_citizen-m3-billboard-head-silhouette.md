# Citizen M3 Billboard head silhouette

- Time: `2026-08-19 23:48:03 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `CitizenFlywheelModels, citizen.vert, M3 Billboard tests, and town rendering documentation`

## Completed

- Recorded the client result that the 68/72-block Body/Billboard ownership transition has no ghosting, empty frame, or jitter.
- Identified the remaining visible pop as silhouette discontinuity: the original four-vertex Billboard stretched only the torso-front UV over the full resident height and contained no head geometry.
- Replaced it with an eight-vertex shared mesh containing a 0.6-by-1.35-block body quad and a separate approximately 0.45-by-0.45-block head quad using the vanilla head-front UV.
- Added a distinct head `partId` so the sleeping shader flips the torso and head V regions independently.

## Decisions

- Keep Billboard as one instanced model and one draw batch per skin. Adding four shared static vertices is cheaper and simpler than adding another instancer or instance field.
- Preserve total 1.8-block standing height and the existing sleeping length transform, while narrowing the head silhouette relative to the body.
- Do not change the 58-byte instance stride, owner lifecycle, material batch count, dirty-byte accounting, or 68/72/96 distance thresholds.

## Validation

- The new regression test failed against the old model because no Billboard head part existed.
- Focused `FlywheelCitizenBackendTest` passed after the change, covering eight vertices, body/head tags, silhouette split, head UVs, and sleeping head/body UV flipping.

## Remaining

- Rebuild/restart the client, cross the 68/72-block boundary again, and confirm the added head makes the LOD change acceptably subtle in standing and sleeping scenes.
- Complete Embeddium/Oculus and JFR/RenderDoc validation before enabling M3 by default.
