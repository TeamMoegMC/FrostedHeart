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

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.util.client.Lang;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableObject;

/**
 * @author  khjxiaogu
 */
public class EditListDialog<T> extends EditDialog {
    public static final Editor<Collection<String>> STRING_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, "", EditPrompt.TEXT_EDITOR,Components::str, c).open();
    private final Consumer<Collection<T>> callback;
    private final Component title;
    private final Layer configPanel;
    private final Button buttonAccept, buttonCancel;
    @Getter
    protected final List<T> list;
    protected final Editor<T> editor;
    private final LayerScrollBar scroll;
    private final T def;
    private final Function<T, Component> read;
    private final Function<T, CIcon> toicon;
    boolean modified;
    public static <T> Editor<Collection<T>> createSetEditor(T def, Supplier<Stream<T>> availables, Function<T, Component> toread, Function<T, CIcon> icon) {
    	
    	
    	return (p, l, v,c) ->{
    		MutableObject<EditListDialog<T>> dialog=new MutableObject<>();
    		dialog.setValue(new EditListDialog<T>(p, l, v, def,(p2, l2, v2, c2) -> new SelectDialog<T>(p2, l2, v2, c2,
    			() -> availables.get().filter(t->!dialog.getValue().getList().contains(t)).toList(), toread, null, icon).open(), toread,icon, c));
    		dialog.getValue().open();
    	};
    }
    public EditListDialog(UIWidget p, Component label, Collection<T> vx, Editor<T> editor, Function<T, Component> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, null, editor, toread, null, li);
    }
    public EditListDialog(UIWidget p, Component label, Collection<T> vx, T def, Editor<T> editor, Function<T, Component> toread, Consumer<Collection<T>> li) {
        this(p, label, vx, def, editor, toread, o->CIcons.nop(), li);
    }

    public EditListDialog(UIWidget p, Component label, Collection<T> vx, T def, Editor<T> editor, Function<T, Component> toread, Function<T, CIcon> icon, Consumer<Collection<T>> li) {
        super(p);
        callback = li;
        if (vx != null)
            list = new ArrayList<>(vx);
        else
            list = new ArrayList<>();
        title = (label).copy();
        this.editor = editor;
        this.def = def;
        this.read = toread;
        this.toicon = icon;
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        configPanel = new Layer(this) {
            @Override
            public void addUIElements() {
                for (int i = 0; i < list.size(); i++) {
                    add(new ButtonConfigValue(this, i));
                }
                add(new ButtonAddValue(this));
            }

            @Override
            public void alignWidgets() {
                for (UIWidget w : super.elements) {
                    w.setWidth(super.getWidth() - 16);
                }
                align(false);
                //scroll.setMaxValue();
            }
        };

        scroll = new LayerScrollBar(this, configPanel);
        buttonAccept =TextButton.create(this, Components.empty(), IconButton.Icon.CHECK.toCIcon(), (button) -> {
            callback.accept(list);
            modified = false;
            close();
        });
        buttonCancel = TextButton.create(this, Components.empty(), IconButton.Icon.CROSS.toCIcon(), (button) -> close());
    }
    int movingIndex;
    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
    	if(moving!=null) {
    		movingIndex=(int) Mth.clamp((this.getMouseY()-configPanel.getY())/(moving.getHeight()+1),0,list.size()-1);
    		if(MouseHelper.isLeftPressed()) {
    			moving.setY((int) movingIndex*(moving.getHeight()+1));
    		}else {
    			int originIndex=moving.index;
    			moving=null;
    			if(originIndex!=movingIndex) {
	    			int newPos=(int) (movingIndex);
	    			T obj=list.remove(originIndex);
	    			list.add(Math.min(list.size(), newPos),obj);
	    			configPanel.refresh();
	    			this.modified=true;
    			}
    		}
    	}
		super.render(graphics, x, y, w, h);
	}
	@Override
    public void addUIElements() {
        add(buttonAccept);
        add(buttonCancel);
        add(configPanel);
        add(scroll);
    }

    @Override
    public void alignWidgets() {
        configPanel.setPosAndSize(5, 25, width - 10, height - 30);
        configPanel.alignWidgets();
        scroll.setPosAndSize(width - 16, 25, 8, height - 30);

        buttonAccept.setPos(width - 26, 4);
        buttonCancel.setPos(width - 47, 4);
    }
    ButtonConfigValue moving;
    public void setMoving(ButtonConfigValue moving) {
    	this.moving=moving;
    	movingIndex=moving.index;
    }
    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        CGuiHelper.drawUIBackground(matrixStack, x, y, w, h);
        matrixStack.drawString(getFont(), getTitle(), x+5, y+5, 0xFF000000,false);
    }

    @Override
    public Component getTitle() {
        return title;
    }
    @Override
    public void onClose() {
    }
    public void onChange() {
    	
    }
    @Override
    public void onClosed() {
        if (modified) {
            ConfirmDialog.EDITOR.open(this, Lang.translateKey("gui.chorda.editor.unsaved_changes"), true, e -> {
                if (!e) open();
            });
        }
    }

    public class ButtonAddValue extends Button {
        public ButtonAddValue(Layer panel) {
            super(panel);
            setHeight(18);
            setTitle(Components.str("+ ").append(Lang.translateKey("gui.add")));
        }



		@Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
            super.drawBackground(matrixStack, x, y, w, h);
            
            matrixStack.drawString(getFont(), getTitle(), x+4, y+5, 0xFFFFFFFF);
        }

        @Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();
            editor.open(this,Lang.translateKey("gui.add"), def, s -> {
                if (s != null) {
                    modified = true;
                    list.add(s);
                    ((Layer)parent).refresh();
                    onChange();
                }
            });

        }
    }

    public class ButtonConfigValue extends Button {
        public final int index;
        public CIcon icon;
        public ButtonConfigValue(Layer panel, int i) {
            super(panel);
            index = i;
            
            icon=toicon.apply(list.get(index));
            setHeight(18);
        }

        @Override
		public int getY() {
        	int offset=0;
        	int ioffset=0;
        	if(moving!=this&&moving!=null) {
        		
				if(index>moving.index) {//originally behind item, simulating them being moved one step forward
					offset-=this.height+1;
					ioffset--;
				}
				if(index+ioffset>=movingIndex) {//currently behind insertion, simulating them being moved backword
					offset+=this.height+1;
				}
        	}
        	return super.getY()+offset;
		}

		@Override
        public void getTooltip(Consumer<Component> l) {
            if (getMouseX() >=  width - 19) {
                l.accept(Components.translatable("selectServer.delete"));
            }else if (getMouseX() >=width - 36) {
            	l.accept(Components.translatable("gui.chorda.editor.reorder"));
             }  else {
                l.accept(read.apply(list.get(index)));
            }
        }

        @Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
        	super.drawBackground(matrixStack, x, y, w, h);
        	if(this==moving) {
        		matrixStack.pose().pushPose();
        		matrixStack.pose().translate(0, 0, 10);
        	}
            boolean mouseOver = isMouseOver();
            int ioffset = 1;
            if (icon!=null&&icon!=CIcons.nop()) {
            	icon.draw(matrixStack, x + ioffset, y+1, 16, 16);
                ioffset += 13;
            }
            int th=(height-getFont().lineHeight)/2;
            if (mouseOver) {

            	//matrixStack.fill(x, y, x+w, y+h, 0x21FFFFFF);

                if (getMouseX() >= w - 19) {
                	matrixStack.fill(x + w - 19, y, x+w, y+h, 0x21FFFFFF);
                }
            }
            matrixStack.drawString(getFont(), read.apply(list.get(index)), x+4+ ioffset, y+th, 0xFFFFFFFF);

            //if (mouseOver) {
            	IconButton.Icon.CROSS.toCIcon().draw(matrixStack, x+w-18, y+1,height-2,height-2);
            	//matrixStack.drawString(getFont(), "[-]", x+w-16, y+2, 0xFFFFFFFF);
            	matrixStack.drawString(getFont(), "||||||||", x+w-36, y+2, 0xFFFFFFFF);
            	matrixStack.drawString(getFont(), "||||||||", x+w-36, y+h-getFont().lineHeight, 0xFFFFFFFF);
            //}
          	if(this==moving) {
        		matrixStack.pose().popPose();
        	}
        }

        @Override
		public Cursor getCursor() {
        	if(MouseHelper.isMouseIn(getMouseX(), getMouseY(), this.getWidth()-36, 0, 20, height))
        		return Cursor.MOVE;
        	return null;
		}

		@Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();

            if (getMouseX() >=width - 16) {
                list.remove(index);
                modified = true;
                ((Layer)parent).refresh();

            }else if (getMouseX() >=width - 36) {
               setMoving(this);

            } else {
                editor.open(this, Components.translatable("gui.chorda.editor.edit"),list.get(index), s -> {
                    modified = true;
                    list.set(index, s);
                    ((Layer)parent).refresh();
                    onChange();
                });
            }
        }
    }
}