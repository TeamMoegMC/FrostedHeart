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

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillPower;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillWorkAccess;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillWorkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Uses the native break path: accepted START latches the tool and face, the pre-wear hook
 * records a pending debit, and only a successful Forge removeBlock settles it. A wheel
 * expands from the outer destroyBlock return after the center's drops/XP. Each neighbor
 * re-enters the same native method, but cannot start another cut.
 * Progress, permission, durability and loot still belong to the game mode.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin_StrataDrill implements StrataDrillWorkAccess {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos delayedDestroyPos;

    @Unique private StrataDrillAssembly fh$workAssembly;
    @Unique private ItemStack fh$workStack;
    @Unique private int fh$workSlot;
    @Unique private BlockPos fh$workPos;
    @Unique private long fh$workStart;
    // A successful instant break still needs a short visible work beat.
    @Unique private long fh$instantUntil;
    @Unique private BlockPos fh$expectedInstant;
    @Unique private StrataDrillPower.Removal fh$pendingRemoval;

    // Store the plane normal from accepted START, not the finishing packet's face.
    @Unique private Direction.Axis fh$wheelStartNormal;
    // Non-null only inside the native completion call and the bounded same-plane extras.
    @Unique private BlockPos fh$wheelCenter;
    @Unique private Direction.Axis fh$wheelCutNormal;
    @Unique private ItemStack fh$wheelStack;
    @Unique private StrataDrillAssembly fh$wheelAssembly;
    @Unique private int fh$wheelSlot;
    // Depth 1 is the planned native call; callbacks nested inside it cannot inherit the cut.
    @Unique private int fh$wheelDepth;
    @Unique private boolean fh$wheelCenterEligible;
    @Unique private boolean fh$wheelCenterRemoved;
    @Unique private boolean fh$cuttingWheelNeighbors;
    @Unique private BlockState fh$wheelExtraState;
    @Unique private BlockPos fh$wheelExtraPos;
    @Unique private boolean fh$inWheelExtraRemoval;
    @Unique private ItemStack fh$wheelExtraLoot;

    /** The first progress notification is inside START after Forge, reach, and vanilla restrictions. */
    @Inject(method = "handleBlockBreakAction",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V",
                    ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void fh$onAcceptedStart(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                    int maxBuildHeight, int sequence, CallbackInfo ci,
                                    PlayerInteractEvent.LeftClickBlock event, float progress, BlockState state) {
        if (progress > 0 && !state.isAir()) {
            fh$begin(pos, 0);
            fh$wheelStartNormal = fh$workAssembly != null && fh$workAssembly.wheel() && !player.isShiftKeyDown()
                    ? face.getAxis() : null;
        } else {
            fh$stop();
        }
    }

    /**
     * Capture only the two accepted START call sites (creative and insta-mine).
     * An independent destroyBlock call, or work denied by Forge, does not qualify.
     */
    @Inject(method = "handleBlockBreakAction", at = {
            @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V",
                    ordinal = 0),
            @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V",
                    ordinal = 1)
    })
    private void fh$expectInstant(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                  int maxBuildHeight, int sequence, CallbackInfo ci) {
        fh$wheelStartNormal = null;
        ItemStack stack = player.getMainHandItem();
        StrataDrillAssembly powered = StrataDrillPower.poweredAssembly(stack, player.isCreative());
        if (powered != null) {
            fh$expectedInstant = pos;
            fh$planWheelCut(pos, face.getAxis(), stack, powered);
        }
    }

    /** Ordinary STOP completion uses the accepted START cut plane. */
    @Inject(method = "handleBlockBreakAction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V",
            ordinal = 2))
    private void fh$planWheelStop(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                  int maxBuildHeight, int sequence, CallbackInfo ci) {
        fh$finishWheelStart(pos);
    }

    /** A STOP that queued completion still uses the direction and stack from its original START. */
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private void fh$planDelayedWheel(CallbackInfo ci) {
        fh$finishWheelStart(delayedDestroyPos);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void fh$enterWheelRemoval(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (fh$wheelCenter != null) ++fh$wheelDepth;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void fh$leaveWheelRemoval(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (fh$wheelCenter == null || --fh$wheelDepth != 0 || fh$cuttingWheelNeighbors) return;
        try {
            fh$cuttingWheelNeighbors = true;
            StrataDrillWorkAccess previous = StrataDrillPower.enterWheelNeighborScope(this);
            try {
                fh$cutWheelNeighbors(player.gameMode);
            } finally {
                StrataDrillPower.restoreWheelNeighborScope(previous);
            }
        } finally {
            fh$clearWheelCut();
        }
    }

    /** A BreakEvent replacement must not inherit the wheel's pending tool callback. */
    @Inject(method = "destroyBlock", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;onBlockStartBreak(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z",
            remap = false))
    private void fh$beforeExtraStartBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (fh$isWheelExtra(pos) && (!fh$ownsWheelStack(fh$wheelStack)
                || level.getBlockState(pos) != fh$wheelExtraState)) {
            fh$resyncBlock(pos);
            cir.setReturnValue(false);
        }
    }

    /** Use the same tool and harvest result as Forge's native mining call, after BreakEvent. */
    @Inject(method = "destroyBlock", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void fh$beforeToolDamage(BlockPos pos, CallbackInfoReturnable<Boolean> cir,
                                     BlockState originalState, int experience, BlockEntity blockEntity, Block block,
                                     ItemStack actualTool, ItemStack toolBeforeWear, boolean canHarvest) {
        boolean extra = fh$isWheelExtra(pos);
        StrataDrillAssembly powered = StrataDrillPower.poweredAssembly(actualTool, player.isCreative());
        if (extra && (!fh$ownsWheelStack(actualTool) || powered != fh$wheelAssembly || !canHarvest
                || blockEntity != null || originalState != fh$wheelExtraState
                || level.getBlockState(pos) != originalState)) {
            // A Forge callback may replace the held tool, prohibit harvest or change the cell.
            // The extra break must not touch that replacement or wear the previous tool.
            fh$resyncBlock(pos);
            cir.setReturnValue(false);
            return;
        }
        if (fh$isWheelCenter(pos)) {
            fh$wheelCenterEligible = fh$ownsWheelStack(actualTool) && powered == fh$wheelAssembly
                    && level.getBlockState(pos) == originalState;
        }
        if (powered != null && !player.isCreative()) {
            if (extra) fh$wheelExtraLoot = toolBeforeWear;
            fh$pendingRemoval = new StrataDrillPower.Removal(actualTool, pos, powered.steam(), extra,
                    fh$wheelDepth, fh$pendingRemoval);
        }
    }

    /** Use the empty hand's loot context without swapping the player's actual held item. */
    @ModifyArg(method = "destroyBlock", index = 5, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack fh$useBareHandLootWhenDry(ItemStack originalToolCopy) {
        // Vanilla copied it before any wear or charge was spent. The last powered block still
        // receives the original S04 loot and enchantments, even though the live stack is now empty.
        return StrataDrillAssembly.isDrill(originalToolCopy) && !StrataDrillPower.canRun(originalToolCopy, false)
                ? ItemStack.EMPTY : originalToolCopy;
    }

    // Forge adds this method to ServerPlayerGameMode; Mojang's namespace mappings do not contain it.
    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void fh$validateWheelRemoval(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
        if (fh$isWheelCenter(pos)) {
            fh$wheelCenterEligible = fh$wheelCenterEligible && canHarvest && fh$wheelCanReach(pos)
                    && fh$isWheelBlock(pos, level.getBlockState(pos));
        } else if (fh$isWheelExtra(pos) && (!canHarvest || level.getBlockState(pos) != fh$wheelExtraState)) {
            // If a native tool hook changed the block after the wear call, don't remove the
            // replacement and don't charge it. Vanilla has already run the tool callback.
            fh$finishPayment(pos, false);
            fh$resyncBlock(pos);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeBlock", at = @At("RETURN"), remap = false)
    private void fh$afterRemoval(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
        boolean removed = cir.getReturnValueZ();
        if (fh$isWheelCenter(pos)) {
            // destroyBlock returns true even when Forge's actual removal reports false.
            fh$wheelCenterRemoved = fh$wheelCenterEligible && removed;
        } else if (!removed && fh$wheelCenterRemoved && fh$wheelDepth == 1) {
            // Forge sent optimistic air to the mining client. A refused extra must not leave a ghost.
            fh$resyncBlock(pos);
        }
        if (removed && pos.equals(fh$expectedInstant)) {
            fh$begin(pos, level.getGameTime() + 2);
        }
        fh$finishPayment(pos, removed);
    }

    @Unique
    private void fh$finishPayment(BlockPos pos, boolean removed) {
        StrataDrillPower.Removal pending = fh$pendingRemoval;
        if (pending != null && pending.wheelDepth() == fh$wheelDepth && pos.equals(pending.pos())) {
            fh$pendingRemoval = pending.previous();
            if (removed) StrataDrillPower.spend(pending);
        }
    }

    @Unique
    private void fh$resyncBlock(BlockPos pos) {
        player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
    }

    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void fh$afterAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                int maxBuildHeight, int sequence, CallbackInfo ci) {
        fh$expectedInstant = null;
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            fh$wheelStartNormal = null;
        }
        if (fh$workAssembly == null) {
            return;
        }
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK && pos.equals(fh$workPos)) {
            // Even mods that cancel vanilla's ABORT must not leave a remote drill spinning.
            fh$stop();
        } else {
            // STOP may queue the vanilla delayed destroy; it does not necessarily end work.
            fh$updateWork();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void fh$afterVanillaTick(CallbackInfo ci) {
        if (fh$workAssembly != null) {
            fh$updateWork();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void fh$leavingLevel(ServerLevel nextLevel, CallbackInfo ci) {
        if (level != nextLevel) {
            // The old entity/world is untracked (and the client forgets its old record).
            // The player already points at the new level in setServerLevel: do not send an old edge there.
            fh$expectedInstant = null;
            fh$pendingRemoval = null;
            fh$clearWheelCut();
            fh$clearWork();
        }
    }

    @Unique
    private void fh$finishWheelStart(BlockPos center) {
        Direction.Axis normal = fh$wheelStartNormal;
        if (normal == null || !center.equals(fh$workPos) || fh$instantUntil != 0
                || player.getInventory().selected != fh$workSlot || player.getMainHandItem() != fh$workStack) return;

        ItemStack stack = fh$workStack;
        StrataDrillAssembly powered = StrataDrillPower.poweredAssembly(stack, false);
        if (powered != null && powered == fh$workAssembly) fh$planWheelCut(center, normal, stack, powered);
    }

    @Unique
    private void fh$planWheelCut(BlockPos center, Direction.Axis normal, ItemStack stack, StrataDrillAssembly powered) {
        if (fh$wheelCenter != null || !powered.wheel() || !fh$wheelMode()) return;
        fh$wheelCenter = center.immutable();
        fh$wheelCutNormal = normal;
        fh$wheelStack = stack;
        fh$wheelAssembly = powered;
        fh$wheelSlot = player.getInventory().selected;
    }

    @Unique
    private boolean fh$isWheelCenter(BlockPos pos) {
        return fh$wheelDepth == 1 && pos.equals(fh$wheelCenter);
    }

    @Unique
    private boolean fh$isWheelExtra(BlockPos pos) {
        return fh$inWheelExtraRemoval && fh$wheelDepth == 1 && pos.equals(fh$wheelExtraPos);
    }

    @Override
    public boolean fh$isDiscountedWheelStack(ItemStack stack) {
        if (fh$wheelExtraPos == null || fh$wheelDepth != (fh$inWheelExtraRemoval ? 1 : 0)) return false;
        return stack == fh$wheelExtraLoot || (stack == fh$wheelStack && fh$ownsWheelStack(stack));
    }

    @Unique
    private boolean fh$wheelMode() {
        return player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                && player.isAlive() && !player.isShiftKeyDown();
    }

    @Unique
    private boolean fh$ownsWheelStack(ItemStack tool) {
        return fh$wheelCenter != null && tool == fh$wheelStack && player.getMainHandItem() == tool
                && player.getInventory().selected == fh$wheelSlot && fh$wheelMode();
    }

    @Unique
    private boolean fh$wheelCanReach(BlockPos pos) {
        // mayInteract includes the world border and vanilla spawn protection. getChunkNow cannot load.
        return !level.isOutsideBuildHeight(pos) && player.canReach(pos, 1.5) && level.mayInteract(player, pos)
                && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    @Unique
    private boolean fh$isWheelBlock(BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && state.getDestroySpeed(level, pos) >= 0;
    }

    @Unique
    private void fh$cutWheelNeighbors(ServerPlayerGameMode gameMode) {
        BlockPos center = fh$wheelCenter;
        Direction.Axis normal = fh$wheelCutNormal;
        if (center == null || normal == null || !fh$wheelCenterRemoved) return;

        // No offset on the hit normal. Center's native drops/XP are already complete.
        // Fixed order for partial fuel: cardinal neighbors before corners; on walls, up then down first.
        Direction first = normal == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction second = normal == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        Direction oppositeSecond = second.getOpposite();
        BlockPos plusFirst = center.relative(first);
        BlockPos minusFirst = center.relative(first.getOpposite());
        if (!fh$tryWheelNeighbor(gameMode, plusFirst)
                || !fh$tryWheelNeighbor(gameMode, minusFirst)
                || !fh$tryWheelNeighbor(gameMode, center.relative(second))
                || !fh$tryWheelNeighbor(gameMode, center.relative(oppositeSecond))
                || !fh$tryWheelNeighbor(gameMode, plusFirst.relative(second))
                || !fh$tryWheelNeighbor(gameMode, plusFirst.relative(oppositeSecond))
                || !fh$tryWheelNeighbor(gameMode, minusFirst.relative(second))) return;
        fh$tryWheelNeighbor(gameMode, minusFirst.relative(oppositeSecond));
    }

    /** Skip unavailable cells without ending the cut; only loss of the tool or drive stops the scan. */
    @Unique
    private boolean fh$tryWheelNeighbor(ServerPlayerGameMode gameMode, BlockPos pos) {
        if (!fh$ownsWheelStack(fh$wheelStack)
                || StrataDrillPower.poweredWheelNeighbor(fh$wheelStack) != fh$wheelAssembly) return false;
        if (!fh$wheelCanReach(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if (!fh$isWheelBlock(pos, state)) return true;

        // The cheaper fuel threshold applies even in these native harvest and wear calls,
        // not just when spending. A reentrant removal is not the planned extra operation.
        fh$wheelExtraPos = pos;
        fh$wheelExtraState = state;
        StrataDrillPower.Removal previous = fh$pendingRemoval;
        try {
            // Skip poor-tier cells without posting BreakEvent. Forge rechecks after its callbacks.
            if (state.canHarvestBlock(level, pos, player) && fh$ownsWheelStack(fh$wheelStack)
                    && level.getBlockState(pos) == state) {
                fh$inWheelExtraRemoval = true;
                gameMode.destroyBlock(pos);
            }
        } finally {
            // Early cancellation may bypass native removeBlock. Do not keep an abandoned debit.
            fh$pendingRemoval = previous;
            fh$wheelDepth = 0;
            fh$inWheelExtraRemoval = false;
            fh$wheelExtraPos = null;
            fh$wheelExtraState = null;
            fh$wheelExtraLoot = null;
        }
        return true;
    }

    @Unique
    private void fh$clearWheelCut() {
        fh$wheelCenter = null;
        fh$wheelCutNormal = null;
        fh$wheelStack = null;
        fh$wheelAssembly = null;
        fh$wheelDepth = 0;
        fh$wheelCenterEligible = false;
        fh$wheelCenterRemoved = false;
        fh$cuttingWheelNeighbors = false;
        fh$inWheelExtraRemoval = false;
        fh$wheelExtraPos = null;
        fh$wheelExtraState = null;
        fh$wheelExtraLoot = null;
    }

    @Unique
    private void fh$begin(BlockPos pos, long instantUntil) {
        ItemStack stack = player.getMainHandItem();
        StrataDrillAssembly assembly = StrataDrillPower.poweredAssembly(stack, player.isCreative());
        if (assembly == null || player.isSpectator()) {
            fh$stop();
            return;
        }
        if (fh$workAssembly != assembly || fh$workStack != stack || fh$workSlot != player.getInventory().selected) {
            fh$stop();
            fh$workAssembly = assembly;
            fh$workStack = stack;
            fh$workSlot = player.getInventory().selected;
            fh$workStart = level.getGameTime();
            fh$workPos = pos.immutable();
            fh$instantUntil = instantUntil;
            fh$broadcast(true, fh$workStart);
        } else {
            fh$workPos = pos.immutable();
            fh$instantUntil = instantUntil;
        }
    }

    @Unique
    private void fh$updateWork() {
        ItemStack held = player.getMainHandItem();
        if (held != fh$workStack || player.getInventory().selected != fh$workSlot || player.isSpectator()
                || !player.isAlive() || StrataDrillPower.poweredAssembly(held, player.isCreative()) != fh$workAssembly) {
            fh$stop();
            return;
        }
        if (fh$instantUntil != 0) {
            if (level.getGameTime() >= fh$instantUntil) {
                fh$stop();
            }
            return;
        }
        boolean hasAttempt = (isDestroyingBlock && destroyPos.equals(fh$workPos))
                || (hasDelayedDestroy && delayedDestroyPos.equals(fh$workPos));
        if (!hasAttempt || !player.canReach(fh$workPos, 1.5) || level.isEmptyBlock(fh$workPos)) {
            fh$stop();
        }
    }

    @Unique
    private void fh$stop() {
        if (fh$workAssembly != null) {
            fh$clearWork();
            fh$broadcast(false, level.getGameTime());
        }
    }

    @Unique
    private void fh$clearWork() {
        fh$wheelStartNormal = null;
        fh$workAssembly = null;
        fh$workStack = null;
        fh$workPos = null;
        fh$instantUntil = 0;
    }

    @Unique
    private void fh$broadcast(boolean running, long tick) {
        FHNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new StrataDrillWorkPacket(player.getId(), running, tick));
    }

    @Override
    public void fh$sendStrataDrillSnapshot(ServerPlayer observer) {
        if (fh$workAssembly != null && !player.isSpectator() && player.isAlive()
                && player.getMainHandItem() == fh$workStack && player.getInventory().selected == fh$workSlot
                && StrataDrillPower.poweredAssembly(fh$workStack, player.isCreative()) == fh$workAssembly) {
            FHNetwork.INSTANCE.sendPlayer(observer, new StrataDrillWorkPacket(player.getId(), true, fh$workStart));
        }
    }
}
