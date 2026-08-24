/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingResult;
import com.teammoeg.frostedheart.content.town.transport.P2PRouteCardState;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** Player-held two-endpoint pairing tool. Stored facts are hints, never connection authority. */
public final class FreightRouteCardItem extends Item {
    private static final String STATE_TAG = "p2pRoute";
    private static final String CUSTOM_MODEL_DATA_TAG = "CustomModelData";
    private static final int SELECTED_MODEL_DATA = 1;

    public FreightRouteCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof P2PTerminalBlockEntity terminal) {
            return useOnTerminal(player, context.getItemInHand(), terminal);
        }
        clearOrUnbind(player, context.getItemInHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            clearOrUnbind(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        getState(stack).selectedEndpoint().ifPresent(endpoint -> {
            tooltip.add(Component.translatable(
                            "tooltip.frostedheart.freight_route_card.selected",
                            Component.translatable(roleNameKey(endpoint.role())),
                            endpoint.pos().pos().getX(), endpoint.pos().pos().getY(),
                            endpoint.pos().pos().getZ())
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable(
                            "tooltip.frostedheart.freight_route_card.selected_hint")
                    .withStyle(ChatFormatting.GRAY));
        });
    }

    private static InteractionResult useOnTerminal(
            ServerPlayer player,
            ItemStack stack,
            P2PTerminalBlockEntity clicked
    ) {
        if (!clicked.claimOrAuthorize(player)) {
            feedback(player, "message.frostedheart.freight_route_card.permission_denied");
            return InteractionResult.CONSUME;
        }
        P2PRouteCardState state = getState(stack);
        if (state.selectedEndpoint().isEmpty()) {
            setState(stack, P2PRouteCardState.selected(clicked.endpointFact()));
            feedback(player, "message.frostedheart.freight_route_card.endpoint_selected");
            return InteractionResult.CONSUME;
        }

        P2PTerminalEndpoint selected = state.selectedEndpoint().orElseThrow();
        Optional<P2PTerminalBlockEntity> first = resolveLoadedTerminal(player, selected);
        TeamDataHolder holder = CTeamDataManager.get(player);
        if (first.isEmpty() || holder == null
                || !first.get().belongsToTeam(holder.getId())
                || !clicked.belongsToTeam(holder.getId())) {
            feedback(player, "message.frostedheart.freight_route_card.endpoint_unavailable");
            return InteractionResult.CONSUME;
        }
        TeamTown town = clicked.resolveTown().orElse(null);
        if (town == null || first.get().resolveTown().map(other ->
                other.getTownData().orElse(null) != town.getTownData().orElse(null)).orElse(true)) {
            feedback(player, "message.frostedheart.freight_route_card.permission_denied");
            return InteractionResult.CONSUME;
        }

        P2PBindingResult result = town.bindOrRebindP2PTerminals(
                first.get().endpointFact(), clicked.endpointFact(),
                first.get().isRedstonePowered(), clicked.isRedstonePowered());
        if (result.decision() == P2PBindingDecision.ACCEPTED) {
            setState(stack, P2PRouteCardState.connected(
                    result.connectionId().orElseThrow()));
            feedback(player, "message.frostedheart.freight_route_card.connected");
        } else {
            feedback(player, decisionMessage(result.decision()));
        }
        return InteractionResult.CONSUME;
    }

    private static Optional<P2PTerminalBlockEntity> resolveLoadedTerminal(
            ServerPlayer player,
            P2PTerminalEndpoint endpoint
    ) {
        ServerLevel level = player.server.getLevel(endpoint.pos().dimension());
        if (level == null || !level.hasChunkAt(endpoint.pos().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(endpoint.pos().pos())
                instanceof P2PTerminalBlockEntity terminal
                ? Optional.of(terminal) : Optional.empty();
    }

    private static void clearOrUnbind(ServerPlayer player, ItemStack stack) {
        P2PRouteCardState state = getState(stack);
        if (state.connectionId().isPresent()) {
            TeamDataHolder holder = CTeamDataManager.get(player);
            TeamTown town = holder == null ? null
                    : new TeamTownProvider(holder.getId()).getTown();
            P2PBindingDecision decision = town == null
                    ? P2PBindingDecision.INVALID_ENDPOINT
                    : town.unbindP2PConnection(state.connectionId().orElseThrow()).decision();
            feedback(player, decision == P2PBindingDecision.ACCEPTED
                    ? "message.frostedheart.freight_route_card.disconnected"
                    : "message.frostedheart.freight_route_card.stale_connection");
            setState(stack, P2PRouteCardState.EMPTY);
            return;
        }
        if (state.selectedEndpoint().isPresent()) {
            setState(stack, P2PRouteCardState.EMPTY);
            feedback(player, "message.frostedheart.freight_route_card.selection_cleared");
        }
    }

    private static String decisionMessage(P2PBindingDecision decision) {
        return switch (decision) {
            case INSUFFICIENT_CAPACITY ->
                    "message.frostedheart.freight_route_card.insufficient_capacity";
            case CROSS_DIMENSION ->
                    "message.frostedheart.freight_route_card.cross_dimension";
            case SELF_LINK -> "message.frostedheart.freight_route_card.self_link";
            case INCOMPATIBLE_ENDPOINTS ->
                    "message.frostedheart.freight_route_card.incompatible";
            default -> "message.frostedheart.freight_route_card.invalid_endpoint";
        };
    }

    private static void feedback(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    public static P2PRouteCardState getState(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()
                || !stack.getTag().contains(STATE_TAG)) {
            return P2PRouteCardState.EMPTY;
        }
        return P2PRouteCardState.CODEC.parse(
                        NbtOps.INSTANCE, stack.getTag().get(STATE_TAG))
                .resultOrPartial(message -> FHMain.LOGGER.warn(
                        "Discarding invalid freight route card state: {}", message))
                .orElse(P2PRouteCardState.EMPTY);
    }

    public static void setState(ItemStack stack, P2PRouteCardState state) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        P2PRouteCardState next = state == null ? P2PRouteCardState.EMPTY : state;
        if (next.equals(P2PRouteCardState.EMPTY)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(STATE_TAG);
                tag.remove(CUSTOM_MODEL_DATA_TAG);
                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        P2PRouteCardState.CODEC.encodeStart(NbtOps.INSTANCE, next)
                .resultOrPartial(message -> FHMain.LOGGER.warn(
                        "Failed to write freight route card state: {}", message))
                .ifPresent(encoded -> {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.put(STATE_TAG, encoded);
                    if (next.selectedEndpoint().isPresent()) {
                        tag.putInt(CUSTOM_MODEL_DATA_TAG, SELECTED_MODEL_DATA);
                    } else {
                        tag.remove(CUSTOM_MODEL_DATA_TAG);
                    }
                });
    }

    private static String roleNameKey(P2PTerminalRole role) {
        return switch (role) {
            case SHIPPING -> "block.frostedheart.shipping_terminal";
            case RECEIVING -> "block.frostedheart.receiving_terminal";
            case BIDIRECTIONAL -> "block.frostedheart.bidirectional_logistics_terminal";
        };
    }
}
