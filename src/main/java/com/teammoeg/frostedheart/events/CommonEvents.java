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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.command.*;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatContainer;
import com.teammoeg.frostedheart.content.town.TeamTownDataS2CPacket;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;
import com.teammoeg.frostedheart.content.utility.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.utility.oredetect.ProspectorPick;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.world.FHGeneration;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.MultiblockFormEvent;
import com.teammoeg.frostedheart.world.FHStructures;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.ServerResources;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.world.MobSpawnInfoBuilder;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import se.mickelus.tetra.items.modular.IModularItem;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    static ResourceLocation ft = new ResourceLocation("storagedrawers:drawers");

    private static final Set<EntityType<?>> VANILLA_ENTITIES = Sets.newHashSet(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN);
    @SubscribeEvent
    public static void checkSleep(SleepingTimeCheckEvent event) {
        if (event.getPlayer().getSleepTimer() >= 100 && !event.getPlayer().getCommandSenderWorld().isClientSide) {
            EnergyCore.applySleep((ServerPlayer) event.getPlayer());
        }
    }
    @SubscribeEvent
    public static void tickEnergy(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START
                && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            if (!player.isSpectator() && !player.isCreative() && player.tickCount % 20 == 0)
                EnergyCore.dT(player);
        }
    }
    @SubscribeEvent
    public static void addManualToPlayer(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
        CompoundTag nbt = event.getPlayer().getPersistentData();
        CompoundTag persistent;

        if (nbt.contains(Player.PERSISTED_NBT_TAG)) {
            persistent = nbt.getCompound(Player.PERSISTED_NBT_TAG);
        } else {
            nbt.put(Player.PERSISTED_NBT_TAG, (persistent = new CompoundTag()));
        }
        if (!persistent.contains(FHUtils.FIRST_LOGIN_GIVE_MANUAL)) {
            persistent.putBoolean(FHUtils.FIRST_LOGIN_GIVE_MANUAL, false);
            event.getPlayer().inventory.add(
                    new ItemStack(RegistryUtils.getItem(new ResourceLocation("ftbquests", "book"))));
            event.getPlayer().inventory.armor.set(3, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_HELMET)
                    .setHoverName(TranslateUtils.translate("itemname.frostedheart.start_head"))));
            event.getPlayer().inventory.armor.set(2, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_CHESTPLATE)
                    .setHoverName(TranslateUtils.translate("itemname.frostedheart.start_chest"))));
            event.getPlayer().inventory.armor.set(1, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_LEGGINGS)
                    .setHoverName(TranslateUtils.translate("itemname.frostedheart.start_leg"))));
            event.getPlayer().inventory.armor.set(0, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_BOOTS)
                    .setHoverName(TranslateUtils.translate("itemname.frostedheart.start_foot"))));
            if (event.getPlayer().abilities.instabuild) {
                event.getPlayer().sendMessage(TranslateUtils.translate("message.frostedheart.creative_help")
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TranslateUtils.str("Click to use command")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/frostedheart research complete all"))), event.getPlayer().getUUID());
            }

            event.getPlayer().sendMessage(TranslateUtils.translate("message.frostedheart.temperature_help"), event.getPlayer().getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void addOreGenFeatures(BiomeLoadingEvent event) {
        if (event.getName() != null) {
            Biome.BiomeCategory category = event.getCategory();
            if (category != Biome.BiomeCategory.NETHER && category != Biome.BiomeCategory.THEEND) {
                FHGeneration.generate_overworld_ores(event);
            }
            //else if(category == NETHER) { generate_nether_ores(event);

            //Structures
            FHGeneration.generate_overworld_structures(event);
        }
    }

    @SubscribeEvent
    public static void addDimensionalSpacing(final WorldEvent.Load event){
        if(event.getWorld() instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) event.getWorld();


            Map<StructureFeature<?>, StructureFeatureConfiguration> tempMap =
                    new HashMap<>(serverWorld.getChunkSource().generator.getSettings().structureConfig());
            tempMap.putIfAbsent(FHStructures.DESTROYED_GENERATOR.get(),
                    StructureSettings.DEFAULTS.get(FHStructures.DESTROYED_GENERATOR.get()));
            serverWorld.getChunkSource().generator.getSettings().structureConfig = tempMap;
        }
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        ServerResources dataPackRegistries = event.getDataPackRegistries();
        // IReloadableResourceManager resourceManager = (IReloadableResourceManager)
        // dataPackRegistries.getResourceManager();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
//            resourceManager.addReloadListener(ChunkCacheInvalidationReloaderListener.INSTANCE);
    }

    @SubscribeEvent
    public static void addReloadListenersLowest(AddReloadListenerEvent event) {
        ServerResources dataPackRegistries = event.getDataPackRegistries();
        event.addListener(new FHRecipeCachingReloadListener(dataPackRegistries));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void biomeLoadingEventRemove(@Nonnull BiomeLoadingEvent event) {
        MobSpawnInfoBuilder spawns = event.getSpawns();

        for (MobCategory en : MobCategory.values()) {
            spawns.getSpawner(en).removeIf(entry -> VANILLA_ENTITIES.contains(entry.type));
            
        }

    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doPlayerInteract(PlayerInteractEvent ite) {
    	if(ite.getPlayer() instanceof ServerPlayer&&!(ite.getPlayer() instanceof FakePlayer)) {
    		FHScenario.trigVar(ite.getPlayer(), EventTriggerType.PLAYER_INTERACT);
    	}
    }
    @SubscribeEvent
    public static void canUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof IModularItem) {
            Set<ToolType> tt = event.getItemStack().getToolTypes();
            int type = 0;
            if (tt.contains(TetraCompat.coreSpade))
                type = 1;
            else if (tt.contains(TetraCompat.geoHammer))
                type = 2;
            else if (tt.contains(TetraCompat.proPick))
                type = 3;
            if (type != 0)
                if (!event.getPlayer().getCooldowns().isOnCooldown(event.getItemStack().getItem())) {
                    event.getPlayer().getCooldowns().addCooldown(event.getItemStack().getItem(), 10);
                    if ((type == 3 && event.getWorld().getRandom().nextBoolean()) || (type != 3 && event.getWorld().getRandom().nextBoolean()))
                        ((IModularItem) event.getItemStack().getItem()).tickProgression(event.getPlayer(), event.getItemStack(), 1);
                    switch (type) {
                        case 1:
                            CoreSpade.doProspect(event.getPlayer(), event.getWorld(), event.getPos(), event.getItemStack(), event.getHand());
                            break;
                        case 2:
                            GeologistsHammer.doProspect(event.getPlayer(), event.getWorld(), event.getPos(), event.getItemStack(), event.getHand());
                            break;
                        case 3:
                            ProspectorPick.doProspect(event.getPlayer(), event.getWorld(), event.getPos(), event.getItemStack(), event.getHand());
                            break;
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
        }
        if (!ResearchListeners.canUseBlock(event.getPlayer(), event.getWorld().getBlockState(event.getHitVec().getBlockPos()).getBlock())) {
            event.setUseBlock(Result.DENY);

            event.getPlayer().displayClientMessage(TranslateUtils.translateMessage("research.cannot_use_block"), true);
        }

    }

    @SubscribeEvent
    public static void death(PlayerEvent.Clone ev) {
        FHUtils.clonePlayerCapability(FHCapabilities.WANTED_FOOD.capability(),ev.getOriginal(),ev.getPlayer());
        FHUtils.clonePlayerCapability(FHCapabilities.ENERGY,ev.getOriginal(),ev.getPlayer());
        FHUtils.clonePlayerCapability(FHCapabilities.SCENARIO,ev.getOriginal(),ev.getPlayer());
        //FHUtils.clonePlayerCapability(PlayerTemperatureData.CAPABILITY,ev.getOriginal(),ev.getPlayer());
        //FHMain.LOGGER.info("clone");
        if (!ev.getPlayer().level.isClientSide) {
            DeathInventoryData orig = DeathInventoryData.get(ev.getOriginal());
            DeathInventoryData nw = DeathInventoryData.get(ev.getPlayer());

            if (nw != null && orig != null)
                nw.copy(orig);
            nw.calledClone();
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof FakePlayer) {
            if (event.getState().getBlock().getTags().contains(ft))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCuriosDrop(DropRulesEvent cde) {
        if ((cde.getEntityLiving() instanceof Player) && FHConfig.SERVER.keepEquipments.get()) {
            cde.addOverride(e -> true, DropRule.ALWAYS_KEEP);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        MobEffectInstance ei = event.getEntityLiving().getEffect(FHEffects.SCURVY.get());
        if (ei != null)
            event.setAmount(event.getAmount() * (0.2f / (ei.getAmplifier() + 1)));
    }

    @SubscribeEvent
    public static void onIEMultiBlockForm(MultiblockFormEvent event) {
        if (event.getPlayer() instanceof FakePlayer) {
            event.setCanceled(true);
            return;
        }
        if (ResearchListeners.multiblock.has(event.getMultiblock()))
            if (event.getPlayer().getCommandSenderWorld().isClientSide) {
                if (!ClientResearchDataAPI.getData().building.has(event.getMultiblock())) {
                    event.setCanceled(true);
                }
            } else {
                if (!ResearchDataAPI.getData(event.getPlayer()).building.has(event.getMultiblock())) {
                    //event.getPlayer().sendStatusMessage(GuiUtils.translateMessage("research.multiblock.cannot_build"), true);
                    event.setCanceled(true);
                }

            }
    }

    //not allow repair
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onItemRepair(AnvilUpdateEvent event) {
        if (event.getLeft().hasTag()) {
            if (event.getLeft().getTag().getBoolean("inner_bounded"))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        Entity ent = event.getSource().getEntity();

        if (!(ent instanceof Player) || ent instanceof FakePlayer) return;
        if (ent.getCommandSenderWorld().isClientSide) return;
        ServerPlayer p = (ServerPlayer) ent;

        ResearchListeners.kill(p, event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onPotionRemove(PotionRemoveEvent event) {
        if (event.getPotion() == FHEffects.ION.get())
            event.setCanceled(true);

    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        ResearchCommand.register(dispatcher);
        DebugCommand.register(dispatcher);
        ScenarioCommand.register(dispatcher);
        TownCommand.register(dispatcher);
        TipCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void tickPlayer(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END
                && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;

            // Scenario runner
            ScenarioConductor runner=FHScenario.getNullable(player);
            if (runner != null && runner.isInited())
            	runner.tick();

            // Heat network statistics update
            if (player.containerMenu instanceof HeatStatContainer) {
            	((HeatStatContainer)player.containerMenu).tick();
            }

            // Research energy display
            if (player.getLevel().getDayTime() % 24000 == 40) {
                long energy = EnergyCore.getEnergy(player);
                int messageNum = player.getRandom().nextInt(3);
                if (energy > 10000)
                    player.displayClientMessage(TranslateUtils.translateMessage("energy.full." + messageNum), false);
                else if (energy >= 5000)
                    player.displayClientMessage(TranslateUtils.translateMessage("energy.suit." + messageNum), false);
                else
                    player.displayClientMessage(TranslateUtils.translateMessage("energy.lack." + messageNum), false);
            }

            // Town data sync (currently, every tick for debug)
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new TeamTownDataS2CPacket(player));
        }
    }
    @SubscribeEvent
    public static void onSleep(SleepingTimeCheckEvent event) {
        long ttime = event.getEntity().getCommandSenderWorld().getDayTime() % 24000;
        if (ttime < 12000)
            event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void playerXPPickUp(PickupXp event) {
        Player player = event.getPlayer();
        for (ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                CompoundTag cn = stack.getTag();
                if (cn == null)
                    continue;
                String inner = cn.getString("inner_cover");
                if (inner.isEmpty() || cn.getBoolean("inner_bounded"))
                    continue;
                CompoundTag cnbt = cn.getCompound("inner_cover_tag");
                int crdmg = cnbt.getInt("Damage");
                if (crdmg > 0 && FHUtils.getEnchantmentLevel(Enchantments.MENDING, cnbt) > 0) {
                    event.setCanceled(true);
                    ExperienceOrb orb = event.getOrb();
                    player.takeXpDelay = 2;
                    player.take(orb, 1);

                    int toRepair = Math.min(orb.value * 2, crdmg);
                    orb.value -= toRepair / 2;
                    crdmg = crdmg - toRepair;
                    cnbt.putInt("Damage", crdmg);
                    cn.put("inner_cover_tag", cnbt);
                    if (orb.value > 0) {
                        player.giveExperiencePoints(orb.value);
                    }
                    orb.remove();
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().level.isClientSide
                && event.getEntityLiving() instanceof ServerPlayer
                && event.getItem().getItem().getTags().contains(FHMain.rl("raw_food"))) {
            ServerPlayer player = (ServerPlayer) event.getEntityLiving();
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
            player.displayClientMessage(TranslateUtils.translate("message.frostedheart.eaten_poisonous_food"), false);
        }
    }

    @SubscribeEvent
    public static void removeSpawnVillage(WorldEvent.CreateSpawnPosition event) {
        if (event.getWorld() instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) event.getWorld();
            try {
                serverWorld.getChunkSource().generator.getSettings().structureConfig().keySet()
                        .remove(StructureFeature.VILLAGE);
            } catch (UnsupportedOperationException e) {
                FHMain.LOGGER.error("Failed to remove vanilla village structures", e);
            }
        }
    }


    @SubscribeEvent
    public static void removeVanillaVillages(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) event.getWorld();
            try {
                serverWorld.getChunkSource().generator.getSettings().structureConfig().keySet()
                        .remove(StructureFeature.VILLAGE);
            } catch (UnsupportedOperationException e) {
                FHMain.LOGGER.error("Failed to remove vanilla village structures", e);
            }
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer && !(event.getPlayer() instanceof FakePlayer)) {
            ServerLevel serverWorld = ((ServerPlayer) event.getPlayer()).getLevel();
            DeathInventoryData dit = DeathInventoryData.get(event.getPlayer());
            dit.tryCallClone(event.getPlayer());
            if (FHConfig.SERVER.keepEquipments.get() && !event.getPlayer().level.isClientSide) {
                if (dit != null)
                    dit.alive(event.getPlayer().inventory);
            }
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
            EnergyCore.getCapability(event.getPlayer()).ifPresent(t->{t.onrespawn();t.sendUpdate((ServerPlayer) event.getPlayer());});
            PlayerTemperatureData.getCapability(event.getPlayer()).ifPresent(PlayerTemperatureData::reset);
            
        }
    }

    @SubscribeEvent
    public static void setKeepInventory(FMLServerStartedEvent event) {
        if (FHConfig.SERVER.alwaysKeepInventory.get()) {
            for (ServerLevel world : event.getServer().getAllLevels()) {
                world.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, event.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void tickResearch(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START
                && event.player instanceof ServerPlayer) {
            ResearchListeners.tick((ServerPlayer) event.player);
        }
    }
}
