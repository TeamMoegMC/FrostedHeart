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

package com.teammoeg.frostedheart.research.machines;

import java.util.Optional;
import java.util.Random;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.recipes.ResearchPaperRecipe;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedheart.research.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;

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
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    ResearchGame game = new ResearchGame();
    public static final int INK_SLOT = 2;
    public static final int PAPER_SLOT = 1;
    public static final int EXAMINE_SLOT = 0;
    public static int ENERGY_PER_COMBINE = 100;
    public static int ENERGY_PER_PAPER = 3000;

    public DrawingDeskTileEntity() {
        super(FHTileTypes.DRAWING_DESK.get());
    }

    @Override
    public boolean canUseGui(PlayerEntity arg0) {
        return true;
    }

    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public void doGraphicalUpdates() {
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack item) {
        if (slot == EXAMINE_SLOT)
            return true;
        else if (slot == INK_SLOT)
            return item.getItem() instanceof IPen && ((IPen) item.getItem()).canUse(null, item, 1);
        else if (slot == PAPER_SLOT)
            return ResearchPaperRecipe.recipes.stream().anyMatch(r -> r.paper.test(item));
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

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        if (!descPacket) {

            ItemStackHelper.saveAllItems(nbt, inventory);
        }
    }

    public ResearchGame getGame() {
        return game;
    }

    public void initGame(ServerPlayerEntity player) {
        if (inventory.get(PAPER_SLOT).isEmpty()) return;
        int lvl = ResearchListeners.fetchGameLevel(player);
        if (lvl < 0) return;
        Optional<ResearchPaperRecipe> pr = ResearchPaperRecipe.recipes.stream().filter(r -> r.maxlevel >= lvl && r.paper.test(inventory.get(PAPER_SLOT))).findAny();
        if (!pr.isPresent()) return;
        if (!EnergyCore.hasEnoughEnergy(player, ENERGY_PER_PAPER)) return;
        if (!damageInk(player, 5, lvl)) return;
        EnergyCore.consumeEnergy(player, ENERGY_PER_PAPER);
        inventory.get(PAPER_SLOT).shrink(1);
        game.init(GenerateInfo.all[lvl], new Random());
        game.setLvl(lvl);
    }

    private boolean damageInk(ServerPlayerEntity spe, int val, int lvl) {
        ItemStack is = inventory.get(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        if (pen.getLevel(is, spe) < lvl) return false;
        return pen.damage(spe, is, val);
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
        return ResearchPaperRecipe.recipes.stream().anyMatch(r -> r.maxlevel >= lvl && r.paper.test(is));
    }

    public boolean tryCombine(ServerPlayerEntity player, CardPos cp1, CardPos cp2) {
        ItemStack is = inventory.get(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        if (pen.getLevel(is, player) < game.getLvl())
            return false;
        return pen.tryDamage(player, is, 1, () -> {
            if (EnergyCore.hasEnoughEnergy(player, ENERGY_PER_COMBINE)) {
                if (game.tryCombine(cp1, cp2)) {
                    EnergyCore.consumeEnergy(player, ENERGY_PER_COMBINE);
                    return true;
                }
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

    public void submitItem(ServerPlayerEntity sender) {
        inventory.set(EXAMINE_SLOT, ResearchListeners.submitItem(sender, inventory.get(EXAMINE_SLOT)));
    }

}
