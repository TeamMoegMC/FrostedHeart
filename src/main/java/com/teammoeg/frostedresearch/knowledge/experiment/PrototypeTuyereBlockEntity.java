package com.teammoeg.frostedresearch.knowledge.experiment;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class PrototypeTuyereBlockEntity extends BlockEntity {
    private ResearchSampleBinding binding;
    public PrototypeTuyereBlockEntity(BlockPos pos,BlockState state) { super(FRContents.BlockEntityTypes.PROTOTYPE_TUYERE.get(),pos,state); }
    public Optional<ResearchSampleBinding> binding() { return Optional.ofNullable(binding); }
    public void placed(KnowledgeService service) {
        var targets=service.observationTargets(TuyereTrials.EXPERIMENT);binding=targets.size()==1 ? targets.get(0) : null;setChanged();
    }
    public Optional<ResearchSampleBinding> cycle(KnowledgeService service) {
        var targets=service.observationTargets(TuyereTrials.EXPERIMENT);
        if(targets.isEmpty()) return Optional.empty();
        int at=binding==null ? -1 : targets.indexOf(binding);
        binding=targets.get((at+1)%targets.size());setChanged();return binding();
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);if(binding!=null) tag.put("binding",ResearchSampleBinding.CODEC.encodeStart(NbtOps.INSTANCE,binding).result().orElseThrow()); }
    @Override public void load(CompoundTag tag) { super.load(tag);binding=tag.contains("binding") ? ResearchSampleBinding.CODEC.parse(NbtOps.INSTANCE,tag.get("binding")).result().orElse(null) : null; }
}
