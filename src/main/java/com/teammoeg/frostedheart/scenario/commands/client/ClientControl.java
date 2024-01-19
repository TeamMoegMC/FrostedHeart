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

package com.teammoeg.frostedheart.scenario.commands.client;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.scenario.client.IClientScene;
import com.teammoeg.frostedheart.scenario.client.gui.layered.GLImageContent;
import com.teammoeg.frostedheart.scenario.client.gui.layered.GraphicsImageContent;
import com.teammoeg.frostedheart.scenario.client.gui.layered.ImageScreenDialog;
import com.teammoeg.frostedheart.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.scenario.client.gui.layered.TextContent;
import com.teammoeg.frostedheart.scenario.client.gui.layered.Transition;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestFile;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.client.audio.BackgroundMusicSelector;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class ClientControl implements IClientControlCommand {
	public void link(IClientScene runner,@Param("lid")String linkId) {
		runner.setPreset(Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"fh$scenario$link:"+linkId)).setUnderlined(true));
	}
	public void endlink(IClientScene runner) {
		runner.setPreset(null);
	}
	@Override
	public void showTask(IClientScene runner,@Param("q")String q,@Param("t")int t) {
		QuestFile qf=FTBQuests.PROXY.getQuestFile(false);
		Quest quest=qf.getQuest(QuestFile.parseCodeString(q));
		Task tsk=quest.tasks.get(t);
		ITextComponent itt;
		if(tsk instanceof ItemTask) {
			itt=GuiUtils.translateMessage("item_task",tsk.getTitle());
			
		}else if(tsk instanceof KillTask) {
			itt=GuiUtils.translateMessage("kill_task",tsk.getTitle());
		}else {
			itt=GuiUtils.translateMessage("other_task",tsk.getTitle());
		}
		runner.cls();
		runner.processClient(ClientTextComponentUtils.parse(itt.getString()), true, false);
		runner.setActHud(null, tsk.getTitle().getString());
	}
	@Override
	public void speed(IClientScene runner,@Param("v")double value,@Param("space")Integer s) {
		runner.setSpeed(value);
		
	}
	@Override
	public void showTitle(IClientScene runner,@Param("t")String t,@Param("st")String st,@Param("in")Integer i1,@Param("show")Integer i2,@Param("out")Integer i3) {
		ITextComponent t1=null,t2=null;
		if(t!=null) {
			t1=ClientTextComponentUtils.parse(t);
			ClientUtils.mc().ingameGUI.renderTitles(t1,null,i1==null?-1:i1,i2==null?-1:i2, i3==null?-1:i3);
		}
		if(st!=null) {
			t2=ClientTextComponentUtils.parse(st);
			ClientUtils.mc().ingameGUI.renderTitles(null,t2,i1==null?-1:i1,i2==null?-1:i2, i3==null?-1:i3);
		}
		
	}
	@Override
	public void fullScreenDialog(IClientScene runner,@Param("show")Integer show,@Param("x")Float x,@Param("y")Float y,@Param("w")Float w,@Param("m")Integer m) {
		if(show!=null) {
			if(show>0&&ClientScene.dialog==null) {
				ClientScene.dialog=new ImageScreenDialog(GuiUtils.str(""));
				ClientUtils.mc().displayGuiScreen(ClientScene.dialog);
			}else {
				ClientScene.dialog.closeScreen();
				ClientScene.dialog=null;
			}
		}
		if(ClientScene.dialog==null)
			return;
		if(x!=null)
			ClientScene.dialog.dialogX=(x);
		if(y!=null)
			ClientScene.dialog.dialogY=(y);
		if(w!=null)
			ClientScene.dialog.dialogW=(w);
		if(m!=null)
			ClientScene.dialog.alignMiddle=m>0;
	}
	@Override
	public void startLayer(IClientScene runner,@Param("n")@Param("name")String name) {
		if(ClientScene.dialog==null)
			return;
		LayerManager lm=ClientScene.layers.peekLast();
		if(lm==null) {
			lm=ClientScene.dialog.primary;
		}else{
			if(name!=null) {
				lm=lm.getLayer(name);
			}else {
				lm=new LayerManager();
			}
		}
		ClientScene.layers.add(lm);
	}
	@Override
	public void showLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("trans")String transition,@Param("t")int time,@Param("x")float x,@Param("y")float y,@Param("w")Float w,@Param("h")Float h) {
		if(ClientScene.dialog==null)
			return;
		LayerManager lm=ClientScene.layers.pollLast();
		if(w==null)
			w=1f;
		if(h==null)
			h=1f;
		if(time<=0)
			time=10;
		lm.setX((x));
		lm.setY((y));
		lm.setWidth((w));
		lm.setHeight((h));
		lm.commitChanges(transition!=null?Transition.valueOf(transition.toLowerCase()):null,time);
		if(ClientScene.layers.isEmpty()) {
			ClientScene.dialog.primary=lm;
		}else {
			ClientScene.layers.peekLast().addLayer(name, lm);
		}
	}
	@Override
	public void ImageLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("s")String path,@Param("x")float x,@Param("y")float y,@Param("w")Float w,@Param("h")Float h,@Param("sx")int u,@Param("sy")int v,@Param("sw")Integer uw,@Param("sh")Integer uh,@Param("z")int z,@Param("opacity")Float opacity) {
		if(ClientScene.dialog==null)
			return;
		if(w==null)
			w=-1f;
		if(h==null)
			h=-1f;
		if(uw==null)
			uw=-1;
		if(uh==null)
			uh=-1;
		if(opacity==null)
			opacity=1f;
		ResourceLocation ip=FHScenarioClient.getPathOf(new ResourceLocation(path), "textures/gui/");
		GraphicsImageContent ic=new GraphicsImageContent(ip,(int)x,(int)(y),(int)(float)(w),(int)(float)(h));
		ic.setZ(z);
		ic.setOpacity(opacity);
		ic.ix=u;
		ic.iy=v;
		ic.iw=uw;
		ic.ih=uh;
		ClientScene.layers.peekLast().addLayer(name,ic);
	}
	@Override
	public void TextLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("text")String text,@Param("x")float x,@Param("y")float y,@Param("w")Float w,@Param("h")Float h,@Param("z")int z,@Param("opacity")Float opacity,@Param("shadow")int shadow,@Param("resize")float resize,@Param("cv")int cv,@Param("ch")int ch,@Param("clr")Integer color) {
		if(ClientScene.dialog==null)
			return;
		if(w==null)
			w=1f;
		if(h==null)
			h=1f;
		if(opacity==null)
			opacity=1f;
		if(resize==0)
			resize=1;
		if(color==null)
			color=0xFFFFFF;
		TextContent tc=new TextContent(ClientTextComponentUtils.parse(text),(x),(y),(w),(h), shadow>0);
		tc.setOpacity(opacity);
		tc.setZ(z);
		tc.setResize(resize);
		tc.centerH=ch>0;
		tc.centerV=cv>0;
		tc.color=0xFFFFFF&color;
		ClientScene.layers.peekLast().addLayer(name,tc);
		
	}
	@Override
	public void freeLayer(IClientScene runner,@Param("n")@Param("name")String name) {
		if(ClientScene.dialog==null)
			return;

		ClientScene.layers.peekLast().freeLayer(name);
		
	}
	@Override
	public void bgm(IClientScene runner,@Param("n")@Param("name")String name) {
		//ISound sound=SimpleSound.music();
		ClientUtils.mc().getMusicTicker().stop();
		ClientUtils.mc().getMusicTicker().selectRandomBackgroundMusic(new BackgroundMusicSelector(new SoundEvent(FHScenarioClient.getPathOf(new ResourceLocation(name),"")), 0, 0, true));
	
	}
	@Override
	public void stopbgm(IClientScene runner) {
		//ISound sound=SimpleSound.music();
		ClientUtils.mc().getMusicTicker().stop();
	}
	List<ISound> current=new ArrayList<>();
	@Override
	public void sound(IClientScene runner,@Param("n")@Param("name")String name,@Param("repeat")int rep) {
		//ISound sound=SimpleSound.music();
		ISound sound=new SimpleSound(FHScenarioClient.getPathOf(new ResourceLocation(name),""), SoundCategory.MASTER,1, 1, rep>0, 0, AttenuationType.LINEAR, 0, 0, 0, true);
		ClientUtils.mc().getSoundHandler().play(sound);
		current.add(sound);
	}
	@Override
	public void stopAllsounds(IClientScene runner) {
		current.forEach(ClientUtils.mc().getSoundHandler()::stop);
		current.clear();
		
	}
}
