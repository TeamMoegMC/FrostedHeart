package com.teammoeg.frostedheart;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.common.container.ElectrolyzerContainer;
import com.teammoeg.frostedheart.common.tile.ElectrolyzerTile;
import com.teammoeg.frostedheart.common.tile.GeneratorTileEntity;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.function.Supplier;

public class FHTileTypes {
    public static final DeferredRegister<TileEntityType<?>> REGISTER = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, FHMain.MODID);

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(
            ForgeRegistries.CONTAINERS, FHMain.MODID);

    public static final RegistryObject<TileEntityType<GeneratorTileEntity>> GENERATOR_T1 = REGISTER.register(
            "generator", makeType(() -> new GeneratorTileEntity(1, 1), () -> FHContent.Multiblocks.generator)
    );

    public static final RegistryObject<TileEntityType<ElectrolyzerTile>> ELECTROLYZER = REGISTER.register(
            "electrolyzer", makeType(() -> new ElectrolyzerTile(), () -> FHContent.Blocks.electrolyzer)
    );

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
        return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
    }

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
        return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
    }

    public static final RegistryObject<ContainerType<ElectrolyzerContainer>> ELECTROLYZER_CONTAINER = CONTAINERS
            .register("electrolyzer_container", () -> new ContainerType<>(ElectrolyzerContainer::new));

}
