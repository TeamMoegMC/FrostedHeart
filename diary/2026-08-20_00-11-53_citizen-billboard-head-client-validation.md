# Citizen Billboard head client validation

- Time: `2026-08-20 00:11:53 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent, recording user validation`
- Status: `completed`
- Scope: `M3 Billboard head visual validation, instance metrics, and town living documentation`

## Completed

- Recorded the client confirmation that the separate Billboard body/head quads have acceptable far-distance appearance.
- Closed the Billboard head silhouette acceptance item with 715 active Billboard instances across seven skin batches.
- Recorded an observed `118,784 B` peak that exactly matches deletion plus recreation of all 1024 58-byte instances.

## Decisions

- Accept the Billboard head change without further geometry or UV changes.
- Keep Oculus compatibility for the final compatibility pass, as explicitly requested by the user.
- Keep GPU performance open: the reported backend hook timing excludes Flywheel's render-layer CPU work and GPU pass.

## Validation

- Client metrics: `cache=1024`, `detailed=0`, `batchFrustum=715`, `body=0`, `billboard=715`, `batchDraws=7`.
- Steady frame: `instanceDirtyBytes=0`; recorded peak: `peakInstanceDirtyBytes=118784`.
- Backend: `flywheel_m3_instancing`; benchmark: `moving:1024`; user visual result: Billboard head effect has no issue.

## Remaining

- Oculus compatibility and measured Flywheel GPU performance remain pending by design.
