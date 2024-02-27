package com.teammoeg.frostedheart.content.generator;


import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.client.Point;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonBoolean;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonState;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import blusunrize.immersiveengineering.common.network.MessageTileSync;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class MasterGeneratorScreen<T extends MasterGeneratorTileEntity<T>> extends IEContainerScreen<MasterGeneratorContainer<T>> {
	T tile;
	public static final int TEXW=512;
	public static final int TEXH=256;
	private static final ResourceLocation TEXTURE = GuiUtils.makeTextureLocation("general_generator");
	public static class MasterGeneratorGuiButtonBoolean extends GuiButtonBoolean{

		public MasterGeneratorGuiButtonBoolean(int x, int y, int w, int h, boolean state,
				int u, int v, IIEPressable<GuiButtonState<Boolean>> handler) {
			super(x, y, w, h, null, state, TEXTURE, u, v, 0, handler);
		}
		
		public void blit(MatrixStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
		      blit(matrixStack, x, y,getBlitOffset(), uOffset, vOffset, uWidth, vHeight, TEXH, TEXW);
		   }
	}
	public static class MasterGeneratorGuiButtonUpgrade extends GuiButtonState<Integer>{

		public MasterGeneratorGuiButtonUpgrade(int x, int y, int w, int h,
				int initialState,  int u, int v,
				IIEPressable<GuiButtonState<Integer>> handler) {
			super(x, y, w, h, StringTextComponent.EMPTY, new Integer[] {0,1,2,3}, initialState, TEXTURE, u, v, 1, handler);
		}

		public void blit(MatrixStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
		      blit(matrixStack, x, y,getBlitOffset(), uOffset, vOffset, uWidth, vHeight, TEXH, TEXW);
		   }
	}
	
	public static final Point[] rangelevels=new Point[] {
			new Point(256,0),new Point(384,0),
			new Point(256,64),new Point(384,64),
			new Point(256,128)
	};
	public MasterGeneratorScreen(MasterGeneratorContainer<T> inventorySlotsIn, PlayerInventory inv, ITextComponent title) {
		super(inventorySlotsIn, inv, title);
		tile=inventorySlotsIn.tile;
		this.ySize=222;
		
	}
	public void blit(MatrixStack matrixStack, int x, int y,int w,int h, int u, int v, int uWidth, int vHeight) {
		AbstractGui.blit(matrixStack,guiLeft + x,guiTop+y, w, h, u, v, uWidth, vHeight, TEXW, TEXH);
	}
	public void blit(MatrixStack matrixStack, int x, int y,int w,int h, int u, int v) {
		blit(matrixStack,x,y, w, h, u, v, w, h);
	}
	public MasterGeneratorContainer<T> getContainer() {
		return container;
	}
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
		ClientUtils.bindTexture(TEXTURE);
		//background
		this.blit(matrixStack, 0, 0, this.xSize, this.ySize, 0, 0);

		//System.out.println(ininvarrx+","+ininvarry+"-"+inarryl);
		//range circle
		int actualRangeLvl=(int) (tile.getRangeLevel()+0.05);
		this.blit(matrixStack, 24,61, 128, 64,rangelevels[actualRangeLvl].getX(), rangelevels[actualRangeLvl].getY());
		
		//fuel slots
		Point in=container.getSlotIn();
		Point out=container.getSlotOut();


		int ininvarry=in.getY()+6;
		int outinvarry=out.getY()+6;
		int ininvarrx=in.getX()+18;
		int outinvarrx=98;
		int inarryl=76-ininvarrx;
		int outarryl=out.getX()-2-outinvarrx;
		//arrows
		this.blit(matrixStack, ininvarrx,ininvarry, inarryl, 4, 511-inarryl, 132);
		this.blit(matrixStack, outinvarrx,outinvarry, outarryl, 4,511-outarryl,132);
		//slot background
		this.blit(matrixStack,in.getX()-2, in.getY()-2, 20, 20, 404, 128);
		this.blit(matrixStack,out.getX()-2, out.getY()-2, 20, 20, 424, 128);
		if(container.getTank()!=null) {
			this.blit(matrixStack,133,55, 20, 64, 384, 128);
			this.blit(matrixStack,98, 84, 34, 4, 444, 128);
			GuiHelper.handleGuiTank(matrixStack, container.getTank(), guiLeft + 135, guiTop + 57, 16, 60, 0, 0, 0, 0, x, y, TEXTURE, null);
			ClientUtils.bindTexture(TEXTURE);
		}
		

		//upgrade arrow
		this.blit(matrixStack, 85, 93, 6, 22, 412, 148);
		//generator symbol
		this.blit(matrixStack, 76, 44, 24, 48, 176+24*(tile.isWorking()?2:1), (container.getTier()-1)*48);
		
		//range gauge
		matrixStack.push();
		matrixStack.translate(guiLeft+35, guiTop+35, 0);//move to gauge center
		matrixStack.rotate(new Quaternion(new Vector3f(0,0,1),tile.getRangeLevel()/4f*271f,true));//rotate around Z
		AbstractGui.blit(matrixStack,-10,-10, 20,20, 276, 192, 20, 20, TEXW, TEXH);//draw with center offset
		matrixStack.pop();
		
		//temp gauge
		matrixStack.push();
		matrixStack.translate(guiLeft+88, guiTop+26, 0);
		matrixStack.rotate(new Quaternion(new Vector3f(0,0,1),(tile.getTemperatureLevel())/4f*271f,true));
		AbstractGui.blit(matrixStack,-14,-14, 28,28, 248, 192, 28, 28, TEXW, TEXH);
		matrixStack.pop();
		
		//overdrive gauge
		matrixStack.push();
		matrixStack.translate(guiLeft+141, guiTop+35, 0);
		matrixStack.rotate(new Quaternion(new Vector3f(0,0,1),0*271f,true));
		AbstractGui.blit(matrixStack,-10,-10, 20,20, 276, 192, 20, 20, TEXW, TEXH);
		matrixStack.pop();
	}
	private void drawCenterText(MatrixStack matrixStack,int x,int y,String s,int clr) {
		this.font.drawText(matrixStack,GuiUtils.str(s),x-this.font.getStringWidth(s)/2, y-4, clr);
	}
	protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
		//titles
	    this.font.drawText(matrixStack, this.title, this.titleX, this.titleY, 0xff404040);
	    //this.font.drawText(matrixStack, this.playerInventory.getDisplayName(), this.playerInventoryTitleX, this.playerInventoryTitleY+5, 0xff404040);
	    //temp level
	    drawCenterText(matrixStack,88,40, TemperatureDisplayHelper.toTemperatureDeltaInt( tile.getActualTemp())+"",0xffffffff);
	    //range level
	    drawCenterText(matrixStack,35,45, tile.getActualRange()+"",0xffffffff);
	    //overdrive level
	    drawCenterText(matrixStack,141,45, 0+"",0xffffffff);
	}
    @Override
    public void init() {
        super.init();
        this.buttons.clear();
        this.addButton(new MasterGeneratorGuiButtonBoolean(guiLeft + 5, guiTop + 24, 11, 22, tile.isWorking(), 472, 148,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setWorking(!btn.getState());
                    tag.putBoolean("isWorking", tile.isWorking());
                    FHNetwork.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        this.addButton(new MasterGeneratorGuiButtonBoolean(guiLeft + 160, guiTop + 24, 11, 22, tile.isOverdrive(),450, 148,
                btn -> {
                    CompoundNBT tag = new CompoundNBT();
                    tile.setOverdrive(!btn.getState());
                    tag.putBoolean("isOverdrive", tile.isOverdrive());
                    FHNetwork.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        this.addButton(new MasterGeneratorGuiButtonUpgrade(guiLeft + 75, guiTop + 116, 26, 18, 0,424, 148,
                btn -> {
                    /*CompoundNBT tag = new CompoundNBT();
                    tile.setOverdrive(!btn.getState());
                    tag.putBoolean("isOverdrive", tile.isOverdrive());
                    FHNetwork.sendToServer(new MessageTileSync(tile.master(), tag));*/
                    fullInit();
                }));

        
    }
    @Override
    public void render(MatrixStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<ITextComponent> tooltip = new ArrayList<>();
        if(container.getTank()!=null) {
        	
        	GuiHelper.handleGuiTank(transform, container.getTank(), guiLeft + 135, guiTop + 57, 16, 60, 384, 192, 16, 60, mouseX, mouseY, TEXTURE, tooltip);
        }
        if (isMouseIn(mouseX, mouseY, 5, 24, 11, 22)) {
            if (tile.isWorking()) {
                tooltip.add(GuiUtils.translateGui("generator.mode.off"));
            } else {
                tooltip.add(GuiUtils.translateGui("generator.mode.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 160, 24, 11, 22)) {
            if (tile.isOverdrive()) {
                tooltip.add(GuiUtils.translateGui("generator.overdrive.off"));
            } else {
                tooltip.add(GuiUtils.translateGui("generator.overdrive.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 63, 0, 50, 50)) {
            tooltip.add(GuiUtils.translateGui("generator.temperature.level").appendString(TemperatureDisplayHelper.toTemperatureDeltaIntString( tile.getActualTemp())));
        }

        if (isMouseIn(mouseX, mouseY, 18, 18, 32, 32)) {
            tooltip.add(GuiUtils.translateGui("generator.range.level").appendString(Integer.toString(tile.getActualRange())));
        }

        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }

}
