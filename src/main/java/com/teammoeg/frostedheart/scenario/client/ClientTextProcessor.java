package com.teammoeg.frostedheart.scenario.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.mixin.minecraft.NewChatGuiAccessor;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioResponsePacket;
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
import net.minecraftforge.fml.network.PacketDistributor;

public class ClientTextProcessor {
	static class TextInfo{
		ITextComponent parent;
		int line;
		IReorderingProcessor text;
		boolean reline;
		boolean addLimit() {
			if(text instanceof SizedReorderingProcessor) {
				SizedReorderingProcessor t=(SizedReorderingProcessor) text;
				if(!t.isFinished) { 
					t.limit++;
					return true;
				}
			}
			return false;
		}

		public TextInfo(ITextComponent parent, int line, IReorderingProcessor text, boolean reline) {
			super();
			this.parent = parent;
			this.line = line;
			this.text = text;
			this.reline = reline;
		}

		IReorderingProcessor asFinished() {
			return (text instanceof SizedReorderingProcessor)?((SizedReorderingProcessor) text).asFinished():text;
			
		}
		boolean isFinished() {
			return !(text instanceof SizedReorderingProcessor)||((SizedReorderingProcessor) text).isFinished;
		}
	}
	static LinkedList<TextInfo> msgQueue=new LinkedList<>();
	static int ticks;
	static int page=0;
	static int wait;
	static int ticksToContinue;
	static boolean unFinished=false;
	static boolean hasText=false;
	public static void showOneChar() {

		unFinished=false;
		for(TextInfo t:msgQueue) {
			if(t.addLimit()) { 
				unFinished=true;
				break;
			}
			
		}
		if(!unFinished&&hasText){
			ticksToContinue=40;
			hasText=false;
		}
	}
	public static void reset() {
		ticks=0;
		page=0;
		wait=0;
		ticksToContinue=0;
		msgQueue.clear();
		unFinished=false;
		hasText=false;
	}
	public static void sendContinuePacket(boolean isSkip) {
		FHPacketHandler.send(PacketDistributor.SERVER.noArg(),new ClientScenarioResponsePacket(isSkip,0));
	}
	public static class SizedReorderingProcessor implements IReorderingProcessor{
		IReorderingProcessor origin;
		int limit=0;
		boolean isFinished=false;
		public SizedReorderingProcessor(IReorderingProcessor origin) {
			super();
			this.origin = origin;
		}
		public boolean hasText() {
			return limit>0;
		}
		public IReorderingProcessor asFinished() {
			if(isFinished)return origin;
			return this;
		}
		@Override
		public boolean accept(ICharacterConsumer p_accept_1_) {
			ReferenceValue<Integer> renderTracker=new ReferenceValue<>(0);
			return origin.accept((i,s,c)->{
				isFinished=true;
				if(renderTracker.getVal()<limit) {
					p_accept_1_.accept(i, s, c);
				}else {
					isFinished=false;
				}
				if(c!=65533) {
					renderTracker.setVal(renderTracker.getVal()+1);
				}
				return true;
			});
		}

		public void checkIsFinished() {
			origin.accept((i,s,c)->{
				isFinished=true;
				if(i>=limit){
					isFinished=false;
				}
				return true;
			});
		}
		
	}
	public static boolean hasNext() {
		return unFinished;
	}
	public static boolean isTick() {
		if(wait>0){
			wait--;
			return false;
		}
		if(ticksToContinue>0) {
			ticksToContinue--;
			if(ticksToContinue<=0) {
				ticksToContinue=0;
				sendContinuePacket(false);
			}
		}
		ticks++;
		if(ticks>=2) {
			ticks=0;
			return true;
		}
		return false;
	}

	public static void cls() {
		msgQueue.clear();
	}
	public static void setText(String txt) {
		cls();
		process(txt,true,false);
	}
	private static int countCh(IReorderingProcessor p) {
		ReferenceValue<Integer> count=new ReferenceValue<>(0);
		//if(p instanceof SizedReorderingProcessor)
		//	p=((SizedReorderingProcessor) p).origin;
		p.accept((i,s,c)->{
			if(c!=65533)
				count.setVal(count.getVal()+1);
			return true;
		});
		return count.getVal();
	}
	//fh$scenario$link:
	static int w;
	public static Style preset=null;
	public static void processClient(ITextComponent item, boolean isReline2, boolean isNowait) {
		if(preset!=null)
			item=item.copyRaw().mergeStyle(preset);
		List<IReorderingProcessor> lines;
        if(!msgQueue.isEmpty()&&!msgQueue.peekLast().reline) {
        	TextInfo ti=msgQueue.remove(msgQueue.size()-1);
        	int lastline=ti.line;
        	int lastLimit=countCh(ti.text);
        	System.out.println(lastLimit);
        	IFormattableTextComponent ntext=ti.parent.deepCopy().appendSibling(item);
        	lines=RenderComponentsUtil.func_238505_a_(ntext, w,ClientUtils.mc().fontRenderer);
        	for(int i=lastline;i<lines.size();i++) {
        		
        		IReorderingProcessor line=lines.get(i);
        		
        			
        		//System.out.println(i);
        		//System.out.println(toString(line));
        		if(!isNowait) {
        			SizedReorderingProcessor sized=new SizedReorderingProcessor(line);
        			if(i==lastline)
        				sized.limit=lastLimit;
        			line=sized;
        		}
        		//System.out.println(toString(line));
        		msgQueue.add(new TextInfo(ntext,i,line,true));
        	}
        }else {
	        lines=RenderComponentsUtil.func_238505_a_(item, w,ClientUtils.mc().fontRenderer);
	        int i=0;
	        for(IReorderingProcessor line:lines) {
	        	msgQueue.add(new TextInfo(item,i++,isNowait?line:new SizedReorderingProcessor(line),true));
	        }
        }
        if(!msgQueue.isEmpty()) {
        	msgQueue.peekLast().reline=isReline2;
        }
	}
	public static void process(String text, boolean isReline2, boolean isNowait) {
		if(!text.isEmpty()) {
			hasText=true;
			ITextComponent item=ClientTextComponentUtils.parse(text);
			processClient(item,isReline2,isNowait);
		}else if(!msgQueue.isEmpty()) {
        	msgQueue.peekLast().reline=isReline2;
        }
       
	}
	public static String toString(IReorderingProcessor ipp) {
		StringBuilder sb=new StringBuilder();
		ipp.accept((i,s,c)->{
			sb.appendCodePoint(c);
			return true;
		});
		return sb.toString();
		
	}
	public static void render(Minecraft mc) {
		final int fhchatid=0x05301110;
		w=MathHelper.floor((double)mc.ingameGUI.getChatGUI().getChatWidth() / mc.ingameGUI.getChatGUI().getScale());
        if(ClientTextProcessor.isTick()&&!mc.isGamePaused()) {
        	List<ChatLine<IReorderingProcessor>> i=((NewChatGuiAccessor)mc.ingameGUI.getChatGUI()).getDrawnChatLines();
            
        	
            if(!msgQueue.isEmpty()) {
            	ClientTextProcessor.showOneChar();
            	int j=0;
            	i.removeIf(l->l.getChatLineID()==fhchatid);
            	for(TextInfo t:msgQueue) {
            		if(t.isFinished()&&t.reline) {
            			i.add(0,new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(),t.asFinished(),0));
            			System.out.println("removed "+toString(t.text));
            			j++;
            		}else {
            			//if(hasText) {
            				
            				i.add(0,new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(),t.asFinished(),fhchatid));
            			//}
            			break;
            		}
            		
            	}
            	while(--j>=0) {
            		msgQueue.remove(0);
            	}
            }

        }
	}
}
