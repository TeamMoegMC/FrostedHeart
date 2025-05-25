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

package com.teammoeg.frostedheart.infrastructure.command;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.caupona.data.recipes.FoodValueRecipe;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.world.FHFeatures;
import com.teammoeg.frostedheart.util.mixin.SeedSetable;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.effects.Effect;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugCommand {
	private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> pBiomeRegistry) {
		return PalettedContainer.codecRO(pBiomeRegistry.asHolderIdMap(), pBiomeRegistry.holderByNameCodec(),
				PalettedContainer.Strategy.SECTION_BIOMES, pBiomeRegistry.getHolderOrThrow(Biomes.PLAINS));
	}

	private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
			Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES,
			Blocks.AIR.defaultBlockState());
	private static CompoundTag saved;
	private static SectionPos lastPos;

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("debug")
				.then(Commands.literal("generate_airship").executes(ct -> {
					FHFeatures.SPACECRAFT.get().place(NoneFeatureConfiguration.INSTANCE,
							((ServerLevel) ct.getSource().getPlayerOrException().level()),
							((ServerLevel) ct.getSource().getPlayerOrException().level()).getChunkSource()
									.getGenerator(),
							ct.getSource().getPlayerOrException().level().random,
							ct.getSource().getPlayerOrException().blockPosition());
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("export_food").executes(ct -> {
					Set<Item> items = new HashSet<>();
					try (PrintStream ps = new PrintStream(
							FMLPaths.GAMEDIR.get().resolve("./food_healing.csv").toFile());
							Scanner sc = new Scanner(FMLPaths.GAMEDIR.get().resolve("./food_values.csv").toFile(),
									"UTF-8")) {
						if (sc.hasNextLine()) {
							sc.nextLine();
							while (sc.hasNextLine()) {
								String line = sc.nextLine();
								if (!line.isEmpty()) {
									String[] parts = line.split(",");
									if (parts.length == 0)
										break;
									ResourceLocation item = new ResourceLocation(parts[0]);
									Item it = CRegistryHelper.getItem(item);

									if (it == null || it == Items.AIR) {
										ps.println(item + "," + parts[1]);
									} else {
										items.add(it);
										FoodProperties f = it.getFoodProperties();
										if (f == null)
											ps.println(item + "," + parts[1]);
										else
											ps.println(item + "," + f.getNutrition());
									}
								}
							}
						}
						for (Item ix : CRegistryHelper.getItems()) {
							if (ix == null || ix == Items.AIR)
								continue;

							if (items.contains(ix))
								continue;
							ItemStack is = new ItemStack(ix);
							FoodValueRecipe fvr = null;
							if (FoodValueRecipe.recipes != null)
								fvr = FoodValueRecipe.recipes.get(ix);
							if (!is.isEdible() && fvr == null)
								continue;
							// if (ix instanceof StewItem) continue;
							items.add(ix);
							FoodProperties f = is.getFoodProperties(null);
							if (f != null)
								ps.println(CRegistryHelper.getRegistryName(ix) + "," + is.getDisplayName().getString()
										+ "," + f.getNutrition() + "," + f.getSaturationModifier());
							else if (fvr != null)
								ps.println(CRegistryHelper.getRegistryName(ix) + "," + is.getDisplayName().getString()
										+ "," + fvr.heal + "," + fvr.sat);
						}
					} catch (Exception e) {
						FHMain.LOGGER.error("Error while exporting food values");
						e.printStackTrace();
					}
					ct.getSource().sendSuccess(() -> Components.str("Exported " + items.size() + " Foods"), true);
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("create_backup").executes(ct -> {
					ServerPlayer spe = ct.getSource().getPlayer();
					BlockPos pos = new BlockPos((int) spe.position().x, (int) spe.position().y, (int) spe.position().z);
					ChunkPos cp = new ChunkPos(pos);
					
					LevelChunk chunk = spe.level().getChunkAt(pos);
					int secIdx = chunk.getSectionIndex(pos.getY());
					LevelChunkSection sec = chunk.getSections()[secIdx];
					lastPos = SectionPos.of(pos);
					// 在这里放置你的读取写入代码
					CompoundTag compoundtag1 = new CompoundTag();
					compoundtag1.put("block_states", BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, sec.getStates())
							.getOrThrow(false, FHMain.LOGGER::error));
					compoundtag1.put("biomes",
							makeBiomeCodec(spe.server.registryAccess().registryOrThrow(ForgeRegistries.Keys.BIOMES))
									.encodeStart(NbtOps.INSTANCE, sec.getBiomes())
									.getOrThrow(false, FHMain.LOGGER::error));
					// simulate save
					saved = compoundtag1;
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("restore_backup").executes(ct -> {
					ServerPlayer spe = ct.getSource().getPlayer();
					SectionPos sec=lastPos;
					ChunkPos cp = new ChunkPos(sec.x(),sec.z());
					LevelChunk chunk=spe.level().getChunk(cp.x, cp.z);
					int secIdx =chunk.getSectionIndexFromSectionY(sec.getY());
					CompoundTag compoundtag = saved;
					PalettedContainer<BlockState> palettedcontainer;
					if (compoundtag.contains("block_states", 10)) {
						palettedcontainer = BLOCK_STATE_CODEC
								.parse(NbtOps.INSTANCE, compoundtag.getCompound("block_states"))
								.promotePartial((p_188283_) -> {
								}).getOrThrow(false, FHMain.LOGGER::error);
					} else {
						palettedcontainer = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY,
								Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
					}
					Registry<Biome> registry = spe.server.registryAccess().registryOrThrow(ForgeRegistries.Keys.BIOMES);
					PalettedContainerRO<Holder<Biome>> palettedcontainerro;
					if (compoundtag.contains("biomes", 10)) {
						palettedcontainerro = makeBiomeCodec(registry)
								.parse(NbtOps.INSTANCE, compoundtag.getCompound("biomes"))
								.promotePartial((p_188274_) -> {
								}).getOrThrow(false, FHMain.LOGGER::error);
					} else {
						palettedcontainerro = new PalettedContainer<>(registry.asHolderIdMap(),
								registry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
					}
					ServerGamePacketListenerImpl impl;
					// 在这里放置你的读取代码
					LevelChunkSection levelchunksection = new LevelChunkSection(palettedcontainer, palettedcontainerro);
					boolean flag = chunk.getSections()[secIdx].hasOnlyAir();
					chunk.getSections()[secIdx] = levelchunksection;
					chunk.setUnsaved(true);

					for (int y = 0; y < 16; y++) {
						int cy = chunk.getSectionYFromSectionIndex(secIdx) + y;
						for (int x = 0; x < 16; x++)
							for (int z = 0; z < 16; z++) {
								BlockState pState = levelchunksection.getBlockState(x, y, z);
								Block block = pState.getBlock();
								final BlockPos pPos = new BlockPos(x, cy, z);

								chunk.getHeightmaps().forEach(ent -> {
									if (ent.getKey().keepAfterWorldgen()) {
										ent.getValue().update(pPos.getX(), pPos.getY(), pPos.getZ(), pState);
									}
								});
								boolean flag1 = levelchunksection.hasOnlyAir();
								if (flag != flag1) {
									spe.level().getChunkSource().getLightEngine().updateSectionStatus(pPos, flag1);
								}

								chunk.getSkyLightSources().update(chunk, x, cy, z);
								spe.level().getChunkSource().getLightEngine().checkBlock(pPos);
							}
					}
					ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk,
							spe.level().getLightEngine(), null, null);
					((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false)
							.forEach(e -> e.connection.send(packet));
					return Command.SINGLE_SUCCESS;
				}))/*
					 * .then(Commands.literal("export_quests").executes(ct -> {
					 * List<Quest> quests =
					 * FTBQuests.PROXY.getQuestFile(false).chapterGroups.stream().flatMap(e ->
					 * e.chapters.stream()).flatMap(e -> e.quests.stream())
					 * .collect(Collectors.toList());
					 * JsonArray ja = new JsonArray();
					 * Gson gs = new GsonBuilder().setPrettyPrinting().create();
					 * quests.stream().map(e -> {
					 * JsonObject jo = new JsonObject();
					 * jo.addProperty("title", e.getTitle().getString());
					 * jo.addProperty("subtitle", e.getSubtitle().getString());
					 * jo.addProperty("chapter", e.getQuestChapter().getTitle().getString());
					 * JsonArray dec = new JsonArray();
					 * for (Component it : e.getDescription()) {
					 * dec.add(it.getString());
					 * }
					 * jo.add("description", dec);
					 * JsonArray fow = new JsonArray();
					 * for (QuestObject qo : e.dependencies) {
					 * fow.add(qo.getTitle().getString());
					 * }
					 * jo.add("parents", fow);
					 * JsonArray chi = new JsonArray();
					 * for (QuestObject qo : e.getDependants()) {
					 * chi.add(qo.getTitle().getString());
					 * }
					 * jo.add("children", chi);
					 * JsonArray tsk = new JsonArray();
					 * 
					 * for (Task t : e.tasks) {
					 * String out = "";
					 * if (t instanceof CheckmarkTask) {
					 * out = "√ " + t.getTitle().getString();
					 * } else
					 * out = t.getTitle().getString();
					 * tsk.add(out);
					 * }
					 * jo.add("tasks", tsk);
					 * JsonArray rwd = new JsonArray();
					 * for (Reward r : e.rewards) {
					 * rwd.add(r.getTitle().getString());
					 * }
					 * jo.add("rewards", rwd);
					 * 
					 * return jo;
					 * }).forEach(ja::add);
					 * try {
					 * FileUtil.transfer(gs.toJson(ja), new File(FMLPaths.GAMEDIR.get().toFile(),
					 * "quest_export.json"));
					 * } catch (IOException e1) {
					 * e1.printStackTrace();
					 * }
					 * return Command.SINGLE_SUCCESS;
					 * }))
					 */.then(Commands.literal("export_researches").executes(ct -> {
					List<Research> quests = FHResearch.getAllResearch();
					JsonObject out = new JsonObject();
					JsonObject categories = new JsonObject();
					for (ResearchCategory rc : ResearchCategory.values()) {
						JsonObject cat = new JsonObject();
						cat.addProperty("name", rc.getName().getString());
						cat.addProperty("desc", rc.getDesc().getString());
						categories.add(rc.name(), cat);
					}
					out.add("categories", categories);
					JsonArray ja = new JsonArray();

					Gson gs = new GsonBuilder().setPrettyPrinting().create();
					quests.stream().map(e -> {
						JsonObject jo = new JsonObject();
						jo.addProperty("title", e.getName().getString());
						jo.addProperty("points", e.getRequiredPoints());
						jo.addProperty("category", e.getCategory().toString());
						JsonArray odec = new JsonArray();
						for (Component it : e.getODesc()) {
							odec.add(it.getString());
						}
						jo.add("description", odec);
						JsonArray adec = new JsonArray();
						for (Component it : e.getAltDesc()) {
							adec.add(it.getString());
						}
						jo.add("alt_description", adec);
						JsonArray fow = new JsonArray();
						for (Research qo : e.getParents()) {
							fow.add(qo.getName().getString());
						}
						jo.add("parents", fow);
						JsonArray chi = new JsonArray();
						for (Research qo : e.getChildren()) {
							chi.add(qo.getName().getString());
						}
						jo.add("children", chi);
						JsonArray tsk = new JsonArray();

						for (Clue t : e.getClues()) {
							JsonObject joc = new JsonObject();
							joc.addProperty("name", t.getName(e).getString());
							Component desc = t.getDescription(e);
							Component hint = t.getHint(e);
							if (desc != null)
								joc.addProperty("desc", desc.getString());
							if (hint != null)
								joc.addProperty("hint", hint.getString());
							joc.addProperty("percent", t.getResearchContribution());
							joc.addProperty("required", t.isRequired());
							tsk.add(joc);
						}
						jo.add("clues", tsk);
						JsonArray rwd = new JsonArray();
						for (Effect r : e.getEffects()) {
							JsonObject joe = new JsonObject();
							joe.addProperty("name", r.getName(e).getString());

							JsonArray dec = new JsonArray();
							for (Component it : r.getTooltip(e)) {
								dec.add(it.getString());
							}
							joe.add("description", dec);
							rwd.add(joe);
						}
						jo.add("effects", rwd);

						return jo;
					}).forEach(ja::add);
					out.add("researches", ja);
					try {
						FileUtil.transfer(gs.toJson(out),
								new File(FMLPaths.GAMEDIR.get().toFile(), "research_export.json"));

					} catch (IOException e1) {
						FHMain.LOGGER.error("Error while exporting researches");
						e1.printStackTrace();
					}
					return Command.SINGLE_SUCCESS;
				})).then(Commands.literal("seed").executes(ct -> {
					ct.getSource().sendSuccess(() -> Components.str("" + ct.getSource().getLevel().getSeed()), false);
					return Command.SINGLE_SUCCESS;
				}).then(Commands.literal("set")
						.then(Commands.argument("seed", LongArgumentType.longArg()).executes(ct -> {
							if (ct.getSource().getLevel() instanceof SeedSetable ss) {
								ss.setSeed(LongArgumentType.getLong(ct, "seed"));
							}
							ct.getSource().sendSuccess(
									() -> Components.str("seeds set.").withStyle(ChatFormatting.GREEN), true);
							return Command.SINGLE_SUCCESS;
						}))))
		/*
		 * .then(Commands.literal("sort_chunks").executes(ct -> {
		 * long now = System.currentTimeMillis();
		 * ReferenceValue<Integer> tchunks = new ReferenceValue<>(0);
		 * Map<ResourceKey<Level>, Map<Team, List<SendChunkPacket.SingleChunk>>>
		 * chunksToSend = new HashMap<>();
		 * 
		 * FTBTeamsAPI.api().getManager().getKnownPlayerTeams().values().stream().map(t-
		 * >(PlayerTeam)t).filter(t -> t.getEffectiveTeam() != t).map(t -> Pair.of(t,
		 * FTBChunksAPI.api().getManager().getOrCreateData(t))).filter(p ->
		 * p.getSecond() != null)
		 * .forEach(d -> {
		 * 
		 * ChunkTeamData newData =
		 * FTBChunksAPI.api().getManager().getOrCreateData(d.getFirst().getEffectiveTeam
		 * ());
		 * d.getSecond().getClaimedChunks().forEach(c -> {
		 * ((ClaimedChunkManagerImpl)(d.getSecond().getManager())).unregisterClaim(c.
		 * getPos());
		 * ((ClaimedChunkManagerImpl)(d.getSecond().getManager())).registerClaim(
		 * c.getPos(),);
		 * c.teamData = newData;
		 * newData.manager.claimedChunks.put(c.pos, c);
		 * newData.save();
		 * chunksToSend.computeIfAbsent(c.pos.dimension, s -> new
		 * HashMap<>()).computeIfAbsent(d.getFirst().actualTeam, s -> new
		 * ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z,
		 * c));
		 * tchunks.val++;
		 * });
		 * 
		 * });
		 * if (tchunks.val > 0)
		 * for (Entry<ResourceKey<Level>, Map<Team, List<SingleChunk>>> entry :
		 * chunksToSend.entrySet()) {
		 * for (Entry<Team, List<SingleChunk>> entry2 : entry.getValue().entrySet()) {
		 * SendManyChunksPacket packet = new SendManyChunksPacket(entry.getKey(),
		 * entry2.getKey().getId(), entry2.getValue());
		 * packet.sendToAll(ct.getSource().getServer());
		 * }
		 * }
		 * ct.getSource().sendSuccess(()->Lang.str("Fixed " + tchunks.val + " Chunks"),
		 * true);
		 * return Command.SINGLE_SUCCESS;
		 * }))
		 */;

		for (String string : new String[] { FHMain.MODID, FHMain.ALIAS, FHMain.TWRID }) {
			dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(add));
		}
	}

}
