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

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.FertilizedFarmlandBlock;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import com.teammoeg.frostedheart.content.climate.block.wardrobe.WardrobeBlock;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder.PartialBlockstate;
import net.minecraftforge.client.model.generators.loaders.ObjModelBuilder;

public class FHBlockStateGen {
	
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> existed() {
        return (c, p) -> {if(!p.models().existingFileHelper.exists(c.getId(), PackType.CLIENT_RESOURCES, ".json", "blockstates")) {
        	throw new NoSuchElementException("Blockstate definition for "+c.getId()+" does not exists!");
        }};
    }
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAll(
            ResourceLocation texture) {
        return (c, p) -> p.simpleBlock(c.get(), p.models()
                .cubeAll(c.getName(), texture));
    }
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleObj(
            ResourceLocation model) {
        return (c, p) -> p.simpleBlock(c.get(), p.models().withExistingParent(model.getPath(), new ResourceLocation("minecraft:block/block")).customLoader(ObjModelBuilder::begin)
                .automaticCulling(false)
                .modelLocation(makeObjRl(model))
                .flipV(true).end().renderType("cutout"));
    }

    public static ResourceLocation makeObjRl(ResourceLocation in)
    {
        return new ResourceLocation(in.getNamespace(), "models/block/"+in.getPath()+".obj");
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAll(
            String path) {
        return (c, p) -> p.simpleBlock(c.get(), p.models()
                .cubeAll(c.getName(), p.modLoc("block/" + path)));
    }
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simpleCubeAllRandom(
            String texturePath, int textureCount, boolean translucent) {

        return (c, p) -> {
            ResourceLocation texture = p.modLoc("block/" + texturePath);

            VariantBlockStateBuilder builder = p.getVariantBuilder(c.get());

            ConfiguredModel[] models = new ConfiguredModel[textureCount];
            for (int t = 1; t < textureCount + 1; t++) {
                ModelFile file;
                if (translucent) {
                    file = p.models().withExistingParent(c.getName() + "_" + t, p.mcLoc("block/cube_all"))
                            .texture("all", texture + "_" + t)
                            .renderType("translucent");
                } else {
                    file = p.models().withExistingParent(c.getName() + "_" + t, p.mcLoc("block/cube_all"))
                            .texture("all", texture + "_" + t);
                }
                models[t - 1] = new ConfiguredModel(file);
            }

            builder.partialState().addModels(models);
        };
    }

    // snow layered block state
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> snowLayered(
            String layerPath, String blockPath, String texturePath) {
        return (c, p) -> {
            SnowLayerBlock layer = (SnowLayerBlock) c.get();
            ResourceLocation texture = p.modLoc("block/" + texturePath);

            ModelFile[] models = new ModelFile[8];
            models[0] = p.models().withExistingParent(layerPath + "_height2", p.mcLoc("block/snow_height2"))
                    .texture("particle", texture)
                    .texture("texture", texture);
            models[1] = p.models().withExistingParent(layerPath + "_height4", p.mcLoc("block/snow_height4"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[2] = p.models().withExistingParent(layerPath + "_height6", p.mcLoc("block/snow_height6"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[3] = p.models().withExistingParent(layerPath + "_height8", p.mcLoc("block/snow_height8"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[4] = p.models().withExistingParent(layerPath + "_height10", p.mcLoc("block/snow_height10"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[5] = p.models().withExistingParent(layerPath + "_height12", p.mcLoc("block/snow_height12"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[6] = p.models().withExistingParent(layerPath + "_height14", p.mcLoc("block/snow_height14"))
                            .texture("particle", texture)
                            .texture("texture", texture);
            models[7] = p.models().withExistingParent(blockPath, p.mcLoc("block/cube_all"))
                            .texture("all", texture);

            p.getVariantBuilder(layer)
                    .partialState().with(SnowLayerBlock.LAYERS, 1)
                    .modelForState().modelFile(models[0]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 2)
                    .modelForState().modelFile(models[1]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 3)
                    .modelForState().modelFile(models[2]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 4)
                    .modelForState().modelFile(models[3]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 5)
                    .modelForState().modelFile(models[4]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 6)
                    .modelForState().modelFile(models[5]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 7)
                    .modelForState().modelFile(models[6]).addModel()
                    .partialState().with(SnowLayerBlock.LAYERS, 8)
                    .modelForState().modelFile(models[7]).addModel();
        };
    }

    // snow layered block state
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> layeredRandom(
            String layerPath, String blockPath, String texturePath, int textureCount, int layerCount, boolean translucent
    ) {
        return (c, p) -> {
            ResourceLocation texture = p.modLoc("block/" + texturePath);

            VariantBlockStateBuilder builder = p.getVariantBuilder(c.get());

            for (int l = 1; l < layerCount + 1; l++) {

                ConfiguredModel[] models = new ConfiguredModel[textureCount];
                for (int t = 1; t < textureCount + 1; t++) {
                    ModelFile file;
                    if (l == 8) {
                        if (translucent) {
                            file = p.models().withExistingParent(blockPath + "_" + t, p.mcLoc("block/cube_all"))
                                    .texture("all", texture + "_" + t)
                                    .renderType("translucent");
                        } else {
                            file = p.models().withExistingParent(blockPath + "_" + t, p.mcLoc("block/cube_all"))
                                    .texture("all", texture + "_" + t);
                        }
                    } else {
                        int pixels = l * 2;
                        if (translucent) {
                            file = p.models().withExistingParent(layerPath + "_height" + pixels + "_" + t, p.mcLoc("block/snow_height" + pixels))
                                    .texture("particle", texture + "_" + t)
                                    .texture("texture", texture + "_" + t)
                                    .renderType("translucent");
                        } else {
                            file = p.models().withExistingParent(layerPath + "_height" + pixels + "_" + t, p.mcLoc("block/snow_height" + pixels))
                                    .texture("particle", texture + "_" + t)
                                    .texture("texture", texture + "_" + t);
                        }

                    }

                    models[t - 1] = new ConfiguredModel(file);
                }

                builder.partialState().with(SnowLayerBlock.LAYERS, l).addModels(models);
            }
        };
    }

    public static NonNullBiConsumer<DataGenContext<Item, BlockItem>, RegistrateItemModelProvider> itemModelLayered(
            String path, String texturePath) {
        return (c, p) -> {
            ResourceLocation texture = p.modLoc("block/" + texturePath);
            p.withExistingParent(path, p.mcLoc("block/snow_height2"))
                    .texture("particle", texture)
                    .texture("texture", texture);
        };
    }

    public static NonNullBiConsumer<DataGenContext<Item, BlockItem>, RegistrateItemModelProvider> itemModelLayeredTranslucent(
            String path, String texturePath) {
        return (c, p) -> {
            ResourceLocation texture = p.modLoc("block/" + texturePath);
            p.withExistingParent(path, p.mcLoc("block/snow_height2"))
                    .texture("particle", texture)
                    .texture("texture", texture)
                    .renderType("translucent");
        };
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> ruinedMachines() {
        return b -> b
                .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                        .requiresCorrectToolForDrops()
                        .strength(10, 10)
                        .sound(SoundType.METAL)
                        .noOcclusion())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .tag(BlockTags.NEEDS_IRON_TOOL);
    }
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> rotateOrient(
            String path) {
        return (c, p) -> p.getVariantBuilder(c.get()).forAllStates(bs->{
        	AttachFace facing=bs.getValue(BlockStateProperties.ATTACH_FACE);
        	Direction orient=bs.getValue(BlockStateProperties.HORIZONTAL_FACING);
			
			float xrot=0;
			
		
			switch(facing) {
			case CEILING:xrot=180;break;
			case FLOOR:xrot=0;orient=orient.getOpposite();break;
			case WALL:xrot=90;orient=orient.getOpposite();break;
			}
			float yrot=orient.toYRot();
			return ConfiguredModel.builder().modelFile(p.models().getExistingFile(FHMain.rl(path))).rotationY((int) yrot).rotationX((int) xrot).build();
        });
    }
	public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> wardrobeState(String location) {
		return (ctx,provider)->{
			provider.getVariantBuilder(ctx.get()).forAllStates(bs->{
				Direction facing=bs.getValue(BlockStateProperties.HORIZONTAL_FACING);
				float yrot=facing.toYRot();
				StringBuilder sb=new StringBuilder(location);
				switch(bs.getValue(WardrobeBlock.HALF)) {
				case UPPER:sb.append("_top");break;
				case LOWER:sb.append("_bottom");break;
				}
				switch(bs.getValue(WardrobeBlock.HINGE)) {
				case LEFT:sb.append("_left");break;
				case RIGHT:sb.append("_right");break;
				}
				if(bs.getValue(WardrobeBlock.OPEN)) {
					sb.append("_open");
				}
				return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(FHMain.rl(sb.toString()))).rotationY((int) yrot).build();
			});
		};
	}
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> farmland() {
        return (c, p) -> {
            FertilizedFarmlandBlock layer = (FertilizedFarmlandBlock) c.get();

            ModelFile origin = p.models().withExistingParent("fertilized_farmland",p.mcLoc("block/farmland"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.mcLoc("block/farmland"));;
            ModelFile origin_moist = p.models().withExistingParent("fertilized_farmland_moist",p.mcLoc("block/farmland_moist"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.mcLoc("block/farmland_moist"));

            ModelFile[] fertilized =new ModelFile[3];
            fertilized[0] = p.models().withExistingParent("fertilized_farmland_increasing",p.mcLoc("block/farmland"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_increasing"));
            fertilized[1] = p.models().withExistingParent("fertilized_farmland_accelerated",p.mcLoc("block/farmland"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_accelerated"));
            fertilized[2] = p.models().withExistingParent("fertilized_farmland_preserved",p.mcLoc("block/farmland"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_preserved"));

            ModelFile[] fertilized_moist =new ModelFile[3];
            fertilized_moist[0] = p.models().withExistingParent("fertilized_farmland_moist_increasing",p.mcLoc("block/farmland_moist"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_moist_increasing"));
            fertilized_moist[1] = p.models().withExistingParent("fertilized_farmland_moist_accelerated",p.mcLoc("block/farmland_moist"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_moist_accelerated"));
            fertilized_moist[2] = p.models().withExistingParent("fertilized_farmland_moist_preserved",p.mcLoc("block/farmland_moist"))
                    .texture("dirt",p.mcLoc("block/dirt"))
                    .texture("top",p.modLoc("block/fertilized/fertilized_farmland_moist_preserved"));

            /*for(int j = 0; j < 7; j++) {
                p.getVariantBuilder(layer)
                        .partialState().with(FertilizedFarmlandBlock.FERTILIZER, 0).with(FertilizedFarmlandBlock.MOISTURE,j)
                        .modelForState().modelFile(origin).addModel();
            }
            p.getVariantBuilder(layer)
                    .partialState().with(FertilizedFarmlandBlock.FERTILIZER, 0).with(FertilizedFarmlandBlock.MOISTURE,7)
                    .modelForState().modelFile(origin_moist).addModel();*/
            for (int i = 0; i < 3; i++) {
            	FertilizerType type=FertilizerType.values()[i];
                for(int j = 0; j < 7; j++) {
                    p.getVariantBuilder(layer)
                            .partialState().with(FertilizedDirt.FERTILIZER, type).with(FertilizedFarmlandBlock.MOISTURE,j)
                            .modelForState().modelFile(fertilized[i]).addModel();
                }
                p.getVariantBuilder(layer)
                        .partialState().with(FertilizedDirt.FERTILIZER, type).with(FertilizedFarmlandBlock.MOISTURE,7)
                        .modelForState().modelFile(fertilized_moist[i]).addModel();
            }
        };
    }

}
