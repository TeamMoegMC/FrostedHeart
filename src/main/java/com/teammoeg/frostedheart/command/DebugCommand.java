/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.research.research.ResearchCategory;
import com.teammoeg.frostedheart.util.ReferenceValue;
import com.teammoeg.frostedheart.util.io.FileUtil;
import com.teammoeg.frostedheart.world.FHFeatures;
import com.teammoeg.thermopolium.items.StewItem;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket.SingleChunk;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class DebugCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("debug")
                .then(Commands.literal("generate_airship").executes(ct -> {
                    FHFeatures.spacecraft_feature.generate(((ServerWorld) ct.getSource().asPlayer().world), ((ServerWorld) ct.getSource().asPlayer().world).getChunkProvider().getChunkGenerator(), ct.getSource().asPlayer().world.rand,
                            ct.getSource().asPlayer().getPosition());
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("export_food").executes(ct -> {
                    Set<Item> items = new HashSet<>();
                    try (PrintStream ps = new PrintStream(FMLPaths.GAMEDIR.get()
                            .resolve("./food_healing.csv").toFile()); Scanner sc = new Scanner(FMLPaths.GAMEDIR.get()
                            .resolve("./food_values.csv").toFile(), "UTF-8")) {
                        if (sc.hasNextLine()) {
                            sc.nextLine();
                            while (sc.hasNextLine()) {
                                String line = sc.nextLine();
                                if (!line.isEmpty()) {
                                    String[] parts = line.split(",");
                                    if (parts.length == 0) break;
                                    ResourceLocation item = new ResourceLocation(parts[0]);
                                    Item it = ForgeRegistries.ITEMS.getValue(item);

                                    if (it == null || it == Items.AIR) {
                                        ps.println(item + "," + parts[1]);
                                    } else {
                                        items.add(it);
                                        Food f = it.getFood();
                                        if (f == null)
                                            ps.println(item + "," + parts[1]);
                                        else
                                            ps.println(item + "," + f.getHealing());
                                    }
                                }
                            }
                        }
                        for (Item ix : ForgeRegistries.ITEMS) {
                            if (ix == null || ix == Items.AIR) continue;
                            if (items.contains(ix)) continue;
                            if (!ix.isFood()) continue;
                            if (ix instanceof StewItem) continue;
                            items.add(ix);
                            Food f = ix.getFood();
                            if (f != null)
                                ps.println(ix.getRegistryName() + "," + f.getHealing());
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    ct.getSource().sendFeedback(GuiUtils.str("Exported " + items.size() + " Foods"), true);
                    return Command.SINGLE_SUCCESS;
                }))

                .then(Commands.literal("export_quests").executes(ct -> {
                    List<Quest> quests = FTBQuests.PROXY.getQuestFile(false).chapterGroups.stream().flatMap(e -> e.chapters.stream()).flatMap(e -> e.quests.stream())
                            .collect(Collectors.toList());
                    JsonArray ja = new JsonArray();
                    Gson gs = new GsonBuilder().setPrettyPrinting().create();
                    quests.stream().map(e -> {
                        JsonObject jo = new JsonObject();
                        jo.addProperty("title", e.getTitle().getString());
                        jo.addProperty("subtitle", e.getSubtitle().getString());
                        jo.addProperty("chapter", e.getQuestChapter().getTitle().getString());
                        JsonArray dec = new JsonArray();
                        for (ITextComponent it : e.getDescription()) {
                            dec.add(it.getString());
                        }
                        jo.add("description", dec);
                        JsonArray fow = new JsonArray();
                        for (QuestObject qo : e.dependencies) {
                            fow.add(qo.getTitle().getString());
                        }
                        jo.add("parents", fow);
                        JsonArray chi = new JsonArray();
                        for (QuestObject qo : e.getDependants()) {
                            chi.add(qo.getTitle().getString());
                        }
                        jo.add("children", chi);
                        JsonArray tsk = new JsonArray();

                        for (Task t : e.tasks) {
                            String out = "";
                            if (t instanceof CheckmarkTask) {
                                out = "√ " + t.getTitle().getString();
                            } else
                                out = t.getTitle().getString();
                            tsk.add(out);
                        }
                        jo.add("tasks", tsk);
                        JsonArray rwd = new JsonArray();
                        for (Reward r : e.rewards) {
                            rwd.add(r.getTitle().getString());
                        }
                        jo.add("rewards", rwd);

                        return jo;
                    }).forEach(ja::add);
                    try {
                        FileUtil.transfer(gs.toJson(ja), new File(FMLPaths.GAMEDIR.get().toFile(), "quest_export.json"));
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("export_researches").executes(ct -> {
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
                        for (ITextComponent it : e.getODesc()) {
                            odec.add(it.getString());
                        }
                        jo.add("description", odec);
                        JsonArray adec = new JsonArray();
                        for (ITextComponent it : e.getAltDesc()) {
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
                            joc.addProperty("name", t.getName().getString());
                            ITextComponent desc = t.getDescription();
                            ITextComponent hint = t.getHint();
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
                            joe.addProperty("name", r.getName().getString());

                            JsonArray dec = new JsonArray();
                            for (ITextComponent it : r.getTooltip()) {
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
                        FileUtil.transfer(gs.toJson(out), new File(FMLPaths.GAMEDIR.get().toFile(), "research_export.json"));

                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("sort_chunks").executes(ct -> {
                    long now = System.currentTimeMillis();
                    ReferenceValue<Integer> tchunks = new ReferenceValue<>(0);
                    Map<RegistryKey<World>, Map<Team, List<SendChunkPacket.SingleChunk>>> chunksToSend = new HashMap<>();
                    FTBTeamsAPI.getManager().getKnownPlayers().values().stream().filter(t -> t.actualTeam != t).map(t -> Pair.of(t, FTBChunksAPI.manager.getData(t))).filter(p -> p.getSecond() != null)
                            .forEach(d -> {

                                FTBChunksTeamData newData = FTBChunksAPI.getManager().getData(d.getFirst().actualTeam);
                                d.getSecond().getClaimedChunks().forEach(c -> {
                                    d.getSecond().manager.claimedChunks.remove(c.pos);
                                    d.getSecond().save();
                                    c.teamData = newData;
                                    newData.manager.claimedChunks.put(c.pos, c);
                                    newData.save();
                                    chunksToSend.computeIfAbsent(c.pos.dimension, s -> new HashMap<>()).computeIfAbsent(d.getFirst().actualTeam, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z, c));
                                    tchunks.val++;
                                });

                            });
                    if (tchunks.val > 0)
                        for (Entry<RegistryKey<World>, Map<Team, List<SingleChunk>>> entry : chunksToSend.entrySet()) {
                            for (Entry<Team, List<SingleChunk>> entry2 : entry.getValue().entrySet()) {
                                SendManyChunksPacket packet = new SendManyChunksPacket();
                                packet.dimension = entry.getKey();
                                packet.teamId = entry2.getKey().getId();
                                packet.chunks = entry2.getValue();
                                packet.sendToAll(ct.getSource().getServer());
                            }
                        }
                    ct.getSource().sendFeedback(GuiUtils.str("Fixed " + tchunks.val + " Chunks"), true);
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
    }

}
