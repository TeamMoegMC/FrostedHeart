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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stereowalker.survive.Survive;
import com.stereowalker.survive.temperature.TemperatureChangeInstance;
import com.stereowalker.survive.util.data.BlockTemperatureData;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.UV4i;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.text.TextFormatting.*;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FHClientForgeEvents {

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
                ChunkData data = ChunkData.get(mc.world, pos);
                if (data.getStatus().isAtLeast(ChunkData.Status.CLIENT)) {
                    list.add(GRAY + I18n.format("frostedheart.tooltip.f3_average_temperature", WHITE + String.format("%.1f", data.getTemperatureAtBlock(pos))));
                }
            } else {
                list.add(GRAY + I18n.format("frostedheart.tooltip.f3_invalid_chunk_data"));
            }
        }
    }

    @SubscribeEvent
    public static void renderGameOverlay(RenderGameOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        mc.getProfiler().startSection("frostedheart_temperature");
        if (Minecraft.isGuiEnabled() && mc.playerController.gameIsSurvivalOrAdventure() && mc.world != null && event.getType() == RenderGameOverlayEvent.ElementType.FOOD) {
            BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
            if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                ChunkData data = ChunkData.get(mc.world, pos);
                if (data.getStatus().isAtLeast(ChunkData.Status.CLIENT)) {
                    // FETCH TEMPERATURE
                    int temperature = (int) data.getTemperatureAtBlock(pos);

                    // RENDER CONFIGURATION
                    int w = event.getWindow().getScaledWidth();
                    int h = event.getWindow().getScaledHeight();
                    int offsetX = 0;
                    int offsetY = 0;
                    if (FHConfig.CLIENT.tempOrbPosition.get() == FHConfig.TempOrbPos.MIDDLE) {
                        offsetX = w / 2 - 18;
                        offsetY = h - 84;
                    } else if (FHConfig.CLIENT.tempOrbPosition.get() == FHConfig.TempOrbPos.TOP_LEFT) {
                        offsetX = FHConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = FHConfig.CLIENT.tempOrbOffsetY.get();
                    } else if (FHConfig.CLIENT.tempOrbPosition.get() == FHConfig.TempOrbPos.TOP_RIGHT) {
                        offsetX = w - 36 + FHConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = FHConfig.CLIENT.tempOrbOffsetY.get();
                    } else if (FHConfig.CLIENT.tempOrbPosition.get() == FHConfig.TempOrbPos.BOTTOM_LEFT) {
                        offsetX = FHConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = h - 36 + FHConfig.CLIENT.tempOrbOffsetY.get();
                    } else if (FHConfig.CLIENT.tempOrbPosition.get() == FHConfig.TempOrbPos.BOTTOM_RIGHT) {
                        offsetX = w - 36 + FHConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = h - 36 + FHConfig.CLIENT.tempOrbOffsetY.get();
                    }

                    // RENDER TEMPERATURE
                    renderTemp(event.getMatrixStack(), mc, temperature, offsetX, offsetY, true);
                }
            }
        }

        mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
        mc.getProfiler().endSection();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.disableAlphaTest();
    }

    private static void renderTemp(MatrixStack stack, Minecraft mc, double temp, int offsetX, int offsetY, boolean celsius) {
        UV4i unitUV = celsius ? new UV4i(0, 25, 13, 34) : new UV4i(13, 25, 26, 34);
        UV4i signUV = temp >= 0 ? new UV4i(61, 17, 68, 24) : new UV4i(68, 17, 75, 24);
        double abs = Math.abs(temp);
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(abs));
        bigDecimal.round(new MathContext(1));
        int integer = bigDecimal.intValue();
        int decimal = (int) (bigDecimal.subtract(new BigDecimal(integer)).doubleValue() * 10);

        ResourceLocation digits = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/digits.png");
        ResourceLocation moderate = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/moderate.png");
        ResourceLocation chilly = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/chilly.png");
        ResourceLocation cold = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/cold.png");
        ResourceLocation frigid = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/frigid.png");
        ResourceLocation hadean = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/hadean.png");

        // draw orb
        if (temp > 0) {
            mc.getTextureManager().bindTexture(moderate);
        } else if (temp > -20) {
            mc.getTextureManager().bindTexture(chilly);
        } else if (temp > -40) {
            mc.getTextureManager().bindTexture(cold);
        } else if (temp > -80) {
            mc.getTextureManager().bindTexture(frigid);
        } else {
            mc.getTextureManager().bindTexture(hadean);
        }
        IngameGui.blit(stack, offsetX + 0, offsetY + 0, 0, 0, 36, 36, 36, 36);

        // draw temperature
        mc.getTextureManager().bindTexture(digits);
        // sign and unit
        IngameGui.blit(stack, offsetX + 1, offsetY + 12, signUV.x, signUV.y, signUV.w, signUV.h, 100, 34);
        IngameGui.blit(stack, offsetX + 11, offsetY + 24, unitUV.x, unitUV.y, unitUV.w, unitUV.h, 100, 34);
        // digits
        ArrayList<UV4i> uv4is = getIntegerDigitUVs(integer);
        UV4i decUV = getDecDigitUV(decimal);
        if (uv4is.size() == 1) {
            UV4i uv1 = uv4is.get(0);
            IngameGui.blit(stack, offsetX + 13, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
            IngameGui.blit(stack, offsetX + 25, offsetY + 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
        } else if (uv4is.size() == 2) {
            UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1);
            IngameGui.blit(stack, offsetX + 8, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
            IngameGui.blit(stack, offsetX + 18, offsetY + 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
            IngameGui.blit(stack, offsetX + 28, offsetY + 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
        } else if (uv4is.size() == 3) {
            UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1), uv3 = uv4is.get(2);
            IngameGui.blit(stack, offsetX + 7, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
            IngameGui.blit(stack, offsetX + 14, offsetY + 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
            IngameGui.blit(stack, offsetX + 24, offsetY + 7, uv3.x, uv3.y, uv3.w, uv3.h, 100, 34);
        }
    }

    private static ArrayList<UV4i> getIntegerDigitUVs(int digit) {
        ArrayList<UV4i> rtn = new ArrayList<>();
        UV4i v1, v2, v3;
        if (digit / 10 == 0) { // len = 1
            int firstDigit = digit;
            if (firstDigit == 0) firstDigit += 10;
            v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            rtn.add(v1);
        } else if (digit / 10 < 10) { // len = 2
            int firstDigit = digit / 10;
            if (firstDigit == 0) firstDigit += 10;
            int secondDigit = digit % 10;
            if (secondDigit == 0) secondDigit += 10;
            v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
            rtn.add(v1);
            rtn.add(v2);
        } else { // len = 3
            int thirdDigit = digit % 10;
            if (thirdDigit == 0) thirdDigit += 10;
            int secondDigit = digit / 10;
            if (secondDigit == 0) secondDigit += 10;
            int firstDigit = digit / 100;
            if (firstDigit == 0) firstDigit += 10;
            v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
            v3 = new UV4i(10 * (thirdDigit - 1), 0, 10 * thirdDigit, 17);
            rtn.add(v1);
            rtn.add(v2);
            rtn.add(v3);
        }
        return rtn;
    }

    private static UV4i getDecDigitUV(int dec) {
        return new UV4i(6 * (dec - 1), 17, 6 * dec, 25);
    }
}
