package com.teammoeg.frostedheart.client.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.UV4i;
import com.teammoeg.frostedheart.climate.SurviveTemperature;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.AttackIndicatorStatus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.system.CallbackI;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

public class FrostedHud {
    public static boolean renderHotbar = true;
    public static boolean renderHealth = true;
    public static boolean renderArmor = true;
    public static boolean renderFood = true;
    public static boolean renderThirst = true;
    public static boolean renderHealthMount = true;
    public static boolean renderExperience = true;
    public static boolean renderJumpBar = true;
    public static boolean renderHelmet = true;

    public static final ResourceLocation HUD_ELEMENTS = new ResourceLocation(FHMain.MODID, "textures/gui/hud/hudelements.png");
    protected static final ResourceLocation FROZEN_OVERLAY_PATH = new ResourceLocation(FHMain.MODID, "textures/gui/misc/frozen.png");

    public static void renderSetup() {
        Minecraft mc = Minecraft.getInstance();
        renderHealth = !mc.player.isCreative() && !mc.player.isSpectator();
        renderHealthMount = renderHealth && mc.player.getRidingEntity() instanceof LivingEntity;
        renderFood = renderHealth && !renderHealthMount;
        renderThirst = renderHealth && !renderHealthMount;
        renderJumpBar = renderHealth && mc.player.isRidingHorse();
        renderArmor = renderHealth && mc.player.getTotalArmorValue() > 0;
        renderExperience = renderHealth;
        renderHelmet = renderHealth;
    }

    public static PlayerEntity getRenderViewPlayer() {
        return !(Minecraft.getInstance().getRenderViewEntity() instanceof PlayerEntity) ? null : (PlayerEntity)Minecraft.getInstance().getRenderViewEntity();
    }

    private static void renderHotbarItem(int x, int y, float partialTicks, PlayerEntity player, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!stack.isEmpty()) {
            float f = (float)stack.getAnimationsToGo() - partialTicks;
            if (f > 0.0F) {
                RenderSystem.pushMatrix();
                float f1 = 1.0F + f / 5.0F;
                RenderSystem.translatef((float)(x + 8), (float)(y + 12), 0.0F);
                RenderSystem.scalef(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                RenderSystem.translatef((float)(-(x + 8)), (float)(-(y + 12)), 0.0F);
            }

            mc.getItemRenderer().renderItemAndEffectIntoGUI(player, stack, x, y);
            if (f > 0.0F) {
                RenderSystem.popMatrix();
            }

            mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, stack, x, y);
        }
    }

    public static void renderHotbar(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player, float partialTicks) {
        mc.getProfiler().startSection("frostedheart_hotbar");
        PlayerEntity playerentity = getRenderViewPlayer();

        mc.ingameGUI.blit(stack, x + BasePos.off_hand.getA(), y + BasePos.off_hand.getB(), UV.off_hand_slot.x, UV.off_hand_slot.y, UV.off_hand_slot.w, UV.off_hand_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_1.getA(), y + BasePos.hotbar_1.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_2.getA(), y + BasePos.hotbar_2.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_3.getA(), y + BasePos.hotbar_3.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_4.getA(), y + BasePos.hotbar_4.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_5.getA(), y + BasePos.hotbar_5.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_6.getA(), y + BasePos.hotbar_6.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_7.getA(), y + BasePos.hotbar_7.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_8.getA(), y + BasePos.hotbar_8.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);
        mc.ingameGUI.blit(stack, x + BasePos.hotbar_9.getA(), y + BasePos.hotbar_9.getB(), UV.hotbar_slot.x, UV.hotbar_slot.y, UV.hotbar_slot.w, UV.hotbar_slot.h);

        if (playerentity != null) {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            ItemStack itemstack = playerentity.getHeldItemOffhand();
            HandSide handside = playerentity.getPrimaryHand().opposite();

            RenderSystem.enableRescaleNormal();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableAlphaTest();

            for(int i1 = 0; i1 < 9; ++i1) {
                int j1 = x - 90 + i1 * 20 + 2;
                int k1 = y - 16 - 3 + 1; // +1
                renderHotbarItem(j1, k1, partialTicks, playerentity, playerentity.inventory.mainInventory.get(i1));
            }

            if (!itemstack.isEmpty()) {
                int i2 = y - 16 - 3 + 1; // +1
                if (handside == HandSide.LEFT) {
                    renderHotbarItem(x - 91 - 26 - 2 - 2, i2, partialTicks, playerentity, itemstack);
                } else {
                    renderHotbarItem(x + 91 + 10, i2, partialTicks, playerentity, itemstack);
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
                    int l1 = (int)(f * 19.0F);
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.ingameGUI.blit(stack, k2, j2, 0, 94, 18, 18);
                    mc.ingameGUI.blit(stack, k2, j2 + 18 - l1, 18, 112 - l1, 18, l1);
                }
            }

            RenderSystem.disableRescaleNormal();
            RenderSystem.disableBlend();
            RenderSystem.enableAlphaTest();
        }


        mc.getProfiler().endSection();
    }

    public static void renderExperience(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_experience");
        mc.ingameGUI.blit(stack, x + BasePos.exp_bar.getA(), y + BasePos.exp_bar.getB(), UV.exp_bar_frame.x, UV.exp_bar_frame.y, UV.exp_bar_frame.w, UV.exp_bar_frame.h);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        int i = mc.player.xpBarCap();
        if (i > 0) {
            int j = 182;
            int k = (int)(mc.player.experience * 183.0F);
            int l = y - 32 + 3;
//            mc.ingameGUI.blit(stack, x - 91, l, 0, 64, 182, 5);
            if (k > 0) {
                mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA(), y + BarPos.exp_bar.getB(), UV.exp_bar.x, UV.exp_bar.y, k, UV.exp_bar.h);
            }
        }
        if (mc.player.experienceLevel > 0) {
            String s = "" + mc.player.experienceLevel;
            int i1 = (x * 2 - mc.fontRenderer.getStringWidth(s)) / 2;
            int j1 = y - 29;
            mc.fontRenderer.drawString(stack, s, (float)(i1 + 1), (float)j1, 0);
            mc.fontRenderer.drawString(stack, s, (float)(i1 - 1), (float)j1, 0);
            mc.fontRenderer.drawString(stack, s, (float)i1, (float)(j1 + 1), 0);
            mc.fontRenderer.drawString(stack, s, (float)i1, (float)(j1 - 1), 0);
            mc.fontRenderer.drawString(stack, s, (float)i1, (float)j1, 8453920);
        }
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        mc.getProfiler().endSection();
    }

    public static void renderHypothermia(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_hypothermia");
        int k = (int)((10 - Math.abs(SurviveTemperature.getBodyTemperature(player))) / 9 * 183.0F);
        mc.ingameGUI.blit(stack, x + BarPos.exp_bar.getA(), y + BarPos.exp_bar.getB(), UV.hypothermia_bar.x, UV.hypothermia_bar.y, k, UV.hypothermia_bar.h);
        mc.getProfiler().endSection();
    }

    public static void renderHealth(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_health");
        mc.ingameGUI.blit(stack, x + BasePos.left_threequarters.getA(), y + BasePos.left_threequarters.getB(), UV.left_threequarters_frame.x, UV.left_threequarters_frame.y, UV.left_threequarters_frame.w, UV.left_threequarters_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.left_threequarters.getA(), y + IconPos.left_threequarters.getB(), UV.icon_health_normal.x, UV.icon_health_normal.y, UV.icon_health_normal.w, UV.icon_health_normal.h);
        mc.getProfiler().endSection();
    }

    public static void renderFood(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_food");
        mc.ingameGUI.blit(stack, x + BasePos.right_half_1.getB().getA(), y + BasePos.right_half_1.getB().getB(), UV.right_half_frame.x, UV.right_half_frame.y, UV.right_half_frame.w, UV.right_half_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.right_half_1.getB().getA(), y + IconPos.right_half_1.getB().getB(), UV.icon_hunger_normal.x, UV.icon_hunger_normal.y, UV.icon_hunger_normal.w, UV.icon_hunger_normal.h);
        mc.getProfiler().endSection();
    }

    public static void renderThirst(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_thirst");
        mc.ingameGUI.blit(stack, x + BasePos.right_half_2.getB().getA(), y + BasePos.right_half_2.getB().getB(), UV.right_half_frame.x, UV.right_half_frame.y, UV.right_half_frame.w, UV.right_half_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.right_half_2.getB().getA(), y + IconPos.right_half_2.getB().getB(), UV.icon_thirst_normal.x, UV.icon_thirst_normal.y, UV.icon_thirst_normal.w, UV.icon_thirst_normal.h);
        mc.getProfiler().endSection();
    }

    public static void renderTemperature(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_temperature");
        mc.ingameGUI.blit(stack, x + BasePos.temperature_orb_frame.getA(), y + BasePos.temperature_orb_frame.getB() + 3, UV.temperature_orb_frame.x, UV.temperature_orb_frame.y, UV.temperature_orb_frame.w, UV.temperature_orb_frame.h);
        if (mc.world != null) {
            BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
            if (mc.world.chunkExists(pos.getX() >> 4, pos.getZ() >> 4)) {
                int temperature = (int) ChunkData.getTemperature(mc.world, pos);
                renderTemp(stack, mc, temperature, x + BarPos.temp_orb.getA(), y + BarPos.temp_orb.getB() + 3, true);
            }
        }
        mc.getProfiler().endSection();
    }

    public static void renderArmor(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_armor");
        if (renderHealthMount) {
            mc.ingameGUI.blit(stack, x + BasePos.left_half_1.getA().getA(), y + BasePos.left_half_1.getA().getB(), UV.left_half_frame.x, UV.left_half_frame.y, UV.left_half_frame.w, UV.left_half_frame.h);
            mc.ingameGUI.blit(stack, x + IconPos.left_half_1.getA().getA(), y + IconPos.left_half_1.getA().getB(), UV.icon_defence_normal.x, UV.icon_defence_normal.y, UV.icon_defence_normal.w, UV.icon_defence_normal.h);
        } else {
            mc.ingameGUI.blit(stack, x + BasePos.left_half_1.getA().getA(), y + BasePos.left_half_1.getA().getB(), UV.left_half_frame.x, UV.left_half_frame.y, UV.left_half_frame.w, UV.left_half_frame.h);
            mc.ingameGUI.blit(stack, x + IconPos.left_half_1.getA().getA(), y + IconPos.left_half_1.getA().getB(), UV.icon_defence_normal.x, UV.icon_defence_normal.y, UV.icon_defence_normal.w, UV.icon_defence_normal.h);
        }
        mc.getProfiler().endSection();
    }

    public static void renderMountHealth(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_mounthealth");
        mc.ingameGUI.blit(stack, x + BasePos.right_threequarters.getA(), y + BasePos.right_threequarters.getB(), UV.right_threequarters_frame.x, UV.right_threequarters_frame.y, UV.right_threequarters_frame.w, UV.right_threequarters_frame.h);
        mc.ingameGUI.blit(stack, x + IconPos.right_threequarters.getA(), y + IconPos.right_threequarters.getB(), UV.icon_horse_normal.x, UV.icon_horse_normal.y, UV.icon_horse_normal.w, UV.icon_horse_normal.h);
        mc.getProfiler().endSection();
    }

    public static void renderJumpbar(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        mc.getProfiler().startSection("frostedheart_jumpbar");

        mc.getProfiler().endSection();
    }

    public static void renderFrozenOverlay(MatrixStack stack, int x, int y, Minecraft mc, ClientPlayerEntity player) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableAlphaTest();
        mc.getTextureManager().bindTexture(FROZEN_OVERLAY_PATH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.0D, (double)y, -90.0D).tex(0.0F, 1.0F).endVertex();
        bufferbuilder.pos((double)x*2, (double)y, -90.0D).tex(1.0F, 1.0F).endVertex();
        bufferbuilder.pos((double)x*2, 0.0D, -90.0D).tex(1.0F, 0.0F).endVertex();
        bufferbuilder.pos(0.0D, 0.0D, -90.0D).tex(0.0F, 0.0F).endVertex();
        tessellator.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static class BasePos {
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

    private static class BarPos {
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

    private static class IconPos {
        public static final Tuple<Integer, Integer> left_threequarters = new Tuple<>(-47, -56);
        public static final Tuple<Integer, Integer> right_threequarters = new Tuple<>(35, -56);
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_1 = new Tuple<>(new Tuple<>(-71, -54), new Tuple<>(-33, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_2 = new Tuple<>(new Tuple<>(-91, -54), new Tuple<>(-53, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> left_half_3 = new Tuple<>(new Tuple<>(-111, -54), new Tuple<>(-73, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_1 = new Tuple<>(new Tuple<>(59, -54), new Tuple<>(21, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_2 = new Tuple<>(new Tuple<>(79, -54), new Tuple<>(41, -54));
        public static final Tuple<Tuple<Integer, Integer>, Tuple<Integer, Integer>> right_half_3 = new Tuple<>(new Tuple<>(99, -54), new Tuple<>(61, -54));
    }

    private static class UV {
        public static final UV4i hotbar_slot = new UV4i(1, 1, 20, 20);
        public static final UV4i off_hand_slot = new UV4i(22, 1, 22, 22);
        public static final UV4i exp_bar_frame = new UV4i(45, 1, 184, 7);
        public static final UV4i temperature_orb_frame = new UV4i(1, 24, 43, 43);
        public static final UV4i left_threequarters_frame = new UV4i(45, 9,36, 38);
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
