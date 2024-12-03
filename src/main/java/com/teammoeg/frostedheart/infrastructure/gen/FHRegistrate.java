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

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.CreateBlockEntityBuilder;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.VirtualFluidBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.Util;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FHRegistrate extends AbstractRegistrate<FHRegistrate> {
    private static final Map<RegistryEntry<?>, RegistryObject<CreativeModeTab>> TAB_LOOKUP = Collections.synchronizedMap(new IdentityHashMap<>());
    public static final ResourceLocation STILL = new ResourceLocation("block/water_still");
    public static final ResourceLocation FLOW = new ResourceLocation("block/water_flow");

    @Nullable
    protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
    @Nullable
    protected RegistryObject<CreativeModeTab> currentTab;

    protected FHRegistrate(String modid) {
        super(modid);
    }

    public static FHRegistrate create(String modid) {
        return new FHRegistrate(modid);
    }

    public static boolean isInCreativeTab(RegistryEntry<?> entry, RegistryObject<CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) == tab;
    }

    public FHRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
        currentTooltipModifierFactory = factory;
        return self();
    }

    @Nullable
    public Function<Item, TooltipModifier> getTooltipModifierFactory() {
        return currentTooltipModifierFactory;
    }

    @Nullable
    public FHRegistrate setCreativeTab(RegistryObject<CreativeModeTab> tab) {
        currentTab = tab;
        return self();
    }

    public RegistryObject<CreativeModeTab> getCreativeTab() {
        return currentTab;
    }

    @Override
    public FHRegistrate registerEventListeners(IEventBus bus) {
        return super.registerEventListeners(bus);
    }

    @Override
    protected <R, T extends R> RegistryEntry<T> accept(String name, ResourceKey<? extends Registry<R>> type,
                                                       Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator,
                                                       NonNullFunction<RegistryObject<T>, ? extends RegistryEntry<T>> entryFactory) {
        RegistryEntry<T> entry = super.accept(name, type, builder, creator, entryFactory);
        if (type.equals(Registries.ITEM)) {
            if (currentTooltipModifierFactory != null) {
                TooltipModifier.REGISTRY.registerDeferred(entry.getId(), currentTooltipModifierFactory);
            }
        }
        if (currentTab != null) {
            TAB_LOOKUP.put(entry, currentTab);
        }
        return entry;
    }

    @Override
    public <T extends BlockEntity> CreateBlockEntityBuilder<T, FHRegistrate> blockEntity(String name,
                                                                                             BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return blockEntity(self(), name, factory);
    }

    @Override
    public <T extends BlockEntity, P> CreateBlockEntityBuilder<T, P> blockEntity(P parent, String name,
                                                                                 BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return (CreateBlockEntityBuilder<T, P>) entry(name,
                (callback) -> CreateBlockEntityBuilder.create(this, parent, name, callback, factory));
    }

    @Override
    public <T extends Entity> CreateEntityBuilder<T, FHRegistrate> entity(String name,
                                                                              EntityType.EntityFactory<T> factory, MobCategory classification) {
        return this.entity(self(), name, factory, classification);
    }

    @Override
    public <T extends Entity, P> CreateEntityBuilder<T, P> entity(P parent, String name,
                                                                  EntityType.EntityFactory<T> factory, MobCategory classification) {
        return (CreateEntityBuilder<T, P>) this.entry(name, (callback) -> {
            return CreateEntityBuilder.create(this, parent, name, callback, factory, classification);
        });
    }

    /* Util */

    public static <T extends Block> NonNullConsumer<? super T> casingConnectivity(
            BiConsumer<T, CasingConnectivity> consumer) {
        return entry -> onClient(() -> () -> registerCasingConnectivity(entry, consumer));
    }

    public static <T extends Block> NonNullConsumer<? super T> blockModel(
            Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        return entry -> onClient(() -> () -> registerBlockModel(entry, func));
    }

    public static <T extends Item> NonNullConsumer<? super T> itemModel(
            Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        return entry -> onClient(() -> () -> registerItemModel(entry, func));
    }

    public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
            Supplier<ConnectedTextureBehaviour> behavior) {
        return entry -> onClient(() -> () -> registerCTBehviour(entry, behavior));
    }

    protected static void onClient(Supplier<Runnable> toRun) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, toRun);
    }

    @OnlyIn(Dist.CLIENT)
    private static <T extends Block> void registerCasingConnectivity(T entry,
                                                                     BiConsumer<T, CasingConnectivity> consumer) {
        consumer.accept(entry, CreateClient.CASING_CONNECTIVITY);
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerBlockModel(Block entry,
                                           Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), func.get());
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerItemModel(Item entry,
                                          Supplier<NonNullFunction<BakedModel, ? extends BakedModel>> func) {
        CreateClient.MODEL_SWAPPER.getCustomItemModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), func.get());
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
        ConnectedTextureBehaviour behavior = behaviorSupplier.get();
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
    }

    /**
     * Create a simple fluid type, with custom textures and color
     * @param properties
     * @param stillTexture
     * @param flowingTexture
     * @param color
     * @return
     */
    public static FluidType simpleFluidType(FluidType.Properties properties, ResourceLocation stillTexture,
                                             ResourceLocation flowingTexture, int color) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }

                    @Override
                    public int getTintColor() {
                        return color;
                    }
                });
            }
        };
    }

    /**
     * Create a simple fluid type, with custom textures but default color 0xFFFFFFFF
     * @param properties
     * @param stillTexture
     * @param flowingTexture
     * @return
     */
    public static FluidType simpleFluidType(FluidType.Properties properties, ResourceLocation stillTexture,
                                            ResourceLocation flowingTexture) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }
                });
            }
        };
    }

    /**
     * Create a simple fluid type with default textures (water) and custom color
     * @param properties
     * @param color
     * @return
     */
    public static FluidType simpleFluidType(FluidType.Properties properties, int color) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return new ResourceLocation("block/water_still");
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return new ResourceLocation("block/water_flow");
                    }

                    @Override
                    public int getTintColor() {
                        return color;
                    }
                });
            }
        };
    }

    // standard fluid with name-based textures
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardFluid(String name) {
        return fluid(name, new ResourceLocation(getModid(), "fluid/" + name + "_still"), new ResourceLocation(getModid(), "fluid/" + name + "_flow"));
    }

    // standard fluid with custom textures, custom type factory
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardFluid(String name,
                                                                                   FluidBuilder.FluidTypeFactory typeFactory) {
        return fluid(name, new ResourceLocation(getModid(), "fluid/" + name + "_still"), new ResourceLocation(getModid(), "fluid/" + name + "_flow"),
                typeFactory);
    }

    // Flowing fluid with name-based textures
    public <T extends ForgeFlowingFluid> FluidBuilder<T, FHRegistrate> flowingFluid(String name,
                                                                                        FluidBuilder.FluidTypeFactory typeFactory, NonNullFunction<ForgeFlowingFluid.Properties, T> factory) {
        return entry(name,
                c -> new SimpleFluidBuilder<>(self(), self(), name, c, new ResourceLocation(getModid(), "fluid/" + name + "_still"),
                        new ResourceLocation(getModid(), "fluid/" + name + "_flow"), typeFactory, factory)).defaultLang();
    }

    // Flowing fluid with custom textures
    public <T extends ForgeFlowingFluid> FluidBuilder<T, FHRegistrate> flowingFluid(String name,
                                                                                        ResourceLocation still, ResourceLocation flow, FluidBuilder.FluidTypeFactory typeFactory,
                                                                                        NonNullFunction<ForgeFlowingFluid.Properties, T> factory) {
        return entry(name, c -> new SimpleFluidBuilder<>(self(), self(), name, c, still, flow, typeFactory, factory)).defaultLang();
    }

    // Virtual fluid with name-based textures
    public FluidBuilder<VirtualFluid, FHRegistrate> virtualFluid(String name) {
        return entry(name,
                c -> new SimpleFluidBuilder<VirtualFluid, FHRegistrate>(self(), self(), name, c,
                        new ResourceLocation(getModid(), "fluid/" + name + "_still"),
                        new ResourceLocation(getModid(), "fluid/" + name + "_flow"),
                        FHRegistrate::simpleFluidType, VirtualFluid::new)).defaultLang();
    }

    // Virtual fluid with custom textures
    public FluidBuilder<VirtualFluid, FHRegistrate> virtualFluid(String name, ResourceLocation still, ResourceLocation flow) {
        return entry(name, c -> new SimpleFluidBuilder<>(self(), self(), name, c, still, flow,
                FHRegistrate::simpleFluidType, VirtualFluid::new)).defaultLang();
    }

    // Virtual colored fluids

    // Cuation: you must set properties in this method due to registrate limitations
    // By default properties are empty except for the descriptionId

    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredFluid(String name, ResourceLocation still,
                                                                        ResourceLocation flow, int color, FluidType.Properties properties) {
        return entry(name,
                c -> new SimpleFluidBuilder<VirtualFluid, FHRegistrate>(self(), self(), name, c,
                        still, flow, () -> new FluidType(properties
                        .descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name)))) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override
                            public ResourceLocation getStillTexture() {
                                return still;
                            }

                            @Override
                            public ResourceLocation getFlowingTexture() {
                                return flow;
                            }

                            @Override
                            public int getTintColor() {
                                return color;
                            }
                        });
                    }
                }, VirtualFluid::new)).defaultLang();
    }

    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredFluid(String name, ResourceLocation still,
                                                                        ResourceLocation flow, int color) {
        return virtualColoredFluid(name, still, flow, color, FluidType.Properties.create().descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }

    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredWater(String name, int color,
                                                                        FluidType.Properties properties) {
        return virtualColoredFluid(name, STILL, FLOW, color, properties);
    }

    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredWater(String name, int color) {
        return virtualColoredFluid(name, STILL, FLOW, color, FluidType.Properties.create().descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }

    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredGas(String name, int color) {
        return virtualColoredFluid(name, STILL, FLOW, color, FluidType.Properties.create().density(-1000).descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }
    public FluidBuilder<VirtualFluid, FHRegistrate> virtualColoredLiquid(String name, int color) {
        return virtualColoredFluid(name, STILL, FLOW, color, FluidType.Properties.create().density(1000).descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }

    // Standard colored fluids

    // Standard fluid with custom textures and custom color
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardColoredFluid(String name, ResourceLocation still, ResourceLocation flowing, int color, FluidType.Properties properties) {
        return fluid("test", still, flowing, () ->
                new FluidType(properties.descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name)))) {
                    @Override
                    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                            @Override
                            public int getTintColor() {
                                return color;
                            }

                            @Override
                            public ResourceLocation getStillTexture() {
                                return still;
                            }

                            @Override
                            public ResourceLocation getFlowingTexture() {
                                return flowing;
                            }

                        });
                    }
                });
    }

    // No properties arg with custom textures
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardColoredFluid(String name, ResourceLocation still, ResourceLocation flowing, int color) {
        return standardColoredFluid(name, still, flowing, color, FluidType.Properties.create().descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }

    // Standard fluid with default water textures and custom color
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardColoredWater(String name, int color, FluidType.Properties properties) {
        return standardColoredFluid(name, STILL, FLOW, color, properties);
    }

    // No properties arg
    public FluidBuilder<ForgeFlowingFluid.Flowing, FHRegistrate> standardColoredWater(String name, int color) {
        return standardColoredFluid(name, STILL, FLOW, color, FluidType.Properties.create().descriptionId(Util.makeDescriptionId("fluid", new ResourceLocation(getModid(), name))));
    }

}
