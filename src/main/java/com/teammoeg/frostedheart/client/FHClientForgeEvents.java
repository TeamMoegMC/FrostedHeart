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

package com.teammoeg.frostedheart.client;

import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.GRAY;
import static net.minecraft.util.text.TextFormatting.WHITE;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import com.stereowalker.survive.entity.SurviveEntityStats;
import com.teammoeg.frostedheart.client.hud.FrostedHud;
import com.teammoeg.frostedheart.climate.SurviveTemperature;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stereowalker.survive.Survive;
import com.stereowalker.survive.temperature.TemperatureChangeInstance;
import com.stereowalker.survive.util.data.BlockTemperatureData;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.render.FHBipedLayerRenderer;
import com.teammoeg.frostedheart.client.util.FHClientUtils;
import com.teammoeg.frostedheart.client.util.UV4i;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FHClientForgeEvents {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if(!FHBipedLayerRenderer.rendersAssigned) {
            for(Object render : FHClientUtils.mc().getRenderManager().renderers.values())
                if(BipedRenderer.class.isAssignableFrom(render.getClass()))
                    ((BipedRenderer)render).addLayer(new FHBipedLayerRenderer<>((BipedRenderer)render));
                else if(ArmorStandRenderer.class.isAssignableFrom(render.getClass()))
                    ((ArmorStandRenderer)render).addLayer(new FHBipedLayerRenderer<>((ArmorStandRenderer)render));
            FHBipedLayerRenderer.rendersAssigned = true;
        }
    }

    @SubscribeEvent
    public static void addItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        for (ResourceLocation id : Survive.armorModifierMap.keySet()) {
            Item armor = ForgeRegistries.ITEMS.getValue(id);
            float weightMod = Survive.armorModifierMap.get(id).getWeightModifier();
            if (Survive.armorModifierMap.get(id).getTemperatureModifier().size() > 0) {
                TemperatureChangeInstance instance = Survive.armorModifierMap.get(id).getTemperatureModifier().get(0); // Get the first instance, we don't need the rest..
                float tempMod = instance.getTemperature();
                if (stack.getItem() == armor) {
                    event.getToolTip().add(new TranslationTextComponent("tooltip.frostedheart.survive_armor_temp_mod").appendString(String.valueOf(tempMod)).mergeStyle(TextFormatting.GRAY));
                    event.getToolTip().add(new TranslationTextComponent("tooltip.frostedheart.survive_weight_mod").appendString(String.valueOf(weightMod)).mergeStyle(TextFormatting.GRAY));
                }
            }
        }

        for (ResourceLocation id : Survive.blockTemperatureMap.keySet()) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            BlockTemperatureData data = Survive.blockTemperatureMap.get(id);
            float tempMod = data.getTemperatureModifier();
            int range = data.getRange();
            if (block != null && stack.getItem() == block.asItem()) {
                event.getToolTip().add(new TranslationTextComponent("tooltip.frostedheart.survive_block_temp_mod").appendString(String.valueOf(tempMod)).mergeStyle(TextFormatting.GRAY));
                event.getToolTip().add(new TranslationTextComponent("tooltip.frostedheart.survive_range").appendString(String.valueOf(range)).mergeStyle(TextFormatting.GRAY));
            }
        }

    }

    @SubscribeEvent
    public static void onRenderGameOverlayText(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getInstance();
        List<String> list = event.getRight();
        if (mc.world != null && mc.gameSettings.showDebugInfo) {
            BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
            if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                list.add("");
                list.add(AQUA + FHMain.MODNAME);
                list.add(GRAY + I18n.format("tooltip.frostedheart.f3_average_temperature", WHITE + String.format("%.1f", ChunkData.getTemperature(mc.world,pos))));
            } else {
                list.add(GRAY + I18n.format("tooltip.frostedheart.f3_invalid_chunk_data"));
            }
        }
    }

    @SubscribeEvent
    public static void renderVanillaOverlay(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity clientPlayer = mc.player;
        PlayerEntity renderViewPlayer = FrostedHud.getRenderViewPlayer();

        if (renderViewPlayer == null || clientPlayer == null || mc.gameSettings.hideGUI) {
            return;
        }

        MatrixStack stack = event.getMatrixStack();
        int anchorX = event.getWindow().getScaledWidth() / 2;
        int anchorY = event.getWindow().getScaledHeight();
        float partialTicks =  event.getPartialTicks();

        FrostedHud.renderSetup(clientPlayer, renderViewPlayer);

//        RenderSystem.disableAlphaTest();

        if (event.getType() == RenderGameOverlayEvent.ElementType.HELMET && FrostedHud.renderFrozen) {
            FrostedHud.renderFrozenOverlay(stack, anchorX, anchorY, mc, clientPlayer);
        }

        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && FrostedHud.renderHotbar) {
            if (mc.playerController.getCurrentGameType() == GameType.SPECTATOR) {
                mc.ingameGUI.getSpectatorGui().func_238528_a_(stack, partialTicks);
            } else {
                FrostedHud.renderHotbar(stack, anchorX, anchorY, mc, renderViewPlayer, partialTicks);
            }
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE && FrostedHud.renderExperience) {
            if (FrostedHud.renderHypothermia) {
                FrostedHud.renderHypothermia(stack, anchorX, anchorY, mc, clientPlayer);
            } else {
                FrostedHud.renderExperience(stack, anchorX, anchorY, mc, clientPlayer);
            }
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.HEALTH && FrostedHud.renderHealth) {
            FrostedHud.renderHealth(stack, anchorX, anchorY, mc, renderViewPlayer);
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.FOOD) {
            if (FrostedHud.renderFood) FrostedHud.renderFood(stack, anchorX, anchorY, mc, renderViewPlayer);
            if (FrostedHud.renderThirst) FrostedHud.renderThirst(stack, anchorX, anchorY, mc, renderViewPlayer);
            if (FrostedHud.renderHealth) FrostedHud.renderTemperature(stack, anchorX, anchorY, mc, renderViewPlayer);
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.ARMOR && FrostedHud.renderArmor) {
            FrostedHud.renderArmor(stack, anchorX, anchorY, mc, clientPlayer);
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.HEALTHMOUNT && FrostedHud.renderHealthMount) {
            FrostedHud.renderMountHealth(stack, anchorX, anchorY, mc, clientPlayer);
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.JUMPBAR && FrostedHud.renderJumpBar) {
            FrostedHud.renderJumpbar(stack, anchorX, anchorY, mc, clientPlayer);
            event.setCanceled(true);
        }

//        RenderSystem.enableAlphaTest();
    }
}
