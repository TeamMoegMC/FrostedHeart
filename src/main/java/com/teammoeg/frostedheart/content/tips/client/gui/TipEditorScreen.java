package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.base.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipEditsList;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TipEditorScreen extends Screen {
    private final TipEditsList list;
    private final IconButton saveButton;

    public TipEditorScreen() {
        super(Lang.str("Tip Editor"));
        this.list = new TipEditsList(ClientUtils.mc(), ClientUtils.screenWidth(), ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30, 28);
        list.setRenderBackground(false);
        this.saveButton = new IconButton(ClientUtils.screenWidth()/2 - 10, ClientUtils.screenHeight()-25, IconButton.Icon.FOLDER, FHColorHelper.CYAN, 2, Component.literal("Save"), (b) -> {
            var json = list.getJson();
            if (json != null) {
                Tip.builder("").fromJson(json).build().saveAsFile();
            }
        });
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);
        list.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void resize(@NotNull Minecraft pMinecraft, int pWidth, int pHeight) {
        super.resize(pMinecraft, pWidth, pHeight);
        list.updateSize(ClientUtils.screenWidth(), ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30);
        saveButton.setPosition(ClientUtils.screenWidth()/2 - 10, ClientUtils.screenHeight()-25);
    }

    @Override
    protected void init() {
        super.init();
        addWidget(list);
        addRenderableWidget(saveButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
