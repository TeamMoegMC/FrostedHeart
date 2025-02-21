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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.CIconFTBWrapper;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.gui.editor.*;
import com.teammoeg.frostedheart.content.research.research.Research;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ClueEditor<T extends Clue> extends BaseEditDialog {

    public static final Editor<ItemClue> ITEM = (p, l, v, c) -> new Item(p, l, v, c).open();
    public static final Editor<CustomClue> CUSTOM = (p, l, v, c) -> new Custom(p, l, v, c).open();
    public static final Editor<AdvancementClue> ADVA = (p, l, v, c) -> new Advancement(p, l, v, c).open();
    public static final Editor<KillClue> KILL = (p, l, v, c) -> new Kill(p, l, v, c).open();
    public static final Editor<MinigameClue> GAME = (p, l, v, c) -> new Minigame(p, l, v, c).open();
    public static final Editor<Research> RESEARCH_GAME = (p, l, v, c) -> {
        MinigameClue ex = null;
        List<Clue> clues = v.getClues();
        for (Clue cl : clues) {
            if (cl instanceof MinigameClue) {
                ex = (MinigameClue) cl;
                break;
            }
        }
        final MinigameClue fex = ex;
        new Minigame(p, l, ex, e -> {
            if (fex == null)
                clues.add(e);
        }).open();
    };
    public static final Editor<Clue> EDITOR = new EditorSelector.EditorSelectorBuilder<Clue>()
        .addEditor("Submit Item", ITEM,v->v instanceof ItemClue)
        .addEditor("Triger in program", CUSTOM,v->v instanceof CustomClue)
        .addEditor("Advancement", ADVA,v->v instanceof AdvancementClue)
        .addEditor("Kill Entity", KILL,v->v instanceof KillClue)
        .addEditor("Complete minigame", GAME,v->v instanceof MinigameClue)
        .build();
    public static final Editor<Collection<Clue>> EDITOR_LIST = (p, l, v, c) -> new EditListDialog<Clue>(p, l, v, EDITOR, e -> e.getBrief() + e.getBriefDesc(), c).open();
    protected LabeledTextBoxAndBtn nonce;
    protected LabeledTextBox name;
    protected LabeledTextBox desc;
    protected LabeledTextBox hint;
    protected RealBox cont;
    protected LabeledSelection<Boolean> req;
    Component lbl;
    T e;
    Consumer<T> cb;
    public ClueEditor(UIWidget panel, Component lbl, T e, Consumer<T> cb) {
        super(panel);

        this.lbl = lbl;


        if (e == null) {
            e = createClue();
        }
        this.e = e;
        nonce = new LabeledTextBoxAndBtn(this, Components.str("nonce"), e != null ? e.getNonce() : "", Components.str("Random"), t -> t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
        name = new LabeledTextBox(this, Components.str("Name"), e.name);
        desc = new LabeledTextBox(this, Components.str("Description"), e.desc);
        hint = new LabeledTextBox(this, Components.str("Hint"), e.hint);
        cont = new RealBox(this, Components.str("Contribution(%)"), e.contribution * 100);
        req = LabeledSelection.createBool(this, Components.str("Required"), e.required);
        this.cb = cb;
    }

    public static Collection<ClueClosure> generate(Research r) {
        return r.getClues().stream().map(t -> new ClueClosure(r, t)).collect(Collectors.toList());
    }

    @Override
    public void addUIElements() {
        add(ResearchEditUtils.getTitle(this, lbl));
        add(nonce);
        add(name);
        add(desc);
        add(hint);
        add(cont);
        add(req);
    }

    public abstract T createClue();

    @Override
    public void onClose() {
        e.name = name.getText();
        e.desc = desc.getText();
        e.hint = hint.getText();
        e.contribution = (float) cont.getNum() / 100f;
        e.setNonce(nonce.getText());
        e.required = req.getSelection();
        cb.accept(e);

    }

    private static class Advancement extends Listener<AdvancementClue> {

        public Advancement(UIWidget panel, Component lbl, AdvancementClue e, Consumer<AdvancementClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new LabeledOpenEditorButton<>(this,Components.str( e.advancement.toString()), Components.str("Select Advancement"), SelectDialog.EDITOR_ADVANCEMENT, e.advancement, c -> e.advancement = c));
            add(LabeledSelection.createCriterion(this, Components.str("Select Criterion"), e.advancement, e.criterion, c -> e.criterion = c));

        }

        @Override
        public AdvancementClue createClue() {
            return new AdvancementClue();
        }
    }

    private static class Custom extends ClueEditor<CustomClue> {

        public Custom(UIWidget panel, Component lbl, CustomClue e, Consumer<CustomClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public CustomClue createClue() {
            return new CustomClue();
        }
    }

    private static class Item extends ClueEditor<ItemClue> {
        LabeledSelection<Boolean> cons;

        public Item(UIWidget panel, Component lbl, ItemClue e, Consumer<ItemClue> cb) {
            super(panel, lbl, e, cb);
            cons = LabeledSelection.createBool(this, Components.str("Consumes item"), this.e.consume);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            
            add(new OpenEditorButton<Pair<Ingredient,Integer>>(this, Components.str("Select Ingredient"), IngredientEditor.EDITOR, 
            	e.stack,(e.stack == null ? CIcons.nop() : CIcons.getIcon(e.stack.getFirst(),e.stack.getSecond())),
            	c -> e.stack = c));
            add(cons);
        }

        @Override
        public ItemClue createClue() {
            return new ItemClue();
        }

        @Override
        public void onClose() {
            e.consume = cons.getSelection();
            super.onClose();
        }
    }

    private static class Kill extends Listener<KillClue> {

        public Kill(UIWidget panel, Component lbl, KillClue e, Consumer<KillClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(new LabeledOpenEditorButton<>(this, e.type == null ? Components.empty() : e.type.getDescription(), Components.str("Select Entity"), SelectDialog.EDITOR_ENTITY, e.type, c -> {
                e.type = c;
                desc.setText("@" + c.getDescriptionId());
            }));
        }

        @Override
        public KillClue createClue() {
            return new KillClue();
        }

    }

    private static abstract class Listener<U extends ListenerClue> extends ClueEditor<U> {
        LabeledSelection<Boolean> aa;

        public Listener(UIWidget panel, Component lbl, U e, Consumer<U> cb) {
            super(panel, lbl, e, cb);
            aa = LabeledSelection.createBool(this, Components.str("Listen even not active"), this.e.alwaysOn);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(aa);
        }

        @Override
        public void onClose() {
            e.alwaysOn = aa.getSelection();
            super.onClose();
        }


    }

    private static class Minigame extends ClueEditor<MinigameClue> {
        LabeledSelection<Integer> lvl;

        public Minigame(UIWidget panel, Component lbl, MinigameClue e, Consumer<MinigameClue> cb) {
            super(panel, lbl, e, cb);
            lvl = new LabeledSelection<>(this, Components.str("Level"), this.e.getLevel(), Arrays.asList(0, 1, 2, 3), String::valueOf);
        }

        @Override
        public void addUIElements() {
            super.addUIElements();
            add(lvl);
        }

        @Override
        public MinigameClue createClue() {
            return new MinigameClue();
        }

        @Override
        public void onClose() {
            e.setLevel(lvl.getSelection());
            super.onClose();
        }


    }
}
