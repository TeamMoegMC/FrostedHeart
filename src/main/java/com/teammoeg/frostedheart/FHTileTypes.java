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

package com.teammoeg.frostedheart;

import java.util.Collection;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import com.teammoeg.frostedheart.content.decoration.oilburner.GasVentTileEntity;
import com.teammoeg.frostedheart.content.decoration.oilburner.OilBurnerTileEntity;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorTileEntity;
import com.teammoeg.frostedheart.content.incubator.IncubatorTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.DebugHeaterTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.radiator.RadiatorTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.steamcore.SteamCoreTileEntity;
import com.teammoeg.frostedheart.research.machines.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.research.machines.MechCalcTileEntity;

import com.teammoeg.frostedheart.town.Farm.FarmBlockTileEntity;
import com.teammoeg.frostedheart.town.house.HouseTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHTileTypes {
    public static final DeferredRegister<TileEntityType<?>> REGISTER = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, FHMain.MODID);

    public static final RegistryObject<TileEntityType<T1GeneratorTileEntity>> GENERATOR_T1 = REGISTER.register(
            "generator", makeType(T1GeneratorTileEntity::new, () -> FHMultiblocks.generator)
    );

    public static final RegistryObject<TileEntityType<HeatPipeTileEntity>> HEATPIPE = REGISTER.register(
            "heat_pipe", makeType(HeatPipeTileEntity::new, () -> FHBlocks.heat_pipe)
    );
    public static final RegistryObject<TileEntityType<DebugHeaterTileEntity>> DEBUGHEATER = REGISTER.register(
            "debug_heater", makeType(DebugHeaterTileEntity::new, () -> FHBlocks.debug_heater)
    );
    public static final RegistryObject<TileEntityType<ChargerTileEntity>> CHARGER = REGISTER.register(
            "charger", makeType(ChargerTileEntity::new, () -> FHBlocks.charger)
    );

    public static final RegistryObject<TileEntityType<RadiatorTileEntity>> RADIATOR = REGISTER.register(
            "heat_radiator", makeType(RadiatorTileEntity::new, () -> FHMultiblocks.radiator));

    public static final RegistryObject<TileEntityType<T2GeneratorTileEntity>> GENERATOR_T2 = REGISTER.register(
            "generator_t2", makeType(T2GeneratorTileEntity::new, () -> FHMultiblocks.generator_t2)
    );
    public static final RegistryObject<TileEntityType<OilBurnerTileEntity>> OIL_BURNER = REGISTER.register(
            "oil_burner", makeType(OilBurnerTileEntity::new, () -> FHBlocks.oilburner)
    );
    public static final RegistryObject<TileEntityType<GasVentTileEntity>> GAS_VENT = REGISTER.register(
            "gas_vent", makeType(GasVentTileEntity::new, () -> FHBlocks.gasvent)
    );

    public static final RegistryObject<TileEntityType<DrawingDeskTileEntity>> DRAWING_DESK = REGISTER.register(
            "drawing_desk", makeType(DrawingDeskTileEntity::new, () -> FHBlocks.drawing_desk)
    );
    public static final RegistryObject<TileEntityType<RelicChestTileEntity>> RELIC_CHEST = REGISTER.register(
            "relic_chest", makeType(RelicChestTileEntity::new, () -> FHBlocks.relic_chest)
    );

    public static final RegistryObject<TileEntityType<MechCalcTileEntity>> MECH_CALC = REGISTER.register(
            "mechanical_calculator", makeType(MechCalcTileEntity::new, () -> FHBlocks.mech_calc)
    );

    public static final RegistryObject<TileEntityType<SteamCoreTileEntity>> STEAM_CORE = REGISTER.register(
            "steam_core", makeType(SteamCoreTileEntity::new, () -> FHBlocks.steam_core)
    );

    public static final RegistryObject<TileEntityType<SaunaTileEntity>> SAUNA = REGISTER.register(
            "sauna", makeType(SaunaTileEntity::new, () -> FHBlocks.sauna)
    );
    public static final RegistryObject<TileEntityType<?>> INCUBATOR = REGISTER.register(
            "incubator", makeType(IncubatorTileEntity::new, () -> FHBlocks.incubator1)
    );
    public static final RegistryObject<TileEntityType<?>> INCUBATOR2 = REGISTER.register(
            "heat_incubator", makeType(HeatIncubatorTileEntity::new, () -> FHBlocks.incubator2)
    );
    public static final RegistryObject<TileEntityType<HouseTileEntity>> HOUSE = REGISTER.register(
            "house", makeType(HouseTileEntity::new, () -> FHBlocks.house)
    );
    public static final RegistryObject<TileEntityType<FarmBlockTileEntity>> FARM = REGISTER.register(
            "farm_block", makeType(FarmBlockTileEntity::new, () -> FHBlocks.farm)
    );
    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
        return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
    }

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
        return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
    }

}