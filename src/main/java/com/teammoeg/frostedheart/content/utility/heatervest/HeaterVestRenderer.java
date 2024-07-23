/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility.heatervest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.util.FHUtils;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public class HeaterVestRenderer<E extends LivingEntity, M extends HumanoidModel<E>> extends RenderLayer<E, M> {
    public static boolean rendersAssigned = false;
    public static Map<UUID, Pair<ItemStack, Integer>> HEATER_VEST_PLAYERS = new HashMap<>();

    public static void addWornHeaterVest(LivingEntity living, ItemStack heaterVest) {
        HEATER_VEST_PLAYERS.put(living.getUUID(), Pair.of(heaterVest, 5));
    }

    public HeaterVestRenderer(RenderLayerParent<E, M> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, E living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chest = living.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && (chest.getItem() == FHItems.heater_vest.get() || ItemNBTHelper.hasKey(chest, FHUtils.NBT_HEATER_VEST))) {
            ItemStack heaterVest = chest.getItem() == FHItems.heater_vest.get() ? chest : ItemNBTHelper.getItemStack(chest, FHUtils.NBT_HEATER_VEST);
            addWornHeaterVest(living, heaterVest);
        } else if (ModList.get().isLoaded("curios")) {
            ItemStack heaterVest = CuriosCompat.getHeaterVest(living);
            if (!heaterVest.isEmpty())
                addWornHeaterVest(living, heaterVest);
        }

        if (HEATER_VEST_PLAYERS.containsKey(living.getUUID())) {
            Pair<ItemStack, Integer> entry = HEATER_VEST_PLAYERS.get(living.getUUID());
            renderHeaterVest(entry.getLeft(), matrixStackIn, bufferIn, packedLightIn, living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            int time = entry.getValue() - 1;
            if (time <= 0)
                HEATER_VEST_PLAYERS.remove(living.getUUID());
            else
                HEATER_VEST_PLAYERS.put(living.getUUID(), Pair.of(entry.getLeft(), time));
        }
    }

    private void renderHeaterVest(ItemStack heaterVest, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, E living, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!heaterVest.isEmpty()) {
            HumanoidModel<E> model = FHItems.heater_vest.get().getArmorModel(living, heaterVest, EquipmentSlot.CHEST, null);
            if (model != null) {
                model.setupAnim(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                RenderType type = model.renderType(
                        new ResourceLocation(FHItems.heater_vest.get().getArmorTexture(heaterVest, living, EquipmentSlot.CHEST, null))
                );
                model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(type), packedLightIn, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            }
        }
    }
}
