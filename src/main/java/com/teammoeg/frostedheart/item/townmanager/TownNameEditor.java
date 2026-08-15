/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.Verifier;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import com.teammoeg.frostedheart.content.town.network.TownNameEditRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/** Inline town-title editor. Enter or clicking elsewhere commits; Escape cancels. */
final class TownNameEditor extends TextBox {
    private final Supplier<TeamTown> townSource;
    private boolean focusedLastFrame;
    private boolean cancelPending;
    @Nullable
    private String pendingName;

    TownNameEditor(UILayer parent, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(TownManagerScreen.CONTENT_X + 3, TownManagerScreen.CONTENT_Y + 1);
        setSize(TownManagerScreen.CONTENT_WIDTH - 14, 13);
        setMaxLength(TownNamingModel.MAX_TOWN_NAME_LENGTH);
        setFilter(Verifier.successOrComponent(
                value -> TownNamingModel.normalizeTownName(value).isPresent(),
                () -> Component.translatable("gui.frostedheart.town_manager.name_required")));
        textColor = Coloring.argb(0xFFFFAA00);
        errorColor = Coloring.argb(0xFFFF5555);
    }

    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && isFocused()) cancelPending = true;
        return super.onKeyPressed(keyCode, scanCode, modifier);
    }

    @Override
    public void onTextChanged() {
        // TextBox also invokes this callback while moving the cursor and from
        // setText(..., false). Names are committed on Enter or focus loss.
    }

    @Override
    public void onEnterPressed() {
        commit();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        boolean focused = isFocused();
        if (!focused && focusedLastFrame) {
            if (cancelPending) syncAuthoritativeName();
            else commit();
            cancelPending = false;
        } else if (!focused) {
            syncAuthoritativeName();
        }
        focusedLastFrame = focused;
        super.render(graphics, x, y, width, height, hint);
    }

    @Override
    public void drawTextBox(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        if (isFocused()) {
            graphics.fill(x, y, x + width, y + 1, 0xFFFFAA00);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFAA00);
        }
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        super.getTooltip(tooltip);
        tooltip.accept(Component.translatable("gui.frostedheart.town_manager.edit_town_name_hint"));
    }

    private void commit() {
        TownNamingModel.normalizeTownName(getText()).ifPresent(name -> {
            setEditorText(name);
            TeamTown town = townSource.get();
            if (name.equals(pendingName)) return;
            if (pendingName == null && town != null && name.equals(town.getName())) return;
            pendingName = name;
            FHNetwork.INSTANCE.sendToServer(new TownNameEditRequestPacket(name));
        });
    }

    private void syncAuthoritativeName() {
        TeamTown town = townSource.get();
        if (town != null) {
            if (pendingName != null) {
                if (!pendingName.equals(town.getName())) return;
                pendingName = null;
            }
            setEditorText(town.getName());
        }
    }

    private void setEditorText(String text) {
        if (text.equals(getText())) return;
        setText(text, false);
    }
}
