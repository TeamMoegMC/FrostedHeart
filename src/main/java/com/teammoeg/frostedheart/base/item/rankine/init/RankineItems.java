package com.teammoeg.frostedheart.base.item.rankine.init;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.rankine.KnifeItem;
import com.teammoeg.frostedheart.base.item.rankine.SpearItem;
import com.teammoeg.frostedheart.base.item.rankine.alloys.AlloySpearItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;

public class RankineItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FHMain.MODID);
    public static Item.Properties DEF_BUILDING = new Item.Properties().stacksTo(64);


    public static final RegistryObject<Item> FLINT_KNIFE = ITEMS.register("flint_knife", () -> new KnifeItem(RankineToolMaterials.FLINT, 1, -1.5F, new Item.Properties()));
    public static final RegistryObject<Item> FLINT_PICKAXE = ITEMS.register("flint_pickaxe", () -> new PickaxeItem(RankineToolMaterials.FLINT, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> FLINT_AXE = ITEMS.register("flint_axe", () -> new AxeItem(RankineToolMaterials.FLINT, 4.0F, -3.2F, new Item.Properties()));
    public static final RegistryObject<Item> FLINT_SHOVEL = ITEMS.register("flint_shovel", () -> new ShovelItem(RankineToolMaterials.FLINT, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> FLINT_SPEAR = ITEMS.register("flint_spear", () -> new SpearItem(RankineToolMaterials.FLINT, 2, -2.9F, new ResourceLocation("frostedheart:textures/item/rankine/entity/flint_spear.png"),new Item.Properties()));
    public static final RegistryObject<Item> FLINT_HOE = ITEMS.register("flint_hoe", () -> new HoeItem(RankineToolMaterials.FLINT, 0, -3.0F, new Item.Properties()));


    public static final RegistryObject<Item> BRONZE_SPEAR = ITEMS.register("bronze_spear", () -> new AlloySpearItem(RankineToolMaterials.ALLOY, 2, -2.9F, "75Cu-25Sn",new ResourceLocation("rankine:alloying/bronze_alloying"), new ResourceLocation("frostedheart:textures/item/rankine/entity/bronze_spear.png"), new Item.Properties()));
    public static final RegistryObject<Item> ELEMENT = ITEMS.register("element", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ENDER_AMALGAM_SPEAR = ITEMS.register("ender_amalgam_spear", () -> new AlloySpearItem(RankineToolMaterials.ALLOY,2, -2.9F, "80Ed-20Au",new ResourceLocation("rankine:alloying/ender_amalgam_alloying"), new ResourceLocation("rankine:textures/entity/ender_amalgam_spear.png"), new Item.Properties()));

    public static Item getItem(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("rankine",name));
        if (item != null) {
            return item;
        } else {
            return Items.AIR;
        }
    }




}

