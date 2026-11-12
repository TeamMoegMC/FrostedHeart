package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

public final class TrialKilnBlock extends BaseEntityBlock {
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    private final boolean successful;
    public TrialKilnBlock(Properties properties, boolean successful) { super(properties); this.successful = successful; registerDefaultState(stateDefinition.any().setValue(PHASE, 0)); }
    public boolean successful() { return successful; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PHASE); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TrialKilnBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FRContents.BlockEntityTypes.TRIAL_KILN.get(), TrialKilnBlockEntity::serverTick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TrialKilnBlockEntity kiln) {
            int phase = state.getValue(PHASE); ItemStack held = player.getItemInHand(hand);
            if (phase >= 2 && held.isEmpty()) {
                player.displayClientMessage(Component.translatable(outcomeMessage(phase)), true);
                kiln.collect(player);
            } else if (phase == 1) player.displayClientMessage(Component.translatable("message.frostedresearch.trial_kiln.running", (kiln.remaining() + 19) / 20), true);
            else if (phase >= 2) player.displayClientMessage(Component.translatable(outcomeMessage(phase)), true);
            else {
                var service = KnowledgeService.forPlayer(server);
                if (held.isEmpty()) {
                    var selected = kiln.cycleTarget(service);
                    Component title = selected.map(service::targetLabel).orElse(Component.empty());
                    player.displayClientMessage(selected.isPresent() ? Component.translatable("message.frostedresearch.trial_kiln.target", title)
                        : Component.translatable("message.frostedresearch.trial_kiln.unbound"), true);
                } else {
                    var start = kiln.start(service, held);
                    player.displayClientMessage(Component.translatable("message.frostedresearch.trial_kiln." + start.name().toLowerCase(Locale.ROOT)), true);
                    if (start == TrialKilnBlockEntity.Start.STARTED && kiln.binding().isEmpty()) player.sendSystemMessage(Component.translatable("message.frostedresearch.trial_kiln.unbound"));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private String outcomeMessage(int phase) {
        return "message.frostedresearch.trial_kiln." + (phase == 2 ? "good" : this == FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get() ? "clay_failed" : "sand_failed");
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(); }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && !level.isClientSide && level.getBlockEntity(pos) instanceof TrialKilnBlockEntity kiln) {
            ItemStack result = kiln.settle(); if (!result.isEmpty()) popResource(level, pos, result);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
