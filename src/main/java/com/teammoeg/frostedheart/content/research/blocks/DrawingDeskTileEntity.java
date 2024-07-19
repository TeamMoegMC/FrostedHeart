/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.blocks;

import java.util.Optional;
import java.util.Random;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.recipes.ResearchPaperRecipe;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DrawingDeskTileEntity extends IEBaseTileEntity implements IInteractionObjectIE, IIEInventory {
    public static final int INK_SLOT = 2;
    public static final int PAPER_SLOT = 1;
    public static final int EXAMINE_SLOT = 0;
    public static int ENERGY_PER_COMBINE = 100;
    public static int ENERGY_PER_PAPER = 3000;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    ResearchGame game = new ResearchGame();

    public DrawingDeskTileEntity() {
        super(FHTileTypes.DRAWING_DESK.get());
    }

    @Override
    public boolean canUseGui(PlayerEntity arg0) {
        return true;
    }

    private boolean damageInk(ServerPlayerEntity spe, int val, int lvl) {
        ItemStack is = inventory.get(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        if (pen.getLevel(is, spe) < lvl) return false;
        return pen.damage(spe, is, val);
    }

    @Override
    public void doGraphicalUpdates() {
    }

    public ResearchGame getGame() {
        return game;
    }

    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    public void initGame(ServerPlayerEntity player) {
        if (inventory.get(PAPER_SLOT).isEmpty()) return;
        int lvl = ResearchListeners.fetchGameLevel(player);
        if (lvl < 0) return;
        Optional<ResearchPaperRecipe> pr = FHUtils.filterRecipes(this.getWorld().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().filter(r -> r.maxlevel >= lvl && r.paper.test(inventory.get(PAPER_SLOT))).findAny();
        if (!pr.isPresent()) return;
        if (EnergyCore.getEnergy(player)<=0) return;
        if (!damageInk(player, 5, lvl)) return;
        EnergyCore.costEnergy(player, 1);
        inventory.get(PAPER_SLOT).shrink(1);
        game.init(GenerateInfo.all[lvl], new Random());
        game.setLvl(lvl);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isInkSatisfied(int val) {
        ItemStack is = inventory.get(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        return pen.getLevel(is, ClientUtils.getPlayer()) >= ResearchListeners.fetchGameLevel() && pen.canUse(ClientUtils.getPlayer(), is, val);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isPaperSatisfied() {
        ItemStack is = inventory.get(PAPER_SLOT);
        if (is.isEmpty()) return false;
        int lvl = ResearchListeners.fetchGameLevel();
        return FHUtils.filterRecipes(this.getWorld().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.maxlevel >= lvl && r.paper.test(is));
    }

    @Override
    public boolean isStackValid(int slot, ItemStack item) {
        if (slot == EXAMINE_SLOT)
            return true;
        else if (slot == INK_SLOT)
            return item.getItem() instanceof IPen && ((IPen) item.getItem()).canUse(null, item, 1);
        else if (slot == PAPER_SLOT)
            return FHUtils.filterRecipes(this.getWorld().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.paper.test(item));
        else
            return false;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (nbt.contains("gamedata"))
            game.load(nbt.getCompound("gamedata"));
        if (!descPacket) {

            ItemStackHelper.loadAllItems(nbt, inventory);
        }


    }

    public void submitItem(ServerPlayerEntity sender) {
        inventory.set(EXAMINE_SLOT, ResearchListeners.submitItem(sender, inventory.get(EXAMINE_SLOT)));
    }

    public boolean tryCombine(ServerPlayerEntity player, CardPos cp1, CardPos cp2) {
        ItemStack is = inventory.get(INK_SLOT);
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

    public void updateGame(ServerPlayerEntity player) {
        if (game.isFinished()) {

            ResearchListeners.commitGameLevel(player, game.getLvl());
            game.reset();
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        if (!descPacket) {

            ItemStackHelper.saveAllItems(nbt, inventory);
        }
    }

}
