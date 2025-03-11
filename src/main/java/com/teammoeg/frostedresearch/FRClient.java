package com.teammoeg.frostedresearch;

import java.util.function.Function;

import com.teammoeg.chorda.client.model.DynamicBlockModelReference;
import com.teammoeg.frostedresearch.blocks.MechCalcRenderer;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.MenuScreenWrapper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FRClient {
	

	public FRClient() {
		// TODO Auto-generated constructor stub
	}
	 @SubscribeEvent
	 public static void setup(FMLClientSetupEvent event) {
		MechCalcRenderer.MODEL = DynamicBlockModelReference.getModelCached(FRMain.MODID, "block/mechanical_calculator_movable").register();
		
	}
	 @SubscribeEvent
	public static void initScreen(FMLClientSetupEvent event) {
		registerFTBScreen(FRContents.MenuTypes.DRAW_DESK.get(), DrawDeskScreen::new);
	}
    public static <C extends AbstractContainerMenu, S extends BaseScreen> void
    registerFTBScreen(MenuType<C> type, Function<C, S> factory) {
        MenuScreens.register(type, FTBScreenFactory(factory));
    }
    public static <C extends AbstractContainerMenu, S extends BaseScreen> MenuScreens.ScreenConstructor<C, MenuScreenWrapper<C>>
    FTBScreenFactory(Function<C, S> factory) {
        return (c, i, t) -> new MenuScreenWrapper<>(factory.apply(c), c, i, t).disableSlotDrawing();
    }
}
