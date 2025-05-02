package com.teammoeg.frostedheart.mixin.minecraft;


import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(WorldOpenFlows.class)
public class MixinWorldOpenFlows {
    /*@Redirect(
            method = "loadWorldStem",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/server/WorldLoader;loadWorldStem(...)Ljava/util/concurrent/CompletableFuture;")
    )*/
    @Overwrite
    public WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess pLevelStorage, boolean pSafeMode, PackRepository pPackRepository) throws Exception {

//        if (cir.getReturnValue() == null) {
//            System.out.println("WorldStem 正在重建默认配置...");




            LevelSettings levelSettings = new LevelSettings(
                    "New World",
                    GameType.SURVIVAL,
                    false,
                    Difficulty.NORMAL,
                    false,
                    new GameRules(),
                    WorldDataConfiguration.DEFAULT
            );




            PackRepository packrepository = ServerPacksSource.createPackRepository(pLevelStorage);
            WorldDataConfiguration worlddataconfiguration = pLevelStorage.getDataConfiguration();
            WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(packrepository, worlddataconfiguration, false, false);

            Function<RegistryAccess, WorldDimensions> pDimensionsGetter = WorldPresets::createNormalWorldDimensions;


            WorldStem newWorldStem = (WorldStem)this.loadWorldDataBlocking(worldloader$packconfig, (dataLoadContext) -> {

                WorldDimensions Dimensions = (WorldDimensions)pDimensionsGetter.apply(dataLoadContext.datapackWorldgen());
            WorldGenSettings newSettings = new WorldGenSettings(
                    WorldOptions.defaultWithRandomSeed(), // 默认随机种子
                    Dimensions
            );

                WorldDimensions.Complete worlddimensions$complete = Dimensions.bake(dataLoadContext.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));

                return new WorldLoader.DataLoadOutput(new PrimaryLevelData(levelSettings, newSettings.options(), worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()), worlddimensions$complete.dimensionsRegistryAccess());
            }, WorldStem::new);



            // 5. 创建并返回新的 WorldStem
//            WorldStem newWorldStem = new WorldStem(worldDataR, registryAccess, );
//            cir.setReturnValue(newWorldStem);

//        }
        return newWorldStem;
    }

    public  <D, R> R loadWorldDataBlocking(WorldLoader.PackConfig pPackConfig, WorldLoader.WorldDataSupplier<D> pWorldDataSupplier, WorldLoader.ResultFactory<D, R> pResultFactory) throws Exception {
        Minecraft minecraft = Minecraft.getInstance();

        WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(pPackConfig, Commands.CommandSelection.INTEGRATED, 2);
        CompletableFuture<R> completablefuture = WorldLoader.load(worldloader$initconfig, pWorldDataSupplier, pResultFactory, Util.backgroundExecutor(), minecraft);


        Minecraft var10000 = minecraft;
        Objects.requireNonNull(completablefuture);
        var10000.managedBlock(completablefuture::isDone);
        return completablefuture.get();
    }
}
