/*
 * Copyright (c) 2024 TeamMoeg
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

import com.google.common.collect.Sets;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.bootstrap.common.ToolCompat;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatContainer;
import com.teammoeg.frostedheart.content.utility.DeathInventoryData;
import com.teammoeg.frostedheart.content.utility.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.utility.oredetect.ProspectorPick;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.data.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.util.CConstants;
import com.teammoeg.frostedheart.util.IgnitionHandler;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.items.modular.IModularItem;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Miscellaneous common events that are not specific to any particular module.
 * Examples: steam energy, utility, data, compat
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FHCommonEvents {

	private static final Set<EntityType<?>> VANILLA_ENTITIES = Sets.newHashSet(EntityType.COW, EntityType.SHEEP,
			EntityType.PIG, EntityType.CHICKEN);
	private static final TagKey<Block> DRAWERS = BlockTags.create(new ResourceLocation("storagedrawers:drawers"));

	@SubscribeEvent
	public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof ServerPlayer) {// server-side only capabilities
			ServerPlayer player = (ServerPlayer) event.getObject();
			if (!(player instanceof FakePlayer)) {
				event.addCapability(new ResourceLocation(FHMain.MODID, "death_inventory"),
						FHCapabilities.DEATH_INV.provider());
			}
		}
		// Common capabilities

	}

    @SubscribeEvent
    public static void attachToWorld(AttachCapabilitiesEvent<Level> event) {
        
    }
	@SubscribeEvent
	public static void loginReminder(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
		CompoundTag nbt = event.getEntity().getPersistentData();
		CompoundTag persistent;

		if (nbt.contains(Player.PERSISTED_NBT_TAG)) {
			persistent = nbt.getCompound(Player.PERSISTED_NBT_TAG);
		} else {
			nbt.put(Player.PERSISTED_NBT_TAG, (persistent = new CompoundTag()));
		}
		if (!persistent.contains(CConstants.FIRST_LOGIN_GIVE_MANUAL)) {
			persistent.putBoolean(CConstants.FIRST_LOGIN_GIVE_MANUAL, false);
			event.getEntity().getInventory()
					.add(new ItemStack(CRegistryHelper.getItem(new ResourceLocation("ftbquests", "book"))));
			event.getEntity().getInventory().armor.set(3, CUtils.ArmorNBT(new ItemStack(FHItems.space_hat), 107, 6)
					.setHoverName(Lang.translateKey("itemname.frostedheart.start_head")));
			event.getEntity().getInventory().armor.set(2, CUtils.ArmorNBT(new ItemStack(FHItems.space_jacket), 107, 6)
					.setHoverName(Lang.translateKey("itemname.frostedheart.start_chest")));
			event.getEntity().getInventory().armor.set(1, CUtils.ArmorNBT(new ItemStack(FHItems.space_pants), 107, 6)
					.setHoverName(Lang.translateKey("itemname.frostedheart.start_leg")));
			event.getEntity().getInventory().armor.set(0, CUtils.ArmorNBT(new ItemStack(FHItems.space_boots), 107, 6)
					.setHoverName(Lang.translateKey("itemname.frostedheart.start_foot")));
			if (event.getEntity().getAbilities().instabuild) {
				event.getEntity()
						.sendSystemMessage(Lang.translateKey("message.frostedheart.creative_help")
								.setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW)
										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
												Components.str("Click to use command")))
										.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
												"/frostedheart research complete all"))));
			}

			event.getEntity().sendSystemMessage(Lang.translateKey("message.frostedheart.temperature_help"));
		}
	}

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent event) {
		ReloadableServerResources dataPackRegistries = event.getServerResources();
		// IReloadableResourceManager resourceManager = (IReloadableResourceManager)
		// dataPackRegistries.getResourceManager();
		// event.addListener(new FHRecipeReloadListener(dataPackRegistries));
//            resourceManager.addReloadListener(ChunkCacheInvalidationReloaderListener.INSTANCE);
	}

	@SubscribeEvent
	public static void addReloadListenersLowest(AddReloadListenerEvent event) {
		ReloadableServerResources dataPackRegistries = event.getServerResources();
		event.addListener(new FHRecipeCachingReloadListener(dataPackRegistries));
	}

	/**
	 * Lights the block on fire if it can be lit, otherwise places a fire block.
	 */
	@SubscribeEvent
	public static void lightingFire(PlayerInteractEvent.RightClickBlock event) {
		ItemStack handStack = event.getEntity().getMainHandItem();
		ItemStack offHandStack = event.getEntity().getOffhandItem();
		Player player = event.getEntity();
		Level level = event.getLevel();
		RandomSource rand = level.random;
		BlockPos blockpos = event.getPos();
		BlockState blockstate = level.getBlockState(blockpos);

		if (!handStack.isEmpty() && !offHandStack.isEmpty() && !handStack.is(ItemTags.CREEPER_IGNITERS)
				&& (handStack.is(Tags.Items.RODS_WOODEN) && offHandStack.is(Tags.Items.RODS_WOODEN)
						|| handStack.is(FHTags.Items.IGNITION_METAL.tag)
								&& offHandStack.is(FHTags.Items.IGNITION_MATERIAL.tag)
						|| handStack.is(FHTags.Items.IGNITION_MATERIAL.tag)
								&& offHandStack.is(FHTags.Items.IGNITION_METAL.tag))) {
			// place fire block
			if (!CampfireBlock.canLight(blockstate) && !CandleBlock.canLight(blockstate)
					&& !CandleCakeBlock.canLight(blockstate)) {
				BlockPos blockpos1 = blockpos.relative(event.getHitVec().getDirection());
				if (BaseFireBlock.canBePlacedAt(level, blockpos1, player.getDirection())) {
					player.swing(InteractionHand.MAIN_HAND);
					level.playSound(player, blockpos1, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
							level.getRandom().nextFloat() * 0.4F + 0.8F);
					if (level.isClientSide()) {
						for (int i = 0; i < 5; i++) {
							level.addParticle(ParticleTypes.SMOKE,
									player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25,
									player.getY() + 0.5f + rand.nextFloat() * 0.25,
									player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
						}
						level.addParticle(ParticleTypes.FLAME,
								player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25,
								player.getY() + 0.5f + rand.nextFloat() * 0.25,
								player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
					}
					if (IgnitionHandler.tryIgnition(rand, handStack, offHandStack)) {
						BlockState blockstate1 = BaseFireBlock.getState(level, blockpos1);
						level.setBlock(blockpos1, blockstate1, 11);
						level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
						event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
					} else {
						event.setCancellationResult(InteractionResult.PASS);
						event.setCanceled(true);
					}

				} else {
					event.setCancellationResult(InteractionResult.FAIL);
					event.setCanceled(true);
				}
			}
			// light the block
			else {
				player.swing(InteractionHand.MAIN_HAND);
				level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
						level.getRandom().nextFloat() * 0.4F + 0.8F);

				if (level.isClientSide()) {
					for (int i = 0; i < 5; i++) {
						level.addParticle(ParticleTypes.SMOKE,
								player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25,
								player.getY() + 0.5f + rand.nextFloat() * 0.25,
								player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
					}
					level.addParticle(ParticleTypes.FLAME,
							player.getX() + player.getLookAngle().x() + rand.nextFloat() * 0.25,
							player.getY() + 0.5f + rand.nextFloat() * 0.25,
							player.getZ() + player.getLookAngle().z() + rand.nextFloat() * 0.25, 0, 0.01, 0);
				}

				if (IgnitionHandler.tryIgnition(rand, handStack, offHandStack)) {
					level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.LIT, true), 11);
					level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
					event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
					event.setCanceled(true);
				} else {
					event.setCancellationResult(InteractionResult.PASS);
					event.setCanceled(true);
				}

			}
		}

	}

	@SubscribeEvent
	public static void prospecting(PlayerInteractEvent.RightClickBlock event) {
		InteractionResult rs = null;
		if (event.getItemStack().canPerformAction(ToolCompat.coreSpade))
			rs = CoreSpade.doProspect(event.getEntity(), event.getLevel(), event.getPos(), event.getItemStack(),
					event.getHand());
		else if (event.getItemStack().canPerformAction(ToolCompat.geoHammer))
			rs = GeologistsHammer.doProspect(event.getEntity(), event.getLevel(), event.getPos(), event.getItemStack(),
					event.getHand());
		else if (event.getItemStack().canPerformAction(ToolCompat.proPick))
			rs = ProspectorPick.doProspect(event.getEntity(), event.getLevel(), event.getPos(), event.getItemStack(),
					event.getHand());
		if (rs != null) {
			if (event.getItemStack().getItem() instanceof IModularItem mo && event.getLevel().getRandom().nextBoolean())
				mo.tickProgression(event.getEntity(), event.getItemStack(),1);
			event.setCancellationResult(rs);
			event.setCanceled(true);
		}

	}

	@SubscribeEvent
	public static void death(PlayerEvent.Clone ev) {
		//call this to make capability temporary available for copy
		ev.getOriginal().reviveCaps();
		CUtils.clonePlayerCapability(FHCapabilities.WANTED_FOOD.capability(), ev.getOriginal(), ev.getEntity());
		// CUtils.clonePlayerCapability(FHCapabilities.ENERGY,ev.getOriginal(),ev.getEntity());
		CUtils.clonePlayerCapability(FHCapabilities.SCENARIO, ev.getOriginal(), ev.getEntity());
		CUtils.clonePlayerCapability(FHCapabilities.WAYPOINT, ev.getOriginal(), ev.getEntity());
		//System.out.println(LazyOptional.empty());
		//System.out.println(FHCapabilities.PLAYER_TEMP.getCapability(ev.getOriginal()));
		CUtils.copyPlayerCapability(FHCapabilities.PLAYER_TEMP, ev.getOriginal(), ev.getEntity());
		//System.out.println("clone called");
		//System.out.println(FHCapabilities.PLAYER_TEMP.getCapability(ev.getEntity()).orElse(null));
		//System.out.println("called cloneex");
		if(ev.isWasDeath()) 
			FHCapabilities.PLAYER_TEMP.getCapability(ev.getEntity()).ifPresent(t -> t.deathResetTemperature());
		// FHMain.LOGGER.info("clone");

			DeathInventoryData orig = DeathInventoryData.get(ev.getOriginal());
			DeathInventoryData nw = DeathInventoryData.get(ev.getEntity());
			//System.out.println("dit:"+orig+"/"+nw);
			if (nw != null && orig != null)
				nw.copy(orig);
			if (nw != null)
				nw.calledClone();

		//re-invalidate to make capability discarded
		ev.getOriginal().invalidateCaps();
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getPlayer() instanceof FakePlayer) {
			if (ForgeRegistries.BLOCKS.getHolder(event.getState().getBlock()).get().is(DRAWERS))
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onCuriosDrop(DropRulesEvent cde) {
		if ((cde.getEntity() instanceof Player) && FHConfig.SERVER.keepEquipments.get()) {
			cde.addOverride(e -> true, DropRule.ALWAYS_KEEP);
		}
	}

	@SubscribeEvent
	public static void onHeal(LivingHealEvent event) {
		MobEffectInstance ei = event.getEntity().getEffect(FHMobEffects.SCURVY.get());
		if (ei != null)
			event.setAmount(event.getAmount() * (0.2f / (ei.getAmplifier() + 1)));
	}

	// not allow repair
	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
	public static void onItemRepair(AnvilUpdateEvent event) {
		if (event.getLeft().hasTag()) {
			if (event.getLeft().getTag().getBoolean("inner_bounded"))
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPotionRemove(MobEffectEvent.Remove event) {
		if (event.getEffect() == FHMobEffects.ION.get())
			event.setCanceled(true);

	}

	@SubscribeEvent
	public static void tickPlayer(PlayerTickEvent event) {
		if (event.side == LogicalSide.SERVER && event.phase == Phase.END
				&& event.player instanceof ServerPlayer player) {
			// Heat network statistics update
			if (player.containerMenu instanceof HeatStatContainer) {
				((HeatStatContainer) player.containerMenu).tick();
			}
		}
	}

	@SubscribeEvent
	public static void disableSleepJumpTimeInStorm(SleepingTimeCheckEvent event) {
		// Disable sleep jumping time if it is day time (thunder)
		long ttime = event.getEntity().getCommandSenderWorld().getDayTime() % 24000;
		if (ttime < 12000)
			event.setResult(Result.DENY);
	}

	@SubscribeEvent
	public static void playerXPPickUp(PickupXp event) {
		Player player = event.getEntity();
		FHCapabilities.PLAYER_TEMP.getCapability(player).ifPresent(p->{
			List<BodyPart> randomized=new ArrayList<>(Arrays.asList(BodyPart.values()));
			Collections.shuffle(randomized);
			for(BodyPart bp:randomized) {
				ItemStackHandler ish=p.getClothesByPart(bp);
				for(int i=0;i<ish.getSlots();i++) {
					ItemStack stack=ish.getStackInSlot(i);
					if (!stack.isEmpty()) {
						if (stack.getDamageValue() > 0 && stack.getEnchantmentLevel(Enchantments.MENDING) > 0) {
							event.setCanceled(true);
							ExperienceOrb orb = event.getOrb();
							player.takeXpDelay = 2;
							player.take(orb, 1);
							int damage=stack.getDamageValue();
							int toRepair = (int) Math.min(orb.value * stack.getXpRepairRatio(), damage);
							orb.value -= toRepair / stack.getXpRepairRatio();
							stack.setDamageValue(damage-toRepair);
							if (orb.value > 0) {
								player.giveExperiencePoints(orb.value);
							}
							orb.remove(RemovalReason.DISCARDED);
							return;
						}
					}
				}
			}

		});

	}

	@SubscribeEvent
	public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer && !(event.getEntity() instanceof FakePlayer)) {
			DeathInventoryData dit = DeathInventoryData.get(event.getEntity());
			//dit.tryCallClone(event.getEntity());
			//System.out.println("respawn called");
			if (FHConfig.SERVER.keepEquipments.get() && !event.getEntity().level().isClientSide) {
				if (dit != null) {
					//System.out.println("restore items");
					dit.alive(event.getEntity().getInventory());
				}
			}
		}
	}

	@SubscribeEvent
	public static void disableTillingCoarseDirtIntoDirt(BlockEvent.BlockToolModificationEvent event) {
		if (!event.getLevel().isClientSide() && event.getToolAction() == ToolActions.HOE_TILL) {
			BlockState state = event.getState();
			if (state.is(Blocks.COARSE_DIRT)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void disableShovellingCoarseDirtIntoPath(BlockEvent.BlockToolModificationEvent event) {
		if (!event.getLevel().isClientSide() && event.getToolAction() == ToolActions.SHOVEL_FLATTEN) {
			BlockState state = event.getState();
			if (state.is(Blocks.COARSE_DIRT)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void tillMudIntoDirt(BlockEvent.BlockToolModificationEvent event) {
		if (!event.getLevel().isClientSide() && event.getPlayer() instanceof ServerPlayer player
				&& event.getToolAction() == ToolActions.HOE_TILL) {
			BlockState state = event.getState();
			if (state.is(Blocks.MUD)) {
				BlockPos pos = event.getPos();
				ServerLevel level = (ServerLevel) event.getLevel();
				ItemStack offHand = player.getOffhandItem();
				if (offHand.isEmpty()) {
					event.setCanceled(true);
				} else if (offHand.is(FHItems.BIOMASS.get())) {
					level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
					offHand.shrink(1);
					event.getHeldItemStack().hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(event.getContext().getHand()));
				}
			}
		}
	}

	@SubscribeEvent
	public static void shovelSnow(BlockEvent.BlockToolModificationEvent event) {
		if (!event.getLevel().isClientSide() && event.getToolAction() == ToolActions.SHOVEL_FLATTEN) {
			var state = event.getState();
			if (state.getBlock() instanceof SnowLayerBlock) {
				var pos = event.getPos();
				var level = (ServerLevel)event.getLevel();
				// 获取最顶层的雪片
				while (level.getBlockState(pos.above()).getBlock() instanceof SnowLayerBlock) {
					pos = pos.offset(0, 1, 0);
				}
				state = level.getBlockState(pos);

				// 减少1层雪
				peelSnowLayer(level, state, pos);

				// 生成掉落物
				var player = (ServerPlayer)event.getPlayer();
				if (player != null && !player.isCreative()) {
					var drops = Block.getDrops(state.getBlock().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1),
							level, pos, null, player, event.getHeldItemStack());
					for (ItemStack drop : drops) {
						// 在玩家当前选择的面掉落方块
						Block.popResourceFromFace(level, event.getPos(), event.getContext().getClickedFace(), drop);
					}
					event.getHeldItemStack().hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(event.getContext().getHand()));
				}
			}
		}
	}

	/**
	 * 削掉一层雪
	 *
	 * @return 雪片是否被摧毁
	 */
	public static boolean peelSnowLayer(Level level, BlockState snowState, BlockPos snowPos) {
		int layers = snowState.getValue(SnowLayerBlock.LAYERS);
		BlockState newState;
		if (layers > 1) {
			newState = snowState.setValue(SnowLayerBlock.LAYERS, layers - 1);
			level.setBlockAndUpdate(snowPos, newState);
		} else {
			newState = Blocks.AIR.defaultBlockState();
			level.destroyBlock(snowPos, false);
		}
		level.sendBlockUpdated(snowPos, snowState, newState, 11);
		return layers <= 1;
	}

	@SubscribeEvent
	public static void disableSpawningPhantoms(PlayerSpawnPhantomsEvent event) {
		event.setResult(Result.DENY);
	}

}
