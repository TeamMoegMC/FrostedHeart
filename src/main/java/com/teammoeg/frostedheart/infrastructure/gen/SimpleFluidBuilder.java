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

package com.teammoeg.frostedheart.infrastructure.gen;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.ForgeRegistries;

public class SimpleFluidBuilder<T extends ForgeFlowingFluid, P> extends FluidBuilder<T, P> {

    // use supplier
    public SimpleFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
                               ResourceLocation stillTexture, ResourceLocation flowingTexture, NonNullSupplier<FluidType> typeFactory,
                               NonNullFunction<ForgeFlowingFluid.Properties, T> factory) {
        super(owner, parent, name, callback, stillTexture, flowingTexture, typeFactory, factory);
        //source(factory);
    }

    // Use typefactory
    public SimpleFluidBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
                               ResourceLocation stillTexture, ResourceLocation flowingTexture, FluidBuilder.FluidTypeFactory typeFactory,
                               NonNullFunction<ForgeFlowingFluid.Properties, T> factory) {
        super(owner, parent, name, callback, stillTexture, flowingTexture, typeFactory, factory);
        //source(factory);
    }


    @Override
    public NonNullSupplier<T> asSupplier() {
        return this::getEntry;
    }
}
