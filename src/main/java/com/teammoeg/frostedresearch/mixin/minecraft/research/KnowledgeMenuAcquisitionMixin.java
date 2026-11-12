package com.teammoeg.frostedresearch.mixin.minecraft.research;

import com.teammoeg.frostedresearch.knowledge.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(AbstractContainerMenu.class)
public abstract class KnowledgeMenuAcquisitionMixin {
    @Shadow public abstract ItemStack getCarried();
    @Unique private List<ItemStack> frostedresearch$owned;
    @Inject(method = "clicked", at = @At("HEAD"))
    private void frostedresearch$before(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        frostedresearch$owned = player instanceof ServerPlayer && KnowledgeBehaviorBridge.interested(IdeaAcquisitionService.ITEM_OBTAINED)
            ? KnowledgeBehaviorBridge.owned(player, getCarried()) : null;
    }
    @Inject(method = "clicked", at = @At("RETURN"))
    private void frostedresearch$after(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        var before = frostedresearch$owned; frostedresearch$owned = null;
        if (before != null && player instanceof ServerPlayer server)
            KnowledgeBehaviorBridge.obtained(server, KnowledgeBehaviorBridge.gained(before, KnowledgeBehaviorBridge.owned(player, getCarried())));
    }
}
