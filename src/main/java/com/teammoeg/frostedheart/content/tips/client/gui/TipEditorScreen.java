package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.chorda.widget.ActionStateIconButton;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.chorda.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.TipManager;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipEditsList;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipRequestPacket;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.chorda.util.client.ColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TipEditorScreen extends Screen {
    private final TipEditsList list;

    public TipEditorScreen() {
        super(Components.str("Tip Editor"));
        this.list = new TipEditsList(ClientUtils.mc(), ClientUtils.screenWidth() + 172, ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30, 28);
        list.setRenderBackground(false);
        list.setLeftPos(-172);
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
        list.updateSize(ClientUtils.screenWidth() + 172, ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30);
        list.setLeftPos(-172);
    }

    @Override
    protected void init() {
        super.init();
        addWidget(list);
        // 保存按钮
        addRenderableWidget(new ActionStateIconButton(ClientUtils.screenWidth()/2 - 30, ClientUtils.screenHeight()-25, IconButton.Icon.FOLDER, ColorHelper.CYAN, 2, Component.translatable("gui.frostedheart.save_as_file"), Component.translatable("gui.frostedheart.saved"), (b) -> {
            var json = list.toJson();
            if (json != null) {
                Tip.builder("").fromJson(json).build().saveAsFile();
                TipManager.INSTANCE.loadFromFile();
            }
        }));
        // 发送按钮
        ActionStateIconButton sendButton = new ActionStateIconButton(ClientUtils.screenWidth()/2 + 5, ClientUtils.screenHeight()-25, IconButton.Icon.GIVE, ColorHelper.CYAN, 2, Component.translatable("gui.frostedheart.tip_editor.send"), Component.translatable("gui.frostedheart.sent"), (b) -> {
            var json = list.toJson();
            if (json != null) {
                Tip tip = Tip.builder("").fromJson(json).build();
                FHNetwork.sendToServer(new DisplayCustomTipRequestPacket(tip));
            }
        });
        if (!ClientUtils.getPlayer().hasPermissions(2)) {
            sendButton.active = false;
            sendButton.setMessage(Component.translatable("gui.frostedheart.tip_editor.no_permission"));
        }
        addRenderableWidget(sendButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
