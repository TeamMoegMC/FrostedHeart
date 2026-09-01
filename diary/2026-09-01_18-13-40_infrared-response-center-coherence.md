# Infrared response-center coherence

- Time: `2026-09-01 18:13:40 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `InfraredViewRenderer request/response center ownership and tests`

## Completed

- Rejected non-full responses whose server-selected center differs from the
  currently installed texture center.
- Reused `snapshotAvailable` and `requestCenterValid` to force a full request on
  the next client tick after rejection.
- Made every accepted response update the existing requested-center baseline;
  a later player-center difference therefore forces a new full request.
- Added a private-state reflection test covering mismatched delta rejection,
  matching delta acceptance, and full response center takeover.
- Updated the infrared living documents and active plan.

## Decisions

- Kept server-side center selection and the existing response center fields.
- Added no center to C2S and no pending request, cache, observer, or texture
  state.

## Validation

- Focused `InfraredViewRendererStateTest`: passed.
- Production and test compilation: passed as part of the focused Gradle run.
- Complete selected thermal/infrared/render JUnit suite: `118/118` passed.
- `git diff --check`: passed.

## Remaining

- Live-check rapid center crossings on Vanilla and Oculus/Embeddium.
- Run the planned 100-client JFR/heap/network performance gate.
