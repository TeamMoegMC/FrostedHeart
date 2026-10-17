/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation;
import com.teammoeg.frostedresearch.knowledge.client.ui.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

/**
 * A field-note sheet using the same Chorda CUI and materials as the journal.
 */
public final class ObservationRecordScreen extends CUIScreenWrapper {
    public ObservationRecordScreen(Observation observation) {
        super(new NoteLayer(observation));
    }

    public void rejected(String reason) {
        ((NoteLayer) getPrimaryLayer()).rejected(reason);
    }

    @Override
    public void onClose() {
        ((NoteLayer) getPrimaryLayer()).discard();
    }

    private static final class NoteLayer extends PrimaryLayer {
        private final Observation observation;
        private final net.minecraft.world.item.ItemStack icon;
        private final Component title;
        private final JournalText text;
        private JournalButton keep;
        private Component status = Component.empty();
        private boolean expanded, waiting;
        private FormattedCharSequence heading;

        NoteLayer(Observation observation) {
            this.observation = observation;
            title = ObservationPresentation.title(observation);
            icon = KnowledgePresentation.icon(KnowledgeElement.observation(observation));
            text = new JournalText(this);
            setTheme(INSTANCE);
        }

        @Override
        public boolean onInit() {
            var window = Minecraft.getInstance().getWindow();
            setSize(Math.min(342, window.getGuiScaledWidth() - 24), Math.min(246, window.getGuiScaledHeight() - 24));
            return super.onInit();
        }

        @Override
        public void addChildUIElements() {
            int w = getWidth(), h = getHeight();
            heading = net.minecraft.locale.Language.getInstance().getVisualOrder(getFont().substrByWidth(title, w - 76));
            text.setPosAndSize(23, 71, w - 46, h - 113);
            List<Component> lines = new ArrayList<>(ObservationPresentation.summary(observation));
            if (expanded) lines.addAll(ObservationPresentation.details(observation));
            text.text(lines);
            add(text);
            JournalButton detail = new JournalButton(this, t(expanded ? "details.hide" : "details.show"), true, () -> {
                expanded = !expanded;
                refresh();
            });
            detail.setPosAndSize(21, 50, w - 42, 15);
            add(detail);
            keep = new JournalButton(this, t("observation.keep"), false, () -> {
                waiting = true;
                keep.setEnabled(false);
                FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.ACCEPT));
            });
            keep.setPosAndSize(22, h - 31, (w - 50) / 2, 20);
            keep.setEnabled(!waiting);
            add(keep);
            JournalButton discard = new JournalButton(this, t("observation.discard"), true, this::discard);
            discard.setPosAndSize(w / 2 + 3, h - 31, (w - 50) / 2, 20);
            add(discard);
        }

        @Override
        public void alignWidgets() {
        }

        void rejected(String reason) {
            waiting = false;
            status = KnowledgePresentation.status(reason);
            keep.setEnabled(true);
        }

        void discard() {
            FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.DISCARD));
            Minecraft.getInstance().setScreen(null);
        }

        @Override
        public void back() {
            discard();
        }

        @Override
        public boolean onCloseQuery() {
            return false;
        }

        @Override
        public Screen getPrevScreen() {
            return null;
        }

        @Override
        public boolean onKeyPressed(int key, int scan, int modifiers) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                discard();
                return true;
            }
            return super.onKeyPressed(key, scan, modifiers);
        }

        @Override
        public void drawBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
            drawPaper(g, x, y, w, h);
            g.drawString(getFont(), t("observation.eyebrow"), x + 23, y + 15, MUTED, false);
            g.renderItem(icon, x + 24, y + 31);
            g.drawString(getFont(), heading, x + 48, y + 34, INK, false);
            rule(g, x + 23, y + 66, w - 46);
            if (!status.getString().isBlank())
                g.drawString(getFont(), getFont().substrByWidth(status, w - 46).getString(), x + 23, y + h - 43, ACCENT, false);
        }
    }
}
