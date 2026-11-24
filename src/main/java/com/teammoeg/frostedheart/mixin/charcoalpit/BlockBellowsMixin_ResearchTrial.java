package com.teammoeg.frostedheart.mixin.charcoalpit;
import charcoalPit.block.BlockBellows;
import com.teammoeg.frostedresearch.knowledge.IdeaAcquisitionService;
import com.teammoeg.frostedresearch.knowledge.experiment.TuyereTrials;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBellows.class)
public class BlockBellowsMixin_ResearchTrial {
    @Unique
    private static final ResourceLocation frost$MANUAL_BLOW = new ResourceLocation("frostedheart:manual_bellows_blow");
    @Unique
    private static final ResourceLocation frost$BELLOWS = new ResourceLocation("charcoal_pit:bellows");
    @Unique
    private final Map<ServerLevel, Map<BlockPos, ManualPress>> frost$presses = new WeakHashMap<>();

    private record ManualPress(UUID player, long tick) {}

    @Inject(method = "use", at = @At("HEAD"))
    private void rememberManualPress(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)
                || !state.is(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(frost$BELLOWS))
                || state.getValue(BlockBellows.PUSH) || server.getBlockTicks().hasScheduledTick(pos, state.getBlock())) return;
        long now = server.getGameTime();
        var presses = frost$presses.computeIfAbsent(server, ignored -> new HashMap<>());
        // A broken or unloaded bellows can leave no scheduled callback.
        presses.values().removeIf(press -> press.tick() < now);
        presses.put(pos.immutable(), new ManualPress(player.getUUID(), now + 10));
    }

    @Inject(method = "blow", remap = false, at = @At(value = "INVOKE",
        target = "LcharcoalPit/tile/TileBloomery;blow(I)V", shift = At.Shift.AFTER, remap = false))
    private void onManualAirDelivery(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(level instanceof ServerLevel server)) return;
        var presses = frost$presses.get(server);
        var press = presses == null ? null : presses.remove(pos);
        if (press == null || press.tick() != server.getGameTime()) return;
        ServerPlayer player = server.getServer().getPlayerList().getPlayer(press.player());
        if (player != null && player.serverLevel() == server)
            IdeaAcquisitionService.emit(player, frost$MANUAL_BLOW, Optional.empty());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void clearManualPress(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        var presses = frost$presses.get(level);
        if (presses != null) presses.remove(pos);
    }

    @Inject(method="blow",at=@At("HEAD"),cancellable=true,remap=false)
    private void researchTrial(Level level,BlockPos pos,BlockState state,CallbackInfo ci) {
        if(level instanceof ServerLevel server && TuyereTrials.tryBlow(server,pos,state)) ci.cancel();
    }
}
