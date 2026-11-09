package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.network.TownHousingPlanUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownStaffingPlanUpdatePacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/** Controlled component transitions inside the opt-in rendering client; no extra network protocol. */
public final class TownEditorClientChecks {
    private TownEditorClientChecks() {}

    public static List<String> verify(UILayer parent) throws Exception {
        TeamTownData data = new TeamTownData(null);
        TeamTown town = data.createTeamTown();
        BlockPos job = new BlockPos(0, 65, 0), home = job.offset(10, 0, 0);
        data.applyBuildingUpdate(Map.of(job, new HuntingBaseBuilding(job), home, new HouseBuilding(home)), Set.of());
        data.applyStaffingPlan(new TownStaffingPlan(List.of(new TownStaffingPlan.Entry(job, 2))));
        data.applyHousingPlan(new TownHousingPlan(List.of(new TownHousingPlan.Entry(home, 2))));

        TownStaffingPanel staffing = new TownStaffingPanel(parent, 0, 0, () -> town);
        call(staffing, "syncDraft", TeamTown.class, town);
        targets(staffing, "draftTargets").put(job, 4);
        call(staffing, "syncDraft", TeamTown.class, town);
        require(targets(staffing, "draftTargets").get(job) == 4, "retain the optimistic staffing draft before any reply");
        field(staffing, "targetBuilding", job);
        FriendlyByteBuf staffWire = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new TownStaffingPlanUpdatePacket(data.getStaffingPlan()).encode(staffWire);
            data.applyStaffingPlan(new TownStaffingPlanUpdatePacket(staffWire).plan());
        } finally { staffWire.release(); }
        call(staffing, "syncDraft", TeamTown.class, town);
        require(targets(staffing, "draftTargets").get(job) == 4, "defer a staffing reply until the current gesture ends");
        field(staffing, "targetBuilding", null);
        call(staffing, "syncDraft", TeamTown.class, town);
        require(targets(staffing, "draftTargets").get(job) == 2, "equal authoritative staffing reply must roll four back to two");

        TownHousingPanel housing = new TownHousingPanel(parent, 0, 0, () -> town);
        call(housing, "sync", TeamTown.class, town);
        targets(housing, "draftGuarantees").put(home, 4);
        field(housing, "editingTarget", home);
        FriendlyByteBuf houseWire = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new TownHousingPlanUpdatePacket(data.getHousingPlan()).encode(houseWire);
            data.applyHousingPlan(new TownHousingPlanUpdatePacket(houseWire).plan());
        } finally { houseWire.release(); }
        call(housing, "sync", TeamTown.class, town);
        require(targets(housing, "draftGuarantees").get(home) == 4, "defer a housing reply until the current gesture ends");
        field(housing, "editingTarget", null);
        call(housing, "sync", TeamTown.class, town);
        require(targets(housing, "draftGuarantees").get(home) == 2, "equal authoritative housing reply must roll four back to two");

        data.applyNameUpdate("Original");
        TownNameEditor name = new TownNameEditor(parent, () -> town);
        call(name, "syncAuthoritativeName");
        submitLocally(name, "A");
        call(name, "syncAuthoritativeName");
        require(name.getText().equals("A"), "retain the optimistic town name until a name reply arrives");
        long submittedAt = (long) field(name, "pendingNameUpdateId");
        data.applyNameUpdate("A");
        data.applyNameUpdate("B");
        call(name, "commit", boolean.class, false);
        require((long) field(name, "pendingNameUpdateId") == submittedAt,
                "blur after Enter must not resubmit A over the later authoritative B");
        call(name, "syncAuthoritativeName");
        require(name.getText().equals("B") && field(name, "pendingName") == null,
                "two name packets before a render must settle on B and clear the pending A");
        submitLocally(name, "A");
        data.applyNameUpdate("B");
        call(name, "syncAuthoritativeName");
        require(name.getText().equals("B") && field(name, "pendingName") == null,
                "an equal-name authoritative reply must also end the wait");

        UUID id = UUID.randomUUID();
        Resident original = new Resident("Original", "Resident", id);
        data.applyResidentUpdate(Map.of(id, original), Set.of());
        TownResidentsPanel residents = new TownResidentsPanel(parent, 0, 0, () -> town);
        call(residents, "syncNameEditors", Resident.class, original);
        TextBox first = (TextBox) field(residents, "firstNameBox");
        first.setText("A", false);
        field(residents, "displayedNameKey", id + "\u0000A\u0000Resident");
        field(residents, "pendingNameResident", original);
        Resident latest = new Resident("B", "Resident", id);
        data.applyResidentUpdate(Map.of(id, latest), Set.of());
        call(residents, "commitResidentName", boolean.class, false);
        require(field(residents, "pendingNameResident") == original,
                "resident blur after Enter must not submit A over B");
        call(residents, "syncNameEditors", Resident.class, latest);
        require(first.getText().equals("B") && field(residents, "pendingNameResident") == null,
                "a coalesced resident update must clear the pending intermediate name");

        first.setText("A", false);
        field(residents, "displayedNameKey", id + "\u0000A\u0000Resident");
        field(residents, "pendingNameResident", latest);
        Resident other = new Resident("Other", "Resident", UUID.randomUUID());
        data.applyResidentUpdate(Map.of(other.getUUID(), other), Set.of());
        call(residents, "syncNameEditors", Resident.class, latest);
        require(first.getText().equals("A"), "another resident's update must not acknowledge this pending edit");
        Resident equalReply = new Resident("B", "Resident", id);
        data.applyResidentUpdate(Map.of(id, equalReply), Set.of());
        call(residents, "syncNameEditors", Resident.class, equalReply);
        require(first.getText().equals("B") && field(residents, "pendingNameResident") == null,
                "an equal-name replacement snapshot must clear the resident draft");
        first.setText("Unsubmitted", false);
        field(first, "focusedLastFrame", true);
        call(residents, "syncNameEditors", Resident.class, equalReply);
        require(first.getText().equals("Unsubmitted"), "background sync must preserve text until child focus-loss commit");
        field(first, "focusedLastFrame", false);
        return List.of("equal staffing reply after drag", "equal housing reply after drag",
                "town rename coalescing and blur", "equal town-name reply", "resident rename coalescing and blur",
                "resident reply identity and unrelated updates", "resident focus-loss ordering");
    }

    private static void submitLocally(TownNameEditor editor, String name) throws Exception {
        editor.setText(name, false);
        field(editor, "cleanName", name);
        field(editor, "pendingName", name);
        field(editor, "pendingNameUpdateId", TownClientUpdates.getNameUpdateId());
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, Integer> targets(Object panel, String name) throws Exception {
        return (Map<BlockPos, Integer>) field(panel, name);
    }

    private static Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void field(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void call(Object target, String name) throws Exception {
        var method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void call(Object target, String name, Class<?> type, Object value) throws Exception {
        var method = target.getClass().getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
