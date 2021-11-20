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

package com.teammoeg.frostedheart.client.hud;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.UV4i;
import com.teammoeg.frostedheart.climate.TemperatureCore;

import gloridifice.watersource.common.capability.WaterLevelCapability;
import gloridifice.watersource.registry.EffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.AttackIndicatorStatus;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FoodStats;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class FrostedHud {
    public static boolean renderHotbar = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderThirst = true;
    public static boolean renderHealthMount = true;
    public static boolean renderExperience = true;
    public static boolean renderJumpBar = true;
    public static boolean renderHypothermia = true;
    public static boolean renderFrozen = true;

    public static final ResourceLocation HUD_ELEMENTS = new ResourceLocation(FHMain.MODID, "textures/gui/hud/hudelements.png");
    //    public static final ResourceLocation FROZEN_OVERLAY_PATH = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_overlay.png");
    public static final ResourceLocation FROZEN_OVERLAY_1 = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_stage_1.png");
    public static final ResourceLocation FROZEN_OVERLAY_2 = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_stage_2.png");
    public static final ResourceLocation FROZEN_OVERLAY_3 = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_stage_3.png");
    public static final ResourceLocation FROZEN_OVERLAY_4 = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_stage_4.png");
    public static final ResourceLocation FROZEN_OVERLAY_5 = new ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_stage_5.png");
    public static final ResourceLocation FIRE_VIGNETTE = new ResourceLocation(FHMain.MODID, "textures/gui/hud/fire_vignette.png");
    public static final ResourceLocation ICE_VIGNETTE = new ResourceLocation(FHMain.MODID, "textures/gui/hud/ice_vignette.png");
//    public static final ResourceLocation LEFT_HALF_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/left_half.png");
//    public static final ResourceLocation RIGHT_HALF_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/right_half.png");
//    public static final ResourceLocation LEFT_THREEQUARTERS_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/left_threequarters.png");
//    public static final ResourceLocation RIGHT_THREEQUARTERS_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/right_threequarters.png");

    public static final ResourceLocation ABSORPTION = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/absorption.png");
    public static final ResourceLocation DEFENCE = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/defence.png");
    public static final ResourceLocation HORSEHP = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/horsehp.png");
    public static final ResourceLocation HP = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/hp.png");
    public static final ResourceLocation HUNGER = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/hunger.png");
    public static final ResourceLocation JUMP = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/jump.png");
    public static final ResourceLocation OXYGEN = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/oxygen.png");
    public static final ResourceLocation THIRST = new ResourceLocation(FHMain.MODID, "textures/gui/hud/atlantes/thirst.png");

    public static void renderSetup(ClientPlayerEntity player, PlayerEntity renderViewPlayer) {
        Minecraft mc = Minecraft.getInstance();
        renderHealth = !player.isCreative() && !player.isSpectator();
        renderHealthMount = renderHealth && player.getRidingEntity() instanceof LivingEntity;
        renderFood = renderHealth && !renderHealthMount;
        renderThirst = renderHealth && !renderHealthMount;
        renderJumpBar = renderHealth && player.isRidingHorse();
        renderArmor = renderHealth && player.getTotalArmorValue() > 0;
        renderExperience = renderHealth;
        float bt=TemperatureCore.getBodyTemperature(renderViewPlayer);
        renderHypothermia = renderHealth &&  bt< -0.5||bt > 0.5;
        renderFrozen = renderHealth && bt <= -1.0;
        //System.out.println(TemperatureCore.getBodyTemperature(renderViewPlayer));
    }

    public static PlayerEntity getRenderViewPlayer() {
        return !(Minecraft.getInstance().getRenderViewEntity() instanceof PlayerEntity) ? null : (PlayerEntity) Minecraft.getInstance().getRenderViewEntity();
    }

    private static void renderHotbarItem(int x, int y, float partialTicks, PlayerEntity player, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!stack.isEmpty()) {
            float f = (float) stack.getAnimationsToGo() - partialTicks;
            if (f > 0.0F) {
                RenderSystem.pushMatrix();
                float f1 = 1.0F + f / 5.0F;
                RenderSystem.translatef((float) (x + 8), (float) (y + 12), 0.0F);
                RenderSystem.scalef(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                RenderSystem.translatef((float) (-(x + 8)), (float) (-(y + 12)), 0.0F);
            }

            mc.getItemRenderer().renderItemAndEffectIntoGUI(player, stack, x, y);
            if (f > 0.0F) {
                RenderSystem.popMatrix();
            }

            mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, stack, x, y);
        }
    }

    public static void renderHotbar(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player, float partialTicks) {
        mc.getProfiler().startSection("frostedheart_hotbar");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.hotbar_1.getA(), y + BasePos.hotbar_1.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_2.getA(), y + BasePos.hotbar_2.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_3.getA(), y + BasePos.hotbar_3.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_4.getA(), y + BasePos.hotbar_4.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_5.getA(), y + BasePos.hotbar_5.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_6.getA(), y + BasePos.hotbar_6.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_7.getA(), y + BasePos.hotbar_7.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_8.getA(), y + BasePos.hotbar_8.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_9.getA(), y + BasePos.hotbar_9.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);

        // Selection overlay
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_1.getA() - 1 + player.inventory.currentItem * 20, y + BasePos.hotbar_1.getB() - 1, UV.selected.x, UV.selected.y, UV.selected.w, UV.selected.h);

        if (player.getHeldItemOffhand().isEmpty()) {
            mc.ingameGUI.blit(stack, x + BasePos.off_hand.getA(), y + BasePos.off_hand.getB(), UV.off_hand_slot.x, UV.off_hand_slot.y, UV.off_hand_slot.w, UV.off_hand_slot.h);
        } else {
            mc.ingameGUI.blit(stack, x + BasePos.off_hand.getA(), y + BasePos.off_hand.getB(), UV.selected.x, UV.selected.y, UV.selected.w, UV.selected.h);
        }

        ItemStack itemstack = player.getHeldItemOffhand();
        HandSide handside = player.getPrimaryHand().opposite();

        RenderSystem.enableRescaleNormal();

        for (int i1 = 0; i1 < 9; ++i1) {
            int j1 = x - 90 + i1 * 20 + 2;
            int k1 = y - 16 - 3 + 1; // +1
            renderHotbarItem(j1, k1, partialTicks, player, player.inventory.mainInventory.get(i1));
        }

        if (!itemstack.isEmpty()) {
            int i2 = y - 16 - 3 + 1; // +1
            if (handside == HandSide.LEFT) {
                renderHotbarItem(x - 91 - 26 - 2 - 2, i2, partialTicks, player, itemstack);
            } else {
                renderHotbarItem(x + 91 + 10, i2, partialTicks, player, itemstack);
            }
        }

        if (mc.gameSettings.attackIndicator == AttackIndicatorStatus.HOTBAR) {
            float f = mc.player.getCooledAttackStrength(0.0F);
            if (f < 1.0F) {
                int j2 = y - 20;
                int k2 = x + 91 + 6;
                if (handside == HandSide.RIGHT) {
                    k2 = x - 91 - 22;
                }

                mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
                int l1 = (int) (f * 19.0F);
                mc.ingameGUI.blit(stack, k2, j2, 0, 94, 18, 18);
                mc.ingameGUI.blit(stack, k2, j2 + 18 - l1, 18, 112 - l1, 18, l1);
            }
        }

        RenderSystem.disableRescaleNormal();
        RenderSystem.disableBlend();

        mc.getProfiler().endSection();
    }

    public static void renderExperience(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_experience");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.exp_bar.getA(), y + BasePos.exp_bar.getB(), UV.exp_bar_frame.x, UV.exp_bar_frame.y, UV.exp_bar_frame.w, UV.exp_bar_frame.h);
        int i = mc.player.xpBarCap();
        if (i > 0) {
            int j = 182;
            int k = (int) (mc.player.experience * 183.0F);
            int l = y - 32 + 3;
            if (k > 0) {
                mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA(), y + BarPos.exp_bar.getB(), UV.exp_bar.x, UV.exp_bar.y, k, UV.exp_bar.h);
            }
        }
        if (mc.player.experienceLevel > 0) {
            String s = "" + mc.player.experienceLevel;
            int i1 = (x * 2 - mc.fontRenderer.getStringWidth(s)) / 2;
            int j1 = y - 29;
            mc.fontRenderer.drawString(stack, s, (float) (i1 + 1), (float) j1, 0);
            mc.fontRenderer.drawString(stack, s, (float) (i1 - 1), (float) j1, 0);
            mc.fontRenderer.drawString(stack, s, (float) i1, (float) (j1 + 1), 0);
            mc.fontRenderer.drawString(stack, s, (float) i1, (float) (j1 - 1), 0);
            mc.fontRenderer.drawString(stack, s, (float) i1, (float) j1, 8453920);
        }

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    private static int calculateHypoBarLength(double startTemp, double endTemp,float curtemp) {
    	double atemp=Math.abs(curtemp);
    	if(atemp<startTemp)return 0;
        return (int) ((Math.min(atemp, endTemp) - startTemp) / (endTemp - startTemp) * 182.0F);
    }

    public static void renderHypothermia(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_hypothermia");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();
        float temp=TemperatureCore.getBodyTemperature(player);
        mc.ingameGUI.blit(stack, x + BasePos.exp_bar.getA(), y + BasePos.exp_bar.getB(), UV.exp_bar_frame.x, UV.exp_bar_frame.y, UV.exp_bar_frame.w, UV.exp_bar_frame.h);
//        double startTemp = -0.5, endTemp = -3.0;
//        int k = (int) ((Math.abs(Math.max(TemperatureCore.getBodyTemperature(player), endTemp)) - Math.abs(startTemp)) / (Math.abs(endTemp) - Math.abs(startTemp)) * 181.0F);
        if(temp<-0.5) {
	        int stage1length = calculateHypoBarLength(0.5, 1.0,temp);
	        int stage2length = calculateHypoBarLength(2.0, 3.0,temp);
	        if(stage1length>0)
	        	mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA() + 1, y + BarPos.exp_bar.getB(), UV.hypothermia_bar.x, UV.hypothermia_bar.y, stage1length, UV.hypothermia_bar.h);
	        if(stage2length>0)
	        	mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA() + 1, y + BarPos.exp_bar.getB(), UV.hypothermia_bar.x, UV.hypothermia_bar.y + 6, stage2length, UV.hypothermia_bar.h);
        }else if(temp>0.5) {
	        int stage1length = calculateHypoBarLength(0.5,1.0,temp);
	        int stage2length = calculateHypoBarLength(2.0,3.0,temp);
	        if(stage1length>0)
	        	mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA() + 1, y + BarPos.exp_bar.getB(), UV.hypothermia_bar.x, UV.hypothermia_bar.y + 12, stage1length, UV.hypothermia_bar.h);
	        if(stage2length>0)
	        	mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA() + 1, y + BarPos.exp_bar.getB(), UV.hypothermia_bar.x, UV.hypothermia_bar.y + 6, stage2length, UV.hypothermia_bar.h);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderHealth(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_health");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.left_threequarters.getA(), y + BasePos.left_threequarters.getB(), UV.left_threequarters_frame.x, UV.left_threequarters_frame.y, UV.left_threequarters_frame.w, UV.left_threequarters_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.left_threequarters.getA(), y + IconPos.left_threequarters.getB(), UV.icon_health_normal.x, UV.icon_health_normal.y, UV.icon_health_normal.w, UV.icon_health_normal.h);

        float health = player.getHealth();
        //ModifiableAttributeInstance attrMaxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        float healthMax =/* (float) attrMaxHealth.getValue()*/player.getMaxHealth();
        float absorb = player.getAbsorptionAmount(); // let's say max is 20

        // range: [0, 99]
        int healthState = health == 0 ? 0 : MathHelper.ceil(health / healthMax * 100) - 1;
        int absorbState = absorb == 0 ? 0 : MathHelper.ceil(absorb / 20 * 100) - 1;
        if(healthState>99)
        	healthState=99;
        // range: [0, 9]
        int healthCol = healthState / 10;
        int absorbCol = absorbState / 10;

        // range: [0, 9]
        int healthRow = healthState % 10;
        int absorbRow = absorbState % 10;

        mc.getTextureManager().bindTexture(HP);
        mc.ingameGUI.blit(stack, x + BarPos.left_threequarters_inner.getA(), y + BarPos.left_threequarters_inner.getB(), healthCol * UV.health_bar.w, healthRow * UV.health_bar.h, UV.health_bar.w, UV.health_bar.h, 320, 320);
        mc.getTextureManager().bindTexture(ABSORPTION);
        mc.ingameGUI.blit(stack, x + BarPos.left_threequarters_outer.getA(), y + BarPos.left_threequarters_outer.getB(), absorbCol * UV.absorption_bar.w, absorbRow * UV.absorption_bar.h, UV.absorption_bar.w, UV.absorption_bar.h, 360, 360);
        int ihealth = (int) Math.ceil(health);
        int offset = mc.fontRenderer.getStringWidth(String.valueOf(ihealth)) / 2;
        mc.fontRenderer.drawString(stack, String.valueOf(ihealth), x + BasePos.left_threequarters.getA() + UV.left_threequarters_frame.w / 2.0F - offset, y + BasePos.left_threequarters.getB() + UV.left_threequarters_frame.h / 2.0F, 0xFFFFFF);

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderFood(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_food");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.right_half_1.getB().getA(), y + BasePos.right_half_1.getB().getB(), UV.right_half_frame.x, UV.right_half_frame.y, UV.right_half_frame.w, UV.right_half_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.right_half_1.getB().getA(), y + IconPos.right_half_1.getB().getB(), UV.icon_hunger_normal.x, UV.icon_hunger_normal.y, UV.icon_hunger_normal.w, UV.icon_hunger_normal.h);
        FoodStats stats = mc.player.getFoodStats();
        int foodLevel = stats.getFoodLevel();
        int foodLevelState = MathHelper.ceil(foodLevel / 20.0F * 100) - 1;
        if(foodLevelState>99)
        	foodLevelState=99;
        int hungerCol = foodLevelState / 10;
        int hungerRow = foodLevelState % 10;
        mc.getTextureManager().bindTexture(HUNGER);
        mc.ingameGUI.blit(stack, x + BarPos.right_half_1.getB().getA(), y + BarPos.right_half_1.getB().getB(), hungerCol * UV.hunger_bar.w, hungerRow * UV.hunger_bar.h, UV.hunger_bar.w, UV.hunger_bar.h, 160, 320);

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderThirst(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_thirst");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        EffectInstance effectInstance = mc.player.getActivePotionEffect(EffectRegistry.THIRST);
        boolean isThirsty = effectInstance != null;
        mc.ingameGUI.blit(stack, x + BasePos.right_half_2.getB().getA(), y + BasePos.right_half_2.getB().getB(), UV.right_half_frame.x, UV.right_half_frame.y, UV.right_half_frame.w, UV.right_half_frame.h);
        if (isThirsty) {
            mc.ingameGUI.blit(stack, x + IconPos.right_half_2.getB().getA(), y + IconPos.right_half_2.getB().getB(), UV.icon_thirst_abnormal_green.x, UV.icon_thirst_abnormal_green.y, UV.icon_thirst_abnormal_green.w, UV.icon_thirst_abnormal_green.h);
        } else {
            mc.ingameGUI.blit(stack, x + IconPos.right_half_2.getB().getA(), y + IconPos.right_half_2.getB().getB(), UV.icon_thirst_normal.x, UV.icon_thirst_normal.y, UV.icon_thirst_normal.w, UV.icon_thirst_normal.h);
        }
        mc.getTextureManager().bindTexture(THIRST);
        player.getCapability(WaterLevelCapability.PLAYER_WATER_LEVEL).ifPresent(data -> {
            int waterLevel = data.getWaterLevel();
            int waterLevelState = MathHelper.ceil(waterLevel / 20.0F * 100) - 1;
            if(waterLevelState>99)
            	waterLevelState=99;
            int waterCol = waterLevelState / 10;
            int waterRow = waterLevelState % 10;
            mc.ingameGUI.blit(stack, x + BarPos.right_half_2.getB().getA(), y + BarPos.right_half_2.getB().getB(), waterCol * UV.thirst_bar.w, waterRow * UV.thirst_bar.h, UV.thirst_bar.w, UV.thirst_bar.h, 160, 320);
        });

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderTemperature(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_temperature");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.temperature_orb_frame.getA(), y + BasePos.temperature_orb_frame.getB() + 3, UV.temperature_orb_frame.x, UV.temperature_orb_frame.y, UV.temperature_orb_frame.w, UV.temperature_orb_frame.h);
        if (mc.world != null) {
            BlockPos pos = new BlockPos(player.getPosX(), player.getBoundingBox().minY, player.getPosZ());
            if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                int temperature = (int) TemperatureCore.getEnvTemperature(player);
                renderTemp(stack, mc, temperature, x + BarPos.temp_orb.getA(), y + BarPos.temp_orb.getB() + 3, true);
            }
        }

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderArmor(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_armor");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.left_half_1.getA().getA(), y + BasePos.left_half_1.getA().getB(), UV.left_half_frame.x, UV.left_half_frame.y, UV.left_half_frame.w, UV.left_half_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.left_half_1.getA().getA(), y + IconPos.left_half_1.getA().getB(), UV.icon_defence_normal.x, UV.icon_defence_normal.y, UV.icon_defence_normal.w, UV.icon_defence_normal.h);
        int armorValue = player.getTotalArmorValue();
        int armorValueState = MathHelper.ceil(armorValue / 20.0F * 100) - 1;
        if(armorValueState>99)
        	armorValueState=99;
        int armorCol = armorValueState / 10;
        int armorRow = armorValueState % 10;
        mc.getTextureManager().bindTexture(DEFENCE);
        mc.ingameGUI.blit(stack, x + BarPos.left_half_1.getA().getA(), y + BarPos.left_half_1.getA().getB(), armorCol * UV.defence_bar.w, armorRow * UV.defence_bar.h, UV.defence_bar.w, UV.defence_bar.h, 160, 320);

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderMountHealth(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_mounthealth");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        mc.ingameGUI.blit(stack, x + BasePos.right_threequarters.getA(), y + BasePos.right_threequarters.getB(), UV.right_threequarters_frame.x, UV.right_threequarters_frame.y, UV.right_threequarters_frame.w, UV.right_threequarters_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.right_threequarters.getA(), y + IconPos.right_threequarters.getB(), UV.icon_horse_normal.x, UV.icon_horse_normal.y, UV.icon_horse_normal.w, UV.icon_horse_normal.h);
        Entity tmp = player.getRidingEntity();
        if (!(tmp instanceof LivingEntity)) return;
        LivingEntity mount = (LivingEntity) tmp;
        int health = (int) mount.getHealth();
        float healthMax = mount.getMaxHealth();
        int healthState = MathHelper.ceil(health / healthMax * 100) - 1;
        if(healthState>99)
        	healthState=99;
        int healthCol = healthState / 10;
        int healthRow = healthState % 10;
        mc.getTextureManager().bindTexture(HORSEHP);
        mc.ingameGUI.blit(stack, x + BarPos.right_threequarters_inner.getA(), y + BarPos.right_threequarters_inner.getB(), healthCol * UV.horse_health_bar.w, healthRow * UV.horse_health_bar.h, UV.horse_health_bar.w, UV.horse_health_bar.h, 320, 320);

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderJumpbar(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_jumpbar");
        mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);
        RenderSystem.enableBlend();

        float jumpPower = player.getHorseJumpPower();
        int jumpState = MathHelper.ceil(jumpPower * 100) - 1;
        if(jumpState>99)
        	jumpState=99;
        int jumpCol = jumpState / 10;
        int jumpRow = jumpState % 10;
        mc.getTextureManager().bindTexture(JUMP);
        mc.ingameGUI.blit(stack, x + BarPos.right_threequarters_outer.getA(), y + BarPos.right_threequarters_outer.getB(), jumpCol * UV.horse_jump_bar.w, jumpRow * UV.horse_jump_bar.h, UV.horse_jump_bar.w, UV.horse_jump_bar.h, 360, 360);

        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderFrozenOverlay(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_frozen");
        float temp = TemperatureCore.getBodyTemperature(player);
        ResourceLocation texture;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        if (temp >= -2) {
            texture = FROZEN_OVERLAY_1;
        } else if (temp >= -3) {
            texture = FROZEN_OVERLAY_2;
        } else if (temp >= -4) {
            texture = FROZEN_OVERLAY_3;
        } else if (temp >= -5) {
            texture = FROZEN_OVERLAY_4;
        } else {
            texture = FROZEN_OVERLAY_5;
        }
        mc.getTextureManager().bindTexture(texture);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.0D, (double) y, -90.0D).tex(0.0F, 1.0F).endVertex();
        bufferbuilder.pos((double) x * 2, (double) y, -90.0D).tex(1.0F, 1.0F).endVertex();
        bufferbuilder.pos((double) x * 2, 0.0D, -90.0D).tex(1.0F, 0.0F).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0F, 0.0F).endVertex();
        tessellator.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderFrozenVignette(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_vignette");
        float temp = MathHelper.clamp(TemperatureCore.getBodyTemperature(player), -10, -1);
        // -10 < temp < 0 ===== 1 - 0
        float opacityDelta = (Math.abs(temp) - 0.5F) / 9.5F;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.color4f(1, 1, 1, (float) Math.min(opacityDelta, 1));
        RenderSystem.disableAlphaTest();
        mc.getTextureManager().bindTexture(ICE_VIGNETTE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0D, y, -90.0D).tex(0.0F, 1.0F).endVertex();
        buffer.pos(x * 2, y, -90.0D).tex(1.0F, 1.0F).endVertex();
        buffer.pos(x * 2, 0.0D, -90.0D).tex(1.0F, 0.0F).endVertex();
        buffer.pos(0.0D, 0.0D, -90.0D).tex(0.0F, 0.0F).endVertex();
        tessellator.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static void renderAirBar(MatrixStack stack, int x, int y, Minecraft mc, PlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_air");
        RenderSystem.enableBlend();
        int air = player.getAir();
        int maxAir = 300;
        if (player.areEyesInFluid(FluidTags.WATER) || air < maxAir) {
            mc.getTextureManager().bindTexture(FrostedHud.HUD_ELEMENTS);

            mc.ingameGUI.blit(stack, x + BasePos.right_half_3.getB().getA(), y + BasePos.right_half_3.getB().getB(), UV.right_half_frame.x, UV.right_half_frame.y, UV.right_half_frame.w, UV.right_half_frame.h);
            if (air <= 30) {
                mc.ingameGUI.blit(stack, x + IconPos.right_half_3.getB().getA(), y + IconPos.right_half_3.getB().getB(), UV.icon_oxygen_abnormal_white.x, UV.icon_oxygen_abnormal_white.y, UV.icon_oxygen_abnormal_white.w, UV.icon_oxygen_abnormal_white.h);
            } else {
                mc.ingameGUI.blit(stack, x + IconPos.right_half_3.getB().getA(), y + IconPos.right_half_3.getB().getB(), UV.icon_oxygen_normal.x, UV.icon_oxygen_normal.y, UV.icon_oxygen_normal.w, UV.icon_oxygen_normal.h);
            }
            int airState = MathHelper.ceil(air / (float) maxAir * 100) - 1;
            if(airState>99)
            	airState=99;
            int airCol = airState / 10;
            int airRow = airState % 10;
            mc.getTextureManager().bindTexture(OXYGEN);
            mc.ingameGUI.blit(stack, x + BarPos.right_half_3.getB().getA(), y + BarPos.right_half_3.getB().getB(), airCol * UV.oxygen_bar.w, airRow * UV.oxygen_bar.h, UV.oxygen_bar.w, UV.oxygen_bar.h, 160, 320);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().endSection();
    }

    public static class BasePos {
        public static final Tuple<Integer, Integer> hotbar_1 = new Tuple<>(-90, -20);
        public static final Tuple<Integer, Integer> hotbar_2 = new Tuple<>(-70, -20);
        public static final Tuple<Integer, Integer> hotbar_3 = new Tuple<>(-50, -20);
        public static final Tuple<Integer, Integer> hotbar_4 = new Tuple<>(-30, -20);
        public static final Tuple<Integer, Integer> hotbar_5 = new Tuple<>(-10, -20);
        public static final Tuple<Integer, Integer> hotbar_6 = new Tuple<>(10, -20);
        public static final Tuple<Integer, Integer> hotbar_7 = new Tuple<>(30, -20);
        public static final Tuple<Integer, Integer> hotbar_8 = new Tuple<>(50, -20);
        public static final Tuple<Integer, Integer> hotbar_9 = new Tuple<>(70, -20);
        public static final Tuple<Integer, Integer> off_hand = new Tuple<>(-124, -21);
        public static final Tuple<Integer, Integer> exp_bar = new Tuple<>(-112 + 19, -27);
        public static final Tuple<Integer, Integer> temperature_orb_frame = new Tuple<>(-22, -76);
        public static final Tuple<Integer, Integer> left_threequarters = new Tuple<>(-59, -66);
        public static final Tuple<Integer, Integer> right_threequarters = new Tuple<>(23, -66);
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_1 = new Tuple<>(new Tuple<>(-79, -64), new Tuple<>(-41, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_2 = new Tuple<>(new Tuple<>(-99, -64), new Tuple<>(-61, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_3 = new Tuple<>(new Tuple<>(-119, -64), new Tuple<>(-81, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_1 = new Tuple<>(new Tuple<>(56, -64), new Tuple<>(18, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_2 = new Tuple<>(new Tuple<>(76, -64), new Tuple<>(38, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_3 = new Tuple<>(new Tuple<>(96, -64), new Tuple<>(58, -64));
    }

    public static class BarPos {
        public static final Tuple<Integer, Integer> exp_bar = new Tuple<>(-112 + 19, -26);
        public static final Tuple<Integer, Integer> temp_orb = new Tuple<>(-22 + 3, -76 + 3);
        public static final Tuple<Integer, Integer> left_threequarters_inner = new Tuple<>(-57, -64);
        public static final Tuple<Integer, Integer> left_threequarters_outer = new Tuple<>(-59, -66);
        public static final Tuple<Integer, Integer> right_threequarters_inner = new Tuple<>(25, -64);
        public static final Tuple<Integer, Integer> right_threequarters_outer = new Tuple<>(23, -66);
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_1 = new Tuple<>(new Tuple<>(-79, -64), new Tuple<>(-41, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_2 = new Tuple<>(new Tuple<>(-99, -64), new Tuple<>(-61, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_3 = new Tuple<>(new Tuple<>(-119, -64), new Tuple<>(-81, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_1 = new Tuple<>(new Tuple<>(63, -64), new Tuple<>(25, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_2 = new Tuple<>(new Tuple<>(83, -64), new Tuple<>(45, -64));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_3 = new Tuple<>(new Tuple<>(103, -64), new Tuple<>(65, -64));
    }

    public static class IconPos {
        public static final Tuple<Integer, Integer> left_threequarters = new Tuple<>(-47, -56);
        public static final Tuple<Integer, Integer> right_threequarters = new Tuple<>(35, -56);
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_1 = new Tuple<>(new Tuple<>(-71, -54), new Tuple<>(-33, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_2 = new Tuple<>(new Tuple<>(-91, -54), new Tuple<>(-53, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_3 = new Tuple<>(new Tuple<>(-111, -54), new Tuple<>(-73, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_1 = new Tuple<>(new Tuple<>(59, -54), new Tuple<>(21, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_2 = new Tuple<>(new Tuple<>(79, -54), new Tuple<>(41, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_3 = new Tuple<>(new Tuple<>(99, -54), new Tuple<>(61, -54));
    }

    public static class UV {
        public static final UV4i hotbar_slot = new UV4i(1, 1, 20, 20);
        public static final UV4i off_hand_slot = new UV4i(22, 1, 22, 22);
        public static final UV4i selected = new UV4i(108, 109, 22, 22);
        public static final UV4i exp_bar_frame = new UV4i(45, 1, 184, 7);
        public static final UV4i temperature_orb_frame = new UV4i(1, 24, 43, 43);
        public static final UV4i left_threequarters_frame = new UV4i(45, 9, 36, 38);
        public static final UV4i right_threequarters_frame = new UV4i(1, 113, 36, 38);
        public static final UV4i left_half_frame = new UV4i(82, 9, 23, 24 + 10);
        public static final UV4i right_half_frame = new UV4i(106, 9, 23, 24 + 10);
        public static final UV4i exp_bar = new UV4i(1, 70, 182, 5);
        public static final UV4i hypothermia_bar = new UV4i(1, 152, 182, 5);
        public static final UV4i health_bar = new UV4i(1, 76, 32, 32);
        public static final UV4i absorption_bar = new UV4i(34, 76, 36, 36);
        public static final UV4i hunger_bar = new UV4i(71, 76, 16, 32);
        public static final UV4i thirst_bar = new UV4i(88, 76, 16, 32);
        public static final UV4i oxygen_bar = new UV4i(105, 76, 16, 32);
        public static final UV4i defence_bar = new UV4i(122, 76, 16, 32);
        public static final UV4i horse_health_bar = new UV4i(38, 113, 32, 32);
        public static final UV4i horse_jump_bar = new UV4i(71, 109, 36, 36);
        public static final UV4i icon_health_normal = new UV4i(130, 9, 12, 12);
        public static final UV4i icon_health_abnormal_white = new UV4i(130, 22, 12, 12);
        public static final UV4i icon_health_abnormal_green = new UV4i(130, 35, 12, 12);
        public static final UV4i icon_health_abnormal_black = new UV4i(130, 48, 12, 12);
        public static final UV4i icon_health_abnormal_cyan = new UV4i(195, 9, 12, 12);
        public static final UV4i icon_health_hardcore_normal = new UV4i(208, 9, 12, 12);
        public static final UV4i icon_health_hardcore_abnormal_white = new UV4i(208, 22, 12, 12);
        public static final UV4i icon_health_hardcore_abnormal_green = new UV4i(208, 35, 12, 12);
        public static final UV4i icon_health_hardcore_abnormal_black = new UV4i(208, 48, 12, 12);
        public static final UV4i icon_health_hardcore_abnormal_cyan = new UV4i(195, 22, 12, 12);
        public static final UV4i icon_hunger_normal = new UV4i(143, 9, 12, 12);
        public static final UV4i icon_hunger_abnormal_white = new UV4i(143, 22, 12, 12);
        public static final UV4i icon_hunger_abnormal_green = new UV4i(143, 35, 12, 12);
        public static final UV4i icon_thirst_normal = new UV4i(156, 9, 12, 12);
        public static final UV4i icon_thirst_abnormal_white = new UV4i(156, 22, 12, 12);
        public static final UV4i icon_thirst_abnormal_green = new UV4i(156, 35, 12, 12);
        public static final UV4i icon_oxygen_normal = new UV4i(169, 9, 12, 12);
        public static final UV4i icon_oxygen_abnormal_white = new UV4i(169, 22, 12, 12);
        public static final UV4i icon_oxygen_abnormal_green = new UV4i(169, 35, 12, 12);
        public static final UV4i icon_defence_normal = new UV4i(182, 9, 12, 12);
        public static final UV4i icon_defence_abnormal_white = new UV4i(182, 22, 12, 12);
        public static final UV4i icon_horse_normal = new UV4i(143, 48, 12, 12);
        public static final UV4i icon_horse_abnormal_white = new UV4i(156, 48, 12, 12);
    }

    private static void renderTemp(MatrixStack stack, Minecraft mc, double temp, int offsetX, int offsetY, boolean celsius) {
        UV4i unitUV = celsius ? new UV4i(false, 0, 25, 13, 34) : new UV4i(false, 13, 25, 26, 34);
        UV4i signUV = temp >= 0 ? new UV4i(false, 61, 17, 68, 24) : new UV4i(false, 68, 17, 75, 24);
        double abs = Math.abs(temp);
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(abs));
        bigDecimal.round(new MathContext(1));
        int integer = bigDecimal.intValue();
        int decimal = (int) (bigDecimal.subtract(new BigDecimal(integer)).doubleValue() * 10);

        ResourceLocation digits = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/digits.png");
        ResourceLocation change_rate_arrows = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/change_rate_arrows.png");
        ResourceLocation ardent = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/ardent.png");
        ResourceLocation fervid = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/fervid.png");
        ResourceLocation hot = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/hot.png");
        ResourceLocation warm = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/warm.png");
        ResourceLocation moderate = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/moderate.png");
        ResourceLocation chilly = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/chilly.png");
        ResourceLocation cold = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/cold.png");
        ResourceLocation frigid = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/frigid.png");
        ResourceLocation hadean = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/hadean.png");

        // draw orb
        if (temp > 80) {
            mc.getTextureManager().bindTexture(ardent);
        } else if (temp > 60) {
            mc.getTextureManager().bindTexture(fervid);
        } else if (temp > 40) {
            mc.getTextureManager().bindTexture(hot);
        } else if (temp > 20) {
            mc.getTextureManager().bindTexture(warm);
        } else if (temp > 0) {
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
        mc.getTextureManager().bindTexture(HUD_ELEMENTS);
    }

    private static ArrayList<UV4i> getIntegerDigitUVs(int digit) {
        ArrayList<UV4i> rtn = new ArrayList<>();
        UV4i v1, v2, v3;
        if (digit / 10 == 0) { // len = 1
            int firstDigit = digit;
            if (firstDigit == 0) firstDigit += 10;
            v1 = new UV4i(false, 10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            rtn.add(v1);
        } else if (digit / 10 < 10) { // len = 2
            int firstDigit = digit / 10;
            if (firstDigit == 0) firstDigit += 10;
            int secondDigit = digit % 10;
            if (secondDigit == 0) secondDigit += 10;
            v1 = new UV4i(false, 10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            v2 = new UV4i(false, 10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
            rtn.add(v1);
            rtn.add(v2);
        } else { // len = 3
            int thirdDigit = digit % 10;
            if (thirdDigit == 0) thirdDigit += 10;
            int secondDigit = digit / 10;
            if (secondDigit == 0) secondDigit += 10;
            int firstDigit = digit / 100;
            if (firstDigit == 0) firstDigit += 10;
            v1 = new UV4i(false, 10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
            v2 = new UV4i(false, 10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
            v3 = new UV4i(false, 10 * (thirdDigit - 1), 0, 10 * thirdDigit, 17);
            rtn.add(v1);
            rtn.add(v2);
            rtn.add(v3);
        }
        return rtn;
    }

    private static UV4i getDecDigitUV(int dec) {
        return new UV4i(false, 6 * (dec - 1), 17, 6 * dec, 25);
    }
}
