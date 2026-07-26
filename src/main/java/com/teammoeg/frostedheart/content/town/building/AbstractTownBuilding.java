/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.event.ITownBuildingChangeEventListener;
import com.teammoeg.frostedheart.content.town.event.TownBuildingChangeEvent;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 *
 */
@Getter
public abstract class AbstractTownBuilding implements ITownBuilding{

    /**
     * The codec for (de)serializing any town building. Delegates to {@link ITownBuilding#CODEC}.
     */
    public static final Codec<ITownBuilding> CODEC = ITownBuilding.CODEC;

    /**
     * BlockPos of the main block of the building
     */
    @Getter
    protected final BlockPos pos;

    /**
     * 建筑最初创建时，默认状态为未初始化，检查能否工作时应优先检查此项，或许可以避免意外的空指针？
     */
    private boolean initialized = false;

    private boolean occupiedAreaOverlapped = false;

    private boolean isStructureValid = false;

    private OccupiedVolume occupiedVolume = OccupiedVolume.EMPTY;

    /**
     * 变化监听。由 TeamTownData 在 building 装入 Map 后注入；
     * 各字段 setter 通过该监听 fire 事件，从而进入增量同步的脏标记。
     * 解码阶段此值为 null，因此 setter 不会误触发脏标记。
     */
    @Getter(AccessLevel.NONE)
    private transient ITownBuildingChangeEventListener changeListener;

    public void setChangeEventListener(ITownBuildingChangeEventListener listener) {
        this.changeListener = listener;
    }

    /**
     * 字段发生变化时调用，向监听者 fire 一个建筑变更事件。
     * 若监听尚未注入（如解码阶段）则不产生任何效果。
     */
    protected void fireChange() {
        if (this.changeListener != null) {
            this.changeListener.onBuildingChange(new TownBuildingChangeEvent(this, this.pos));
        }
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        fireChange();
    }

    public void setOccupiedAreaOverlapped(boolean occupiedAreaOverlapped) {
        this.occupiedAreaOverlapped = occupiedAreaOverlapped;
        fireChange();
    }

    public void setIsStructureValid(boolean isStructureValid) {
        this.isStructureValid = isStructureValid;
        fireChange();
    }

    public void setOccupiedVolume(OccupiedVolume occupiedVolume) {
        this.occupiedVolume = occupiedVolume;
        fireChange();
    }

    protected AbstractTownBuilding(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public boolean isBuildingWorkable(){
        return initialized && !occupiedAreaOverlapped && isStructureValid;
    }

    @Override
    public boolean work(ITownWithBuildings town, ServerLevel world){
        return work(town);
    }

    public boolean work(ITownWithBuildings town){
        return true;
    }

    public void onRemoved(ITownWithBuildings town){

    }

}
