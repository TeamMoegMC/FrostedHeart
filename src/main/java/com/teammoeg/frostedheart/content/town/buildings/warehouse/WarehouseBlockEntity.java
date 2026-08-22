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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

public class WarehouseBlockEntity extends AbstractTownBuildingBlockEntity<WarehouseBuilding> implements MenuProvider {

    public WarehouseBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WAREHOUSE.get(),pos,state);
    }

    @Override
    public void refresh(@NotNull WarehouseBuilding building) {
        boolean wasWorkableBefore = building.isBuildingWorkable();
        super.refresh(building);
        ITownWithBuildings buildingTown = this.getTown();
        if(buildingTown instanceof TeamTown teamTown){
            teamTown.getTownData().ifPresent(TeamTownData::reloadMaxCapacity);
        }

        if (!wasWorkableBefore && building.isBuildingWorkable() && level != null) {
            for (BlockPos emitterPos : building.getEmitterPositions()) {
                if (level.isLoaded(emitterPos) && level.getBlockEntity(emitterPos) instanceof WarehouseLevelEmitterBlockEntity emitter) {
                    emitter.ensureWatcherAndRefresh();
                }
            }
            for (BlockPos interfacePos : building.getInterfacePositions()) {
                if (level.isLoaded(interfacePos) && level.getBlockEntity(interfacePos) instanceof WarehouseInterfaceBlockEntity iface) {
                    iface.ensureWatcherAndRefresh();
                }
            }
        }
    }

    public boolean scanStructure(WarehouseBuilding building){
        BlockPos warehousePos = this.getBlockPos();
        BlockPos doorPos = AbstractBlockScanner.getDoorAdjacent(level, warehousePos);
        if (doorPos == null) {
            clearWallDevices(building);
            return false;
        }
        BlockPos floorBelowDoor = AbstractBlockScanner.getBlockBelow(Objects.requireNonNull(level), (pos)->!(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
        if (floorBelowDoor == null) {
            clearWallDevices(building);
            return false;
        }
        for (Direction direction : AbstractBlockScanner.PLANE_DIRECTIONS) {
            BlockPos startPos = floorBelowDoor.relative(direction);//找到门下方块旁边的方块
            if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isBuildingBlock(level, startPos.above(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.below();
            }
            WarehouseBlockScanner scanner = new WarehouseBlockScanner(level, startPos);
            if(scanner.scan()){
            	building.setArea(scanner.getArea());
            	building.setVolume(scanner.getVolume());
                //容量与体积相似，但是在随着房间高度增高略有衰减
                building.setDecorationAmount(scanner.decorations.values().stream().mapToInt(Integer::intValue).sum());

                building.setCapacity(building.getArea() * Math.pow(building.getVolume() * 0.02 / building.getArea(), 0.9) * 1980 + building.getDecorationAmount() * 512);
            	building.setOccupiedVolume(scanner.getOccupiedVolume());
                publishInterfaces(building, scanner.getWallInterfacePositions());
                publishEmitters(building, scanner.getWallEmitterPositions());
                return true;
            }
        }
        clearWallDevices(building);
        return false;
    }

    private void publishInterfaces(WarehouseBuilding building, Set<BlockPos> discovered) {
        if (level == null || townProvider == null) {
            clearInterfaces(building);
            return;
        }

        Set<BlockPos> accepted = new LinkedHashSet<>();
        for (BlockPos interfacePos : discovered) {
            if (level.getBlockEntity(interfacePos) instanceof WarehouseInterfaceBlockEntity warehouseInterface
                    && warehouseInterface.tryBind(townProvider, worldPosition)) {
                accepted.add(interfacePos.immutable());
            }
        }

        Set<BlockPos> previous = building.replaceInterfaces(accepted);
        previous.removeAll(accepted);
        for (BlockPos interfacePos : accepted) {
            if (level.getBlockEntity(interfacePos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
                warehouseInterface.ensureWatcherAndRefresh();
            }
        }
        unregisterRemovedInterfaces(previous);
        for (BlockPos removedPos : previous) {
            if (level.isLoaded(removedPos)
                    && level.getBlockEntity(removedPos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
                warehouseInterface.unbindIfBoundTo(townProvider, worldPosition);
            }
        }
    }

    private void publishEmitters(WarehouseBuilding building, Set<BlockPos> discovered) {
        if (level == null || townProvider == null) {
            clearEmitters(building);
            return;
        }

        Set<BlockPos> accepted = new LinkedHashSet<>();
        for (BlockPos emitterPos : discovered) {
            if (level.getBlockEntity(emitterPos) instanceof WarehouseLevelEmitterBlockEntity levelEmitter
                    && levelEmitter.tryBind(townProvider, worldPosition)) {
                accepted.add(emitterPos.immutable());
            }
        }

        Set<BlockPos> previous = building.replaceEmitters(accepted);
        previous.removeAll(accepted);
        for (BlockPos removedPos : previous) {
            if (level.isLoaded(removedPos)
                    && level.getBlockEntity(removedPos) instanceof WarehouseLevelEmitterBlockEntity levelEmitter) {
                levelEmitter.unbindIfBoundTo(townProvider, worldPosition);
            }
        }
    }

    private void clearInterfaces(WarehouseBuilding building) {
        Set<BlockPos> previous = building.replaceInterfaces(Set.of());
        if (level == null || townProvider == null) {
            return;
        }
        unregisterRemovedInterfaces(previous);
        for (BlockPos interfacePos : previous) {
            if (level.isLoaded(interfacePos)
                    && level.getBlockEntity(interfacePos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
                warehouseInterface.unbindIfBoundTo(townProvider, worldPosition);
            }
        }
    }

    private void unregisterRemovedInterfaces(Set<BlockPos> removedPositions) {
        if (level == null || townProvider == null || !(townProvider.getTown() instanceof TeamTown teamTown)) {
            return;
        }
        for (BlockPos removedPos : removedPositions) {
            teamTown.unregisterTransportEndpoint(new TransportEndpointId(
                    GlobalPos.of(level.dimension(), removedPos)));
        }
    }

    private void clearEmitters(WarehouseBuilding building) {
        Set<BlockPos> previous = building.replaceEmitters(Set.of());
        if (level == null || townProvider == null) {
            return;
        }
        for (BlockPos emitterPos : previous) {
            if (level.isLoaded(emitterPos)
                    && level.getBlockEntity(emitterPos) instanceof WarehouseLevelEmitterBlockEntity levelEmitter) {
                levelEmitter.unbindIfBoundTo(townProvider, worldPosition);
            }
        }
    }

    private void clearWallDevices(WarehouseBuilding building) {
        clearInterfaces(building);
        clearEmitters(building);
    }


    @Override
    public @Nullable WarehouseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof WarehouseBuilding){
            return (WarehouseBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new WarehouseMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.warehouse");
    }

    @Override
    public @NotNull WarehouseBuilding createBuilding() {
        return new WarehouseBuilding(this.getBlockPos());
    }
}
