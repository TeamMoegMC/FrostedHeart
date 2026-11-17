package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.TextureTesselator;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.frostedresearch.gui.drawdesk.CardButton;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.game.*;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/** Existing desk tiles and input protocol, displayed inside the idea-link page. */
public final class LinkGamePanel extends UILayer {
    private final ClientResearchGame client;
    private final BlockPos clientDesk;
    private final int cell;
    private final CardButton[][] buttons = new CardButton[9][9];
    private boolean pending;

    public LinkGamePanel(UIElement parent, BlockPos desk, ResearchGame board, int cell) {
        super(parent);
        clientDesk = desk;
        this.cell = cell;
        client = new ClientResearchGame(board, desk) {
            @Override public void accept(ResearchGame game) { calculateCardNum(); }
            @Override public boolean tryCombine(CardPos first, CardPos second) {
                if (pending) return false;
                boolean legal = second == null ? ResearchGame.FINAL_CARD.equals(first) && board.isTouchable(first)
                        : board.canCombine(first, second);
                if (legal) {
                    FRNetwork.INSTANCE.sendToServer(FHDrawingDeskOperationPacket.linkMove(desk, first, second));
                    pending = true;
                }
                return legal;
            }
            @Override public boolean finishForTesting(CardPos pos) {
                if (pending) return false;
                if (!ResearchGame.FINAL_CARD.equals(pos) || board.isEmpty()) return false;
                FRNetwork.INSTANCE.sendToServer(FHDrawingDeskOperationPacket.linkMove(desk, pos, null));
                pending = true;
                return true;
            }
        };
        setSize(cell * 9, cell * 9);
    }

    public void restart() {
        if (pending) return;
        pending = true;
        FRNetwork.INSTANCE.sendToServer(FHDrawingDeskOperationPacket.linkReset(clientDesk));
    }

    @Override public void addUIElements() {
        for (int x = 0; x < 9; x++) for (int y = 0; y < 9; y++) {
            final CardPos pos = CardPos.valueOf(x, y);
            var button = new CardButton(this, client, x, y) {
                @Override public void onClicked(MouseButton mouse) {
                    if (!pending) super.onClicked(mouse);
                }
                @Override public void collectRenderInfo(TextureTesselator texture, GuiGraphics graphics,
                        int x, int y, int width, int height, RenderingHint hint) {
                    var card = client.get(pos);
                    if (!card.isShow()) return;
                    boolean active = client.isTouchable(pos);
                    DrawDeskIcons.getIcon(card.getCt(), card.getCard(), active)
                            .tesselate(texture, graphics, x, y, cell - 1, cell - 1);
                    if (active && (isMouseOver() || pos.equals(client.getLastSelect())))
                        DrawDeskIcons.SELECTED.tesselate(texture, graphics, x, y, cell - 1, cell - 1);
                }
            };
            button.setPosAndSize(x * cell, y * cell, cell, cell);
            buttons[x][y] = button;
            add(button);
        }
    }
    @Override public void alignWidgets() {}

    @Override public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        try (var texture = TesselateHelper.getTextureTesselator(DrawDeskIcons.LOCATION)) {
            for (var column : buttons) for (var button : column)
                button.collectRenderInfo(texture, graphics, x + button.getX(), y + button.getY(),
                        button.getWidth(), button.getHeight(), hint);
        }
    }
}
