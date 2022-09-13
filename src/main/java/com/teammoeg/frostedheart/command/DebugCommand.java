package com.teammoeg.frostedheart.command;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.world.FHFeatures;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.loading.FMLPaths;

public class DebugCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("debug")
                .then(Commands.literal("generate_airship").executes(ct -> {
                    FHFeatures.spacecraft_feature.generate(((ServerWorld) ct.getSource().asPlayer().world), ((ServerWorld) ct.getSource().asPlayer().world).getChunkProvider().getChunkGenerator(), ct.getSource().asPlayer().world.rand,
                            ct.getSource().asPlayer().getPosition());
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("export_quests").executes(ct -> {
                	List<Quest> quests=FTBQuests.PROXY.getQuestFile(false).chapterGroups.stream().flatMap(e->e.chapters.stream()).flatMap(e->e.quests.stream())
                     .collect(Collectors.toList());
                	JsonArray ja=new JsonArray();
                	Gson gs=new GsonBuilder().setPrettyPrinting().create();
                	quests.stream().map(e->{
                		JsonObject jo=new JsonObject();
                		jo.addProperty("title", e.getTitle().getString());
                		jo.addProperty("subtitle", e.getSubtitle().getString());
                		jo.addProperty("chapter", e.getQuestChapter().getTitle().getString());
                		JsonArray dec=new JsonArray();
                		for(ITextComponent it:e.getDescription()) {
                			dec.add(it.getString());
                		}
                		jo.add("description", dec);
                		JsonArray fow=new JsonArray();
                		for(QuestObject qo:e.dependencies) {
                			fow.add(qo.getTitle().getString());
                		}
                		jo.add("parents", fow);
                		JsonArray chi=new JsonArray();
                		for(QuestObject qo:e.getDependants()) {
                			chi.add(qo.getTitle().getString());
                		}
                		jo.add("children",chi);
                		JsonArray tsk=new JsonArray();
                		
                		for(Task t:e.tasks) {
                			JsonObject task=new JsonObject();
                			task.addProperty("title", t.getTitle().getString());
                
                			task.addProperty("type",t.getObjectType().ordinal());
                			tsk.add(task);
                		}
                		jo.add("tasks", tsk);
                		JsonArray rwd=new JsonArray();
                		for(Reward t:e.rewards) {
                			JsonObject task=new JsonObject();
                			task.addProperty("title", t.getTitle().getString());
                			task.addProperty("type",t.getObjectType().ordinal());
                			rwd.add(task);
                		}
                		jo.add("rewards", rwd);
                		
                		return jo;
                	}).forEach(ja::add);
                	try {
						FileUtil.transfer(gs.toJson(ja), new File(FMLPaths.GAMEDIR.get().toFile(),"quest_export.json"));
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    return Command.SINGLE_SUCCESS;
                }));
        
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
    }
  
}
