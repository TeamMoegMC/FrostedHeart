package com.teammoeg.frostedheart.scenario.client;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.mixin.minecraft.NewChatGuiAccessor;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.scenario.network.ClientScenarioResponsePacket;
import com.teammoeg.frostedheart.scenario.parser.StringParseReader;

import dev.ftb.mods.ftblibrary.util.ClientTextComponentUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.network.PacketDistributor;

public class ClientTextProcessor {
	static List<SizedReorderingProcessor> textlines=new ArrayList<>();
	static int ticks;
	static int page=0;
	static int ticksToContinue;
	static boolean isReline;
	static boolean unFinished=false;
	static boolean hasText=false;
	static boolean initialized;
	public static void showOneChar() {
		unFinished=false;
		for(SizedReorderingProcessor text:textlines) {
			if(!text.isFinished) {
				text.limit++;
				unFinished=true;
				break;
			}
		}
		if(!unFinished&&hasText){
			ticksToContinue=40;
			hasText=false;
		}
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
			return origin.accept((i,s,c)->{
				isFinished=true;
				if(i<limit) {
					p_accept_1_.accept(i, s, c);
				}else {
					isFinished=false;
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
	public static boolean isReline() {
		return !unFinished&&isReline;
	}
	public static boolean hasNext() {
		return unFinished;
	}
	public static boolean isTick() {
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
		textlines.clear();
	}
	public static void setText(String txt) {
		cls();
		process(txt);
	}
	public static void setReline(boolean isReline2) {
		isReline=isReline2;
		
	}
	static int w;
	public static void process(String text) {
		hasText=true;
		ITextComponent item=ClientTextComponentUtils.parse(text);
        List<IReorderingProcessor> lines=RenderComponentsUtil.func_238505_a_(item, w,ClientUtils.mc().fontRenderer);
        for(IReorderingProcessor line:lines) {
        	textlines.add(new SizedReorderingProcessor(line));
        }
	}
	public static void render(Minecraft mc) {
		final int fhchatid=0x05301110;
		w=MathHelper.floor((double)mc.ingameGUI.getChatGUI().getChatWidth() / mc.ingameGUI.getChatGUI().getScale());
        if(ClientTextProcessor.isTick()&&!mc.isGamePaused()) {
        	List<ChatLine<IReorderingProcessor>> i=((NewChatGuiAccessor)mc.ingameGUI.getChatGUI()).getDrawnChatLines();
            
        	
            if(!textlines.isEmpty()) {
            	ClientTextProcessor.showOneChar();
            	if(ClientTextProcessor.isReline()||textlines.size()>1)
            	textlines.removeIf(t->{
            		if(t.isFinished) {
            			i.add(0,new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(),t.asFinished(),0));
            			return true;
            		}
            		return false;
            	});
            	if(hasText) {
	            	i.removeIf(t->t.getChatLineID()==fhchatid);
	            	for(SizedReorderingProcessor line:textlines)
	            		if(line.hasText())
	            			i.add(0,new ChatLine<IReorderingProcessor>(mc.ingameGUI.getTicks(),line.asFinished(),fhchatid));
            	}
	            
            }
        }
	}
}
