# Calculated Sky Light Pillar Caps

- Time: `2026-11-14 20:02:27 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarClusters`, `LightPillarShelter`, `LightPillarSources`, `LightPillarWorldChanges`), GameTests. Branch `feature/pillar-fake`, uncommitted, after `c97399997`.

## Completed

- Each light now has its own cap, and only lights with the same cap merge, after `2026-11-14_17-49-28_calculated-sky-light-pillar-fake.md`; before, roofing the light a cluster read its cap at hid a whole glowstone floor's pillar (empireu, in game).
- `LightPillarClusters` joins two touching lights only when their caps are equal, so open lights, lights under a porch, and lights under a higher ceiling are separate clusters; each keeps the box of blocks its lights read their cap from, widened by `1`.
- A block change that stops light differently calls `queueSectionsReadingCapAt`, which queues the sections of the block's chunk column and of a beside column across a chunk border, down from the block's section, with a cluster whose box holds the block.
- GameTests `lightClustersSplitWhereTheirCapsDiffer` (glowstone under a porch roof, a ceiling, and open sky gives three clusters; torches under a stair roof stay alone) and `lightClustersQueueTheSectionsReadingAChangedCap`.

## Decisions

- empireu asked for the full solution over splitting by sky light alone, which would have left covered lights under ceilings of different heights sharing one cap.
- The shelter still reads its own cap per slot, the same value as its cluster's, since it rebuilds in the tick of a block change.
- A chunk column that unloads does not queue the sections beside it: its blocks did not change, and a walk would read the unloaded column as open.
- The box widens by `1` along both axes, so it also holds the `4` diagonal columns; changes there queue a walk that changes nothing.
- A Subordinate review, read only, found the per-column count, clearing the `LevelColumns` when indexing stops, and missing test cases. Not taken: queueing on unload.

## Validation

- Calculated sky GameTests: all `68` pass, before the review fixes and after, the `66` before and `2` new.
- In game, by empireu: a glowstone floor beside a stepped wooden roof looks right.

## Remaining

- Not yet seen in game: a floor from a porch into a higher ceiling, lowering a ceiling in a closed room, and the slot count under a stair roof.
- `LightPillarSources.walk` and the hooks are client only, so no GameTest walks a real section or calls them.
- A burst of block changes over lit floors, such as snow on a glowstone floor, queues their sections again, walked at `4` a tick.
- `docs/climate/calculated-sky.md` is not updated, as for the fake.
