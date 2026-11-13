package com.teammoeg.frostedresearch.knowledge.client.experiment;

import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceMenu;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceStartPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** General controlled-firing machine, using native container input and vanilla GUI artwork. */
public final class ControlledFurnaceScreen extends AbstractContainerScreen<ControlledFurnaceMenu> {
    private static final ResourceLocation PANEL = new ResourceLocation("minecraft", "textures/gui/demo_background.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private Button mode, target, start, cancel;
    private boolean displayedTrial;
    private Optional<ResearchSampleBinding> displayedTarget = Optional.empty();

    public ControlledFurnaceScreen(ControlledFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 276;
        imageHeight = 232;
        inventoryLabelX = ControlledFurnaceMenu.INVENTORY_X;
        inventoryLabelY = ControlledFurnaceMenu.INVENTORY_Y - 12;
    }

    private static Component t(String key, Object... arguments) {
        return Component.translatable("gui.frostedresearch.furnace." + key, arguments);
    }

    private Button button(int x, int y, int width, Component label, Runnable action) {
        return addRenderableWidget(Button.builder(label, ignored -> action.run())
                .bounds(leftPos + x, topPos + y, width, 20).build());
    }

    private void action(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        mode = button(8, 21, 95, Component.empty(), () -> action(2));
        mode.setTooltip(Tooltip.create(t("mode_help")));
        target = button(109, 21, 159, Component.empty(), () -> action(5));
        button(216, 43, 24, Component.literal("−"), () -> action(0)).setTooltip(Tooltip.create(t("minus")));
        button(244, 43, 24, Component.literal("+"), () -> action(1)).setTooltip(Tooltip.create(t("plus")));
        start = button(8, 112, 68, t("start"), () -> FRNetwork.INSTANCE.sendToServer(
                new ControlledFurnaceStartPacket(menu.containerId, menu.getBlock().getBlockPos(), displayedTrial, displayedTarget)));
        cancel = button(80, 112, 68, t("cancel"), () -> action(4));
        cancel.setTooltip(Tooltip.create(t("cancel_help")));
        updateControls();
    }

    private void updateControls() {
        var furnace = menu.getBlock();
        displayedTrial = furnace.trial();
        displayedTarget = furnace.selectedTarget();
        mode.setMessage(t(displayedTrial ? "trial" : "production"));
        mode.active = !furnace.displayedBusy();
        Component label = !displayedTrial ? t("no_target") : furnace.targetTitle().getString().isBlank()
                ? t("choose_target") : furnace.targetTitle();
        if (!label.equals(target.getMessage())) {
            target.setMessage(label);
            target.setTooltip(Tooltip.create(label));
        }
        target.active = displayedTrial && furnace.targetCount() > 1 && !furnace.displayedBusy();
        start.active = !furnace.displayedBusy();
        cancel.active = furnace.displayedBusy() && furnace.remaining() > 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blitNineSliced(PANEL, leftPos, topPos, imageWidth, imageHeight, 4, 248, 166, 0, 0);
        for (var slot : menu.slots)
            graphics.blit(FURNACE, leftPos + slot.x - 1, topPos + slot.y - 1, 55, 16, 18, 18);
        graphics.blit(FURNACE, leftPos + 132, topPos + 90, 79, 34, 24, 17);
        var furnace = menu.getBlock();
        if (furnace.duration() > 0) {
            int progress = Math.min(24, Math.max(0, (int) (24L * (furnace.duration() - furnace.remaining()) / furnace.duration())));
            if (progress > 0) graphics.blit(FURNACE, leftPos + 132, topPos + 90, 176, 14, progress, 16);
        }
    }

    private void line(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(text, width)), x, y, color, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        var furnace = menu.getBlock();
        line(graphics, t("next", furnace.temperature()), 8, 47, 200, 0x404040);
        line(graphics, t("process", furnace.lockedTemperature()), 8, 61, 260, 0x666666);
        String[] labels = {"input", "fuel", "output"};
        for (int i = 0; i < labels.length; i++) {
            Component text = t(labels[i]);
            graphics.drawString(font, text, ControlledFurnaceMenu.POSITIONS[i][0] + 8 - font.width(text) / 2, 77, 0x404040, false);
        }
        line(graphics, t(furnace.message()), 156, 118, 112, 0x404040);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(156, 112, 112, 20, mouseX, mouseY))
            graphics.renderTooltip(font, t(menu.getBlock().message()), mouseX, mouseY);
        else if (isHovering(130, 88, 28, 22, mouseX, mouseY)) {
            var furnace = menu.getBlock();
            graphics.renderComponentTooltip(font, List.of(
                    t("progress", Math.max(0, furnace.duration() - furnace.remaining()), furnace.duration()), t("rules")), mouseX, mouseY);
        }
    }
}
