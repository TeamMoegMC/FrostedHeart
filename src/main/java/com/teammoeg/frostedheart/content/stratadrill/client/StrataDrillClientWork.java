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

package com.teammoeg.frostedheart.content.stratadrill.client;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillPower;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Visual timing only; no mining, inventory or gameplay authority on the client. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class StrataDrillClientWork {
    private static final int PREDICT_TICKS = 4;
    // The completion tick and five vanilla destroy-delay ticks before the next START.
    private static final int BETWEEN_BLOCKS_TICKS = 6;
    private static final Int2ObjectOpenHashMap<MotionClock> REMOTE = new Int2ObjectOpenHashMap<>();
    private static final MotionClock LOCAL = new MotionClock();

    private static ClientLevel level;
    private static ItemStack localDrill = ItemStack.EMPTY;
    private static StrataDrillAssembly localAssembly;
    private static int localSlot = -1;
    private static BlockPos predictedTarget;
    private static Block predictedBlock;
    private static long predictUntil;
    private static long continueUntil;
    private static boolean serverRunning;

    private StrataDrillClientWork() {}

    private static void selectLevel(ClientLevel next) {
        if (level == next) return;
        level = next;
        REMOTE.clear();
        localDrill = ItemStack.EMPTY;
        localAssembly = null;
        localSlot = -1;
        clearLocalWork();
    }

    private static void clearLocalWork() {
        LOCAL.clear();
        clearMiningIntent();
        serverRunning = false;
    }

    private static void clearMiningIntent() {
        predictedTarget = null;
        predictedBlock = null;
        predictUntil = 0;
        continueUntil = 0;
    }

    private static boolean holdsLocalTool(AbstractClientPlayer player, ItemStack stack) {
        return localAssembly != null && localSlot == player.getInventory().selected
                && StrataDrillAssembly.sameVisualTool(localDrill, stack);
    }

    /** Called on the main client thread by the FH work-edge packet after ordinary entity pairing. */
    public static void accept(int entityId, boolean running, long serverTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft.level;
        selectLevel(current);
        if (current == null || minecraft.player == null) return;
        if (entityId == minecraft.player.getId()) {
            // Keep the local predicted clock continuous; the server acknowledgement sets authority, not a new pose.
            if (!holdsLocalTool(minecraft.player, minecraft.player.getMainHandItem())) return;
            // A late acknowledgement must not re-lift the drill after the local button was released.
            // A STOP can refer to the previous block, so it only clears the server's confirmation.
            serverRunning = running && minecraft.options.keyAttack.isDown();
            return;
        }
        if (!(current.getEntity(entityId) instanceof AbstractClientPlayer)) return;
        MotionClock clock = REMOTE.get(entityId);
        if (clock == null) {
            if (!running) return;
            clock = new MotionClock();
            REMOTE.put(entityId, clock);
        }
        clock.setRunning(running, serverTick);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft.level;
        selectLevel(current);
        LocalPlayer player = minecraft.player;
        if (current == null || player == null || minecraft.gameMode == null) return;

        ItemStack main = player.getMainHandItem();
        StrataDrillAssembly assembly = StrataDrillAssembly.read(main);
        if (assembly == null) {
            if (localAssembly != null) {
                localDrill = ItemStack.EMPTY;
                localAssembly = null;
                localSlot = -1;
                clearLocalWork();
            }
            return;
        }
        if (assembly != localAssembly || !holdsLocalTool(player, main)) {
            clearLocalWork();
            localAssembly = assembly;
            localSlot = player.getInventory().selected;
        }
        localDrill = main;

        long now = current.getGameTime();
        boolean canUse = !player.isSpectator() && !player.isUsingItem() && player.isAlive();
        boolean pressingMine = canUse && minecraft.screen == null && minecraft.options.keyAttack.isDown();
        BlockPos target = pressingMine && minecraft.hitResult instanceof BlockHitResult block
                && block.getType() == HitResult.Type.BLOCK ? block.getBlockPos() : null;

        boolean predicts = false;
        if (target == null) {
            clearMiningIntent();
            serverRunning = false;
        } else if (minecraft.gameMode.isDestroying()) {
            if (!target.equals(predictedTarget)) {
                predictedTarget = target.immutable();
                predictedBlock = current.getBlockState(target).getBlock();
                predictUntil = now + PREDICT_TICKS;
            }
            predicts = now < predictUntil;
        } else if (predictedTarget != null && LOCAL.running
                && current.getBlockState(predictedTarget).getBlock() != predictedBlock) {
            // Keep the pose across a locally observed completion, not an arbitrary STOP, abort or denied START.
            continueUntil = now + BETWEEN_BLOCKS_TICKS;
            predictedTarget = null;
            predictedBlock = null;
        }
        // A cancelled START may not send an animation edge. Never leave a local prediction running indefinitely.
        boolean betweenBlocks = target != null && now < continueUntil;
        boolean active = (serverRunning || predicts || betweenBlocks)
                && StrataDrillPower.canRun(main, player.isCreative());
        if (!active) continueUntil = 0;
        LOCAL.setRunning(active, now);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel() == level) {
            REMOTE.remove(event.getEntity().getId());
            if (event.getEntity() == Minecraft.getInstance().player) selectLevel(null);
        }
    }

    /** A finite [0, 1) phase for moving parts; NaN means to draw the ordinary static model. */
    static float phase(AbstractClientPlayer actor, float partialTick) {
        MotionClock clock = clockFor(actor);
        return clock == null ? Float.NaN : clock.phase(level.getGameTime(), partialTick);
    }

    /** Weight placed into the cut, separate from shaft angle but on the same accepted work edge. */
    static float loadAmount(AbstractClientPlayer actor, float partialTick) {
        MotionClock clock = clockFor(actor);
        return clock == null ? 0 : clock.loadAmount(level.getGameTime(), partialTick);
    }

    private static MotionClock clockFor(AbstractClientPlayer actor) {
        if (actor.level() != level || level == null) return null;
        if (actor == Minecraft.getInstance().player) {
            return holdsLocalTool(actor, actor.getMainHandItem()) ? LOCAL : null;
        }
        return REMOTE.get(actor.getId());
    }

    /**
     * One scalar clock drives all the rigid parts. Reversing an edge starts from the existing motion;
     * slowing down parks on an authored rest revolution rather than snapping to the static model.
     */
    static final class MotionClock {
        private static final double TURNS_PER_TICK = 0.105;
        private static final double START_TICKS = 4;

        private boolean running;
        private long edgeTick;
        private double edgePhase;
        private double initialSpeed;
        private double stopTravel;
        private double stopTicks;
        private double edgeLoad;

        void clear() {
            running = false;
            edgeTick = 0;
            edgePhase = 0;
            initialSpeed = 0;
            stopTravel = 0;
            stopTicks = 0;
            edgeLoad = 0;
        }

        void setRunning(boolean next, long tick) {
            if (next == running || tick < edgeTick) return;
            double t = tick - edgeTick;
            double angle = turns(t);
            double speed = speed(t);
            edgeLoad = load(t);
            edgePhase = angle - Math.floor(angle);
            initialSpeed = speed;
            edgeTick = tick;
            running = next;
            if (!running) {
                if (edgePhase < 0.00001 && speed < 0.00001) {
                    stopTicks = 0;
                    return;
                }
                stopTravel = 1 - edgePhase;
                if (stopTravel < speed * (5.0 / 3.0)) stopTravel += 1;
                stopTicks = Math.max(5, Math.min(15, 2 * stopTravel / Math.max(speed, TURNS_PER_TICK)));
            }
        }

        float phase(long tick, float partialTick) {
            double t = Math.max(0, tick - edgeTick + (double) partialTick);
            if (!running && t >= stopTicks) return Float.NaN;
            double angle = turns(t);
            return (float) (angle - Math.floor(angle));
        }

        float loadAmount(long tick, float partialTick) {
            return (float) load(Math.max(0, tick - edgeTick + (double) partialTick));
        }

        private double load(double t) {
            // Brace into the block once, then keep the load steady. Unload as the head parks.
            double duration = running ? 7 : Math.min(7, stopTicks);
            if (t >= duration) return running ? 1 : 0;
            double u = t / duration;
            double blend = u * u * (3 - 2 * u);
            return edgeLoad + ((running ? 1 : 0) - edgeLoad) * blend;
        }

        private double turns(double t) {
            if (running) {
                double ramp = Math.min(t, START_TICKS);
                return edgePhase + ramp * initialSpeed
                        + (TURNS_PER_TICK - initialSpeed) * ramp * ramp / (2 * START_TICKS)
                        + Math.max(0, t - START_TICKS) * TURNS_PER_TICK;
            }
            if (t >= stopTicks) return Math.ceil(edgePhase + stopTravel);
            double u = t / stopTicks;
            double u2 = u * u;
            // Cubic Hermite, same angular speed at exit, zero speed at the next rest pose.
            return edgePhase + (3 * u2 - 2 * u2 * u) * stopTravel
                    + (u2 * u - 2 * u2 + u) * stopTicks * initialSpeed;
        }

        private double speed(double t) {
            if (running) {
                return initialSpeed + (TURNS_PER_TICK - initialSpeed) * Math.min(1, t / START_TICKS);
            }
            if (t >= stopTicks) return 0;
            double u = t / stopTicks;
            return Math.max(0, (6 * u - 6 * u * u) * stopTravel / stopTicks
                    + (3 * u * u - 4 * u + 1) * initialSpeed);
        }
    }
}
