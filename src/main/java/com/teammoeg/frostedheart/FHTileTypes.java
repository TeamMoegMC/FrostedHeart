package com.teammoeg.frostedheart;

import java.util.Collection;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.content.cmupdate.CMUpdateTileEntity;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import com.teammoeg.frostedheart.content.decoration.oilburner.OilBurnerTileEntity;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.DebugHeaterTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.HeatPipeTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.radiator.RadiatorTileEntity;
import com.teammoeg.frostedheart.research.machines.DrawingDeskTileEntity;

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
            "generator", makeType(() -> new T1GeneratorTileEntity(1, 2, 1), () -> FHMultiblocks.generator)
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
            "heat_radiator", makeType(() -> new RadiatorTileEntity(), () -> FHMultiblocks.radiator));

    public static final RegistryObject<TileEntityType<T2GeneratorTileEntity>> GENERATOR_T2 = REGISTER.register(
            "generator_t2", makeType(() -> new T2GeneratorTileEntity(1, 2, 1), () -> FHMultiblocks.generator_t2)
    );
    public static final RegistryObject<TileEntityType<OilBurnerTileEntity>> OIL_BURNER = REGISTER.register(
            "oil_burner", makeType(() -> new OilBurnerTileEntity(), () -> FHBlocks.oilburner)
    );

	public static final RegistryObject<TileEntityType<CMUpdateTileEntity>> CMUPDATE = REGISTER.register(
            "cm_update", makeType(() -> new CMUpdateTileEntity(), () -> FHBlocks.cmupdate)
    );

    public static final RegistryObject<TileEntityType<DrawingDeskTileEntity>> DRAWING_DESK = REGISTER.register(
            "drawing_desk", makeType(() -> new DrawingDeskTileEntity(), () -> FHBlocks.drawing_desk)
    );
    public static final RegistryObject<TileEntityType<RelicChestTileEntity>> RELIC_CHEST = REGISTER.register(
            "relic_chest", makeType(() -> new RelicChestTileEntity(), () -> FHBlocks.relic_chest)
    );

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeType(Supplier<T> create, Supplier<Block> valid) {
        return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
    }

    private static <T extends TileEntity> Supplier<TileEntityType<T>> makeTypeMultipleBlocks(Supplier<T> create, Supplier<Collection<Block>> valid) {
        return () -> new TileEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
    }

}