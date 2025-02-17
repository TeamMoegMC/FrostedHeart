package com.teammoeg.chorda.client.icon;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.FHIconWrapper;
import com.teammoeg.chorda.client.icon.CIcons.AnimatedIcon;
import com.teammoeg.chorda.client.icon.CIcons.CombinedIcon;
import com.teammoeg.chorda.client.icon.CIcons.FHDelegateIcon;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.IngredientIcon;
import com.teammoeg.chorda.client.icon.CIcons.ItemIcon;
import com.teammoeg.chorda.client.icon.CIcons.NopIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextureIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextureUVIcon;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.gui.editor.BaseEditDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditListDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditPrompt;
import com.teammoeg.frostedheart.content.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.content.research.gui.editor.Editor;
import com.teammoeg.frostedheart.content.research.gui.editor.EditorSelector;
import com.teammoeg.frostedheart.content.research.gui.editor.IngredientEditor;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledTextBox;
import com.teammoeg.frostedheart.content.research.gui.editor.NumberBox;
import com.teammoeg.frostedheart.content.research.gui.editor.OpenEditorButton;
import com.teammoeg.frostedheart.content.research.gui.editor.SelectDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.SelectItemStackDialog;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public abstract class IconEditor<T extends CIcon> extends BaseEditDialog {

    public static final Editor<ItemIcon> ITEM_EDITOR = (p, l, v, c) -> SelectItemStackDialog.EDITOR.open(p, l, v == null ? null : v.stack, e -> c.accept(new ItemIcon(e)));
    public static final Editor<TextureIcon> TEXTURE_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? "" : (v.rl == null ? "" : v.rl.toString()),
            e -> c.accept(new TextureIcon(new ResourceLocation(e))));
    public static final Editor<IngredientIcon> INGREDIENT_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR_INGREDIENT_EXTERN.open(p, l, v == null ? null : v.igd,
            e -> c.accept(new IngredientIcon(e)));        public static final Editor<CIcon> EDITOR = (p, l, v, c) -> {
        if (v == null || v instanceof NopIcon) {
            new EditorSelector<>(p, l, c).addEditor("Empty", IconEditor.NOP_EDITOR)
                    .addEditor("ItemStack", IconEditor.ITEM_EDITOR).addEditor("Texture", IconEditor.TEXTURE_EDITOR)
                    .addEditor("Texture with UV", IconEditor.UV_EDITOR).addEditor("Text", IconEditor.TEXT_EDITOR)
                    .addEditor("Ingredient", IconEditor.INGREDIENT_EDITOR)
                    .addEditor("IngredientWithSize", IconEditor.INGREDIENT_SIZE_EDITOR)
                    .addEditor("Internal", IconEditor.INTERNAL_EDITOR)
                    .addEditor("Combined", IconEditor.COMBINED_EDITOR)
                    .addEditor("Animated", IconEditor.ANIMATED_EDITOR).open();
        } else
            new EditorSelector<>(p, l, (o, t) -> true, v, c).addEditor("Edit", IconEditor.CHANGE_EDITOR)
                    .addEditor("New", IconEditor.NOP_CHANGE_EDITOR).open();
    };
    public static final Editor<CIcon> INGREDIENT_SIZE_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR.open(p, l, null, e -> c.accept(CIcons.getIcon(e.getBaseIngredient(),e.getCount())));
    public static final Editor<CIcon> CHANGE_EDITOR = (p, l, v, c) -> {
        if (v instanceof ItemIcon) {
            IconEditor.ITEM_EDITOR.open(p, l, (ItemIcon) v, c::accept);
        } else if (v instanceof CombinedIcon) {
            IconEditor.COMBINED_EDITOR.open(p, l, (CombinedIcon) v, c::accept);
        } else if (v instanceof IngredientIcon) {
            IconEditor.INGREDIENT_EDITOR.open(p, l, (IngredientIcon) v, c::accept);
        } else if (v instanceof AnimatedIcon) {
            IconEditor.ANIMATED_EDITOR.open(p, l, (AnimatedIcon) v, c::accept);
        } else if (v instanceof TextureIcon) {
            IconEditor.TEXTURE_EDITOR.open(p, l, (TextureIcon) v, c::accept);
        } else if (v instanceof TextureUVIcon) {
            IconEditor.UV_EDITOR.open(p, l, (TextureUVIcon) v, e -> c.accept(v));
        } else if (v instanceof TextIcon) {
            IconEditor.TEXT_EDITOR.open(p, l, (TextIcon) v, c::accept);
        } else if (v instanceof FHDelegateIcon) {
            IconEditor.INTERNAL_EDITOR.open(p, l, (FHDelegateIcon) v, c::accept);
        } else
            IconEditor.NOP_CHANGE_EDITOR.open(p, l, v, c);
    };
    public static final Editor<TextIcon> TEXT_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? null : v.text.getString(), e -> c.accept(new TextIcon(ClientTextComponentUtils.parse(e))));
    public static final Editor<CIcon> NOP_EDITOR = (p, l, v, c) -> {
        c.accept(NopIcon.INSTANCE);
        p.getGui().refreshWidgets();
    };
    public static final Editor<CombinedIcon> COMBINED_EDITOR = (p, l, v, c) -> new Combined(p, l, v, c).open();
    public static final Editor<FHDelegateIcon> INTERNAL_EDITOR = (p, l, v, c) -> new SelectDialog<>(p, l, v == null ? null : v.name, o -> c.accept(new FHDelegateIcon(o)), TechIcons.internals::keySet, Components::str, e -> new String[]{e}, TechIcons.internals::get).open();
    public static final Editor<TextureUVIcon> UV_EDITOR = (p, l, v, c) -> new UV(p, l, v, c).open();
    T v;
    public IconEditor(Widget panel, T v) {
        super(panel);
        this.v = v;
    }        public static final Editor<CIcon> NOP_CHANGE_EDITOR = (p, l, v, c) -> EDITOR.open(p, l, null, c);

    @Override
    public void draw(GuiGraphics arg0, Theme arg1, int arg2, int arg3, int arg4, int arg5) {
        super.draw(arg0, arg1, arg2, arg3, arg4, arg5);
        v.draw(arg0, arg2 + 300, arg3 + 20, 32, 32);
    }        public static final Editor<AnimatedIcon> ANIMATED_EDITOR = (p, l, v, c) -> new EditListDialog<>(p, l, v == null ? null : v.icons, null, EDITOR, e -> e.getClass().getSimpleName(),
            e -> new FHIconWrapper(e), e -> c.accept(new AnimatedIcon(e.toArray(new CIcon[0])))).open();

    private static class Combined extends IconEditor<CombinedIcon> {
        String label;
        Consumer<CombinedIcon> i;
        public Combined(Widget panel, String label, CombinedIcon v, Consumer<CombinedIcon> i) {
            super(panel, v == null ? new CombinedIcon(null, null) : v);
            this.label = label;
            this.i = i;
        }

        @Override
        public void addWidgets() {
            add(EditUtils.getTitle(this, label));
            add(new OpenEditorButton<>(this, "Edit base icon", EDITOR, v.large, e -> v.large = e));
            add(new OpenEditorButton<>(this, "Edit corner icon", EDITOR, v.small, e -> v.small = e));
        }

        @Override
        public void onClose() {
            i.accept(v);

        }

    }

    private static class UV extends IconEditor<TextureUVIcon> {
        String label;
        Consumer<TextureUVIcon> i;
        LabeledTextBox rl;
        NumberBox x;
        NumberBox y;
        NumberBox w;
        NumberBox h;
        NumberBox tw;
        NumberBox th;

        public UV(Widget panel, String label, TextureUVIcon v, Consumer<TextureUVIcon> i) {
            super(panel, v == null ? new TextureUVIcon() : v);
            this.label = label;
            this.i = i;
            v = this.v;
            rl = new LabeledTextBox(this, "Texture", this.v.rl == null ? "" : this.v.rl.toString());
            x = new NumberBox(this, "X", (v.x));
            y = new NumberBox(this, "Y", (v.y));
            w = new NumberBox(this, "Width", (v.w));
            h = new NumberBox(this, "Height", (v.h));
            tw = new NumberBox(this, "Texture Width", (v.tw));
            th = new NumberBox(this, "Texture Height", (v.th));
        }

        @Override
        public void addWidgets() {
            add(EditUtils.getTitle(this, label));
            add(rl);
            add(x);
            add(y);
            add(w);
            add(h);
            add(tw);
            add(th);
            add(new SimpleTextButton(this, Components.str("Commit"), Icon.empty()) {
                @Override
                public void onClicked(MouseButton arg0) {
                    v.rl = new ResourceLocation(rl.getText());
                    v.x = (int) x.getNum();
                    v.y = (int) y.getNum();
                    v.w = (int) w.getNum();
                    v.h = (int) h.getNum();
                    v.tw = (int) tw.getNum();
                    v.th = (int) th.getNum();

                }

            });
        }

        @Override
        public void onClose() {

            v.rl = new ResourceLocation(rl.getText());
            v.x = (int) x.getNum();
            v.y = (int) y.getNum();
            v.w = (int) w.getNum();
            v.h = (int) h.getNum();
            v.tw = (int) tw.getNum();
            v.th = (int) th.getNum();
            i.accept(v);

        }

    }









}