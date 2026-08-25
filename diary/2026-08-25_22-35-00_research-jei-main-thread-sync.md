# Research JEI main-thread synchronization

- Time: `2026-08-25 22:35:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Frosted Research JEI visibility refresh after catalogue reload and result changes`

## Completed

- Corrected the remaining Design revoke visibility failure after inspecting the real client log.
- Routed every `ResearchJeiBridge#sync` request through the Minecraft client executor before calling JEI APIs.
- Kept the stable recipe-ID-to-JEI-registration mapping added in the preceding fix.
- Made idempotent grant/revoke of a known team result resend the full snapshot, so an administrator command also reconciles a stale client projection.
- Updated the living integration documentation with the client-main-thread requirement.

## Decisions

- `DistExecutor` remains only a physical-side guard. It is not treated as a thread switch; JEI mutations are always scheduled explicitly.

## Validation

- The prior failure was confirmed by JEI's runtime exception: visibility mutations were invoked from `Server thread` and rejected because JEI requires the client main thread.
- `./gradlew compileJava` passed.

## Remaining

- Recheck `frostedresearch_test:smoke_design` after restarting the client with this build.
