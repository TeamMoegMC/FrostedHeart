/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.mixin.client.curios;

import com.teammoeg.frostedheart.content.climate.player.thermalitem.WarmStoneGateBPacketCounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/** Records decoded Curios stack packets without introducing a compile dependency on Curios internals. */
@Pseudo
@Mixin(targets = "top.theillusivec4.curios.common.network.server.sync.SPacketSyncStack", remap = false)
public abstract class SPacketSyncStackMixin {
    @Unique
    private static Field fh$entityIdField;
    @Unique
    private static Field fh$slotIdField;
    @Unique
    private static Field fh$curioIdField;
    @Unique
    private static Field fh$stackField;

    @Inject(method = "decode", at = @At("RETURN"), remap = false)
    private static void fh$recordDecodedStackPacket(
            FriendlyByteBuf buffer,
            CallbackInfoReturnable<Object> callbackInfo
    ) {
        if (!WarmStoneGateBPacketCounter.isEnabled()) {
            return;
        }
        try {
            Object packet = callbackInfo.getReturnValue();
            fh$resolveFields(packet.getClass());
            WarmStoneGateBPacketCounter.onCuriosStackPacket(
                    fh$entityIdField.getInt(packet),
                    (String) fh$curioIdField.get(packet),
                    fh$slotIdField.getInt(packet),
                    (ItemStack) fh$stackField.get(packet)
            );
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WarmStoneGateBPacketCounter.onProbeError(
                    "curios_sync_stack:" + exception.getClass().getSimpleName());
        }
    }

    @Unique
    private static void fh$resolveFields(Class<?> packetClass) throws NoSuchFieldException {
        if (fh$stackField != null) {
            return;
        }
        fh$entityIdField = fh$getAccessibleField(packetClass, "entityId");
        fh$slotIdField = fh$getAccessibleField(packetClass, "slotId");
        fh$curioIdField = fh$getAccessibleField(packetClass, "curioId");
        fh$stackField = fh$getAccessibleField(packetClass, "stack");
    }

    @Unique
    private static Field fh$getAccessibleField(
            Class<?> packetClass,
            String fieldName
    ) throws NoSuchFieldException {
        Field field = packetClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
