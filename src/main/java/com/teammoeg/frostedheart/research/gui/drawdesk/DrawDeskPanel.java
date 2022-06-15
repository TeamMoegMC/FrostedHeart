package com.teammoeg.frostedheart.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHDrawingDeskOperationPacket;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket;
import com.teammoeg.frostedheart.network.research.FHResearchControlPacket.Operator;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.gui.TechButton;
import com.teammoeg.frostedheart.research.gui.tech.ResearchProgressPanel;

import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class DrawDeskPanel extends Panel {
	DrawDeskScreen dd;
	MainGamePanel mgp;
	public DrawDeskPanel(DrawDeskScreen p) {
		super(p);
		dd=p;
		mgp=new MainGamePanel(this,dd);
		mgp.setPosAndSize(165, 25,218,164);
	}
	public void openHelp() {
		showHelp=true;
		mgp.setEnabled(false);
		
	}
	public void closeHelp() {
		showHelp=false;
		mgp.setEnabled(true);
	}
	
	boolean showHelp;
	@Override
	public void addWidgets() {
		
		add(mgp);
		HelpPanel hp=new HelpPanel(this);
		hp.setPosAndSize(140, 19, 243, 170);
		add(hp);
		
		ResearchProgressPanel p = new ResearchProgressPanel(this);
		p.setPosAndSize(14, 19, 111, 68);
		add(p);

		TechButton techTree = new TechButton(this, DrawDeskIcons.TECH) {

			@Override
			public void onClicked(MouseButton arg0) {
				dd.showTechTree();
			}


		};
		techTree.setPosAndSize(16, 68, 36, 19);

		add(techTree);
		TechButton techStop = new TechButton(this, DrawDeskIcons.STOP) {

			@Override
			public void onClicked(MouseButton arg0) {
				Research current=ClientResearchDataAPI.getData().getCurrentResearch().orElse(null);
				if(current!=null)
				PacketHandler.sendToServer(new FHResearchControlPacket(Operator.PAUSE,current));
			}
		};
		techStop.setPosAndSize(55, 68, 19, 19);
		add(techStop);
		Button itemSubmit = new Button(this) {

			@Override
			public void onClicked(MouseButton arg0) {
				PacketHandler.sendToServer(new FHDrawingDeskOperationPacket(dd.getTile().getPos(),3));
			}

			@Override
			public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
				if(isMouseOver())
					DrawDeskIcons.EXAMINE.draw(matrixStack, x, y, w, h);
			}

			@Override
			public void addMouseOverText(TooltipList list) {
				super.addMouseOverText(list);
				list.add(GuiUtils.translateGui("draw_desk.examine"));
			}
			
		};
		itemSubmit.setPosAndSize(113,109, 18, 18);
		add(itemSubmit);
		int sw = 387;
		int sh = 203;
		this.setSize(sw, sh);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
	}

	@Override
	public void alignWidgets() {
	}
	boolean enabled;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	@Override
	public boolean keyPressed(Key k) {
		if(showHelp&&k.esc()) {
			closeHelp();
			return true;
		}
		return super.keyPressed(k);
	}


}
