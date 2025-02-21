package com.teammoeg.chorda.client.icon;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.editor.BaseEditDialog;
import com.teammoeg.chorda.client.cui.editor.EditListDialog;
import com.teammoeg.chorda.client.cui.editor.EditPrompt;
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorSelector;
import com.teammoeg.chorda.client.cui.editor.IngredientEditor;
import com.teammoeg.chorda.client.cui.editor.LabeledTextBox;
import com.teammoeg.chorda.client.cui.editor.NumberBox;
import com.teammoeg.chorda.client.cui.editor.OpenEditorButton;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;
import com.teammoeg.chorda.client.cui.editor.SelectStackDialog;
import com.teammoeg.chorda.client.icon.CIcons.AnimatedIcon;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.CombinedIcon;
import com.teammoeg.chorda.client.icon.CIcons.FHDelegateIcon;
import com.teammoeg.chorda.client.icon.CIcons.IngredientIcon;
import com.teammoeg.chorda.client.icon.CIcons.ItemIcon;
import com.teammoeg.chorda.client.icon.CIcons.NopIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextureIcon;
import com.teammoeg.chorda.client.icon.CIcons.TextureUVIcon;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class IconEditor<T extends CIcon> extends BaseEditDialog {

    public static final Editor<ItemIcon> ITEM_EDITOR = (p, l, v, c) -> SelectStackDialog.EDITOR.open(p, l, v == null ? null : v.stack, e -> c.accept(new ItemIcon(e)));
    public static final Editor<TextureIcon> TEXTURE_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? "" : (v.rl == null ? "" : v.rl.toString()),
            e -> c.accept(new TextureIcon(new ResourceLocation(e))));
    public static final Editor<IngredientIcon> INGREDIENT_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR_INGREDIENT_EXTERN.open(p, l, v == null ? null : v.igd,
            e -> c.accept(new IngredientIcon(e)));       
    public static final Editor<CIcon> EDITOR =
            new EditorSelector.EditorSelectorBuilder<CIcon>(v->v==null||v==CIcons.nop())
            .addEditorWhenEmpty("Empty", IconEditor.NOP_EDITOR)
            .addEditor("ItemStack", IconEditor.ITEM_EDITOR,v->v.getClass()==ItemIcon.class)
            .addEditor("Texture", IconEditor.TEXTURE_EDITOR,v->v.getClass()==TextureIcon.class)
            .addEditor("Texture with UV", IconEditor.UV_EDITOR,v->v.getClass()==TextureIcon.class)
            .addEditor("Text", IconEditor.TEXT_EDITOR,v->v.getClass()==TextIcon.class)
            .addEditor("Ingredient", IconEditor.INGREDIENT_EDITOR,v->v.getClass()==IngredientIcon.class)
            .addEditorWhenEmpty("IngredientWithSize", IconEditor.INGREDIENT_SIZE_EDITOR)
            .addEditor("Internal", IconEditor.INTERNAL_EDITOR,v->v instanceof FHDelegateIcon)
            .addEditor("Combined", IconEditor.COMBINED_EDITOR,v->v.getClass()== CombinedIcon.class)
            .addEditor("Animated", IconEditor.ANIMATED_EDITOR,v->v.getClass()== AnimatedIcon.class)
            .build();

    ;
    public static final Editor<CIcon> INGREDIENT_SIZE_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR.open(p, l, null, e -> c.accept(CIcons.getIcon(e.getFirst(),e.getSecond())));
    public static final Editor<TextIcon> TEXT_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? null : v.text.getString(), e -> c.accept(new TextIcon(StringTextComponentParser.parse(e))));
    public static final Editor<CIcon> NOP_EDITOR = (p, l, v, c) -> {
        c.accept(NopIcon.INSTANCE);
        p.refresh();
 
    };
    public static final Editor<CombinedIcon> COMBINED_EDITOR = (p, l, v, c) -> new Combined(p, l, v, c).open();
    public static final Editor<FHDelegateIcon> INTERNAL_EDITOR = (p, l, v, c) -> new SelectDialog<String>(p, l, v == null ? null : v.name, o -> c.accept(new FHDelegateIcon(o)), TechIcons.internals::keySet, Components::str, e -> new String[]{e}, TechIcons.internals::get).open();
    public static final Editor<TextureUVIcon> UV_EDITOR = (p, l, v, c) -> new UV(p, l, v, c).open();
    T v;
    public IconEditor(UIWidget panel, T v) {
        super(panel);
        this.v = v;
    }        public static final Editor<CIcon> NOP_CHANGE_EDITOR = (p, l, v, c) -> EDITOR.open(p, l, null, c);

    @Override
    public void render(GuiGraphics arg0, int arg2, int arg3, int arg4, int arg5) {
        super.render(arg0,arg2 + 300, arg3 + 20, 32, 32);
    }
    public static final Editor<AnimatedIcon> ANIMATED_EDITOR = (p, l, v, c) -> new EditListDialog<>(p, l, v == null ? null : v.icons, null, EDITOR, e -> Components.str(e.getClass().getSimpleName()),
            e -> e, e -> c.accept(new AnimatedIcon(e.toArray(new CIcon[0])))).open();

    private static class Combined extends IconEditor<CombinedIcon> {
        Component label;
        Consumer<CombinedIcon> i;
        public Combined(UIWidget panel, Component label, CombinedIcon v, Consumer<CombinedIcon> i) {
            super(panel, v == null ? new CombinedIcon(null, null) : v);
            this.label = label;
            this.i = i;
        }

        @Override
        public void addUIElements() {
            add(EditUtils.getTitle(this, label));
            add(new OpenEditorButton<>(this, Components.str("Edit base icon"), EDITOR, v.large, e -> v.large = e));
            add(new OpenEditorButton<>(this, Components.str("Edit corner icon"), EDITOR, v.small, e -> v.small = e));
        }

        @Override
        public void onClose() {
            i.accept(v);

        }

    }

    private static class UV extends IconEditor<TextureUVIcon> {
    	Component label;
        Consumer<TextureUVIcon> i;
        LabeledTextBox rl;
        NumberBox x;
        NumberBox y;
        NumberBox w;
        NumberBox h;
        NumberBox tw;
        NumberBox th;

        public UV(UIWidget panel, Component label, TextureUVIcon v, Consumer<TextureUVIcon> i) {
            super(panel, v == null ? new TextureUVIcon() : v);
            this.label = label;
            this.i = i;
            v = this.v;
            rl = new LabeledTextBox(this, Components.str("Texture"), this.v.rl == null ? "" : this.v.rl.toString());
            x = new NumberBox(this, Components.str("X"), (v.x));
            y = new NumberBox(this, Components.str("Y"), (v.y));
            w = new NumberBox(this, Components.str("Width"), (v.w));
            h = new NumberBox(this, Components.str("Height"), (v.h));
            tw = new NumberBox(this, Components.str("Texture Width"), (v.tw));
            th = new NumberBox(this, Components.str("Texture Height"), (v.th));
        }

        @Override
        public void addUIElements() {
            add(EditUtils.getTitle(this, label));
            add(rl);
            add(x);
            add(y);
            add(w);
            add(h);
            add(tw);
            add(th);
            add(new TextButton(this, Components.str("Commit"), CIcons.nop()) {
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