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

package com.teammoeg.frostedheart.content.scenario.client;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.util.struct.BitObserverList;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.client.dialog.IScenarioDialog;
import com.teammoeg.frostedheart.content.scenario.client.dialog.TextInfo;
import com.teammoeg.frostedheart.content.scenario.client.dialog.TextInfo.SizedReorderingProcessor;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.content.scenario.network.C2SClientReadyPacket;
import com.teammoeg.frostedheart.content.scenario.network.C2SScenarioResponsePacket;
import com.teammoeg.frostedheart.content.scenario.network.C2SSettingsPacket;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.mixin.minecraft.accessors.NewChatGuiAccessor;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

public class ClientScene implements IClientScene {
	private static GuiMessageTag SCENARIO=new GuiMessageTag(13684944, (GuiMessageTag.Icon)null, Lang.translateKey("chat.tag.frostedheart.scenario"), "Scenario");
	public static ClientScene INSTANCE;
	public int curRunId;
	public int curTextId;
	public IScenarioDialog dialog;
	public LinkedList<LayerManager> layers=new LinkedList<>();
	public ClientScene() {
		super();
		w=Mth.floor((double) Minecraft.getInstance().gui.getChat().getWidth() / Minecraft.getInstance().gui.getChat().getScale());
		this.setSpeed(1);
	}
	public static int fromRelativeXW(float val) {
		return (int) (val*ClientUtils.mc().getWindow().getGuiScaledWidth());
	}
	public static int fromRelativeYH(float val) {
		return (int) (val*ClientUtils.mc().getWindow().getGuiScaledHeight());
	}
	LinkedList<Component> origmsgQueue=new LinkedList<>();
	LinkedList<TextInfo> msgQueue = new LinkedList<>();
	int ticks;
	int page = 0;
	int wait;
	int ticksToContinue;
	public int ticksActUpdate;
	public int ticksActStUpdate;
	boolean unFinished = false;
	boolean hasText = false;
	boolean canSkip = false;
	public boolean sendImmediately=false;
	public BitObserverList onRenderComplete=new BitObserverList();
	public BitObserverList onTransitionComplete=new BitObserverList();
	Component currentActTitle;


	Component currentActSubtitle;
	public Component getCurrentActTitle() {
		return currentActTitle;
	}

	public Component getCurrentActSubtitle() {
		return currentActSubtitle;
	}
	public void setSpeed(double value) {
		value*=FHConfig.CLIENT.textSpeed.get();
		if(value<=2) {
			setTicksBetweenShow((int) (2/value));
		}else {
			setTicksBetweenShow(1);
			setCharsPerShow((int) (value/2));
		}
		
	}
	@Override
	public void showOneChar() {
		int i=0;
		for(TextInfo t:msgQueue) {
			if(!t.hasText())break;
			i++;
		}
		unFinished = false;
		for (TextInfo t : msgQueue) {
			i--;
			if (t.addLimit(charsPerShow,showWordMode)) {
				unFinished = true;
				break;
			}

		}
		if(i!=0) {
			needUpdate=true;
			//System.out.println("Force update");
		}

	}
	
	@Override
	public void reset() {
		ticks = 0;
		page = 0;
		wait = 0;
		ticksToContinue = 0;
		msgQueue.clear();
		origmsgQueue.clear();
		unFinished = false;
		hasText = false;
	}

	@Override
	public void sendContinuePacket(boolean isSkip) {
		// if(canSkip)
		FHNetwork.INSTANCE.send(PacketDistributor.SERVER.noArg(), new C2SScenarioResponsePacket(curTextId,isSkip, 0));
		status=RunStatus.RUNNING;
		canSkip=false;
	}

	@Override
	public boolean hasNext() {
		return unFinished;
	}
	public int charsPerShow=1;
	public int ticksBetweenShow=2;
	public boolean showWordMode;
	@Override
	public boolean isTick() {
		if (wait > 0) {
			wait--;
			return false;
		}
		if (!unFinished && status==RunStatus.WAITCLIENT && ticksToContinue<=0) {
			if(sendImmediately) {
				sendContinuePacket(false);
			}else {
				ticksToContinue = FHConfig.CLIENT.autoModeInterval.get();
			}
			hasText = false;
			canSkip = true;
		} else if (status==RunStatus.WAITCLIENT||status==RunStatus.WAITTIMER) {
			canSkip = true;
		}
		if (ticksToContinue > 0) {
			ticksToContinue--;
			if (ticksToContinue <= 0) {
				ticksToContinue = 0;
				sendContinuePacket(false);
			}
		}
		ticks++;
		if (ticks >= ticksBetweenShow) {
			ticks = 0;
			return true;
		}
		return false;
	}

	@Override
	public void cls() {
		Minecraft mc = ClientUtils.mc();
		List<GuiMessage.Line> i = ((NewChatGuiAccessor) mc.gui.getChat()).getTrimmedMessages();
		i.removeIf(l -> l.tag()==SCENARIO);
		msgQueue.clear();
		for(Component ic:origmsgQueue) {
			for(FormattedCharSequence j:ComponentRenderUtils.wrapComponents(ic,w, ClientUtils.mc().font))
				i.add(0, new GuiMessage.Line(mc.gui.getGuiTicks(), j, GuiMessageTag.system(),true));
		}
		origmsgQueue.clear();
		
		if(dialog!=null&&dialog.hasDialog()) {
			dialog.updateTextLines(msgQueue);
		}
		shouldWrap = false;
		needUpdate = false;
	}

	@Override
	public void setText(String txt) {
		cls();
		process(curTextId,txt, true, false, true,RunStatus.WAITCLIENT);
	}

	private int countCh(FormattedCharSequence p) {
		MutableInt count = new MutableInt(0);
		// if(p instanceof SizedReorderingProcessor)
		// p=((SizedReorderingProcessor) p).origin;
		p.accept((i, s, c) -> {
			if (c != 65533)
				count.increment();
			return true;
		});
		return count.getValue();
	}

	// fh$scenario$link:
	private Style preset = null;
	public boolean shouldWrap;

	@Override
	public void processClient(Component item, boolean isReline, boolean isNowait) {
		if (getPreset() != null)
			item = item.copy().withStyle(getPreset());
		List<FormattedCharSequence> lines;
		if (!msgQueue.isEmpty() && !shouldWrap) {
			TextInfo ti = msgQueue.remove(msgQueue.size() - 1);
			int lastline = ti.line;
			int lastLimit = countCh(ti.text);
			MutableComponent ntext = ti.parent.copy().append(item);
			origmsgQueue.pollLast();
			origmsgQueue.add(ntext);
			lines = ComponentRenderUtils.wrapComponents(ntext, getDialogWidth(), ClientUtils.mc().font);
			for (int i = lastline; i < lines.size(); i++) {
				FormattedCharSequence line = lines.get(i);
				if (!isNowait) {
					SizedReorderingProcessor sized = new SizedReorderingProcessor(line);
					if (i == lastline)
						sized.setLimit(lastLimit);
					line = sized;
				}
				// System.out.println(toString(line));
				msgQueue.add(new TextInfo(ntext, i, line));
			}
		} else {
			origmsgQueue.add(item);
			lines = ComponentRenderUtils.wrapComponents(item, getDialogWidth(), ClientUtils.mc().font);
			int i = 0;
			for (FormattedCharSequence line : lines) {
				msgQueue.add(new TextInfo(item, i++, isNowait ? line : new SizedReorderingProcessor(line)));
			}
		}
		needUpdate = true;
		shouldWrap = isReline;
		unFinished=true;
		curTextId=curRunId;
	}

	boolean needUpdate = false;
	final int fhchatid = 0x05301110;
	RunStatus status;

	@Override
	public void process(int curTextId,String text, boolean isReline, boolean isNowait, boolean resetScene, RunStatus status) {
		if (resetScene) {
			cls();
		}
		//System.out.println("Received " + isReline + " " + text + " " + resetScene);
		if (!text.isEmpty()) {
			hasText = true;
			Component item = StringTextComponentParser.parse(text);
			processClient(item, isReline, isNowait);
		}
		shouldWrap = isReline;
		this.status = status;
		this.curTextId=curTextId;

	}

	public static String toString(FormattedCharSequence ipp) {
		StringBuilder sb = new StringBuilder();
		ipp.accept((i, s, c) -> {
			sb.appendCodePoint(c);
			return true;
		});
		return sb.toString();

	}
	public static String toCurrentString(FormattedCharSequence ipp) {
		StringBuilder sb = new StringBuilder();
		ipp.accept((i, s, c) -> {
			sb.appendCodePoint(c);
			return true;
		});
		return sb.toString();

	}
	int w;
	double lastScale=0;
	int lastW=0,lastH=0;
	public void tick(Minecraft mc) {
		w=Mth.floor((double) mc.gui.getChat().getWidth() / mc.gui.getChat().getScale());
		if (!mc.isPaused()) {
			if(lastScale!=mc.getWindow().getGuiScale()||lastW!=mc.getWindow().getGuiScaledWidth()||lastH!=mc.getWindow().getGuiScaledHeight()) {
				lastScale=mc.getWindow().getGuiScale();
				lastW=mc.getWindow().getGuiScaledWidth();
				lastH=mc.getWindow().getGuiScaledHeight();
				this.sendClientUpdate();
			}
			if(ticksActUpdate>0)
				ticksActUpdate--;
			if(ticksActStUpdate>0)
				ticksActStUpdate--;
			List<GuiMessage.Line> i = ((NewChatGuiAccessor) mc.gui.getChat()).getTrimmedMessages();
			if(dialog!=null) {
				dialog.tickDialog();
				
			}
			if (!msgQueue.isEmpty()) {
				if (isTick())
					showOneChar();

				if(needUpdate||mc.gui.getGuiTicks() % 20 == 0) {
					needUpdate = false;
					if (dialog==null||!dialog.hasDialog()) {
						i.removeIf(l -> l.tag() == SCENARIO);
						for (TextInfo t : msgQueue) {
							if (t.hasText()) {
								// if(hasText) {
								i.add(0, new GuiMessage.Line(mc.gui.getGuiTicks(), t.asFinished(), SCENARIO,false));
								// }
							}
						}
					}else {
						dialog.updateTextLines(msgQueue);
					}
				}
			}
		}
	}
	public void sendClientReady() {
		FHNetwork.INSTANCE.sendToServer(new C2SClientReadyPacket(ClientUtils.mc().getLanguageManager().getSelected()));
	}
	public void sendClientUpdate() {
		FHNetwork.INSTANCE.sendToServer(new C2SSettingsPacket());
	}
	@Override
	public void setActHud(String title, String subtitle) {
		if(title!=null) {
			if(!title.isEmpty()) 
				this.currentActTitle=StringTextComponentParser.parse(title);//.deepCopy().mergeStyle(Style.EMPTY.applyFormatting(TextFormatting.YELLOW).applyFormatting(TextFormatting.BOLD));
			else
				this.currentActTitle=null;
			ticksActUpdate=20;
		}
		if(subtitle!=null) {
			if(!subtitle.isEmpty()) {
				this.currentActSubtitle=StringTextComponentParser.parse(subtitle);
			}else 
				this.currentActSubtitle=null;
			ticksActStUpdate=20;
		}
			
		
		
	}
	@Override
	public Style getPreset() {
		return preset;
	}
	@Override
	public void setPreset(Style preset) {
		this.preset = preset;
	}
	@Override
	public int getCharsPerShow() {
		return charsPerShow;
	}
	@Override
	public void setCharsPerShow(int charsPerShow) {
		this.charsPerShow = charsPerShow;
	}
	@Override
	public int getTicksBetweenShow() {
		return ticksBetweenShow;
	}
	@Override
	public void setTicksBetweenShow(int ticksBetweenShow) {
		this.ticksBetweenShow = ticksBetweenShow;
	}
	@Override
	public boolean isShowWordMode() {
		return showWordMode;
	}
	@Override
	public void setShowWordMode(boolean showWordMode) {
		this.showWordMode = showWordMode;
	}

	int getDialogWidth() {
		if(dialog!=null&&dialog.hasDialog()) {
			return dialog.getDialogWidth();
		}
		return w;
	}
	@Override
	public int getRunId() {
		return curRunId;
	}
}
