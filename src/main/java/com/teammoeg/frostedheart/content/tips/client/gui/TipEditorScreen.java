package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.base.client.gui.widget.ActionStateIconButton;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.base.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipEditsList;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipRequestPacket;
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

    public TipEditorScreen() {
        super(Lang.str("Tip Editor"));
        this.list = new TipEditsList(ClientUtils.mc(), ClientUtils.screenWidth(), ClientUtils.screenHeight(), 10, ClientUtils.screenHeight() - 30, 28);
        list.setRenderBackground(false);
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
    }

    @Override
    protected void init() {
        super.init();
        addWidget(list);
        // 保存按钮
        addRenderableWidget(new ActionStateIconButton(ClientUtils.screenWidth()/2 - 30, ClientUtils.screenHeight()-25, IconButton.Icon.FOLDER, FHColorHelper.CYAN, 2, Component.translatable("gui.frostedheart.save_as_file"), Component.translatable("gui.frostedheart.saved"), (b) -> {
            var json = list.getJson();
            if (json != null) {
                Tip.builder("").fromJson(json).build().saveAsFile();
            }
        }));
        // 发送按钮
        ActionStateIconButton sendButton = new ActionStateIconButton(ClientUtils.screenWidth()/2 + 5, ClientUtils.screenHeight()-25, IconButton.Icon.GIVE, FHColorHelper.CYAN, 2, Component.translatable("gui.frostedheart.tip_editor.send"), Component.translatable("gui.frostedheart.sent"), (b) -> {
            var json = list.getJson();
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
