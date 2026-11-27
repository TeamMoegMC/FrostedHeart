package com.teammoeg.frostedresearch.knowledge.client.experiment;

import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceBlockEntity;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceMenu;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceStartPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class ControlledFurnaceScreen extends AbstractContainerScreen<ControlledFurnaceMenu> {
    private static final ResourceLocation PANEL = new ResourceLocation("minecraft", "textures/gui/demo_background.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private EditBox temperature, timer;
    private Button cooler, hotter, start, stop;

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

    private EditBox field(int x, int y, int digits, Component label, String value) {
        var box = addRenderableWidget(new EditBox(font, leftPos + x, topPos + y, 64, 16, label));
        box.setMaxLength(digits);
        box.setFilter(text -> text.length() <= digits && text.chars().allMatch(c -> c >= '0' && c <= '9'));
        box.setValue(value);
        box.setResponder(ignored -> updateControls());
        return box;
    }

    @Override protected void init() {
        super.init();
        var furnace = menu.getBlock();
        temperature = field(112, 23, 4, t("temperature"), temperature == null
                ? Integer.toString(furnace.temperature()) : temperature.getValue());
        temperature.setTooltip(Tooltip.create(t("temperature_help", ControlledFurnaceBlockEntity.MIN_TEMPERATURE,
                ControlledFurnaceBlockEntity.MAX_TEMPERATURE)));
        timer = field(112, 46, 5, t("timer"), timer == null ? "" : timer.getValue());
        timer.setTooltip(Tooltip.create(t("timer_help", ControlledFurnaceBlockEntity.MAX_TIMER_SECONDS)));
        cooler = button(184, 21, 38, Component.literal("−50"), () -> adjust(-50));
        hotter = button(226, 21, 38, Component.literal("+50"), () -> adjust(50));
        start = button(8, 112, 68, t("start"), this::start);
        stop = button(80, 112, 68, t("stop"), () -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ControlledFurnaceMenu.STOP));
        updateControls();
    }

    private static int value(EditBox box, int fallback) {
        return box.getValue().isEmpty() ? fallback : Integer.parseInt(box.getValue());
    }

    private void adjust(int amount) {
        int current = value(temperature, menu.getBlock().temperature());
        temperature.setValue(Integer.toString(Mth.clamp(current + amount,
                ControlledFurnaceBlockEntity.MIN_TEMPERATURE, ControlledFurnaceBlockEntity.MAX_TEMPERATURE)));
    }

    private void start() {
        FRNetwork.INSTANCE.sendToServer(new ControlledFurnaceStartPacket(menu.containerId, menu.getBlock().getBlockPos(),
                value(temperature, 0), value(timer, 0)));
    }

    private void updateControls() {
        if (start == null) return;
        boolean running = menu.getBlock().running();
        int chosen = value(temperature, 0);
        start.active = !running && chosen >= ControlledFurnaceBlockEntity.MIN_TEMPERATURE
                && chosen <= ControlledFurnaceBlockEntity.MAX_TEMPERATURE
                && value(timer, 0) <= ControlledFurnaceBlockEntity.MAX_TIMER_SECONDS;
        stop.active = running;
        hotter.active = cooler.active = !running;
        temperature.setVisible(!running);
        timer.setVisible(!running);
        if (running && getFocused() instanceof EditBox) setFocused(null);
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox input && key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_TAB) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) setFocused(null);
            else input.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blitNineSliced(PANEL, leftPos, topPos, imageWidth, imageHeight, 4, 248, 166, 0, 0);
        for (var slot : menu.slots)
            graphics.blit(FURNACE, leftPos + slot.x - 1, topPos + slot.y - 1, 55, 16, 18, 18);
    }

    private void line(GuiGraphics graphics, Component text, int x, int y, int width) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(text, width)), x, y, 0x404040, false);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        var furnace = menu.getBlock();
        if (furnace.running()) {
            line(graphics, t("firing_at", furnace.temperature()), 8, 28, 170);
            line(graphics, furnace.timed() ? t("time_left", furnace.timerSeconds())
                    : t("elapsed", furnace.elapsedSeconds()), 8, 51, 260);
        } else {
            line(graphics, t("temperature"), 8, 28, 102);
            line(graphics, t("timer"), 8, 51, 102);
            line(graphics, t("timer_blank"), 182, 51, 88);
        }
        Component specimens = t(furnace.running() ? "locked_samples" : "samples");
        line(graphics, specimens, ControlledFurnaceMenu.SAMPLE_X + (106 - font.width(specimens)) / 2, 70, 106);
        Component fuel = t("fuel");
        line(graphics, fuel, ControlledFurnaceMenu.FUEL_X + (16 - font.width(fuel)) / 2, 70, 58);
        line(graphics, t(furnace.message()), 156, 118, 112);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(156, 112, 112, 20, mouseX, mouseY))
            graphics.renderTooltip(font, t(menu.getBlock().message()), mouseX, mouseY);
    }
}
