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
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;

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
                return com.teammoeg.frostedresearch.knowledge.item.ResearchNotes.isBlank(item)
                        || CUtils.filterRecipes(getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.paper.test(item));
            else
                return false;
        }
    };
    ResearchGame game = new ResearchGame();
    /**
     * Knowledge research run and calculation requirement the current game counts toward; null serves the legacy research.
     */
    private UUID calculationRun;
    private String calculationRequirement = "";
    private int calculationLevel = -1;

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


    /**
     * Starts a game for the knowledge calculation target while it still needs one, otherwise for the legacy research.
     */
    public boolean initGame(ServerPlayer player) {
        if (inventory.getStackInSlot(PAPER_SLOT).isEmpty()) return false;
        int lvl = gameLevel(player);
        if (lvl < 0) return false;
        Optional<ResearchPaperRecipe> pr = CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().filter(r -> r.maxlevel >= lvl && r.paper.test(inventory.getStackInSlot(PAPER_SLOT))).findAny();
        if (!pr.isPresent()) return false;
        //if (EnergyCore.getEnergy(player) <= 0) return;
        if (!damageInk(player, 5, lvl)) return false;
        //EnergyCore.costEnergy(player, 1);
        inventory.getStackInSlot(PAPER_SLOT).shrink(1);
        game.init(GenerateInfo.all[lvl], new Random());
        game.setLvl(lvl);
        return true;
    }

    private int gameLevel(ServerPlayer player) {
        if (calculationRun != null) {
            OptionalInt level = com.teammoeg.frostedresearch.knowledge.KnowledgeService.forPlayer(player)
                    .calculationLevel(calculationRun, calculationRequirement);
            if (level.isPresent()) return calculationLevel = level.getAsInt();
            clearCalculation();
        }
        return ResearchHooks.fetchGameLevel(player);
    }

    /**
     * Points the next games at a knowledge research calculation. Any unfinished game is discarded.
     */
    public void setCalculation(UUID run, String requirement, int level) {
        calculationRun = run;
        calculationRequirement = requirement;
        calculationLevel = level;
        game.reset();
    }

    /**
     * Returns the desk to legacy research calculation and discards any unfinished game.
     */
    public void stopCalculation() {
        clearCalculation();
        game.reset();
    }

    private void clearCalculation() {
        calculationRun = null;
        calculationRequirement = "";
        calculationLevel = -1;
    }

    public boolean hasCalculation() {
        return calculationRun != null;
    }

    public UUID calculationRun() {
        return calculationRun;
    }

    public boolean isCalculating(UUID run, String requirement) {
        return run != null && run.equals(calculationRun) && requirement.equals(calculationRequirement);
    }

    @OnlyIn(Dist.CLIENT)
    private int clientGameLevel() {
        return calculationRun != null ? calculationLevel : ResearchHooks.fetchGameLevel();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isInkSatisfied(int val) {
        ItemStack is = inventory.getStackInSlot(INK_SLOT);
        if (is.isEmpty() || !(is.getItem() instanceof IPen)) return false;
        IPen pen = (IPen) is.getItem();
        return pen.getLevel(is, ClientUtils.getPlayer()) >= clientGameLevel() && pen.canUse(ClientUtils.getPlayer(), is, val);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isPaperSatisfied() {
        ItemStack is = inventory.getStackInSlot(PAPER_SLOT);
        if (is.isEmpty()) return false;
        int lvl = clientGameLevel();
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE).stream().anyMatch(r -> r.maxlevel >= lvl && r.paper.test(is));
    }


    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (nbt.contains("gamedata"))
            game.load(nbt.getCompound("gamedata"));
        if (nbt.hasUUID("calculation_run")) {
            calculationRun = nbt.getUUID("calculation_run");
            calculationRequirement = nbt.getString("calculation_requirement");
            calculationLevel = nbt.getInt("calculation_level");
        } else clearCalculation();
        
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
        // Testing shortcut: a player allowed to cheat finishes a running game by clicking its final card.
        if (cp2 == null && ResearchGame.FINAL_CARD.equals(cp1) && player.hasPermissions(2))
            return game.forceWin();
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
            if (calculationRun != null) {
                var service = com.teammoeg.frostedresearch.knowledge.KnowledgeService.forPlayer(player);
                service.recordCalculation(calculationRun, calculationRequirement, game.getLvl());
                // The next reset starts another game for the same requirement until it is satisfied.
                if (service.calculationLevel(calculationRun, calculationRequirement).isEmpty()) clearCalculation();
            } else ResearchHooks.commitGameLevel(player, game.getLvl());
            game.reset();
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        if (calculationRun != null) {
            nbt.putUUID("calculation_run", calculationRun);
            nbt.putString("calculation_requirement", calculationRequirement);
            nbt.putInt("calculation_level", calculationLevel);
        }
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
