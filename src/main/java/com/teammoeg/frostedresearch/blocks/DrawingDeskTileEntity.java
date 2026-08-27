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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.IdeaCandidate;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedresearch.gui.drawdesk.game.GenerateInfo;
import com.teammoeg.frostedresearch.gui.drawdesk.game.ResearchGame;
import com.teammoeg.frostedresearch.recipe.ResearchPaperRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private final List<UUID> pinnedEvidence = new ArrayList<>();
    private GamePurpose gamePurpose = GamePurpose.NONE;
    private final List<IdeaCandidate> pendingCandidates = new ArrayList<>();
    private final List<IdeaCandidate> ideaCandidates = new ArrayList<>();
    private InspirationStatus inspirationStatus = InspirationStatus.NONE;

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
        gamePurpose = GamePurpose.LEGACY_CLUE;
        clearCandidates();
    }

    public boolean pinEvidence(ServerPlayer player, UUID recordId) {
        if (gamePurpose == GamePurpose.V2_INSPIRATION) return false;
        if (pinnedEvidence.contains(recordId)) {
            pinnedEvidence.remove(recordId);
            clearCandidates();
            inspirationStatus = InspirationStatus.NONE;
            return true;
        }
        if (pinnedEvidence.size() >= 5 || KnowledgeDataAPI.getData(player).get().observation(recordId).isEmpty()) return false;
        pinnedEvidence.add(recordId);
        clearCandidates();
        inspirationStatus = InspirationStatus.NONE;
        return true;
    }

    public void clearKnowledgeSession() {
        pinnedEvidence.clear();
        clearCandidates();
        gamePurpose = GamePurpose.NONE;
        game.reset();
        inspirationStatus = InspirationStatus.NONE;
    }

    public void initInspirationGame(ServerPlayer player) {
        if (gamePurpose == GamePurpose.V2_INSPIRATION) return;
        List<IdeaCandidate> matched = TeamResearchService.findIdeaCandidates(
                KnowledgeDataAPI.getData(player).get(), Set.copyOf(pinnedEvidence));
        if (matched.isEmpty()) {
            inspirationStatus = InspirationStatus.NO_CANDIDATE;
            return;
        }
        Optional<ResearchPaperRecipe> paper = CUtils.filterRecipes(getLevel().getRecipeManager(), ResearchPaperRecipe.TYPE)
                .stream().filter(recipe -> recipe.maxlevel >= 0 && recipe.paper.test(inventory.getStackInSlot(PAPER_SLOT))).findAny();
        if (paper.isEmpty()) {
            inspirationStatus = InspirationStatus.NEED_PAPER;
            return;
        }
        if (!damageInk(player, 5, 0)) {
            inspirationStatus = InspirationStatus.NEED_INK;
            return;
        }
        inventory.getStackInSlot(PAPER_SLOT).shrink(1);
        game.init(GenerateInfo.all[0], new Random());
        game.setLvl(0);
        gamePurpose = GamePurpose.V2_INSPIRATION;
        inspirationStatus = InspirationStatus.STARTED;
        pendingCandidates.clear();
        pendingCandidates.addAll(matched.stream().limit(3).toList());
        ideaCandidates.clear();
    }

    /** Restarts only the active card layout; the session already paid its one paper/ink cost. */
    public void restartInspirationGame() {
        if (gamePurpose != GamePurpose.V2_INSPIRATION || pendingCandidates.isEmpty()) return;
        game.init(GenerateInfo.all[0], new Random());
        game.setLvl(0);
        inspirationStatus = InspirationStatus.STARTED;
    }

    public boolean recordIdeaCandidate(ServerPlayer player, int candidateIndex) {
        if (candidateIndex < 0 || candidateIndex >= ideaCandidates.size()) return false;
        IdeaCandidate selected = ideaCandidates.get(candidateIndex);
        IdeaCandidate current = TeamResearchService.findIdeaCandidates(
                        KnowledgeDataAPI.getData(player).get(), Set.copyOf(pinnedEvidence)).stream()
                .filter(candidate -> candidate.semanticKey().equals(selected.semanticKey())).findFirst().orElse(null);
        if (current == null) return false;
        TeamResearchService.recordIdea(player, current.topicId(), current.ideaId(),
                "drawing_desk:" + worldPosition.toShortString(), current.evidence());
        clearKnowledgeSession();
        return true;
    }

    public boolean recordIdeaCandidate(ServerPlayer player) { return recordIdeaCandidate(player, 0); }

    public List<UUID> getPinnedEvidence() { return List.copyOf(pinnedEvidence); }
    public GamePurpose getGamePurpose() { return gamePurpose; }
    public InspirationStatus getInspirationStatus() { return inspirationStatus; }
    public List<IdeaCandidate> getIdeaCandidates() { return List.copyOf(ideaCandidates); }
    public boolean hasIdeaCandidate() { return !ideaCandidates.isEmpty(); }

    private void clearCandidates() {
        pendingCandidates.clear();
        ideaCandidates.clear();
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
        pinnedEvidence.clear();
        ListTag pins = nbt.getList("knowledge_pins", Tag.TAG_INT_ARRAY);
        for (int index = 0; index < pins.size() && pinnedEvidence.size() < 5; index++) {
            try {
                UUID pin = NbtUtils.loadUUID(pins.get(index));
                if (!pinnedEvidence.contains(pin)) pinnedEvidence.add(pin);
            } catch (IllegalArgumentException ignored) {
                // Ignore one malformed historical pin while preserving the rest of the desk state.
            }
        }
        try {
            gamePurpose = GamePurpose.valueOf(nbt.getString("game_purpose"));
        } catch (IllegalArgumentException ignored) {
            gamePurpose = GamePurpose.NONE;
        }
        try {
            inspirationStatus = InspirationStatus.valueOf(nbt.getString("inspiration_status"));
        } catch (IllegalArgumentException ignored) {
            inspirationStatus = InspirationStatus.NONE;
        }
        readCandidates(nbt, "idea_candidates", ideaCandidates);
        if (!descPacket) readCandidates(nbt, "pending_idea_candidates", pendingCandidates);
        
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
            if (gamePurpose == GamePurpose.LEGACY_CLUE) {
                ResearchHooks.commitGameLevel(player, game.getLvl());
            } else if (gamePurpose == GamePurpose.V2_INSPIRATION) {
                Set<String> expected = pendingCandidates.stream().map(IdeaCandidate::semanticKey)
                        .collect(java.util.stream.Collectors.toSet());
                ideaCandidates.clear();
                TeamResearchService.findIdeaCandidates(KnowledgeDataAPI.getData(player).get(), Set.copyOf(pinnedEvidence))
                        .stream().filter(candidate -> expected.contains(candidate.semanticKey())).limit(3)
                        .forEach(ideaCandidates::add);
                pendingCandidates.clear();
                if (ideaCandidates.size() == 1) {
                    IdeaCandidate candidate = ideaCandidates.get(0);
                    TeamResearchService.recordIdea(player, candidate.topicId(), candidate.ideaId(),
                            "drawing_desk:" + worldPosition.toShortString(), candidate.evidence());
                    ideaCandidates.clear();
                    pinnedEvidence.clear();
                    inspirationStatus = InspirationStatus.IDEA_RECORDED;
                } else {
                    inspirationStatus = ideaCandidates.isEmpty()
                            ? InspirationStatus.NO_CANDIDATE
                            : InspirationStatus.CANDIDATES_READY;
                }
            }
            game.reset();
            gamePurpose = GamePurpose.NONE;
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.put("gamedata", game.serialize());
        ListTag pins = new ListTag();
        pinnedEvidence.forEach(id -> pins.add(NbtUtils.createUUID(id)));
        nbt.put("knowledge_pins", pins);
        nbt.putString("game_purpose", gamePurpose.name());
        nbt.putString("inspiration_status", inspirationStatus.name());
        writeCandidates(nbt, "idea_candidates", ideaCandidates);
        if (!descPacket) {
            writeCandidates(nbt, "pending_idea_candidates", pendingCandidates);
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

    public enum GamePurpose {
        NONE,
        LEGACY_CLUE,
        V2_INSPIRATION
    }

    /** Client-visible, topic-neutral feedback for an inspiration start request. */
    public enum InspirationStatus {
        NONE,
        NO_CANDIDATE,
        NEED_PAPER,
        NEED_INK,
        STARTED,
        CANDIDATES_READY,
        IDEA_RECORDED
    }

    private static void writeCandidates(CompoundTag nbt, String key, List<IdeaCandidate> candidates) {
        ListTag list = new ListTag();
        for (IdeaCandidate candidate : candidates) {
            CompoundTag entry = new CompoundTag();
            entry.putString("topic", candidate.topicId().toString());
            entry.putString("idea", candidate.ideaId().toString());
            entry.putString("source", candidate.source());
            ListTag evidence = new ListTag();
            candidate.evidence().forEach(id -> evidence.add(NbtUtils.createUUID(id)));
            entry.put("evidence", evidence);
            list.add(entry);
        }
        nbt.put(key, list);
    }

    private static void readCandidates(CompoundTag nbt, String key, List<IdeaCandidate> output) {
        output.clear();
        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size() && output.size() < 3; index++) {
            CompoundTag entry = list.getCompound(index);
            ResourceLocation topic = ResourceLocation.tryParse(entry.getString("topic"));
            ResourceLocation idea = ResourceLocation.tryParse(entry.getString("idea"));
            if (topic == null || idea == null) continue;
            Set<UUID> evidence = new java.util.LinkedHashSet<>();
            ListTag ids = entry.getList("evidence", Tag.TAG_INT_ARRAY);
            for (int evidenceIndex = 0; evidenceIndex < ids.size(); evidenceIndex++) {
                try {
                    evidence.add(NbtUtils.loadUUID(ids.get(evidenceIndex)));
                } catch (IllegalArgumentException ignored) {
                    // Keep the remaining candidate usable if one stored UUID is malformed.
                }
            }
            output.add(new IdeaCandidate(topic, idea, evidence, entry.getString("source")));
        }
    }

}
