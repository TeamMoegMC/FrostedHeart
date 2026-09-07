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

    public PageSignatures withResolvedBlock(
            PageSignatures base,
            int blockIndex,
            int signatureId
    ) {
        int localX = blockIndex & 15;
        int localZ = blockIndex >>> 4 & 15;
        int localY = blockIndex >>> 8 & 15;
        int brick = localX >>> 2
                | (localZ >>> 2) << 2
                | (localY >>> 2) << 4;
        int write = 0;
        int minX = (brick & 3) << 2;
        int minZ = (brick >>> 2 & 3) << 2;
        int minY = (brick >>> 4 & 3) << 2;
        for (int y = minY; y < minY + 4; y++) {
            for (int z = minZ; z < minZ + 4; z++) {
                for (int x = minX; x < minX + 4; x++) {
                    brickValues[write++] = base.get(x | z << 4 | y << 8);
                }
            }
        }
        int within = (localX & 3)
                | (localZ & 3) << 2
                | (localY & 3) << 4;
        brickValues[within] = signatureId;
        return pageBuilder.reset(base)
                .setBrick(brick, brickValues)
                .buildBricks();
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
