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

import static net.minecraft.entity.EntityType.*;
import static net.minecraft.world.biome.Biome.Category.*;

import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHDamageSources;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.WorldTemperature;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.climate.data.DeathInventoryData;
import com.teammoeg.frostedheart.climate.data.FHDataManager;
import com.teammoeg.frostedheart.climate.data.FHDataReloadManager;
import com.teammoeg.frostedheart.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.climate.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.command.AddTempCommand;
import com.teammoeg.frostedheart.command.ClimateCommand;
import com.teammoeg.frostedheart.command.DebugCommand;
import com.teammoeg.frostedheart.command.ResearchCommand;
import com.teammoeg.frostedheart.command.ScenarioCommand;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.agriculture.FHBerryBushBlock;
import com.teammoeg.frostedheart.content.agriculture.FHCropBlock;
import com.teammoeg.frostedheart.content.foods.DailyKitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.tools.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.tools.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.tools.oredetect.ProspectorPick;
import com.teammoeg.frostedheart.recipe.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.recipe.FHRecipeReloadListener;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.research.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.research.network.FHResearchSyncEndPacket;
import com.teammoeg.frostedheart.research.network.FHResearchSyncPacket;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.frostedheart.world.FHStructureFeatures;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.MultiblockFormEvent;
import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.world.MobSpawnInfoBuilder;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.items.modular.IModularItem;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    static ResourceLocation ft = new ResourceLocation("storagedrawers:drawers");

    private static final Set<EntityType<?>> VANILLA_ENTITIES = Sets.newHashSet(COW, SHEEP, PIG, CHICKEN);
    @SubscribeEvent
    public static void checkSleep(SleepingTimeCheckEvent event) {
        if (event.getPlayer().getSleepTimer() >= 100 && !event.getPlayer().getEntityWorld().isRemote) {
            EnergyCore.applySleep(ChunkHeatData.getTemperature(event.getPlayer().getEntityWorld(), event.getSleepingLocation().orElseGet(event.getPlayer()::getPosition)), (ServerPlayerEntity) event.getPlayer());
        }
    }
    @SubscribeEvent
    public static void tickEnergy(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START
                && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            if (!player.isSpectator() && !player.isCreative() && player.ticksExisted % 20 == 0)
                EnergyCore.dT(player);
        }
    }
    @SubscribeEvent
    public static void addManualToPlayer(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
        CompoundNBT nbt = event.getPlayer().getPersistentData();
        CompoundNBT persistent;

        if (nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
            persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        } else {
            nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
        }
        if (!persistent.contains(FHUtils.FIRST_LOGIN_GIVE_MANUAL)) {
            persistent.putBoolean(FHUtils.FIRST_LOGIN_GIVE_MANUAL, false);
            event.getPlayer().inventory.addItemStackToInventory(
                    new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ftbquests", "book"))));
            event.getPlayer().inventory.armorInventory.set(3, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_HELMET)
                    .setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_head"))));
            event.getPlayer().inventory.armorInventory.set(2, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_CHESTPLATE)
                    .setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_chest"))));
            event.getPlayer().inventory.armorInventory.set(1, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_LEGGINGS)
                    .setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_leg"))));
            event.getPlayer().inventory.armorInventory.set(0, FHUtils.ArmorLiningNBT(new ItemStack(Items.IRON_BOOTS)
                    .setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_foot"))));
            if (event.getPlayer().abilities.isCreativeMode) {
                event.getPlayer().sendMessage(new TranslationTextComponent("message.frostedheart.creative_help")
                        .setStyle(Style.EMPTY.applyFormatting(TextFormatting.YELLOW)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, GuiUtils.str("Click to use command")))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/frostedheart research complete all"))), event.getPlayer().getUniqueID());
            }

            event.getPlayer().sendMessage(new TranslationTextComponent("message.frostedheart.temperature_help"), event.getPlayer().getUniqueID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void addOreGenFeatures(BiomeLoadingEvent event) {
        if (event.getName() != null) {
            Biome.Category category = event.getCategory();
            if (category != NETHER && category != THEEND) {
                // Generate gravel and clay disks
                if (category == RIVER || category == BEACH) {
                    for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_DISK)
                        event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
                }
                // Generate rankine ores
                for (ConfiguredFeature<?, ?> feature : FHFeatures.FH_ORES)
                    event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
                // Generate clay and gravel deposit
                if (category != TAIGA && category != EXTREME_HILLS && category != OCEAN && category != DESERT && category != RIVER) {
                    event.getGeneration().withFeature(GenerationStage.Decoration.LOCAL_MODIFICATIONS, FHFeatures.clay_deposit);
                    event.getGeneration().withFeature(GenerationStage.Decoration.LOCAL_MODIFICATIONS, FHFeatures.gravel_deposit);
                }
            }
            //Structures
            if (category == EXTREME_HILLS || category == TAIGA) {
                event.getGeneration().withStructure(FHStructureFeatures.OBSERVATORY_FEATURE);
            }
        }
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        // IReloadableResourceManager resourceManager = (IReloadableResourceManager)
        // dataPackRegistries.getResourceManager();
        event.addListener(new FHRecipeReloadListener(dataPackRegistries));
        event.addListener(FHDataReloadManager.INSTANCE);
//            resourceManager.addReloadListener(ChunkCacheInvalidationReloaderListener.INSTANCE);
    }

    @SubscribeEvent
    public static void addReloadListenersLowest(AddReloadListenerEvent event) {
        DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
        event.addListener(new FHRecipeCachingReloadListener(dataPackRegistries));
    }

    @SubscribeEvent
    public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        Block growBlock = event.getState().getBlock();

        float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
        boolean bz = WorldClimate.isBlizzard(event.getWorld());
        if (bz) {
            if (FHUtils.isBlizzardHarming(event.getWorld(), event.getPos())) {
                event.getWorld().setBlockState(event.getPos(), Blocks.AIR.getDefaultState(), 2);
            } else if (event.getWorld().getRandom().nextInt(3) == 0) {
                event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
            }
            event.setResult(Event.Result.DENY);
        } else if (growBlock instanceof FHCropBlock) {
            return;
        } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
            if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                if (temp < -6 && event.getWorld().getRandom().nextInt(3) == 0) {
                    event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock))
                        event.getWorld().setBlockState(event.getPos(), Blocks.AIR.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }
        } else {
            if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE || bz) {
                // Set back to default state, might not be necessary
                if ((bz || temp < 0) && event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock) && cbs != growBlock.getDefaultState())
                        event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            } else if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX) {
                if (event.getWorld().getRandom().nextInt(3) == 0) {
                    BlockState cbs = event.getWorld().getBlockState(event.getPos());
                    if (cbs.matchesBlock(growBlock))
                        event.getWorld().setBlockState(event.getPos(), Blocks.AIR.getDefaultState(), 2);
                }
                event.setResult(Event.Result.DENY);
            }

        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void biomeLoadingEventRemove(@Nonnull BiomeLoadingEvent event) {
        MobSpawnInfoBuilder spawns = event.getSpawns();

        for (EntityClassification en : EntityClassification.values()) {
            spawns.getSpawner(en).removeIf(entry -> VANILLA_ENTITIES.contains(entry.type));
            
        }

    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doPlayerInteract(PlayerInteractEvent ite) {
    	if(ite.getPlayer() instanceof ServerPlayerEntity&&!(ite.getPlayer() instanceof FakePlayer)) {
    		ScenarioConductor cond=FHScenario.get(ite.getPlayer());
    		cond.playerInited=true;
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
                if (!event.getPlayer().getCooldownTracker().hasCooldown(event.getItemStack().getItem())) {
                    event.getPlayer().getCooldownTracker().setCooldown(event.getItemStack().getItem(), 10);
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
                    event.setCancellationResult(ActionResultType.SUCCESS);
                    event.setCanceled(true);
                }
        }
        if (!ResearchListeners.canUseBlock(event.getPlayer(), event.getWorld().getBlockState(event.getHitVec().getPos()).getBlock())) {
            event.setUseBlock(Result.DENY);

            event.getPlayer().sendStatusMessage(GuiUtils.translateMessage("research.cannot_use_block"), true);
        }

    }

    @SubscribeEvent
    public static void death(PlayerEvent.Clone ev) {
        FHUtils.copyPlayerCapability(DailyKitchen.WANTED_FOOD_CAPABILITY,ev.getOriginal(),ev.getPlayer());
        FHUtils.copyPlayerCapability(EnergyCore.CAPABILITY,ev.getOriginal(),ev.getPlayer());
        //FHUtils.copyPlayerCapability(PlayerTemperatureData.CAPABILITY,ev.getOriginal(),ev.getPlayer());
        //FHMain.LOGGER.info("clone");
        if (!ev.getPlayer().world.isRemote) {
            DeathInventoryData orig = DeathInventoryData.get(ev.getOriginal());
            DeathInventoryData nw = DeathInventoryData.get(ev.getPlayer());

            if (nw != null && orig != null)
                nw.copy(orig);
            nw.calledClone();
        }
    }

    @SubscribeEvent
    public static void finishedEatingFood(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && event.getEntityLiving() instanceof ServerPlayerEntity) {
            ItemStack is = event.getItem();
            Item it = event.getItem().getItem();
            ITempAdjustFood adj = null;
            // System.out.println(it.getRegistryName());
            double tspeed = FHConfig.SERVER.tempSpeed.get();
            if (it instanceof ITempAdjustFood) {
                adj = (ITempAdjustFood) it;
            } else {
                adj = FHDataManager.getFood(is);
            }
            if (adj != null) {
                float current = PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).map(t->t.getBodyTemp()).orElse(0f);
                float max = adj.getMaxTemp(event.getItem());
                float min = adj.getMinTemp(event.getItem());
                float heat = adj.getHeat(event.getItem(),PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).map(t->t.getEnvTemp()).orElse(0f));
                if (heat > 1) {
                    event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPERTHERMIA_INSTANT, (heat) * 2);
                } else if (heat < -1)
                    event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPOTHERMIA_INSTANT, (heat) * 2);
                if (heat > 0) {
                    if (current >= max)
                        return;
                    current += heat * tspeed;
                    if (current > max)
                        current = max;
                } else {
                    if (current <= min)
                        return;
                    current += heat * tspeed;
                    if (current <= min)
                        return;
                }
                final float toset=current;
                PlayerTemperatureData.getCapability((ServerPlayerEntity) event.getEntityLiving()).ifPresent(t->t.setBodyTemp(toset));
            }

            DailyKitchen.tryGiveBenefits((ServerPlayerEntity) event.getEntityLiving(), is);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onArmorDamage(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity && (event.getSource().isFireDamage() || !event.getSource().isUnblockable())) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            float damage = event.getAmount();
            DamageSource p_234563_1_ = event.getSource();
            if (damage > 0) {
                damage = damage / 8.0F;
                if (p_234563_1_.isFireDamage())// fire damage more
                    damage *= 2;
                else if (p_234563_1_.isExplosion())// explode add a lot
                    damage *= 4;
                int amount = (int) damage;
                if (amount != damage)
                    amount += player.getRNG().nextDouble() < (damage - amount) ? 1 : 0;
                if (amount <= 0)
                    return;
                for (ItemStack itemstack : player.getArmorInventoryList()) {
                    if (itemstack.isEmpty())
                        continue;
                    CompoundNBT cn = itemstack.getTag();
                    if (cn == null)
                        continue;
                    String inner = cn.getString("inner_cover");
                    if (inner.isEmpty())
                        continue;
                    if (cn.getBoolean("inner_bounded")) {
                        int dmg = cn.getInt("inner_damage");
                        if (dmg < itemstack.getDamage()) {
                            dmg = itemstack.getDamage();
                        }
                        dmg += amount;
                        if (dmg >= itemstack.getMaxDamage()) {
                            cn.remove("inner_cover");
                            cn.remove("inner_cover_tag");
                            cn.remove("inner_bounded");
                            cn.remove("inner_damage");
                            player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
                        } else cn.putInt("inner_damage", dmg);
                        continue;
                    }
                    CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
                    int i = FHUtils.getEnchantmentLevel(Enchantments.UNBREAKING, cnbt);
                    int j = 0;
                    if (i > 0)
                        for (int k = 0; i > 0 && k < amount; ++k) {
                            if (UnbreakingEnchantment.negateDamage(itemstack, i, player.getRNG())) {
                                ++j;
                            }
                        }
                    amount -= j;
                    if (amount <= 0)
                        continue;
                    int crdmg = cnbt.getInt("Damage");
                    crdmg += amount;
                    InstallInnerRecipe ri = InstallInnerRecipe.recipeList.get(new ResourceLocation(inner));

                    if (ri != null && ri.getDurability() <= crdmg) {// damaged
                        cn.remove("inner_cover");
                        cn.remove("inner_cover_tag");
                        cn.remove("inner_bounded");
                        player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
                    } else {
                        cnbt.putInt("Damage", crdmg);
                        cn.put("inner_cover_tag", cnbt);
                    }
                }

            }
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
        if ((cde.getEntityLiving() instanceof PlayerEntity) && FHConfig.SERVER.keepEquipments.get()) {
            cde.addOverride(e -> true, DropRule.ALWAYS_KEEP);
        }
    }

    // TODO create grow temperature mappings for every plant in the modpack
    @SubscribeEvent
    public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            Block growBlock = event.getPlacedBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof IGrowable) {
                if (growBlock instanceof SaplingBlock) {
                    if (temp < -5) {
                    	player.sendStatusMessage(GuiUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(-6)), true);
                    }
                } else if (growBlock instanceof FHCropBlock) {
                    int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(GuiUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock instanceof FHBerryBushBlock) {
                    int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
                    if (temp < growTemp) {
                        event.setCanceled(true);
                        player.sendStatusMessage(GuiUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(growTemp)), true);
                    }
                } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                    if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE) {
                        event.setCanceled(true);
                        player.sendStatusMessage(GuiUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE)), true);
                    }
                } else if (growBlock == Blocks.NETHERRACK) {

                } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(GuiUtils.translateMessage("crop_not_growable", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE)), true);

                }
            }
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        EffectInstance ei = event.getEntityLiving().getActivePotionEffect(FHEffects.SCURVY.get());
        if (ei != null)
            event.setAmount(event.getAmount() * (0.2f / (ei.getAmplifier() + 1)));
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onIEMultiBlockForm(MultiblockFormEvent event) {
        if (event.getPlayer() instanceof FakePlayer) {
            event.setCanceled(true);
            return;
        }
        if (ResearchListeners.multiblock.has(event.getMultiblock()))
            if (event.getPlayer().getEntityWorld().isRemote) {
                if (!ClientResearchDataAPI.getData().building.has(event.getMultiblock())) {
                    event.setCanceled(true);
                }
            } else {
                if (!ResearchDataAPI.getData((ServerPlayerEntity) event.getPlayer()).building.has(event.getMultiblock())) {
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

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        Entity ent = event.getSource().getTrueSource();

        if (ent == null || !(ent instanceof PlayerEntity) || ent instanceof FakePlayer) return;
        if (ent.getEntityWorld().isRemote) return;
        ServerPlayerEntity p = (ServerPlayerEntity) ent;

        ResearchListeners.kill(p, event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onPotionRemove(PotionRemoveEvent event) {
        if (event.getPotion() == FHEffects.ION.get())
            event.setCanceled(true);

    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        AddTempCommand.register(dispatcher);
        ResearchCommand.register(dispatcher);
        ClimateCommand.register(dispatcher);
        DebugCommand.register(dispatcher);
        ScenarioCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
            World world = event.world;
            if (!world.isRemote && world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;
                SchedulerQueue.tickAll(serverWorld);
                int i = 0;
                for (TeamResearchData trd : FHResearchDataManager.INSTANCE.getAllData()) {
                    if (serverWorld.getDimensionKey().equals(trd.generatorData.dimension)) {
                        if (serverWorld.getGameTime() % 20 == i % 20) {//Split town calculations to multiple seconds
                            if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
                                trd.townData.tick(serverWorld);
                            }
                        }
                    }
                    i++;
                }

                // Update clock source every second, and check hour data if it needs an update
                if (serverWorld.getGameTime() % 20 == 0) {
                    WorldClimate data = WorldClimate.get(serverWorld);
                    data.updateClock(serverWorld);
                    data.updateCache(serverWorld);
                    data.trimTempEventStream();

                }
                if (world.getDayTime() % 24000 == 40) {
                    for (PlayerEntity spe : world.getPlayers()) {
                        if (spe instanceof ServerPlayerEntity && !(spe instanceof FakePlayer)) {
                            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) spe;
                            long energy = EnergyCore.getEnergy(spe);
                            if (energy > 10000)
                                serverPlayer.sendStatusMessage(GuiUtils.translateMessage("energy.full"), false);
                            else if (energy >= 5000)
                                serverPlayer.sendStatusMessage(GuiUtils.translateMessage("energy.suit"), false);
                            else
                                serverPlayer.sendStatusMessage(GuiUtils.translateMessage("energy.lack"), false);
                        }
                    }
                }
            }

        }
    }

    @SubscribeEvent
    public static void tickPlayer(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.END
                && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            ScenarioConductor runner=FHScenario.get(player);
            runner.tick();
        }
    }
    @SubscribeEvent
    public static void onSleep(SleepingTimeCheckEvent event) {
        long ttime = event.getEntity().getEntityWorld().getDayTime() % 24000;
        if (ttime < 12000)
            event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void onUseBoneMeal(BonemealEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            Block growBlock = event.getBlock().getBlock();
            float temp = ChunkHeatData.getTemperature(event.getWorld(), event.getPos());
            if (growBlock instanceof FHCropBlock) {
                int growTemp = ((FHCropBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(GuiUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock instanceof FHBerryBushBlock) {
                int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature() + WorldTemperature.BONEMEAL_TEMPERATURE;
                if (temp < growTemp) {
                    event.setCanceled(true);
                    player.sendStatusMessage(GuiUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(growTemp)), true);
                }
            } else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
                if (temp < WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                    event.setCanceled(true);
                    player.sendStatusMessage(GuiUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.HEMP_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
                }
            } else if (temp < WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE) {
                event.setCanceled(true);
                player.sendStatusMessage(GuiUtils.translateMessage("crop_no_bonemeal", ChunkHeatData.toDisplaySoil(WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE + WorldTemperature.BONEMEAL_TEMPERATURE)), true);
            }
        }
    }

    @SubscribeEvent
    public static void playerXPPickUp(PickupXp event) {
        PlayerEntity player = event.getPlayer();
        for (ItemStack stack : player.getArmorInventoryList()) {
            if (!stack.isEmpty()) {
                CompoundNBT cn = stack.getTag();
                if (cn == null)
                    continue;
                String inner = cn.getString("inner_cover");
                if (inner.isEmpty() || cn.getBoolean("inner_bounded"))
                    continue;
                CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
                int crdmg = cnbt.getInt("Damage");
                if (crdmg > 0 && FHUtils.getEnchantmentLevel(Enchantments.MENDING, cnbt) > 0) {
                    event.setCanceled(true);
                    ExperienceOrbEntity orb = event.getOrb();
                    player.xpCooldown = 2;
                    player.onItemPickup(orb, 1);

                    int toRepair = Math.min(orb.xpValue * 2, crdmg);
                    orb.xpValue -= toRepair / 2;
                    crdmg = crdmg - toRepair;
                    cnbt.putInt("Damage", crdmg);
                    cn.put("inner_cover_tag", cnbt);
                    if (orb.xpValue > 0) {
                        player.giveExperiencePoints(orb.xpValue);
                    }
                    orb.remove();
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
                && event.getEntityLiving() instanceof ServerPlayerEntity
                && event.getItem().getItem().getTags().contains(FHMain.rl("raw_food"))) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            player.addPotionEffect(new EffectInstance(Effects.HUNGER, 400, 1));
            player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.eaten_poisonous_food"), false);
        }
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void removeSpawnVillage(WorldEvent.CreateSpawnPosition event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) event.getWorld();
            try {
                serverWorld.getChunkProvider().generator.func_235957_b_().func_236195_a_().keySet()
                        .remove(Structure.VILLAGE);
            } catch (UnsupportedOperationException e) {
            }
        }
    }


    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void removeVanillaVillages(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) event.getWorld();
            try {
                serverWorld.getChunkProvider().generator.func_235957_b_().func_236195_a_().keySet()
                        .remove(Structure.VILLAGE);
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity && !(event.getPlayer() instanceof FakePlayer)) {
            ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();
            DeathInventoryData dit = DeathInventoryData.get(event.getPlayer());
            dit.tryCallClone(event.getPlayer());
            if (FHConfig.SERVER.keepEquipments.get() && !event.getPlayer().world.isRemote) {
                if (dit != null)
                    dit.alive(event.getPlayer().inventory);
            }
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
            EnergyCore.getCapability(event.getPlayer()).ifPresent(t->{t.onrespawn();t.sendUpdate((ServerPlayerEntity) event.getPlayer());});
            PlayerTemperatureData.getCapability(event.getPlayer()).ifPresent(t->t.reset());
            
        }
    }

    @SubscribeEvent
    public static void setKeepInventory(FMLServerStartedEvent event) {
        if (FHConfig.SERVER.alwaysKeepInventory.get()) {
            for (ServerWorld world : event.getServer().getWorlds()) {
                world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, event.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();
            PacketTarget currentPlayer=PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer());
            FHResearch.sendSyncPacket(currentPlayer);
            FHNetwork.send(currentPlayer,new FHDatapackSyncPacket());
            FHNetwork.send(currentPlayer,new FHResearchDataSyncPacket(
                            FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity) event.getPlayer())));
            serverWorld.getCapability(WorldClimate.CAPABILITY).ifPresent((cap) -> {
                FHNetwork.send(currentPlayer,new FHClimatePacket(cap));
            });
            //System.out.println("=x-x=");
            //System.out.println(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS.getValue(new ResourceLocation(FHMain.MODID,"add_loot")));
        }
    }

    @SubscribeEvent
    public static void syncDataWhenDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();

            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
        }
    }

    @SubscribeEvent
    public static void tickResearch(PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START
                && event.player instanceof ServerPlayerEntity) {
            ResearchListeners.tick((ServerPlayerEntity) event.player);
        }
    }
}
