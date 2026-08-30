/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Map;

/** Main-thread loaded-only state-static signature capture. */
public final class MinecraftSignatureCapture {
    private final ServerLevel level;
    private final StateStaticThermalResolver resolver;
    private final ThermalSignatureRegistry signatures;
    private final Map<BlockState, Integer> staticSignatureIds;
    private final PageSignatures.Builder pageBuilder =
            new PageSignatures.Builder();
    private final BlockPos.MutableBlockPos position =
            new BlockPos.MutableBlockPos();

    public MinecraftSignatureCapture(
            ServerLevel level,
            StateStaticThermalResolver resolver,
            ThermalSignatureRegistry signatures,
            Map<BlockState, Integer> staticSignatureIds
    ) {
        this.level = level;
        this.resolver = resolver;
        this.signatures = signatures;
        this.staticSignatureIds = staticSignatureIds;
    }

    public PageSignatures capturePage(
            long sectionKey,
            LevelChunkSection section
    ) {
        LevelChunkSection loadedSection = section == null
                ? loadedSection(sectionKey)
                : section;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    pageBuilder.set(
                            x | z << 4 | y << 8,
                            loadedSection == null
                                    ? -1
                                    : resolveId(loadedSection.getBlockState(
                                            x, y, z)));
                }
            }
        }
        return pageBuilder.build();
    }

    public int resolveSignatureId(int worldX, int worldY, int worldZ) {
        if (level.isOutsideBuildHeight(worldY)) {
            return -1;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(worldX),
                SectionPos.blockToSectionCoord(worldZ));
        if (chunk == null) {
            return -1;
        }
        position.set(worldX, worldY, worldZ);
        return resolveId(chunk.getBlockState(position));
    }

    private int resolveId(BlockState state) {
        Integer staticSignatureId = staticSignatureIds.get(state);
        if (staticSignatureId != null) {
            return staticSignatureId;
        }
        ResolvedThermalSignature resolved =
                resolver.resolve(state, state.getFluidState());
        return resolved == null ? -1 : signatures.idOrDefault(resolved, -1);
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
                ? chunk.getSections()[sectionIndex]
                : null;
    }
}
