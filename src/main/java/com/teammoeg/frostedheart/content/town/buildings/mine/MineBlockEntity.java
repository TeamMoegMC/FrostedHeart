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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class MineBlockEntity extends AbstractTownBuildingBlockEntity<MineBuilding> implements MenuProvider {

    public MineBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE.get(),pos,state);
    }

    public StructureScanResult scanStructure(MineBuilding building) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
        var chunk = serverLevel.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        if (chunk == null) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
        Holder<Biome> biome = CUtils.fastGetBiome(chunk, worldPosition);
        building.setBiomePath(CRegistryHelper.getBiomeKeyRuntime(level, biome.value()));
        return StructureScanResult.VALID;
    }

    @Override
    public @Nullable MineBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof MineBuilding){
            return (MineBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Override
    public @NotNull MineBuilding createBuilding() {
        return new MineBuilding(this.getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MineMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.mine");
    }
}
