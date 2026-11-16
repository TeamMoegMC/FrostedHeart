package com.teammoeg.frostedheart.content.town.render;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.network.TownRoadViewPacket;
import com.teammoeg.frostedheart.content.town.network.TownRoadViewRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Uses the existing zone-view key. Geometry is deduplicated once per received version. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class TownRoadRenderer {
    /** Road colours: linked to the tower, the largest group while none is, and every other group. */
    public static final int ROAD_TOWER = 0xaa44ddff, ROAD_LARGEST = 0xaa88ff88, ROAD_OTHER = 0xaaffbb44;
    /** Building controller colours, in the same order; red marks a building cut off from the group allowed to work. */
    public static final int BLOCK_TOWER = 0x5544ddff, BLOCK_LARGEST = 0x5588ff88, BLOCK_CUT_OFF = 0xaaff5555;
    private static final UUID UNKNOWN = new UUID(0, 0);
    private static UUID version = UNKNOWN, holder;
    private static ResourceLocation dimension;
    private static Map<BlockPos, Integer> floors = Map.of();
    private static Set<BlockPos> connected = Set.of(), working = Set.of();
    private static boolean hasView;
    private static int requestTicks;
    public static void reset() {
        version = UNKNOWN; holder = null; dimension = null; floors = Map.of(); connected = Set.of(); working = Set.of(); hasView = false; requestTicks = 0;
    }
    public static void accept(TownRoadViewPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !packet.dimension.equals(level.dimension().location())
                || !TownClientUpdates.isCurrentHolder(packet.holder)) return;
        Map<BlockPos, Integer> cells = new HashMap<>();
        packet.view.routes().forEach(route -> {
            int status = route.powered() ? 2 : packet.view.working().contains(route.from()) ? 1 : 0;
            route.steps().forEach(pos -> cells.merge(pos, status, Math::max));
        });
        floors = Map.copyOf(cells); connected = packet.view.connected(); working = packet.view.working();
        holder = packet.holder; version = packet.version; dimension = packet.dimension; hasView = true;
    }
    public static boolean matches(UUID id) { return hasView && java.util.Objects.equals(holder, id); }
    /** Whether the view still waits for the town's first road check: every finished check lets some group work. */
    public static boolean checking(com.teammoeg.frostedheart.content.town.TeamTown town) {
        return town.getTownBuildings().values().stream()
                .anyMatch(com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding::requiresRoad)
                && (!hasView || working.isEmpty());
    }
    public static boolean nearRoad(BlockPos pos) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (floors.containsKey(pos.offset(dx, dy, dz))) return true;
        return false;
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null) { reset(); return; }
        if (dimension != null && !dimension.equals(mc.level.dimension().location())) reset();
        if (!RoomZoneRenderer.visible && com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenCache.values().isEmpty()) return;
        if (TownClientUpdates.currentTown().isEmpty()) return;
        if (--requestTicks > 0) return;
        requestTicks = 40;
        FHNetwork.INSTANCE.sendToServer(new TownRoadViewRequestPacket(version));
    }
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS || !RoomZoneRenderer.visible || !hasView) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(dimension)) return;
        var town = TeamTownData.getClientTown().orElse(null);
        if (town == null) return;
        PoseStack pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        try (var shapes = TesselateHelper.getShape3DTesslator()) {
            var matrix = pose.last().pose();
            BlockPos center = event.getCamera().getBlockPosition();
            floors.forEach((pos, status) -> {
                if (pos.distSqr(center) <= 96 * 96)
                    RoomZoneRenderer.renderDoubleFloorFace(shapes, matrix, pos.getX(), pos.getY() + 0.09f, pos.getZ(),
                            status == 2 ? ROAD_TOWER : status == 1 ? ROAD_LARGEST : ROAD_OTHER);
            });
            town.getTownBuildings().forEach((pos, building) -> {
                // Buildings off the road network, such as mines, get no road verdict.
                if (building.requiresRoad() && pos.distSqr(center) <= 96 * 96)
                    shapes.cube(matrix, new AABB(pos).inflate(0.025), connected.contains(pos) ? BLOCK_TOWER : working.contains(pos) ? BLOCK_LARGEST : BLOCK_CUT_OFF);
            });
        } finally { pose.popPose(); }
    }
}
