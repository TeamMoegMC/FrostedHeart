# Calculated Sky Light Pillar Caps

- Time: `2026-11-14 20:02:27 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarClusters`, `LightPillarShelter`, `LightPillarSources`, `LightPillarWorldChanges`, `LightPillarWorkQueue`), GameTests. Branch `feature/pillar-fake`, uncommitted, after `c97399997`.

## Completed

Each light now has its own cap, and only lights with the same cap merge, after `2026-11-14_17-49-28_calculated-sky-light-pillar-fake.md`. Before, a cluster read its cap at one light, so roofing that light of a glowstone floor hid the pillar of the whole floor (empireu, in game).

- `LightPillarShelter.readLightCap` holds the cap rule: the light's own column's cap, or for a full block whose own cap is `ceil(top)` the highest of its `4` side columns' caps. `LightPillarShelter.build` and the scan both use it.
- `LightPillarSources.walk` reads each light's cap with its own `LevelColumns`, set to the level for the sections walked in a tick, and passes it to `LightPillarClusters.addLight`. `/calculatedSky` prints the blocks read for caps in the last second.
- `LightPillarClusters` joins two touching lights only when their caps are equal, so open lights, lights under a porch, and lights under a higher ceiling become separate clusters, and a stair roof gives a cluster for each strip of lights under one ceiling height. Each cluster keeps the box of blocks its lights read their cap from: their columns, widened by `1` for a cluster with a full block, from the lowest `ceil(top)` up to the cap's block, or without end when open.
- A block change that stops light differently (`LightPillarOccluders.stopsTheSame` false) calls `LightPillarClusters.queueSectionsReadingCapAt` from `LightPillarWorldChanges.onBlockChanged`. It looks at the sections of the block's chunk column, and of a chunk column beside it where a side column crosses a chunk border, from the block's section down, and queues each section with a cluster whose box holds the block. A count of clusters per chunk column skips columns without clusters after one lookup.
- When a chunk column gets its light, `queueSectionsReadingCapsIn` queues the sections beside it whose boxes reach into it, since their full blocks read an unloaded side column as open. A tags update queues every section with clusters through `queueEverySection`.
- `LightPillarWorkQueue.queueSection` is public, and the queue reports the level's lowest section and section count. `LightPillarSources.stopIndexing` clears the level of both `LevelColumns`.
- `/calculatedSky` world changes line counts the sections queued for caps.
- GameTests:
  - `lightClustersSplitWhereTheirCapsDiffer`: a row of glowstone under a porch roof, a ceiling, and open sky gives three clusters with their light counts and middles, and torches under a stair roof stay alone.
  - `lightClustersQueueTheSectionsReadingAChangedCap`: changes between a torch and its roof, at the roof, above it, at the torch, beside it, in the section above below a roof, far above an open torch, in side columns of glowstone across the east and south chunk borders, at and above the cap, around a cluster of two glowstone blocks, after a section was removed, and while not tracking; a chunk column beside the glowstone getting its light; and queueing every section.
  - The existing calls of `addLight` pass `OPEN_CAP`.

## Decisions

- empireu asked for the full solution over splitting by sky light alone, which would have left covered lights under ceilings of different heights sharing one cap.
- The shelter still reads its own cap per slot, the same value as its cluster's, because it rebuilds in the tick of a block change while a section waits for its walk.
- A chunk column that unloads does not queue the sections beside it. Its blocks did not change, so their caps stay right, and a walk would read the unloaded column as open.
- The box widens by `1` along both axes, so it also holds the 4 diagonal columns, which no cap reads. Changes there only queue a walk that changes nothing.
- Found in review, a Subordinate session, read only: the per-column count, clearing the `LevelColumns` when indexing stops, a wrong message in the stair test, and the missing test cases for a cluster of two lights and the south chunk border. Not taken: queueing on unload, as above.

## Validation

- `bash agent-tools/build.sh` passes.
- `bash agent-tools/gametest.sh frostedheart_calculated_sky`: all `68` pass before the review fixes and again after them, the `66` before and `2` new.
- In game, by empireu: a glowstone floor beside a stepped wooden roof, before and after, looks as expected.

## Remaining

- Not yet seen in game: a floor running from a porch into a higher ceiling, lowering a ceiling inside a closed room, and the slot count under a stair roof with `/calculatedSky pillars`.
- `LightPillarSources.walk` and the hooks are client only, so no GameTest walks a real section or calls the hooks. The tests call the cluster queries directly.
- A burst of block changes over lit floors, such as snow settling on an open glowstone floor, queues their sections again, walked at `4` sections a tick.
- `docs/climate/calculated-sky.md` is not updated, as for the fake as a whole.
