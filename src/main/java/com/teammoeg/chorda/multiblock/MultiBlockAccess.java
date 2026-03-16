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

package com.teammoeg.chorda.multiblock;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * 多方块结构访问接口，提供多方块成型和玩家关联的能力。
 * 用于在多方块形成过程中设置触发玩家信息和控制成型逻辑。
 * <p>
 * Access interface for multiblock structures, providing capabilities for multiblock formation
 * and player association. Used to set triggering player information and control formation logic
 * during the multiblock formation process.
 */
public interface MultiBlockAccess {

    /**
     * 在指定位置以给定的旋转、镜像和方向触发多方块成型。
     * <p>
     * Triggers multiblock formation at the specified position with the given rotation,
     * mirror, and direction.
     *
     * @param world 当前世界 / The current level
     * @param pos 触发成型的方块位置 / The block position triggering formation
     * @param rot 旋转状态 / The rotation state
     * @param mirror 镜像状态 / The mirror state
     * @param sideHit 被点击的面方向 / The side that was hit
     */
    void callForm(Level world, BlockPos pos, Rotation rot, Mirror mirror, Direction sideHit);

    /**
     * 设置触发多方块成型的玩家。
     * <p>
     * Sets the player who triggered the multiblock formation.
     *
     * @param spe 触发成型的服务端玩家 / The server player who triggered formation
     */
    void setPlayer(ServerPlayer spe);

    /**
     * 设置与此多方块关联的 UUID（通常为玩家或队伍的 UUID）。
     * <p>
     * Sets the UUID associated with this multiblock (typically a player or team UUID).
     *
     * @param id 要关联的 UUID / The UUID to associate
     */
	void setUUID(UUID id);
}

