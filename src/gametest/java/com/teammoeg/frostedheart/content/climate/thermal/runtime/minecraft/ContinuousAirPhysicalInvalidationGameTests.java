/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual state changes, direct/routed gameplay reads, and the production chunk-save payload. */
@GameTestHolder("frostedheart_physical_invalidation")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ContinuousAirPhysicalInvalidationGameTests {
    private static final class SaveCheck {
        final GameTestHelper helper;
        final BlockPos material;
        final int stateId;
        volatile boolean observed;
        SaveCheck(GameTestHelper helper, BlockPos material, int stateId) {
            this.helper = helper; this.material = material; this.stateId = stateId;
        }
    }
    private static volatile SaveCheck saving;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void saved(ChunkDataEvent.Save event) {
        var check = saving;
        if (check == null || event.getLevel() != check.helper.getLevel()
                || !event.getChunk().getPos().equals(new net.minecraft.world.level.ChunkPos(check.material))) return;
        var chunk = event.getChunk();
        var state = DormantChunkThermalState.decode(event.getData(), chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
        check.helper.assertTrue(state != null, "Real native save contains thermal data");
        var sample = new MaterialSample();
        int block = (check.material.getX() & 15) | (check.material.getZ() & 15) << 4 | (check.material.getY() & 15) << 8;
        check.helper.assertTrue(state.readMaterial(check.material.getY() >> 4, block, check.stateId, sample)
                        == DormantChunkThermalState.MaterialRead.MATCH && Double.isFinite(sample.enthalpyJ()),
                "The outgoing v6 save must retain the new state ID and its material energy");
        check.observed = true;
        saving = null;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1800)
    public static void equivalentStateKeepsQueriesWhileRealReplacementInvalidates(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos passage = fire.above(), direct = fire.above(2), material = fire.east(2);
        level.setBlockAndUpdate(passage, Blocks.STONE_SLAB.defaultBlockState());
        level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState());
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var air = new ThermalEnvironmentSample();
        var route = new ThermalEnvironmentSample();
        var matter = new MaterialSample();
        var raw = new QueryPublication.MutableSample();
        long[] changedRevision = {0};
        SaveCheck[] written = {null};
        var next = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            sample(level, direct, air); sample(level, passage, route);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(air) && route.airAvailable() && route.sharedAirRegion(),
                    "Warm actual room and slab route before checking invalidation");
            helper.assertTrue(page(helper, material).currentPublication() != null
                            && MinecraftThermalInput.sampleMaterial(level, material, matter)
                            && matter.source() == MaterialSample.Source.LIVE,
                    "Actual log must have a complete live material publication");
        }).thenExecute(() -> {
            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
            var page = page(helper, material);
            var profiles = MinecraftThermalProfiles.prepare();
            helper.assertTrue(profiles.signatures().samePhysicalProperties(
                            profiles.states().signatureId(level.getBlockState(material)), profiles.states().signatureId(next)),
                    "This real axis change preserves the configured physical properties");
            // Prevent a worker publication during these short synchronous assertions, never while waiting.
            synchronized (publication) {
                long revision = page.liveGeometryRevision(), physical = page.lastPhysicalGeometryRevision();
                helper.assertTrue(publication.tryReadAir(direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, raw),
                        "Strict direct query exists before the world change");
                double before = raw.temperatureC();
                level.setBlockAndUpdate(material, next);
                changedRevision[0] = page.liveGeometryRevision();
                helper.assertTrue(changedRevision[0] > revision && page.currentPublication() == null,
                        "Full state capture is still required and cannot publish under the held monitor");
                helper.assertTrue(page.lastPhysicalGeometryRevision() == physical
                                && publication.tryReadAir(direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, raw)
                                && raw.temperatureC() == before,
                        "An equivalent state update keeps the same actual strict Air field readable");
                sample(level, passage, route);
                helper.assertTrue(route.airAvailable() && route.sharedAirRegion(),
                        "Gameplay route remains readable while the state-only capture is pending");
            }
        }).thenWaitUntil(() -> {
            var current = page(helper, material).currentPublication();
            helper.assertTrue(current != null && current.geometryRevision() >= changedRevision[0]
                            && MinecraftThermalInput.sampleMaterial(level, material, matter)
                            && matter.source() == MaterialSample.Source.LIVE,
                    "State metadata still reaches a full material publication");
        }).thenExecute(() -> {
            written[0] = new SaveCheck(helper, material, Block.BLOCK_STATE_REGISTRY.getId(next));
            saving = written[0];
            level.getChunkAt(material).setUnsaved(true);
            level.getServer().saveEverything(true, true, true);
        }).thenWaitUntil(() -> helper.assertTrue(written[0].observed, "Observe the actual native save payload"))
                .thenExecute(() -> {
            var publication = MinecraftThermalInput.queryPublicationForObservation(level);
            var page = page(helper, material);
            synchronized (publication) {
                long physical = page.lastPhysicalGeometryRevision();
                level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState());
                helper.assertTrue(page.lastPhysicalGeometryRevision() == physical,
                        "First change in the pending position remains metadata-only");
                level.setBlockAndUpdate(material, Blocks.AIR.defaultBlockState());
                helper.assertTrue(page.lastPhysicalGeometryRevision() > physical
                                && !publication.tryReadAir(direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, raw),
                        "Removing that same pending material must invalidate the old strict Air field");
                sample(level, passage, route);
                helper.assertTrue(!route.sharedAirRegion(), "The stale passage route must also stop serving live Air");
            }
        }).thenWaitUntil(() -> {
            sample(level, direct, air); sample(level, passage, route);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(air) && route.sharedAirRegion()
                            && MinecraftThermalInput.describeAirRead(level, direct).startsWith("geometry_current"),
                    "Real geometry replacement republishes both direct and routed gameplay reads");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_PHYSICAL_INVALIDATION_RESULT,stateOnlyCaptureRevision={},nativeSaveObserved={},directTick={},routeTick={}",
                    changedRevision[0], written[0].observed, air.airSampleTick(), route.airSampleTick());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenSucceed();
    }

    private static ThermalPageHandle page(GameTestHelper helper, BlockPos position) {
        var chunk = helper.getLevel().getChunkAt(position);
        var attachment = (MinecraftThermalSectionAttachment) (Object) chunk.getSections()[chunk.getSectionIndex(position.getY())];
        var owner = attachment.frostedheart$getThermalInputOwner();
        helper.assertTrue(owner != null && owner.page() != null, "Real Section has a thermal Page");
        return owner.page();
    }

    private static void sample(net.minecraft.server.level.ServerLevel level, BlockPos position, ThermalEnvironmentSample out) {
        ContinuousAirProductionGameTests.sampleAt(level, position.getX() + .5, position.getY() + .5, position.getZ() + .5, out);
    }
}
