package com.teammoeg.frostedheart.scenario.client;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.mixin.minecraft.NewChatGuiAccessor;
import com.teammoeg.frostedheart.scenario.client.gui.layered.ImageScreenDialog;
import com.teammoeg.frostedheart.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioResponsePacket;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.util.ReferenceValue;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.PacketDistributor;

public class ClientScene implements IClientScene {
	public static ClientScene INSTANCE;
	public static ImageScreenDialog dialog;
	public static LinkedList<LayerManager> layers=new LinkedList<>();
	public static class TextInfo {
		ITextComponent parent;
		int line;
		IReorderingProcessor text;
		public boolean addLimit(int amount,boolean toSpace) {
			if (text instanceof SizedReorderingProcessor) {
				SizedReorderingProcessor t = (SizedReorderingProcessor) text;
				if (!t.isFinished) {
					if(toSpace)
						t.limit=t.nextSpace();
					else
						t.limit+=amount;
					return true;
				}
			}
			return false;
		}

		public TextInfo(ITextComponent parent, int line, IReorderingProcessor text) {
			super();
			this.parent = parent;
			this.line = line;
			this.text = text;
			
		}

		public int getMaxLen() {
			return ClientUtils.mc().fontRenderer.getStringWidth(ClientScene.toString(getFinished()))+30;
		}
		public int getCurLen() {
			return ClientUtils.mc().fontRenderer.getStringWidth(ClientScene.toString(text))+30;
		}
		public IReorderingProcessor asFinished() {
			return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).asFinished() : text;

		}
		public boolean isFinished() {
			return !(text instanceof SizedReorderingProcessor) || ((SizedReorderingProcessor) text).isFinished;
		}

		public boolean hasText() {
			return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).hasText() : true;
		}

		public IReorderingProcessor getFinished() {
			// TODO Auto-generated method stub
			return (text instanceof SizedReorderingProcessor) ? ((SizedReorderingProcessor) text).origin : text;
		}
	}

	public ClientScene() {
		super();
		this.setSpeed(1);
	}
	public static int fromRelativeXW(float val) {
		return (int) (val*ClientUtils.mc().getMainWindow().getScaledWidth());
	}
	public static int fromRelativeYH(float val) {
		return (int) (val*ClientUtils.mc().getMainWindow().getScaledHeight());
	}
	LinkedList<ITextComponent> origmsgQueue=new LinkedList<>();
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
	ITextComponent currentActTitle;


	ITextComponent currentActSubtitle;
	public ITextComponent getCurrentActTitle() {
		return currentActTitle;
	}

	public ITextComponent getCurrentActSubtitle() {
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
		unFinished = false;
		hasText = false;
	}

	@Override
	public void sendContinuePacket(boolean isSkip) {
		// if(canSkip)
		FHPacketHandler.send(PacketDistributor.SERVER.noArg(), new ClientScenarioResponsePacket(isSkip, 0));
		status=RunStatus.RUNNING;
		canSkip=false;
	}

	public static class SizedReorderingProcessor implements IReorderingProcessor {
		IReorderingProcessor origin;
		int limit = 0;
		boolean isFinished = false;

		public SizedReorderingProcessor(IReorderingProcessor origin) {
			super();
			this.origin = origin;
		}

		public boolean hasText() {
			return limit > 0;
		}

		public IReorderingProcessor asFinished() {
			if (isFinished) return origin;
			return this;
		}
		public int nextSpace() {
			ReferenceValue<Integer> renderTracker = new ReferenceValue<>(0);
			ReferenceValue<Integer> retTracker = new ReferenceValue<>();
			origin.accept((i, s, c) -> {
				if (c != 65533) {
					renderTracker.setVal(renderTracker.getVal() + 1);
				}
				if (renderTracker.getVal() < limit) return true;
				if(Character.isWhitespace(c)) {
					retTracker.setVal(renderTracker.getVal());
				}
				return true;
			});
			retTracker.setIfAbsent(renderTracker::getVal);
			return retTracker.getVal();
		}
		@Override
		public boolean accept(ICharacterConsumer p_accept_1_) {
			ReferenceValue<Integer> renderTracker = new ReferenceValue<>(0);
			return origin.accept((i, s, c) -> {
				isFinished = true;
				if (renderTracker.getVal() < limit) {
					p_accept_1_.accept(i, s, c);
				} else {
					isFinished = false;
				}
				if (c != 65533) {
					renderTracker.setVal(renderTracker.getVal() + 1);
				}
				return true;
			});
		}

		public void checkIsFinished() {
			origin.accept((i, s, c) -> {
				isFinished = true;
				if (i >= limit) {
					isFinished = false;
				}
				return true;
			});
		}

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
			ticksToContinue = FHConfig.CLIENT.autoModeInterval.get();
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
		List<ChatLine<IReorderingProcessor>> i = ((NewChatGuiAccessor) mc.ingameGUI.getChatGUI()).getDrawnChatLines();
		i.removeIf(l -> l.getChatLineID() == fhchatid);
		for (TextInfo t : msgQueue) {
			i.add(0, new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(), t.getFinished(), 0));
		}
		msgQueue.clear();
		if(mc.currentScreen instanceof IScenarioDialog) {
			IScenarioDialog dialogBox=(IScenarioDialog) mc.currentScreen;
			dialogBox.updateTextLines(msgQueue);
		}
		shouldWrap = false;
		needUpdate = false;
	}

	@Override
	public void setText(String txt) {
		cls();
		process(txt, true, false, true,RunStatus.WAITCLIENT);
	}

	private int countCh(IReorderingProcessor p) {
		ReferenceValue<Integer> count = new ReferenceValue<>(0);
		// if(p instanceof SizedReorderingProcessor)
		// p=((SizedReorderingProcessor) p).origin;
		p.accept((i, s, c) -> {
			if (c != 65533)
				count.setVal(count.getVal() + 1);
			return true;
		});
		return count.getVal();
	}

	// fh$scenario$link:
	private Style preset = null;
	public boolean shouldWrap;

	@Override
	public void processClient(ITextComponent item, boolean isReline, boolean isNowait) {
		if (getPreset() != null)
			item = item.deepCopy().mergeStyle(getPreset());
		List<IReorderingProcessor> lines;
		//System.out.println(msgQueue.size() + ":" + shouldWrap);
		if (!msgQueue.isEmpty() && !shouldWrap) {
			TextInfo ti = msgQueue.remove(msgQueue.size() - 1);
			int lastline = ti.line;
			int lastLimit = countCh(ti.text);
			System.out.println(lastLimit);
			IFormattableTextComponent ntext = ti.parent.deepCopy().appendSibling(item);
			lines = RenderComponentsUtil.func_238505_a_(ntext, getDialogWidth(), ClientUtils.mc().fontRenderer);
			for (int i = lastline; i < lines.size(); i++) {

				IReorderingProcessor line = lines.get(i);

				// System.out.println(i);
				// System.out.println(toString(line));
				if (!isNowait) {
					SizedReorderingProcessor sized = new SizedReorderingProcessor(line);
					if (i == lastline)
						sized.limit = lastLimit;
					line = sized;
				}
				// System.out.println(toString(line));
				msgQueue.add(new TextInfo(ntext, i, line));
			}
		} else {
			lines = RenderComponentsUtil.func_238505_a_(item, getDialogWidth(), ClientUtils.mc().fontRenderer);
			int i = 0;
			for (IReorderingProcessor line : lines) {
				msgQueue.add(new TextInfo(item, i++, isNowait ? line : new SizedReorderingProcessor(line)));
			}
		}
		needUpdate = true;
		shouldWrap = isReline;
		unFinished=true;
	}

	boolean needUpdate = false;
	final int fhchatid = 0x05301110;
	RunStatus status;

	@Override
	public void process(String text, boolean isReline, boolean isNowait, boolean resetScene, RunStatus status) {
		if (resetScene) {
			cls();
		}
		//System.out.println("Received " + isReline + " " + text + " " + resetScene);
		if (!text.isEmpty()) {
			hasText = true;
			ITextComponent item = ClientTextComponentUtils.parse(text);
			processClient(item, isReline, isNowait);
		}
		shouldWrap = isReline;
		this.status = status;

	}

	public static String toString(IReorderingProcessor ipp) {
		StringBuilder sb = new StringBuilder();
		ipp.accept((i, s, c) -> {
			sb.appendCodePoint(c);
			return true;
		});
		return sb.toString();

	}
	public static String toCurrentString(IReorderingProcessor ipp) {
		StringBuilder sb = new StringBuilder();
		ipp.accept((i, s, c) -> {
			sb.appendCodePoint(c);
			return true;
		});
		return sb.toString();

	}
	int w;
	
	public void render(Minecraft mc) {
		w=MathHelper.floor((double) mc.ingameGUI.getChatGUI().getChatWidth() / mc.ingameGUI.getChatGUI().getScale());
		if (!mc.isGamePaused()) {
			if(ticksActUpdate>0)
				ticksActUpdate--;
			if(ticksActStUpdate>0)
				ticksActStUpdate--;
			List<ChatLine<IReorderingProcessor>> i = ((NewChatGuiAccessor) mc.ingameGUI.getChatGUI()).getDrawnChatLines();
			IScenarioDialog dialogBox=null;
			if(mc.currentScreen instanceof IScenarioDialog) {
				dialogBox=(IScenarioDialog) mc.currentScreen;
				dialogBox.tickDialog();
			}
			if (!msgQueue.isEmpty()) {
				if (isTick())
					showOneChar();

				if(needUpdate||mc.ingameGUI.getTicks() % 20 == 0) {
					needUpdate = false;
					if (dialogBox==null) {
						i.removeIf(l -> l.getChatLineID() == fhchatid);
						for (TextInfo t : msgQueue) {
							if (t.hasText()) {
								// if(hasText) {
								i.add(0, new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(), t.asFinished(), fhchatid));
								// }
							}
						}
					}else {
						dialogBox.updateTextLines(msgQueue);
					}
				}
			}
		}
	}

	@Override
	public void setActHud(String title, String subtitle) {
		if(title!=null) {
			if(!title.isEmpty()) 
				this.currentActTitle=ClientTextComponentUtils.parse(title);//.deepCopy().mergeStyle(Style.EMPTY.applyFormatting(TextFormatting.YELLOW).applyFormatting(TextFormatting.BOLD));
			else
				this.currentActTitle=null;
			ticksActUpdate=20;
		}
		if(subtitle!=null) {
			if(!subtitle.isEmpty()) {
				this.currentActSubtitle=ClientTextComponentUtils.parse(subtitle);
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
		if(ClientUtils.mc().currentScreen instanceof IScenarioDialog) {
			return ((IScenarioDialog) ClientUtils.mc().currentScreen).getDialogWidth();
		}else
			return w;
	}
}
