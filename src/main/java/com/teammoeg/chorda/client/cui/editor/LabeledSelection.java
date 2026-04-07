/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.client.cui.editor;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.text.Components;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 带标签的选择控件，通过左/右键点击在一组选项中循环切换。
 * 提供布尔选择、枚举选择和自定义列表选择的工厂方法。
 * <p>
 * Labeled selection widget that cycles through a set of options via left/right
 * mouse button clicks. Provides factory methods for boolean selection, enum
 * selection, and custom list selection.
 *
 * @param <R> 选项值类型 / The option value type
 */
public class LabeledSelection<R> extends LabeledPane<TextButton> {
    List<R> objs;


    Function<R, CIcon> toicon;
    int sel;

    public LabeledSelection(UIElement panel, Component lab, R val, Collection<R> aobjs, Function<R, Component> atostr,Function<R, CIcon> atoicon) {
        this(panel, lab, val, new ArrayList<>(aobjs), atostr,atoicon);
    }

    public LabeledSelection(UIElement panel, Component lab, R val, List<R> aobjs, Function<R, Component> atostr,Function<R, CIcon> atoicon) {
        super(panel, lab);
        this.objs = aobjs;
        if(val==null)
        	val=objs.get(0);
        sel = objs.indexOf(val);
        obj = new TextButton(this,atostr!=null?atostr.apply(val):Components.empty(), atoicon==null?CIcons.nop():atoicon.apply(val)) {

            @Override
            public void getTooltip(TooltipBuilder list) {
            	if(atostr!=null) {
	                int i = 0;
	                for (R elm : objs) {
	                    if (i == sel)
	                        list.accept(Components.str("->").append(atostr.apply(elm)));
	                    else
	                        list.accept(atostr.apply(elm));
	                    i++;
	                }
            	}
            }


			@Override
            public void onClicked(MouseButton arg0) {
                if (arg0==MouseButton.LEFT)
                    sel++;
                else if (arg0==MouseButton.RIGHT)
                    sel--;
                if (sel >= objs.size())
                    sel = 0;
                if (sel < 0)
                    sel = objs.size() - 1;
                if(atostr!=null)
                	this.setTitle(atostr.apply(objs.get(sel)));
                this.setIcon(atoicon.apply(objs.get(sel)));
                LabeledSelection.this.refresh();
                onChange(objs.get(sel));
            }

        };

        //obj.setHeight(20);
    }

    public LabeledSelection(UIElement panel, Component lab, R val, R[] aobjs, Function<R, Component> atostr,Function<R, CIcon> atoicon) {
        this(panel, lab, val, Arrays.asList(aobjs), atostr,atoicon);
    }

    /**
     * 创建一个布尔选择控件。
     * <p>
     * Creates a boolean selection widget.
     *
     * @param p   父UI元素 / the parent UI element
     * @param lab 标签文本 / the label text
     * @param val 初始布尔值 / the initial boolean value
     * @return 布尔选择控件 / the boolean selection widget
     */
    public static LabeledSelection<Boolean> createBool(UIElement p, Component lab, Boolean val) {
        return new LabeledSelection<>(p, lab, val, Arrays.asList(true, false), null,t->t? FlatIcon.BOX_ON.toCIcon(): FlatIcon.BOX.toCIcon()) {
            @Override
            public void addUIElements() {
                if (obj != null)
                	add(obj);
                add(label);
            }
        };
    }

    /**
     * 创建一个枚举选择控件。
     * <p>
     * Creates an enum selection widget.
     *
     * @param p   父UI元素 / the parent UI element
     * @param lab 标签文本 / the label text
     * @param en  枚举类 / the enum class
     * @param val 初始枚举值 / the initial enum value
     * @param <T> 枚举类型 / the enum type
     * @return 枚举选择控件 / the enum selection widget
     */
    public static <T extends Enum<T>> LabeledSelection<T> createEnum(UIElement p, Component lab, Class<T> en, T val) {
        return new LabeledSelection<>(p, lab, val, Arrays.asList(en.getEnumConstants()), a->Components.str(a.name()),a->CIcons.nop());
    }
    public static <T extends Enum<T>> LabeledSelection<T> createEnum(UIElement p, Component lab, Class<T> en, T val, Function<T, Component> atostr,Function<T, CIcon> atoicon) {
        return new LabeledSelection<>(p, lab, val, Arrays.asList(en.getEnumConstants()), atostr, atoicon);
    }

    public static LabeledSelection<String> createCriterion(UIElement p, Component lab, Advancement advx, String val, Consumer<String> cb) {
        List<String> cit = new ArrayList<>();
        cit.add("");
        if (advx != null) {
            cit.addAll(advx.getCriteria().keySet());
        }
        return new LabeledSelection<String>(p, lab, val, cit, a->Components.str(String.valueOf(a)),a->CIcons.nop()) {

            @Override
            public void onChange(String current) {
                cb.accept(current);
                super.onChange(current);

            }

        };
    }

    /**
     * 获取当前选中的值。
     * <p>
     * Gets the currently selected value.
     *
     * @return 当前选中的值 / the currently selected value
     */
    public R getSelection() {
        return objs.get(sel);
    }

    /**
     * 设置当前选中的值。
     * <p>
     * Sets the currently selected value.
     *
     * @param val 要选中的值 / the value to select
     */
    public void setSelection(R val) {
        sel = objs.indexOf(val);
    }

    public void onChange(R current) {
    }

    @Override
    public void alignWidgets() {
        int w = (int)(this.getWidth()*0.45F);
        obj.setX(this.getWidth() - (w+obj.getWidth())/2);
        label.setPos(4, (this.getContentHeight() - 8) / 2);
        setHeight(getContentHeight());
    }
}
