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

package com.teammoeg.frostedheart.events;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.hud.FrostedHud;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiClickedEvent;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.*;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.recipes.InspireRecipe;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.content.temperature.heatervest.HeaterVestRenderer;
import com.teammoeg.frostedheart.data.BlockTempData;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.research.effects.EffectShowCategory;
import com.teammoeg.frostedheart.research.events.ClientResearchStatusEvent;
import com.teammoeg.frostedheart.research.gui.FHGuiHelper;
import com.teammoeg.frostedheart.research.gui.tech.ResearchToast;
import com.teammoeg.frostedheart.util.FHVersion;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.lwjgl.system.CallbackI;

import java.util.List;

import static net.minecraft.util.text.TextFormatting.GRAY;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
	@SubscribeEvent
	public static void tickClient(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			PlayerEntity pe = ClientUtils.getPlayer();
			if (pe != null && pe.getActivePotionEffect(FHEffects.NYCTALOPIA) != null) {
				ClientUtils.applyspg = true;
				ClientUtils.spgamma = MathHelper.clamp((float) (ClientUtils.mc().gameSettings.gamma), 0f, 1f) * 0.1f
						- 1f;
			} else {
				ClientUtils.applyspg = false;
			}
		}
	}

	@SubscribeEvent
	public static void tickClient(Unload event) {
		ClientUtils.applyspg = false;
	}

	@SuppressWarnings({ "unchecked", "resource" })
	@SubscribeEvent
	public static void drawUpdateReminder(GuiScreenEvent.DrawScreenEvent.Post event) {
		Screen gui = event.getGui();
		if (gui instanceof MainMenuScreen) {
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
				MatrixStack matrixStack = event.getMatrixStack();
				FHVersion clientVersion = FHMain.local.fetchVersion().orElse(FHVersion.empty);
				FontRenderer font = gui.getMinecraft().fontRenderer;
				if (!stableVersion.isEmpty() && (clientVersion.isEmpty() || !clientVersion.laterThan(stableVersion))) {
					List<IReorderingProcessor> list = font.trimStringToWidth(GuiUtils.translateGui("update_recommended")
							.appendString(stableVersion.getOriginal()).mergeStyle(TextFormatting.BOLD), 70);
					int l = 0;
					for (IReorderingProcessor line : list) {
						FHGuiHelper.drawLine(matrixStack, Color4I.rgba(0, 0, 0, 255), 0, gui.height / 2 - 1 + l, 72,
								gui.height / 2 + 9 + l);
						font.drawTextWithShadow(matrixStack, line, 1, gui.height / 2.0F + l, 0xFFFFFF);

						l += 9;
					}
					if (isStable) {
						IFormattableTextComponent itxc = new StringTextComponent("CurseForge")
								.mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
								.mergeStyle(TextFormatting.GOLD);
						boolean needEvents = true;
						for (IGuiEventListener x : gui.getEventListeners())
							if (x instanceof GuiClickedEvent) {
								needEvents = false;
								break;
							}
						font.drawTextWithShadow(matrixStack, itxc, 1, gui.height / 2.0F + l, 0xFFFFFF);
						Style opencf = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
								"https://www.curseforge.com/minecraft/modpacks/the-winter-rescue"));
						// Though the capture is ? extends IGuiEventListener, I can't add new to it
						// unless I cast it to List
						if (needEvents)
							((List<IGuiEventListener>) gui.getEventListeners()).add(new GuiClickedEvent(1,
									(int) (gui.height / 2.0F + l), font.getStringPropertyWidth(itxc) + 1,
									(int) (gui.height / 2.0F + l + 9), () -> gui.handleComponentClicked(opencf)));
						if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
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
						}
					}
				}
			});
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
				event.getPlayer().sendStatusMessage(GuiUtils.translateGui("update_recommended")
						.appendString(stableVersion.getOriginal()).mergeStyle(TextFormatting.BOLD), false);
				if (isStable) {
					event.getPlayer()
							.sendStatusMessage(new StringTextComponent("CurseForge")
									.setStyle(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
											"https://www.curseforge.com/minecraft/modpacks/the-winter-rescue")))
									.mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
									.mergeStyle(TextFormatting.GOLD), false);

					if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode()
							.equalsIgnoreCase("zh_cn")) {
						event.getPlayer()
								.sendStatusMessage(new StringTextComponent("MCBBS")
										.setStyle(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
												"https://www.mcbbs.net/thread-1227167-1-1.html")))
										.mergeStyle(TextFormatting.UNDERLINE).mergeStyle(TextFormatting.BOLD)
										.mergeStyle(TextFormatting.DARK_RED), false);
					}
				}
			}
		});
		if (ServerLifecycleHooks.getCurrentServer() != null)
			if (FHMain.saveNeedUpdate) {
				event.getPlayer().sendStatusMessage(
						GuiUtils.translateGui("save_update_needed", FHMain.lastServerConfig.getAbsolutePath())
								.mergeStyle(TextFormatting.RED),
						false);
			} else if (FHMain.lastbkf != null) {
				event.getPlayer().sendStatusMessage(GuiUtils.translateGui("save_updated")
						.appendSibling(new StringTextComponent(FHMain.lastbkf.getName()).setStyle(Style.EMPTY
								.setClickEvent(
										new ClickEvent(ClickEvent.Action.OPEN_FILE, FHMain.lastbkf.getAbsolutePath()))
								.applyFormatting(TextFormatting.UNDERLINE))),
						false);
			}
	}

	/**
	 * @param event
	 */
	@SuppressWarnings({ "resource", "unchecked", "rawtypes" })
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (!HeaterVestRenderer.rendersAssigned) {
			for (Object render : ClientUtils.mc().getRenderManager().renderers.values())
				if (BipedRenderer.class.isAssignableFrom(render.getClass()))
					((BipedRenderer) render).addLayer(new HeaterVestRenderer<>((BipedRenderer) render));
				else if (ArmorStandRenderer.class.isAssignableFrom(render.getClass()))
					((ArmorStandRenderer) render).addLayer(new HeaterVestRenderer<>((ArmorStandRenderer) render));
			HeaterVestRenderer.rendersAssigned = true;
		}

	}

	@SubscribeEvent
	public void onWorldUnLoad(Unload event) {

	}

	@SubscribeEvent
	public static void onResearchStatus(ClientResearchStatusEvent event) {
		if(event.isCompletion())
			ClientUtils.mc().getToastGui().add(new ResearchToast(event.getResearch()));
		for (Effect e : event.getResearch().getEffects())
			if (e instanceof EffectCrafting || e instanceof EffectShowCategory) {
				JEICompat.syncJEI();
				return;
			}
	}

	@SubscribeEvent
	public static void addNormalItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		Item i = stack.getItem();
		if (i == Items.FLINT) {
			event.getToolTip().add(GuiUtils.translateTooltip("double_flint_ignition").mergeStyle(GRAY));
		}
	}

	@SubscribeEvent
	public static void addItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		Item i = stack.getItem();
		ITempAdjustFood itf = null;
		IWarmKeepingEquipment iwe = null;
		for(InspireRecipe ir:InspireRecipe.recipes) {
			if(ir.item.test(stack)) {
				event.getToolTip().add(GuiUtils.translateTooltip("inspire_item").mergeStyle(TextFormatting.GRAY));
				break;
			}
		}
		float tspeed = (float) (double) FHConfig.SERVER.tempSpeed.get();
		if (i instanceof ITempAdjustFood) {
			itf = (ITempAdjustFood) i;
		} else {
			itf = FHDataManager.getFood(stack);
		}
		if (i instanceof IWarmKeepingEquipment) {
			iwe = (IWarmKeepingEquipment) i;
		} else {
			String s = ItemNBTHelper.getString(stack, "inner_cover");
			EquipmentSlotType aes = MobEntity.getSlotForItemStack(stack);
			if (s.length() > 0 && aes != null) {
				event.getToolTip().add(GuiUtils.translateTooltip("inner").mergeStyle(TextFormatting.GREEN)
						.appendSibling(new TranslationTextComponent("item." + s.replaceFirst(":", "."))));
				if (!ItemNBTHelper.getBoolean(stack, "inner_bounded")) {
					if (stack.hasTag() && stack.getTag().contains("inner_cover_tag")) {
						CompoundNBT cn = stack.getTag().getCompound("inner_cover_tag");
						int damage = cn.getInt("Damage");
						if (damage != 0) {
							RecipeInner ri = RecipeInner.recipeList.get(new ResourceLocation(s));
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
		}
		BlockTempData btd = FHDataManager.getBlockData(stack);
		if (btd != null) {
			float temp = btd.getTemp();
			temp = (Math.round(temp * 100)) / 100.0F;// round
			String temps = Float.toString(temp);
			if (temp != 0)
				if (temp > 0)
					event.getToolTip()
							.add(GuiUtils.translateTooltip("block_temp", "+" + temps).mergeStyle(TextFormatting.GOLD));
				else
					event.getToolTip()
							.add(GuiUtils.translateTooltip("block_temp", temps).mergeStyle(TextFormatting.AQUA));
		}
		if (itf != null) {
			float temp = itf.getHeat(stack) * tspeed;
			temp = (Math.round(temp * 1000)) / 1000.0F;// round
			String temps = Float.toString(temp);
			if (temp != 0)
				if (temp > 0)
					event.getToolTip()
							.add(GuiUtils.translateTooltip("food_temp", "+" + temps).mergeStyle(TextFormatting.GOLD));
				else
					event.getToolTip()
							.add(GuiUtils.translateTooltip("food_temp", temps).mergeStyle(TextFormatting.AQUA));
		}
		if (iwe != null) {
			float temp = iwe.getFactor(null, stack);
			temp = Math.round(temp * 100);
			String temps = Float.toString(temp);
			if (temp != 0)
				if (temp > 0)
					event.getToolTip()
							.add(GuiUtils.translateTooltip("armor_warm", temps).mergeStyle(TextFormatting.GOLD));
				else
					event.getToolTip()
							.add(GuiUtils.translateTooltip("armor_warm", temps).mergeStyle(TextFormatting.AQUA));
		}
		if (i instanceof IHeatingEquipment) {
			float temp = ((IHeatingEquipment) i).getMax(stack) * tspeed;
			temp = (Math.round(temp * 2000)) / 1000.0F;
			String temps = Float.toString(temp);
			if (temp != 0)
				if (temp > 0)
					event.getToolTip().add(
							GuiUtils.translateTooltip("armor_heating", "+" + temps).mergeStyle(TextFormatting.GOLD));
				else
					event.getToolTip()
							.add(GuiUtils.translateTooltip("armor_heating", temps).mergeStyle(TextFormatting.AQUA));
		}
	}

	@SubscribeEvent
	public static void onPostRenderOverlay(RenderGameOverlayEvent.Post event) {
		PlayerEntity player = FrostedHud.getRenderViewPlayer();
		Minecraft mc = Minecraft.getInstance();
		MatrixStack stack = event.getMatrixStack();
		int anchorX = event.getWindow().getScaledWidth() / 2;
		int anchorY = event.getWindow().getScaledHeight();
		if (event.getType() == RenderGameOverlayEvent.ElementType.VIGNETTE && player != null) {
			
			if (!player.isCreative() && !player.isSpectator()) {
				if (TemperatureCore.getBodyTemperature(player) <= -0.5) {
					FrostedHud.renderFrozenVignette(stack, anchorX, anchorY, mc, player);
				} else if (TemperatureCore.getBodyTemperature(player) >= 0.5) {
					FrostedHud.renderHeatVignette(stack, anchorX, anchorY, mc, player);
				}
				if (TemperatureCore.getBodyTemperature(player) <= -1.0) {
					FrostedHud.renderFrozenOverlay(stack, anchorX, anchorY, mc, player);
				}
			}
//			if (FrostedHud.renderForecast)
//				FrostedHud.renderForecast(stack, anchorX, anchorY, mc, player);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
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
		float partialTicks = event.getPartialTicks();

		FrostedHud.renderSetup(clientPlayer, renderViewPlayer);
		if (FHConfig.CLIENT.enableUI.get()) {
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
				if (FrostedHud.renderFood)
					FrostedHud.renderFood(stack, anchorX, anchorY, mc, renderViewPlayer);
				if (FrostedHud.renderThirst)
					FrostedHud.renderThirst(stack, anchorX, anchorY, mc, renderViewPlayer);
				if (FrostedHud.renderHealth)
					FrostedHud.renderTemperature(stack, anchorX, anchorY, mc, renderViewPlayer);
				if (FrostedHud.renderForecast)
					FrostedHud.renderForecast(stack, anchorX, anchorY, mc, renderViewPlayer);
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
	public static void addWeatherItemTooltips(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (stack.getItem() == FHItems.temperatureProbe) {
			event.getToolTip().add(GuiUtils.translateTooltip("temperature_probe").mergeStyle(TextFormatting.GRAY));
		}
		if (stack.getItem() == FHItems.weatherRadar) {
			event.getToolTip().add(GuiUtils.translateTooltip("weather_radar").mergeStyle(TextFormatting.GRAY));
		}
		if (stack.getItem() == FHItems.weatherHelmet) {
			event.getToolTip().add(GuiUtils.translateTooltip("weather_helmet").mergeStyle(TextFormatting.GRAY));
		}
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
}
