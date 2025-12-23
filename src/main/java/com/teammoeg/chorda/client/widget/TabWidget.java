package com.teammoeg.chorda.client.widget;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * A widget that wraps another widget.
 * 仅作为标识，尽可能保留原widget的性质
 * @param <T> Widget Type
 */
public class TabWidget<T extends AbstractWidget> extends AbstractWidget {
    public TabWidget(T widget) {
        super(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), widget.getMessage());
        this.widget = widget;
    }

    public final T widget;

    @Override
    public int getHeight() {
        return this.widget.getHeight();
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param pGuiGraphics the GuiGraphics object used for rendering.
     * @param pMouseX      the x-coordinate of the mouse cursor.
     * @param pMouseY      the y-coordinate of the mouse cursor.
     * @param pPartialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.widget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void setTooltip(@Nullable Tooltip pTooltip) {
        this.widget.setTooltip(pTooltip);
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return this.widget.getTooltip();
    }

    @Override
    public void setTooltipDelay(int pTooltipMsDelay) {
        this.widget.setTooltipDelay(pTooltipMsDelay);
    }

    public static MutableComponent wrapDefaultNarrationMessage(Component pMessage) {
        return Component.translatable("gui.narrate.button", pMessage);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        return;
    }


    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     *
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was clicked.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return this.widget.mouseClicked(pMouseX, pMouseY, pButton);
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     *
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was released.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return this.widget.mouseReleased(pMouseX, pMouseY, pButton);
    }

    /**
     * Called when the mouse is dragged within the GUI element.
     * <p>
     *
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that is being dragged.
     * @param pDragX the X distance of the drag.
     * @param pDragY the Y distance of the drag.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return this.widget.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    /**
     * Retrieves the next focus path based on the given focus navigation event.
     * <p>
     *
     * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
     * @param pEvent the focus navigation event.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
        return this.widget.nextFocusPath(pEvent);
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     *
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     */
    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return this.widget.isMouseOver(pMouseX, pMouseY);
    }

    @Override
    public int getWidth() {
        return this.widget.getWidth();
    }

    @Override
    public void setWidth(int pWidth) {
        this.widget.setWidth(pWidth);
    }

    @Override
    public void setHeight(int value) {
        this.widget.setHeight(value);
    }

    @Override
    public void setAlpha(float pAlpha) {
        this.widget.setAlpha(pAlpha);
    }

    @Override
    public void setMessage(Component pMessage) {
        this.widget.setMessage(pMessage);
    }

    @Override
    public Component getMessage() {
        return this.widget.getMessage();
    }

    /**
     * {@return {@code true} if the GUI element is focused, {@code false} otherwise}
     */
    @Override
    public boolean isFocused() {
        return this.widget.isFocused();
    }

    public boolean isHovered() {
        return this.widget.isHovered();
    }

    public boolean isHoveredOrFocused() {
        return this.widget.isHoveredOrFocused();
    }

    /**
     * {@return {@code true} if the element is active, {@code false} otherwise}
     */
    @Override
    public boolean isActive() {
        return this.widget.isActive();
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean pFocused) {
        this.widget.setFocused(pFocused);
    }

    public static final int UNSET_FG_COLOR = -1;
    protected int packedFGColor = UNSET_FG_COLOR;

    @Override
    public int getFGColor() {
        return this.widget.getFGColor();
    }

    @Override
    public void setFGColor(int color) {
        this.widget.setFGColor(color);
    }

    @Override
    public void clearFGColor() {
        this.widget.clearFGColor();
    }

    /**
     * {@return the narration priority}
     */
    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.widget.narrationPriority();
    }

    /**
     * Updates the narration output with the current narration information.
     *
     * @param pNarrationElementOutput the output to update with narration information.
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }

    @Override
    public int getX() {
        return this.widget.getX();
    }

    @Override
    public void setX(int pX) {
        this.widget.setX(pX);
    }

    @Override
    public int getY() {
        return this.widget.getY();
    }

    @Override
    public void setY(int pY) {
        this.widget.setY(pY);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> pConsumer) {
        this.widget.visitWidgets(pConsumer);
    }

    /**
     * {@return the {@link ScreenRectangle } occupied by the GUI element}
     */
    @Override
    public ScreenRectangle getRectangle() {
        return this.widget.getRectangle();
    }

    /**
     * Returns the tab order group of the GUI component.
     * Tab order group determines the order in which the components are traversed when using keyboard navigation.
     * <p>
     *
     * @return The tab order group of the GUI component.
     */
    @Override
    public int getTabOrderGroup() {
        return this.widget.getTabOrderGroup();
    }

    @Override
    public void setTabOrderGroup(int pTabOrderGroup) {
        this.widget.setTabOrderGroup(pTabOrderGroup);
    }
}