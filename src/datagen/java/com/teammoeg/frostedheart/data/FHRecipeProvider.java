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

package com.teammoeg.frostedheart.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.register.IEItems;
import com.simibubi.create.Create;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPTags;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.PhysicalState;
import com.teammoeg.frostedheart.content.climate.data.*;
import com.teammoeg.frostedresearch.FRContents;
import com.yanny.age.stone.subscribers.ItemSubscriber;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import blusunrize.immersiveengineering.api.IETags;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.ExcelHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.trade.policy.TradeBuilder;

import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipeProvider extends RecipeProvider {
	private final HashMap<String, Integer> PATH_COUNT = new HashMap<>();
	DataGenerator dg;
	public FHRecipeProvider(DataGenerator generatorIn) {
		super(generatorIn.getPackOutput());
		dg=generatorIn;
	}

	@SuppressWarnings("resource")
	@Override
	protected void buildRecipes(@Nonnull Consumer<FinishedRecipe> out) {

		/*try (PrintStream ps=new PrintStream(openDebugFile("food_healing.csv"));
				Scanner sc = new Scanner(openDatagenResource("/data/frostedheart/data/food_values.csv"), "UTF-8")) {
			if(sc.hasNextLine()) {
				sc.nextLine();
				while(sc.hasNextLine()) {
					String line=sc.nextLine();
					if(!line.isEmpty()) {
						String[] parts=line.split(",");
						if(parts.length==0)break;
						ResourceLocation id=new ResourceLocation(FHMain.MODID,"diet_value/"+parts[0].replaceAll(":","/"));
						ResourceLocation item=new ResourceLocation(parts[0]);
						Item it= CRegistryHelper.getItem(item);
						if(it==null||it==Items.AIR) {
							FHMain.LOGGER.warn("TWR Recipe: " + item + " not exist");
							FHMain.LOGGER.info(item+","+parts[1]);
						}else {
							FoodProperties f=it.getFoodProperties();
							if(f==null)
								FHMain.LOGGER.info("Food Value: " + item+","+parts[1]);
							else
								FHMain.LOGGER.info("Food Nutrition: " + item+","+f.getNutrition());
						}
						NutritionRecipeBuilder dvb=new NutritionRecipeBuilder().item(it);
						float grain=Float.parseFloat(parts[2])*10f;
						float veg=Float.parseFloat(parts[3])*10f;
						float oil=Float.parseFloat(parts[4])*10f;
						float protein=Float.parseFloat(parts[5])*10f;
						dvb.nutrition(grain,veg,oil,protein);
						dvb.save(out,id);
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/

		//biome
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/biome_temperature.xlsx"), m->{
			ResourceLocation biome=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("biome")));
			out.accept(new BiomeTempData(biome,(float)ExcelHelper.getCellValueAsNumber(m.get("temperature"))).toFinished(FHMain.rl("biome_temperature/"+biome.getPath())));
		});
		//block
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/block_temperature.xlsx"), m->{
			
			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("block")));
			if(!block.getPath().isEmpty())
			out.accept(new BlockTempData(CRegistryHelper.getBlock(block),
					(float)ExcelHelper.getCellValueAsNumber(m.get("temperature")),
					ExcelHelper.getCellValueAsBoolean(m.get("level_divide")),
					ExcelHelper.getCellValueAsBoolean(m.get("must_lit"))
					).toFinished(FHMain.rl("block_temperature/"+block.getPath())));
		});
		Set<BlockState> allStates=new HashSet<>();
		// state transition
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/state_transition.xlsx"), m->{
			try {
				String name=ExcelHelper.getCellValueAsString(m.get("block"));
			BlockState block=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),name,true).blockState();
			String solidName=ExcelHelper.getCellValueAsString(m.get("solid"));
			BlockState solid=null;
			if(solidName!=null&&!solidName.isEmpty())
				try {
					solid=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),solidName,true).blockState();
				}catch(Throwable t) {
					FHMain.LOGGER.error("error parsing solid "+solidName+" for state transition");
					t.printStackTrace();
				}
			String liquidName=ExcelHelper.getCellValueAsString(m.get("liquid"));
			BlockState liquid=null;
			if(liquidName!=null&&!liquidName.isEmpty())
				try {
					liquid=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),liquidName,true).blockState();
				}catch(Throwable t) {
					FHMain.LOGGER.error("error parsing liquid "+liquidName+" for state transition");
					t.printStackTrace();
				}
			String gasName=ExcelHelper.getCellValueAsString(m.get("gas"));
			BlockState gas=null;
			if(gasName!=null&&!gasName.isEmpty())
				try {
					gas=BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(),gasName,true).blockState();
				}catch(Throwable t) {
					FHMain.LOGGER.error("error parsing gas "+gasName+" for state transition");
					t.printStackTrace();
				}
			String blockPath=name.replaceAll(":", "/").replaceAll("[^A-Za-z_/]", "_");
				if(!allStates.add(block)) {
					FHMain.LOGGER.error("Duplicated state "+block+" for state transition");
					return;
				}
				out.accept(new StateTransitionData(
						block,
						ExcelHelper.getCellValueAsBoolean(m.get("all_state")),
						PhysicalState.fromString(ExcelHelper.getCellValueAsString(m.get("state"))),
						solid,
						liquid,
						gas,
						(float)ExcelHelper.getCellValueAsNumber(m.get("freeze_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("melt_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("condense_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("evaporate_temp")),
						(int)ExcelHelper.getCellValueAsNumber(m.get("heat_capacity")),
						ExcelHelper.getCellValueAsBoolean(m.get("will_transit"))
				).toFinished(FHMain.rl("state_transition/"+blockPath)));

			}catch(Throwable t) {
				t.printStackTrace();
			}
		});
		//world
		out.accept(new WorldTempData(new ResourceLocation("the_nether"),300).toFinished(FHMain.rl("level_temperature/nether")));
		out.accept(new WorldTempData(new ResourceLocation("the_end"),-300).toFinished(FHMain.rl("level_temperature/the_end")));
		//plant
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/plant_temperature.xlsx"), m->{
			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("block")));
			ResourceLocation dead=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("dead")));
			out.accept(new PlantTempData(CRegistryHelper.getBlockThrow(block),
					(float)ExcelHelper.getCellValueAsNumber(m.get("grow_time_days")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_fertilize")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_grow")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_survive")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_fertilize")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_grow")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_survive")),
					!ExcelHelper.getCellValueAsBoolean(m.get("survive_snow")),
					!ExcelHelper.getCellValueAsBoolean(m.get("survive_blizzard")),
					CRegistryHelper.getBlockThrow(dead),
					ExcelHelper.getCellValueAsBoolean(m.get("will_die")),
					(int)ExcelHelper.getCellValueAsNumber(m.get("heat_capacity")),
					(int)ExcelHelper.getCellValueAsNumber(m.get("min_skylight")),
					(int)ExcelHelper.getCellValueAsNumber(m.get("max_skylight"))
					).toFinished(FHMain.rl("plant_temperature/"+block.getPath())));
		});
		//drink
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/drink_temperature.xlsx"), m->{
			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("fluid")));
			out.accept(new DrinkTempData(CRegistryHelper.getFluid(block),
					(float)ExcelHelper.getCellValueAsNumber(m.get("heat"))
					).toFinished(FHMain.rl("drink_temperature/"+block.getPath())));
		});

		/*ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/nutrition.xlsx"), m->{
			ResourceLocation itemid = new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("ID")));
			NutritionRecipeBuilder dvb =new NutritionRecipeBuilder().ItemID(itemid);
			dvb.nutrition((float)ExcelHelper.getCellValueAsNumber(m.get("Gr2")),(float)ExcelHelper.getCellValueAsNumber(m.get("Va2")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("Oi2")),(float)ExcelHelper.getCellValueAsNumber(m.get("Pt2")));
			dvb.save(out,new ResourceLocation(FHMain.MODID,"diet_value/"+ itemid.getNamespace()+ "/"+ itemid.getPath()));

		});*/
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/new_food_value.xlsx"), m->{
			ResourceLocation itemid = new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("id")));
			NutritionRecipeBuilder dvb =new NutritionRecipeBuilder().ItemID(itemid);
			float base=(float)ExcelHelper.getCellValueAsNumber(m.get("Base"));
			if(base<=0)return;
			float gr=(float)ExcelHelper.getCellValueAsNumber(m.get("Grain"));
			float ve=(float)ExcelHelper.getCellValueAsNumber(m.get("Vegetable"));
			float oi=(float)ExcelHelper.getCellValueAsNumber(m.get("Fat"));
			float pr=(float)ExcelHelper.getCellValueAsNumber(m.get("Protein"));
			dvb.based(base/(gr+ve+oi+pr)*40000);
			dvb.nutrition(gr,ve,oi,pr);
			dvb.save(out,new ResourceLocation(FHMain.MODID,"diet_value/"+ itemid.getNamespace()+ "/"+ itemid.getPath()));

		});
//		CPFluids.getAll().filter(o->!Arrays.stream(ovride).anyMatch(CRegistries.getRegistryName(o).getPath()::equals)).forEach(f-> {
//
//				out.accept(new WaterLevelFluidRecipe(new ResourceLocation(FHMain.MODID,"water_level/"+ CRegistries.getRegistryName(f).getPath()+"_thermos"),Ingredient.of(ItemTags.create(new ResourceLocation(FHMain.MODID,"thermos"))),f,2,2));
//		});
//		CPFluids.getAll().filter(o-> CRegistries.getRegistryName(o).getPath().equals("dilute_soup")).forEach(f-> {
//
//				out.accept(new WaterLevelFluidRecipe(new ResourceLocation(FHMain.MODID,"water_level/"+ CRegistries.getRegistryName(f).getPath()+"_thermos"),Ingredient.of(ItemTags.create(new ResourceLocation(FHMain.MODID,"thermos"))),f,3,2));
//		});
        Map<String, Float[]> materials = Map.of(
                "hay", new Float[]{.2f,0.2f, 250.0f},
                "hide", new Float[]{.4f,0.6f, 300.0f},
                //"cotton", new Float[]{0.3f, 400.0f},
                "wool", new Float[]{.2f,0.2f, 500.0f}
                //"down", new Float[]{0.7f, 600.0f}
        );
        //List<ArmorTempData> armorData=new ArrayList<>();
        for(BodyPart part:BodyPart.values()) {
        	if(part.slot!=null) {
        		
        		out.accept(armorData(FHItems.straw_lining,part,materials.get("hay")[2],materials.get("hay")[1],materials.get("hay")[0]));
        		out.accept(armorData(FHItems.buff_coat,part,materials.get("hide")[2],materials.get("hide")[1],materials.get("hide")[0]));
        		out.accept(armorData(FHItems.gambeson,part,materials.get("wool")[2],materials.get("wool")[1],materials.get("wool")[0]));
        		out.accept(armorData(FHItems.kelp_lining,part,200f,.5f,0.8f));
        		//out.accept(armorData(FHItems.cotton,part,500f,.2f,.5f));
        		//out.accept(armorData(FHItems.straw_lining,part,600f,.2f,.7f));
        	}
        }

        out.accept(armorArmorData(FHItems.space_hat,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_jacket,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_pants,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_boots,500f,.2f,1.0f));

		// leather is good at water resistance but bad insulation
		out.accept(armorData(FHItems.hide_hat, BodyPart.HEAD, 200f,.2f,0.5f));
		out.accept(armorData(FHItems.hide_jacket, BodyPart.TORSO, 200f,.2f,0.5f));
		out.accept(armorData(FHItems.hide_pants, BodyPart.LEGS, 200f,.2f,0.5f));
		out.accept(armorData(FHItems.hide_boots, BodyPart.FEET, 200f,.2f,0.5f));
		out.accept(armorData(FHItems.hide_gloves, BodyPart.HANDS, 200f,.2f,0.5f));

		out.accept(armorData(FHItems.hay_hat, BodyPart.HEAD, 250f,.2f,0.1f));
		out.accept(armorData(FHItems.hay_jacket, BodyPart.TORSO, 250f,.2f,0.1f));
		out.accept(armorData(FHItems.hay_pants, BodyPart.LEGS, 250f,.2f,0.1f));
		out.accept(armorData(FHItems.hay_boots, BodyPart.FEET, 250f,.2f,0.1f));
		out.accept(armorData(FHItems.hay_gloves, BodyPart.HANDS, 250f,.2f,0.1f));

		out.accept(armorData(FHItems.rabbit_hat, BodyPart.HEAD, 300f,.2f,0.05f));
		out.accept(armorData(FHItems.rabbit_jacket, BodyPart.TORSO, 300f,.2f,0.05f));
		out.accept(armorData(FHItems.rabbit_pants, BodyPart.LEGS, 300f,.2f,0.05f));
		out.accept(armorData(FHItems.rabbit_fur_socks, BodyPart.FEET, 300f,.2f,0.05f));
		out.accept(armorData(FHItems.rabbit_gloves, BodyPart.HANDS, 300f,.2f,0.05f));

		out.accept(armorData(FHItems.fox_hat, BodyPart.HEAD, 400f,.2f,0.4f));
		out.accept(armorData(FHItems.fox_jacket, BodyPart.TORSO, 400f,.2f,0.4f));
		out.accept(armorData(FHItems.fox_pants, BodyPart.LEGS, 400f,.2f,0.4f));
		out.accept(armorData(FHItems.fox_boots, BodyPart.FEET, 400f,.2f,0.4f));
		out.accept(armorData(FHItems.fox_gloves, BodyPart.HANDS, 400f,.2f,0.4f));

		out.accept(armorData(FHItems.wolf_hat, BodyPart.HEAD, 350f,.2f,0.3f));
		out.accept(armorData(FHItems.wolf_jacket, BodyPart.TORSO, 350f,.2f,0.3f));
		out.accept(armorData(FHItems.wolf_pants, BodyPart.LEGS, 350f,.2f,0.3f));
		out.accept(armorData(FHItems.wolf_boots, BodyPart.FEET, 350f,.2f,0.3f));
		out.accept(armorData(FHItems.wolf_gloves, BodyPart.HANDS, 350f,.2f,0.3f));

		out.accept(armorData(FHItems.polar_bear_hat, BodyPart.HEAD, 900f,.2f,0.85f));
		out.accept(armorData(FHItems.polar_bear_jacket, BodyPart.TORSO,  900f,.2f,0.85f));
		out.accept(armorData(FHItems.polar_bear_pants, BodyPart.LEGS, 900f,.2f,0.85f));
		out.accept(armorData(FHItems.polar_bear_boots, BodyPart.FEET, 900f,.2f,0.85f));
		out.accept(armorData(FHItems.polar_bear_gloves, BodyPart.HANDS, 900f,.2f,0.85f));

		out.accept(armorData(FHItems.wool_hat, BodyPart.HEAD, 500f,.2f,0.1f));
		out.accept(armorData(FHItems.wool_jacket, BodyPart.TORSO, 500f,.2f,0.1f));
		out.accept(armorData(FHItems.wool_pants, BodyPart.LEGS, 500f,.2f,0.1f));
		out.accept(armorData(FHItems.wool_boots, BodyPart.FEET, 500f,.2f,0.1f));
		out.accept(armorData(FHItems.wool_gloves, BodyPart.HANDS, 500f,.2f,0.1f));


		buildTradePolicies(out);
	}
	private FinishedRecipe armorData(ItemLike item,BodyPart part,float insulation,float heat_proof,float cold_proof) {
		
		return new ArmorTempData(item.asItem(), Optional.of(part), insulation, heat_proof,cold_proof).toFinished(FHMain.rl("armor_insulation/lining/"+CRegistryHelper.getPath(item.asItem())+"_"+part.name().toLowerCase()));
		
	}
	private FinishedRecipe armorArmorData(ItemLike item,float insulation,float heat_proof,float cold_proof) {
		return new ArmorTempData(item.asItem(), Optional.of(BodyPart.fromVanilla(((Equipable)item.asItem()).getEquipmentSlot())), insulation, heat_proof,cold_proof).toFinished(FHMain.rl("armor_insulation/"+CRegistryHelper.getPath(item.asItem())));
		
	}
	private Workbook openWorkBook(String name) {
		try {
			HSSFWorkbook book=new HSSFWorkbook(openDatagenResource(name));
			return book;
		} catch (OfficeXmlFileException | IOException e) {
			try {
				XSSFWorkbook book=new XSSFWorkbook(openDatagenResource(name));
				return book;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return new HSSFWorkbook();
	}

	public static InputStream openDatagenResource(String name) {
		return FHRecipeProvider.class.getResourceAsStream(name);
	}
	public static File openDebugFile(String name) {
		File parfile=FMLPaths.GAMEDIR.get().resolve("debug_output").toFile();
		parfile.mkdirs();
		return new File(parfile,name);
	}

	public static ItemStack createCagedEntityFromString(String nbtString) {
		ItemStack cageStack = new ItemStack(ModRegistry.CAGE_ITEM.get());

		try {
			// Use TagParser to parse the NBT string
			CompoundTag nbt = TagParser.parseTag(nbtString);
			cageStack.setTag(nbt);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return cageStack;
	}

	// Alternative method using NBT parsing (simpler but requires the exact string)
	public static ItemStack cagedRabbit() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:0,Air:300s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:0.0d,Name:\"forge:step_height_addition\"},{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"},{Base:16.0d,Modifiers:[{Amount:-0.05702389017647923d,Name:\"Random spawn bonus\",Operation:1,UUID:[I;795285848,-850571614,-1612973660,805794482]}],Name:\"minecraft:generic.follow_range\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:3.0f,HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:0b,MoreCarrotTicks:0,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],RabbitType:5,Rotation:[0.0f,0.0f],\"forge:spawn_type\":\"SPAWN_EGG\",id:\"minecraft:rabbit\"},Name:\"Rabbit\",Scale:1.0f,UUID:[I;1022712411,-492944690,-1891121019,-1631982831]}}}";
		return createCagedEntityFromString(s);
	}

	public static ItemStack cagedBoar() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:-23406,Air:300s,Anger:0s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:0.0d,Name:\"forge:step_height_addition\"},{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},Generation:1,HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:15.0f,HurtBy:\"\",HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:0b,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],Rotation:[0.0f,0.0f],id:\"stone_age:boar\"},Name:\"Boar\",Scale:0.5833333f,UUID:[I;-1895757116,-1906228399,-2085885897,1184858982]}}}";
		return createCagedEntityFromString(s);
	}

	public static ItemStack cagedFox() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:0,Air:300s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:32.0d,Modifiers:[{Amount:0.0607411310162985d,Name:\"Random spawn bonus\",Operation:1,UUID:[I;-1877090398,1958232654,-1936817837,-988659072]}],Name:\"minecraft:generic.follow_range\"},{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"},{Base:0.0d,Name:\"forge:step_height_addition\"}],Brain:{memories:{}},CanPickUpLoot:1b,CanUpdate:1b,Crouching:0b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:10.0f,HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:0b,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],Rotation:[0.0f,0.0f],Sitting:0b,Sleeping:1b,Trusted:[],Type:\"snow\",\"forge:spawn_type\":\"SPAWN_EGG\",id:\"minecraft:fox\"},Name:\"狐狸\",Scale:0.78125f,UUID:[I;1000385981,-924824054,-1685937253,-1638237081]}}}";
		return createCagedEntityFromString(s);
	}

	public static ItemStack cagedAuroch() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:-23930,Air:300s,Anger:0s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"},{Base:0.0d,Name:\"forge:step_height_addition\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},Generation:1,HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:20.0f,HurtBy:\"\",HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:0b,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],Rotation:[0.0f,0.0f],id:\"stone_age:auroch\"},Name:\"野牛\",Scale:0.5833333f,UUID:[I;-967854644,1756646617,-1205931748,45898938]}}}";
		return createCagedEntityFromString(s);
	}

	public static ItemStack cagedFowl() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:0,Air:300s,Anger:0s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:16.0d,Modifiers:[{Amount:0.031182960451612573d,Name:\"Random spawn bonus\",Operation:1,UUID:[I;-687819678,1971996645,-2002249144,-837993083]}],Name:\"minecraft:generic.follow_range\"},{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"},{Base:0.0d,Name:\"forge:step_height_addition\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},Generation:0,HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:6.0f,HurtBy:\"\",HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:1b,Motion:[0.0d,-0.0784000015258789d,0.0d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],Rotation:[0.0f,0.0f],\"forge:spawn_type\":\"SPAWN_EGG\",id:\"stone_age:fowl\"},Name:\"野鸡\",Scale:0.89285713f,UUID:[I;123255624,343491862,-1805069578,-1463733712]}}}";
		return createCagedEntityFromString(s);
	}

	public static ItemStack cagedMouflon() {
		String s = "{BlockEntityTag:{MobHolder:{EntityData:{AbsorptionAmount:0.0f,Age:-23933,Air:300s,Anger:0s,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.30000001192092896d,Name:\"minecraft:generic.movement_speed\"},{Base:0.0d,Name:\"forge:step_height_addition\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,DeathTime:0s,FallDistance:0.0f,FallFlying:0b,Fire:0s,ForcedAge:0,ForgeCaps:{\"curios:inventory\":{Curios:[]}},ForgeData:{},Generation:1,HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:20.0f,HurtBy:\"\",HurtByTimestamp:0,HurtTime:0s,InLove:0,Invulnerable:0b,LeftHanded:0b,Motion:[0.13874070260499188d,-0.0784000015258789d,0.0013633874297243307d],OnGround:1b,PersistenceRequired:0b,PortalCooldown:0,Pos:[0.5d,0.0626d,0.5d],Rotation:[0.0f,0.0f],id:\"stone_age:mouflon\"},Name:\"野羊\",Scale:0.5833333f,UUID:[I;62466033,-929150022,-1427858985,1970102500]}}}";
		return createCagedEntityFromString(s);
	}

	private void buildTradePolicies(@Nonnull Consumer<FinishedRecipe> out) {

		// test
//		trade().group().buy(10,10,10,FHItems.rye_bread.get())
//		.buy(1, 0.1f,20,FHItems.straw_lining.get())
//		.buy(10,10,10,Items.RAW_COPPER)
//		.sell(10, 1, 100,FHItems.energy_core.get())
//		.sell(10, 1, 5,Items.COPPER_INGOT)
//		.basic()
//		.finish()
//		.weight(1).id("test").finish(out);

		// general:
		// maxstore: storage capacity for that item
		// recover: average daily recover of the need (for buying) and for saling.
		// price: relative "value" of item

		// night soil collector
		trade().group()
				// buys food
				// .buy("refugee_needs", 30, 5, 3, Ingredient.of(FHTags.Items.REFUGEE_NEEDS.tag))
				.buy("vegetables", 30, 3, 8, Ingredient.of(CPTags.Items.VEGETABLES))
				.buy("cereals", 30, 3, 5, Ingredient.of(CPTags.Items.CEREALS))
				.buy("cereals_baked", 30, 3, 7, Ingredient.of(CPTags.Items.BAKED))
				.buy("eggs", 30, 1, 10, Ingredient.of(CPTags.Items.EGGS))
				.buy(30, 2, 10, ItemSubscriber.fat.asItem())
				.buy("sugar", 30, 2, 10, Ingredient.of(CPTags.Items.SUGAR))
				.buy("walnut", 30, 2, 10, Ingredient.of(CPTags.Items.WALNUT))
				// buys clothes
				.buy(5, 0.1f, 15, FHItems.straw_lining.get())
				.buy(5, 0.1f, 30, FHItems.buff_coat.get())
				.buy(5, 0.1f, 60, FHItems.gambeson.get())
				// buys processed fuel
				.buy(64, 5, 6, IEItems.Ingredients.COAL_COKE.asItem())
				// buys job related tools
				.buy(3, 1, 20, Items.IRON_SWORD.asItem())
				.buy(3, 1, 15, FHItems.BRONZE_SHOVEL.get())
				.buy(3, .5f, 40, IEItems.Tools.STEEL_SHOVEL.asItem())
				.buy(1, .1f, 30, FHItems.SNOWSHOES.get())
				.buy(1, .1f, 30, FHItems.ICE_SKATES.get())
				// sells hunting results
				.sell(256, 32F, 1, FHItems.night_soil.asItem())
				.sell(256, 16F, 1, FHItems.dung.asItem())
				// sell intelligence, but slow
				.sell(1, 0.03f, 200, FRContents.Items.intelligence.get())
				.basic()
				.finish()
				.profession(VillagerProfession.SHEPHERD)
				.levelExp(1000, 2000, 3000, 4000, 5000)
				.weight(1)
				.id("night_soil_collector")
				.finish(out);

		// hunting
		trade().group()
				// buys food
				// .buy("refugee_needs", 30, 5, 3, Ingredient.of(FHTags.Items.REFUGEE_NEEDS.tag))
				.buy("vegetables", 30, 3, 8, Ingredient.of(CPTags.Items.VEGETABLES))
				.buy("cereals", 30, 3, 5, Ingredient.of(CPTags.Items.CEREALS))
				.buy("cereals_baked", 30, 3, 7, Ingredient.of(CPTags.Items.BAKED))
				.buy("eggs", 30, 1, 10, Ingredient.of(CPTags.Items.EGGS))
				.buy(30, 2, 10, ItemSubscriber.fat.asItem())
				.buy("sugar", 30, 2, 10, Ingredient.of(CPTags.Items.SUGAR))
				.buy("walnut", 30, 2, 10, Ingredient.of(CPTags.Items.WALNUT))
				// buys clothes
				.buy(5, 0.1f, 15, FHItems.straw_lining.get())
				.buy(5, 0.1f, 30, FHItems.buff_coat.get())
				.buy(5, 0.1f, 60, FHItems.gambeson.get())
				// buys processed fuel
				.buy(64, 5, 6, IEItems.Ingredients.COAL_COKE.asItem())
				// buys job related tools
				.buy(64, 10, 1, Items.ARROW)
				.buy(3, 1, 10, Items.BOW)
				.buy(3, 1, 20, Items.IRON_SWORD)
				.buy(3, 1, 15, FHItems.BRONZE_SWORD.get())
				.buy(3, .5f, 40, IEItems.Tools.STEEL_SWORD.asItem())
				.buy(1, .1f, 30, FHItems.SNOWSHOES.get())
				.buy(1, .1f, 30, FHItems.ICE_SKATES.get())
				// sells hunting results
				.sell(4, 1f, 15, FHItems.wolf_hide.get())
				.sell(4, 0.6f, 20, FHItems.fox_hide.get())
				.sell(4, 2f, 10, Items.RABBIT_HIDE)
				.sell(12, 6f, 5, FHItems.WOLF_MEAT.get())
				.sell(12, 3f, 7, FHItems.FOX_MEAT.get())
				.sell(12, 3f, 5, ItemSubscriber.venison)
				.sell(12, 2f, 30, ItemSubscriber.fat)
				.sell(16, 6F, 4, Items.RABBIT)
				.sell(8, 5F, 20, Items.CHICKEN)
				.sell(8, 5F, 20, Items.FEATHER)
				.sell(16, 4F, 1, FHItems.night_soil.asItem())
				.sell(16, 8F, 1, FHItems.dung.asItem())
				.sell("caged_rabbit", 1, .1f, 100, cagedRabbit())
				.sell("caged_boar", 1, .05f, 500, cagedBoar())
				.sell("caged_boar", 1, .05f, 500, cagedAuroch())
				.sell("caged_boar", 1, .05f, 500, cagedMouflon())
				.sell("caged_boar", 1, .05f, 500, cagedFowl())
				.sell("caged_boar", 1, .025f, 1000, cagedFox())
				// sell intelligence, but slow
				.sell(2, 0.08f, 200, FRContents.Items.intelligence.get())
				.basic()
				.finish()
				.profession(VillagerProfession.BUTCHER)
				.levelExp(1000, 2000, 3000, 4000, 5000)
				.weight(10)
				.id("hunter")
				.finish(out);

		// sea hunter
		trade().group()
				// buys food
				.buy("vegetables", 30, 3, 8, Ingredient.of(CPTags.Items.VEGETABLES))
				.buy("cereals", 30, 3, 5, Ingredient.of(CPTags.Items.CEREALS))
				.buy("cereals_baked", 30, 3, 7, Ingredient.of(CPTags.Items.BAKED))
				.buy("eggs", 30, 1, 10, Ingredient.of(CPTags.Items.EGGS))
				.buy(30, 2, 10, ItemSubscriber.fat.asItem())
				.buy("sugar", 30, 2, 10, Ingredient.of(CPTags.Items.SUGAR))
				.buy("walnut", 30, 2, 10, Ingredient.of(CPTags.Items.WALNUT))
				// buys clothes
				.buy(5, 0.1f, 15, FHItems.straw_lining.get())
				.buy(5, 0.1f, 30, FHItems.buff_coat.get())
				.buy(5, 0.1f, 60, FHItems.gambeson.get())
				// buys processed fuel
				.buy(64, 5, 6, IEItems.Ingredients.COAL_COKE.asItem())
				// buys job related tools
				.buy(64, 10, 1, Items.ARROW)
				.buy(5, 1, 10, Items.BOW)
				.buy(5, 1, 10, Items.FISHING_ROD)
				.buy(1, .1f, 30, FHItems.SNOWSHOES.get())
				.buy(1, .1f, 30, FHItems.ICE_SKATES.get())
				.buy("boats", 1, .1f, 50, Ingredient.of(ItemTags.BOATS))
				.buy(3, .5f, 40, IEItems.Tools.STEEL_SWORD.asItem())
				.buy(3, 1, 20, Items.IRON_SWORD)
				.buy(3, 1, 15, FHItems.BRONZE_SWORD.get())
				// sells hunting results
				.sell(8, 0.2f, 20, FHItems.polar_bear_hide.get())
				.sell(16, 2f, 8, FHItems.POLAR_BEAR_MEAT.get())
				.sell(16, 2f, 8, FHItems.RAW_WHALE_MEAT.get())
				.sell(16, 8f, 5, Items.COD)
				.sell(10, 2f, 50, Items.SEAGRASS)
				.sell(10, 2f, 50, Items.SEA_PICKLE)
				.sell(10, 2f, 50, Items.KELP)
				.sell(8, 1f, 30, ItemSubscriber.fat)
				.sell(16, 4F, 1, FHItems.night_soil.asItem())
				.sell(16, 8F, 1, FHItems.dung.asItem())
				// sell intelligence, but slow
				.sell(2, 0.05f, 200, FRContents.Items.intelligence.get())
				.basic()
				.finish()
				.profession(VillagerProfession.FISHERMAN)
				.levelExp(1000, 2000, 3000, 4000, 5000)
				.weight(2)
				.id("ocean_hunter")
				.finish(out);

		// mining
		trade().group()
				// buys food
				.buy("vegetables", 30, 3, 8, Ingredient.of(CPTags.Items.VEGETABLES))
				.buy("cereals", 30, 3, 5, Ingredient.of(CPTags.Items.CEREALS))
				.buy("cereals_baked", 30, 3, 7, Ingredient.of(CPTags.Items.BAKED))
				.buy("eggs", 30, 1, 10, Ingredient.of(CPTags.Items.EGGS))
				.buy(30, 2, 10, ItemSubscriber.fat.asItem())
				.buy("sugar", 30, 2, 10, Ingredient.of(CPTags.Items.SUGAR))
				.buy("walnut", 30, 2, 10, Ingredient.of(CPTags.Items.WALNUT))
				// buys clothes
				.buy(5, 0.1f, 15, FHItems.straw_lining.get())
				.buy(5, 0.1f, 30, FHItems.buff_coat.get())
				.buy(5, 0.1f, 60, FHItems.gambeson.get())
				// buys processed fuel
				.buy(64, 5, 6, IEItems.Ingredients.COAL_COKE.asItem())
				// buys job related tools
				.buy(1, .1f, 30, FHItems.SNOWSHOES.get())
				.buy(1, .1f, 30, FHItems.ICE_SKATES.get())
				.buy(3, .5f, 40, IEItems.Tools.STEEL_PICK.asItem())
				.buy(3, .5f, 40, IEItems.Tools.STEEL_SHOVEL.asItem())
				.buy(3, 1, 20, Items.IRON_SHOVEL)
				.buy(3, 1, 20, Items.IRON_PICKAXE)
				.buy(3, 1, 20, Items.IRON_SHOVEL)
				.buy(3, 1, 15, FHItems.BRONZE_PICKAXE.asItem())
				.buy(3, 1, 15, FHItems.BRONZE_SHOVEL.asItem())
				.buy(64, 10, 10, Items.TNT)
				// sells mining results
				.sell(16, 4F, 1, FHItems.night_soil.asItem())
				.sell(128, 32, 3, Items.COAL)
				.sell(128, 32, 1, Items.RAW_COPPER)
				.sell(64, 16, 2, Items.RAW_IRON)
				.sell(64, 16, 2, FHItems.RAW_TIN.asItem())
				.sell(64, 4, 5, FHItems.RAW_MAGNESITE.asItem())
				.sell(64, 4, 5, FHItems.RAW_HALITE.asItem())
				.sell(64, 4, 5, FHItems.RAW_SYLVITE.asItem())
				.sell(64, 8, 5, FHItems.RAW_PYRITE.asItem())
				.sell(32, 8, 5, CRegistryHelper.getItem(ImmersiveEngineering.rl("raw_nickel")))
				.sell(32, 8, 5, CRegistryHelper.getItem(ImmersiveEngineering.rl("raw_lead")))
				.sell(16, 2, 30, CRegistryHelper.getItem(ImmersiveEngineering.rl("raw_silver")))
				.sell(64, 16, 2, CRegistryHelper.getItem(Create.asResource("raw_zinc")))
				.sell(64, 16, 5, FHItems.KAOLIN.asItem())
				.sell(64, 16, 5, FHItems.BAUXITE.asItem())
				.sell(64, 16, 3, FHItems.PEAT.asItem())
				.sell(16, 2, 50, Items.RAW_GOLD)
				.sell(16, .2f, 100, Items.DIAMOND)
				.sell(32, 3, 10, Items.LAPIS_LAZULI)
				.sell(32, 3, 10, Items.REDSTONE)
				.sell(16, .1f, 500, Items.EMERALD)
				.sell(32, 1, 30, Items.AMETHYST_SHARD)
				.sell(32, 1, 200, Items.LAVA_BUCKET)
				.sell(32, 1, 100, Items.MAGMA_BLOCK)
				// sell intelligence, but slow
				.sell(2, 0.05f, 200, FRContents.Items.intelligence.get())
				.basic()
				.finish()
				.profession(VillagerProfession.TOOLSMITH)
				.levelExp(1000, 2000, 3000, 4000, 5000)
				.weight(5)
				.id("miner")
				.finish(out);

		// explorer
		trade().group()
				// buys food
				.buy("vegetables", 64, 3, 8, Ingredient.of(CPTags.Items.VEGETABLES))
				.buy("cereals", 64, 3, 5, Ingredient.of(CPTags.Items.CEREALS))
				.buy("cereals_baked", 64, 3, 7, Ingredient.of(CPTags.Items.BAKED))
				.buy("eggs", 64, 1, 10, Ingredient.of(CPTags.Items.EGGS))
				.buy(64, 2, 10, ItemSubscriber.fat.asItem())
				.buy("sugar", 64, 2, 10, Ingredient.of(CPTags.Items.SUGAR))
				.buy("walnut", 64, 2, 10, Ingredient.of(CPTags.Items.WALNUT))
				// buys clothes
				.buy(10, 0.2f, 15, FHItems.straw_lining.get())
				.buy(10, 0.2f, 30, FHItems.buff_coat.get())
				.buy(10, 0.2f, 60, FHItems.gambeson.get())
				// buys processed fuel
				.buy(128, 5, 6, IEItems.Ingredients.COAL_COKE.asItem())
				// buys job related tools
				.buy(5, .1f, 30, FHItems.SNOWSHOES.get())
				.buy(5, .1f, 30, FHItems.ICE_SKATES.get())
				.buy(32, 5, 1, Items.ARROW)
				.buy(1, .1f, 10, Items.BOW)
				.buy(5, .1f, 50, Items.COMPASS)
				.buy(5, .1f, 100, Items.MAP)
				.buy(5, .1f, 50, Items.BOOK)
				// sells
				.sell(16, 4F, 1, FHItems.night_soil.asItem())
				// rare saplings
				.sell(1, 0.05f, 300, CRegistryHelper.getItem(CPMain.rl("walnut_sapling")))
				.sell(1, 0.05f, 500, CRegistryHelper.getItem(CPMain.rl("fig_sapling")))
				.sell(1, 0.05f, 300, CRegistryHelper.getItem(CPMain.rl("wolfberry_sapling")))
				.sell(1, 0.05f, 100, Items.OAK_SAPLING)
				.sell(3, 0.05f, 100, Items.BIRCH_SAPLING)
				.sell(1, 0.05f, 500, Items.JUNGLE_SAPLING)
				.sell(1, 0.05f, 300, Items.ACACIA_SAPLING)
				.sell(1, 0.05f, 100, Items.DARK_OAK_SAPLING)
				.sell(1, 0.05f, 500, Items.MANGROVE_PROPAGULE)
				.sell(1, 0.05f, 800, Items.CHERRY_SAPLING)
				.sell(1, 0.05f, 500, Items.AZALEA)
				.sell(1, 0.05f, 500, Items.FLOWERING_AZALEA)
				.sell(1, 0.05f, 800, Items.BAMBOO)
				.sell(1, 0.05f, 800, Items.COCOA_BEANS)
				.sell(3, 0.05f, 200, Items.SUGAR_CANE)
				.sell(3, 0.05f, 800, Items.CACTUS)
				.sell(3, 0.05f, 200, Items.LILY_PAD)
				.sell(1, 0.05f, 100, Items.HONEYCOMB_BLOCK)
				.sell(1, 0.05f, 100, CRegistryHelper.getItem(CPMain.rl("silphium")))
				.sell(12, 1f, 5, Items.BROWN_MUSHROOM)
				.sell(12, 1f, 5, Items.RED_MUSHROOM)
				.sell(8, 1f, 20, Items.DANDELION)
				.sell(6, 1f, 30, Items.GLOW_BERRIES)
				.sell(10, 1f, 20, Items.SWEET_BERRIES)
				.sell(6, 1f, 50, FHBlocks.RUBBER_DANDELION.asItem())
				// sell intelligence, but slow
				.sell(4, 0.2f, 200, FRContents.Items.intelligence.get())
				.basic()
				.finish()
				.profession(VillagerProfession.CARTOGRAPHER)
				.levelExp(1000, 2000, 3000, 4000, 5000)
				.weight(1)
				.id("explorer")
				.finish(out);

		
	}
	private TradeBuilder trade() {
		return new TradeBuilder();
	}
	private void recipesGenerator(@Nonnull Consumer<FinishedRecipe> out) {
		GeneratorRecipeBuilder.builder(IETags.slag, 1).addInput(ItemTags.COALS).setTime(1000).build(out,
				toRL("generator/slag"));
		
	}
	private ResourceLocation toRL(String s) {
		if (!s.contains("/"))
			s = "crafting/" + s;
		if (PATH_COUNT.containsKey(s)) {
			int count = PATH_COUNT.get(s) + 1;
			PATH_COUNT.put(s, count);
			return new ResourceLocation(FHMain.MODID, s + count);
		}
		PATH_COUNT.put(s, 1);
		return new ResourceLocation(FHMain.MODID, s);
	}

	String[] ovride=new String[] {
			"dilute_soup",
			"nail_soup"
	};


}
