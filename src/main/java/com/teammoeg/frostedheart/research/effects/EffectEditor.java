/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.research.gui.FHIcons.IconEditor;
import com.teammoeg.frostedheart.research.gui.editor.*;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class EffectEditor<T extends Effect> extends BaseEditDialog {
    public static final Editor<EffectBuilding> BUILD = (p, l, v, c) -> {
        new Building(p, l, v, c).open();
    };
    public static final Editor<EffectCrafting> CRAFT = (p, l, v, c) -> {
        new Crafting(p, l, v, c).open();
    };
    public static final Editor<EffectItemReward> ITEM = (p, l, v, c) -> {
        new ItemReward(p, l, v, c).open();
    };
    public static final Editor<EffectStats> STATS = (p, l, v, c) -> {
        new Stats(p, l, v, c).open();
    };
    public static final Editor<EffectUse> USE = (p, l, v, c) -> {
        new Use(p, l, v, c).open();
    };
    public static final Editor<EffectShowCategory> CAT = (p, l, v, c) -> {
        new Category(p, l, v, c).open();
    };
    public static final Editor<Effect> EDITOR = (p, l, v, c) -> {
        if (v instanceof EffectBuilding)
            BUILD.open(p, l, (EffectBuilding) v, e -> c.accept(e));
        else if (v instanceof EffectCrafting)
            CRAFT.open(p, l, (EffectCrafting) v, e -> c.accept(e));
        else if (v instanceof EffectItemReward)
            ITEM.open(p, l, (EffectItemReward) v, e -> c.accept(e));
        else if (v instanceof EffectStats)
            STATS.open(p, l, (EffectStats) v, e -> c.accept(e));
        else if (v instanceof EffectUse)
            USE.open(p, l, (EffectUse) v, e -> c.accept(e));
        else if (v instanceof EffectShowCategory)
            CAT.open(p, l, (EffectShowCategory) v, e -> c.accept(e));
        else
            new EditorSelector<>(p, l, c).addEditor("Building", BUILD).addEditor("Craft", CRAFT).addEditor("Item Reward", ITEM).addEditor("Add Stats", STATS).addEditor("Add Usage", USE).addEditor("Recipe Category", CAT).open();

    };

    String lbl;
    T e;
    Consumer<T> cb;
    protected LabeledTextBoxAndBtn nonce;
    protected LabeledTextBox name;
    protected LabeledSelection<Boolean> sd;
    public static final Editor<Collection<Effect>> EFFECT_LIST = (p, l, v, c) -> {
        new EditListDialog<>(p, l, v, null, EffectEditor.EDITOR, Effect::getBrief, Effect::getIcon, c).open();
    };

    public EffectEditor(Widget panel, String lbl, T e, Consumer<T> cb) {
        super(panel);

        this.lbl = lbl;


        if (e == null) {
            e = createEffect();
        }
        this.e = e;
        nonce = new LabeledTextBoxAndBtn(this, "nonce", e.getNonce(), "Random", t -> t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
        name = new LabeledTextBox(this, "name", e.name);
        sd = LabeledSelection.createBool(this, "Hide", e.isHidden());
        this.cb = cb;
    }

    public abstract T createEffect();

    @Override
    public void onClose() {
        e.name = name.getText();
        e.hidden = sd.getSelection();
        if (e.getRId() != 0) {
            e.setNewId(nonce.getText());
        } else {
            e.nonce = nonce.getText();
        }
        cb.accept(e);

    }

    @Override
    public void addWidgets() {
        add(EditUtils.getTitle(this, lbl));
        add(nonce);
        add(name);
        add(new OpenEditorButton<>(this, "Edit Description", EditListDialog.STRING_LIST, e.tooltip, s -> e.tooltip = new ArrayList<>(s)));
        add(new OpenEditorButton<>(this, "Edit icon", IconEditor.EDITOR, e.icon == null ? e.getDefaultIcon() : e.icon, s -> e.icon = s));
        add(sd);
    }

    private static class Building extends EffectEditor<EffectBuilding> {

        public Building(Widget panel, String lbl, EffectBuilding e, Consumer<EffectBuilding> cb) {
            super(panel, lbl, e, cb);

        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new LabeledOpenEditorButton<>(this, e.multiblock == null ? "" : e.multiblock.getUniqueName().toString(), "Select Multiblock", SelectDialog.EDITOR_MULTIBLOCK, e.multiblock, s -> e.multiblock = s));

        }

        @Override
        public EffectBuilding createEffect() {
            return new EffectBuilding();
        }

        @Override
        public void onClose() {
            if (e.multiblock != null)
                super.onClose();
        }

    }

    private static class Category extends EffectEditor<EffectShowCategory> {
        LabeledTextBox category;

        public Category(Widget panel, String lbl, EffectShowCategory e, Consumer<EffectShowCategory> cb) {
            super(panel, lbl, e, cb);
            category = new LabeledTextBox(this, "category id", this.e.cate == null ? "" : this.e.cate.toString());
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(category);
        }

        @Override
        public EffectShowCategory createEffect() {
            return new EffectShowCategory();
        }

        @Override
        public void onClose() {
            if (category.getText().isEmpty()) return;
            e.cate = new ResourceLocation(category.getText());
            super.onClose();
        }

    }

    private static class Crafting extends EffectEditor<EffectCrafting> {

        public Crafting(Widget panel, String lbl, EffectCrafting e, Consumer<EffectCrafting> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(EditUtils.getTitle(this, "Only the first in the following takes effects"));
            add(new OpenEditorButton<>(this, "Select Item", SelectItemStackDialog.EDITOR, e.item == null ? ItemStack.EMPTY : new ItemStack(e.item), e.item == null ? Icon.EMPTY : ItemIcon.getItemIcon(e.item), s -> {
                e.item = s.getItem();
            }));
            add(new OpenEditorButton<>(this, "Edit ItemStack", SelectItemStackDialog.EDITOR, e.itemStack == null ? ItemStack.EMPTY : e.itemStack, e.itemStack == null ? Icon.EMPTY : ItemIcon.getItemIcon(e.itemStack), s -> {
                e.item = null;
                e.itemStack = s;
            }));
            add(new OpenEditorButton<>(this, "Edit Recipe IDs", EditListDialog.STRING_LIST, e.unlocks.stream().map(IRecipe::getId).map(String::valueOf).collect(Collectors.toList()), s -> {
                e.setList(s);
                e.item = null;
                e.itemStack = null;
            }));

        }

        @Override
        public EffectCrafting createEffect() {
            return new EffectCrafting();
        }

    }

    private static class ItemReward extends EffectEditor<EffectItemReward> {
        private static String fromItemStack(ItemStack s) {
            return s.getDisplayName().getString() + " x " + s.getCount();
        }

        public ItemReward(Widget panel, String lbl, EffectItemReward e, Consumer<EffectItemReward> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new LabeledOpenEditorButton<>(this, e.rewards.size() > 0 ? fromItemStack(e.rewards.get(0)) : "", "Edit Rewards", SelectItemStackDialog.STACK_LIST, e.rewards, s -> e.rewards = new ArrayList<>(s)));

        }

        @Override
        public EffectItemReward createEffect() {
            return new EffectItemReward();
        }

    }

    private static class Stats extends EffectEditor<EffectStats> {
        LabeledSelection<Boolean> perc;
        LabeledTextBox name;
        RealBox val;

        public Stats(Widget panel, String lbl, EffectStats e, Consumer<EffectStats> cb) {
            super(panel, lbl, e, cb);
            perc = LabeledSelection.createBool(this, "percent", this.e.isPercentage);
            name = new LabeledTextBox(this, "Variant name", this.e.vars);
            val = new RealBox(this, "Variant add", this.e.val);

        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(perc);
            add(name);
            add(val);

        }

        @Override
        public EffectStats createEffect() {
            return new EffectStats();
        }

        @Override
        public void onClose() {
            e.isPercentage = perc.getSelection();
            e.val = val.getNum();
            e.vars = name.getText();
            super.onClose();
        }

    }

    private static class Use extends EffectEditor<EffectUse> {

        public Use(Widget panel, String lbl, EffectUse e, Consumer<EffectUse> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new LabeledOpenEditorButton<>(this, e.blocks.isEmpty() ? "" : e.blocks.get(0).getTranslatedName().getString(), "Edit blocks", SelectItemStackDialog.BLOCK_LIST, e.blocks, e.blocks.isEmpty() ? Icon.EMPTY : ItemIcon.getItemIcon(e.blocks.get(0).asItem()), s -> e.blocks = new ArrayList<>(s)));

        }

        @Override
        public EffectUse createEffect() {
            return new EffectUse();
        }

    }
}
