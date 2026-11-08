# Team and town persistence architecture design

- Time: `2026-10-19 18:44:30 +08:00`
- Author: `Codex; OpenAI; primary architecture analysis`
- Status: `completed`（design and source investigation; implementation remains planned）
- Scope: `Chorda team persistence, town simulation ownership, snapshot lifecycle, loading and migration design`

## Completed

- Added the [proposed architecture and implementation plan](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则).
- Verified current synchronous full team saves, globally dirty simulation saves, eager team decoding, deferred initialization events, and independent network dirty queues.
- Identified repeated linear UUID lookup during batch resident restoration: identity matching is worst-case quadratic. Proposed a temporary UUID-to-session-ID map with the existing ID-to-slot index to handle swap-remove correctly.
- Verified that staffing/housing getters normalize persistent plans, several components expose live CompoundTags, and mutation tracking must include nested state and lifecycle changes.
- Checked local Forge/Minecraft source: manual save-all passes forced=true; stop saves can pass flush=true with forced=false. An asynchronous writer must propagate both contexts and drain without depending on another tick.

## Decisions

- Retain main-thread gameplay authority, existing town/resident models, NBT, and the native save boundary. Use component revisions, frozen encoded components, owner-scoped records, and one background writer.
- Propose combining a player's existing team record with that town's simulation snapshot; AI towns retain independent identity and per-town files. TownSimData stays a team-independent runtime object.
- Keep initial snapshot capture at the same native save boundary. Staggering authoritative snapshots across ticks would introduce additional timing differences for cross-town transfers.
- Treat true on-demand disk loading, eviction, finer record formats, and off-thread Codec conversion as later measured extensions.
- Documentation impact: proposed behavior belongs in plans. No living behavior document or read-only design document was modified.

## Validation

- Read the current code, relevant town/research/knowledge documentation and prior persistence/lifecycle diaries.
- Checked native save semantics against the existing Forge 1.20.1 source artifact in build/fg_cache.
- Reused the prior turn's read-only CT2 inspection: small empty-town samples do not establish large-population performance.
- This work changes documentation only; no game build, game execution, or save mutation was needed. All relative Markdown links resolve, both UTF-8 files have balanced code fences, and no trailing whitespace was found.

## Remaining

- Execute P0-P4 only as implementation work: baseline real populated towns, complete mutation coverage, implement/version the writer and migration, validate restart/lifecycle behavior, then update living documentation.
- No performance gain or production capacity is claimed by this design-only result.
