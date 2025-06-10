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

package com.teammoeg.frostedheart.content.wheelmenu;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.MouseCaptureUtil;
import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.ColorHelper;
import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.client.widget.IconButton;
import com.teammoeg.chorda.config.ConfigFileType;
import com.teammoeg.chorda.io.ConfigFileUtil;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.math.CircleDimension;
import com.teammoeg.chorda.math.Dimension2D;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.content.tips.TipRenderer;
import com.teammoeg.frostedheart.content.tips.client.gui.archive.Alignment;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FGuis;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;

import org.joml.Quaternionf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class WheelMenuRenderer {
	public static final IGuiOverlay OVERLAY = WheelMenuRenderer::render;

	protected static float wheelRadius = 60;
	protected static float ringWidth = 30;

	//registered selections
	protected static final TreeMap<ResourceLocation,Selection> selections = new TreeMap<>(CUtils.RESOURCE_LOCATION_COMPARATOR);
	//selections by id user choose to show
	protected static final List<ResourceLocation> displayedSelections=new ArrayList<>();
	protected static final Set<ResourceLocation> hiddenSelections=new HashSet<>();
	
	public static final Map<ResourceLocation,Selection> registeredSelections=new LinkedHashMap<>();
	//selections created by user, rl namespace must be wheel_menu_user
	public static final List<UserSelection> userSelections=new ArrayList<>();
	//selections from world settings, rl namespace must be wheel_menu_world
	public static final List<UserSelection> worldSelections=new ArrayList<>();
	//selections gathered during current session
	protected static final List<Selection> availableSelections = new ArrayList<>();
	protected static final List<Selection> visibleSelections = new ArrayList<>();
	public static final ConfigFileType<UserSelection> configType=new ConfigFileType<>(UserSelection.CODEC,"wheelmenu");
	public static final File wheelmenuConfig=new File(FMLPaths.CONFIGDIR.get().toFile(),"wheelmenu-visibility.json");
	@Override
	public String toString() {
		return "WheelMenuRenderer";
	}

	private static final List<Point> positions = new ArrayList<>();
	private static final List<Float> degrees = new ArrayList<>();
	@Getter
	protected static Selection hoveredSelection;
	protected static boolean mouseMoved = false;
	// create a virtual screen to track mouse movement
	protected static Dimension2D virtualScreen;
	@Getter
	public static boolean isOpened;
	public static boolean isClosing;
	static float openingStatus;

	public static void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
		if (!isOpened || visibleSelections.isEmpty())
			return;
		float p;
		if (isClosing)
			p = Mth.clamp((openingStatus + (1 - partialTicks) * 2) / 3f, 0, 1);
		else
			p = Mth.clamp((openingStatus + partialTicks) / 3f, 0, 1);
		int size = visibleSelections.size();
		int cw = ClientUtils.screenCenterX();
		int ch = ClientUtils.screenCenterY();
		var font = gui.getFont();
		var pose = graphics.pose();
		float innerRadius = wheelRadius - ringWidth;
		virtualScreen.addPos(MouseCaptureUtil.getAndResetCapturedDeltaX(),
				MouseCaptureUtil.getAndResetCapturedDeltaY());

		pose.pushPose();
		pose.translate(cw, ch, 0);
		pose.scale(p, p, p);

		// 背景圆环
		FGuis.drawRing(graphics, 0, 0, innerRadius, wheelRadius, 0, 360,
				ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));
		if (!mouseMoved)
			FGuis.drawRing(graphics, 0, 0, innerRadius - 6, innerRadius - 2, 0, 360,
					ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));

		float halfSliceSize = 360F / (size * 2);

		if (mouseMoved) {
			double radian = Math.atan2(virtualScreen.getX(), -(virtualScreen.getY()));
			double degree = Math.toDegrees(radian);
			if (degree < 0)
				degree += 360;
			int selectedIndex = findIndex(degree + halfSliceSize, size);
			Selection lastHovered = hoveredSelection;
			hoveredSelection = visibleSelections.get(selectedIndex);
			if (hoveredSelection != lastHovered) {
				hoveredSelection.hoverAction.execute(lastHovered);
			}
			// 跟随鼠标移动的细圆环
			pose.pushPose();
			pose.rotateAround(new Quaternionf().rotateZ((float) radian), 0, 0, 0);
			FGuis.drawRing(graphics, 0, 0, innerRadius - 6, innerRadius - 2, halfSliceSize/2,
					360-halfSliceSize, ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F * p));
			FGuis.drawRing(graphics, 0, 0, innerRadius - 6, innerRadius - 2, -halfSliceSize, halfSliceSize,
					ColorHelper.setAlpha(ColorHelper.CYAN, p));
			pose.popPose();

			// 当前选择的选项的圆环
			pose.pushPose();
			pose.rotateAround(new Quaternionf().rotateZ((float) Math.toRadians(degrees.get(selectedIndex))), 0, 0, 0);
			FGuis.drawRing(graphics, 0, 0, innerRadius, wheelRadius, -halfSliceSize, halfSliceSize,
					ColorHelper.setAlpha(ColorHelper.CYAN, 0.5F * p),
					ColorHelper.setAlpha(ColorHelper.CYAN, p*0.15f));
			pose.popPose();
		} else {
			mouseMoved = !MouseHelper.isMouseIn(virtualScreen.getX(), virtualScreen.getY(), -50, -50, 100, 100);
			hoveredSelection = null;
		}
		// 渲染选项
		if (size == positions.size())
			for (int i = 0; i < size; i++) {
				visibleSelections.get(i).render(gui, graphics, partialTicks,positions.get(i).getX(), positions.get(i).getY(), 16, 16);
			}

		// 渲染“鼠标”
		FGuis.drawRing(graphics, (int) virtualScreen.getX()/2, (int) virtualScreen.getY()/2, 3, 6, 0, 360,
				ColorHelper.setAlpha(ColorHelper.CYAN, p));

		// 渲染选项标题
		var message = hoveredSelection != null ? hoveredSelection.getMessage() : Component.translatable("gui.frostedheart.wheel_menu.message",
				FHKeyMappings.key_openWheelMenu.get().getKey().getDisplayName());
		var lines = font.split(message, (int) (innerRadius * 2 - 16));
		CGuiHelper.drawStringLines(graphics, font, lines, 0, -lines.size() * 5, ColorHelper.CYAN, 1, true, true, Alignment.CENTER);
		pose.popPose();
	}

	private static int findIndex(double degrees, int size) {
		float sliceSize = 360F / size;
		for (int i = 1; i <= size; i++) {
			if (degrees <= sliceSize * i) {
				return Math.max(i - 1, 0);
			}
		}
		return 0;
	}

	public static void registerSelections(){
		//System.out.println("fire registries");
		if(!isInitialized) {
		MinecraftForge.EVENT_BUS.post(new WheelMenuSelectionRegisterEvent(registeredSelections));
		// 在此处添加轮盘选项

		registeredSelections.put(new ResourceLocation("wheel_menu","edit"),new Selection(Component.translatable("gui.wheel_menu.editor.edit"), IconButton.Icon.LIST.toCIcon(), s->{
			WheelMenuEditors.openConfigScreen();
		}));
		isInitialized=true;
		}

	}

	private static boolean isInitialized=false;
	public static void collectSelections(){
		registerSelections();
		selections.clear();
		selections.putAll(registeredSelections);
		worldSelections.forEach(u->selections.put(u.worldLocation(),new Selection(u)));
		userSelections.forEach(u->selections.put(u.userLocation(), new Selection(u)));
	}

	public static void openIfNewSelection() {
		
		if(displayedSelections.size()<10) {
			boolean modified=false;
			HashSet<ResourceLocation> set=new HashSet<>(displayedSelections);
			for(Entry<ResourceLocation, Selection> rl:selections.entrySet()) {
				if(!rl.getValue().isAutoAddable()) {
					hiddenSelections.add(rl.getKey());
					continue;
				}
				rl.getValue().validateVisibility();
				if(rl.getValue().isVisible()&&!hiddenSelections.contains(rl.getKey())&&!set.contains(rl.getKey())) {
					displayedSelections.add(0,rl.getKey());
					modified=true;
				}
			}
			if(modified) {
				saveUserSelectedOptions();
			}
		}
	}

	protected static boolean init() {
		visibleSelections.clear();
		availableSelections.clear();
		positions.clear();
		degrees.clear();
		wheelRadius = FHConfig.CLIENT.wheelMenuRadius.get();
		ringWidth = 30 * Math.max(1, wheelRadius / FHConfig.CLIENT.wheelMenuRadius.getDefault());
		virtualScreen = TipRenderer.isTipRendering() ? new CircleDimension(ClientUtils.screenWidth()) : new CircleDimension(wheelRadius * 2);
		openIfNewSelection();
		ArrayList<ResourceLocation> loc=new ArrayList<>(displayedSelections);
		
		boolean rslt=!MinecraftForge.EVENT_BUS.post(new WheelMenuOpenEvent(loc));
		if(rslt) {
			
			for(ResourceLocation s:loc) {
				Selection sel=selections.get(s);
				if(sel!=null) {
					availableSelections.add(sel);
				}
			}
			
			availableSelections.add(0,new Selection(Component.translatable("gui.close"), IconButton.Icon.CROSS.toCIcon(), Selection.NO_ACTION));
		}
		return rslt;
	}

	private static void update() {
		positions.clear();
		int size = visibleSelections.size();
		double averageRadius = (wheelRadius + wheelRadius - ringWidth) / 2.0;
		double angleStep = 2 * Math.PI / size;
		for (int i = 0; i < size; i++) {
			double theta = Math.PI / 2 - i * angleStep;
			double x = averageRadius * Math.cos(theta);
			double y = averageRadius * Math.sin(theta);
			positions.add(new Point((int) x, (int) -y));
		}

		degrees.clear();
		float sliceSize = 360F / size;
		for (int i = 0; i < size; i++) {
			float angle = i * sliceSize;
			degrees.add(angle);
		}
	}

	public static void open() {
		if (init() && !selections.isEmpty()) {
			MouseCaptureUtil.startMouseCapture();
			isOpened = true;
		} 
	}

	public static void tick() {
		
		if (ClientUtils.getPlayer() == null) {// not in world
			if(isOpened) {
				openingStatus = 0;
				onClose();
			}
		}
		if (isOpened) {
			if (FHKeyMappings.key_openWheelMenu.get().isDown()) {
				isClosing = false;
				if (openingStatus < 6)
					openingStatus++;
			} else {
				isClosing = true;
				openingStatus -= 2;
				if (openingStatus <= 0) {
					openingStatus = 0;
					isClosing = false;
					onClose();
					return;
				}
				
			}
			int prevsize=visibleSelections.size();
			visibleSelections.clear();
			for (Selection selection : availableSelections) {
				selection.tick();
				if (selection.visible) {
					visibleSelections.add(selection);
				}
			}
			if (prevsize!=visibleSelections.size())
				update();
		}
	}

	public static void onClose() {
		FHKeyMappings.key_openWheelMenu.get().clickCount=0;
		MouseCaptureUtil.stopMouseCapture();
		isOpened = false;
		mouseMoved = false;
		if (ClientUtils.getPlayer() != null && hoveredSelection != null) {
			hoveredSelection.selectAction.execute(hoveredSelection);
		}
	}

	public static Point getMousePos() {
		if (virtualScreen != null) {
			return new Point((int) virtualScreen.getX(), (int) virtualScreen.getY());
		} else {
			return new Point(0, 0);
		}
	}

	private static class WheelMenuConfig{
		public  List<ResourceLocation> displayed;
		public  List<ResourceLocation> hidden;
		public WheelMenuConfig(List<ResourceLocation> displayed, List<ResourceLocation> hidden) {
			super();
			this.displayed = displayed;
			this.hidden = hidden;
		}
		public WheelMenuConfig(List<ResourceLocation> displayed, Set<ResourceLocation> hidden) {
			super();
			this.displayed = displayed;
			this.hidden = new ArrayList<>(hidden);
		}
	
		
	}

	private static final Codec<WheelMenuConfig> CONFIG_CODEC=RecordCodecBuilder.create(i->i.group(
			Codec.list(ResourceLocation.CODEC).fieldOf("displayed").forGetter(o->o.displayed),
			Codec.list(ResourceLocation.CODEC).fieldOf("hidden").forGetter(o->o.hidden)
			).apply(i, WheelMenuConfig::new));


	public static void saveUserSelectedOptions() {
		try {
			FileUtil.transfer(CONFIG_CODEC.encodeStart(JsonOps.INSTANCE, new WheelMenuConfig(displayedSelections,hiddenSelections)).getOrThrow(false, FHMain.LOGGER::warn).toString(),wheelmenuConfig);
		} catch (IOException e) {
			FHMain.LOGGER.error("Could not save wheelmenu display settings",e);
		}
	}

	public static void load() {
		WheelMenuRenderer.userSelections.clear();
        WheelMenuRenderer.userSelections.addAll(ConfigFileUtil.loadAll(WheelMenuRenderer.configType).values());
        displayedSelections.clear();
        hiddenSelections.clear();
  		try {
  			if(wheelmenuConfig.exists()) {
			WheelMenuConfig config = CONFIG_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(FileUtil.readString(wheelmenuConfig))).getOrThrow(true, FHMain.LOGGER::warn);
			displayedSelections.addAll(config.displayed);
      
			hiddenSelections.addAll(config.hidden);
  			}
		} catch (JsonSyntaxException | IOException e) {
			FHMain.LOGGER.error("Could not load wheelmenu display settings",e);
		}
        
        WheelMenuRenderer.collectSelections();
	}
}
