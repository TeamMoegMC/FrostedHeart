# P2P Container Face Texture

- Time: `2026-08-24 15:56:58 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed`
- Scope: `shipping and receiving terminal block models`

## Change

- Changed the local-container face of the shipping and receiving terminals to use
  `frostedheart:block/warehouse`, matching the equivalent face of the warehouse interface.
- Kept the bidirectional terminal unchanged because it has no automatic adjacent-container connection face.
- Added a resource regression test for both sides of this distinction.

## Documentation Impact

- No living-system documentation update was needed; transfer behavior, orientation semantics, and lifecycle are unchanged.

## Validation

- `P2PTerminalResourcesTest`: 4 tests passed with zero failures, errors, or skips.
- All three terminal block-model JSON files parsed successfully.
- Scoped `git diff --check` passed.
