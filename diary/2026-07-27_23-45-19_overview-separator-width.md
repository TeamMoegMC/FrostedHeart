# Overview separator width

- Time: `2026-07-27 23:45:19 +0800`
- Author: `Codex /root; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `House overview, Warehouse overview, shared BuildingInfoElement`

## Completed

- Shortened the shared House/Warehouse overview separator by one glyph so automatic text wrapping keeps it on one visual line.

## Validation

- `git diff --check` passes.
- `./gradlew compileJava` passes.
