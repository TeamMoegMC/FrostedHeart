/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingState;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingResult;
import com.teammoeg.frostedheart.content.town.transport.P2PDirectedBinding;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSummary;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import com.teammoeg.frostedheart.content.town.transport.TransportTransferBudget;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Local terminal inventory and runtime facts. Town binding authority lives in {@link TeamTownData}. */
public class P2PTerminalBlockEntity extends CBlockEntity
        implements CTickableBlockEntity, MenuProvider {
    private static final double MAX_ACCESS_DISTANCE_SQUARED = 64.0;
    private static final int FAILURE_COOLDOWN_TICKS = 5;
    private static final int TRANSFER_VISUAL_TICKS = 4;
    private static final int RECEIVER_CONTAINER_PROBE_INTERVAL_TICKS = 20;

    private TeamTownProvider townProvider;
    private boolean redstonePowered;
    private final P2PTerminalBuffer buffer = new P2PTerminalBuffer(this::onBufferChanged);
    private final P2PItemFilter sendFilter = new P2PItemFilter();
    private final P2PItemFilter receiveFilter = new P2PItemFilter();
    private LazyOptional<IItemHandler> externalInventoryCapability = LazyOptional.empty();
    private final TransportTransferBudget transferBudget = new TransportTransferBudget();
    private ItemStack recoveryStack = ItemStack.EMPTY;
    private int failureCooldown;
    private int transferVisualTicks;
    private int receiverContainerProbeTicks;
    private boolean receiverContainerUnavailable;
    private boolean peerUnavailable;
    private P2PBindingDecision lastDecision = P2PBindingDecision.INVALID_REQUEST;
    private double requiredAdditionalCapacity;
    private transient P2PBindingState observedBindings;
    private transient long sourceScanCount;
    private transient long blockStateWriteCount;

    public P2PTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.P2P_TERMINAL.get(), pos, state);
        reviveExternalCapability();
    }

    public P2PTerminalRole getRole() {
        return getBlockState().getBlock() instanceof P2PTerminalBlock terminal
                ? terminal.role()
                : P2PTerminalRole.SHIPPING;
    }

    public P2PTerminalEndpoint endpointFact() {
        if (level == null) {
            throw new IllegalStateException("A P2P terminal must be loaded to expose endpoint facts.");
        }
        return new P2PTerminalEndpoint(
                GlobalPos.of(level.dimension(), worldPosition), getRole());
    }

    public boolean isRedstonePowered() {
        return redstonePowered;
    }

    public TeamTownProvider getTownProvider() {
        return townProvider;
    }

    public P2PTerminalBuffer getBuffer() {
        return buffer;
    }

    public P2PItemFilter getSendFilter() {
        return sendFilter;
    }

    public P2PItemFilter getReceiveFilter() {
        return receiveFilter;
    }

    public ItemStack getRecoveryStack() {
        return recoveryStack.copy();
    }

    public P2PTerminalMenuView getMenuView() {
        Optional<TeamTown> townResult = resolveTown();
        if (townResult.isEmpty() || level == null) {
            return new P2PTerminalMenuView(getRole(), currentVisualState(), List.of(),
                    P2PFilterSnapshot.from(sendFilter), P2PFilterSnapshot.from(receiveFilter),
                    maximumRate(), 0.0, 0.0, lastDecision, requiredAdditionalCapacity);
        }
        TeamTown town = townResult.get();
        P2PBindingState state = town.getP2PBindingState();
        GlobalPos localPos = endpointFact().pos();
        List<P2PTerminalConnectionView> connections = new ArrayList<>();
        for (UUID connectionId : state.connectionIdsAt(localPos)) {
            if (connections.size() >= P2PTerminalMenuView.MAX_VISIBLE_CONNECTIONS) {
                break;
            }
            List<P2PDirectedBinding> directions = state.connection(connectionId).orElse(List.of());
            P2PTerminalEndpoint peer = null;
            int outgoingRate = -1;
            int incomingRate = -1;
            for (P2PDirectedBinding direction : directions) {
                if (direction.sender().pos().equals(localPos)) {
                    peer = direction.receiver();
                    outgoingRate = direction.rateItemsPerSecond();
                } else if (direction.receiver().pos().equals(localPos)) {
                    peer = direction.sender();
                    incomingRate = direction.rateItemsPerSecond();
                }
            }
            if (peer == null) {
                continue;
            }
            Optional<P2PTerminalBlockEntity> loadedPeer = loadedTerminal(peer, true);
            Optional<P2PFilterSummaryState.Entry> cachedPeer = town.getP2PFilterSummaryState()
                    .get(peer.pos());
            connections.add(new P2PTerminalConnectionView(
                    connectionId, peer, outgoingRate, incomingRate, loadedPeer.isPresent(),
                    loadedPeer.map(terminal -> P2PFilterSnapshot.from(terminal.sendFilter))
                            .or(() -> cachedPeer.map(P2PFilterSummaryState.Entry::sendFilter)),
                    loadedPeer.map(terminal -> P2PFilterSnapshot.from(terminal.receiveFilter))
                            .or(() -> cachedPeer.map(P2PFilterSummaryState.Entry::receiveFilter))));
        }
        TownTransportSummary summary = town.getTransportSummary();
        return new P2PTerminalMenuView(getRole(), currentVisualState(), connections,
                P2PFilterSnapshot.from(sendFilter), P2PFilterSnapshot.from(receiveFilter),
                maximumRate(), summary.totalCapacity(), summary.remainingRegistrableCapacity(),
                lastDecision, requiredAdditionalCapacity);
    }

    public boolean setFilterEntry(
            ServerPlayer player,
            boolean sending,
            int slot,
            ItemStack template
    ) {
        if (!claimOrAuthorize(player) || slot < 0 || slot >= P2PItemFilter.SLOT_COUNT) {
            return false;
        }
        (sending ? sendFilter : receiveFilter).setEntry(slot, template);
        failureCooldown = 0;
        setChanged();
        publishFilterSummary();
        return true;
    }

    public boolean toggleFilterMode(ServerPlayer player, boolean sending, boolean fuzzyMode) {
        if (!claimOrAuthorize(player)) {
            return false;
        }
        P2PItemFilter filter = sending ? sendFilter : receiveFilter;
        if (fuzzyMode) {
            filter.setFuzzy(!filter.isFuzzy());
        } else {
            filter.setWhitelist(!filter.isWhitelist());
        }
        failureCooldown = 0;
        setChanged();
        publishFilterSummary();
        return true;
    }

    public P2PBindingResult setTransportRate(ServerPlayer player, int rateItemsPerSecond) {
        Optional<TeamTown> townResult = resolveTown();
        if (!claimOrAuthorize(player) || townResult.isEmpty()) {
            return new P2PBindingResult(P2PBindingDecision.INVALID_REQUEST,
                    Optional.empty(), Set.of(), 0.0,
                    new TownTransportSummary(0.0, 0.0, 0.0, 0.0, 1.0));
        }
        P2PBindingResult result = townResult.get().setP2PTransportRate(
                endpointFact().pos(), rateItemsPerSecond);
        lastDecision = result.decision();
        requiredAdditionalCapacity = result.requiredAdditionalCapacity();
        failureCooldown = 0;
        setChanged();
        return result;
    }

    public P2PBindingResult unbindConnection(ServerPlayer player, UUID connectionId) {
        if (!claimOrAuthorize(player) || connectionId == null) {
            return invalidBindingResult();
        }
        Optional<TeamTown> townResult = resolveTown();
        if (townResult.isEmpty() || !townResult.get().getP2PBindingState()
                .connectionIdsAt(endpointFact().pos()).contains(connectionId)) {
            return invalidBindingResult();
        }
        P2PBindingResult result = townResult.get().unbindP2PConnection(connectionId);
        lastDecision = result.decision();
        requiredAdditionalCapacity = result.requiredAdditionalCapacity();
        failureCooldown = 0;
        setChanged();
        return result;
    }

    private static P2PBindingResult invalidBindingResult() {
        return new P2PBindingResult(P2PBindingDecision.INVALID_REQUEST,
                Optional.empty(), Set.of(), 0.0,
                new TownTransportSummary(0.0, 0.0, 0.0, 0.0, 1.0));
    }

    private int maximumRate() {
        return FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS.maximumRateItemsPerSecond.get();
    }

    private P2PTerminalVisualState currentVisualState() {
        BlockState state = getBlockState();
        return state.hasProperty(P2PTerminalBlock.VISUAL_STATE)
                ? state.getValue(P2PTerminalBlock.VISUAL_STATE)
                : P2PTerminalVisualState.UNBOUND;
    }

    public boolean belongsToTeam(UUID teamId) {
        return townProvider != null && Objects.equals(townProvider.ownerUUID, teamId);
    }

    public boolean claimOrAuthorize(ServerPlayer player) {
        if (!isLiveAndNear(player)) {
            return false;
        }
        TeamDataHolder holder = CTeamDataManager.get(player);
        if (holder == null || TeamTownData.resolveTownDimension(holder) == null
                || !player.serverLevel().dimension().equals(
                TeamTownData.resolveTownDimension(holder))) {
            return false;
        }
        if (townProvider == null) {
            townProvider = new TeamTownProvider(holder.getId());
            setChanged();
        } else if (!Objects.equals(townProvider.ownerUUID, holder.getId())) {
            return false;
        }
        publishFilterSummary();
        refreshVisualStateFromTown();
        return true;
    }

    private void refreshVisualStateFromTown() {
        Optional<TeamTown> townResult = resolveTown();
        if (townResult.isEmpty()) {
            updateVisualState(null, null);
            return;
        }
        TeamTown town = townResult.get();
        updateVisualState(town, town.getP2PBindingState());
    }

    public Optional<TeamTown> resolveTown() {
        if (townProvider == null || townProvider.ownerUUID == null
                || !(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        TeamDataHolder holder = CTeamDataManager.getDataByResearchID(townProvider.ownerUUID);
        if (holder == null || !serverLevel.dimension().equals(
                TeamTownData.resolveTownDimension(holder))) {
            return Optional.empty();
        }
        return Optional.ofNullable(townProvider.getTown());
    }

    public void onNeighborFactChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        receiverContainerProbeTicks = 0;
        boolean next = level.hasNeighborSignal(worldPosition);
        if (next != redstonePowered) {
            redstonePowered = next;
            failureCooldown = 0;
            transferBudget.reset();
            setChanged();
        }
        serverTick(false);
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide) {
            serverTick(true);
        }
    }

    private void serverTick(boolean allowTransfer) {
        Optional<TeamTown> townResult = resolveTown();
        if (townResult.isEmpty()) {
            clearStaleTownRelationship();
            transferBudget.reset();
            receiverContainerUnavailable = false;
            peerUnavailable = townProvider != null;
            updateVisualState(null, null);
            return;
        }
        TeamTown town = townResult.get();
        P2PBindingState bindings = town.getP2PBindingState();
        if (bindings != observedBindings || !allowTransfer) {
            updateBufferLocks(bindings);
            synchronizeRedstoneReservations(town, bindings);
            bindings = town.getP2PBindingState();
            observedBindings = bindings;
        }
        refreshReceiverContainerFact(bindings);

        if (allowTransfer) {
            if (failureCooldown > 0) {
                failureCooldown--;
            }
            if (transferVisualTicks > 0) {
                transferVisualTicks--;
            }
            performOutgoingTransfer(town, bindings);
        }
        updateVisualState(town, bindings);
    }

    private void performOutgoingTransfer(TeamTown town, P2PBindingState bindings) {
        GlobalPos localPos = endpointFact().pos();
        P2PDirectedBinding binding = bindings.outgoing(localPos).orElse(null);
        if (binding == null || binding.rateItemsPerSecond() <= 0 || binding.redstonePaused()) {
            transferBudget.reset();
            peerUnavailable = false;
            return;
        }
        Optional<P2PTerminalBlockEntity> targetTerminalResult = loadedTerminal(
                binding.receiver(), true);
        if (targetTerminalResult.isEmpty()) {
            transferBudget.reset();
            if (isEndpointChunkLoaded(binding.receiver())) {
                town.unbindP2PConnection(binding.connectionId());
                peerUnavailable = false;
            } else {
                peerUnavailable = true;
            }
            return;
        }
        P2PTerminalBlockEntity targetTerminal = targetTerminalResult.get();
        List<P2PDirectedBinding> incoming = bindings.incoming(binding.receiver().pos());
        if (!P2PFairTransferScheduler.isSenderTurn(
                incoming, localPos, level.getGameTime())) {
            peerUnavailable = false;
            return;
        }
        Optional<IItemHandler> sourceResult = resolveP2PSourceHandler();
        if (sourceResult.isEmpty()) {
            transferBudget.reset();
            peerUnavailable = false;
            return;
        }
        if (targetTerminal.receiverContainerUnavailable) {
            transferBudget.reset();
            peerUnavailable = false;
            return;
        }
        Optional<IItemHandler> targetResult = targetTerminal.resolveP2PTargetHandler();
        if (targetResult.isEmpty()) {
            transferBudget.reset();
            if (targetTerminal.getRole() == P2PTerminalRole.RECEIVING) {
                targetTerminal.recordReceiverContainerUnavailable(true);
                peerUnavailable = false;
            } else {
                peerUnavailable = true;
            }
            return;
        }
        if (targetTerminal.getRole() == P2PTerminalRole.RECEIVING) {
            targetTerminal.recordReceiverContainerUnavailable(false);
        }
        IItemHandler source = sourceResult.get();
        IItemHandler target = targetResult.get();
        if (!recoveryStack.isEmpty() && !returnRecoveryTo(source)) {
            peerUnavailable = false;
            return;
        }
        Predicate<ItemStack> combinedFilter = stack -> sendFilter.matches(stack)
                && targetTerminal.receiveFilter.matches(stack);
        if (failureCooldown > 0) {
            peerUnavailable = false;
            return;
        }
        sourceScanCount++;
        boolean demand = hasTransferDemand(source, target, combinedFilter);
        if (!demand) {
            failureCooldown = FAILURE_COOLDOWN_TICKS;
            peerUnavailable = false;
            return;
        }

        TransportReservation reservation = town.getTransportReservation(
                binding.sender().transportEndpointId()).orElse(null);
        if (reservation == null || reservation.admissionStatus() != TransportAdmissionStatus.ACTIVE) {
            transferBudget.reset();
            return;
        }
        TownTransportSummary summary = town.getTransportSummary();
        double effectiveRate = binding.rateItemsPerSecond() * summary.effectiveRateScale();
        int sourceCount = Math.max(1, incoming.size());
        double scheduledRate = effectiveRate * sourceCount;
        if (!Double.isFinite(scheduledRate)) {
            transferBudget.reset();
            return;
        }
        transferVisualTicks = TRANSFER_VISUAL_TICKS;
        targetTerminal.transferVisualTicks = TRANSFER_VISUAL_TICKS;
        int budget = transferBudget.beginTick(scheduledRate, true);
        if (budget <= 0) {
            peerUnavailable = false;
            return;
        }
        P2PItemTransfer.Result result = P2PItemTransfer.move(
                source, target, combinedFilter, budget, this::retainRecovery);
        if (result.movedItems() > 0) {
            targetTerminal.setChanged();
        } else if (result.shouldCooldown()) {
            failureCooldown = FAILURE_COOLDOWN_TICKS;
        }
        peerUnavailable = false;
    }

    private void refreshReceiverContainerFact(P2PBindingState bindings) {
        boolean needsContainer = getRole() == P2PTerminalRole.RECEIVING
                && !bindings.incoming(endpointFact().pos()).isEmpty();
        if (!needsContainer) {
            receiverContainerProbeTicks = 0;
            receiverContainerUnavailable = false;
            return;
        }
        if (receiverContainerProbeTicks > 0) {
            receiverContainerProbeTicks--;
            if (receiverContainerProbeTicks > 0) {
                return;
            }
        }
        recordReceiverContainerUnavailable(adjacentInventoryCapability().isEmpty());
    }

    private void recordReceiverContainerUnavailable(boolean unavailable) {
        receiverContainerUnavailable = unavailable;
        receiverContainerProbeTicks = RECEIVER_CONTAINER_PROBE_INTERVAL_TICKS;
    }

    private Optional<IItemHandler> resolveP2PSourceHandler() {
        if (getRole() == P2PTerminalRole.BIDIRECTIONAL) {
            return buffer.isPendingLocked()
                    ? Optional.empty() : Optional.of(buffer.p2pSourceView());
        }
        if (getRole() != P2PTerminalRole.SHIPPING) {
            return Optional.empty();
        }
        return adjacentInventoryCapability();
    }

    private Optional<IItemHandler> resolveP2PTargetHandler() {
        if (getRole() == P2PTerminalRole.BIDIRECTIONAL) {
            return buffer.isReceivedLocked()
                    ? Optional.empty() : Optional.of(buffer.p2pTargetView());
        }
        if (getRole() != P2PTerminalRole.RECEIVING) {
            return Optional.empty();
        }
        return adjacentInventoryCapability();
    }

    private Optional<IItemHandler> adjacentInventoryCapability() {
        if (level == null || !(getBlockState().getBlock() instanceof P2PTerminalBlock terminal)) {
            return Optional.empty();
        }
        Direction towardInventory = terminal.inventoryConnectionFace(getBlockState());
        BlockEntity adjacent = level.getBlockEntity(worldPosition.relative(towardInventory));
        if (adjacent == null) {
            return Optional.empty();
        }
        return adjacent.getCapability(ForgeCapabilities.ITEM_HANDLER,
                towardInventory.getOpposite()).resolve();
    }

    private Optional<P2PTerminalBlockEntity> loadedTerminal(
            P2PTerminalEndpoint endpoint,
            boolean validateTeam
    ) {
        if (!(level instanceof ServerLevel localLevel) || endpoint == null) {
            return Optional.empty();
        }
        ServerLevel targetLevel = localLevel.getServer().getLevel(endpoint.pos().dimension());
        if (targetLevel == null || !targetLevel.isLoaded(endpoint.pos().pos())) {
            return Optional.empty();
        }
        BlockEntity blockEntity = targetLevel.getBlockEntity(endpoint.pos().pos());
        if (!(blockEntity instanceof P2PTerminalBlockEntity terminal)
                || terminal.getRole() != endpoint.role()
                || validateTeam && (townProvider == null
                || !terminal.belongsToTeam(townProvider.ownerUUID))) {
            return Optional.empty();
        }
        return Optional.of(terminal);
    }

    private boolean isEndpointChunkLoaded(P2PTerminalEndpoint endpoint) {
        if (!(level instanceof ServerLevel localLevel) || endpoint == null) {
            return false;
        }
        ServerLevel targetLevel = localLevel.getServer().getLevel(endpoint.pos().dimension());
        return targetLevel != null && targetLevel.isLoaded(endpoint.pos().pos());
    }

    private void clearStaleTownRelationship() {
        if (townProvider == null || level == null || level.isClientSide) {
            return;
        }
        TeamTown staleTown = townProvider.getTown();
        if (staleTown == null) {
            return;
        }
        GlobalPos endpoint = endpointFact().pos();
        List.copyOf(staleTown.getP2PBindingState().connectionIdsAt(endpoint))
                .forEach(staleTown::unbindP2PConnection);
        staleTown.removeP2PFilterSummary(endpoint);
    }

    private void synchronizeRedstoneReservations(TeamTown town, P2PBindingState bindings) {
        GlobalPos localPos = endpointFact().pos();
        if (!bindings.connectionIdsAt(localPos).isEmpty()) {
            town.setP2PEndpointRedstonePowered(localPos, redstonePowered);
        }
    }

    private void updateBufferLocks(P2PBindingState bindings) {
        if (getRole() != P2PTerminalRole.BIDIRECTIONAL) {
            buffer.setLocks(false, false);
            return;
        }
        GlobalPos localPos = endpointFact().pos();
        boolean outgoing = bindings.outgoing(localPos).isPresent();
        boolean incoming = !bindings.incoming(localPos).isEmpty();
        buffer.setLocks(incoming && !outgoing, outgoing && !incoming);
    }

    private static boolean hasTransferDemand(
            IItemHandler source,
            IItemHandler target,
            Predicate<ItemStack> filter
    ) {
        for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
            ItemStack visible = source.getStackInSlot(sourceSlot);
            if (visible.isEmpty() || !filter.test(visible)) {
                continue;
            }
            ItemStack extracted = source.extractItem(sourceSlot, 1, true);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack remainder = extracted;
            for (int targetSlot = 0; targetSlot < target.getSlots()
                    && !remainder.isEmpty(); targetSlot++) {
                remainder = target.insertItem(targetSlot, remainder, true);
            }
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean returnRecoveryTo(IItemHandler source) {
        ItemStack remainder = recoveryStack;
        for (int slot = 0; slot < source.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = source.insertItem(slot, remainder, false);
        }
        if (ItemStack.matches(recoveryStack, remainder)) {
            return false;
        }
        recoveryStack = remainder.copy();
        setChanged();
        return recoveryStack.isEmpty();
    }

    private void retainRecovery(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!recoveryStack.isEmpty()) {
            throw new IllegalStateException("P2P sender already owns an unresolved recovery stack.");
        }
        recoveryStack = stack.copy();
        setChanged();
    }

    private void onBufferChanged() {
        failureCooldown = 0;
        setChanged();
    }

    private void publishFilterSummary() {
        if (level == null || level.isClientSide) {
            return;
        }
        resolveTown().ifPresent(town -> town.updateP2PFilterSummary(
                endpointFact().pos(), P2PFilterSnapshot.from(sendFilter),
                P2PFilterSnapshot.from(receiveFilter)));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        reviveExternalCapability();
        publishFilterSummary();
        onNeighborFactChanged();
    }

    @Override
    public void onRemoved() {
        if (level != null && !level.isClientSide
                && !(level.getBlockState(worldPosition).getBlock() instanceof P2PTerminalBlock)) {
            resolveTown().ifPresent(town -> {
                GlobalPos endpoint = endpointFact().pos();
                List.copyOf(town.getP2PBindingState().connectionIdsAt(endpoint))
                        .forEach(town::unbindP2PConnection);
                town.removeP2PFilterSummary(endpoint);
            });
            for (int slot = 0; slot < P2PTerminalBuffer.TOTAL_SLOTS; slot++) {
                ItemStack stack = buffer.inventory().extractItem(slot, Integer.MAX_VALUE, false);
                dropStack(stack);
            }
            dropStack(recoveryStack);
            recoveryStack = ItemStack.EMPTY;
        }
        transferBudget.reset();
        externalInventoryCapability.invalidate();
        super.onRemoved();
    }

    private void dropStack(ItemStack stack) {
        if (level != null && !stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack.copy());
        }
    }

    void updateVisualState(@Nullable TeamTown town, @Nullable P2PBindingState bindings) {
        if (level == null || level.isClientSide
                || !(level.getBlockState(worldPosition).getBlock() instanceof P2PTerminalBlock)) {
            return;
        }
        P2PBindingState state = bindings;
        if (state == null && town != null) {
            state = town.getP2PBindingState();
        }
        boolean bound = state != null
                && !state.connectionIdsAt(endpointFact().pos()).isEmpty();
        TownTransportSummary summary = town == null ? null : town.getTransportSummary();
        boolean shortage = summary != null && TransportReservationModel.meaningfullyGreater(
                summary.reservedCapacity(), summary.totalCapacity());
        boolean unavailableReceiverContainer = state != null
                && hasUnavailableReceiverContainer(state);
        P2PTerminalVisualState next = selectVisualState(bound, redstonePowered, shortage,
                unavailableReceiverContainer, peerUnavailable, transferVisualTicks > 0);
        BlockState current = level.getBlockState(worldPosition);
        if (current.getValue(P2PTerminalBlock.VISUAL_STATE) != next) {
            level.setBlock(worldPosition,
                    current.setValue(P2PTerminalBlock.VISUAL_STATE, next),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            blockStateWriteCount++;
        }
    }

    private boolean hasUnavailableReceiverContainer(P2PBindingState bindings) {
        if (getRole() == P2PTerminalRole.RECEIVING) {
            return receiverContainerUnavailable
                    && !bindings.incoming(endpointFact().pos()).isEmpty();
        }
        P2PDirectedBinding outgoing = bindings.outgoing(endpointFact().pos()).orElse(null);
        if (outgoing == null || outgoing.receiver().role() != P2PTerminalRole.RECEIVING) {
            return false;
        }
        return loadedTerminal(outgoing.receiver(), true)
                .map(terminal -> terminal.receiverContainerUnavailable)
                .orElse(false);
    }

    static P2PTerminalVisualState selectVisualState(
            boolean bound,
            boolean redstonePowered,
            boolean shortage,
            boolean receiverContainerUnavailable,
            boolean peerUnavailable,
            boolean transferring
    ) {
        if (!bound) {
            return P2PTerminalVisualState.UNBOUND;
        }
        if (redstonePowered) {
            return P2PTerminalVisualState.REDSTONE_PAUSED;
        }
        if (shortage) {
            return P2PTerminalVisualState.SHORTAGE;
        }
        if (receiverContainerUnavailable) {
            return P2PTerminalVisualState.RECEIVER_CONTAINER_UNAVAILABLE;
        }
        if (peerUnavailable) {
            return P2PTerminalVisualState.PEER_UNLOADED;
        }
        return transferring
                ? P2PTerminalVisualState.TRANSFERRING
                : P2PTerminalVisualState.IDLE;
    }

    long getSourceScanCount() {
        return sourceScanCount;
    }

    long getBlockStateWriteCount() {
        return blockStateWriteCount;
    }

    private boolean isLiveAndNear(ServerPlayer player) {
        return player != null && level == player.level() && !isRemoved()
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= MAX_ACCESS_DISTANCE_SQUARED;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        redstonePowered = nbt.getBoolean("redstonePowered");
        townProvider = nbt.contains("townProvider")
                && ITownProviderSerializable.fromNBT(nbt.getCompound("townProvider"))
                instanceof TeamTownProvider provider ? provider : null;
        buffer.deserializeNBT(nbt.getCompound("buffer"));
        sendFilter.deserializeNBT(nbt.getCompound("sendFilter"));
        receiveFilter.deserializeNBT(nbt.getCompound("receiveFilter"));
        recoveryStack = nbt.contains("recoveryStack")
                ? ItemStack.of(nbt.getCompound("recoveryStack")) : ItemStack.EMPTY;
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putBoolean("redstonePowered", redstonePowered);
        if (townProvider != null) {
            nbt.put("townProvider", townProvider.toNBT());
        }
        nbt.put("buffer", buffer.serializeNBT());
        nbt.put("sendFilter", sendFilter.serializeNBT());
        nbt.put("receiveFilter", receiveFilter.serializeNBT());
        if (!recoveryStack.isEmpty()) {
            nbt.put("recoveryStack", recoveryStack.save(new CompoundTag()));
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability == ForgeCapabilities.ITEM_HANDLER
                && exposesExternalInventoryOn(getRole(), side)) {
            return externalInventoryCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    /** Bidirectional terminals expose the same restricted buffer view on every face. */
    static boolean exposesExternalInventoryOn(
            P2PTerminalRole role,
            @Nullable Direction side
    ) {
        return role == P2PTerminalRole.BIDIRECTIONAL;
    }

    @Override
    public void invalidateCaps() {
        externalInventoryCapability.invalidate();
        super.invalidateCaps();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        reviveExternalCapability();
    }

    private void reviveExternalCapability() {
        if (!externalInventoryCapability.isPresent()) {
            externalInventoryCapability = LazyOptional.of(buffer::externalView);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new P2PTerminalMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(displayNameKey(getRole()));
    }

    static String displayNameKey(P2PTerminalRole role) {
        return switch (role) {
            case SHIPPING -> "container.frostedheart.shipping_terminal";
            case RECEIVING -> "container.frostedheart.receiving_terminal";
            case BIDIRECTIONAL -> "container.frostedheart.bidirectional_logistics_terminal";
        };
    }
}
