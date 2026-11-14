package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.RenameBox;
import com.teammoeg.frostedheart.content.town.network.TownHousingPlanUpdatePacket;
import com.teammoeg.frostedheart.content.town.network.TownStaffingPlanUpdatePacket;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        // Residents are renamed on the seal's residents page in a one-shot box: Enter hands the name over once, losing
        // focus afterwards must not hand it over again, and losing focus before Enter cancels. While it is open the page
        // does not rebuild, so incoming resident updates cannot overwrite the draft.
        int[] done = {0}, cancelled = {0};
        String[] handed = {null};
        RenameBox enter = new RenameBox(parent, "Original Resident", 48, submitted -> { done[0]++; handed[0] = submitted; }, () -> cancelled[0]++);
        enter.setFocused(true);
        enter.setText("A Resident", false);
        enter.onEnterPressed();
        enter.setFocused(false);
        enter.tick();
        enter.onEnterPressed();
        require(done[0] == 1 && cancelled[0] == 0 && "A Resident".equals(handed[0]),
                "resident rename hands its name over once; blur after Enter must not resubmit");
        RenameBox blur = new RenameBox(parent, "Original Resident", 48, submitted -> done[0]++, () -> cancelled[0]++);
        blur.setFocused(true);
        blur.setText("Unsubmitted", false);
        blur.setFocused(false);
        blur.tick();
        blur.tick();
        require(done[0] == 1 && cancelled[0] == 1, "resident rename without Enter cancels once and submits nothing");
        return List.of("equal staffing reply after drag", "equal housing reply after drag",
                "town rename coalescing and blur", "equal town-name reply", "resident rename submits once",
                "resident rename blur cancels");
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
