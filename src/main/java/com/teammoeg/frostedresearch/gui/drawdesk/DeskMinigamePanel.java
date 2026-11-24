package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.cui.base.UILayer;
import net.minecraft.network.chat.Component;

/** A stage task displayed on the drawing desk's parchment. */
public abstract class DeskMinigamePanel extends UILayer {
    protected DeskMinigamePanel(DrawDeskLayer parent) { super(parent); }
    public abstract Component projectTitle();
    public abstract String statusKey();
    public abstract void start();
    public void flushPending() {}
}
