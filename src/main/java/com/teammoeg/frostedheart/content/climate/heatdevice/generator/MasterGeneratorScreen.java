package com.teammoeg.frostedheart.content.climate.heatdevice.generator;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.AtlasUV;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.RotatableUV;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonBoolean;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonState;
import blusunrize.immersiveengineering.client.utils.GuiHelper;
import blusunrize.immersiveengineering.common.network.MessageTileSync;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;

import blusunrize.immersiveengineering.client.gui.elements.GuiButtonIE.IIEPressable;

public class MasterGeneratorScreen<T extends MasterGeneratorTileEntity<T>> extends IEContainerScreen<MasterGeneratorContainer<T>> {
	T tile;
	public static final int TEXW=512;
	public static final int TEXH=256;
	private static final ResourceLocation TEXTURE = TranslateUtils.makeTextureLocation("general_generator");
	public static class MasterGeneratorGuiButtonBoolean extends GuiButtonBoolean{

		public MasterGeneratorGuiButtonBoolean(int x, int y, int w, int h, boolean state,
				int u, int v, IIEPressable<GuiButtonState<Boolean>> handler) {
			super(x, y, w, h, null, state, TEXTURE, u, v, 0, handler);
		}
		
		public void blit(PoseStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
		      blit(matrixStack, x, y,getBlitOffset(), uOffset, vOffset, uWidth, vHeight, TEXH, TEXW);
		   }
	}
	public static class MasterGeneratorGuiButtonUpgrade extends GuiButtonState<Integer>{

		public MasterGeneratorGuiButtonUpgrade(int x, int y, int w, int h,
				int initialState,  int u, int v,
				IIEPressable<GuiButtonState<Integer>> handler) {
			super(x, y, w, h, Component.empty(), new Integer[] {0,1,2,3}, initialState, TEXTURE, u, v, 1, handler);
		}

		public void blit(PoseStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
		      blit(matrixStack, x, y,getBlitOffset(), uOffset, vOffset, uWidth, vHeight, TEXH, TEXW);
		   }
	}
	
	private static final AtlasUV rangeicons=new AtlasUV(TEXTURE, 256, 0, 128, 64, 2, 5, TEXW, TEXH);
	private static final Point rangePoint=new Point(24,61);
	private static final RotatableUV minorPointer=new RotatableUV(TEXTURE, 276, 192, 20, 20, 10, 10, TEXW, TEXH);
	private static final RotatableUV majorPointer=new RotatableUV(TEXTURE, 248, 192, 28, 28, 14, 14, TEXW, TEXH);
	private static final Point tempGauge=new Point(74, 12);
	private static final Point rangeGauge=new Point(25, 25);
	private static final Point overGauge=new Point(131, 25);
	private static final AtlasUV generatorSymbol=new AtlasUV(TEXTURE, 176, 0, 24, 48, 3, 12, TEXW, TEXH);
	private static final Point generatorPos=new Point(76, 44);
	MasterGeneratorGuiButtonUpgrade upgrade;
	public MasterGeneratorScreen(MasterGeneratorContainer<T> inventorySlotsIn, Inventory inv, Component title) {
		super(inventorySlotsIn, inv, title);
		tile=inventorySlotsIn.tile;
		this.imageHeight=222;
		
	}
	public void blit(PoseStack matrixStack, int x, int y,int w,int h, int u, int v, int uWidth, int vHeight) {
		GuiComponent.blit(matrixStack,leftPos + x,topPos+y, w, h, u, v, uWidth, vHeight, TEXW, TEXH);
	}
	public void blit(PoseStack matrixStack, int x, int y,int w,int h, int u, int v) {
		blit(matrixStack,x,y, w, h, u, v, w, h);
	}
	public MasterGeneratorContainer<T> getMenu() {
		return menu;
	}
	@Override
	protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
		ClientUtils.bindTexture(TEXTURE);
		//background
		this.blit(matrixStack, 0, 0, this.imageWidth, this.imageHeight, 0, 0);

		//System.out.println(ininvarrx+","+ininvarry+"-"+inarryl);
		//range circle
		int actualRangeLvl=(int) (tile.getRangeLevel()+0.05);
		rangeicons.blitAtlas(matrixStack, leftPos, topPos, rangePoint, actualRangeLvl);
		
		//fuel slots
		Point in=menu.getSlotIn();
		Point out=menu.getSlotOut();


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
		if(menu.getTank()!=null) {
			this.blit(matrixStack,133,55, 20, 64, 384, 128);
			this.blit(matrixStack,98, 84, 34, 4, 444, 128);
			GuiHelper.handleGuiTank(matrixStack, menu.getTank(), leftPos + 135, topPos + 57, 16, 60, 0, 0, 0, 0, x, y, TEXTURE, null);
			ClientUtils.bindTexture(TEXTURE);
		}
		

		//upgrade arrow
		this.blit(matrixStack, 85, 93, 6, 22, 412, 148);

		//generator symbol
		generatorSymbol.blitAtlas(matrixStack, leftPos, topPos, generatorPos,((tile.isWorking()&&tile.guiData.get(MasterGeneratorTileEntity.PROCESS)>0)?2:1),(menu.getTier()-1));
		
		
		//range gauge
		minorPointer.blitRotated(matrixStack, leftPos, topPos, rangeGauge, tile.getRangeLevel()/4f*271f);
		//temp gauge
		majorPointer.blitRotated(matrixStack, leftPos, topPos, tempGauge, (tile.getTemperatureLevel())/4f*271f);
		//overdrive gauge
		minorPointer.blitRotated(matrixStack, leftPos, topPos, overGauge, menu.data.get(MasterGeneratorTileEntity.OVERDRIVE)/1000f*271f);
	}
	private void drawCenterText(PoseStack matrixStack,int x,int y,String s,int clr) {
		this.font.draw(matrixStack,TranslateUtils.str(s),x- (float) this.font.width(s) /2, y-4, clr);
	}
	protected void renderLabels(PoseStack matrixStack, int x, int y) {
		//titles
	    this.font.draw(matrixStack, this.title, this.titleLabelX, this.titleLabelY, 0xff404040);
	    //this.font.drawText(matrixStack, this.playerInventory.getDisplayName(), this.playerInventoryTitleX, this.playerInventoryTitleY+5, 0xff404040);
	    //temp level
	    drawCenterText(matrixStack,88,40, TemperatureDisplayHelper.toTemperatureDeltaInt( tile.getActualTemp())+"",0xffffffff);
	    //range level
	    drawCenterText(matrixStack,35,45, tile.getActualRange()+"",0xffffffff);
	    //overdrive level
	    drawCenterText(matrixStack,141,45,  menu.data.get(MasterGeneratorTileEntity.OVERDRIVE)/10+"",0xffffffff);
	}
	boolean validStructure;
	
    @Override
    public void init() {
        super.init();
        this.buttons.clear();
        validStructure=tile.isValidStructure();
        this.addButton(new MasterGeneratorGuiButtonBoolean(leftPos + 5, topPos + 24, 11, 22, tile.isWorking(), 472, 148,
                btn -> {
                    CompoundTag tag = new CompoundTag();
                    tile.setWorking(!btn.getState());
                    tag.putBoolean("isWorking", tile.isWorking());
                    ImmersiveEngineering.packetHandler.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        this.addButton(new MasterGeneratorGuiButtonBoolean(leftPos + 160, topPos + 24, 11, 22, tile.isOverdrive(),450, 148,
                btn -> {
                    CompoundTag tag = new CompoundTag();
                    tile.setOverdrive(!btn.getState());
                    tag.putBoolean("isOverdrive", tile.isOverdrive());
                    ImmersiveEngineering.packetHandler.sendToServer(new MessageTileSync(tile.master(), tag));
                    fullInit();
                }));
        int level=1;
        Player player=ClientUtils.mc().player;
        if(tile.isBroken) {
        	if(FHUtils.hasItems(player, tile.getRepairCost())) 
        		level=2;
        	else
        		level=3;
        } else {
        	 if(validStructure&&ResearchListeners.hasMultiblock(null, tile.getNextLevelMultiblock())&&FHUtils.hasItems(player, tile.getUpgradeCost())) {
        		 level=0;
        	 }
        }
        this.addButton(upgrade=new MasterGeneratorGuiButtonUpgrade(leftPos + 75, topPos + 116, 26, 18, level,424, 148,
                btn -> {
                	
                	FHNetwork.sendToServer(new GeneratorModifyPacket());
                    fullInit();
                }));

        
    }
    @Override
    public void render(PoseStack transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);
        List<Component> tooltip = new ArrayList<>();
        if(menu.getTank()!=null) {
        	
        	GuiHelper.handleGuiTank(transform, menu.getTank(), leftPos + 135, topPos + 57, 16, 60, 384, 192, 16, 60, mouseX, mouseY, TEXTURE, tooltip);
        }
        if (isMouseIn(mouseX, mouseY, 5, 24, 11, 22)) {
            if (tile.isWorking()) {
                tooltip.add(TranslateUtils.translateGui("generator.mode.off"));
            } else {
                tooltip.add(TranslateUtils.translateGui("generator.mode.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 160, 24, 11, 22)) {
            if (tile.isOverdrive()) {
                tooltip.add(TranslateUtils.translateGui("generator.overdrive.off"));
            } else {
                tooltip.add(TranslateUtils.translateGui("generator.overdrive.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 63, 0, 50, 50)) {
            tooltip.add(TranslateUtils.translateGui("generator.temperature.level").append(TemperatureDisplayHelper.toTemperatureDeltaIntString( tile.getActualTemp())));
        }

        if (isMouseIn(mouseX, mouseY, 18, 18, 32, 32)) {
            tooltip.add(TranslateUtils.translateGui("generator.range.level").append(Integer.toString(tile.getActualRange())));
        }
        if (isMouseIn(mouseX, mouseY, 124, 18, 32, 32)) {
            tooltip.add(TranslateUtils.translateGui("generator.over.level",menu.data.get(MasterGeneratorTileEntity.OVERDRIVE)/10f));
        }
        if (isMouseIn(mouseX, mouseY, 75, 116, 26, 18)) {
        	Optional<GeneratorData> generatorData=tile.getDataNoCheck();
        	if(tile.getNextLevelMultiblock()!=null&&!tile.isBroken) {
        		upgrade.setStateByInt(1);
        		if(!validStructure) {
        			Vec3i v3i=tile.getNextLevelMultiblock().getSize(ClientUtils.mc().level);
        			tooltip.add(TranslateUtils.translateGui("generator.no_enough_space",v3i.getX(),v3i.getY(),v3i.getZ()));
        		}else if(!ResearchListeners.hasMultiblock(null, tile.getNextLevelMultiblock())) {
        			tooltip.add(TranslateUtils.translateGui("generator.incomplete_research"));
        		} else {
        			tooltip.add(TranslateUtils.translateGui("generator.upgrade_material"));
        			BitSet bs=FHUtils.checkItemList(ClientUtils.mc().player, tile.getUpgradeCost());
        			int i=0;
        			boolean isOk=true;;
        			for(IngredientWithSize iws:tile.getUpgradeCost()) {
        				ItemStack[] iss=iws.getMatchingStacks();
        				MutableComponent iftc=TranslateUtils.str(iws.getCount()+"x ").append(iss[(int) ((new Date().getTime()/1000)%iss.length)].getHoverName());
        				if(bs.get(i))
        					iftc=iftc.withStyle(ChatFormatting.GREEN);
        				else
        					iftc=iftc.withStyle(ChatFormatting.RED);
        				isOk&=bs.get(i);
        				i++;
        				tooltip.add(iftc);
        			}
        			upgrade.setStateByInt(isOk?0:1);
        		}
        	}else if(tile.isBroken) {
    			tooltip.add(TranslateUtils.translateGui("generator.repair_material"));
    			BitSet bs=FHUtils.checkItemList(ClientUtils.mc().player, tile.getRepairCost());
    			int i=0;
    			boolean isOk=true;;
    			for(IngredientWithSize iws:tile.getRepairCost()) {
    				ItemStack[] iss=iws.getMatchingStacks();
    				MutableComponent iftc=TranslateUtils.str(iws.getCount()+"x ").append(iss[(int) ((new Date().getTime()/1000)%iss.length)].getHoverName());
    				if(bs.get(i))
    					iftc=iftc.withStyle(ChatFormatting.GREEN);
    				else
    					iftc=iftc.withStyle(ChatFormatting.RED);
    				isOk&=bs.get(i);
    				i++;
    				tooltip.add(iftc);
    			}
    			upgrade.setStateByInt(isOk?2:3);
        		
        	}
        }
        if (!tooltip.isEmpty()) {
            net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(transform, tooltip, mouseX, mouseY, width, height, -1, font);
        }
    }

}
