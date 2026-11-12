package com.teammoeg.frostedresearch.knowledge.experiment;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

/** Called by actual bellows blow before the original Bloomery checks; no furnace is required. */
public final class TuyereTrials {
    public static final ResourceLocation EXPERIMENT=new ResourceLocation("frostedresearch:tuyere_trial");
    public static final ResourceLocation NORMAL=new ResourceLocation("charcoal_pit:sandy_tuyere");
    private TuyereTrials() {}
    public static boolean tryBlow(ServerLevel level,BlockPos bellows,BlockState state) {
        if(!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;
        BlockPos target=bellows.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        if(!(level.getBlockEntity(target) instanceof PrototypeTuyereBlockEntity prototype)) return false;
        var binding=prototype.binding().orElse(null);if(binding==null) return false;
        var team=KnowledgeDataAPI.getData(binding.team()).orElse(null);if(team==null) return false;
        var service=KnowledgeService.forTeam(team.team(),level.getGameTime());
        if(!service.observationTargets(EXPERIMENT).contains(binding)) return false;
        var normal=ForgeRegistries.BLOCKS.getValue(NORMAL);
        if(normal==null || normal.defaultBlockState().isAir() || !level.setBlockAndUpdate(target,normal.defaultBlockState())) return false;
        PendingResearchProofs.put(level,target,binding);return true;
    }
}
