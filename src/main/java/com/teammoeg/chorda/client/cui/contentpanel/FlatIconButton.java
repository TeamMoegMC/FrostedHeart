package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.FlatIcon;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 扁平图标按钮，使用FlatIcon矢量图标作为显示内容的紧凑按钮。
 * 常用于内容面板中文本行的行内操作按钮。
 * <p>
 * Flat icon button using FlatIcon vector icons as compact button display content.
 * Commonly used as inline action buttons within text lines in content panels.
 */
@Getter
@Setter
public class FlatIconButton extends Button {
    protected Consumer<MouseButton> clickAction;
    protected FlatIcon fIcon;
    protected float scale;

    public FlatIconButton(UIElement panel, Component t, FlatIcon icon, Consumer<MouseButton> clickAction) {
        super(panel, t, icon.toCIcon());
        this.clickAction = clickAction;
        this.fIcon = icon;
        setSize(icon.size.width + 2, icon.size.height + 2);
    }

    @Override
    public void onClicked(MouseButton button) {
        clickAction.accept(button);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        theme().drawButton(graphics, x, y, w, h, isMouseOver(), isEnabled());
        fIcon.render(graphics.pose(), x+1, y+1, theme().UITextColor());
//        icon.draw(graphics, x+1, y+1, fIcon.size.width, fIcon.size.height);
    }

    public FlatIconButton scale(float scale) {
        int w = Math.round(this.width / this.scale);
        int h = Math.round(this.height / this.scale);
        this.scale = scale;
        setSize(w, h);
        refresh();
        return this;
    }
}
