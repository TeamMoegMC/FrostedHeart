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

package com.teammoeg.frostedheart.content.scenario.commands.client;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.FHScenarioClient;
import com.teammoeg.frostedheart.content.scenario.client.IClientScene;
import com.teammoeg.frostedheart.content.scenario.client.dialog.HUDDialog;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.Transition;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d.GraphicsImageContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d.GraphicsLineContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d.GraphicsRectContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d.GraphicsTextContent;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.Rect;

import dev.ftb.mods.ftblibrary.icon.Color4I;
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
	public void hudDialog(IClientScene runner,@Param("show")Integer show) {
		HUDDialog id=null;
		if(show!=null) {
			if(show>0) {
				if(!(ClientScene.INSTANCE.dialog instanceof HUDDialog)) {
					id=new HUDDialog();
					ClientScene.INSTANCE.dialog=id;
				}else {
					id=(HUDDialog) ClientScene.INSTANCE.dialog;
				}
			}else if(ClientScene.INSTANCE.dialog!=null){
				ClientScene.INSTANCE.dialog.closeDialog();
				ClientScene.INSTANCE.dialog=null;
			}
		}
	}
	@Override
	public void fullScreenDialog(IClientScene runner,@Param("show")Integer show,@Param("x")Float x,@Param("y")Float y,@Param("w")Float w,@Param("m")Integer m) {
		ImageScreenDialog id=null;
		if(show!=null) {
			if(show>0) {
				if(!(ClientScene.INSTANCE.dialog instanceof ImageScreenDialog)) {
					id=new ImageScreenDialog(GuiUtils.str(""));
					if(ClientScene.INSTANCE.dialog!=null)
						ClientScene.INSTANCE.dialog.closeDialog();
					ClientScene.INSTANCE.dialog=id;
					ClientUtils.mc().displayGuiScreen(id);
				}else {
					id=(ImageScreenDialog) ClientScene.INSTANCE.dialog;
				}
				
				
			}else {
				if(ClientScene.INSTANCE.dialog!=null){
					ClientScene.INSTANCE.dialog.closeDialog();
				}
				/*while(ClientUtils.mc().currentScreen instanceof IScenarioDialog)
					ClientUtils.mc().currentScreen.closeScreen();*/
			}
			
		}
		if(id==null)
			return;
		if(x!=null)
			id.dialogX=(x);
		if(y!=null)
			id.dialogY=(y);
		if(w!=null)
			id.dialogW=(w);
		if(m!=null)
			id.alignMiddle=m>0;
	}
	@Override
	public void startLayer(IClientScene runner,@Param("n")@Param("name")String name) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		LayerManager lm=ClientScene.INSTANCE.layers.peekLast();
		if(lm==null) {
			lm=ClientScene.INSTANCE.dialog.getPrimary();
		}else{
			if(name!=null) {
				lm=lm.getLayer(name);
			}else {
				lm=new LayerManager();
			}
		}
		ClientScene.INSTANCE.layers.add(lm);
	}
	@Override
	public void showLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("trans")String transition,@Param("t")int time,@Param("x")float x,@Param("y")float y,@Param("w")Float w,@Param("h")Float h) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		LayerManager lm=ClientScene.INSTANCE.layers.pollLast();
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
		if(ClientScene.INSTANCE.layers.isEmpty()) {
			ClientScene.INSTANCE.dialog.setPrimary(lm);
		}else {
			ClientScene.INSTANCE.layers.peekLast().addLayer(name, lm);
		}
	}
	@Override
	public void ImageLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("s")String path,@Param("")Rect drect,@Param("s")Rect srect,@Param("z")int z,@Param("opacity")Float opacity) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		if(opacity==null)
			opacity=1f;
		ResourceLocation ip=FHScenarioClient.getPathOf(new ResourceLocation(path), "textures/gui/");
		GraphicsImageContent ic=new GraphicsImageContent(ip,drect,srect);
		ic.setZ(z);
		ic.setOpacity(opacity);
		ClientScene.INSTANCE.layers.peekLast().addLayer(name,ic);
	}
	@Override
	public void TextLayer(IClientScene runner,@Param("n")@Param("name")String name,@Param("text")String text,@Param("")Rect rect,@Param("z")int z,@Param("opacity")Float opacity,@Param("shadow")int shadow,@Param("resize")int resize) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		if(opacity==null)
			opacity=1f;
		if(resize==0)
			resize=9;
		GraphicsTextContent tc=new GraphicsTextContent(ClientTextComponentUtils.parse(text),rect,resize,shadow>0);
		tc.setOpacity(opacity);
		tc.setZ(z);
		ClientScene.INSTANCE.layers.peekLast().addLayer(name,tc);
		
	}
	@Override
	public void FillRect(IClientScene runner,@Param("n")@Param("name")String name,@Param("")Rect rect,@Param("z")int z,@Param("clr")Color4I color) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		GraphicsRectContent tc=new GraphicsRectContent(color,rect);
		tc.setZ(z);
		ClientScene.INSTANCE.layers.peekLast().addLayer(name,tc);
		
	}
	@Override
	public void DrawLine(IClientScene runner,@Param("n")@Param("name")String name,@Param("s")Point start,@Param("d")Point end,@Param("w")int w,@Param("z")int z,@Param("clr")Color4I color) {
		if(ClientScene.INSTANCE.dialog==null)
			return;
		if(w<=0)
			w=1;
		GraphicsLineContent tc=new GraphicsLineContent(color,start,end);
		tc.setZ(z);
		tc.wid=w;
		ClientScene.INSTANCE.layers.peekLast().addLayer(name,tc);
		
	}
	@Override
	public void freeLayer(IClientScene runner,@Param("n")@Param("name")String name) {
		if(ClientScene.INSTANCE.dialog==null)
			return;

		ClientScene.INSTANCE.layers.peekLast().freeLayer(name);
		
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
