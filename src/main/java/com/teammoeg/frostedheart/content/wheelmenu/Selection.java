package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.List;
import java.util.function.Predicate;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;

import dev.ftb.mods.ftblibrary.util.TextComponentParser;
import lombok.Getter;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;

public class Selection {
	public static TypedCodecRegistry<Selection.Action> registry=new TypedCodecRegistry<>();
	static{
		registry.register(KeyMappingTriggerAction.class, "key", KeyMappingTriggerAction.CODEC);
		registry.register(CommandInputAction.class, "command", CommandInputAction.CODEC);
	}
	public static final Codec<Selection.Action> USER_ACTION_CODEC=registry.codec();
	public static final Codec<List<UserSelection>> USER_SELECTION_LIST=Codec.list(UserSelection.CODEC);
	public static record UserSelection(String id,String message, CIcon icon, Selection.Action selectAction) {
		public static final Codec<UserSelection> CODEC=RecordCodecBuilder.create(t->t.group(
				Codec.STRING.fieldOf("id").forGetter(UserSelection::id),
				Codec.STRING.fieldOf("name").forGetter(UserSelection::message),
				CIcons.CODEC.fieldOf("icon").forGetter(UserSelection::icon),
				USER_ACTION_CODEC.fieldOf("action").forGetter(UserSelection::selectAction)
			).apply(t, UserSelection::new));
		public Selection createSelection() {
			return new Selection(getParsedMessage(),icon,selectAction==null?Selection.NO_ACTION:selectAction);
		}
		public Component getParsedMessage() {
			return StringTextComponentParser.parse(message);
		}
	}
	public static final Predicate<Selection> ALWAYS_VISIBLE = s -> true;
	public static final Selection.Action NO_ACTION = s -> {};

	protected final Predicate<Selection> visibility;
	protected final Selection.Action selectAction;
	protected final Selection.Action hoverAction;
	@Getter
	public final CIcon icon;
	@Getter
	protected Component message;
	@Getter
	protected boolean visible;
	@Getter
	protected boolean hovered;
	public int color;

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 */
	public Selection(Component message, CIcon icon, Selection.Action selectAction) {
		this(message, icon, ColorHelper.CYAN, ALWAYS_VISIBLE, selectAction, NO_ACTION);
	}

	/**
	 * @param icon        {@link ItemStack}, {@link IconButton.Icon},
	 *                    {@link Component}, {@code null}
	 * @param color       图标为 {@link IconButton.Icon} 时的颜色
	 * @param visibility  选项在什么情况下可见，每tick更新
	 * @param selectAction 选择后的行动 (选中 -> 松开Tab)
	 * @param hoverAction 选中选项后的行动
	 */
	public Selection(Component message, CIcon icon, int color, Predicate<Selection> visibility, Selection.Action selectAction,
			Selection.Action hoverAction) {
		this.message = message;
		this.icon = icon;
		this.color = color;
		this.visibility = visibility;
		this.selectAction = selectAction;
		this.hoverAction = hoverAction;
	}
	public Selection(String keyDesc, CIcon icon) {
		this(Components.translatable(keyDesc), icon, ColorHelper.CYAN, ALWAYS_VISIBLE, new KeyMappingTriggerAction(keyDesc), NO_ACTION);
	}
	

	protected void render(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y, int width, int height) {
		if (!visible)
			return;
		renderSelection(gui, graphics, partialTick,x,y, width, height);
		if (hovered) {
			renderWhenHovered(gui, graphics, partialTick,x,y, width, height);
		}
	}

	@SuppressWarnings("unused")
	protected void renderSelection(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y,  int width, int height) {
		icon.draw(graphics, x-(width/2), y-(height/2), width, height);
	}

	/**
	 * 选中选项时渲染
	 */
	@SuppressWarnings("unused")
	protected void renderWhenHovered(ForgeGui gui, GuiGraphics graphics, float partialTick,int x,int y,  int width, int height) {
	}

	protected void tick() {
		hovered = WheelMenuRenderer.hoveredSelection == this;
		validateVisibility();
	}
	public void validateVisibility() {
		visible = visibility.test(this);
	}

	@OnlyIn(Dist.CLIENT)
	public interface Action {
		void execute(Selection selection);
	}
	public static record KeyMappingTriggerAction(KeyMapping km) implements Action{
		public static final MapCodec<KeyMappingTriggerAction> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
			Codec.STRING.comapFlatMap(o->{
				KeyMapping nkm=KeyMapping.ALL.get(o);
				if(nkm==null)
					return DataResult.error(()->"Invalid key!");
				return DataResult.success(nkm);
			}, KeyMapping::getName).fieldOf("key").forGetter(KeyMappingTriggerAction::km)
			).apply(t,KeyMappingTriggerAction::new));
		/**
		 * Create a keymapping action. name is key description id.
		 * */
		public KeyMappingTriggerAction(String name) {
			this(KeyMapping.ALL.get(name));
		}
		public KeyMapping getKey() {
			return km;
		}
		@Override
		public void execute(Selection selection) {
			km.setDown(true);
			km.clickCount++;
			MinecraftForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.PRESS, 0));//mock key press
			MinecraftForge.EVENT_BUS.post(new InputEvent.Key(0, 0, InputConstants.RELEASE, 0));
			km.setDown(false);
			km.consumeClick();
		}

	}
	public static record CommandInputAction(String command) implements Action{
		public static final MapCodec<CommandInputAction> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
			ExtraCodecs.validate(Codec.STRING, n->n.startsWith("/")?DataResult.success(n):DataResult.error(()->"Commands must starts with '/'")).fieldOf("command").forGetter(CommandInputAction::command)
			).apply(t,CommandInputAction::new));
		public String getCommand() {
			return command;
		}
		@Override
		public void execute(Selection selection) {
			 String s1 = SharedConstants.filterText(command);
             if (s1.startsWith("/")) {
                if (!ClientUtils.getPlayer().connection.sendUnsignedCommand(s1.substring(1))) {
                   FHMain.LOGGER.error("Not allowed to run unsigned command '{}' from wheelmenu action", s1);
                }
             } else {
            	 FHMain.LOGGER.error("Failed to run command without '/' prefix from wheelmenu action: '{}'", (Object)s1);
             }
		}

	}
}