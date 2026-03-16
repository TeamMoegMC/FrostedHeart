package com.teammoeg.chorda.compat.ftb;

import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * FTB Chunks模组兼容工具类。提供与FTB Chunks区块声明系统交互的辅助方法。
 * <p>
 * FTB Chunks mod compatibility utility class. Provides helper methods for interacting
 * with the FTB Chunks chunk claiming system.
 */
public class FTBChunks {

    /**
     * 获取FTB Chunks的区块声明管理器实例。
     * <p>
     * Gets the FTB Chunks claimed chunk manager instance.
     *
     * @return 区块声明管理器实例 / The claimed chunk manager instance
     */
    public static ClaimedChunkManagerImpl getInstance() {
        return ClaimedChunkManagerImpl.getInstance();
    }

    /**
     * 检查玩家是否可以编辑指定位置的方块。
     * <p>
     * Checks whether a player can edit a block at the specified position.
     *
     * @param player 要检查权限的玩家 / The player to check permissions for
     * @param pos 要检查的方块位置 / The block position to check
     * @return 如果交互应该被阻止则为true / true if the interaction should be prevented
     */
    public static boolean playerCanEdit(Player player, BlockPos pos) {
        return getInstance().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null);
    }

    /**
     * 获取指定位置的已声明区块信息。
     * <p>
     * Gets the claimed chunk at the specified position.
     *
     * @param level 世界实例 / The level instance
     * @param pos 方块位置 / The block position
     * @return 已声明的区块，如果该区块未被声明则返回null / The claimed chunk, or null if the chunk is not claimed
     */
    @Nullable
    public static ClaimedChunkImpl getClaimedChunk(Level level, BlockPos pos) {
        return getInstance().getChunk(new ChunkDimPos(level, pos));
    }
}
