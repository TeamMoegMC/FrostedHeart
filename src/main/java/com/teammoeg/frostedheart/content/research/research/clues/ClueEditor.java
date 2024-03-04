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

package com.teammoeg.frostedheart.content.research.research.clues;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.editor.BaseEditDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditListDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.content.research.gui.editor.Editor;
import com.teammoeg.frostedheart.content.research.gui.editor.EditorSelector;
import com.teammoeg.frostedheart.content.research.gui.editor.IngredientEditor;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledOpenEditorButton;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledSelection;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledTextBox;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledTextBoxAndBtn;
import com.teammoeg.frostedheart.content.research.gui.editor.OpenEditorButton;
import com.teammoeg.frostedheart.content.research.gui.editor.RealBox;
import com.teammoeg.frostedheart.content.research.gui.editor.SelectDialog;
import com.teammoeg.frostedheart.content.research.research.Research;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Widget;

public abstract class ClueEditor<T extends Clue> extends BaseEditDialog {
    private static class Advancement extends Listener<AdvancementClue> {

        public Advancement(Widget panel, String lbl, AdvancementClue e, Consumer<AdvancementClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new LabeledOpenEditorButton<>(this, e.advancement.toString(), "Select Advancement", SelectDialog.EDITOR_ADVANCEMENT, e.advancement, c -> {

                e.advancement = c;
            }));
            add(LabeledSelection.createCriterion(this, "Select Criterion", e.advancement, e.criterion, c -> e.criterion = c));

        }

        @Override
        public AdvancementClue createClue() {
            return new AdvancementClue();
        }
    }
    private static class Custom extends ClueEditor<CustomClue> {

        public Custom(Widget panel, String lbl, CustomClue e, Consumer<CustomClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public CustomClue createClue() {
            return new CustomClue();
        }
    }
    private static class Item extends ClueEditor<ItemClue> {
        LabeledSelection<Boolean> cons;

        public Item(Widget panel, String lbl, ItemClue e, Consumer<ItemClue> cb) {
            super(panel, lbl, e, cb);
            cons = LabeledSelection.createBool(this, "Consumes item", this.e.consume);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new OpenEditorButton<>(this, "Select Ingredient", IngredientEditor.EDITOR, e.stack, e.stack == null ? Icon.EMPTY : FHIcons.getIcon(e.stack), c -> e.stack = c));
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

        public Kill(Widget panel, String lbl, KillClue e, Consumer<KillClue> cb) {
            super(panel, lbl, e, cb);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
            add(new LabeledOpenEditorButton<>(this, e.type == null ? "" : e.type.getName().getString(), "Select Entity", SelectDialog.EDITOR_ENTITY, e.type, c -> {
                e.type = c;
                desc.setText("@" + c.getTranslationKey());
            }));
        }

        @Override
        public KillClue createClue() {
            return new KillClue();
        }

    }
    private static abstract class Listener<U extends ListenerClue> extends ClueEditor<U> {
        LabeledSelection<Boolean> aa;

        public Listener(Widget panel, String lbl, U e, Consumer<U> cb) {
            super(panel, lbl, e, cb);
            aa = LabeledSelection.createBool(this, "Listen even not active", this.e.alwaysOn);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
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

        public Minigame(Widget panel, String lbl, MinigameClue e, Consumer<MinigameClue> cb) {
            super(panel, lbl, e, cb);
            lvl = new LabeledSelection<>(this, "Level", this.e.getLevel(), Arrays.asList(0, 1, 2, 3), String::valueOf);
        }

        @Override
        public void addWidgets() {
            super.addWidgets();
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
    public static final Editor<ItemClue> ITEM = (p, l, v, c) -> {
        new Item(p, l, v, c).open();
    };
    public static final Editor<CustomClue> CUSTOM = (p, l, v, c) -> {
        new Custom(p, l, v, c).open();
    };
    public static final Editor<AdvancementClue> ADVA = (p, l, v, c) -> {
        new Advancement(p, l, v, c).open();
    };
    public static final Editor<KillClue> KILL = (p, l, v, c) -> {
        new Kill(p, l, v, c).open();
    };
    public static final Editor<MinigameClue> GAME = (p, l, v, c) -> {
        new Minigame(p, l, v, c).open();
    };
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
    public static final Editor<Clue> EDITOR = (p, l, v, c) -> {
        if (v == null) {
            new EditorSelector<>(p, l, c)
                    .addEditor("Submit Item", ITEM)
                    .addEditor("Triger in program", CUSTOM)
                    .addEditor("Advancement", ADVA)
                    .addEditor("Kill Entity", KILL)
                    .addEditor("Complete minigame", GAME)
                    .open();

        } else if (v instanceof ItemClue)
            ITEM.open(p, l, (ItemClue) v, e -> c.accept(e));
        else if (v instanceof MinigameClue)
            GAME.open(p, l, (MinigameClue) v, e -> c.accept(e));
        else if (v instanceof AdvancementClue)
            ADVA.open(p, l, (AdvancementClue) v, e -> c.accept(e));
        else if (v instanceof KillClue)
            KILL.open(p, l, (KillClue) v, e -> c.accept(e));
        else
            CUSTOM.open(p, l, (CustomClue) v, e -> c.accept(e));
    };
    public static final Editor<Collection<Clue>> EDITOR_LIST = (p, l, v, c) -> {
        new EditListDialog<>(p, l, v, EDITOR, e -> e.getBrief() + e.getBriefDesc(), c).open();
    };
    String lbl;
    T e;
    Consumer<T> cb;

    protected LabeledTextBoxAndBtn nonce;

    protected LabeledTextBox name;

    protected LabeledTextBox desc;

    protected LabeledTextBox hint;

    protected RealBox cont;

    protected LabeledSelection<Boolean> req;

    public ClueEditor(Widget panel, String lbl, T e, Consumer<T> cb) {
        super(panel);

        this.lbl = lbl;


        if (e == null) {
            e = createClue();
        }
        this.e = e;
        nonce = new LabeledTextBoxAndBtn(this, "nonce", e != null ? e.getNonce() : "", "Random", t -> t.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
        name = new LabeledTextBox(this, "Name", e.name);
        desc = new LabeledTextBox(this, "Description", e.desc);
        hint = new LabeledTextBox(this, "Hint", e.hint);
        cont = new RealBox(this, "Contribution(%)", e.contribution * 100);
        req = LabeledSelection.createBool(this, "Required", e.required);
        this.cb = cb;
    }

    @Override
    public void addWidgets() {
        add(EditUtils.getTitle(this, lbl));
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
        if (e.getRId() != 0) {
            e.setNewId(nonce.getText());
        } else {
            e.nonce = nonce.getText();
        }
        e.required = req.getSelection();
        cb.accept(e);

    }
}
