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

package com.teammoeg.frostedheart.events;

import java.util.List;
import java.util.Map;

import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.UnlockedTipManager;
import com.teammoeg.frostedheart.content.tips.client.util.TipDisplayUtil;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraftforge.client.event.*;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHClientTeamDataManager;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHDataManager;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.client.hud.FrostedHud;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.recipes.InspireRecipe;
import com.teammoeg.frostedheart.content.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.gui.ResearchToast;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.content.research.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.content.research.research.effects.EffectShowCategory;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.dialog.HUDDialog;
import com.teammoeg.frostedheart.content.scenario.network.ClientLinkClickedPacket;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestRenderer;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHGuiHelper;
import com.teammoeg.frostedheart.util.client.GuiClickedEvent;
import com.teammoeg.frostedheart.util.version.FHVersion;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void addItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item i = stack.getItem();
        ITempAdjustFood itf = null;
        //IWarmKeepingEquipment iwe = null;
        for (InspireRecipe ir : FHUtils.filterRecipes(null, InspireRecipe.TYPE)) {
            if (ir.item.test(stack)) {
                event.getToolTip().add(TranslateUtils.translateTooltip("inspire_item").withStyle(ChatFormatting.GRAY));
                break;
            }
        }
        float tspeed = (float) (double) FHConfig.SERVER.tempSpeed.get();
        if (i instanceof ITempAdjustFood) {
            itf = (ITempAdjustFood) i;
        } else {
            itf = FHDataManager.getFood(stack);
        }
       /* if (i instanceof IWarmKeepingEquipment) {
            iwe = (IWarmKeepingEquipment) i;
        } else {
            String s = ItemNBTHelper.getString(stack, "inner_cover");
            EquipmentSlotType aes = MobEntity.getSlotForItemStack(stack);
            if (s.length() > 0 && aes != null) {
                event.getToolTip().add(GuiUtils.translateTooltip("inner").mergeStyle(TextFormatting.GREEN)
                        .appendSibling(TranslateUtils.translate("item." + s.replaceFirst(":", "."))));
                if (!ItemNBTHelper.getBoolean(stack, "inner_bounded")) {
                    if (stack.hasTag() && stack.getTag().contains("inner_cover_tag")) {
                        CompoundNBT cn = stack.getTag().getCompound("inner_cover_tag");
                        int damage = cn.getInt("Damage");
                        if (damage != 0) {
                            InstallInnerRecipe ri = InstallInnerRecipe.recipeList.get(new ResourceLocation(s));
                            if (ri != null) {
                                int maxDmg = ri.getDurability();
                                float temp = damage * 1.0F / maxDmg;
                                String temps = Integer.toString((Math.round(temp * 100)));
                                event.getToolTip().add(GuiUtils.translateTooltip("inner_damage", temps));
                            }
                        }
                        if (cn.contains("Enchantments")) {
                            ListNBT ln = cn.getList("Enchantments", 10);
                            if (!ln.isEmpty()) {
                                event.getToolTip().add(
                                        GuiUtils.translateTooltip("inner_enchantment").mergeStyle(TextFormatting.GRAY));
                                ItemStack.addEnchantmentTooltips(event.getToolTip(), ln);
                            }
                        }

                    }
                }
                iwe = FHDataManager.getArmor(s + "_" + aes.getName());
            } else
                iwe = FHDataManager.getArmor(stack);
        }*/
        BlockTempData btd = FHDataManager.getBlockData(stack);
        if (btd != null) {
            float temp = btd.getTemp();
            temp = (Math.round(temp * 100)) / 100.0F;// round
            if (temp != 0)
                if (temp > 0)
                    event.getToolTip()
                            .add(TranslateUtils.translateTooltip("block_temp", TemperatureDisplayHelper.toTemperatureFloatString(temp)).withStyle(ChatFormatting.GOLD));
                else
                    event.getToolTip()
                            .add(TranslateUtils.translateTooltip("block_temp", TemperatureDisplayHelper.toTemperatureFloatString(temp)).withStyle(ChatFormatting.AQUA));
        }
        if (itf != null) {
            float temp = itf.getHeat(stack,
                    event.getEntity() == null ? 37 :PlayerTemperatureData.getCapability(event.getEntity()).map(PlayerTemperatureData::getEnvTemp).orElse(0f)) * tspeed;
            temp = (Math.round(temp * 1000)) / 1000.0F;// round
            if (temp != 0)
                if (temp > 0) 
                    event.getToolTip()
                            .add(TranslateUtils.translateTooltip("food_temp", "+" + TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.GOLD));
                else
                    event.getToolTip()
                            .add(TranslateUtils.translateTooltip("food_temp", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.AQUA));
        }
      /*  if (iwe != null) {
            float temp = iwe.getFactor(null, stack);
            temp = Math.round(temp * 100);
            String temps = Float.toString(temp);
            if (temp != 0)
                event.getToolTip().add(GuiUtils.translateTooltip("armor_warm", temps).mergeStyle(TextFormatting.GOLD));
        }*/
        if (i instanceof IHeatingEquipment) {
            float temp = ((IHeatingEquipment) i).getEffectiveTempAdded(null, stack,0, 0);
            temp = (Math.round(temp * 2000)) / 1000.0F;
            if (temp != 0)
                if (temp > 0)
                    event.getToolTip().add(
                            TranslateUtils.translateTooltip("armor_heating", "+" + TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.GOLD));
                else
                    event.getToolTip()
                            .add(TranslateUtils.translateTooltip("armor_heating", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.AQUA));
        }
        Map<String,Component> rstooltip=JEICompat.research.get(i);
        if(rstooltip!=null)
        	event.getToolTip().addAll(rstooltip.values());
    }

    @SubscribeEvent
    public static void addNormalItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item i = stack.getItem();
        if (i == Items.FLINT) {
            event.getToolTip().add(TranslateUtils.translateTooltip("double_flint_ignition").withStyle(ChatFormatting.GRAY));
        }
    }

    /*@SubscribeEvent
    public static void addWeatherItemTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == FHItems.temperatureProbe.get()) {
            event.getToolTip().add(TranslateUtils.translateTooltip("temperature_probe").mergeStyle(TextFormatting.GRAY));
        }
        if (stack.getItem() == FHItems.weatherRadar.get()) {
            event.getToolTip().add(TranslateUtils.translateTooltip("weather_radar").mergeStyle(TextFormatting.GRAY));
        }
        if (stack.getItem() == FHItems.weatherHelmet.get()) {
            event.getToolTip().add(TranslateUtils.translateTooltip("weather_helmet").mergeStyle(TextFormatting.GRAY));
        }
    }*/

    @SuppressWarnings({"unchecked"})
    @SubscribeEvent
    public static void drawUpdateReminder(GuiScreenEvent.DrawScreenEvent.Post event) {
        Screen gui = event.getGui();
        if (gui instanceof TitleScreen) {
            FHMain.remote.fetchVersion().ifPresent(stableVersion -> {
                boolean isStable = true;
                if (FHMain.pre != null && FHMain.pre.fetchVersion().isPresent()) {
                    FHVersion preversion = FHMain.pre.fetchVersion().resolve().get();
                    if (preversion.laterThan(stableVersion)) {
                        stableVersion = preversion;
                        isStable = false;
                    }
                }
                if (stableVersion.isEmpty())
                    return;
                PoseStack matrixStack = event.getMatrixStack();
                FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
                Font font = gui.getMinecraft().font;
                if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                    List<FormattedCharSequence> list = font.split(TranslateUtils.translateGui("update_recommended")
                            .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), 70);
                    int l = 0;
                    for (FormattedCharSequence line : list) {
                        FHGuiHelper.drawLine(matrixStack, Color4I.rgba(0, 0, 0, 255), 0, gui.height / 2 - 1 + l, 72,
                                gui.height / 2 + 9 + l);
                        font.drawShadow(matrixStack, line, 1, gui.height / 2.0F + l, 0xFFFFFF);

                        l += 9;
                    }
                    if (isStable) {
                        MutableComponent itxc = TranslateUtils.str("CurseForge")
                                .withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GOLD);
                        boolean needEvents = true;
                        for (GuiEventListener x : gui.children())
                            if (x instanceof GuiClickedEvent) {
                                needEvents = false;
                                break;
                            }
                        font.drawShadow(matrixStack, itxc, 1, gui.height / 2.0F + l, 0xFFFFFF);
                        Style opencf = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                "https://www.curseforge.com/minecraft/modpacks/the-winter-rescue"));
                        // Though the capture is ? extends IGuiEventListener, I can't add new to it
                        // unless I cast it to List
                        if (needEvents)
                            ((List<GuiEventListener>) gui.children()).add(new GuiClickedEvent(1,
                                    (int) (gui.height / 2.0F + l), font.width(itxc) + 1,
                                    (int) (gui.height / 2.0F + l + 9), () -> gui.handleComponentClicked(opencf)));
                        /*if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
                                .equalsIgnoreCase("zh_cn")) {
                            l += 9;
                            Style openmcbbs = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                    "https://www.mcbbs.net/thread-1227167-1-1.html"));
                            IFormattableTextComponent itxm = new StringTextComponent("MCBBS")
                                    .mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
                                    .mergeStyle(TextFormatting.DARK_RED);
                            if (needEvents)
                                ((List<IGuiEventListener>) gui.getEventListeners()).add(new GuiClickedEvent(1,
                                        (int) (gui.height / 2.0F + l), font.getStringPropertyWidth(itxm) + 1,
                                        (int) (gui.height / 2.0F + l + 9),
                                        () -> gui.handleComponentClicked(openmcbbs)));
                            font.drawTextWithShadow(matrixStack, itxm, 1, gui.height / 2.0F + l, 0xFFFFFF);
                        }*/
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onResearchStatus(ClientResearchStatusEvent event) {
        if (event.isStatusChanged()) {
            if (event.isCompletion())
                ClientUtils.mc().getToasts().addToast(new ResearchToast(event.getResearch()));
        } else if (!event.isCompletion())
            return;
        for (Effect e : event.getResearch().getEffects())
            if (e instanceof EffectCrafting || e instanceof EffectShowCategory) {
                JEICompat.syncJEI();
                return;
            }
    }

    @SubscribeEvent
    public static void fireLogin(LoggedInEvent event) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FHClientTeamDataManager.INSTANCE::reset);
        ClientScene.INSTANCE=new ClientScene();
    	ClientScene.INSTANCE.sendClientReady();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderCustomHUD(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer clientPlayer = mc.player;
        Player renderViewPlayer = FrostedHud.getRenderViewPlayer();

        if (renderViewPlayer == null || clientPlayer == null || mc.options.hideGui) {
            return;
        }

        PoseStack stack = event.getMatrixStack();
        int anchorX = event.getWindow().getGuiScaledWidth() / 2;
        int anchorY = event.getWindow().getGuiScaledHeight();
        float partialTicks = event.getPartialTicks();

        FrostedHud.renderSetup(clientPlayer, renderViewPlayer);
        if (FHConfig.CLIENT.enableUI.get()) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                if (FrostedHud.renderFrozenOverlay)
                    FrostedHud.renderFrozenOverlay(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderFrozenVignette)
                    FrostedHud.renderFrozenVignette(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderHeatVignette)
                    FrostedHud.renderHeatVignette(stack, anchorX, anchorY, mc, renderViewPlayer);


            }
            if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && FrostedHud.renderHotbar) {
                if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
                    mc.gui.getSpectatorGui().renderHotbar(stack, partialTicks);
                } else {
                    if (FrostedHud.renderForecast)
                        FrostedHud.renderForecast(stack, anchorX, anchorY, mc, renderViewPlayer);
                    FrostedHud.renderHotbar(stack, anchorX, anchorY, mc, renderViewPlayer, partialTicks);
                    FrostedHud.renderScenarioAct(stack, anchorX, anchorY, mc, renderViewPlayer);
                }
                if(ClientScene.INSTANCE.dialog instanceof HUDDialog) {
                	((HUDDialog)ClientScene.INSTANCE.dialog).render(stack, 0, 0, partialTicks);
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
                if (FrostedHud.renderFood)
                    FrostedHud.renderFood(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderThirst)
                    FrostedHud.renderThirst(stack, anchorX, anchorY, mc, renderViewPlayer);
                if (FrostedHud.renderHealth)
                    FrostedHud.renderTemperature(stack, anchorX, anchorY, mc, renderViewPlayer);
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
        }
        // add compatibility to other MOD UIs, may cause problem?
        if (event.isCanceled()) {
            if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD)
                MinecraftForge.EVENT_BUS
                        .post(new RenderGameOverlayEvent.Post(event.getMatrixStack(), event, event.getType()));// compatibility
        }
    }

    @SubscribeEvent
    public static void sendLoginUpdateReminder(PlayerEvent.PlayerLoggedInEvent event) {
    	
        FHMain.remote.fetchVersion().ifPresent(stableVersion -> {
            boolean isStable = true;
            if (FHMain.pre != null && FHMain.pre.fetchVersion().isPresent()) {
                FHVersion preversion = FHMain.pre.fetchVersion().resolve().get();
                if (preversion.laterThan(stableVersion)) {
                    stableVersion = preversion;
                    isStable = false;
                }
            }
            if (stableVersion.isEmpty())
                return;
            FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
            if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
                event.getPlayer().displayClientMessage(TranslateUtils.translateGui("update_recommended")
                        .append(stableVersion.getOriginal()).withStyle(ChatFormatting.BOLD), false);
                if (isStable) {
                    event.getPlayer()
                            .displayClientMessage(TranslateUtils.str("CurseForge")
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                            "https://www.curseforge.com/minecraft/modpacks/the-winter-rescue")))
                                    .withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.BOLD)
                                    .withStyle(ChatFormatting.GOLD), false);

                    /*if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
                            .equalsIgnoreCase("zh_cn")) {
                        event.getPlayer()
                                .sendStatusMessage(new StringTextComponent("MCBBS")
                                        .setStyle(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                "https://www.mcbbs.net/thread-1227167-1-1.html")))
                                        .mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
                                        .mergeStyle(TextFormatting.DARK_RED), false);
                    }*/
                }
            }
        });
        if (ServerLifecycleHooks.getCurrentServer() != null)
            if (FHMain.saveNeedUpdate) {
                event.getPlayer().displayClientMessage(
                        TranslateUtils.translateGui("save_update_needed", FHMain.lastServerConfig.getAbsolutePath())
                                .withStyle(ChatFormatting.RED),
                        false);
            } else if (FHMain.lastbkf != null) {
                event.getPlayer().displayClientMessage(TranslateUtils.translateGui("save_updated")
                                .append(TranslateUtils.str(FHMain.lastbkf.getName()).setStyle(Style.EMPTY
                                        .withClickEvent(
                                                new ClickEvent(ClickEvent.Action.OPEN_FILE, FHMain.lastbkf.getAbsolutePath()))
                                        .applyFormat(ChatFormatting.UNDERLINE))),
                        false);
            }

    }
    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {

       // event.addListener(KGlyphProvider.INSTANCE);

    }
    @SuppressWarnings("resource")
	@SubscribeEvent
    public static void tickClient(ClientTickEvent event) {

        if (event.phase == Phase.START) {
            if (ClientUtils.mc().level != null) {
                Minecraft mc = ClientUtils.mc();
                if(ClientScene.INSTANCE!=null)
                	ClientScene.INSTANCE.tick(mc);

            }
            Player pe = ClientUtils.getPlayer();
            PlayerTemperatureData.getCapability(pe).ifPresent(t->{
            	t.smoothedBodyPrev=t.smoothedBody;
            	t.smoothedBody=t.smoothedBody*.9f+t.getBodyTemp()*.1f;
            });
            
            if (pe != null && pe.getEffect(FHEffects.NYCTALOPIA.get()) != null) {
                ClientUtils.applyspg = true;
                ClientUtils.spgamma = Mth.clamp((float) (ClientUtils.mc().options.gamma), 0f, 1f) * 0.1f
                        - 1f;
            } else {
                ClientUtils.applyspg = false;
                ClientUtils.spgamma = Mth.clamp((float) ClientUtils.mc().options.gamma, 0f, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void unloadWorld(Unload event) {
        ClientUtils.applyspg = false;
    }

    @SuppressWarnings({"resource", "unchecked", "rawtypes"})
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!HeaterVestRenderer.rendersAssigned) {
            for (Object render : ClientUtils.mc().getEntityRenderDispatcher().renderers.values())
                if (HumanoidMobRenderer.class.isAssignableFrom(render.getClass()))
                    ((HumanoidMobRenderer) render).addLayer(new HeaterVestRenderer<>((HumanoidMobRenderer) render));
                else if (ArmorStandRenderer.class.isAssignableFrom(render.getClass()))
                    ((ArmorStandRenderer) render).addLayer(new HeaterVestRenderer<>((ArmorStandRenderer) render));
            HeaterVestRenderer.rendersAssigned = true;
        }
    }    @SuppressWarnings({"resource", "unchecked", "rawtypes"})

    
    @SubscribeEvent
    public void onWorldUnLoad(Unload event) {
    	
    }
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
    	if(event.getOriginalMessage().startsWith("fh$scenario$link:")) {
    		ClientLinkClickedPacket packet=new ClientLinkClickedPacket(event.getOriginalMessage().substring("fh$scenario$link:".length()));
    		FHNetwork.sendToServer(packet);
    		event.setCanceled(true);
    	}
    }
    @SubscribeEvent
    public static void onClientKey(KeyInputEvent event) {
    	if(event.getAction()==GLFW.GLFW_PRESS&&ClientRegistryEvents.key_skipDialog.consumeClick()) {
    		if(ClientScene.INSTANCE!=null)
    			ClientScene.INSTANCE.sendContinuePacket(true);
    		//event.setCanceled(true);
    	}
    }

    /**
     * Add our custom render player
     *
     * @param event fired before player is rendered
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
//        if (event.getPlayer() != null && event.getPlayer().world.isRemote()) {
//            PlayerRenderer renderer = event.getRenderer();
//            // add our custom render layer
//            renderer.addLayer(new FrostbiteRenderer<>(renderer));
//        }
    }

    /*
     * @SubscribeEvent
     * public static void addFutureTempToDebug(RenderGameOverlayEvent.Text event) {
     * Minecraft mc = Minecraft.getInstance();
     * List<String> list = event.getRight();
     * if (mc.gameSettings.showDebugInfo && mc.world != null && mc.player != null) {
     * float currentHourTemp = ClimateData.getTemp(mc.world);
     * float hour1Temp = ClimateData.getFutureTemp(mc.world, 1);
     * float hour2Temp = ClimateData.getFutureTemp(mc.world, 2);
     * float hour3Temp = ClimateData.getFutureTemp(mc.world, 3);
     * float hour4Temp = ClimateData.getFutureTemp(mc.world, 4);
     * float hour5Temp = ClimateData.getFutureTemp(mc.world, 5);
     * float hour6Temp = ClimateData.getFutureTemp(mc.world, 6);
     * float hour7Temp = ClimateData.getFutureTemp(mc.world, 7);
     * float day1Temp = ClimateData.getFutureTemp(mc.world, 1, 0);
     * float day2Temp = ClimateData.getFutureTemp(mc.world, 2, 0);
     * float day3Temp = ClimateData.getFutureTemp(mc.world, 3, 0);
     * float day4Temp = ClimateData.getFutureTemp(mc.world, 4, 0);
     * float day5Temp = ClimateData.getFutureTemp(mc.world, 5, 0);
     * float day6Temp = ClimateData.getFutureTemp(mc.world, 6, 0);
     * float day7Temp = ClimateData.getFutureTemp(mc.world, 7, 0);
     * list.add("TWR Climate Temperature:");
     * list.add(String.format("This Hour: %.1f", currentHourTemp));
     * list.add(String.
     * format("Next 7 Hours: %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f",
     * hour1Temp, hour2Temp, hour3Temp, hour4Temp, hour5Temp, hour6Temp,
     * hour7Temp));
     * list.add(String.
     * format("Next 7 Days: %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %.1f",
     * day1Temp, day2Temp, day3Temp, day4Temp, day5Temp, day6Temp, day7Temp));
     * }
     * }
     */

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        TipDisplayUtil.clearRenderQueue();
        TipDisplayUtil.displayTip("_default", false);
        TipDisplayUtil.displayTip("_default2", false);
        if (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC) == 0) {
            TipDisplayUtil.displayTip("_music_warning", false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TipDisplayUtil.clearRenderQueue();
    }

    @SubscribeEvent
    public static void onGUIOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof TitleScreen) {
            if (!UnlockedTipManager.error.isEmpty()) {
                TipElement ele = new TipElement();
                ele.replaceToError(UnlockedTipManager.UNLOCKED_FILE, UnlockedTipManager.error);
                TipDisplayUtil.displayTip(ele, true);
                UnlockedTipManager.error = "";
            }

//            if (...如果有新版本) {
//              TipHandler.displayTip("update", false);
//            }
        }
    }

    @SubscribeEvent
    public static void onGUIRender(GuiScreenEvent event) {
        if (event.getGui() instanceof SoundOptionsScreen) {
            if (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC) <= 0) {
                TipDisplayUtil.displayTip("_music_warning", false);
            }
        }
    }
}
