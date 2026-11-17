/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.sensor;

import java.util.Locale;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.theme.VanillaTheme;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TemperatureSensorScreen extends AbstractContainerScreen<TemperatureSensorMenu> {
    private static final Theme THEME = VanillaTheme.INSTANCE;
    private final ItemStack thermometer = new ItemStack(FHItems.temperatureProbe.get());
    private final ItemStack redstone = new ItemStack(Items.REDSTONE_TORCH);
    private EditBox target;
    private Button mode;
    private Boolean displayedInversion;
    private boolean editingTarget;
    public TemperatureSensorScreen(TemperatureSensorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 196;
        imageHeight = 124;
    }
    private Component text(String key) { return Component.translatable("gui.frostedheart.temperature_sensor." + key); }
    @Override protected void init() {
        super.init();
        target = new EditBox(font, leftPos + 64, topPos + 58, 72, 20, text("target"));
        editingTarget = false;
        target.setMaxLength(16);
        target.setValue(Float.toString(menu.getTarget()));
        target.setResponder(value -> editingTarget = true);
        addRenderableWidget(target);
        addRenderableWidget(Button.builder(Component.literal("✓"), button -> {
            try {
                float value = Float.parseFloat(target.getValue());
                if (Float.isFinite(value)) { menu.setTarget(value); target.setTextColor(0xFFFFFF); }
                else target.setTextColor(0xFF5555);
            } catch (NumberFormatException ex) { target.setTextColor(0xFF5555); }
        }).bounds(leftPos + 164, topPos + 58, 20, 20)
                .tooltip(Tooltip.create(text("apply"))).build());
        displayedInversion = null;
        mode = addRenderableWidget(Button.builder(modeLabel(), button -> menu.toggleInverted())
                .bounds(leftPos + 12, topPos + 90, 104, 20).build());
        updateModePresentation();
    }
    @Override protected void containerTick() {
        super.containerTick();
        target.tick();
        if (!editingTarget && !target.isFocused()) {
            target.setValue(Float.toString(menu.getTarget()));
            editingTarget = false;
        }
        updateModePresentation();
    }
    private Component modeLabel() {
        return text(menu.isInverted() ? "inversion_on" : "inversion_off");
    }
    private void updateModePresentation() {
        boolean inverted = menu.isInverted();
        if (displayedInversion != null && displayedInversion == inverted) return;
        displayedInversion = inverted;
        mode.setMessage(modeLabel());
        mode.setTooltip(Tooltip.create(text(inverted ? "inverted" : "normal")));
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        THEME.drawUIBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int color = THEME.UITextColor();
        graphics.drawString(font, title, 12, 10, color, THEME.isUITextShadow());
        graphics.renderItem(thermometer, 12, 31);
        graphics.drawString(font, String.format(Locale.ROOT, "%.1f °C", menu.getCurrent()),
                42, 36, color, false);
        graphics.drawString(font, text("target_label"), 12, 64, color, false);
        graphics.drawString(font, "°C", 143, 64, color, false);
        graphics.renderItem(redstone, 127, 92);
        graphics.drawString(font, menu.getPower() + " / 15", 149, 96, color, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(12, 58, 124, 20, mouseX, mouseY))
            graphics.renderTooltip(font, text("target"), mouseX, mouseY);
        else if (isHovering(12, 30, 124, 19, mouseX, mouseY))
            graphics.renderTooltip(font, Component.translatable("gui.frostedheart.temperature_sensor.current",
                    String.format(Locale.ROOT, "%.1f", menu.getCurrent())), mouseX, mouseY);
        else if (isHovering(125, 90, 60, 20, mouseX, mouseY))
            graphics.renderTooltip(font, Component.translatable("gui.frostedheart.temperature_sensor.output",
                    menu.getPower()), mouseX, mouseY);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (target.isFocused() && key != 256) return target.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
}
