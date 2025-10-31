package com.teammoeg.frostedheart.content.town.resource.gui;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AbstractTownItemResourceMenu<T extends BlockEntity> extends CBlockEntityMenu<T> {


    public AbstractTownItemResourceMenu(MenuType<?> menuType, T blockEntity, int id, Inventory inventoryPlayer) {
        super(menuType,blockEntity,id, inventoryPlayer.player,32);

        super.addPlayerInventory(inventoryPlayer, 8, 139, 197);
    }
}
