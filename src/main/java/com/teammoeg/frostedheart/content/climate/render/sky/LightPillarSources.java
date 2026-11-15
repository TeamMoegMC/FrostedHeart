/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.mixin.client.LightEngineAccessor_LightPillars;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Finds the lights in the loaded chunks that can show light pillars, and chooses those to draw on the client tick.
 * LightPillarWorldChanges queues the chunk sections whose lights may have changed, and each tick takes a fixed number
 * of them. A section is skipped when its block palette holds no light that shows pillars, or when neither its sky light
 * nor the lowest layer of the section above it reaches above LightPillarClusters.SKY_LIGHT_THRESHOLD. Otherwise its
 * blocks are walked. A light counts by its sky light, which leaves out lights indoors and in caves. Each light's cap is
 * read with LightPillarShelter.readLightCap and its rims and ceilings with LightPillarShelter.read, and lights that
 * touch and share a shelter are merged in LightPillarClusters. LightPillarSelection chooses from them every tick while
 * there is diamond dust, and takes the shelters with them.
 * The index is kept while the calculated sky is allowed, so nothing is read from the world while nothing changes.
 * Client thread only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightPillarSources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SECTION_TESTS_PER_TICK = 256;
    private static final int SECTIONS_WALKED_PER_TICK = 4;
    private static final int SECTION_BLOCKS = 16 * 16 * 16;
    private static final int TICKS_PER_SECOND = 20;
    private static final int LISTED_PILLARS = 10;
    /**
     * The share of the way to the ground under the camera that the dust's bottom moves per tick, which follows the
     * ground over about 30 seconds.
     */
    private static final double DUST_BOTTOM_STEP = 1.0 - Math.exp(-0.05 / 30.0);
    /**
     * A horizontal move of the camera in one tick beyond this is taken as a teleport, faster than any travel.
     */
    private static final double TELEPORT_BLOCKS = 16.0;
    private static final Predicate<BlockState> SHOWS_PILLAR = LightPillarEmitters::showsPillar;
    private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
    @Nullable
    private static final com.sun.management.ThreadMXBean ALLOCATION_COUNTER = findAllocationCounter();

    private static final LightPillarClusters clusters = new LightPillarClusters();
    private static final LightPillarSelection selection = new LightPillarSelection();
    private static final LightPillarShelter shelter = new LightPillarShelter();
    private static final LightPillarHorizons.LevelColumns shelterColumns =
            new LightPillarHorizons.LevelColumns(shelter.occluders());
    private static final Reference2ObjectOpenHashMap<BlockState, float[]> emitterColors =
            new Reference2ObjectOpenHashMap<>();
    private static final BlockPos.MutableBlockPos lightPosition = new BlockPos.MutableBlockPos();
    private static int[] texturePixels = new int[SECTION_BLOCKS];

    @Nullable
    private static ClientLevel scannedLevel;
    private static boolean failed;
    private static boolean indexing;
    private static int chunkRadius;
    private static double dustBottomY = Double.NaN;
    private static double lastEyeX = Double.NaN;
    private static double lastEyeZ = Double.NaN;

    private static final SecondStatistics currentSecond = new SecondStatistics();
    private static final SecondStatistics lastSecond = new SecondStatistics();
    private static int ticksThisSecond;
    private static int shelterBlocksReadLastSecond;

    private LightPillarSources() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != scannedLevel) {
            reset();
            scannedLevel = level;
        }
        LightPillarWorldChanges.tick();
        rollStatistics();
        if (level == null || failed) {
            return;
        }
        long allocatedBefore = allocatedBytes();
        long start = System.nanoTime();
        try {
            tick(minecraft, level);
        } catch (RuntimeException exception) {
            failed = true;
            reset();
            LOGGER.error("Light pillars stopped after an error. They stay off until the calculated sky is turned on "
                    + "again.", exception);
            return;
        }
        if (indexing) {
            currentSecond.addTick(System.nanoTime() - start, allocatedBytes() - allocatedBefore);
        }
    }

    /**
     * The bytes the client thread allocated so far, or 0 where the JVM does not count them.
     */
    static long allocatedBytes() {
        return ALLOCATION_COUNTER != null ? ALLOCATION_COUNTER.getCurrentThreadAllocatedBytes() : 0L;
    }

    static boolean countsAllocations() {
        return ALLOCATION_COUNTER != null;
    }

    @Nullable
    private static com.sun.management.ThreadMXBean findAllocationCounter() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean threads
                && threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled()) {
            return threads;
        }
        return null;
    }

    private static void tick(Minecraft minecraft, ClientLevel level) {
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        updateDustBottom(level, eye);
        if (!CalculatedSkyRenderer.isAllowed(level)) {
            if (indexing) {
                stopIndexing();
            }
            selection.chooseNone();
            return;
        }
        if (!indexing) {
            LightPillarWorldChanges.startTracking(level);
            indexing = true;
        }
        index(minecraft, level);
        chunkRadius = chunkRadius(minecraft);
        if (DiamondDustExtinction.lastValue() > 0.0) {
            selection.choose(clusters, eye.x, eye.y, eye.z, dustBottomY, chunkRadius);
        } else {
            selection.chooseNone();
        }
    }

    /**
     * The render distance in chunks, or the server's view distance where that is smaller. LightPillarRenderer fades
     * the pillars out in the fog before it.
     */
    private static int chunkRadius(Minecraft minecraft) {
        return Math.max(1, minecraft.options.getEffectiveRenderDistance());
    }

    /**
     * Moves the dust's bottom toward the top of the ground under the camera, or sets it there at first and after the
     * camera jumped further than TELEPORT_BLOCKS in one tick. Until the ground under the camera is loaded after such a
     * jump, the bottom is unknown and no cluster is chosen.
     */
    private static void updateDustBottom(ClientLevel level, Vec3 eye) {
        double movedX = eye.x - lastEyeX;
        double movedZ = eye.z - lastEyeZ;
        if (!(movedX * movedX + movedZ * movedZ <= TELEPORT_BLOCKS * TELEPORT_BLOCKS)) {
            dustBottomY = Double.NaN;
        }
        lastEyeX = eye.x;
        lastEyeZ = eye.z;
        int blockX = Mth.floor(eye.x);
        int blockZ = Mth.floor(eye.z);
        if (!level.hasChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ))) {
            return;
        }
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
        dustBottomY = Double.isNaN(dustBottomY) ? ground : dustBottomY + (ground - dustBottomY) * DUST_BOTTOM_STEP;
    }

    /**
     * Drops the clusters of unloaded columns, then updates queued sections until a budget is used up.
     */
    private static void index(Minecraft minecraft, ClientLevel level) {
        LightPillarWorkQueue queue = LightPillarWorldChanges.queue();
        int lowestSectionY = level.getMinSection();
        int sectionCount = level.getSectionsCount();
        while (queue.nextDroppedColumn()) {
            long column = queue.takenColumn();
            clusters.removeColumn(ChunkPos.getX(column), ChunkPos.getZ(column), lowestSectionY, sectionCount);
            currentSecond.columnsDropped++;
        }
        LayerLightSectionStorage<?> skyLight = skyLightStorage(level);
        int tests = 0;
        int walks = 0;
        shelterColumns.setLevel(level);
        while (tests < SECTION_TESTS_PER_TICK && walks < SECTIONS_WALKED_PER_TICK && queue.nextSection()) {
            tests++;
            if (updateSection(minecraft, level, skyLight, queue.takenSection())) {
                walks++;
            }
        }
        shelterColumns.setLevel(null);
        currentSecond.sectionsTested += tests;
        currentSecond.sectionsWalked += walks;
    }

    /**
     * The storage of the level's sky light, or null for a level without sky light.
     */
    @Nullable
    private static LayerLightSectionStorage<?> skyLightStorage(ClientLevel level) {
        LayerLightEventListener listener = level.getLightEngine().getLayerListener(LightLayer.SKY);
        return listener instanceof LightEngineAccessor_LightPillars accessor ? accessor.frostedheart$getStorage() : null;
    }

    /**
     * Replaces the clusters of a section with the lights found in it now, or removes them when the section cannot hold
     * a light that shows a pillar. Returns whether the section's blocks were walked.
     */
    private static boolean updateSection(Minecraft minecraft, ClientLevel level,
            @Nullable LayerLightSectionStorage<?> skyLight, long sectionKey) {
        int chunkX = SectionPos.x(sectionKey);
        int sectionY = SectionPos.y(sectionKey);
        int chunkZ = SectionPos.z(sectionKey);
        LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
        if (chunk == null || skyLight == null) {
            clusters.removeSection(chunkX, sectionY, chunkZ);
            return false;
        }
        LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
        if (section.hasOnlyAir() || !section.maybeHas(SHOWS_PILLAR)
                || !LightPillarClusters.sectionCanShowPillars(skyLight.getDataLayerData(sectionKey),
                skyLight.getDataLayerData(SectionPos.offset(sectionKey, 0, 1, 0)))) {
            clusters.removeSection(chunkX, sectionY, chunkZ);
            return false;
        }
        walk(minecraft, level, section, chunkX, sectionY, chunkZ);
        return true;
    }

    /**
     * Replaces the section's clusters with every light of the section with sky light above
     * LightPillarClusters.SKY_LIGHT_THRESHOLD at itself or the block above, each with its shelter.
     */
    @SuppressWarnings("deprecation")
    private static void walk(Minecraft minecraft, ClientLevel level, LevelChunkSection section, int chunkX,
            int sectionY, int chunkZ) {
        int originX = SectionPos.sectionToBlockCoord(chunkX);
        int originY = SectionPos.sectionToBlockCoord(sectionY);
        int originZ = SectionPos.sectionToBlockCoord(chunkZ);
        int threshold = LightPillarClusters.SKY_LIGHT_THRESHOLD;
        clusters.beginSection(chunkX, sectionY, chunkZ);
        shelter.beginSection(originX, originZ);
        for (int blockY = 0; blockY < 16; blockY++) {
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    BlockState state = section.getBlockState(blockX, blockY, blockZ);
                    int emission = state.getLightEmission();
                    if (emission < LightPillarEmitters.MINIMUM_EMISSION) {
                        continue;
                    }
                    lightPosition.set(originX + blockX, originY + blockY, originZ + blockZ);
                    int skyLight = level.getBrightness(LightLayer.SKY, lightPosition);
                    lightPosition.setY(lightPosition.getY() + 1);
                    skyLight = Math.max(skyLight, level.getBrightness(LightLayer.SKY, lightPosition));
                    if (skyLight <= threshold) {
                        continue;
                    }
                    float strength = LightPillarEmitters.strength(emission)
                            * Math.min(1.0F, (skyLight - threshold) / (15.0F - threshold));
                    float[] color = emitterColor(minecraft, state);
                    lightPosition.setY(originY + blockY);
                    double lightTop = originY + blockY + LightPillarOccluders.lightTop(state, level, lightPosition);
                    boolean fullBlock = LightPillarOccluders.isFullBlock(state, level, lightPosition);
                    boolean spansBlock = fullBlock || state.getBlock() instanceof LiquidBlock;
                    double cap = LightPillarShelter.readLightCap(shelterColumns, originX + blockX, originZ + blockZ,
                            lightTop, fullBlock);
                    shelter.read(shelterColumns, originX + blockX, originZ + blockZ, lightTop);
                    clusters.addLight(originX + blockX, originY + blockY, originZ + blockZ, lightTop, fullBlock,
                            spansBlock, cap, shelter, color[0] * strength, color[1] * strength,
                            color[2] * strength);
                    currentSecond.lights++;
                }
            }
        }
        shelter.endSection();
        clusters.endSection();
    }

    private static float[] emitterColor(Minecraft minecraft, BlockState state) {
        float[] color = emitterColors.get(state);
        if (color == null) {
            color = findEmitterColor(minecraft, state);
            emitterColors.put(state, color);
        }
        return color;
    }

    /**
     * The state's color from the override table, or else from its particle texture, or white when the texture cannot
     * be read.
     */
    private static float[] findEmitterColor(Minecraft minecraft, BlockState state) {
        float[] override = LightPillarEmitters.overrides().overrideColor(state);
        if (override != null) {
            return override;
        }
        float[] color = new float[3];
        try {
            TextureAtlasSprite sprite = minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            int count = width * height;
            if (texturePixels.length < count) {
                texturePixels = new int[count];
            }
            for (int pixelY = 0; pixelY < height; pixelY++) {
                for (int pixelX = 0; pixelX < width; pixelX++) {
                    texturePixels[pixelY * width + pixelX] = sprite.getPixelRGBA(0, pixelX, pixelY);
                }
            }
            LightPillarEmitters.averageTextureColor(texturePixels, count, color);
        } catch (RuntimeException exception) {
            return WHITE;
        }
        return color;
    }

    private static void stopIndexing() {
        LightPillarWorldChanges.stopTracking();
        clusters.clear();
        shelterColumns.setLevel(null);
        indexing = false;
    }

    private static void reset() {
        stopIndexing();
        selection.clear();
        dustBottomY = Double.NaN;
        lastEyeX = Double.NaN;
        lastEyeZ = Double.NaN;
        currentSecond.clear();
        lastSecond.clear();
        ticksThisSecond = 0;
    }

    private static void rollStatistics() {
        ticksThisSecond++;
        if (ticksThisSecond >= TICKS_PER_SECOND) {
            lastSecond.copy(currentSecond);
            currentSecond.clear();
            shelterBlocksReadLastSecond = shelterColumns.takeBlocksRead();
            ticksThisSecond = 0;
        }
    }

    /**
     * Forgets which block states stop light once the client received new block tags, and reads the shelters again.
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            shelter.occluders().clear();
            clusters.queueEverySection(LightPillarWorldChanges.queue());
        }
    }

    /**
     * The pillars chosen in the last client tick, with their fades.
     */
    static LightPillarSelection selection() {
        return selection;
    }

    /**
     * The shelter reader of the index, whose occluders say which blocks stop light.
     */
    static LightPillarShelter shelter() {
        return shelter;
    }

    static LightPillarClusters clusters() {
        return clusters;
    }

    /**
     * The height of the bottom of the dust, or NaN while it is unknown.
     */
    static double dustBottomY() {
        return dustBottomY;
    }

    /**
     * Forgets the colors found for block states, after a resource reload.
     */
    public static void clearColors() {
        emitterColors.clear();
    }

    public static void clearFailure() {
        failed = false;
    }

    /**
     * A line about the index and the work of the last full second, for the client command.
     */
    public static String describe(ClientLevel level) {
        String state;
        if (failed) {
            state = "stopped after an error, see the log";
        } else if (!CalculatedSkyRenderer.isAllowed(level)) {
            state = "idle while the calculated sky is not drawn";
        } else if (!(DiamondDustExtinction.lastValue() > 0.0)) {
            state = "indexing, none chosen without diamond dust";
        } else {
            state = "indexing, choosing within " + chunkRadius + " chunks";
        }
        double meanMilliseconds = lastSecond.ticks == 0 ? 0.0 : lastSecond.nanos / 1.0e6 / lastSecond.ticks;
        String allocated = ALLOCATION_COUNTER != null ? lastSecond.allocatedBytes + " bytes allocated"
                : "allocations not counted by this JVM";
        return String.format(Locale.ROOT,
                "Light pillars %s; %d clusters in %d sections, %d sections queued; last second %d sections tested, "
                        + "%d walked, %d lights found reading %d blocks for their shelters, %d chunks dropped, "
                        + "tick %.3f ms mean and %.3f ms most, %s; %d chosen, %d of %d slots used, dust bottom y %.1f",
                state, clusters.count(), clusters.sectionCount(),
                LightPillarWorldChanges.queue().queuedSectionCount(), lastSecond.sectionsTested,
                lastSecond.sectionsWalked, lastSecond.lights, shelterBlocksReadLastSecond, lastSecond.columnsDropped,
                meanMilliseconds, lastSecond.mostNanos / 1.0e6, allocated, selection.chosenCount(),
                selection.usedCount(), LightPillarSelection.SLOT_COUNT, dustBottomY);
    }

    /**
     * Lines about the pillars in use with the highest scores, for the client command.
     */
    public static List<String> describePillars(Vec3 eye) {
        List<Integer> used = new ArrayList<>();
        for (int slot = 0; slot < LightPillarSelection.SLOT_COUNT; slot++) {
            if (selection.isUsed(slot)) {
                used.add(slot);
            }
        }
        used.sort(Comparator.comparingDouble(slot -> -selection.slotScore(slot)));
        List<String> lines = new ArrayList<>();
        for (int rank = 0; rank < Math.min(LISTED_PILLARS, used.size()); rank++) {
            int slot = used.get(rank);
            float brightest = Math.max(selection.red(slot), Math.max(selection.green(slot), selection.blue(slot)));
            double distance = Math.hypot(selection.x(slot) - eye.x, selection.z(slot) - eye.z);
            lines.add(String.format(Locale.ROOT,
                    "%d. at %.1f %.1f %.1f, top %.2f, %.1f blocks away, %d %s across %.1f by %.1f blocks, "
                            + "color %.2f %.2f %.2f, strength %.2f, score %.3g, fade %.1f, %s",
                    rank + 1, selection.x(slot), selection.y(slot), selection.z(slot), selection.top(slot), distance,
                    selection.lightCount(slot), selection.lightCount(slot) == 1 ? "light" : "lights",
                    2.0 * selection.halfExtentX(slot), 2.0 * selection.halfExtentZ(slot),
                    selection.red(slot) / brightest, selection.green(slot) / brightest,
                    selection.blue(slot) / brightest, brightest, selection.slotScore(slot), selection.fade(slot, 1.0F),
                    selection.describeShelter(slot, eye.x, eye.z)));
        }
        return lines;
    }

    /**
     * Counts of one second of indexing, and the time and memory its ticks took.
     */
    private static final class SecondStatistics {
        private int ticks;
        private int sectionsTested;
        private int sectionsWalked;
        private int lights;
        private int columnsDropped;
        private long nanos;
        private long mostNanos;
        private long allocatedBytes;

        private void addTick(long tickNanos, long tickAllocatedBytes) {
            ticks++;
            nanos += tickNanos;
            mostNanos = Math.max(mostNanos, tickNanos);
            allocatedBytes += tickAllocatedBytes;
        }

        private void copy(SecondStatistics other) {
            ticks = other.ticks;
            sectionsTested = other.sectionsTested;
            sectionsWalked = other.sectionsWalked;
            lights = other.lights;
            columnsDropped = other.columnsDropped;
            nanos = other.nanos;
            mostNanos = other.mostNanos;
            allocatedBytes = other.allocatedBytes;
        }

        private void clear() {
            ticks = 0;
            sectionsTested = 0;
            sectionsWalked = 0;
            lights = 0;
            columnsDropped = 0;
            nanos = 0;
            mostNanos = 0;
            allocatedBytes = 0;
        }
    }
}
