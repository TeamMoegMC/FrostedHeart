package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.compat.ie.IngredientUtils;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.compat.ie.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.client.AtlasUV;
import com.teammoeg.frostedheart.util.client.Point;
import com.teammoeg.frostedheart.util.client.RotatableUV;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonBoolean;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonState;
import blusunrize.immersiveengineering.client.gui.info.FluidInfoArea;
import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GeneratorScreen<R extends GeneratorState, T extends GeneratorLogic<T, R>> extends IEContainerScreen<GeneratorContainer<R, T>> {
    public static final int TEXW = 512;
    public static final int TEXH = 256;
    private static final ResourceLocation TEXTURE = Lang.makeTextureLocation("general_generator");
    private static final AtlasUV rangeicons = new AtlasUV(TEXTURE, 256, 0, 128, 64, 2, 5, TEXW, TEXH);
    private static final Point rangePoint = new Point(24, 61);
    private static final RotatableUV minorPointer = new RotatableUV(TEXTURE, 276, 192, 20, 20, 10, 10, TEXW, TEXH);
    private static final RotatableUV majorPointer = new RotatableUV(TEXTURE, 248, 192, 28, 28, 14, 14, TEXW, TEXH);
    private static final Point tempGauge = new Point(74, 12);
    private static final Point rangeGauge = new Point(25, 25);
    private static final Point overGauge = new Point(131, 25);
    private static final AtlasUV generatorSymbol = new AtlasUV(TEXTURE, 176, 0, 24, 48, 3, 12, TEXW, TEXH);
    private static final Point generatorPos = new Point(76, 44);
    MasterGeneratorGuiButtonUpgrade upgrade;
    GeneratorLogic<T, R> tile;
    int level;
    boolean validStructure;
    boolean hasResearch;
    List<Component> costStr = new ArrayList<>();;

    public GeneratorScreen(GeneratorContainer<R, T> inventorySlotsIn, Inventory inv, Component title) {
        super(inventorySlotsIn, inv, title, TEXTURE);
        this.imageHeight = 222;

    }

    public GeneratorContainer<R, T> getMenu() {
        return menu;
    }

    @Override
    protected List<InfoArea> makeInfoAreas() {
        if (menu.getTank() == null)
            return super.makeInfoAreas();
        return ImmutableList.of(new FluidInfoArea(menu.getTank(), new Rect2i(135, 27, 16, 60), 0, 0, 0, 0, TEXTURE));
    }
    @Override
	protected void drawBackgroundTexture(GuiGraphics graphics)
	{
		graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXW, TEXH);
	}
    @Override
    protected void drawContainerBackgroundPre(GuiGraphics matrixStack, float partialTicks, int x, int y) {
        // background
        //matrixStack.blit(TEXTURE, 0, 0, 0, 0, this.imageWidth, this.imageHeight,TEXW,TEXH);

        // System.out.println(ininvarrx+","+ininvarry+"-"+inarryl);
        // range circle
        int actualRangeLvl = (int) (menu.rangeLevel.getValue() + 0.05);
        rangeicons.blitAtlas(matrixStack, leftPos, topPos, rangePoint, actualRangeLvl);

        // fuel slots
        Point in = menu.getSlotIn();
        Point out = menu.getSlotOut();

        int ininvarry = in.getY() + 6;
        int outinvarry = out.getY() + 6;
        int ininvarrx = in.getX() + 18;
        int outinvarrx = 98;
        int inarryl = 76 - ininvarrx;
        int outarryl = out.getX() - 2 - outinvarrx;
        // arrows
        matrixStack.blit(TEXTURE,leftPos+ ininvarrx,topPos+ ininvarry, 511 - inarryl, 132, inarryl, 4,TEXW,TEXH);
        matrixStack.blit(TEXTURE,leftPos+ outinvarrx,topPos+  outinvarry, 511 - outarryl, 132, outarryl, 4,TEXW,TEXH);
        // slot background
        matrixStack.blit(TEXTURE,leftPos+ in.getX() - 2,topPos+  in.getY() - 2, 404, 128, 20, 20,TEXW,TEXH);
        matrixStack.blit(TEXTURE,leftPos+ out.getX() - 2,topPos+  out.getY() - 2, 424, 128, 20, 20,TEXW,TEXH);
        if (menu.getTank() != null) {
            matrixStack.blit(TEXTURE,leftPos+ 133,topPos+  55,384, 128, 20, 64,TEXW,TEXH);
            matrixStack.blit(TEXTURE,leftPos+ 98,topPos+  84, 444, 128, 34, 4,TEXW,TEXH);
        }

        // upgrade arrow
        matrixStack.blit(TEXTURE,leftPos+ 85,topPos+  93, 412, 148, 6, 22,TEXW,TEXH);

        // generator symbol
        generatorSymbol.blitAtlas(matrixStack, leftPos, topPos, generatorPos, (menu.isWorking.getValue() && menu.process.getValue() > 0) ? 2 : 1, (menu.getTier() - 1));

        // range gauge
        minorPointer.blitRotated(matrixStack, leftPos, topPos, rangeGauge, menu.rangeLevel.getValue() / 4f * 271f);
        // temp gauge
        majorPointer.blitRotated(matrixStack, leftPos, topPos, tempGauge, menu.tempLevel.getValue() / 4f * 271f);
        // overdrive gauge
        minorPointer.blitRotated(matrixStack, leftPos, topPos, overGauge, menu.overdrive.getValue() * 271f);
    }

    protected void renderLabels(GuiGraphics matrixStack, int x, int y) {
        // titles
        matrixStack.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xff404040);
        // this.font.drawText(matrixStack, this.playerInventory.getDisplayName(),
        // this.playerInventoryTitleX, this.playerInventoryTitleY+5, 0xff404040);
        // temp level
        matrixStack.drawCenteredString(this.font, TemperatureDisplayHelper.toTemperatureDeltaInt(menu.tempDegree.getValue()) + "", 88, 38, 0xffffffff);
        // range level
        matrixStack.drawCenteredString(this.font, menu.rangeBlock.getValue() + "", 35, 41, 0xffffffff);
        // overdrive level
        matrixStack.drawCenteredString(this.font, menu.overdrive.getValue() * 100 + "", 141, 41, 0xffffffff);
    }

    @Override
    public void init() {
        super.init();
        IMultiblockBEHelper<R> helper = (IMultiblockBEHelper<R>) FHMultiblockHelper.getBEHelper(Minecraft.getInstance().level, menu.pos.getValue()).get();
        tile = (GeneratorLogic<T, R>) helper.getMultiblock().logic();
        this.addRenderableWidget(new MasterGeneratorGuiButtonBoolean(leftPos + 5, topPos + 24, 11, 22, menu.isWorking.asSupplier(), 472, 148,
                btn -> {
                    menu.sendMessage(1, btn.getNextState());
                }));
        this.addRenderableWidget(new MasterGeneratorGuiButtonBoolean(leftPos + 160, topPos + 24, 11, 22, menu.isOverdrive.asSupplier(), 450, 148,
                btn -> {
                    menu.sendMessage(2, btn.getNextState());
                }));
        level = 1;
        Player player = ClientUtils.mc().player;
        costStr.clear();
        if (menu.isBroken.getValue()) {
            costStr.add(Lang.translateGui("generator.repair_material"));
            BitSet cost = IngredientUtils.checkItemList(ClientUtils.mc().player, tile.getRepairCost());
            int i = 0;
            for (IngredientWithSize iws : tile.getRepairCost()) {
                ItemStack[] iss = iws.getMatchingStacks();
                MutableComponent iftc = Lang.str(iws.getCount() + "x ").append(iss[(int) ((new Date().getTime() / 1000) % iss.length)].getHoverName());
                if (cost.get(i))
                    iftc = iftc.withStyle(ChatFormatting.GREEN);
                else
                    iftc = iftc.withStyle(ChatFormatting.RED);
                i++;
                costStr.add(iftc);
            }
            if (cost.cardinality() == cost.length())
                level = 2;
            else
                level = 3;
        } else if (tile.getNextLevelMultiblock() != null) {
            validStructure = tile.nextLevelHasValidStructure(Minecraft.getInstance().level, helper);
            List<IngredientWithSize> upgcost = tile.getUpgradeCost(Minecraft.getInstance().level, helper);
            BitSet cost = IngredientUtils.checkItemList(ClientUtils.mc().player, upgcost);
            hasResearch = ResearchListeners.hasMultiblock(null, tile.getNextLevelMultiblock());
            Vec3i v3i = tile.getNextLevelMultiblock().getSize(Minecraft.getInstance().level);
            if (!validStructure) {
                costStr.add(Lang.translateGui("generator.no_enough_space", v3i.getX(), v3i.getY(), v3i.getZ()));
            } else if (!hasResearch) {
                costStr.add(Lang.translateGui("generator.incomplete_research"));
            } else {
                costStr.add(Lang.translateGui("generator.upgrade_material"));
                int i = 0;
                for (IngredientWithSize iws : upgcost) {
                    ItemStack[] iss = iws.getMatchingStacks();
                    MutableComponent iftc = Lang.str(iws.getCount() + "x ").append(iss[(int) ((new Date().getTime() / 1000) % iss.length)].getHoverName());
                    if (cost.get(i))
                        iftc = iftc.withStyle(ChatFormatting.GREEN);
                    else
                        iftc = iftc.withStyle(ChatFormatting.RED);
                    i++;
                    costStr.add(iftc);
                }
                if (cost.cardinality() == cost.length()) {
                    level = 0;
                }
            }

        }
        this.addRenderableWidget(upgrade = new MasterGeneratorGuiButtonUpgrade(leftPos + 75, topPos + 116, 26, 18, () -> level, 424, 148,
                btn -> {
                    FHNetwork.sendToServer(new GeneratorModifyPacket());
                }));

    }

    @Override
    protected void gatherAdditionalTooltips(int mouseX, int mouseY, Consumer<Component> addLine, Consumer<Component> addGray) {
        super.gatherAdditionalTooltips(mouseX, mouseY, addLine, addGray);
        if (isMouseIn(mouseX, mouseY, 5, 24, 11, 22)) {
            if (menu.isWorking.getValue()) {
                addLine.accept(Lang.translateGui("generator.mode.off"));
            } else {
                addLine.accept(Lang.translateGui("generator.mode.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 160, 24, 11, 22)) {
            if (menu.isOverdrive.getValue()) {
                addLine.accept(Lang.translateGui("generator.overdrive.off"));
            } else {
                addLine.accept(Lang.translateGui("generator.overdrive.on"));
            }
        }

        if (isMouseIn(mouseX, mouseY, 63, 0, 50, 50)) {
            addLine.accept(Lang.translateGui("generator.temperature.level").append(TemperatureDisplayHelper.toTemperatureDeltaIntString(menu.tempDegree.getValue())));
        }

        if (isMouseIn(mouseX, mouseY, 18, 18, 32, 32)) {
            addLine.accept(Lang.translateGui("generator.range.level").append(Integer.toString(menu.rangeBlock.getValue())));
        }
        if (isMouseIn(mouseX, mouseY, 124, 18, 32, 32)) {
            addLine.accept(Lang.translateGui("generator.over.level", menu.overdrive.getValue() * 100));
        }
        if (isMouseIn(mouseX, mouseY, 75, 116, 26, 18)) {
            costStr.forEach(addLine);
        }
    }

    @Override
    public void render(GuiGraphics transform, int mouseX, int mouseY, float partial) {
        super.render(transform, mouseX, mouseY, partial);


    }

    public static class MasterGeneratorGuiButtonBoolean extends GuiButtonBoolean {

        public MasterGeneratorGuiButtonBoolean(int x, int y, int w, int h, Supplier<Boolean> state,
                                               int u, int v, IIEPressable<GuiButtonState<Boolean>> handler) {
            super(x, y, w, h, null, state, TEXTURE, u, v, 0, handler);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            if (this.visible) {
                Font fontrenderer = mc.font;
                this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(770, 771, 1, 0);
                RenderSystem.blendFunc(770, 771);
                int u = texU + (offsetDir == 0 ? width : offsetDir == 2 ? -width : 0) * getStateAsInt();
                int v = texV + (offsetDir == 1 ? height : offsetDir == 3 ? -height : 0) * getStateAsInt();
                graphics.blit(texture, getX(), getY(), u, v, width, height, TEXW, TEXH);
                if (!getMessage().getString().isEmpty()) {
                    int txtCol = 0xE0E0E0;
                    if (!this.active)
                        txtCol = 0xA0A0A0;
                    else if (this.isHovered)
                        txtCol = Lib.COLOUR_I_ImmersiveOrange;
                    int[] offset = getTextOffset(fontrenderer);
                    graphics.drawString(fontrenderer, getMessage(), getX() + offset[0], getY() + offset[1], txtCol, false);
                }
            }
        }
    }

    public static class MasterGeneratorGuiButtonUpgrade extends GuiButtonState<Integer> {

        public MasterGeneratorGuiButtonUpgrade(int x, int y, int w, int h,
                                               IntSupplier initialState, int u, int v,
                                               IIEPressable<GuiButtonState<Integer>> handler) {
            super(x, y, w, h, Component.empty(), new Integer[]{0, 1, 2, 3}, initialState, TEXTURE, u, v, 1, handler);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            if (this.visible) {
                Font fontrenderer = mc.font;
                this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(770, 771, 1, 0);
                RenderSystem.blendFunc(770, 771);
                int u = texU + (offsetDir == 0 ? width : offsetDir == 2 ? -width : 0) * getStateAsInt();
                int v = texV + (offsetDir == 1 ? height : offsetDir == 3 ? -height : 0) * getStateAsInt();
                graphics.blit(texture, getX(), getY(), u, v, width, height, TEXW, TEXH);
                if (!getMessage().getString().isEmpty()) {
                    int txtCol = 0xE0E0E0;
                    if (!this.active)
                        txtCol = 0xA0A0A0;
                    else if (this.isHovered)
                        txtCol = Lib.COLOUR_I_ImmersiveOrange;
                    int[] offset = getTextOffset(fontrenderer);
                    graphics.drawString(fontrenderer, getMessage(), getX() + offset[0], getY() + offset[1], txtCol, false);
                }
            }
        }
    }

}
