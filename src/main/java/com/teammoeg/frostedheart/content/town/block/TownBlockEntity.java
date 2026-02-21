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

package com.teammoeg.frostedheart.content.town.block;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A town block's tile entity.
 * <p>
 * Should be implemented by tile entities that are associated with town blocks.
 */
public interface TownBlockEntity<T extends ITownBuilding> {
    /**
     * 刷新Building中关于世界的数据
     * @param building the building
     */
    void refresh(@NotNull T building);

    /**
     * 获取Block关联的Building
     * @return the building
     */
    Optional<T> getBuilding();

    /**
     * 将AbstractTownBuilding转为泛型T。由于在此无法判断类型，需要在子类进行类型转换。
     * @param abstractTownBuilding AbstractTownBuilding with unknown type.
     * @return TownBuilding with type T, null if the type cannot be converted.
     */
    @Nullable T getBuilding(AbstractTownBuilding abstractTownBuilding);

    @NotNull T createBuilding();


}
