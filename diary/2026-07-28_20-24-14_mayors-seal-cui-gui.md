# Mayor's Seal GUI rebuilt on the CUI framework with town statistics

- Time: `2026-07-28 20:24:14 +0800`
- Author: `Kimi-K3 coding agent`
- Status: `completed`
- Scope: `item/townmanager`, `content/town` (data), lang files

## Completed

- Rewrote `item/townmanager/TownManagerScreen` from a vanilla `Screen` (with
  legacy `chorda.client.widget` buttons and todo-ridden placeholder modes) to a
  Chorda CUI `PrimaryLayer`, visually matching town building GUIs:
  `townworkerblock.png` 176x222 frame + left-side `TabImageButtonElement` tabs.
- Four tabs (`TownManagerTab` subclasses, opened client-only via
  `CUIScreenWrapper.open`, no container menu):
  - Town Overview (`TownOverviewTab`, reuses `tabs/TownInfoPanel`): town name,
    population, building/workable counts, average health/mental,
    homeless/unemployed counts, day-over-day deltas from history.
  - Residents (`TownResidentsTab` + `TownResidentsPanel`): scrollable resident
    list + detail (attributes, education, house/work assignment with localized
    building names, work proficiencies). Mirrors `TownWorkforcePanel` layout.
  - Town Buildings (`TownBuildingsTab` + `TownBuildingsPanel`): building list
    (unworkable shown red) + detail (type, coordinates, workable state with
    failure reasons, resident capacity via `ITownResidentBuilding`).
  - Statistics (`TownStatisticsTab` + `TownStatisticsPanel`): three line
    charts (population auto-scaled; avg health/mental fixed 0-100) with latest
    value + delta, middle reference line, scale labels, collecting hint when
    history has fewer than 2 entries.
- Added `content/town/TownHistoryEntry` (record + Codec): daily snapshot
  (day, population, avgHealth, avgMental, buildings). `TeamTownData` now keeps
  up to 30 entries (`MAX_HISTORY_ENTRIES`), recorded at the end of
  `tickMorning` (same-day settlements overwrite), persisted through the
  existing CODEC field `history` and synced to clients by the existing
  per-tick `TeamTownDataS2CPacket` full sync. `TeamTown#getHistory` exposes it.
- `TownManagerClientHelper.openScreen()` now opens via `CUIScreenWrapper`.
- Added `gui.frostedheart.town_manager.*` keys (zh_cn + en_us), including
  per-building-type names under `...town_manager.building.*` with
  `translatableWithFallback` fallback to the class simple name.

## Decisions

- History rides the existing full-data sync instead of a new packet: 30 small
  entries are negligible and no sync cadence changes were needed.
- Item GUI stays client-only (no Menu/NetworkHooks) because the seal is a
  read-only observer; this matches how the old screen and EditUtils work.
- Panels read fresh data through `Supplier<TeamTown>/Supplier<TeamTownData>`
  every render, so the GUI follows sync updates live; selection is normalized
  by UUID/BlockPos when entries disappear.
- Old `town_manage_screen.png` texture is now unreferenced but left in the
  asset tree untouched.

## Validation

- `JAVA_HOME='C:\Program Files\Java\jdk-17' ./gradlew build --offline` passed.
  NOTE: system JAVA_HOME points to JDK 11 and makes Gradle worker daemons
  crash with `GradleWorkerMain` ClassNotFoundException; always set JDK 17.
- Both lang JSON files parse; `git diff --check` clean.
- No references to the removed old screen API remain.

## Remaining

- Not run in game: verify tab hit areas, scrollbar feel, chart readability,
  and text widths at common GUI scales.
- Statistics need two daily settlements before charts appear (by design).
- Pre-existing issue noticed but untouched: `TeamTownData` codec constructor
  ignores the decoded `labour`/`maxLabour` (assigns 0), so labour values reset
  on save reload.

## Follow-up: HouseBuilding resident count fix (20:51)

`TownBuildingsPanel` initially used `ITownResidentBuilding.getResidentsID()` to
display resident counts. HouseBuilding's CODEC does not serialize
`residentsUUID` (only `maxResident`; the `HouseMenu` works around this by
filtering `Resident.housePos`). This caused houses to always show 0/X on the
client. Fixed by counting residents in the position-based way:

```
boolean isHouse = !(building instanceof ITownResidentWorkBuilding);
for (Resident r : town.getAllResidents())
    if (pos.equals(isHouse ? r.getHousePos() : r.getWorkPos()))
        count++;
```
