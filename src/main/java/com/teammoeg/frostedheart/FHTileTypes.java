package com.teammoeg.frostedheart;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.common.GeneratorTileEntity;
import net.minecraft.block.Block;
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

    public static final RegistryObject<TileEntityType<GeneratorTileEntity>> GENERATOR_T1_R1 = REGISTER.register(
            "generator", makeType(() -> new GeneratorTileEntity(1, 1), () -> FHBlocks.generator)
    );

    public static final RegistryObject<TileEntityType<GeneratorTileEntity>> GENERATOR_T2_R1 = REGISTER.register(
            "generator_t2_r1", makeType(() -> new GeneratorTileEntity(2, 1), () -> FHBlocks.generator)
    );

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
        return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
    }

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
        return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
    }
}
