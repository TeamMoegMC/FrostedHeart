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

package com.teammoeg.frostedheart.content.research.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
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
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.io.JsonSerializerRegistry;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.io.Writeable;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

public class FHIcons {

    private static class FHAnimatedIcon extends FHIcon {
        List<FHIcon> icons;

        public FHAnimatedIcon() {
            icons = new ArrayList<>();
        }

        public FHAnimatedIcon(FHIcon[] icons2) {
            this();
            for (FHIcon i : icons2)
                icons.add(i);
        }

        public FHAnimatedIcon(JsonElement elm) {
            if (elm.isJsonObject()) {
                JsonObject jo = elm.getAsJsonObject();
                icons = SerializeUtil.parseJsonElmList(jo.get("icons"), FHIcons::getIcon);
            } else if (elm.isJsonArray()) {
                icons = SerializeUtil.parseJsonElmList(elm, FHIcons::getIcon);
            }
        }

        public FHAnimatedIcon(PacketBuffer buffer) {
            icons = SerializeUtil.readList(buffer, FHIcons::readIcon);
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
            if (icons.size() > 0) {
                GuiHelper.setupDrawing();
                icons.get((int) ((System.currentTimeMillis() / 1000) % icons.size())).draw(ms, x, y, w, h);
            }
        }
        public JsonObject asObject() {
        	JsonObject jo=new JsonObject();
        	jo.add("icons", serialize());
            return jo;
        }
        @Override
        public JsonElement serialize() {

            return SerializeUtil.toJsonList(icons, FHIcon::serialize);
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            SerializeUtil.writeList(buffer, icons, FHIcons::write);
        }
    }

    private static class FHCombinedIcon extends FHIcon {
        FHIcon large;
        FHIcon small;

        public FHCombinedIcon(FHIcon base, FHIcon small) {
            this.large = base;
            this.small = small;
        }

        public FHCombinedIcon(JsonElement elm) {
            if (elm.isJsonObject()) {
                large = FHIcons.getIcon(elm.getAsJsonObject().get("base"));
                small = FHIcons.getIcon(elm.getAsJsonObject().get("small"));
            }
        }

        public FHCombinedIcon(PacketBuffer buffer) {
            large = FHIcons.readIcon(buffer);
            small = FHIcons.readIcon(buffer);
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            if (large != null)
                large.draw(ms, x, y, w, h);
            ms.push();
            ms.translate(0, 0, 110);// let's get top most
            GuiHelper.setupDrawing();
            if (small != null)
                small.draw(ms, x + w / 2, y + h / 2, w / 2, h / 2);
            ms.pop();
        }

        @Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.add("base", large.serialize());
            jo.add("small", small.serialize());
            return jo;
        }
        public JsonObject asObject() {
        	return serialize().getAsJsonObject();
        }
        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            FHIcons.write(small, buffer);
            FHIcons.write(small, buffer);
        }
    }

    private static class FHDelegateIcon extends FHIcon {
        String name;

        public FHDelegateIcon(JsonElement elm) {
            this(elm.getAsJsonObject().get("name").getAsString());
        }

        public FHDelegateIcon(PacketBuffer buffer) {
            this(buffer.readString());
        }

        public FHDelegateIcon(String name) {
            super();
            this.name = name;
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            TechIcons.internals.get(name).draw(ms, x, y, w, h);
        }

        @Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.addProperty("name", name);
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            buffer.writeString(name);
        }

		@Override
		JsonObject asObject() {
			return serialize().getAsJsonObject();
		}
    }

    public static abstract class FHIcon extends Icon implements Cloneable,Writeable {
        public FHIcon() {
            super();
        }

        public JsonElement serialize() {
            JsonObject jo = new JsonObject();
            return jo;
        }
        abstract JsonObject asObject();
        public void write(PacketBuffer buffer) {
        }

        @Override
        public FHIcon clone() {
            try {
                return (FHIcon) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    private static class FHIngredientIcon extends FHAnimatedIcon {
        Ingredient igd;

        public FHIngredientIcon(Ingredient i) {
            igd = i;
            for (ItemStack stack : igd.getMatchingStacks())
                icons.add(new FHItemIcon(stack));
        }

        public FHIngredientIcon(JsonElement elm) {
            this(Ingredient.deserialize(elm.getAsJsonObject().get("ingredient")));
        }

        public FHIngredientIcon(PacketBuffer buffer) {
            this(Ingredient.read(buffer));

        }

        @Override
		public JsonObject asObject() {
			return serialize().getAsJsonObject();
		}

		@Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.add("ingredient", igd.serialize());
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            igd.write(buffer);
        }
    }

    private static class FHItemIcon extends FHIcon {
        ItemStack stack;

        public FHItemIcon(IItemProvider item2) {
            this(new ItemStack(item2));
        }

        public FHItemIcon(ItemStack stack) {
            this.stack = stack;
            init();
        }

        public FHItemIcon(JsonElement elm) {
            if (elm.isJsonPrimitive()) {
                stack = SerializeUtil.fromJson(elm);

            } else if (elm.isJsonObject()) {
                if (elm.getAsJsonObject().has("item")) {
                    JsonElement it = elm.getAsJsonObject().get("item");
                    stack = SerializeUtil.fromJson(it);
                } else
                    stack = SerializeUtil.fromJson(elm);
            }
            init();
        }

        public FHItemIcon(PacketBuffer buffer) {
            stack = SerializeUtil.readOptional(buffer, PacketBuffer::readItemStack).orElse(null);
            init();
        }

        @Override
        public void draw(MatrixStack matrixStack, int x, int y, int w, int h) {
        	//ItemRenderer itemRenderer=ClientUtils.mc().getItemRenderer();
        	/*
            itemRenderer.zLevel = 200.0F;
            net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
            if (font == null) font = ClientUtils.mc().fontRenderer;
            itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
            itemRenderer.renderItemOverlayIntoGUI(font, stack, x, y, null);
            itemRenderer.zLevel = 0.0F;*/
        	matrixStack.push();
        	matrixStack.translate(0,0, 199);
        	GuiHelper.drawItem(matrixStack, stack, x, y, w/16f, h/16f, true, null);
            matrixStack.pop();
            /*ClientUtils.mc().getItemRenderer().renderItem(stack, TransformType.GUI,LightTexture., y, matrixStack, null);
            if (stack != null && stack.getCount() > 1) {
                matrixStack.push();
                matrixStack.translate(x + w - 8, y + h - 7, 199);
                matrixStack.push();
                matrixStack.scale(w / 16f, h / 16f, 0);
                ClientUtils.mc().fontRenderer.drawStringWithShadow(matrixStack, String.valueOf(stack.getCount()), 0, 0,
                        0xffffffff);
                matrixStack.pop();
                matrixStack.pop();
            }*/
        }



        private void init() {
        }
        @Override
		public JsonObject asObject() {
        	JsonObject jo=new JsonObject();
        	jo.add("item", serialize());
			return jo;
		}
        @Override
        public JsonElement serialize() {
            JsonElement je = SerializeUtil.toJson(stack);
            if (je.isJsonPrimitive())
                return je;
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.add("item", je);
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            SerializeUtil.writeOptional2(buffer, stack, PacketBuffer::writeItemStack);
        }
    }

    private static class FHNopIcon extends FHIcon {
        public static final FHNopIcon INSTANCE = new FHNopIcon();

        public static FHNopIcon get(JsonElement e) {
            return INSTANCE;
        }

        public static FHNopIcon get(PacketBuffer e) {
            return INSTANCE;
        }

        private FHNopIcon() {
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
        }

        @Override
        public JsonElement serialize() {
            return JsonNull.INSTANCE;
        }

		@Override
		JsonObject asObject() {
			return new JsonObject();
		}

    }

    private static class FHTextIcon extends FHIcon {
        String text;

        public FHTextIcon(JsonElement elm) {
            this(elm.getAsJsonObject().get("text").getAsString());
        }

        public FHTextIcon(PacketBuffer buffer) {
            this(buffer.readString());
        }

        public FHTextIcon(String text) {
            super();
            this.text = text;
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {

            ms.push();
            ms.translate(x, y, 0);
            ms.scale(w / 16f, h / 16f, 0);

            ms.push();

            ms.scale(2.286f, 2.286f, 0);// scale font height 7 to height 16
            ClientUtils.mc().fontRenderer.drawStringWithShadow(ms, text, 0, 0, 0xFFFFFFFF);
            ms.pop();
            ms.pop();
            GuiHelper.setupDrawing();
        }

        @Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.addProperty("text", text);
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            buffer.writeString(text);
        }

        @Override
		public JsonObject asObject() {
			return serialize().getAsJsonObject();
		}
    }

    private static class FHTextureIcon extends FHIcon {
        Icon nested;
        ResourceLocation rl;

        public FHTextureIcon(JsonElement elm) {
            this(new ResourceLocation(elm.getAsJsonObject().get("location").getAsString()));
        }

        public FHTextureIcon(PacketBuffer buffer) {
            this(buffer.readResourceLocation());
        }

        public FHTextureIcon(ResourceLocation rl) {
            this.rl = rl;
            nested = ImageIcon.getIcon(rl);
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            nested.draw(ms, x, y, w, h);
        }
        @Override
		public JsonObject asObject() {
			return serialize().getAsJsonObject();
		}
        @Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.addProperty("location", rl.toString());
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            buffer.writeResourceLocation(rl);
        }
    }

    private static class FHTextureUVIcon extends FHIcon {
        Icon nested;
        ResourceLocation rl;
        int x, y, w, h, tw, th;

        public FHTextureUVIcon() {
        }

        public FHTextureUVIcon(JsonElement elm) {
            this(new ResourceLocation(elm.getAsJsonObject().get("location").getAsString()),
                    elm.getAsJsonObject().get("x").getAsInt(), elm.getAsJsonObject().get("y").getAsInt(),
                    elm.getAsJsonObject().get("w").getAsInt(), elm.getAsJsonObject().get("h").getAsInt(),
                    JSONUtils.getInt(elm.getAsJsonObject(), "tw", 256),
                    JSONUtils.getInt(elm.getAsJsonObject(), "th", 256));
        }

        public FHTextureUVIcon(PacketBuffer buffer) {
            this(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        public FHTextureUVIcon(ResourceLocation rl, int x, int y, int w, int h, int tw, int th) {
            super();
            this.rl = rl;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.tw = tw;
            this.th = th;
            init();
        }

        @Override
        public void draw(MatrixStack ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            if (nested != null)
                nested.draw(ms, x, y, w, h);
        }

        public void init() {
            nested = ImageIcon.getIcon(rl).withUV(x, y, w, h, tw, th);
        }
        @Override
		public JsonObject asObject() {
			return serialize().getAsJsonObject();
		}
        @Override
        public JsonElement serialize() {
            JsonObject jo = super.serialize().getAsJsonObject();
            jo.addProperty("location", rl.toString());
            jo.addProperty("x", x);
            jo.addProperty("y", y);
            jo.addProperty("w", w);
            jo.addProperty("h", h);
            jo.addProperty("tw", tw);
            jo.addProperty("th", th);
            return jo;
        }

        @Override
        public void write(PacketBuffer buffer) {
            super.write(buffer);
            buffer.writeResourceLocation(rl);
            buffer.writeVarInt(x);
            buffer.writeVarInt(y);
            buffer.writeVarInt(w);
            buffer.writeVarInt(h);
            buffer.writeVarInt(tw);
            buffer.writeVarInt(th);
        }
    }

    public static abstract class IconEditor<T extends FHIcon> extends BaseEditDialog {

        private static class Combined extends IconEditor<FHCombinedIcon> {
            String label;
            Consumer<FHCombinedIcon> i;

            public Combined(Widget panel, String label, FHCombinedIcon v, Consumer<FHCombinedIcon> i) {
                super(panel, v == null ? new FHCombinedIcon(null, null) : v);
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

        private static class UV extends IconEditor<FHTextureUVIcon> {
            String label;
            Consumer<FHTextureUVIcon> i;
            LabeledTextBox rl;
            NumberBox x;
            NumberBox y;
            NumberBox w;
            NumberBox h;
            NumberBox tw;
            NumberBox th;

            public UV(Widget panel, String label, FHTextureUVIcon v, Consumer<FHTextureUVIcon> i) {
                super(panel, v == null ? new FHTextureUVIcon() : v);
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
                add(new SimpleTextButton(this, GuiUtils.str("Commit"), Icon.EMPTY) {
                    @Override
                    public void onClicked(MouseButton arg0) {
                        v.rl = new ResourceLocation(rl.getText());
                        v.x = (int) x.getNum();
                        v.y = (int) y.getNum();
                        v.w = (int) w.getNum();
                        v.h = (int) h.getNum();
                        v.tw = (int) tw.getNum();
                        v.th = (int) th.getNum();
                        v.init();

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
                v.init();
                i.accept(v);

            }

        }
        public static final Editor<FHIcon> EDITOR = (p, l, v, c) -> {
            if (v == null || v instanceof FHNopIcon) {
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
        public static final Editor<FHIcon> CHANGE_EDITOR = (p, l, v, c) -> {
            if (v instanceof FHItemIcon) {
                IconEditor.ITEM_EDITOR.open(p, l, (FHItemIcon) v, c::accept);
            } else if (v instanceof FHCombinedIcon) {
                IconEditor.COMBINED_EDITOR.open(p, l, (FHCombinedIcon) v, c::accept);
            } else if (v instanceof FHIngredientIcon) {
                IconEditor.INGREDIENT_EDITOR.open(p, l, (FHIngredientIcon) v, c::accept);
            } else if (v instanceof FHAnimatedIcon) {
                IconEditor.ANIMATED_EDITOR.open(p, l, (FHAnimatedIcon) v, c::accept);
            } else if (v instanceof FHTextureIcon) {
                IconEditor.TEXTURE_EDITOR.open(p, l, (FHTextureIcon) v, c::accept);
            } else if (v instanceof FHTextureUVIcon) {
                IconEditor.UV_EDITOR.open(p, l, (FHTextureUVIcon) v, e -> c.accept(v));
            } else if (v instanceof FHTextIcon) {
                IconEditor.TEXT_EDITOR.open(p, l, (FHTextIcon) v, c::accept);
            } else if (v instanceof FHDelegateIcon) {
                IconEditor.INTERNAL_EDITOR.open(p, l, (FHDelegateIcon) v, c::accept);
            } else
                IconEditor.NOP_CHANGE_EDITOR.open(p, l, v, c);
        };
        public static final Editor<FHItemIcon> ITEM_EDITOR = (p, l, v, c) -> {
            SelectItemStackDialog.EDITOR.open(p, l, v == null ? null : v.stack, e -> c.accept(new FHItemIcon(e)));
        };
        public static final Editor<FHTextureIcon> TEXTURE_EDITOR = (p, l, v, c) -> {
            EditPrompt.TEXT_EDITOR.open(p, l, v == null ? "" : (v.rl == null ? "" : v.rl.toString()),
                    e -> c.accept(new FHTextureIcon(new ResourceLocation(e))));
        };
        public static final Editor<FHIngredientIcon> INGREDIENT_EDITOR = (p, l, v, c) -> {
            IngredientEditor.EDITOR_INGREDIENT_EXTERN.open(p, l, v == null ? null : v.igd,
                    e -> c.accept(new FHIngredientIcon(e)));
        };

        public static final Editor<FHIcon> INGREDIENT_SIZE_EDITOR = (p, l, v, c) -> {
            IngredientEditor.EDITOR.open(p, l, null, e -> c.accept(FHIcons.getIcon(e)));
        };
        public static final Editor<FHTextIcon> TEXT_EDITOR = (p, l, v, c) -> {
            EditPrompt.TEXT_EDITOR.open(p, l, v == null ? null : v.text, e -> c.accept(new FHTextIcon(e)));
        };
        public static final Editor<FHIcon> NOP_EDITOR = (p, l, v, c) -> {
            c.accept(FHNopIcon.INSTANCE);
            p.getGui().refreshWidgets();
        };
        public static final Editor<FHIcon> NOP_CHANGE_EDITOR = (p, l, v, c) -> {
            EDITOR.open(p, l, null, c);
        };
        public static final Editor<FHAnimatedIcon> ANIMATED_EDITOR = (p, l, v, c) -> {
            new EditListDialog<>(p, l, v == null ? null : v.icons, null, EDITOR, e -> e.getClass().getSimpleName(),
                    e -> e, e -> c.accept(new FHAnimatedIcon(e.toArray(new FHIcon[0])))).open();
        };
        public static final Editor<FHCombinedIcon> COMBINED_EDITOR = (p, l, v, c) -> {
            new Combined(p, l, v, c).open();
        };

        public static final Editor<FHDelegateIcon> INTERNAL_EDITOR = (p, l, v, c) -> {
            new SelectDialog<>(p, l, v == null ? null : v.name, o -> {
                c.accept(new FHDelegateIcon(o));
            }, TechIcons.internals::keySet, GuiUtils::str, e -> new String[]{e}, TechIcons.internals::get).open();
        };

        public static final Editor<FHTextureUVIcon> UV_EDITOR = (p, l, v, c) -> {
            new UV(p, l, v, c).open();
        };

        T v;

        public IconEditor(Widget panel, T v) {
            super(panel);
            this.v = v;
        }

        @Override
        public void draw(MatrixStack arg0, Theme arg1, int arg2, int arg3, int arg4, int arg5) {
            super.draw(arg0, arg1, arg2, arg3, arg4, arg5);
            v.draw(arg0, arg2 + 300, arg3 + 20, 32, 32);
        }

    }

    private static JsonSerializerRegistry<FHIcon> serializers = new JsonSerializerRegistry<>();

    static {
        serializers.register(FHNopIcon.class, "nop", FHNopIcon::get,FHIcon::asObject, FHNopIcon::get);
        serializers.register(FHItemIcon.class, "item", FHItemIcon::new,FHIcon::asObject, FHItemIcon::new);
        serializers.register(FHCombinedIcon.class, "combined", FHCombinedIcon::new,FHIcon::asObject, FHCombinedIcon::new);
        serializers.register(FHAnimatedIcon.class, "animated", FHAnimatedIcon::new,FHIcon::asObject, FHAnimatedIcon::new);
        serializers.register(FHIngredientIcon.class, "ingredient", FHIngredientIcon::new,FHIcon::asObject, FHIngredientIcon::new);
        serializers.register(FHTextureIcon.class, "texture", FHTextureIcon::new,FHIcon::asObject, FHTextureIcon::new);
        serializers.register(FHTextureUVIcon.class, "texture_uv", FHTextureUVIcon::new,FHIcon::asObject, FHTextureUVIcon::new);
        serializers.register(FHTextIcon.class, "text", FHTextIcon::new,FHIcon::asObject, FHTextIcon::new);
        serializers.register(FHDelegateIcon.class, "internal", FHDelegateIcon::new,FHIcon::asObject, FHDelegateIcon::new);
    }

    public static FHIcon getAnimatedIcon(FHIcon... icons) {
        return new FHAnimatedIcon(icons);
    }

    /**
     * Make a FHIcon delegate of the given icon, THIS IS NOT SERIALIZABLE
     * All Serialization progress would result in getting an NOP icon.
     */
    public static FHIcon getDelegateIcon(String name) {
        return new FHDelegateIcon(name);
    }

    public static FHIcon getIcon(Collection<IItemProvider> items) {
        return new FHIngredientIcon(Ingredient.fromItems(items.toArray(new IItemProvider[0])));
    }

    public static FHIcon getIcon(FHIcon base, FHIcon small) {
        return new FHCombinedIcon(base, small);
    }

    public static FHIcon getIcon(IItemProvider item) {
        return new FHItemIcon(item);
    }

    public static FHIcon getIcon(IItemProvider[] items) {
        return new FHIngredientIcon(Ingredient.fromItems(items));
    }

    public static FHIcon getIcon(Ingredient i) {
        return new FHIngredientIcon(i);
    }

    public static FHIcon getIcon(IngredientWithSize i) {
        if (i.getCount() == 1)
            return getIcon(i.getBaseIngredient());
        if (i.getCount() < 10)
            return getIcon(getIcon(i.getBaseIngredient()), getIcon(" " + String.valueOf(i.getCount())));
        return getIcon(getIcon(i.getBaseIngredient()), getIcon(String.valueOf(i.getCount())));
    }

    public static FHIcon getIcon(ItemStack item) {
        return new FHItemIcon(item);
    }

    public static FHIcon getIcon(ItemStack[] stacks) {
        FHIcon[] icons = new FHIcon[stacks.length];
        for (int i = 0; i < stacks.length; i++)
            icons[i] = FHIcons.getIcon(stacks[i]);
        return getAnimatedIcon(icons);
    }

    public static FHIcon getIcon(JsonElement elm) {
        if (elm == null || elm.isJsonNull()) {
            return FHNopIcon.INSTANCE;
        }
        if (elm.isJsonPrimitive()) {
            return new FHItemIcon(elm);
        } else if (elm.isJsonArray()) {
            return new FHAnimatedIcon(elm);
        } else if (elm.isJsonObject()) {
            JsonObject jo = elm.getAsJsonObject();
            if (jo.has("type")) {
                return serializers.deserializeOrDefault(jo, FHNopIcon.INSTANCE);
            }
            return new FHItemIcon(jo);
        }
        return FHNopIcon.INSTANCE;
    }

    public static FHIcon getIcon(ResourceLocation texture) {
        return new FHTextureIcon(texture);
    }

    public static FHIcon getIcon(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
        return new FHTextureUVIcon(texture, x, y, w, h, tw, th);
    }

    public static FHIcon getIcon(String text) {
        return new FHTextIcon(text);
    }

    /**
     * This does not preserve nbt on save
     */
    public static FHIcon getStackIcons(Collection<ItemStack> rewards) {
        return new FHIngredientIcon(Ingredient.fromStacks(rewards.stream()));
    }

    public static FHIcon nop() {
        return FHNopIcon.INSTANCE;
    }

    public static FHIcon readIcon(PacketBuffer elm) {
        return serializers.readOrDefault(elm, FHNopIcon.INSTANCE);
    }
    public static JsonElement save(FHIcon icon) {
    	return serializers.write(icon);
    }
    public static void write(FHIcon icon,PacketBuffer pb) {
    	serializers.write(pb,icon);
    }
}
