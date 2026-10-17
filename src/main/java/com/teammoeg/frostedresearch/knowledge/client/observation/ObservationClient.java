/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationStatePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * One observation per held-key gesture, including release/response races.
 */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class ObservationClient {
    public static final KeyMapping OBSERVE = new KeyMapping("key.frostedresearch.observe", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.frostedresearch");
    private static boolean wasHeld;
    private static boolean active;
    private static boolean waitingForCancel;
    private static boolean queuedPress;

    private ObservationClient() {
    }

    @Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OBSERVE);
        }
    }

    private static boolean held() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.isWindowActive()) return false;
        InputConstants.Key key = OBSERVE.getKey();
        if (key.getValue() < 0) return false;
        long window = minecraft.getWindow().getWindow();
        // KeyConflictContext.IN_GAME is inactive over the progress screen; read the physical gesture.
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OBSERVE.consumeClick()) { /* Discard OS key-repeat clicks. */ }
        boolean down = held();
        if (!down) {
            queuedPress = false;
            if (active) cancelHeld();
        } else if (!wasHeld) {
            if (waitingForCancel) queuedPress = true;
            else start();
        }
        if (down && queuedPress && !waitingForCancel) {
            queuedPress = false;
            start();
        }
        wasHeld = down;
    }

    private static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active || minecraft.player == null || minecraft.screen != null) return;
        ObservationActionPacket request;
        if (minecraft.hitResult instanceof EntityHitResult entity) {
            request = new ObservationActionPacket(ObservationActionPacket.Action.ENTITY, BlockPos.ZERO, entity.getEntity().getId());
        } else if (minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            request = new ObservationActionPacket(ObservationActionPacket.Action.BLOCK, block.getBlockPos(), -1);
        } else return;
        active = true;
        FRNetwork.INSTANCE.sendToServer(request);
    }

    public static void cancelHeld() {
        if (!active) return;
        active = false;
        waitingForCancel = true;
        FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.CANCEL));
        if (Minecraft.getInstance().screen instanceof ObservationProgressScreen)
            Minecraft.getInstance().setScreen(null);
    }

    public static void receive(ObservationStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (packet.state()) {
            case STARTED -> {
                if (active && held())
                    minecraft.setScreen(new ObservationProgressScreen(packet.position(), packet.entityId()));
                else if (active) cancelHeld();
            }
            case OBSERVED, PREVIEW -> {
                if (packet.state() == ObservationStatePacket.State.OBSERVED && (!active || !held())) {
                    if (active) cancelHeld();
                    return;
                }
                active = false;
                Observation.CODEC.parse(NbtOps.INSTANCE, packet.record()).result()
                        .ifPresent(observation -> minecraft.setScreen(new ObservationRecordScreen(observation)));
            }
            case CANCELLED, ACCEPTED -> {
                active = false;
                waitingForCancel = false;
                if (minecraft.screen instanceof ObservationProgressScreen || minecraft.screen instanceof ObservationRecordScreen)
                    minecraft.setScreen(null);
                if (minecraft.player != null && !packet.message().isBlank() && !packet.message().equals("cancelled"))
                    minecraft.player.displayClientMessage(Component.translatable("knowledge.frostedresearch.observation." + packet.message()), true);
            }
            case REJECTED -> {
                if (minecraft.screen instanceof ObservationRecordScreen record) record.rejected(packet.message());
            }
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        wasHeld = active = waitingForCancel = queuedPress = false;
    }
}
