package com.teammoeg.frostedheart.content.tips.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teammoeg.frostedheart.content.tips.TipRenderer;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class TipEditorScreen extends Screen {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private List<Component> contents = new ArrayList<>();
    private String id;
    private String category;
    private String next;
    private ResourceLocation image;
    private boolean alwaysVisible;
    private boolean onceOnly;
    private boolean hidden;
    private boolean pinned;
    private boolean temporary;
    private int displayTime = 30000;
    private int fontColor = FHColorHelper.CYAN;
    private int BGColor = FHColorHelper.BLACK;

    protected TipEditorScreen() {
        super(Component.translatable("gui.frostedheart.tip_editor.title"));
    }

    @Override
    protected void init() {
        addRenderableWidget(new IconButton(50, 50, IconButton.Icon.CHECK, FHColorHelper.CYAN, Component.translatable("gui.yes"), (b) -> {
            if (!TipRenderer.TIP_QUEUE.isEmpty()) {
                TipRenderer.TIP_QUEUE.get(0).saveAsFile();
            }
        }));
    }
}
