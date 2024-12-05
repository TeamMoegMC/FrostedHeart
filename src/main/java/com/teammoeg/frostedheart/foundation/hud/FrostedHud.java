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

package com.teammoeg.frostedheart.foundation.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.FHMobEffects;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.mixin.client.BossHealthOverlayAccess;
import com.teammoeg.frostedheart.util.client.*;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.TemperatureFrame;
import com.teammoeg.frostedheart.content.climate.TemperatureFrame.FrameType;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchVariant;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.util.client.UV.Transition;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

public class FrostedHud {
    static final class Atlases {
        static final AtlasUV health_bar =       new AtlasUV("hud/atlantes/hp.png", 32, 32, 10, 100, 320, 320);
        static final AtlasUV maxhealth_bar =    new AtlasUV("hud/atlantes/maxhp.png", 32, 32, 10, 100, 320, 320);
        static final AtlasUV absorption_bar =   new AtlasUV("hud/atlantes/absorption.png", 36, 36, 10, 100, 360, 360);
        static final AtlasUV hunger_bar =       new AtlasUV("hud/atlantes/hunger.png", 16, 32, 10, 100, 160, 320);
        static final AtlasUV thirst_bar =       new AtlasUV("hud/atlantes/thirst.png", 16, 32, 10, 100, 160, 320);
        static final AtlasUV oxygen_bar =       new AtlasUV("hud/atlantes/oxygen.png", 16, 32, 10, 100, 160, 320);
        static final AtlasUV defence_bar =      new AtlasUV("hud/atlantes/defence.png", 16, 32, 10, 100, 160, 320);
        static final AtlasUV horse_health_bar = new AtlasUV("hud/atlantes/horsehp.png", 32, 32, 10, 100, 320, 320);
        static final AtlasUV horse_jump_bar =   new AtlasUV("hud/atlantes/jump.png", 36, 36, 10, 100, 360, 360);
    }
    static final class BarPos {
        static final Point exp_bar = new Point(-112 + 19, -26);
        static final Point temp_orb = new Point(-22 + 3, -76 + 3);
        static final Point left_threequarters_inner = new Point(-57, -64);
        static final Point left_threequarters_outer = new Point(-59, -66);
        static final Point right_threequarters_inner = new Point(25, -64);
        static final Point right_threequarters_outer = new Point(23, -66);
        static final Point left_half_1 = new Point(-79, -64/* ,-41, -64 */);
        static final Point left_half_2 = new Point(-99, -64/* ,-61, -64 */);
        static final Point left_half_3 = new Point(-119, -64/* ,-81, -64 */);
        static final Point right_half_1 = new Point(/* 63, -64, */25, -64);
        static final Point right_half_2 = new Point(/* 83, -64, */45, -64);
        static final Point right_half_3 = new Point(/* 103, -64, */65, -64);
    }
    static final class BasePos {
        static final Point hotbar_1 = new Point(-90, -20);
        static final Point hotbar_2 = new Point(-70, -20);
        static final Point hotbar_3 = new Point(-50, -20);
        static final Point hotbar_4 = new Point(-30, -20);
        static final Point hotbar_5 = new Point(-10, -20);
        static final Point hotbar_6 = new Point(10, -20);
        static final Point hotbar_7 = new Point(30, -20);
        static final Point hotbar_8 = new Point(50, -20);
        static final Point hotbar_9 = new Point(70, -20);
        static final Point off_hand = new Point(-124, -21);
        static final Point exp_bar = new Point(-112 + 19, -27);
        static final Point temperature_orb_frame = new Point(-22, -76);
        static final Point left_threequarters = new Point(-59, -66);
        static final Point right_threequarters = new Point(23, -66);
        static final Point left_half_1 = new Point(-79, -64/* ,-41, -64 */);
        static final Point left_half_2 = new Point(-99, -64/* ,-61, -64 */);
        static final Point left_half_3 = new Point(-119, -64/* ,-81, -64 */);
        static final Point right_half_1 = new Point(/* 56, -64, */18, -64);
        static final Point right_half_2 = new Point(/* 76, -64, */38, -64);
        static final Point right_half_3 = new Point(/* 96, -64, */58, -64);
        static final Point forecast_window = new Point(-179, 0);
        static final Point forecast_frame = new Point(-81,0);
        static final TextPosition forecast_date = new TextPosition(forecast_window.getX() + 60, 4);
        static final TextPosition forecast_temp = new TextPosition(forecast_window.getX() + 7, 4);
        static final Point forecast_unit = new Point(forecast_window.getX() + 34, 4);
        static final Point forecast_marker = new Point(-158, 0);
        static final TextPosition act_title = new TextPosition(5, 70);
        static final Point act_split = new Point(5, 80);
        
        static final TextPosition act_subtitle = new TextPosition(5, 83);
        static final Point sign=new Point(1, 12);
        static final Point unit=new Point(11, 24);
        
        static final PointSet[] digits=new PointSet[] {
        new PointSet(new Point(13, 7 ), new Point(25, 16), sign, unit),
        new PointSet(new Point(8 , 7 ), new Point(18, 7 ), new Point(28, 16), sign, unit),
        new PointSet(new Point(7 , 7 ), new Point(14, 7 ), new Point(24, 7 ), sign, unit)};
    }
    static final class HUDElements {
        static final TexturedUV hotbar_slot = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 1, 20, 20);
        static final TexturedUV off_hand_slot = new TexturedUV(FrostedHud.HUD_ELEMENTS,22, 1, 22, 22);
        static final TexturedUV selected = new TexturedUV(FrostedHud.HUD_ELEMENTS,108, 109, 22, 22);
        static final TexturedUV exp_bar_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,45, 1, 184, 7);
        static final TexturedUV temperature_orb_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 24, 43, 43);
        static final TexturedUV left_threequarters_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,45, 9, 36, 38);
        static final TexturedUV right_threequarters_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 113, 36, 38);
        static final TexturedUV left_half_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,82, 9, 23, 24 + 10);
        static final TexturedUV right_half_frame = new TexturedUV(FrostedHud.HUD_ELEMENTS,106, 9, 23, 24 + 10);
        static final TexturedUV exp_bar = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 70, 182, 5);
        static final TexturedUV hypothermia_bar = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 152, 182, 5);
        static final TexturedUV hyperthermia_bar = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 164, 182, 5);
        static final TexturedUV dangerthermia_bar = new TexturedUV(FrostedHud.HUD_ELEMENTS,1, 158, 182, 5);
        static final TexturedUV icon_health_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,130, 9, 12, 12);
        static final TexturedUV icon_health_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,130, 22, 12, 12);
        static final TexturedUV icon_health_abnormal_green = new TexturedUV(FrostedHud.HUD_ELEMENTS,130, 35, 12, 12);
        static final TexturedUV icon_health_abnormal_black = new TexturedUV(FrostedHud.HUD_ELEMENTS,130, 48, 12, 12);
        static final TexturedUV icon_health_abnormal_cyan = new TexturedUV(FrostedHud.HUD_ELEMENTS,195, 9, 12, 12);
        static final TexturedUV icon_health_hardcore_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,208, 9, 12, 12);
        static final TexturedUV icon_health_hardcore_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,208, 22, 12, 12);
        static final TexturedUV icon_health_abnormal_orange = new TexturedUV(FrostedHud.HUD_ELEMENTS,221, 9, 12, 12);
        static final TexturedUV icon_health_hardcore_abnormal_orange = new TexturedUV(FrostedHud.HUD_ELEMENTS,221, 22, 12, 12);
        static final TexturedUV icon_health_hardcore_abnormal_green = new TexturedUV(FrostedHud.HUD_ELEMENTS,208, 35, 12, 12);
        static final TexturedUV icon_health_hardcore_abnormal_black = new TexturedUV(FrostedHud.HUD_ELEMENTS,208, 48, 12, 12);
        static final TexturedUV icon_health_hardcore_abnormal_cyan = new TexturedUV(FrostedHud.HUD_ELEMENTS,195, 22, 12, 12);
        static final TexturedUV icon_hunger_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,143, 9, 12, 12);
        static final TexturedUV icon_hunger_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,143, 22, 12, 12);
        static final TexturedUV icon_hunger_abnormal_green = new TexturedUV(FrostedHud.HUD_ELEMENTS,143, 35, 12, 12);
        static final TexturedUV icon_thirst_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,156, 9, 12, 12);
        static final TexturedUV icon_thirst_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,156, 22, 12, 12);
        static final TexturedUV icon_thirst_abnormal_green = new TexturedUV(FrostedHud.HUD_ELEMENTS,156, 35, 12, 12);
        static final TexturedUV icon_oxygen_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,169, 9, 12, 12);
        static final TexturedUV icon_oxygen_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,169, 22, 12, 12);
        static final TexturedUV icon_oxygen_abnormal_green = new TexturedUV(FrostedHud.HUD_ELEMENTS,169, 35, 12, 12);
        static final TexturedUV icon_defence_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,182, 9, 12, 12);
        static final TexturedUV icon_defence_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,182, 22, 12, 12);
        static final TexturedUV icon_horse_normal = new TexturedUV(FrostedHud.HUD_ELEMENTS,143, 48, 12, 12);
        static final TexturedUV icon_horse_abnormal_white = new TexturedUV(FrostedHud.HUD_ELEMENTS,156, 48, 12, 12);
        static final TexturedUV forecast_window = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 0, 358, 16, 512, 256);
        static final TexturedUV forecast_increase = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 32, 12, 12, 512, 256);
        static final TexturedUV forecast_decrease = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 44, 12, 12, 512, 256);
        static final TexturedUV[] hud_decimal_digits=new TexturedUV[10];
        static final TexturedUV[] hud_integer_digits=new TexturedUV[10];
        static {
        	for(int i=0;i<10;i++) {
        		hud_decimal_digits[(i+1)%10]=new TexturedUV(FrostedHud.digits,6*i, 17, 6, 8, 100, 34);
        		hud_integer_digits[(i+1)%10]=new TexturedUV(FrostedHud.digits,10*i, 0, 10, 17, 100, 34);
        	}
        }
        static final TexturedUV hud_positive=TexturedUV.deltaWH(FrostedHud.digits,61, 17, 68, 24, 100, 34);
        static final TexturedUV hud_negative=TexturedUV.deltaWH(FrostedHud.digits,68, 17, 75, 24, 100, 34);
        static final TexturedUV hud_celsius =TexturedUV.deltaWH(FrostedHud.digits,0, 25, 13, 34, 100, 34);
        static final TexturedUV hud_farenhit=TexturedUV.deltaWH(FrostedHud.digits,13, 25, 26, 34, 100, 34);
        
        static final TexturedUV forecast_snow = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 56, 12, 12, 512, 256);
        static final TexturedUV forecast_blizzard = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 68, 12, 12, 512, 256);
        static final TexturedUV forecast_sun = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 80, 12, 12, 512, 256);
        static final TexturedUV forecast_cloud = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 92, 12, 12, 512, 256);
        static final TexturedUV forecast_marker = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,0, 16, 50, 4, 512, 256);
        static final TexturedUV forecast_celsius = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,12, 32, 7, 7, 512, 256);
        static final TexturedUV forecast_fehrenheit = new TexturedUV(FrostedHud.FORECAST_ELEMENTS,19, 32, 7, 7, 512, 256);
        static final UV temperature_orb= new UV(0,0,36,36,36,36);
    }
    static final class IconPos {
        static final Point left_threequarters = new Point(-47, -56);
        static final Point right_threequarters = new Point(35, -56);
        static final Point left_half_1 = new Point(-71, -54/* ,-33, -54 */);
        static final Point left_half_2 = new Point(-91, -54/* ,-53, -54 */);
        static final Point left_half_3 = new Point(-111, -54/* ,-73, -54 */);
        static final Point right_half_1 = new Point(/* 59, -54, */21, -54);
        static final Point right_half_2 = new Point(/* 79, -54, */41, -54);
        static final Point right_half_3 = new Point(/* 99, -54, */61, -54);
    }
    public static boolean renderHotbar = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderThirst = true;
    public static boolean renderHealthMount = true;
    public static boolean renderExperience = true;
    public static boolean renderJumpBar = true;

    public static boolean renderHypothermia = true;
    public static boolean renderFrozenOverlay = true;
    public static boolean renderForecast = true;
    public static boolean renderFrozenVignette = true;
    public static boolean renderHeatVignette = true;
    public static boolean renderWaypoint = true;
    public static boolean renderDebugOverlay = false;
	public static float smoothedBody;
    static final ResourceLocation HUD_ELEMENTS = new ResourceLocation(FHMain.MODID, "textures/gui/hud/hudelements.png");
    // static final ResourceLocation FROZEN_OVERLAY_PATH = new
    // ResourceLocation(FHMain.MODID, "textures/gui/hud/frozen_overlay.png");
    static final ResourceLocation FROZEN_OVERLAY = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_overlay.png");
    static final ResourceLocation FROZEN_OVERLAY_1 = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_stage_1.png");
    static final ResourceLocation FROZEN_OVERLAY_2 = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_stage_2.png");
    static final ResourceLocation FROZEN_OVERLAY_3 = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_stage_3.png");

    static final ResourceLocation FROZEN_OVERLAY_4 = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_stage_4.png");

    static final ResourceLocation FROZEN_OVERLAY_5 = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/frozen_stage_5.png");

    static final ResourceLocation FIRE_VIGNETTE = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/fire_vignette.png");

    static final ResourceLocation ICE_VIGNETTE = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/ice_vignette.png");

    //    static final ResourceLocation LEFT_HALF_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/left_half.png");
//    static final ResourceLocation RIGHT_HALF_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/right_half.png");
//    static final ResourceLocation LEFT_THREEQUARTERS_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/left_threequarters.png");
//    static final ResourceLocation RIGHT_THREEQUARTERS_MASK = new ResourceLocation(FHMain.MODID, "textures/gui/hud/mask/right_threequarters.png");
    static final ResourceLocation FORECAST_ELEMENTS = new ResourceLocation(FHMain.MODID,
            "textures/gui/hud/forecast_elements.png");

    private static final Map<Integer, Integer> clrs = new HashMap<>();

    static {
        clrs.put(2, 0x99FF9800);
        clrs.put(1, 0x44FF9800);
        clrs.put(0, 0x0);
        clrs.put(-1, 0x3357BDE8);
        clrs.put(-2, 0x4457BDE8);
        clrs.put(-3, 0x5557BDE8);
        clrs.put(-4, 0x6657BDE8);
        clrs.put(-5, 0x7757BDE8);
        clrs.put(-6, 0x8857BDE8);
        clrs.put(-7, 0x9957BDE8);
        clrs.put(-8, 0xaa57BDE8);
        clrs.put(-9, 0xbb57BDE8);
        clrs.put(-10, 0xcc57BDE8);
        clrs.put(-11, 0xdd57BDE8);
        clrs.put(-12, 0xee57BDE8);
        clrs.put(-13, 0xff57BDE8);
    }

    static final ResourceLocation digits = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/digits.png");

    static final ResourceLocation change_rate_arrows = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/change_rate_arrows.png");

    static final ResourceLocation ardent = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/ardent.png");

    static final ResourceLocation fervid = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/fervid.png");

    static final ResourceLocation hot = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/hot.png");

    static final ResourceLocation warm = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/warm.png");

    static final ResourceLocation moderate = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/moderate.png");

    static final ResourceLocation chilly = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/chilly.png");

    static final ResourceLocation cold = new ResourceLocation(FHMain.MODID, "textures/gui/temperature_orb/cold.png");

    static final ResourceLocation frigid = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/frigid.png");

    static final ResourceLocation hadean = new ResourceLocation(FHMain.MODID,
            "textures/gui/temperature_orb/hadean.png");
    static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
    private static double calculateHypoBarLength(double startTemp, double endTemp, float curtemp) {
        double atemp = Math.abs(curtemp);
        if (atemp < startTemp)
            return 0;
        return (Math.min(atemp, endTemp) - startTemp) / (endTemp - startTemp);
    }


    private static ArrayList<TexturedUV> getIntegerDigitUVs(int digit) {

        ArrayList<TexturedUV> rtn = new ArrayList<>(3);
        do{
        	int crnDigit=digit%10;
        	digit=digit/10;
        	rtn.add(0, HUDElements.hud_integer_digits[crnDigit]);
        }while(digit>0&&rtn.size()<3);
        return rtn;
    }

    public static Player getRenderViewPlayer() {
        return !(Minecraft.getInstance().getCameraEntity() instanceof Player) ? null
                : (Player) Minecraft.getInstance().getCameraEntity();
    }

    public static void renderAirBar(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_air");
        RenderSystem.enableBlend();
        int air = player.getAirSupply();
        int maxAir = 300;
        if (player.isEyeInFluid(FluidTags.WATER) || air < maxAir) {
            
            HUDElements.right_half_frame.blitAt(stack, x, y, BasePos.right_half_3);
            if (air <= 30) {
                HUDElements.icon_oxygen_abnormal_white.blitAt(stack, x, y, IconPos.right_half_3);
            } else {
                HUDElements.icon_oxygen_normal.blitAt(stack, x, y, IconPos.right_half_3);
            }
            int airState = Mth.ceil(air / (float) maxAir * 100) - 1;
            Atlases.oxygen_bar.blitAtlasVH(stack, x, y, BarPos.right_half_3, airState);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderScenarioAct(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_scenario_act");
       /* double guiScale = mc.getMainWindow().getGuiScaleFactor();
        int ww = mc.getMainWindow().getScaledWidth();
		int wh = mc.getMainWindow().getScaledHeight();
    	float scale = (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4D / guiScale);
    	float minimapRotation = (FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() ? 180F : -mc.player.rotationYaw) % 360F;

    	int s = (int) (64D * scale);
    	double s2d = s / 2D;
    	float s2f = s / 2F;
    	int x = FTBChunksClientConfig.MINIMAP_POSITION.get().getX(ww, s);
    	int y = FTBChunksClientConfig.MINIMAP_POSITION.get().getY(wh, s);//Render our act screen in lower than map;
    	*/
        RenderSystem.enableBlend();

        if (ClientScene.INSTANCE!=null) {
        	Component t=ClientScene.INSTANCE.getCurrentActTitle();
        	Component st=ClientScene.INSTANCE.getCurrentActSubtitle();
        	if(t!=null||st!=null) {
        		int deflen=60;
	        	
	            if(t!=null) { 
	            	int len=mc.font.width(t.getString());
	            	deflen=Math.max(deflen, len-30);
	            	if(ClientScene.INSTANCE.ticksActUpdate>0)
		        		GuiHelper.pushScissor(mc.getWindow(), BasePos.act_title.getX(), BasePos.act_title.getY(), (int) (len*(1-ClientScene.INSTANCE.ticksActUpdate/20f)),40);
	            	BasePos.act_title.drawText(stack, t, 0xfeff06);
	            	if(ClientScene.INSTANCE.ticksActUpdate>0)
	            		GuiHelper.popScissor(mc.getWindow());
	            }
	            stack.hLine(BasePos.act_split.getX(), BasePos.act_split.getX()+deflen, BasePos.act_split.getY(), 0xFFFFFF06);
	            
	            if(st!=null) {
	            	int len=mc.font.width(st.getString());
	            	if(ClientScene.INSTANCE.ticksActStUpdate>0)
		        		GuiHelper.pushScissor(mc.getWindow(), BasePos.act_title.getX(), BasePos.act_title.getY(), (int) (len*(1-ClientScene.INSTANCE.ticksActStUpdate/20f)),40);
	            	BasePos.act_subtitle.drawText(stack, st, 0xffffff);
	            	if(ClientScene.INSTANCE.ticksActStUpdate>0)
		            	GuiHelper.popScissor(mc.getWindow());
	            }
	            
        	}
            
        }
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderArmor(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_armor");
        RenderSystem.enableBlend();

        HUDElements.left_half_frame.blitAt(stack, x, y, BasePos.left_half_1);
        HUDElements.icon_defence_normal.blitAt(stack, x, y, IconPos.left_half_1);
        int armorValue = player.getArmorValue();
        int armorValueState = Mth.ceil(armorValue / 20.0F * 100) - 1;
        Atlases.defence_bar.blitAtlasVH(stack, x, y, BarPos.left_half_1, armorValueState);

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderExperience(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_experience");

        RenderSystem.enableBlend();

        HUDElements.exp_bar_frame.blitAt(stack, x, y, BasePos.exp_bar);
        int i = mc.player.getXpNeededForNextLevel();
        if (i > 0) {
            HUDElements.exp_bar.blit(stack, x, y, BarPos.exp_bar, Transition.RIGHT, mc.player.experienceProgress);
        }
        if (mc.player.experienceLevel > 0) {
            String s = "" + mc.player.experienceLevel;
            int i1 = (x * 2 - mc.font.width(s)) / 2;
            int j1 = y - 29;

            stack.drawString(mc.font, s, i1 + 1, j1, 0, false);
            stack.drawString(mc.font, s, i1 - 1, j1, 0, false);
            stack.drawString(mc.font, s, i1, j1 + 1, 0, false);
            stack.drawString(mc.font, s, i1, j1 - 1, 0, false);
            stack.drawString(mc.font, s, i1, j1, 8453920, false);
        }

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderInsight(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_insight");

        RenderSystem.enableBlend();

        HUDElements.exp_bar_frame.blitAt(stack, x, y, BasePos.exp_bar);
        TeamResearchData data = ClientResearchDataAPI.getData().get();
        float progress = data.getInsightProgress();
        int level = data.getInsightLevel();
        if (progress > 0) {
            HUDElements.exp_bar.blit(stack, x, y, BarPos.exp_bar, Transition.RIGHT, progress);
        }
        if (level > 0) {
            String s = "" + level;
            int i1 = (x * 2 - mc.font.width(s)) / 2;
            int j1 = y - 29;

            stack.drawString(mc.font, s, i1 + 1, j1, 0, false);
            stack.drawString(mc.font, s, i1 - 1, j1, 0, false);
            stack.drawString(mc.font, s, i1, j1 + 1, 0, false);
            stack.drawString(mc.font, s, i1, j1 - 1, 0, false);
            stack.drawString(mc.font, s, i1, j1, 8453920, false);
        }

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderFood(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_food");
        RenderSystem.enableBlend();

        HUDElements.right_half_frame.blitAt(stack, x, y, BasePos.right_half_1);
        MobEffectInstance effectInstance = mc.player.getEffect(MobEffects.HUNGER);
        boolean isHunger = effectInstance != null;
        if (isHunger) {
            HUDElements.icon_hunger_abnormal_green.blitAt(stack, x, y, IconPos.right_half_1);
        } else {
            HUDElements.icon_hunger_normal.blitAt(stack, x, y, IconPos.right_half_1);
        }
        FoodData stats = mc.player.getFoodData();
        int foodLevel = stats.getFoodLevel();
        if (foodLevel > 0) {
            int foodLevelState = Mth.ceil(foodLevel / 20.0F * 100) - 1;
            Atlases.hunger_bar.blitAtlasVH(stack, x, y, BarPos.right_half_1, foodLevelState);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderForecast(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_forecast");
        RenderSystem.enableBlend();

        long date = ClientClimateData.getDate();
        int hourInDay = ClientClimateData.getHourInDay();
        int segmentLength = 13; // we have 4 segments in each day, total 5 day in window, 20 segments.
        int markerLength = 50;
        int markerMovingOffset = hourInDay / 6 * segmentLength; // divide by 6 to get segment index
        int windowOffset = 98; // distance to window edge (skip day window)
        int windowX = x + BasePos.forecast_window.getX() + windowOffset;
        int markerV = HUDElements.forecast_marker.getY();
        int markerH = HUDElements.forecast_marker.getH();
        int firstDayU = HUDElements.forecast_marker.getX() + markerMovingOffset + 2;
        int firstDayW = HUDElements.forecast_marker.getW() - markerMovingOffset - 2;
        // forecast arrows
        // find the first hour lower than cold period bottom
        TemperatureFrame[] toRender = ClientClimateData.forecastData;
        int lastStart = 0;
        int lastLevel = 0;
        int i = -1;
        for (TemperatureFrame fr : toRender) {
            i++;
            if (fr != null) {
                if (lastLevel != fr.toState) {
                    if (lastStart != i) {
                        if (lastLevel != 0) {
                            int end = windowX + i * segmentLength / 2 - 2;
                            int clr = clrs.get(lastLevel);
                            stack.fillGradient(end, 1, end + 6, 15, clr, clrs.get((int) fr.toState));
                            int start = windowX + lastStart * segmentLength / 2 + 4;
                            if (lastStart == 0)
                                start -= 3;
                            stack.fill( start, 1, end, 15, clr);
                        } else if (lastLevel == 0) {
                            int end = windowX + i * segmentLength / 2 - 2;
                            stack.fillGradient(end, 1, end + 6, 15, 0, clrs.get((int) fr.toState));
                        }
                    }
                    lastStart = i;
                    lastLevel = fr.toState;
                }
            }
        }
        if (lastLevel != 0 && lastStart * segmentLength / 2 < 253) {
        	stack.fill(windowX + lastStart * segmentLength / 2 + 4, 1, windowX + 257, 15,
                    clrs.get(lastLevel));
        }
        RenderSystem.enableBlend();
        // window
        HUDElements.forecast_window.blitAt(stack, x, 0, BasePos.forecast_window);
        // markers (moving across window by hour)
        stack.blit(FrostedHud.FORECAST_ELEMENTS,windowX + 2, 0, firstDayU, markerV, firstDayW, markerH, 512, 256);

        HUDElements.forecast_marker.blit(stack, x - markerMovingOffset + markerLength, 0, BasePos.forecast_frame);
        HUDElements.forecast_marker.blit(stack, x - markerMovingOffset + markerLength * 2, 0, BasePos.forecast_frame);
        HUDElements.forecast_marker.blit(stack, x - markerMovingOffset + markerLength * 3, 0, BasePos.forecast_frame);
        HUDElements.forecast_marker.blit(stack, x - markerMovingOffset + markerLength * 4, 0, BasePos.forecast_frame);
        HUDElements.forecast_marker.blit(stack, x - markerMovingOffset + markerLength * 5, 0, BasePos.forecast_frame, 257 - markerLength * 5 + markerMovingOffset + 2);


        FrameType last = FrameType.NOP;
        for (i = 0; i < toRender.length; i++) {
            TemperatureFrame fr = toRender[i];
            if (fr == null) {
                last = FrameType.NOP;
                continue;
            }
            TexturedUV uv = null;
            if (fr.type == FrameType.INCRESING)
                uv = HUDElements.forecast_increase;
            if (fr.type == FrameType.DECREASING)
                uv = HUDElements.forecast_decrease;
            if (fr.type == FrameType.SNOWING)
                uv = HUDElements.forecast_snow;
            if (fr.type == FrameType.STORMING)
                uv = HUDElements.forecast_blizzard;
            if (fr.type == FrameType.RETREATING)
                uv = HUDElements.forecast_sun;
            if ((last.isWeatherEvent() && !fr.type.isWeatherEvent()) || (toRender.length > i + 1 && toRender[i + 1] != null && toRender[i + 1].type.isWeatherEvent())) {
                uv = null;
            }
            last = fr.type;
            if (uv != null)
                uv.blit(stack, windowX + i * segmentLength / 2 - (i == 0 ? 0 : 1), 3);
        }
        boolean f = FHConfig.CLIENT.useFahrenheit.get();
        float temperature = 0;
        float tlvl = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getEnvTemp).orElse(0F);
        tlvl = Math.max(-273, tlvl);
        TexturedUV unit;
        if (f) {
            temperature = (tlvl * 9 / 5 + 32);
            unit = HUDElements.forecast_fehrenheit;
        } else {
            temperature = tlvl;
            unit = HUDElements.forecast_celsius;
        }
        unit.blit(stack, x, 0, BasePos.forecast_unit);
        // day render
        BasePos.forecast_date.drawText(stack,"" + date,x,0,0xe6e6f2);

        BasePos.forecast_temp.drawText(stack,"" + Math.round(temperature),x,0,0xe6e6f2);


        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderFrozenOverlay(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_frozen");
        // render frozen overlay with alpha based on linear interpolation
        float tempDelta = Mth.clamp(Math.abs(PlayerTemperatureData.getCapability(player).map(t->t.smoothedBody).orElse(0F)), 0.5f, 5.0f);
        float opacityDelta = (tempDelta - 0.5F) / 4.5F;
        ResourceLocation texture;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
       // RenderSystem.disableAlphaTest();
        texture = FROZEN_OVERLAY;
        mc.getTextureManager().bindForSetup(texture);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(0.0D, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 1.0F).endVertex();
        bufferbuilder.vertex((double) x * 2, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 1.0F).endVertex();
        bufferbuilder.vertex((double) x * 2, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 0.0F).endVertex();
        bufferbuilder.vertex(0.0D, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 0.0F).endVertex();
        tessellator.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        //RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderFrozenVignette(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_vignette");
        float tempDelta = Mth.clamp(Math.abs(PlayerTemperatureData.getCapability(player).map(t->t.smoothedBody).orElse(0F)), 0.5f, 5.0f);
        float opacityDelta = 0.5F * (tempDelta - 0.5F) / 4.5F;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        //RenderSystem.disableAlphaTest();
        mc.getTextureManager().bindForSetup(ICE_VIGNETTE);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        buffer.vertex(0.0D, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(x * 2, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(x * 2, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(0.0D, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 0.0F).endVertex();
        tessellator.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        //RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderHealth(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_health");
        RenderSystem.enableBlend();

        HUDElements.left_threequarters_frame.blitAt(stack, x, y, BasePos.left_threequarters);

        float health = player.getHealth();
        // ModifiableAttributeInstance attrMaxHealth =
        // player.getAttribute(Attributes.MAX_HEALTH);
        float omax;
        float healthMax = omax = /* (float) attrMaxHealth.getValue() */player.getMaxHealth();

        if (healthMax < 20)
            healthMax = 20;
        float absorb = player.getAbsorptionAmount(); // let's say max is 20

        TexturedUV heart;
        if (mc.level.getLevelData().isHardcore()) {
            if (player.hasEffect(MobEffects.WITHER)) {
                heart = HUDElements.icon_health_hardcore_abnormal_black;
            } else if (player.hasEffect(MobEffects.POISON)) {
                heart = HUDElements.icon_health_hardcore_abnormal_green;
            } else if (player.hasEffect(FHMobEffects.HYPOTHERMIA.get())) {
                heart = HUDElements.icon_health_hardcore_abnormal_cyan;
            } else if (player.hasEffect(FHMobEffects.HYPERTHERMIA.get())) {
                heart = HUDElements.icon_health_hardcore_abnormal_orange;
            } else
                heart = HUDElements.icon_health_hardcore_normal;
        } else {
            if (player.hasEffect(MobEffects.WITHER)) {
                heart = HUDElements.icon_health_abnormal_black;
            } else if (player.hasEffect(MobEffects.POISON)) {
                heart = HUDElements.icon_health_abnormal_green;
            } else if (player.hasEffect(FHMobEffects.HYPOTHERMIA.get())) {
                heart = HUDElements.icon_health_abnormal_cyan;
            } else if (player.hasEffect(FHMobEffects.HYPERTHERMIA.get())) {
                heart = HUDElements.icon_health_abnormal_orange;
            } else
                heart = HUDElements.icon_health_normal;
        }
        heart.blitAt(stack, x, y, IconPos.left_threequarters);

        // range: [0, 99]
        int mhealthState = omax >= 20 ? 99 : Mth.ceil(omax / 20 * 100) - 1;
        int healthState = health <= 0 ? 0 : Mth.ceil(health / healthMax * 100) - 1;
        int absorbState = absorb <= 0 ? 0 : Mth.ceil(absorb / 20 * 100) - 1;
        Atlases.maxhealth_bar.blitAtlasVH(stack, x, y, BarPos.left_threequarters_inner, mhealthState);
        Atlases.health_bar.blitAtlasVH(stack, x, y, BarPos.left_threequarters_inner, healthState);
        Atlases.absorption_bar.blitAtlasVH(stack, x, y, BarPos.left_threequarters_outer, absorbState);
        int ihealth = (int) Math.ceil(health);
        stack.drawCenteredString(mc.font, String.valueOf(ihealth), x + BasePos.left_threequarters.getX() + HUDElements.left_threequarters_frame.getW() / 2,
        		y + BasePos.left_threequarters.getY() + HUDElements.left_threequarters_frame.getH() / 2, 0xFFFFFF);


        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderHeatVignette(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_vignette");
        float tempDelta = Mth.clamp(Math.abs(PlayerTemperatureData.getCapability(player).map(t->t.smoothedBody).orElse(0F)), 0.5f, 5.0f);
        float opacityDelta = 0.5F * (tempDelta - 0.5F) / 4.5F;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        //RenderSystem.disableAlphaTest();
        mc.getTextureManager().bindForSetup(FIRE_VIGNETTE);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        buffer.vertex(0.0D, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(x * 2, y, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(x * 2, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(0.0D, 0.0D, -90.0D).color(1f, 1f, 1f, Math.min(opacityDelta, 1)).uv(0.0F, 0.0F).endVertex();
        tessellator.end();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        //RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderHotbar(GuiGraphics stack, int x, int y, Minecraft mc, Player player,
                                    float partialTicks) {
        mc.getProfiler().push("frostedheart_hotbar");
        RenderSystem.enableBlend();

        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_1);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_2);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_3);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_4);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_5);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_6);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_7);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_8);
        HUDElements.hotbar_slot.blitAt(stack, x, y, BasePos.hotbar_9);

        // Selection overlay
        HUDElements.selected.blitAt(stack, x - 1 + player.getInventory().selected * 20, y - 1,
                BasePos.hotbar_1);

        if (player.getOffhandItem().isEmpty()) {
            HUDElements.off_hand_slot.blitAt(stack, x, y, BasePos.off_hand);
        } else {
            HUDElements.selected.blitAt(stack, x, y, BasePos.off_hand);
        }

        ItemStack itemstack = player.getOffhandItem();
        HumanoidArm handside = player.getMainArm().getOpposite();
        //RenderSystem.enableRescaleNormal();
        int l=1;
        for (int i1 = 0; i1 < 9; ++i1) {
            int j1 = x - 90 + i1 * 20 + 2;
            int k1 = y - 16 - 3 + 1; // +1
            renderHotbarItem(stack,j1, k1, partialTicks, player, player.getInventory().items.get(i1),l++);
        }

        if (!itemstack.isEmpty()) {
            int i2 = y - 16 - 3 + 1; // +1
            if (handside == HumanoidArm.LEFT) {
                renderHotbarItem(stack,x - 91 - 26 - 2 - 2, i2, partialTicks, player, itemstack,l++);
            } else {
                renderHotbarItem(stack,x + 91 + 10, i2, partialTicks, player, itemstack,l++);
            }
        }

        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float f = mc.player.getAttackStrengthScale(0.0F);
            if (f < 1.0F) {
                int j2 = y - 20;
                int k2 = x + 91 + 6;
                if (handside == HumanoidArm.RIGHT) {
                    k2 = x - 91 - 22;
                }

                int l1 = (int) (f * 19.0F);
                stack.blit(GUI_ICONS_LOCATION, k2, j2, 0, 94, 18, 18);
                stack.blit(GUI_ICONS_LOCATION, k2, j2 + 18 - l1, 18, 112 - l1, 18, l1);
            }
        }

        //RenderSystem.disableRescaleNormal();
        RenderSystem.disableBlend();

        mc.getProfiler().pop();
    }
    private static void renderHotbarItem(GuiGraphics pGuiGraphics,int x, int y, float partialTicks, Player player, ItemStack stack,int seed) {
        Minecraft mc = Minecraft.getInstance();
        if (!stack.isEmpty()) {
            float f = stack.getPopTime() - partialTicks;
            if (f > 0.0F) {
                float f1 = 1.0F + f / 5.0F;
                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().translate((float)(x + 8), (float)(y + 12), 0.0F);
                pGuiGraphics.pose().scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                pGuiGraphics.pose().translate((float)(-(x + 8)), (float)(-(y + 12)), 0.0F);
            }

            pGuiGraphics.renderItem(player, stack, x, y,seed);
            if (f > 0.0F) {
            	pGuiGraphics.pose().popPose();
            }

            pGuiGraphics.renderItemDecorations(mc.font, stack, x, y);
        }
    }
    public static void renderHypothermia(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_hypothermia");
        RenderSystem.enableBlend();

        float temp = PlayerTemperatureData.getCapability(player).map(t->t.smoothedBody).orElse(0F);
        HUDElements.exp_bar_frame.blitAt(stack, x, y, BasePos.exp_bar);
//        double startTemp = -0.5, endTemp = -3.0;
//        int k = (int) ((Math.abs(Math.max(TemperatureCore.getBodyTemperature(player), endTemp)) - Math.abs(startTemp)) / (Math.abs(endTemp) - Math.abs(startTemp)) * 181.0F);
        if (temp < -0.5) {
            double stage1length = calculateHypoBarLength(0.5, 1.0, temp);
            double stage2length = calculateHypoBarLength(2.0, 3.0, temp);
            if (stage1length > 0)
                HUDElements.hypothermia_bar.blit(stack, x + 1, y, BarPos.exp_bar, Transition.RIGHT, stage1length);
            if (stage2length > 0)
                HUDElements.dangerthermia_bar.blit(stack, x + 1, y, BarPos.exp_bar, Transition.RIGHT, stage2length);
        } else if (temp > 0.5) {
            double stage1length = calculateHypoBarLength(0.5, 1.0, temp);
            double stage2length = calculateHypoBarLength(2.0, 3.0, temp);
            if (stage1length > 0)
                HUDElements.hyperthermia_bar.blit(stack, x + 1, y, BarPos.exp_bar, Transition.RIGHT, stage1length);
            if (stage2length > 0)
                HUDElements.dangerthermia_bar.blit(stack, x + 1, y, BarPos.exp_bar, Transition.RIGHT, stage2length);
        }
        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderJumpbar(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_jumpbar");
        RenderSystem.enableBlend();

        float jumpPower = player.getJumpRidingScale();
        int jumpState = Mth.ceil(jumpPower * 100) - 1;
        Atlases.horse_jump_bar.blitAtlasVH(stack, x, y, BarPos.right_threequarters_outer, jumpState);

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderMountHealth(GuiGraphics stack, int x, int y, Minecraft mc, LocalPlayer player) {
        mc.getProfiler().push("frostedheart_mounthealth");
        RenderSystem.enableBlend();
        HUDElements.right_threequarters_frame.blitAt(stack, x, y, BasePos.right_threequarters);
        HUDElements.icon_horse_normal.blitAt(stack, x, y, IconPos.right_threequarters);
        Entity tmp = player.getVehicle();
        if (!(tmp instanceof LivingEntity))
            return;
        LivingEntity mount = (LivingEntity) tmp;
        int health = (int) mount.getHealth();
        float healthMax = mount.getMaxHealth();
        int healthState = Mth.ceil(health / healthMax * 100) - 1;
        Atlases.horse_health_bar.blitAtlasVH(stack, x, y, BarPos.right_threequarters_inner, healthState);

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }
    public static void renderSetup(LocalPlayer player, Player renderViewPlayer) {
        // Vanilla replacement
        renderHealth = !player.isCreative() && !player.isSpectator();
        renderHealthMount = renderHealth && player.getVehicle() instanceof LivingEntity;
        renderFood = renderHealth && !renderHealthMount;
        renderThirst = renderHealth && !renderHealthMount;
        renderJumpBar = renderHealth && player.jumpableVehicle()!=null;
        renderArmor = renderHealth && player.getArmorValue() > 0;
        renderExperience = renderHealth;

        // Hypothermia
        float bt = PlayerTemperatureData.getCapability(player).map(t->t.smoothedBody).orElse(0F);
        renderHypothermia = renderHealth && bt < -0.5 || bt > 0.5;

        // Overlay and vignette
        renderFrozenOverlay = FHConfig.CLIENT.enableFrozenOverlay.get() && renderHealth && bt <= -0.5;
        renderFrozenVignette = FHConfig.CLIENT.enableFrozenVignette.get() && renderHealth && bt <= -0.5;
        renderHeatVignette = FHConfig.CLIENT.enableHeatVignette.get() && renderHealth && bt >= 0.5;

        // Waypoint
        renderWaypoint = FHConfig.CLIENT.enableWaypoint.get();

        // Forecast
        boolean configAllows = FHConfig.COMMON.enablesTemperatureForecast.get();
        boolean forceEnables = FHConfig.COMMON.forceEnableTemperatureForecast.get();
        renderForecast = (forceEnables
                || (configAllows && ClientResearchDataAPI.getData().get().getVariantDouble(ResearchVariant.HAS_FORECAST)>0))
        && ((BossHealthOverlayAccess) Minecraft.getInstance().gui.getBossOverlay()).getEvents().isEmpty(); // check if not boss fight
    }

    private static void renderTemp(GuiGraphics stack, Minecraft mc, float temp, int tlevel, int offsetX, int offsetY,
                                   boolean celsius) {
    	TexturedUV unitUV = celsius ? HUDElements.hud_celsius : HUDElements.hud_farenhit;
    	TexturedUV signUV = temp >= 0 ? HUDElements.hud_positive : HUDElements.hud_negative;
 
        double abs = Math.abs(temp);
        ResourceLocation orb;
        // draw orb
        if (tlevel > 80) {
            orb=(ardent);
        } else if (tlevel > 60) {
            orb=(fervid);
        } else if (tlevel > 40) {
            orb=(hot);
        } else if (tlevel > 20) {
            orb=(warm);
        } else if (tlevel > 0) {
            orb=(moderate);
        } else if (tlevel > -20) {
            orb=(chilly);
        } else if (tlevel > -40) {
            orb=(cold);
        } else if (tlevel > -80) {
            orb=(frigid);
        } else {
            orb=(hadean);
        }
        HUDElements.temperature_orb.blit(stack, orb, offsetX, offsetY);

        // draw temperature
        // sign and unit

        //signUV.blit(stack, offsetX, offsetY, BasePos.sign);
        //unitUV.blit(stack, offsetX, offsetY, BasePos.unit);

        // digits
        ArrayList<TexturedUV> uv4is = getIntegerDigitUVs((int) Math.round(abs));
        int size=uv4is.size();
        if(size < 3)
        	uv4is.add(HUDElements.hud_decimal_digits[(int) (Math.round(abs*10)%10)]);
        uv4is.add(signUV);
        uv4is.add(unitUV);
        BasePos.digits[size-1].drawUVs(uv4is, stack, offsetX, offsetY);
        
        // mc.getTextureManager().bindTexture(HUD_ELEMENTS);
    }

    public static void renderTemperature(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_temperature");
        RenderSystem.enableBlend();

        HUDElements.temperature_orb_frame.blitAt(stack, x, y + 3, BasePos.temperature_orb_frame);
        boolean f = FHConfig.CLIENT.useFahrenheit.get();
        float temperature = 0;
        float tlvl = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getFeelTemp).orElse(0F);
        tlvl = Math.max(-273, tlvl);
        if (f)
            temperature = (tlvl * 9 / 5 + 32);
        else
            temperature = tlvl;

        renderTemp(stack, mc, temperature, (int) tlvl, x + BarPos.temp_orb.getX(), y + BarPos.temp_orb.getY() + 3, !f);

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    public static void renderThirst(GuiGraphics stack, int x, int y, Minecraft mc, Player player) {
        mc.getProfiler().push("frostedheart_thirst");
        RenderSystem.enableBlend();

        MobEffectInstance effectInstance = mc.player.getEffect(FHMobEffects.THIRST.get());
        boolean isThirsty = effectInstance != null;
        HUDElements.right_half_frame.blitAt(stack, x, y, BasePos.right_half_2);
        if (isThirsty) {
            HUDElements.icon_thirst_abnormal_green.blitAt(stack, x, y, IconPos.right_half_2);
        } else {
            HUDElements.icon_thirst_normal.blitAt(stack, x, y, IconPos.right_half_2);
        }
        WaterLevelCapability.getCapability(player).ifPresent(data -> {
            int waterLevel = data.getWaterLevel();
            int waterLevelState = Mth.ceil(waterLevel / 20.0F * 100) - 1;
            Atlases.thirst_bar.blitAtlasVH(stack, x, y, BarPos.right_half_2, waterLevelState);
        });

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    private static Screen previousScreen;
    public static void renderDebugOverlay(GuiGraphics stack, Minecraft mc) {
        Screen screen = mc.screen;
        Component text = Component.literal("Current Screen: " + (screen == null ? "Null" : screen.getClass().getSimpleName()));
        if (previousScreen != screen && mc.player != null) {
            mc.player.sendSystemMessage(text);
        }
        stack.drawString(ClientUtils.font(), text, 1, 1, 0xFFFFFFFF);
        previousScreen = screen;
    }
}
