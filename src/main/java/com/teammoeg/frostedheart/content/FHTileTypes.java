/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content;

import java.util.Collection;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.steamenergy.ChargerTileEntity;
import com.teammoeg.frostedheart.steamenergy.DebugHeaterTileEntity;
import com.teammoeg.frostedheart.steamenergy.HeatPipeTileEntity;
import com.teammoeg.frostedheart.steamenergy.RadiatorTileEntity;
import com.teammoeg.frostedheart.tileentity.CrucibleTileEntity;
import com.teammoeg.frostedheart.tileentity.SteamTurbineTileEntity;
import com.teammoeg.frostedheart.tileentity.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.tileentity.T2GeneratorTileEntity;

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
            "generator", makeType(() -> new T1GeneratorTileEntity(1,2, 1), () -> FHBlocks.Multi.generator)
    );
    public static final RegistryObject<TileEntityType<CrucibleTileEntity>> CRUCIBLE = REGISTER.register(
            "crucible", makeType(() -> new CrucibleTileEntity(), () -> FHBlocks.Multi.crucible)
    );
    public static final RegistryObject<TileEntityType<SteamTurbineTileEntity>> STEAMTURBINE = REGISTER.register(
            "steam_turbine", makeType(() -> new SteamTurbineTileEntity(), () -> FHBlocks.Multi.steam_turbine)
    );

    public static final RegistryObject<TileEntityType<HeatPipeTileEntity>> HEATPIPE = REGISTER.register(
            "heat_pipe", makeType(() -> new HeatPipeTileEntity(), () -> FHBlocks.heat_pipe)
    );
    public static final RegistryObject<TileEntityType<DebugHeaterTileEntity>> DEBUGHEATER = REGISTER.register(
            "debug_heater", makeType(() -> new DebugHeaterTileEntity(), () -> FHBlocks.debug_heater)
    );
    public static final RegistryObject<TileEntityType<ChargerTileEntity>> CHARGER = REGISTER.register(
            "charger", makeType(() -> new ChargerTileEntity(), () -> FHBlocks.charger)
    );

	public static final RegistryObject<TileEntityType<RadiatorTileEntity>> RADIATOR = REGISTER.register(
            "heat_radiator", makeType(() -> new RadiatorTileEntity(), () -> FHBlocks.radiator));

	public static final RegistryObject<TileEntityType<T2GeneratorTileEntity>> GENERATOR_T2 = REGISTER.register(
            "generator_r2", makeType(() -> new T2GeneratorTileEntity(2,4, 2), () -> FHBlocks.Multi.generator_t2)
    );
    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
        return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
    }

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
        return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
    }

}
