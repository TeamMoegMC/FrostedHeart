package com.teammoeg.frostedresearch.knowledge.experiment;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

public final class PrototypeTuyereBlock extends BaseEntityBlock {
    public PrototypeTuyereBlock(Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new PrototypeTuyereBlockEntity(pos,state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack) {
        super.setPlacedBy(level,pos,state,entity,stack);
        if(entity instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof PrototypeTuyereBlockEntity prototype) {
            prototype.placed(KnowledgeService.forPlayer(player));
            player.displayClientMessage(Component.translatable(prototype.binding().isPresent() ? "message.frostedresearch.tuyere.bound" : "message.frostedresearch.tuyere.unbound"),true);
        }
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(hand!=InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if(player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof PrototypeTuyereBlockEntity prototype) {
            var service=KnowledgeService.forPlayer(server);var binding=prototype.cycle(service);
            var title=binding.map(value -> KnowledgeDefinitions.current().projects().get(service.state().research().get(value.run()).project()).title()).orElse("");
            player.displayClientMessage(binding.isPresent() ? Component.translatable("message.frostedresearch.tuyere.target",Component.translatableWithFallback(title,title)) : Component.translatable("message.frostedresearch.tuyere.no_targets"),true);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) { return List.of(new ItemStack(this)); }
}
