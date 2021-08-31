/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client.render;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.FHItems;
import com.teammoeg.frostedheart.nbt.FHNBT;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FHBipedLayerRenderer<E extends LivingEntity, M extends BipedModel<E>> extends LayerRenderer<E, M> {
    public static boolean rendersAssigned = false;
    public static Map<UUID, Pair<ItemStack, Integer>> HEATER_VEST_PLAYERS = new HashMap<>();

    public FHBipedLayerRenderer(IEntityRenderer<E, M> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, E living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chest = living.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (!chest.isEmpty() && (chest.getItem() == FHItems.Misc.heater_vest || ItemNBTHelper.hasKey(chest, FHNBT.NBT_HEATER_VEST))) {
            ItemStack powerpack = chest.getItem() == FHItems.Misc.heater_vest ? chest : ItemNBTHelper.getItemStack(chest, FHNBT.NBT_HEATER_VEST);
            addWornPowerpack(living, powerpack);
        }
//        else if(ModList.get().isLoaded("curios"))
//        {
//            ItemStack powerpack = CuriosCompatModule.getPowerpack(living);
//            if(!powerpack.isEmpty())
//                addWornPowerpack(living, powerpack);
//        }

        if (HEATER_VEST_PLAYERS.containsKey(living.getUniqueID())) {
            Pair<ItemStack, Integer> entry = HEATER_VEST_PLAYERS.get(living.getUniqueID());
            renderPowerpack(entry.getLeft(), matrixStackIn, bufferIn, packedLightIn, living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            int time = entry.getValue() - 1;
            if (time <= 0)
                HEATER_VEST_PLAYERS.remove(living.getUniqueID());
            else
                HEATER_VEST_PLAYERS.put(living.getUniqueID(), Pair.of(entry.getLeft(), time));
        }
    }

    public static void addWornPowerpack(LivingEntity living, ItemStack powerpack) {
        HEATER_VEST_PLAYERS.put(living.getUniqueID(), Pair.of(powerpack, 5));
    }

    private void renderPowerpack(ItemStack powerpack, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, E living, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!powerpack.isEmpty()) {
            BipedModel<E> model = FHItems.Misc.heater_vest.getArmorModel(living, powerpack, EquipmentSlotType.CHEST, null);
            if (model != null) {
                model.setRotationAngles(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                RenderType type = model.getRenderType(
                        new ResourceLocation(FHItems.Misc.heater_vest.getArmorTexture(powerpack, living, EquipmentSlotType.CHEST, null))
                );
                model.render(matrixStackIn, bufferIn.getBuffer(type), packedLightIn, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            }
        }
    }
}
