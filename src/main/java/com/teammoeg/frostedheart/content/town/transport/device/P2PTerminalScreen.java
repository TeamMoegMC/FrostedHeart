/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import com.teammoeg.frostedheart.content.town.transport.TransportRateScroll;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared behavior for the distinct endpoint and bidirectional terminal screens. */
public abstract class P2PTerminalScreen extends AbstractContainerScreen<P2PTerminalMenu> {
    private static final int PAGE_RELATED = 0;
    private static final int PAGE_FILTER = 1;
    private static final int FILTER_X = 7;
    private static final int ENDPOINT_FILTER_Y = 52;
    private static final int BIDIRECTIONAL_FILTER_Y = 70;
    private static final int CONNECTION_Y = 44;
    private static final int ENDPOINT_CONNECTION_ROW_HEIGHT = 32;
    private static final int BIDIRECTIONAL_CONNECTION_ROW_HEIGHT = 46;
    private static final int CONNECTION_FLOW_WIDTH = 152;
    private static final int CONNECTION_LINE_HEIGHT = 9;

    private final boolean bidirectionalLayout;
    private final ScrollTracker rateScrollTracker = new ScrollTracker();
    private int page = PAGE_RELATED;
    private boolean bidirectionalInputFilter = true;
    private int connectionScroll;
    private Button relatedTab;
    private Button filterTab;
    private Button inputFilterTab;
    private Button outputFilterTab;
    private Button listModeButton;
    private Button matchModeButton;
    private EditBox rateInput;
    private Button applyRateButton;
    private final Button[] unlinkButtons = new Button[3];

    protected P2PTerminalScreen(
            P2PTerminalMenu menu,
            Inventory inventory,
            Component title,
            boolean bidirectionalLayout
    ) {
        super(menu, inventory, title);
        this.bidirectionalLayout = bidirectionalLayout;
        imageWidth = 176;
        imageHeight = menu.screenHeight();
        inventoryLabelY = menu.playerInventoryY() - 12;
    }

    @Override
    protected void init() {
        super.init();
        relatedTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.frostedheart.p2p_terminal.related"),
                button -> page = PAGE_RELATED).bounds(leftPos + 5, topPos + 20, 82, 18).build());
        filterTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.frostedheart.p2p_terminal.filter"),
                button -> page = PAGE_FILTER).bounds(leftPos + 89, topPos + 20, 82, 18).build());
        inputFilterTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.frostedheart.p2p_terminal.input_filter"),
                button -> bidirectionalInputFilter = true)
                .bounds(leftPos + 7, topPos + 44, 78, 18).build());
        outputFilterTab = addRenderableWidget(Button.builder(
                Component.translatable("gui.frostedheart.p2p_terminal.output_filter"),
                button -> bidirectionalInputFilter = false)
                .bounds(leftPos + 91, topPos + 44, 78, 18).build());
        listModeButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> menu.toggleFilterMode(editingSendFilter(), false))
                .bounds(leftPos + 7, topPos + filterModeY(), 78, 18).build());
        matchModeButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> menu.toggleFilterMode(editingSendFilter(), true))
                .bounds(leftPos + 91, topPos + filterModeY(), 78, 18).build());
        rateInput = new EditBox(font, leftPos + 45, topPos + 94, 48, 16,
                Component.translatable("gui.frostedheart.p2p_terminal.rate"));
        rateInput.setFilter(text -> validRateText(text, menu.getView().maximumRateItemsPerSecond()));
        rateInput.setTooltip(Tooltip.create(Component.translatable(
                "gui.frostedheart.p2p_terminal.rate_adjust_hint")));
        addRenderableWidget(rateInput);
        applyRateButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.frostedheart.p2p_terminal.apply"),
                button -> applyRate()).bounds(leftPos + 98, topPos + 93, 71, 18).build());
        for (int row = 0; row < unlinkButtons.length; row++) {
            int visibleRow = row;
            unlinkButtons[row] = addRenderableWidget(Button.builder(
                    Component.literal("x"), button -> unlinkVisibleConnection(visibleRow))
                    .bounds(leftPos + 159,
                            topPos + 45 + unlinkButtonOffset()
                                    + row * connectionRowHeight(),
                            12, 14)
                    .tooltip(Tooltip.create(Component.translatable(
                            "gui.frostedheart.p2p_terminal.unlink")))
                    .build());
        }
        refreshWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshWidgets();
    }

    private void refreshWidgets() {
        P2PTerminalMenuView view = menu.getView();
        relatedTab.active = page != PAGE_RELATED;
        filterTab.active = page != PAGE_FILTER;
        inputFilterTab.visible = bidirectionalLayout && page == PAGE_FILTER;
        outputFilterTab.visible = bidirectionalLayout && page == PAGE_FILTER;
        inputFilterTab.active = !bidirectionalInputFilter;
        outputFilterTab.active = bidirectionalInputFilter;
        P2PFilterSnapshot filter = filterSnapshot();
        boolean filterPage = page == PAGE_FILTER;
        listModeButton.visible = filterPage;
        matchModeButton.visible = filterPage;
        listModeButton.setMessage(Component.translatable(filter.whitelist()
                ? "gui.frostedheart.p2p_terminal.whitelist"
                : "gui.frostedheart.p2p_terminal.blacklist"));
        matchModeButton.setMessage(Component.translatable(filter.fuzzy()
                ? "gui.frostedheart.p2p_terminal.fuzzy"
                : "gui.frostedheart.p2p_terminal.exact"));
        int acceptedRate = acceptedOutgoingRate();
        boolean canSetRate = page == PAGE_RELATED && acceptedRate >= 0;
        rateInput.setVisible(canSetRate);
        applyRateButton.visible = canSetRate;
        if (canSetRate && !rateInput.isFocused()) {
            String accepted = Integer.toString(acceptedRate);
            if (!accepted.equals(rateInput.getValue())) {
                rateInput.setValue(accepted);
            }
        }
        connectionScroll = Mth.clamp(connectionScroll, 0,
                Math.max(0, view.connections().size() - visibleConnectionRows()));
        for (int row = 0; row < unlinkButtons.length; row++) {
            unlinkButtons[row].visible = page == PAGE_RELATED
                    && row < visibleConnectionRows()
                    && connectionScroll + row < view.connections().size();
        }
    }

    private int acceptedOutgoingRate() {
        return menu.getView().connections().stream()
                .mapToInt(P2PTerminalConnectionView::outgoingRateItemsPerSecond)
                .filter(rate -> rate >= 0).findFirst().orElse(-1);
    }

    private void applyRate() {
        try {
            menu.setTransportRate(Integer.parseInt(rateInput.getValue()));
            rateInput.setFocused(false);
        } catch (NumberFormatException ignored) {
        }
    }

    private void unlinkVisibleConnection(int row) {
        int index = connectionScroll + row;
        List<P2PTerminalConnectionView> connections = menu.getView().connections();
        if (index >= 0 && index < connections.size()) {
            menu.unbindConnection(connections.get(index).connectionId());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (page == PAGE_RELATED) {
            P2PFilterSnapshot peerFilter = peerFilterAt(mouseX, mouseY);
            if (peerFilter != null) {
                graphics.renderComponentTooltip(font, peerFilterTooltip(peerFilter), mouseX, mouseY);
            }
        } else {
            int slot = filterSlotAt(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack entry = filterSnapshot().entries().get(slot);
                if (!entry.isEmpty()) {
                    graphics.renderTooltip(font, entry, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
        graphics.fill(leftPos + 4, topPos + 42, leftPos + 172,
                topPos + (bidirectionalLayout ? 164 : 128), 0xff9b9b9b);
        graphics.fill(leftPos + 4, topPos + menu.playerInventoryY() - 6,
                leftPos + 172, topPos + imageHeight - 3, 0xff9b9b9b);
        drawPlayerSlots(graphics);
        if (bidirectionalLayout) {
            drawBufferSlots(graphics);
        }
        if (page == PAGE_RELATED) {
            drawConnections(graphics);
        } else {
            drawFilter(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        P2PTerminalMenuView view = menu.getView();
        Component status = Component.translatable(
                "gui.frostedheart.p2p_terminal.status." + view.visualState().getSerializedName());
        int naturalStatusWidth = font.width(status);
        int maximumStatusWidth = imageWidth - 14;
        int renderedStatusWidth = Math.min(naturalStatusWidth, maximumStatusWidth);
        int statusX = imageWidth - 7 - renderedStatusWidth;
        int titleWidth = statusX - 13;
        if (titleWidth > 0) {
            drawFittedString(graphics, title, 7, 6, titleWidth, 0x404040);
        }
        if (naturalStatusWidth <= maximumStatusWidth) {
            graphics.drawString(font, status, statusX, 6,
                    statusColor(view.visualState()), false);
        } else {
            drawFittedString(graphics, status, statusX, 6, maximumStatusWidth,
                    statusColor(view.visualState()));
        }
        if (page == PAGE_RELATED) {
            if (rateInput.visible) {
                graphics.drawString(font, Component.translatable(
                        "gui.frostedheart.p2p_terminal.rate"), 7, 98, 0x404040, false);
            }
            Component capacity = Component.translatable(
                    "gui.frostedheart.p2p_terminal.capacity",
                    format(view.townRemainingCapacity()), format(view.townTotalCapacity()));
            drawFittedString(graphics, capacity, 7, 116, 162, 0x404040);
            if (view.lastDecision() == P2PBindingDecision.INSUFFICIENT_CAPACITY) {
                Component required = Component.translatable(
                        "gui.frostedheart.p2p_terminal.capacity_required",
                        format(view.requiredAdditionalCapacity()));
                drawFittedString(graphics, required, 7,
                        bidirectionalLayout ? 106 : 106, 162, 0x9a2828);
            }
        } else if (!bidirectionalLayout) {
            graphics.drawString(font, Component.translatable(editingSendFilter()
                            ? "gui.frostedheart.p2p_terminal.shipping_filter"
                            : "gui.frostedheart.p2p_terminal.receiving_filter"),
                    7, 42, 0x404040, false);
        }
        if (bidirectionalLayout) {
            graphics.drawString(font, Component.translatable(
                    "gui.frostedheart.p2p_terminal.pending_items"), 7, 130, 0x404040, false);
            graphics.drawString(font, Component.translatable(
                    "gui.frostedheart.p2p_terminal.received_items"), 97, 130, 0x404040, false);
        }
    }

    private void drawConnections(GuiGraphics graphics) {
        List<P2PTerminalConnectionView> connections = menu.getView().connections();
        int end = Math.min(connections.size(), connectionScroll + visibleConnectionRows());
        for (int index = connectionScroll; index < end; index++) {
            int row = index - connectionScroll;
            int y = topPos + CONNECTION_Y + row * connectionRowHeight();
            P2PTerminalConnectionView connection = connections.get(index);
            List<ConnectionFlow> flows = connectionFlows(connection);
            int flowColor = connection.peerLoaded() ? 0x303030 : 0x8a3030;
            int textY = y + 1;
            for (ConnectionFlow flow : flows) {
                Component label = flow.label();
                if (font.width(label) <= CONNECTION_FLOW_WIDTH) {
                    graphics.drawString(font, label, leftPos + 7, textY,
                            flowColor, false);
                    textY += CONNECTION_LINE_HEIGHT;
                } else {
                    graphics.drawString(font, flow.sourceLine(), leftPos + 7,
                            textY, flowColor, false);
                    textY += CONNECTION_LINE_HEIGHT;
                    graphics.drawString(font, flow.targetRateLine(), leftPos + 7,
                            textY, flowColor, false);
                    textY += CONNECTION_LINE_HEIGHT;
                }
            }
            int positionY = textY + 1;
            Component position = Component.translatable("gui.frostedheart.p2p_terminal.position",
                    connection.peer().pos().pos().getX(),
                    connection.peer().pos().pos().getY(),
                    connection.peer().pos().pos().getZ());
            drawFittedString(graphics, position, leftPos + 7, positionY, 128,
                    connection.peerLoaded() ? 0x404040 : 0x8a3030);
        }
    }

    static List<ConnectionFlow> connectionFlows(P2PTerminalConnectionView connection) {
        int outgoing = connection.outgoingRateItemsPerSecond();
        int incoming = connection.incomingRateItemsPerSecond();
        Component local = Component.translatable(
                "gui.frostedheart.p2p_terminal.role.local");
        Component peer = terminalRole(connection.peer().role());
        List<ConnectionFlow> flows = new ArrayList<>(2);
        if (outgoing >= 0) {
            flows.add(new ConnectionFlow(local, peer, compactRate(outgoing)));
        }
        if (incoming >= 0) {
            flows.add(new ConnectionFlow(peer, local, compactRate(incoming)));
        }
        return List.copyOf(flows);
    }

    record ConnectionFlow(Component source, Component target, String rate) {
        Component label() {
            return Component.translatable("gui.frostedheart.p2p_terminal.connection_flow",
                    source, target, rate);
        }

        Component sourceLine() {
            return Component.translatable(
                    "gui.frostedheart.p2p_terminal.connection_flow_source", source);
        }

        Component targetRateLine() {
            return Component.translatable(
                    "gui.frostedheart.p2p_terminal.connection_flow_target_rate", target, rate);
        }
    }

    private static Component terminalRole(P2PTerminalRole role) {
        return switch (role) {
            case SHIPPING -> Component.translatable(
                    "gui.frostedheart.p2p_terminal.role.shipping");
            case RECEIVING -> Component.translatable(
                    "gui.frostedheart.p2p_terminal.role.receiving");
            case BIDIRECTIONAL -> Component.translatable(
                    "gui.frostedheart.p2p_terminal.role.bidirectional");
        };
    }

    private void drawFilter(GuiGraphics graphics) {
        P2PFilterSnapshot filter = filterSnapshot();
        for (int slot = 0; slot < P2PItemFilter.SLOT_COUNT; slot++) {
            int x = leftPos + FILTER_X + slot * 18;
            int y = topPos + filterY();
            drawSlotBackground(graphics, x, y);
            ItemStack entry = filter.entries().get(slot);
            if (!entry.isEmpty()) {
                graphics.renderItem(entry, x, y);
            }
        }
    }

    private P2PFilterSnapshot filterSnapshot() {
        return editingSendFilter()
                ? menu.getView().sendFilter() : menu.getView().receiveFilter();
    }

    private boolean editingSendFilter() {
        return sendFilterFor(menu.getRole(), bidirectionalInputFilter);
    }

    static boolean sendFilterFor(P2PTerminalRole role, boolean bidirectionalInputFilter) {
        return role == P2PTerminalRole.SHIPPING
                || role == P2PTerminalRole.BIDIRECTIONAL && bidirectionalInputFilter;
    }

    private P2PFilterSnapshot peerFilterAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - (leftPos + 7);
        int relativeY = (int) mouseY - (topPos + CONNECTION_Y);
        if (relativeX < 0 || relativeX >= CONNECTION_FLOW_WIDTH || relativeY < 0) {
            return null;
        }
        int row = relativeY / connectionRowHeight();
        if (row >= visibleConnectionRows()) {
            return null;
        }
        int index = connectionScroll + row;
        List<P2PTerminalConnectionView> connections = menu.getView().connections();
        if (index >= connections.size()) {
            return null;
        }
        P2PTerminalConnectionView connection = connections.get(index);
        return connection.outgoingRateItemsPerSecond() >= 0
                ? connection.peerReceiveFilter().orElse(null)
                : connection.peerSendFilter().orElse(null);
    }

    private static List<Component> peerFilterTooltip(P2PFilterSnapshot filter) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(filter.whitelist()
                ? "gui.frostedheart.p2p_terminal.whitelist"
                : "gui.frostedheart.p2p_terminal.blacklist"));
        tooltip.add(Component.translatable(filter.fuzzy()
                ? "gui.frostedheart.p2p_terminal.fuzzy"
                : "gui.frostedheart.p2p_terminal.exact"));
        filter.entries().stream().filter(entry -> !entry.isEmpty())
                .map(ItemStack::getHoverName).forEach(tooltip::add);
        if (tooltip.size() == 2) {
            tooltip.add(Component.translatable("gui.frostedheart.p2p_terminal.filter_all"));
        }
        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == PAGE_FILTER && button == 0) {
            int slot = filterSlotAt(mouseX, mouseY);
            if (slot >= 0) {
                menu.setFilterEntry(editingSendFilter(), slot);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == PAGE_RELATED && rateInput.visible && rateInput.isMouseOver(mouseX, mouseY)) {
            rateScrollTracker.addScroll(delta);
            int scrollSteps = rateScrollTracker.getScroll();
            if (scrollSteps != 0) {
                int maximumRate = menu.getView().maximumRateItemsPerSecond();
                int currentRate = rateForScroll(
                        rateInput.getValue(), acceptedOutgoingRate(), maximumRate);
                int adjustedRate = adjustRateForScroll(
                        currentRate, scrollSteps,
                        CInputHelper.isShiftKeyDown(), CInputHelper.isCtrlKeyDown(), maximumRate);
                rateInput.setValue(Integer.toString(adjustedRate));
                rateInput.setFocused(false);
                menu.setTransportRate(adjustedRate);
            }
            return true;
        }
        if (page == PAGE_RELATED
                && mouseY >= topPos + CONNECTION_Y
                && mouseY < topPos + CONNECTION_Y
                + visibleConnectionRows() * connectionRowHeight()) {
            connectionScroll = Mth.clamp(connectionScroll - (int) Math.signum(delta), 0,
                    Math.max(0, menu.getView().connections().size() - visibleConnectionRows()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    static int adjustRateForScroll(
            int currentRate,
            int scrollSteps,
            boolean shiftDown,
            boolean ctrlDown,
            int maximumRate
    ) {
        return TransportRateScroll.adjust(
                currentRate, scrollSteps, shiftDown, ctrlDown, maximumRate);
    }

    static int rateForScroll(String text, int acceptedRate, int maximumRate) {
        return TransportRateScroll.rateForScroll(text, acceptedRate, maximumRate);
    }

    private int filterSlotAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - (leftPos + FILTER_X);
        int relativeY = (int) mouseY - (topPos + filterY());
        if (relativeX < 0 || relativeY < 0 || relativeY >= 16) {
            return -1;
        }
        int slot = relativeX / 18;
        return slot < P2PItemFilter.SLOT_COUNT && relativeX % 18 < 16 ? slot : -1;
    }

    private int filterY() {
        return bidirectionalLayout ? BIDIRECTIONAL_FILTER_Y : ENDPOINT_FILTER_Y;
    }

    private int filterModeY() {
        return bidirectionalLayout ? 94 : 74;
    }

    private int visibleConnectionRows() {
        return bidirectionalLayout ? 1 : 2;
    }

    private int connectionRowHeight() {
        return bidirectionalLayout
                ? BIDIRECTIONAL_CONNECTION_ROW_HEIGHT : ENDPOINT_CONNECTION_ROW_HEIGHT;
    }

    private int unlinkButtonOffset() {
        return bidirectionalLayout ? BIDIRECTIONAL_CONNECTION_ROW_HEIGHT - 12 : 0;
    }

    private void drawBufferSlots(GuiGraphics graphics) {
        for (int slot = 0; slot < P2PTerminalBuffer.TOTAL_SLOTS; slot++) {
            drawSlotBackground(graphics, leftPos + P2PTerminalMenu.bufferSlotX(slot),
                    topPos + P2PTerminalMenu.BUFFER_SLOT_Y);
        }
    }

    private void drawPlayerSlots(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(graphics, leftPos + 8 + column * 18,
                        topPos + menu.playerInventoryY() + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotBackground(graphics, leftPos + 8 + column * 18,
                    topPos + menu.playerHotbarY());
        }
    }

    private void drawFittedString(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int maximumWidth,
            int color
    ) {
        int width = font.width(text);
        if (width <= maximumWidth || width == 0) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        float scale = maximumWidth / (float) width;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
    }

    private static int statusColor(P2PTerminalVisualState state) {
        return switch (state) {
            case IDLE, TRANSFERRING -> 0x287a28;
            case REDSTONE_PAUSED, SHORTAGE -> 0xa06a00;
            default -> 0x9a2828;
        };
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String compactRate(int rate) {
        if (rate >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", rate / 1_000_000.0);
        }
        if (rate >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", rate / 1_000.0);
        }
        return Integer.toString(rate);
    }

    private static boolean validRateText(String text, int maximum) {
        if (text.isEmpty()) {
            return true;
        }
        try {
            int value = Integer.parseInt(text);
            return value >= 0 && value <= maximum;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
