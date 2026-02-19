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

public class FTBChunks {

    public static ClaimedChunkManagerImpl getInstance() {
        return ClaimedChunkManagerImpl.getInstance();
    }

    public static boolean playerCanEdit(Player player, BlockPos pos) {
        return getInstance().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null);
    }

    @Nullable
    public static ClaimedChunkImpl getClaimedChunk(Level level, BlockPos pos) {
        return getInstance().getChunk(new ChunkDimPos(level, pos));
    }
}
