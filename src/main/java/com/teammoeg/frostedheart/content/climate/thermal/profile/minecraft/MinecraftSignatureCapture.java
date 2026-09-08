/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.Blocks;

/** Main-thread loaded-only state-static signature capture. */
public final class MinecraftSignatureCapture {
    private final ServerLevel level;
    private final MinecraftStateThermalTable states;
    private final ThermalSignatureTable signatureTable;
    private final PageSignatures.Builder pageBuilder;
    private final int[] brickValues = new int[PageSignatures.ENTRIES_PER_BRICK];
    private final int[] firstCenter = new int[64];
    private int[] nextCenter = new int[64];
    private final int airSignatureId;
    private final BlockPos.MutableBlockPos position =
            new BlockPos.MutableBlockPos();

    public MinecraftSignatureCapture(
            ServerLevel level,
            MinecraftStateThermalTable states,
            ThermalSignatureTable signatures
    ) {
        this.level = level;
        this.states = states;
        signatureTable = signatures;
        pageBuilder = new PageSignatures.Builder(signatures);
        airSignatureId = states.signatureId(Blocks.AIR.defaultBlockState());
    }

    public PageSignatures unresolvedPage() {
        return PageSignatures.unresolved(signatureTable);
    }

    public PageSignatures captureBricks(
            long sectionKey,
            LevelChunkSection section,
            PageSignatures base,
            long brickMask
    ) {
        if (brickMask == 0L) {
            return base;
        }
        LevelChunkSection loadedSection = section == null
                ? loadedSection(sectionKey) : section;
        PageSignatures.Builder builder = pageBuilder.reset(base);
        if (loadedSection != null && loadedSection.hasOnlyAir()) {
            long remaining = brickMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                builder.setUniformBrick(brick, airSignatureId);
                remaining &= remaining - 1L;
            }
            return builder.buildBricks();
        }
        long remaining = brickMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            int write = 0;
            int minX = (brick & 3) << 2;
            int minZ = (brick >>> 2 & 3) << 2;
            int minY = (brick >>> 4 & 3) << 2;
            for (int y = minY; y < minY + 4; y++) {
                for (int z = minZ; z < minZ + 4; z++) {
                    for (int x = minX; x < minX + 4; x++) {
                        BlockState state = loadedSection == null
                                ? null : loadedSection.getBlockState(x, y, z);
                        int stateCode = state == null
                                ? MinecraftStateThermalTable.UNRESOLVED_CODE
                                : states.code(state);
                        brickValues[write++] =
                                states.signatureIdFromCode(stateCode);
                    }
                }
            }
            builder.setBrick(brick, brickValues);
            remaining &= remaining - 1L;
        }
        return builder.buildBricks();
    }

    public int resolveSignatureId(int worldX, int worldY, int worldZ) {
        if (level.isOutsideBuildHeight(worldY)) {
            return ThermalSignatureTable.UNRESOLVED;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(worldX),
                SectionPos.blockToSectionCoord(worldZ));
        if (chunk == null) {
            return ThermalSignatureTable.UNRESOLVED;
        }
        position.set(worldX, worldY, worldZ);
        return resolveId(chunk.getBlockState(position));
    }

    public PageSignatures withResolvedBlocks(PageSignatures base,
            short[] centers, int[] signatureIds, int count) {
        if (count == 0) return base;
        java.util.Arrays.fill(firstCenter, -1);
        if (nextCenter.length < count) nextCenter = new int[count];
        long changedBricks = 0;
        for (int i = count - 1; i >= 0; i--) {
            int block = Short.toUnsignedInt(centers[i]);
            int brick = (block & 15) >>> 2
                    | ((block >>> 4 & 15) >>> 2) << 2
                    | ((block >>> 8 & 15) >>> 2) << 4;
            nextCenter[i] = firstCenter[brick];
            firstCenter[brick] = i;
            changedBricks |= 1L << brick;
        }
        pageBuilder.reset(base);
        while (changedBricks != 0) {
            int brick = Long.numberOfTrailingZeros(changedBricks);
            changedBricks &= changedBricks - 1;
            for (int b = 0; b < 64; b++) {
                brickValues[b] = base.get(
                        com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout.pageBlock(brick, b));
            }
            for (int i = firstCenter[brick]; i >= 0; i = nextCenter[i]) {
                int block = Short.toUnsignedInt(centers[i]);
                int within = (block & 3) | (block >>> 4 & 3) << 2 | (block >>> 8 & 3) << 4;
                brickValues[within] = signatureIds[i];
            }
            pageBuilder.setBrick(brick, brickValues);
        }
        return pageBuilder.buildBricks();
    }

    private int resolveId(BlockState state) {
        return states.signatureId(state);
    }

    private LevelChunkSection loadedSection(long sectionKey) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        if (chunk == null) {
            return null;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(
                SectionPos.y(sectionKey));
        return sectionIndex >= 0 && sectionIndex < chunk.getSections().length
                ? chunk.getSections()[sectionIndex] : null;
    }
}
