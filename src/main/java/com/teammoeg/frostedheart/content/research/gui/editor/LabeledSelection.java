/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.lang.Components;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class LabeledSelection<R> extends LabeledPane<Button> {
    List<R> objs;

    Function<R, String> tostr;

    int sel;

    public LabeledSelection(Panel panel, String lab, R val, Collection<R> aobjs, Function<R, String> atostr) {
        this(panel, lab, val, new ArrayList<>(aobjs), atostr);
    }

    public LabeledSelection(Panel panel, String lab, R val, List<R> aobjs, Function<R, String> atostr) {
        super(panel, lab);
        this.objs = aobjs;
        this.tostr = atostr;
        sel = objs.indexOf(val);
        obj = new SimpleTextButton(this, Components.str(tostr.apply(val)), Icon.empty()) {

            @Override
            public void addMouseOverText(TooltipList list) {
                int i = 0;
                for (R elm : objs) {
                    if (i == sel)
                        list.add(Components.str("->" + tostr.apply(elm)));
                    else
                        list.add(Components.str(tostr.apply(elm)));
                    i++;
                }
            }

            @Override
            public void onClicked(MouseButton arg0) {
                if (arg0.isLeft())
                    sel++;
                else if (arg0.isRight())
                    sel--;
                if (sel >= objs.size())
                    sel = 0;
                if (sel < 0)
                    sel = objs.size() - 1;
                this.setTitle(Components.str(tostr.apply(objs.get(sel))));
                refreshWidgets();
                onChange(objs.get(sel));
            }

        };

        obj.setHeight(20);
    }

    public LabeledSelection(Panel panel, String lab, R val, R[] aobjs, Function<R, String> atostr) {
        this(panel, lab, val, Arrays.asList(aobjs), atostr);
    }

    public static LabeledSelection<Boolean> createBool(Panel p, String lab, boolean val) {
        return new LabeledSelection<>(p, lab, val, Arrays.asList(true, false), String::valueOf);
    }

    public static <T extends Enum<T>> LabeledSelection<T> createEnum(Panel p, String lab, Class<T> en, T val) {
        return new LabeledSelection<>(p, lab, val, Arrays.asList(en.getEnumConstants()), Enum::name);
    }

    public static LabeledSelection<String> createCriterion(Panel p, String lab, ResourceLocation adv, String val, Consumer<String> cb) {
        ClientAdvancements cam = ClientUtils.mc().player.connection.getAdvancements();
        Advancement advx = cam.getAdvancements().get(adv);
        List<String> cit = new ArrayList<>();
        cit.add("");
        if (advx != null) {
            cit.addAll(advx.getCriteria().keySet());
        }
        return new LabeledSelection<String>(p, lab, val, cit, String::valueOf) {

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
