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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.content.climate.data.*;
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

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

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
		// state transition
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/state_transition.xlsx"), m->{

			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("block")));
			ResourceLocation solid=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("solid")));
			ResourceLocation liquid=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("liquid")));
			ResourceLocation gas=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("gas")));
			if(!block.getPath().isEmpty())
				out.accept(new StateTransitionData(
						CRegistryHelper.getBlock(block),
						ExcelHelper.getCellValueAsString(m.get("state")),
						CRegistryHelper.getBlock(solid),
						CRegistryHelper.getBlock(liquid),
						CRegistryHelper.getBlock(gas),
						(float)ExcelHelper.getCellValueAsNumber(m.get("freeze_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("melt_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("condense_temp")),
						(float)ExcelHelper.getCellValueAsNumber(m.get("evaporate_temp")),
						(int)ExcelHelper.getCellValueAsNumber(m.get("heat_capacity")),
						ExcelHelper.getCellValueAsBoolean(m.get("will_transit"))
				).toFinished(FHMain.rl("state_transition/"+block.getPath())));
		});
		//world
		out.accept(new WorldTempData(new ResourceLocation("the_nether"),300).toFinished(FHMain.rl("level_temperature/nether")));
		out.accept(new WorldTempData(new ResourceLocation("the_end"),-300).toFinished(FHMain.rl("level_temperature/the_end")));
		//plant
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/plant_temperature.xlsx"), m->{
			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("block")));
			ResourceLocation dead=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("dead")));
			out.accept(new PlantTempData(CRegistryHelper.getBlock(block),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_fertilize")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_grow")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("min_survive")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_fertilize")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_grow")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("max_survive")),
					!ExcelHelper.getCellValueAsBoolean(m.get("survive_snow")),
					!ExcelHelper.getCellValueAsBoolean(m.get("survive_blizzard")),
					CRegistryHelper.getBlock(dead),
					ExcelHelper.getCellValueAsBoolean(m.get("will_die"))
					).toFinished(FHMain.rl("plant_temperature/"+block.getPath())));
		});
		//drink
		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/drink_temperature.xlsx"), m->{
			ResourceLocation block=new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("fluid")));
			out.accept(new DrinkTempData(CRegistryHelper.getFluid(block),
					(float)ExcelHelper.getCellValueAsNumber(m.get("heat"))
					).toFinished(FHMain.rl("drink_temperature/"+block.getPath())));
		});

		ExcelHelper.forEachRowExcludingHeaders(openWorkBook("/data/frostedheart/data/nutrition.xlsx"), m->{
			ResourceLocation itemid = new ResourceLocation(ExcelHelper.getCellValueAsString(m.get("ID")));
			NutritionRecipeBuilder dvb =new NutritionRecipeBuilder().ItemID(itemid);
			dvb.nutrition((float)ExcelHelper.getCellValueAsNumber(m.get("Gr2")),(float)ExcelHelper.getCellValueAsNumber(m.get("Va2")),
					(float)ExcelHelper.getCellValueAsNumber(m.get("Oi2")),(float)ExcelHelper.getCellValueAsNumber(m.get("Pt2")));
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
        for(Entry<String, Float[]> mat:materials.entrySet()) {
        	for(String type:new String[]{"hat","jacket","pants","boots"}) {
        		out.accept(armorArmorData(CRegistryHelper.getItem(FHMain.rl(mat.getKey()+"_"+type)),mat.getValue()[2],mat.getValue()[0],mat.getValue()[1]));
        	} 
        }
        out.accept(armorArmorData(FHItems.space_hat,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_jacket,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_pants,500f,.2f,1.0f));
        out.accept(armorArmorData(FHItems.space_boots,500f,.2f,1.0f));
        
		//recipeTrade(out);
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
	private void recipeTrade(@Nonnull Consumer<FinishedRecipe> out) {
		trade().group().buy(10,10,10,FHItems.rye_bread.get())
		.buy(1, 0.1f,20,FHItems.straw_lining.get())
		.buy(10,10,10,Items.RAW_COPPER).useAction().addFlag("copper", 1).finish()
		.sell(10, 1, 100,FHItems.energy_core.get())
		.sell(10, 1, 5,Items.COPPER_INGOT).restockAction().addFlag("copper", -1).finish().restocksBy().hasFlag("copper").finish()
		.basic()
		.finish()
		.weight(1).id("test").finish(out);;
		
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
