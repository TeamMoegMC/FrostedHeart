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

package com.teammoeg.frostedheart.bootstrap.common;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import com.teammoeg.chorda.menu.MultiBlockMenuServerFactory;
import com.teammoeg.chorda.menu.MultiblockMenuClientFactory;
import com.teammoeg.chorda.menu.MultiblockMenuType;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.decoration.RelicChestContainer;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import com.teammoeg.frostedheart.content.health.screen.NutritionMenu;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorTileEntity;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Container;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Container;
import com.teammoeg.frostedheart.content.incubator.IncubatorTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatContainer;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaContainer;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaTileEntity;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;
import com.teammoeg.frostedheart.content.climate.block.ClothesInventoryMenu;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.block.generator.t1.T1GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.block.generator.t2.T2GeneratorState;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeBlockEntity;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeMenu;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHMenuTypes {
	@FunctionalInterface
	public interface BEMenuFactory<T extends AbstractContainerMenu, BE extends BlockEntity> {
		T get(int id, Inventory inventoryPlayer, BE tile);
	}

	public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
		FHMain.MODID);
	public static final RegistryObject<MenuType<TradeContainer>> TRADE_GUI = CONTAINERS.register("trade", () -> IForgeMenuType.create(TradeContainer::new));
	public static final RegistryObject<MenuType<HeatStatContainer>> HEAT_STAT = CONTAINERS.register("heat_stat", () -> IForgeMenuType.create(HeatStatContainer::new));
	public static final RegistryObject<MenuType<ClothesInventoryMenu>> CLOTHES_GUI = CONTAINERS.register("clothes", () -> IForgeMenuType.create(ClothesInventoryMenu::new));
	public static final RegistryObject<MenuType<NutritionMenu>> NUTRITION_GUI = CONTAINERS.register("nutrition", () -> IForgeMenuType.create(NutritionMenu::new));
	
	public static final MultiblockMenuType<T1GeneratorState, T1GeneratorContainer> GENERATOR_T1 = registerMultiblock("generator",T1GeneratorContainer::new,T1GeneratorContainer::new);
	public static final MultiblockMenuType<T2GeneratorState, T2GeneratorContainer> GENERATOR_T2 = registerMultiblock("generator_t2", T2GeneratorContainer::new, T2GeneratorContainer::new);

	public static final RegistryObject<MenuType<RelicChestContainer>> RELIC_CHEST = register(RelicChestTileEntity.class, ("relic_chest"), RelicChestContainer::new);

	public static final RegistryObject<MenuType<SaunaContainer>> SAUNA = register(SaunaTileEntity.class, ("sauna_vent"), SaunaContainer::new);
	public static final RegistryObject<MenuType<IncubatorT1Container>> INCUBATOR_T1 = register(IncubatorTileEntity.class, ("incubator"), IncubatorT1Container::new);
	public static final RegistryObject<MenuType<IncubatorT2Container>> INCUBATOR_T2 = register(HeatIncubatorTileEntity.class, ("heat_incubator"), IncubatorT2Container::new);
	public static final RegistryObject<MenuType<WardrobeMenu>> WARDROBE = register(WardrobeBlockEntity.class, ("wardrobe"), WardrobeMenu::new);

	
	@SuppressWarnings("unchecked")
	public static <T extends AbstractContainerMenu, BE extends BlockEntity> RegistryObject<MenuType<T>> register(Class<BE> BEClass, String name, BEMenuFactory<T, BE> factory) {
		return CONTAINERS.register(name, () -> IForgeMenuType.create((id, inv, pb) -> {
			BlockEntity be = inv.player.level().getBlockEntity(pb.readBlockPos());
			if (BEClass.isInstance(be))
				return factory.get(id, inv, (BE) be);
			return null;
		}));
	}

	public static <S extends IMultiblockState, C extends AbstractContainerMenu> MultiblockMenuType<S, C> registerMultiblock(
		String name,
		MultiBlockMenuServerFactory<S, C> container,
		MultiblockMenuClientFactory<C> client) {
		RegistryObject<MenuType<C>> typeRef = CONTAINERS.register(name,() -> {
			Mutable<MenuType<C>> typeBox = new MutableObject<>();
			MenuType<C> type = new MenuType<>((id, inv) -> client.create(typeBox.getValue(), id, inv), FeatureFlagSet.of());
			typeBox.setValue(type);
			return type;
		});
		return new MultiblockMenuType<>(typeRef, container);
	}
}
