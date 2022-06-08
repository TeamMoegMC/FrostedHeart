package com.teammoeg.frostedheart.research.gui.editor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.MutableColor4I;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * @author LatvianModder,khjxiaogu
 */
public class EditListDialog<T> extends EditDialog {
	public static final Editor<Collection<String>> STRING_LIST=(p,l,v,c)->{
		new EditListDialog<>(p,l,v,"",SingleEditDialog.TEXT_EDITOR,e->e,c).open();
	};
	public static final Editor<Collection<Research>> RESEARCH_LIST=(p,l,v,c)->{
		new EditListDialog<>(p,l,v,null,ResearchSelectorDialog.EDITOR,e->e.getName().getString(),c).open();
	};
	public class ButtonConfigValue extends Button {
		public final int index;

		public ButtonConfigValue(Panel panel, int i) {
			super(panel);
			index = i;
			setHeight(12);
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			boolean mouseOver = getMouseY() >= 20 && isMouseOver();


			if (mouseOver) {

				Color4I.WHITE.withAlpha(33).draw(matrixStack, x, y, w, h);

				if (getMouseX() >= x + w - 19) {
					Color4I.WHITE.withAlpha(33).draw(matrixStack, x + w - 19, y, 19, h);
				}
			}

			theme.drawString(matrixStack,read.apply(list.get(index)), x + 4, y + 2);

			if (mouseOver) {
				theme.drawString(matrixStack, "[-]", x + w - 16, y + 2, Color4I.WHITE, 0);
			}

			RenderSystem.color4f(1F, 1F, 1F, 1F);
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();

			if (getMouseX() >= getX() + width - 19) {
					list.remove(index);
					parent.refreshWidgets();
				
			} else {
				editor.open(this,"Edit",list.get(index), s-> {
					list.set(index, s);
					parent.refreshWidgets();
				});
			}
		}

		@Override
		public void addMouseOverText(TooltipList l) {
			if (getMouseX() >= getX() + width - 19) {
				l.translate("selectServer.delete");
			} else {
				l.add(new StringTextComponent(read.apply(list.get(index))));
			}
		}
	}

	public class ButtonAddValue extends Button {
		public ButtonAddValue(Panel panel) {
			super(panel);
			setHeight(12);
			setTitle(new StringTextComponent("+ ").appendSibling(new TranslationTextComponent("gui.add")));
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			boolean mouseOver = getMouseY() >= 20 && isMouseOver();

			if (mouseOver) {
				Color4I.WHITE.withAlpha(33).draw(matrixStack, x, y, w, h);
			}

			theme.drawString(matrixStack, getTitle(), x + 4, y + 2, theme.getContentColor(getWidgetType()), Theme.SHADOW);
			RenderSystem.color4f(1F, 1F, 1F, 1F);
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			editor.open(this,"New",def, s-> {
				if(s!=null) {
					list.add(s);
					parent.refreshWidgets();
				}
			});
			
		}

		@Override
		public void addMouseOverText(TooltipList list) {
		}
	}

	private final Consumer<Collection<T>> callback;

	private final ITextComponent title;
	private final Panel configPanel;
	private final Button buttonAccept, buttonCancel;
	private final List<T> list;
	private final Editor<T> editor;
	private final PanelScrollBar scroll;
	private final T def;
	private final Function<T,String> read;

	public EditListDialog(Widget p,String label,Collection<T> vx,T def,Editor<T> editor,Function<T,String> toread,Consumer<Collection<T>> li) {
		super(p);
		callback = li;
		list=new ArrayList<>(vx);
		title = new StringTextComponent(label).mergeStyle(TextFormatting.BOLD);
		this.editor=editor;
		this.def=def;
		this.read=toread;
		int sw = 387;
		int sh = 203;
		this.setSize(sw, sh);
		configPanel = new Panel(this) {
			@Override
			public void addWidgets() {
				for (int i = 0; i < list.size(); i++) {
					add(new ButtonConfigValue(this, i));
				}
				add(new ButtonAddValue(this));
			}

			@Override
			public void alignWidgets() {
				for (Widget w : widgets) {
					w.setWidth(width - 16);
				}

				scroll.setMaxValue(align(WidgetLayout.VERTICAL));
			}
		};

		scroll = new PanelScrollBar(this, configPanel);
		buttonAccept = new SimpleButton(this, new TranslationTextComponent("gui.accept"), Icons.ACCEPT, (widget, button) ->{
			callback.accept(list);
			close();
		});
		buttonCancel = new SimpleButton(this, new TranslationTextComponent("gui.cancel"), Icons.CANCEL, (widget, button) -> close());
	}

	@Override
	public void addWidgets() {
		add(buttonAccept);
		add(buttonCancel);
		add(configPanel);
		add(scroll);
	}

	@Override
	public void alignWidgets() {
		configPanel.setPosAndSize(0, 20, width, height - 20);
		configPanel.alignWidgets();
		scroll.setPosAndSize(width - 16, 20, 16, height - 20);

		buttonAccept.setPos(width - 18, 2);
		buttonCancel.setPos(width - 38, 2);
	}
	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawPanelBackground(matrixStack, x, y, w, h);
		
		theme.drawString(matrixStack, getTitle(), x,y-10);
	}

	@Override
	public ITextComponent getTitle() {
		return title;
	}

	@Override
	public void onClose() {
	}
}