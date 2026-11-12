package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class TrialKilnBlockEntity extends BlockEntity {
    public static final ResourceLocation EXPERIMENT = new ResourceLocation("frostedresearch:trial_kiln");
    public static final int FUEL = 4, DURATION = 400;
    public enum Start {STARTED, FUEL_MISSING, CHOOSE_TARGET, UNAVAILABLE}
    private int remaining;
    private boolean settled;
    private ResearchSampleBinding binding;
    public TrialKilnBlockEntity(BlockPos pos, BlockState state) { super(FRContents.BlockEntityTypes.TRIAL_KILN.get(), pos, state); }
    public int remaining() { return remaining; }
    public Optional<ResearchSampleBinding> binding() { return Optional.ofNullable(binding); }
    public Optional<ResearchSampleBinding> cycleTarget(KnowledgeService service) {
        var targets = service.sampleTargets(EXPERIMENT);
        int current = binding == null ? -1 : targets.indexOf(binding);
        binding = targets.isEmpty() ? null : targets.get((current + 1) % targets.size());
        setChanged(); return binding();
    }
    public Start start(KnowledgeService service, ItemStack fuel) {
        if (settled || getBlockState().getValue(TrialKilnBlock.PHASE) != 0 || level == null || level.isClientSide) return Start.UNAVAILABLE;
        if (!fuel.is(Items.STICK) || fuel.getCount() < FUEL) return Start.FUEL_MISSING;
        var targets = service.sampleTargets(EXPERIMENT);
        if (binding == null || !targets.contains(binding)) {
            if (targets.size() > 1) return Start.CHOOSE_TARGET;
            binding = targets.isEmpty() ? null : targets.get(0);
        }
        fuel.shrink(FUEL); remaining = DURATION;
        level.setBlock(worldPosition, getBlockState().setValue(TrialKilnBlock.PHASE, 1), 3); setChanged();
        return Start.STARTED;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, TrialKilnBlockEntity kiln) {
        if (level.isClientSide || kiln.settled || state.getValue(TrialKilnBlock.PHASE) != 1) return;
        if (kiln.remaining > 0) kiln.remaining--;
        if (kiln.remaining == 0) level.setBlock(pos, state.setValue(TrialKilnBlock.PHASE, ((TrialKilnBlock)state.getBlock()).successful() ? 2 : 3), 3);
        kiln.setChanged();
    }
    /** Shared by collection and removal, marking settlement before a block replacement can reenter it. */
    public ItemStack settle() {
        if (settled) return ItemStack.EMPTY;
        settled = true; setChanged();
        int phase = getBlockState().getValue(TrialKilnBlock.PHASE);
        ItemStack result = phase < 2 ? new ItemStack(getBlockState().getBlock())
            : new ItemStack(phase == 2 ? FRContents.Items.FIRED_TRIAL_KILN.get() : FRContents.Items.CRACKED_TRIAL_KILN.get());
        if (phase >= 2 && binding != null) binding.write(result);
        return result;
    }
    public void collect(net.minecraft.world.entity.player.Player player) {
        ItemStack result = settle(); player.getInventory().add(result);
        if (!result.isEmpty()) player.drop(result, false);
        if (level != null) level.removeBlock(worldPosition, false);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("remaining", remaining); tag.putBoolean("settled", settled);
        if (binding != null) tag.put("binding", ResearchSampleBinding.CODEC.encodeStart(NbtOps.INSTANCE, binding).result().orElseThrow());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); remaining = Math.max(0, Math.min(DURATION, tag.getInt("remaining"))); settled = tag.getBoolean("settled");
        binding = tag.contains("binding") ? ResearchSampleBinding.CODEC.parse(NbtOps.INSTANCE, tag.get("binding")).result().orElse(null) : null;
    }
}
