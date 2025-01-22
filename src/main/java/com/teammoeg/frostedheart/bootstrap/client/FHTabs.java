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

package com.teammoeg.frostedheart.bootstrap.client;

import com.simibubi.create.foundation.item.TagDependentIngredientItem;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.chorda.creativeTab.TabType;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.infrastructure.gen.FHRegistrate;
import com.teammoeg.frostedheart.util.client.Lang;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class FHTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FHMain.MODID);

    // Building blocks Tab
    public static final RegistryObject<CreativeModeTab> BUILDING_BLOCKS = TABS.register("frostedheart_building_blocks",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(()->new ItemStack(FHBlocks.RUINED_MACHINE_SWITCH.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.building_blocks"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.BUILDING_BLOCKS))
                    .build());
    // Natural blocks Tab
    public static final RegistryObject<CreativeModeTab> NATURAL_BLOCKS = TABS.register("frostedheart_natural_blocks",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(BUILDING_BLOCKS.getKey())
                    .icon(()->new ItemStack(FHBlocks.SILVER_SLUDGE_BLOCK.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.natural_blocks"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.NATURAL_BLOCKS))
                    .build());
    // Functional blocks Tab
    public static final RegistryObject<CreativeModeTab> FUNCTIONAL_BLOCKS = TABS.register("frostedheart_functional_blocks",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(NATURAL_BLOCKS.getKey())
                    .icon(()->new ItemStack(FHBlocks.HEAT_PIPE.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.functional_blocks"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.FUNCTIONAL_BLOCKS))
                    .build());
    // Materials Tab
    public static final RegistryObject<CreativeModeTab> INGREDIENTS = TABS.register("frostedheart_materials",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(FUNCTIONAL_BLOCKS.getKey())
                    .icon(()->new ItemStack(FHItems.ICE_CHIP.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.ingredients"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.INGREDIENTS))
                    .build());
    // Foods
    public static final RegistryObject<CreativeModeTab> FOODS = TABS.register("frostedheart_foods",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(INGREDIENTS.getKey())
                    .icon(()->new ItemStack(FHItems.military_rations.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.foods"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.FOODS))
                    .build());
    // Tools Tab
    public static final RegistryObject<CreativeModeTab> TOOLS = TABS.register("frostedheart_tools",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(FOODS.getKey())
                    .icon(()->new ItemStack(FHItems.hand_stove.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.tools"))
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.TOOLS))
                    .build());
    // Misc Tab
    public static final RegistryObject<CreativeModeTab> MISC = TABS.register("frostedheart_misc",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(TOOLS.getKey())
                    .icon(()->new ItemStack(FHItems.energy_core.get()))
                    .title(Lang.translateKey("itemGroup.frostedheart.misc"))
                    .displayItems(FHTabs::fillFHTab)
                    .displayItems(new RegistrateDisplayItemsGenerator(true, FHTabs.MISC))
                    .build());
    public static final TabType itemGroup = new TabType(MISC);


    public static void fillFHTab(CreativeModeTab.ItemDisplayParameters parms, CreativeModeTab.Output out) {
        for (final RegistryObject<Item> itemRef : FHItems.ITEMS.getEntries()) {
            final Item item = itemRef.get();
            if (item instanceof ICreativeModeTabItem) {
                continue;
            }
            else
                out.accept(itemRef.get());
        }
    }

    /**
     * Credit: Create Mod
     */
    private static class RegistrateDisplayItemsGenerator implements CreativeModeTab.DisplayItemsGenerator {
        private static final Predicate<Item> IS_ITEM_3D_PREDICATE;

        static {
            MutableObject<Predicate<Item>> isItem3d = new MutableObject<>(item -> false);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                isItem3d.setValue(item -> {
                    ItemRenderer itemRenderer = Minecraft.getInstance()
                            .getItemRenderer();
                    BakedModel model = itemRenderer.getModel(new ItemStack(item), null, null, 0);
                    return model.isGui3d();
                });
            });
            IS_ITEM_3D_PREDICATE = isItem3d.getValue();
        }

        @OnlyIn(Dist.CLIENT)
        private static Predicate<Item> makeClient3dItemPredicate() {
            return item -> {
                ItemRenderer itemRenderer = Minecraft.getInstance()
                        .getItemRenderer();
                BakedModel model = itemRenderer.getModel(new ItemStack(item), null, null, 0);
                return model.isGui3d();
            };
        }

        private final boolean addItems;
        private final RegistryObject<CreativeModeTab> tabFilter;

        public RegistrateDisplayItemsGenerator(boolean addItems, RegistryObject<CreativeModeTab> tabFilter) {
            this.addItems = addItems;
            this.tabFilter = tabFilter;
        }

        private static Predicate<Item> makeExclusionPredicate() {
            Set<Item> exclusions = new ReferenceOpenHashSet<>();

            List<ItemProviderEntry<?>> simpleExclusions = List.of(

            );

            List<ItemEntry<TagDependentIngredientItem>> tagDependentExclusions = List.of(

            );

            for (ItemProviderEntry<?> entry : simpleExclusions) {
                exclusions.add(entry.asItem());
            }

            for (ItemEntry<TagDependentIngredientItem> entry : tagDependentExclusions) {
                TagDependentIngredientItem item = entry.get();
                if (item.shouldHide()) {
                    exclusions.add(entry.asItem());
                }
            }

            return exclusions::contains;
        }

        private static List<RegistrateDisplayItemsGenerator.ItemOrdering> makeOrderings() {
            List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings = new ReferenceArrayList<>();

            Map<ItemProviderEntry<?>, ItemProviderEntry<?>> simpleBeforeOrderings = Map.of(
//                    AllItems.EMPTY_BLAZE_BURNER, AllBlocks.BLAZE_BURNER,
//                    AllItems.SCHEDULE, AllBlocks.TRACK_STATION
            );

            Map<ItemProviderEntry<?>, ItemProviderEntry<?>> simpleAfterOrderings = Map.of(
//                    AllItems.VERTICAL_GEARBOX, AllBlocks.GEARBOX
            );

            simpleBeforeOrderings.forEach((entry, otherEntry) -> {
                orderings.add(RegistrateDisplayItemsGenerator.ItemOrdering.before(entry.asItem(), otherEntry.asItem()));
            });

            simpleAfterOrderings.forEach((entry, otherEntry) -> {
                orderings.add(RegistrateDisplayItemsGenerator.ItemOrdering.after(entry.asItem(), otherEntry.asItem()));
            });

            return orderings;
        }

        private static Function<Item, ItemStack> makeStackFunc() {
            Map<Item, Function<Item, ItemStack>> factories = new Reference2ReferenceOpenHashMap<>();

            Map<ItemProviderEntry<?>, Function<Item, ItemStack>> simpleFactories = Map.of(
//                    AllItems.COPPER_BACKTANK, item -> {
//                        ItemStack stack = new ItemStack(item);
//                        stack.getOrCreateTag().putInt("Air", BacktankUtil.maxAirWithoutEnchants());
//                        return stack;
//                    },
//                    AllItems.NETHERITE_BACKTANK, item -> {
//                        ItemStack stack = new ItemStack(item);
//                        stack.getOrCreateTag().putInt("Air", BacktankUtil.maxAirWithoutEnchants());
//                        return stack;
//                    }
            );

            simpleFactories.forEach((entry, factory) -> {
                factories.put(entry.asItem(), factory);
            });

            return item -> {
                Function<Item, ItemStack> factory = factories.get(item);
                if (factory != null) {
                    return factory.apply(item);
                }
                return new ItemStack(item);
            };
        }

        private static Function<Item, CreativeModeTab.TabVisibility> makeVisibilityFunc() {
            Map<Item, CreativeModeTab.TabVisibility> visibilities = new Reference2ObjectOpenHashMap<>();

            Map<ItemProviderEntry<?>, CreativeModeTab.TabVisibility> simpleVisibilities = Map.of(
//                    AllItems.BLAZE_CAKE_BASE, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
            );

            simpleVisibilities.forEach((entry, factory) -> {
                visibilities.put(entry.asItem(), factory);
            });

//            for (BlockEntry<ValveHandleBlock> entry : AllBlocks.DYED_VALVE_HANDLES) {
//                visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//            }

//            for (BlockEntry<SeatBlock> entry : AllBlocks.SEATS) {
//                SeatBlock block = entry.get();
//                if (block.getColor() != DyeColor.RED) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }
//
//            for (BlockEntry<ToolboxBlock> entry : AllBlocks.TOOLBOXES) {
//                ToolboxBlock block = entry.get();
//                if (block.getColor() != DyeColor.BROWN) {
//                    visibilities.put(entry.asItem(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
//                }
//            }

            return item -> {
                CreativeModeTab.TabVisibility visibility = visibilities.get(item);
                if (visibility != null) {
                    return visibility;
                }
                return CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            };
        }

        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
            Predicate<Item> exclusionPredicate = makeExclusionPredicate();
            List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings = makeOrderings();
            Function<Item, ItemStack> stackFunc = makeStackFunc();
            Function<Item, CreativeModeTab.TabVisibility> visibilityFunc = makeVisibilityFunc();

            List<Item> items = new LinkedList<>();
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE.negate())));
            }
            items.addAll(collectBlocks(exclusionPredicate));
            if (addItems) {
                items.addAll(collectItems(exclusionPredicate.or(IS_ITEM_3D_PREDICATE)));
            }

            applyOrderings(items, orderings);
            outputAll(output, items, stackFunc, visibilityFunc);
        }

        private List<Item> collectBlocks(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Block> entry : FHMain.REGISTRATE.getAll(Registries.BLOCK)) {
                if (!FHRegistrate.isInCreativeTab(entry, tabFilter))
                    continue;
                Item item = entry.get()
                        .asItem();
                if (item == Items.AIR)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            items = new ReferenceArrayList<>(new ReferenceLinkedOpenHashSet<>(items));
            return items;
        }

        private List<Item> collectItems(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Item> entry : FHMain.REGISTRATE.getAll(Registries.ITEM)) {
                if (!FHRegistrate.isInCreativeTab(entry, tabFilter))
                    continue;
                Item item = entry.get();
                if (item instanceof BlockItem)
                    continue;
                if (!exclusionPredicate.test(item))
                    items.add(item);
            }
            return items;
        }

        private static void applyOrderings(List<Item> items, List<RegistrateDisplayItemsGenerator.ItemOrdering> orderings) {
            for (RegistrateDisplayItemsGenerator.ItemOrdering ordering : orderings) {
                int anchorIndex = items.indexOf(ordering.anchor());
                if (anchorIndex != -1) {
                    Item item = ordering.item();
                    int itemIndex = items.indexOf(item);
                    if (itemIndex != -1) {
                        items.remove(itemIndex);
                        if (itemIndex < anchorIndex) {
                            anchorIndex--;
                        }
                    }
                    if (ordering.type() == RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER) {
                        items.add(anchorIndex + 1, item);
                    } else {
                        items.add(anchorIndex, item);
                    }
                }
            }
        }

        private static void outputAll(CreativeModeTab.Output output, List<Item> items, Function<Item, ItemStack> stackFunc, Function<Item, CreativeModeTab.TabVisibility> visibilityFunc) {
            for (Item item : items) {
                output.accept(stackFunc.apply(item), visibilityFunc.apply(item));
            }
        }

        private record ItemOrdering(Item item, Item anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type type) {
            public static RegistrateDisplayItemsGenerator.ItemOrdering before(Item item, Item anchor) {
                return new RegistrateDisplayItemsGenerator.ItemOrdering(item, anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type.BEFORE);
            }

            public static RegistrateDisplayItemsGenerator.ItemOrdering after(Item item, Item anchor) {
                return new RegistrateDisplayItemsGenerator.ItemOrdering(item, anchor, RegistrateDisplayItemsGenerator.ItemOrdering.Type.AFTER);
            }

            public enum Type {
                BEFORE,
                AFTER;
            }
        }
    }
}
