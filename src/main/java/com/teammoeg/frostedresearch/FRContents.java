package com.teammoeg.frostedresearch;

import static com.teammoeg.frostedheart.FHMain.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.foundation.data.AssetLookup;
import com.teammoeg.chorda.creativeTab.TabType;
import com.teammoeg.chorda.item.CBlockItem;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.infrastructure.gen.FHBlockStateGen;
import com.teammoeg.frostedresearch.blocks.DrawingDeskBlock;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.blocks.FHBasePen;
import com.teammoeg.frostedresearch.blocks.FHReusablePen;
import com.teammoeg.frostedresearch.blocks.MechCalcBlock;
import com.teammoeg.frostedresearch.blocks.MechCalcTileEntity;
import com.teammoeg.frostedresearch.blocks.RubbingTool;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.item.FRBaseItem;
import com.teammoeg.frostedresearch.recipe.InspireRecipe;
import com.teammoeg.frostedresearch.recipe.ResearchPaperRecipe;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FRContents {
	public static class Blocks {
		public static final DeferredRegister<Block> REGISTER = DeferredRegister.create(
			ForgeRegistries.BLOCKS, FRMain.MODID);
		// DRAWING_DESK
		public static final RegistryObject<DrawingDeskBlock> DRAWING_DESK = register("drawing_desk", () -> new DrawingDeskBlock(
			BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD)
				.sound(SoundType.WOOD)
				.strength(2, 6)
				.noOcclusion()));
		// MECHANICAL_CALCULATOR
		public static final RegistryObject<MechCalcBlock> MECHANICAL_CALCULATOR = register("mechanical_calculator", () -> new MechCalcBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
				.sound(SoundType.METAL)
				.requiresCorrectToolForDrops()
				.strength(2, 10)
				.noOcclusion()));

		protected static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, String itemName, Function<T, Item> item) {
			RegistryObject<T> blk = REGISTER.register(name, block);
			Items.REGISTER.register(itemName, () -> item.apply(blk.get()));
			return blk;
		}

		protected static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
			return register(name, block, name, blk -> new CBlockItem(blk, new Item.Properties(), Tabs.BLOCK_TAB_TYPE));
		}

		protected static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, Function<T, Item> item) {
			return register(name, block, name, item);
		}
	}

	public static class Items {
		public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(
			ForgeRegistries.ITEMS, FRMain.MODID);
		public static RegistryObject<RubbingTool> rubbing_tool = REGISTER
			.register("rubbing_tool", () -> new RubbingTool(new Item.Properties().durability(5).setNoRepair()));
		public static RegistryObject<FHBasePen> charcoal = REGISTER.register("charcoal", () -> new FHBasePen(new Item.Properties().durability(50).setNoRepair()));
		public static RegistryObject<FHReusablePen> quill_and_ink = REGISTER.register("quill_and_ink", () -> new FHReusablePen(new Item.Properties().durability(101).setNoRepair(), 1));
	    public static RegistryObject<FRBaseItem> rubbing_pad = REGISTER
            .register("rubbing_pad",()->new FRBaseItem(new Item.Properties().stacksTo(1)));
	}

	public static class BlockEntityTypes {
		public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(
			ForgeRegistries.BLOCK_ENTITY_TYPES, FRMain.MODID);
		public static final RegistryObject<BlockEntityType<DrawingDeskTileEntity>> DRAWING_DESK = REGISTER.register(
			"drawing_desk", makeType(DrawingDeskTileEntity::new, Blocks.DRAWING_DESK::get));
		public static final RegistryObject<BlockEntityType<MechCalcTileEntity>> MECH_CALC = REGISTER.register(
			"mechanical_calculator", makeType(MechCalcTileEntity::new, Blocks.MECHANICAL_CALCULATOR::get));

		private static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeType(BlockEntitySupplier<T> create, Supplier<Block> valid) {
			return makeTypeMultipleBlocks(create, () -> ImmutableSet.of(valid.get()));
		}

		@SafeVarargs
		private static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeType(BlockEntitySupplier<T> create, Supplier<Block>... valid) {
			return makeTypeMultipleBlocks(create, () -> Arrays.stream(valid).map(Supplier::get).collect(Collectors.toList()));
		}

		private static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeTypeMultipleBlocks(BlockEntitySupplier<T> create, Supplier<Collection<Block>> valid) {
			return () -> new BlockEntityType<>(create, ImmutableSet.copyOf(valid.get()), null);
		}

	}

	public static class Tabs {

		public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FRMain.MODID);
		// Building blocks Tab
		public static final RegistryObject<CreativeModeTab> BUILDING_BLOCKS = TABS.register("main",
			() -> CreativeModeTab
				.builder()
				.withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
				.icon(() -> new ItemStack(Blocks.DRAWING_DESK.get()))
				.title(Lang.translateKey("itemGroup.frostedresearch.building_blocks"))
				.build());
		public static final TabType BLOCK_TAB_TYPE = new TabType(BUILDING_BLOCKS);
	}

	public static class MenuTypes {

		@FunctionalInterface
		public interface BEMenuFactory<T extends AbstractContainerMenu, BE extends BlockEntity> {
			T get(int id, Inventory inventoryPlayer, BE tile);
		}

		public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
			FRMain.MODID);
		public static final RegistryObject<MenuType<DrawDeskContainer>> DRAW_DESK = register(DrawingDeskTileEntity.class, ("draw_desk"), DrawDeskContainer::new);

		@SuppressWarnings("unchecked")
		public static <T extends AbstractContainerMenu, BE extends BlockEntity> RegistryObject<MenuType<T>> register(Class<BE> BEClass, String name, BEMenuFactory<T, BE> factory) {
			return CONTAINERS.register(name, () -> IForgeMenuType.create((id, inv, pb) -> {
				BlockEntity be = inv.player.level().getBlockEntity(pb.readBlockPos());
				if (BEClass.isInstance(be))
					return factory.get(id, inv, (BE) be);
				return null;
			}));
		}

	}

	public static class Recipes {
		public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
			ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID);
		public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
			ForgeRegistries.RECIPE_TYPES, FHMain.MODID);
		static {
			ResearchPaperRecipe.TYPE = createRecipeType("paper");
			ResearchPaperRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("paper", ResearchPaperRecipe.Serializer::new);
			InspireRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("inspire", InspireRecipe.Serializer::new);
			InspireRecipe.TYPE = createRecipeType("inspire");
		}

		public static <T extends Recipe<?>> RegistryObject<RecipeType<T>> createRecipeType(String name) {
			return RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(FHMain.MODID, name)));
		}
	}

	public FRContents() {
		// TODO Auto-generated constructor stub
	}

}
