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

package com.teammoeg.frostedresearch.research.effects;

import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.editor.*;
import com.teammoeg.chorda.client.cui.editor.compat.IEEditors;
import com.teammoeg.chorda.client.icon.CIconFTBWrapper;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.IconEditor;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class EffectEditor<T extends Effect> extends BaseEditDialog {
    public static final Editor<EffectBuilding> BUILD = (p, l, v, c) -> new Building(p, l, v, c).open();
    public static final Editor<EffectCrafting> CRAFT = (p, l, v, c) -> new Crafting(p, l, v, c).open();
    public static final Editor<EffectItemReward> ITEM = (p, l, v, c) -> new ItemReward(p, l, v, c).open();
    public static final Editor<EffectStats> STATS = (p, l, v, c) -> new Stats(p, l, v, c).open();
    public static final Editor<EffectUse> USE = (p, l, v, c) -> new Use(p, l, v, c).open();
    public static final Editor<EffectShowCategory> CAT = (p, l, v, c) -> new Category(p, l, v, c).open();
    public static final Editor<EffectCommand> COMMAND = (p, l, v, c) -> new Command(p, l, v, c).open();
    public static final Editor<EffectExperience> EXP = (p, l, v, c) -> new Exp(p, l, v, c).open();
    public static final Editor<EffectCustom> CUSTOM= (p,l,v,c) -> new Custom(p,l,v,c).open();
    public static final Editor<Effect> EDITOR = 
    	new EditorSelector.EditorSelectorBuilder<Effect>() 
        .addEditor("Building", BUILD,v->v instanceof EffectBuilding)
        .addEditor("Craft", CRAFT,v->v instanceof EffectCrafting)
        .addEditor("Item Reward", ITEM,v->v instanceof EffectItemReward)
        .addEditor("Add Stats", STATS,v->v instanceof EffectStats)
        .addEditor("Add Usage", USE,v->v instanceof EffectUse)
        .addEditor("Recipe Category", CAT,v->v instanceof EffectShowCategory)
        .addEditor("Add Command", COMMAND,v->v instanceof EffectCommand)
        .addEditor("Add Experience", EXP,v->v instanceof EffectExperience)
        .addEditor("Custom",CUSTOM,v->v instanceof EffectCustom)
        .build();
    public static final Editor<Collection<Effect>> EFFECT_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, null, EffectEditor.EDITOR, e->Components.str(e.getBrief()), Effect::getIcon, c).open();
    protected LabeledTextBoxAndBtn nonce;
    protected LabeledTextBox name;
    protected LabeledSelection<Boolean> sd;
    Component lbl;
    T e;
    Consumer<T> cb;

    public EffectEditor(UIWidget panel, Component lbl, T e, Consumer<T> cb) {
        super(panel);

        this.lbl = lbl;


        if (e == null) {
            e = createEffect();
        }
        this.e = e;
        nonce = new LabeledTextBoxAndBtn(this, Components.str("nonce"), e.getNonce(),Components.str("Random"), t -> t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
        name = new LabeledTextBox(this, Components.str("name"), e.name);
        sd = LabeledSelection.createBool(this, Components.str("Hide"), e.isHidden());
        this.cb = cb;
    }

    @Override
    public void addUIElements() {
        add(EditUtils.getTitle(this, lbl));
        add(nonce);
        add(name);
        add(new OpenEditorButton<>(this, Components.str("Edit Description"), EditListDialog.STRING_LIST, e.tooltip, s -> e.tooltip = new ArrayList<>(s)));
        add(new OpenEditorButton<>(this, Components.str("Edit icon"), IconEditor.EDITOR, e.icon == null ? e.getDefaultIcon() : e.icon, s -> e.icon = s));
        add(sd);
    }

    public abstract T createEffect();

    @Override
    public void onClose() {
        e.name = name.getText();
        e.hidden = sd.getSelection();
        e.setNonce(nonce.getText());

        cb.accept(e);

    }

    private static class Building extends EffectEditor<EffectBuilding> {

        public Building(UIWidget panel, Component lbl, EffectBuilding e, Consumer<EffectBuilding> cb) {
            super(panel, lbl, e, cb);

        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new LabeledOpenEditorButton<>(this, e.multiblock == null ? Components.empty() : e.multiblock.getDisplayName(), Components.str("Select Multiblock"), IEEditors.EDITOR_MULTIBLOCK, e.multiblock, s -> e.multiblock = s));

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

        public Category(UIWidget panel, Component lbl, EffectShowCategory e, Consumer<EffectShowCategory> cb) {
            super(panel, lbl, e, cb);
            category = new LabeledTextBox(this, Components.str("category id"), this.e.cate == null ? "" : this.e.cate.toString());
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
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

    private static class Command extends EffectEditor<EffectCommand> {
        public Command(UIWidget panel, Component lbl, EffectCommand e, Consumer<EffectCommand> cb) {
            super(panel, lbl, e, cb);

        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new OpenEditorButton<>(this, Components.str("Edit Commands"), EditListDialog.STRING_LIST, e.rewards, s -> e.rewards = new ArrayList<>(s)));
        }

        @Override
        public EffectCommand createEffect() {
            return new EffectCommand();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

    }

    private static class Crafting extends EffectEditor<EffectCrafting> {

        public Crafting(UIWidget panel, Component lbl, EffectCrafting e, Consumer<EffectCrafting> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(EditUtils.getTitle(this, "Only the first in the following takes effects"));
            add(new OpenEditorButton<Ingredient>(this, Components.str("Edit Item"),
                    IngredientEditor.EDITOR_INGREDIENT,
                    e.ingredient, s -> {
                e.ingredient = s;
            }));
            add(new OpenEditorButton<>(this, Components.str("Edit Recipe IDs"), EditListDialog.STRING_LIST, e.unlocks.stream().map(Recipe::getId).map(String::valueOf).collect(Collectors.toList()), s -> {
                e.setList(s);
                e.ingredient = null;

            }));

        }

        @Override
        public EffectCrafting createEffect() {
            return new EffectCrafting();
        }

    }

    private static class Exp extends EffectEditor<EffectExperience> {
        NumberBox val;

        public Exp(UIWidget panel, Component lbl, EffectExperience e, Consumer<EffectExperience> cb) {
            super(panel, lbl, e, cb);
            val = new NumberBox(this,Components.str("Exp"), this.e.exp);

        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(val);

        }

        @Override
        public EffectExperience createEffect() {
            return new EffectExperience(0);
        }

        @Override
        public void onClose() {
            e.exp = (int) val.getNum();
            super.onClose();
        }

    }

    private static class ItemReward extends EffectEditor<EffectItemReward> {
        public ItemReward(UIWidget panel, Component lbl, EffectItemReward e, Consumer<EffectItemReward> cb) {
            super(panel, lbl, e, cb);
        }

        private static MutableComponent fromItemStack(ItemStack s) {
            return Components.empty().append(s.getHoverName()).append(" x " + s.getCount());
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new LabeledOpenEditorButton<>(this, !e.rewards.isEmpty() ? fromItemStack(e.rewards.get(0)) : Components.empty(), Components.str("Edit Rewards"), Editors.STACK_LIST, e.rewards, s -> e.rewards = new ArrayList<>(s)));

        }

        @Override
        public EffectItemReward createEffect() {
            return new EffectItemReward();
        }

        @Override
        public void onClose() {
            super.onClose();
//            System.out.println(e.rewards);
        }
    }

    private static class Stats extends EffectEditor<EffectStats> {
        LabeledSelection<Boolean> perc;
        LabeledTextBox name;
        RealBox val;

        public Stats(UIWidget panel, Component lbl, EffectStats e, Consumer<EffectStats> cb) {
            super(panel, lbl, e, cb);
            perc = LabeledSelection.createBool(this, Components.str("percent"), this.e.isPercentage);
            name = new LabeledTextBox(this, Components.str("Variant name"), this.e.vars);
            val = new RealBox(this, Components.str("Variant add"), this.e.val);

        }

        @Override
        public void addUIElements() {
            super.addUIElements();
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

        public Use(UIWidget panel, Component lbl, EffectUse e, Consumer<EffectUse> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new LabeledOpenEditorButton<>(this, e.blocks.isEmpty() ? Components.empty() : e.blocks.get(0).getName(),
            	Components.str("Edit blocks"), Editors.BLOCK_LIST, e.blocks,e.blocks.isEmpty() ? CIcons.nop() : CIcons.getIcon(e.blocks),
            		s -> e.blocks = new ArrayList<>(s)));

        }

        @Override
        public EffectUse createEffect() {
            return new EffectUse();
        }

    }
    private static class Custom extends EffectEditor<EffectCustom> {

        public Custom(UIWidget panel, Component lbl, EffectCustom e, Consumer<EffectCustom> cb) {
            super(panel, lbl, e, cb);
        }


        @Override
        public EffectCustom createEffect() {
            return new EffectCustom();
        }

    }
}
