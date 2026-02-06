/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.blocks;

import java.util.Optional;
import java.util.Random;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedresearch.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedresearch.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedresearch.recipe.ResearchPaperRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

public class DrawingDeskTileEntity extends CBlockEntity implements MenuProvider {
    public static final int INK_SLOT = 2;
    public static final int PAPER_SLOT = 1;
    public static final int EXAMINE_SLOT = 0;
    public static int ENERGY_PER_COMBINE = 100;
    public static int ENERGY_PER_PAPER = 3000;
    protected ItemStackHandler inventory = new ItemStackHandler(3) {

        @Override
        public boolean isItemValid(int slot, ItemStack item) {
            if (slot == EXAMINE_SLOT)
                return true;
            else if (slot == INK_SLOT)
                return item.getItem() instanceof IPen && ((IPen) item.getItem()).canUse(null, item, 1);
            else if (slot == PAPER_SLOT)
                return CUtils.filterRecipes(getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.paper.test(item));
            else
                return false;
        }
    };
    ResearchGame game = new ResearchGame();

    public DrawingDeskTileEntity(BlockPos pos, BlockState state) {
        super(FRContents.BlockEntityTypes.DRAWING_DESK.get(), pos, state);
    }


    private boolean damageInk(ServerPlayer spe, int val, int lvl) {
        ItemStack is = inventory.getStackInSlot(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        if (pen.getLevel(is, spe) < lvl) return false;
        return pen.damage(spe, is, val);
    }


    public ResearchGame getGame() {
        return game;
    }


    public ItemStackHandler getInventory() {
        return inventory;
    }


    public void initGame(ServerPlayer player) {
        if (inventory.getStackInSlot(PAPER_SLOT).isEmpty()) return;
        int lvl = ResearchHooks.fetchGameLevel(player);
        if (lvl < 0) return;
        Optional<ResearchPaperRecipe> pr = CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().filter(r -> r.maxlevel >= lvl && r.paper.test(inventory.getStackInSlot(PAPER_SLOT))).findAny();
        if (!pr.isPresent()) return;
        //if (EnergyCore.getEnergy(player) <= 0) return;
        if (!damageInk(player, 5, lvl)) return;
        //EnergyCore.costEnergy(player, 1);
        inventory.getStackInSlot(PAPER_SLOT).shrink(1);
        game.init(GenerateInfo.all[lvl], new Random());
        game.setLvl(lvl);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isInkSatisfied(int val) {
        ItemStack is = inventory.getStackInSlot(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        return pen.getLevel(is, ClientUtils.getPlayer()) >= ResearchHooks.fetchGameLevel() && pen.canUse(ClientUtils.getPlayer(), is, val);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isPaperSatisfied() {
        ItemStack is = inventory.getStackInSlot(PAPER_SLOT);
        if (is.isEmpty()) return false;
        int lvl = ResearchHooks.fetchGameLevel();
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.maxlevel >= lvl && r.paper.test(is));
    }


    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (nbt.contains("gamedata"))
            game.load(nbt.getCompound("gamedata"));
        
        if (!descPacket) {
        	if(nbt.contains("Items")) {
        		NonNullList<ItemStack> invlist=NonNullList.withSize(3,ItemStack.EMPTY);
        		
        		ContainerHelper.loadAllItems(nbt, invlist);
        		for(int i=0;i<invlist.size();i++) {
        			inventory.setStackInSlot(i, invlist.get(i));
        		}
        	}else {
        		inventory.deserializeNBT(nbt.getCompound("inv"));
        	}
        }


    }

    public void submitItem(ServerPlayer sender) {
        inventory.setStackInSlot(EXAMINE_SLOT, ResearchHooks.submitItem(sender, inventory.getStackInSlot(EXAMINE_SLOT)));
    }

    public boolean tryCombine(ServerPlayer player, CardPos cp1, CardPos cp2) {
        ItemStack is = inventory.getStackInSlot(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        if (pen.getLevel(is, player) < game.getLvl())
            return false;
        return pen.tryDamage(player, is, 1, () -> {
            if (game.tryCombine(cp1, cp2)) {
                return true;
            }
            return false;
        });
    }

    public void updateGame(ServerPlayer player) {
        if (game.isFinished()) {

            ResearchHooks.commitGameLevel(player, game.getLvl());
            game.reset();
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        if (!descPacket) {

            nbt.put("inv", inventory.serializeNBT());
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new DrawDeskContainer(pContainerId, pPlayerInventory, this);
    }


    @Override
    public Component getDisplayName() {
        return Lang.translate("gui","draw_desk");
    }

}
