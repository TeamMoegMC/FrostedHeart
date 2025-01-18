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

import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.research.recipe.ResearchPaperRecipe;
import com.teammoeg.frostedheart.util.client.Lang;
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

import java.util.Optional;
import java.util.Random;

public class DrawingDeskTileEntity extends IEBaseBlockEntity implements MenuProvider, IIEInventory {
    public static final int INK_SLOT = 2;
    public static final int PAPER_SLOT = 1;
    public static final int EXAMINE_SLOT = 0;
    public static int ENERGY_PER_COMBINE = 100;
    public static int ENERGY_PER_PAPER = 3000;
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    ResearchGame game = new ResearchGame();

    public DrawingDeskTileEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.DRAWING_DESK.get(), pos, state);
    }


    private boolean damageInk(ServerPlayer spe, int val, int lvl) {
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
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    public void initGame(ServerPlayer player) {
        if (inventory.get(PAPER_SLOT).isEmpty()) return;
        int lvl = ResearchListeners.fetchGameLevel(player);
        if (lvl < 0) return;
        Optional<ResearchPaperRecipe> pr = CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().filter(r -> r.maxlevel >= lvl && r.paper.test(inventory.get(PAPER_SLOT))).findAny();
        if (!pr.isPresent()) return;
        if (EnergyCore.getEnergy(player) <= 0) return;
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
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.maxlevel >= lvl && r.paper.test(is));
    }

    @Override
    public boolean isStackValid(int slot, ItemStack item) {
        if (slot == EXAMINE_SLOT)
            return true;
        else if (slot == INK_SLOT)
            return item.getItem() instanceof IPen && ((IPen) item.getItem()).canUse(null, item, 1);
        else if (slot == PAPER_SLOT)
            return CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.paper.test(item));
        else
            return false;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (nbt.contains("gamedata"))
            game.load(nbt.getCompound("gamedata"));
        if (!descPacket) {

            ContainerHelper.loadAllItems(nbt, inventory);
        }


    }

    public void submitItem(ServerPlayer sender) {
        inventory.set(EXAMINE_SLOT, ResearchListeners.submitItem(sender, inventory.get(EXAMINE_SLOT)));
    }

    public boolean tryCombine(ServerPlayer player, CardPos cp1, CardPos cp2) {
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

    public void updateGame(ServerPlayer player) {
        if (game.isFinished()) {

            ResearchListeners.commitGameLevel(player, game.getLvl());
            game.reset();
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        if (!descPacket) {

            ContainerHelper.saveAllItems(nbt, inventory);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new DrawDeskContainer(pContainerId, pPlayerInventory, this);
    }


    @Override
    public Component getDisplayName() {
        return Lang.translateKey("gui.frostedheart.draw_desk");
    }

}
