/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Knowledge lives inside the CUI desk container; opening it does not replace or close the menu.
 */
public final class KnowledgeScreen {
    private KnowledgeScreen() {
    }

    public static void open(Screen parent, DrawDeskContainer menu) {
        if (parent instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen desk)
            desk.showKnowledge();
    }

    public static KnowledgeLayer current() {
        return Minecraft.getInstance().screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen desk ? desk.getKnowledgeLayer() : null;
    }
}
