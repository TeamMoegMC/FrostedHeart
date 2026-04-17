package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ValveHandleBlock.class)
public class MixinValveHandleBlock {

    /**
     * @reason Disable fake player from making energy
     */
    @Inject(at=@At("HEAD"), method="use", cancellable=true)
    public void use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof FakePlayer) {
            world.destroyBlock(pos, true);
            ci.setReturnValue(InteractionResult.FAIL);
            if (FHConfig.isSpecialMode() && world instanceof ServerLevel sl && sl.hasNearbyAlivePlayer(pos.getX(), pos.getY(), pos.getZ(), 6)) sl.explode(new DeployerFakePlayer(sl, null), pos.getX(), pos.getY(), pos.getZ(), 6, Level.ExplosionInteraction.NONE);
        } else if (player.getFoodData().getFoodLevel() < 4) {
            if (player.getCommandSenderWorld().isClientSide)
                player.displayClientMessage(Lang.translateMessage("crank.feel_hunger"), true);
            ci.setReturnValue(InteractionResult.FAIL);
        }
    }
}
