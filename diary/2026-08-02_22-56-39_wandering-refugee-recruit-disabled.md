# Wandering Refugee recruit button: disable without vacant housing

- Time: `2026-08-02 22:56:39 +0800`
- Author: `WorkBuddy coding agent`
- Status: `completed`
- Scope: `content/town/TeamTown`, `content/town/resident/WanderingRefugeeClientHelper`, `content/town/network/WanderingRefugeeRecruitMessage`, lang files (zh_cn/en_us)

## Completed

- `TeamTown#canAddResident()`: mirrors `TeamTownData.allocateHouse()` free-slot
  logic (any workable `HouseBuilding` with `residentsID.size() < maxResidents`),
  reads only `data.buildings`, safe on both client and server.
- `WanderingRefugeeClientHelper.openScreen()` now:
  - reads the client town snapshot
    (`CClientTeamDataManager...getData(FHSpecialDataTypes.TOWN_DATA)`, null-safe)
    and disables the recruit button initially when the town is full;
  - registers an `ITownDataUpdateListener` (buildings/residents changed) that
    re-evaluates the button state on incremental sync packets (new house,
    resident death frees a slot, etc.);
  - shows a "no vacant housing" tooltip only while the button is disabled via
    an anonymous `TextButton` subclass overriding `getTooltip`.
- Listener lifecycle: static field, previous listener removed on each
  `openScreen`; self-removes when `DialogueOverlay.INSTANCE.isEnabled()` turns
  false (dialogue closed). No changes to shared `DialogueScreen/DialogueOverlay`.
- `WanderingRefugeeRecruitMessage.handle`: added `else` branch sending
  `message.frostedheart.wandering_refugee.cannot_accommodate` when
  `town.addResident` fails; removed the redundant nested `{ }` block.
- Lang: 2 keys added in both zh_cn and en_us
  (`gui...recruit_disabled_no_housing`, `message...cannot_accommodate`).

## Decisions

- Reuse the same `addClientListener/removeClientListener` incremental-sync
  mechanism as the town GUI instead of building a new refresh path.
- Dynamic tooltip (override `getTooltip` checking `isEnabled()`) beats a static
  tooltip array: it disappears automatically when the state flips back.
- Server `addResident` stays authoritative; the client snapshot may be briefly
  stale, worst case is "clickable but fails -> cannot_accommodate message".

## Validation

- `gradlew.bat compileJava` (JDK 17.0.2, `-Xmx1536m`, `--no-daemon`): BUILD
  SUCCESSFUL; only pre-existing deprecation warnings (JEI compat classes).
- Both lang JSON files parse (python `json.load`).
- NOTE: plain bash `./gradlew` fails in this environment (`cygpath: command not
  found`, exec empty) - use PowerShell + `gradlew.bat` with
  `$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.2'`.

## Remaining

- In-game L2 verification needs a dev client (not runnable here): disabled
  state with full town, button re-enabling after building a house / resident
  death, tooltip visibility, and the failure message path.
- Changes are uncommitted on `master` (5 files); commit when user confirms.
