/*
 * Copyright (c) 2024 TeamMoeg
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;

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

    public R getSelection() {
        return objs.get(sel);
    }

    public void setSelection(R val) {
        sel = objs.indexOf(val);
    }

    public void onChange(R current) {
    }



}
