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

package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.item.MagnifyingGlassItem;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Action;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket.Method;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationStatePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** One observation per physical key/use gesture; inventory observation keeps the original menu open. */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class ObservationClient {
    public static final KeyMapping OBSERVE = new KeyMapping("key.frostedresearch.observe", KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.frostedresearch");
    private static boolean wasKeyHeld, wasUseHeld, waitingForCancel;
    private static Trigger queuedPress, trigger;
    private static ObservationActionPacket request;
    private static ItemStack slotSample = ItemStack.EMPTY;

    private enum Trigger {KEY, USE}

    private ObservationClient() {
    }

    @Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OBSERVE);
        }
    }

    private static boolean held(KeyMapping mapping) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.isWindowActive()) return false;
        InputConstants.Key key = mapping.getKey();
        if (key.getValue() < 0) return false;
        long window = minecraft.getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }

    private static boolean held(Trigger input) {
        Minecraft mc = Minecraft.getInstance();
        if (input == Trigger.KEY) return held(OBSERVE);
        if (mc.screen instanceof AbstractContainerScreen<?>)
            return mc.player != null && mc.isWindowActive()
                    && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        return held(mc.options.keyUse);
    }

    static Component inputHint() {
        if (trigger == Trigger.USE && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)
            return Component.translatable("key.mouse.right");
        return (trigger == Trigger.USE ? Minecraft.getInstance().options.keyUse : OBSERVE).getTranslatedKeyMessage();
    }

    private static Method worldMethod() {
        var player = Minecraft.getInstance().player;
        if (player == null) return Method.KEY;
        if (player.getMainHandItem().getItem() instanceof MagnifyingGlassItem) return Method.MAIN_HAND_GLASS;
        if (player.getOffhandItem().getItem() instanceof MagnifyingGlassItem) return Method.OFF_HAND_GLASS;
        return Method.KEY;
    }

    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && worldMethod() != Method.KEY) {
            // Consume before block/entity interaction: observing a chest must not open it.
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        while (OBSERVE.consumeClick()) { /* Discard OS key-repeat clicks. */ }
        InventoryObservationOverlay.tick();
        boolean keyDown = held(Trigger.KEY), useDown = held(Trigger.USE);
        if (request != null && (!held(trigger) || !contextValid())) cancelHeld();
        if (queuedPress != null && !held(queuedPress)) queuedPress = null;
        if (keyDown && !wasKeyHeld) press(Trigger.KEY);
        else if (useDown && !wasUseHeld) press(Trigger.USE);
        if (queuedPress != null && !waitingForCancel) {
            Trigger next = queuedPress;
            queuedPress = null;
            start(next);
        }
        wasKeyHeld = keyDown;
        wasUseHeld = useDown;
    }

    private static void press(Trigger input) {
        if (waitingForCancel) queuedPress = input;
        else start(input);
    }

    private static void start(Trigger input) {
        Minecraft mc = Minecraft.getInstance();
        if (request != null || mc.player == null || InventoryObservationOverlay.hasRecord()) return;
        ObservationActionPacket next;
        if (mc.screen instanceof AbstractContainerScreen<?> screen) {
            Slot slot = physicalSlot(screen);
            // Only server-backed physical slots, not creative catalogue/JEI ghost entries.
            if (slot == null || !slot.isActive()
                    || !ItemRetrieval.canObserve(slot.getItem())) return;
            Method method = input == Trigger.KEY ? Method.KEY : screen instanceof CreativeModeInventoryScreen
                    ? Method.CREATIVE_CURSOR_GLASS : Method.CURSOR_GLASS;
            if (input == Trigger.USE && !(screen.getMenu().getCarried().getItem() instanceof MagnifyingGlassItem)) return;
            next = new ObservationActionPacket(Action.SLOT_ITEM, BlockPos.ZERO, slot.index, serverMenu(screen).containerId, method);
            slotSample = slot.getItem().copy();
            InventoryObservationOverlay.begin(screen);
        } else {
            if (mc.screen != null) return;
            Method method = input == Trigger.KEY ? Method.KEY : worldMethod();
            if (input == Trigger.USE && method == Method.KEY) return;
            HitResult hit = worldTarget();
            if (hit instanceof EntityHitResult entity) {
                next = new ObservationActionPacket(Action.ENTITY, BlockPos.ZERO, entity.getEntity().getId(), -1, method);
            } else if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                next = new ObservationActionPacket(Action.BLOCK, block.getBlockPos(), -1, -1, method);
            } else {
                int hand = method == Method.MAIN_HAND_GLASS ? 1 : method == Method.OFF_HAND_GLASS ? 0
                        : mc.player.getMainHandItem().isEmpty() ? 1 : 0;
                ItemStack item = hand == 0 ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
                if (!ItemRetrieval.canObserve(item)) return;
                next = new ObservationActionPacket(Action.HELD_ITEM, BlockPos.ZERO, hand, -1, method);
            }
        }
        trigger = input;
        request = next;
        FRNetwork.INSTANCE.sendToServer(next);
    }

    private static Slot physicalSlot(AbstractContainerScreen<?> screen) {
        var player = Minecraft.getInstance().player;
        Slot hovered = screen.getSlotUnderMouse();
        if (player == null || hovered == null) return null;
        if (screen instanceof CreativeModeInventoryScreen)
            return player.inventoryMenu.slots.stream().filter(slot -> hovered.isSameInventory(slot)
                    && hovered.getSlotIndex() == slot.getSlotIndex()).findFirst().orElse(null);
        if (screen.getMenu() == player.containerMenu) return hovered;
        return null;
    }

    private static AbstractContainerMenu serverMenu(AbstractContainerScreen<?> screen) {
        var player = Minecraft.getInstance().player;
        // Creative replaces containerMenu locally; its ItemPickerMenu has no server counterpart.
        return screen instanceof CreativeModeInventoryScreen ? player.inventoryMenu : player.containerMenu;
    }

    /** Dropped items are not vanilla pickable entities; clip their small boxes up to the nearest ordinary hit. */
    static HitResult worldTarget() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        double reach = Math.max(mc.player.getBlockReach(), mc.player.getEntityReach());
        Vec3 end = eye.add(mc.player.getViewVector(1).scale(reach));
        HitResult nearest = mc.hitResult;
        double distance = nearest == null || nearest.getType() == HitResult.Type.MISS ? reach * reach : eye.distanceToSqr(nearest.getLocation());
        for (ItemEntity dropped : mc.level.getEntitiesOfClass(ItemEntity.class,
                mc.player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1), ItemEntity::isAlive)) {
            var intersection = dropped.getBoundingBox().inflate(0.15).clip(eye, end);
            if (intersection.isPresent() && eye.distanceToSqr(intersection.get()) < distance) {
                distance = eye.distanceToSqr(intersection.get());
                nearest = new EntityHitResult(dropped, intersection.get());
            }
        }
        return nearest;
    }

    private static boolean contextValid() {
        Minecraft mc = Minecraft.getInstance();
        if (request.action() != Action.SLOT_ITEM)
            return mc.screen == null || mc.screen instanceof ObservationProgressScreen;
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen) || !InventoryObservationOverlay.isHost(screen)
                || serverMenu(screen).containerId != request.containerId()) return false;
        Slot slot = physicalSlot(screen);
        return slot != null && slot.index == request.entityId() && ItemStack.matches(slotSample, slot.getItem())
                && (trigger == Trigger.KEY || screen.getMenu().getCarried().getItem() instanceof MagnifyingGlassItem);
    }

    static boolean inventoryActive() {
        return request != null && request.action() == Action.SLOT_ITEM;
    }

    public static void cancelHeld() {
        if (request == null) return;
        request = null;
        waitingForCancel = true;
        InventoryObservationOverlay.clear();
        FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(Action.CANCEL));
        if (Minecraft.getInstance().screen instanceof ObservationProgressScreen)
            Minecraft.getInstance().setScreen(null);
    }

    public static void receive(ObservationStatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        switch (packet.state()) {
            case STARTED -> {
                if (request != null && held(trigger) && contextValid()) {
                    if (inventoryActive()) InventoryObservationOverlay.started(packet.durationTicks());
                    else mc.setScreen(new ObservationProgressScreen(packet.position(), packet.entityId(), packet.durationTicks(),
                            packet.itemTarget() && request.action() == Action.HELD_ITEM));
                } else if (request != null) cancelHeld();
            }
            case OBSERVED, PREVIEW -> {
                if (packet.state() == ObservationStatePacket.State.OBSERVED
                        && (request == null || !held(trigger) || !contextValid())) {
                    if (request != null) cancelHeld();
                    return;
                }
                boolean inventory = inventoryActive();
                request = null;
                Observation.CODEC.parse(NbtOps.INSTANCE, packet.record()).result().ifPresent(observation -> {
                    if (inventory) InventoryObservationOverlay.record(observation);
                    else mc.setScreen(new ObservationRecordScreen(observation));
                });
            }
            case CANCELLED, ACCEPTED -> {
                request = null;
                waitingForCancel = false;
                InventoryObservationOverlay.clear();
                if (mc.screen instanceof ObservationProgressScreen || mc.screen instanceof ObservationRecordScreen)
                    mc.setScreen(null);
                if (mc.player != null && !packet.message().isBlank() && !packet.message().equals("cancelled"))
                    mc.player.displayClientMessage(Component.translatable("knowledge.frostedresearch.observation." + packet.message()), true);
            }
            case REJECTED -> {
                if (mc.screen instanceof ObservationRecordScreen record) record.rejected(packet.message());
                InventoryObservationOverlay.rejected(packet.message());
            }
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        wasKeyHeld = wasUseHeld = waitingForCancel = false;
        queuedPress = trigger = null;
        request = null;
        InventoryObservationOverlay.clear();
    }
}
