package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingResult;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingState;
import com.teammoeg.frostedheart.content.town.transport.P2PDirectedBinding;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSummary;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportConsumerParameters;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointRequest;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationDecision;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationResult;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologySnapshot;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSnapshot;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSummaryState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Transport admission, pairing and topology rules; loaded devices rebind at lifecycle boundaries. */
final class TownTransportService {
    private final TeamTownData data;
    TownTransportService(TeamTownData data) { this.data = data; }

    public static void prepare(TeamTown town, ResourceKey<Level> dimension) {
        town.prepareWarehouseTopology(dimension);
        town.prepareTransportState();
    }

    public static boolean rebind(Map<GlobalPos, WarehouseTopologyListener> pending, TeamTown town) {
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                entry.getValue().onTownRebound(town);
                iterator.remove();
            } catch (RuntimeException failure) {
                FHMain.LOGGER.error("Town device {} still requires rebinding", entry.getKey(), failure);
            }
        }
        return pending.isEmpty();
    }

    public TownTransportState getTransportState() {
        return data.getTransportState();
    }

    public TownTransportSummary getTransportSummary() {
        return TownTransportSummary.from(getCurrentTransportCapacity(), data.getTransportState());
    }

    public Map<TransportEndpointId, TransportReservation> getTransportReservations() {
        return data.getTransportState().getReservations();
    }

    public P2PBindingState getP2PBindingState() {
        return data.getP2PBindingState();
    }

    public P2PFilterSummaryState getP2PFilterSummaryState() {
        return data.getP2PFilterSummaryState();
    }

    public void updateP2PFilterSummary(
            GlobalPos endpoint,
            P2PFilterSnapshot sendFilter,
            P2PFilterSnapshot receiveFilter
    ) {
        if (endpoint == null || sendFilter == null || receiveFilter == null) {
            return;
        }
        P2PFilterSummaryState previous = data.getP2PFilterSummaryState();
        P2PFilterSummaryState updated;
        try {
            updated = previous.with(endpoint, sendFilter, receiveFilter);
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (updated != previous) {
            data.setP2PFilterSummaryState(updated);
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    public void removeP2PFilterSummary(GlobalPos endpoint) {
        P2PFilterSummaryState previous = data.getP2PFilterSummaryState();
        P2PFilterSummaryState updated = previous.without(endpoint);
        if (updated != previous) {
            data.setP2PFilterSummaryState(updated);
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    public Optional<TransportReservation> getTransportReservation(TransportEndpointId endpointId) {
        return Optional.ofNullable(data.getTransportState().getReservation(endpointId));
    }

    public TownTransportSnapshot getTransportSnapshot() {
        return data.createTransportSnapshot();
    }

    public WarehouseTopologySnapshot prepareWarehouseTopology(
            ResourceKey<Level> authoritativeTownDimension
    ) {
        return currentTransportParameters()
                .map(parameters -> data.refreshWarehouseTopologyIfDirty(
                        parameters, authoritativeTownDimension))
                .orElse(data.getAppliedWarehouseTopology());
    }

    public WarehouseTopologySnapshot getWarehouseTopology() {
        return data.getAppliedWarehouseTopology();
    }

    public void registerWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        data.registerWarehouseTopologyListener(devicePos, listener);
    }

    public void unregisterWarehouseTopologyListener(
            GlobalPos devicePos,
            WarehouseTopologyListener listener
    ) {
        data.unregisterWarehouseTopologyListener(devicePos, listener);
    }

    public TransportReservationResult registerOrUpdateTransportEndpoint(TransportEndpointRequest request) {
        return registerOrUpdateWarehouseInterface(request);
    }

    public TransportReservationResult registerOrUpdateWarehouseInterface(TransportEndpointRequest request) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (request == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        if (request.endpointKind() != TransportEndpointKind.WAREHOUSE_INTERFACE) {
            return result(TransportReservationDecision.INVALID_BINDING,
                    data.getTransportState().getReservation(request.endpointId()), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        data.refreshWarehouseTopologyIfDirty(
                parameters, data.getAppliedWarehouseTopology().townDimension());
        TransportReservation old = data.getTransportState().getReservation(request.endpointId());

        if (old == null && data.getTransportState().getReservations().size()
                >= TownTransportSnapshot.MAX_RESERVATIONS) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }

        if (old != null && old.endpointKind() != request.endpointKind()) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (!parameters.isRateValid(request.rateItemsPerSecond())) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }

        OptionalDouble metricResult = currentWarehouseDistance(request.endpointId());
        if (metricResult.isEmpty()) {
            if (old != null && request.rateItemsPerSecond() != 0) {
                return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
            }
            TransportReservation unavailable = new TransportReservation(
                    request.endpointKind(), 0, 0.0, 0.0,
                    TransportAdmissionStatus.UNAVAILABLE);
            replaceAndMark(request.endpointId(), unavailable);
            return result(TransportReservationDecision.ACCEPTED, unavailable, 0.0);
        }
        double scaleMetric = metricResult.getAsDouble();
        ResolvedTransportAdmission admission = admitResolvedEndpoint(
                request.endpointId(), request.endpointKind(), request.rateItemsPerSecond(),
                scaleMetric, parameters);
        if (admission.decision() == TransportReservationDecision.ACCEPTED) {
            TransportReservation accepted = admission.acceptedReservation().orElseThrow();
            replaceAndMark(request.endpointId(), accepted);
            return result(admission.decision(), accepted,
                    admission.requiredAdditionalCapacity());
        }
        if (admission.decision() == TransportReservationDecision.INSUFFICIENT_CAPACITY
                && old == null) {
            TransportReservation disabled = new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE, 0, scaleMetric, 0.0,
                    TransportAdmissionStatus.DISABLED);
            replaceAndMark(request.endpointId(), disabled);
            return result(admission.decision(), disabled,
                    admission.requiredAdditionalCapacity());
        }
        return result(admission.decision(), old,
                admission.requiredAdditionalCapacity());
    }

    ResolvedTransportAdmission evaluateP2PTransportAdmission(
            GlobalPos sender,
            GlobalPos receiver,
            int rateItemsPerSecond
    ) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (sender == null || receiver == null || parametersResult.isEmpty()) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, null);
        }
        double scaleMetric = TransportReservationModel.p2pManhattanDistance(sender, receiver);
        if (!TransportReservationModel.isFiniteNonNegative(scaleMetric)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_BINDING,
                    data.getTransportState().getReservation(new TransportEndpointId(sender)));
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        return admitResolvedEndpoint(new TransportEndpointId(sender),
                TransportEndpointKind.P2P_DIRECT_LINK, rateItemsPerSecond,
                scaleMetric, parameters);
    }

    private ResolvedTransportAdmission admitResolvedEndpoint(
            TransportEndpointId endpointId,
            TransportEndpointKind endpointKind,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportConsumerParameters parameters
    ) {
        TransportReservation old = endpointId == null
                ? null
                : data.getTransportState().getReservation(endpointId);
        if (endpointId == null || endpointKind == null || parameters == null
                || !parameters.isRateValid(rateItemsPerSecond)
                || !TransportReservationModel.isFiniteNonNegative(scaleMetric)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        if (old != null && old.endpointKind() != endpointKind) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_BINDING, old);
        }
        if (rateItemsPerSecond == 0) {
            return ResolvedTransportAdmission.accepted(old, new TransportReservation(
                    endpointKind, 0, scaleMetric, 0.0,
                    TransportAdmissionStatus.DISABLED), 0.0);
        }

        double candidateReserved = TransportReservationModel.requiredCapacity(
                endpointKind, rateItemsPerSecond, scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(candidateReserved)) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        double oldReserved = old == null ? 0.0 : old.reservedTransportCapacity();
        TransportReservationModel.AdmissionEvaluation evaluation =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        oldReserved, candidateReserved);
        if (!evaluation.valid()) {
            return ResolvedTransportAdmission.invalid(
                    TransportReservationDecision.INVALID_REQUEST, old);
        }
        if (!evaluation.accepted()) {
            return ResolvedTransportAdmission.rejected(old,
                    evaluation.requiredAdditionalCapacity());
        }
        return ResolvedTransportAdmission.accepted(old, new TransportReservation(
                endpointKind, rateItemsPerSecond, scaleMetric, candidateReserved,
                TransportAdmissionStatus.ACTIVE), evaluation.requiredAdditionalCapacity());
    }

    record ResolvedTransportAdmission(
            TransportReservationDecision decision,
            Optional<TransportReservation> previousReservation,
            Optional<TransportReservation> acceptedReservation,
            double requiredAdditionalCapacity
    ) {
        ResolvedTransportAdmission {
            Objects.requireNonNull(decision, "decision");
            previousReservation = previousReservation == null
                    ? Optional.empty() : previousReservation;
            acceptedReservation = acceptedReservation == null
                    ? Optional.empty() : acceptedReservation;
            requiredAdditionalCapacity = TransportReservationModel.isFiniteNonNegative(
                    requiredAdditionalCapacity) ? requiredAdditionalCapacity : 0.0;
        }

        private static ResolvedTransportAdmission invalid(
                TransportReservationDecision decision,
                TransportReservation previous
        ) {
            return new ResolvedTransportAdmission(decision, Optional.ofNullable(previous),
                    Optional.empty(), 0.0);
        }

        private static ResolvedTransportAdmission rejected(
                TransportReservation previous,
                double requiredAdditionalCapacity
        ) {
            return new ResolvedTransportAdmission(
                    TransportReservationDecision.INSUFFICIENT_CAPACITY,
                    Optional.ofNullable(previous), Optional.empty(),
                    requiredAdditionalCapacity);
        }

        private static ResolvedTransportAdmission accepted(
                TransportReservation previous,
                TransportReservation accepted,
                double requiredAdditionalCapacity
        ) {
            return new ResolvedTransportAdmission(TransportReservationDecision.ACCEPTED,
                    Optional.ofNullable(previous), Optional.of(accepted),
                    requiredAdditionalCapacity);
        }
    }

    public P2PBindingResult bindOrRebindP2PTerminals(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second
    ) {
        return bindOrRebindP2PTerminals(first, second, false, false);
    }

    public P2PBindingResult bindOrRebindP2PTerminals(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second,
            boolean firstRedstonePowered,
            boolean secondRedstonePowered
    ) {
        if (first == null || second == null) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        if (first.pos().equals(second.pos())) {
            return p2pResult(P2PBindingDecision.SELF_LINK,
                    null, Set.of(), 0.0);
        }
        if (!first.pos().dimension().equals(second.pos().dimension())) {
            return p2pResult(P2PBindingDecision.CROSS_DIMENSION,
                    null, Set.of(), 0.0);
        }
        boolean hasDirection = first.role().canSend() && second.role().canReceive()
                || second.role().canSend() && first.role().canReceive();
        if (!hasDirection) {
            return p2pResult(P2PBindingDecision.INCOMPATIBLE_ENDPOINTS,
                    null, Set.of(), 0.0);
        }
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);

        P2PBindingState currentBindings = data.getP2PBindingState();
        UUID connectionId = UUID.randomUUID();
        P2PBindingState.BindingPlan plan;
        P2PBindingState candidateBindings;
        try {
            plan = currentBindings.planConnection(first, second,
                    parameters.defaultRateItemsPerSecond(), connectionId);
            candidateBindings = currentBindings.apply(plan);
            candidateBindings = candidateBindings.withEndpointRedstonePowered(
                    first.pos(), firstRedstonePowered);
            candidateBindings = candidateBindings.withEndpointRedstonePowered(
                    second.pos(), secondRedstonePowered);
        } catch (IllegalArgumentException exception) {
            return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                    null, Set.of(), 0.0);
        }

        Map<TransportEndpointId, TransportReservation> candidateReservations =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        candidateReservations.putAll(data.getTransportState().getReservations());
        double removedCapacity = 0.0;
        for (UUID removedConnectionId : plan.removedConnectionIds()) {
            for (P2PDirectedBinding oldBinding : currentBindings.connection(removedConnectionId)
                    .orElse(List.of())) {
                TransportReservation removed = candidateReservations.remove(
                        oldBinding.sender().transportEndpointId());
                if (removed != null) {
                    removedCapacity += removed.reservedTransportCapacity();
                }
            }
        }

        double addedCapacity = 0.0;
        for (P2PDirectedBinding binding : candidateBindings.connection(connectionId)
                .orElse(List.of())) {
            if (!parameters.isRateValid(binding.rateItemsPerSecond())) {
                return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                        null, Set.of(), 0.0);
            }
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            TransportReservation collision = candidateReservations.get(senderId);
            if (collision != null
                    && collision.endpointKind() != TransportEndpointKind.P2P_DIRECT_LINK) {
                return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                        null, Set.of(), 0.0);
            }
            TransportReservation reservation = reservationForBinding(binding, parameters);
            if (reservation == null) {
                return p2pResult(P2PBindingDecision.INVALID_ENDPOINT,
                        null, Set.of(), 0.0);
            }
            candidateReservations.put(senderId, reservation);
            addedCapacity += reservation.reservedTransportCapacity();
        }
        if (candidateReservations.size() > TownTransportSnapshot.MAX_RESERVATIONS) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }

        TransportReservationModel.AdmissionEvaluation admission =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        removedCapacity, addedCapacity);
        if (!admission.valid()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        if (!admission.accepted()) {
            return p2pResult(P2PBindingDecision.INSUFFICIENT_CAPACITY,
                    null, Set.of(), admission.requiredAdditionalCapacity());
        }

        data.setP2PBindingState(candidateBindings);
        boolean reservationsChanged = data.getTransportState().replaceAllReservations(
                candidateReservations, parameters);
        data.markP2PTransportReconciled(parameters);
        if (reservationsChanged || !candidateBindings.bindings().equals(currentBindings.bindings())) {
            data.getDataSyncCache().markTransportStateChanged();
        }
        return p2pResult(P2PBindingDecision.ACCEPTED, connectionId,
                plan.removedConnectionIds(), admission.requiredAdditionalCapacity());
    }

    public P2PBindingResult unbindP2PConnection(UUID connectionId) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (connectionId == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST,
                    null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        List<P2PDirectedBinding> removed = currentBindings.connection(connectionId)
                .orElse(null);
        if (removed == null) {
            return p2pResult(P2PBindingDecision.STALE_CONNECTION,
                    null, Set.of(), 0.0);
        }

        Map<TransportEndpointId, TransportReservation> candidateReservations =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        candidateReservations.putAll(data.getTransportState().getReservations());
        for (P2PDirectedBinding binding : removed) {
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            TransportReservation reservation = candidateReservations.get(senderId);
            if (reservation != null
                    && reservation.endpointKind() == TransportEndpointKind.P2P_DIRECT_LINK) {
                candidateReservations.remove(senderId);
            }
        }
        P2PBindingState candidateBindings = currentBindings.withoutConnection(connectionId);
        data.setP2PBindingState(candidateBindings);
        data.getTransportState().replaceAllReservations(candidateReservations, parameters);
        data.markP2PTransportReconciled(parameters);
        data.getDataSyncCache().markTransportStateChanged();
        return p2pResult(P2PBindingDecision.ACCEPTED,
                connectionId, Set.of(connectionId), 0.0);
    }

    public Optional<UUID> getP2PConnectionIdBetween(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second
    ) {
        return data.getP2PBindingState().connectionIdBetween(first, second);
    }

    public P2PBindingResult setP2PTransportRate(GlobalPos sender, int rateItemsPerSecond) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (sender == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        P2PDirectedBinding current = currentBindings.outgoing(sender).orElse(null);
        if (current == null || !parameters.isRateValid(rateItemsPerSecond)) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        P2PBindingState candidateBindings;
        try {
            candidateBindings = currentBindings.withRate(sender, rateItemsPerSecond);
        } catch (IllegalArgumentException exception) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        P2PDirectedBinding candidate = candidateBindings.outgoing(sender).orElseThrow();
        TransportReservation reservation = reservationForBinding(candidate, parameters);
        if (reservation == null) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportReservation previous = data.getTransportState().getReservation(
                candidate.sender().transportEndpointId());
        double previousCapacity = previous == null ? 0.0
                : previous.reservedTransportCapacity();
        TransportReservationModel.AdmissionEvaluation admission =
                TransportReservationModel.evaluateAdmission(
                        getCurrentTransportCapacity(),
                        data.getTransportState().getReservedTransportCapacity(),
                        previousCapacity, reservation.reservedTransportCapacity());
        if (!admission.valid()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        if (!admission.accepted()) {
            return p2pResult(P2PBindingDecision.INSUFFICIENT_CAPACITY,
                    null, Set.of(), admission.requiredAdditionalCapacity());
        }
        data.setP2PBindingState(candidateBindings);
        replaceAndMark(candidate.sender().transportEndpointId(), reservation);
        data.markP2PTransportReconciled(parameters);
        return p2pResult(P2PBindingDecision.ACCEPTED,
                current.connectionId(), Set.of(), admission.requiredAdditionalCapacity());
    }

    /** Compatibility facade for sender-side redstone state updates. */
    public P2PBindingResult setP2PRedstonePaused(GlobalPos sender, boolean paused) {
        return setP2PEndpointRedstonePowered(sender, paused);
    }

    /** Redstone resumption restores accepted reservations even if it creates town shortage. */
    public P2PBindingResult setP2PEndpointRedstonePowered(
            GlobalPos endpoint,
            boolean powered
    ) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (endpoint == null || parametersResult.isEmpty()) {
            return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        P2PBindingState currentBindings = data.getP2PBindingState();
        Set<UUID> connectionIds = currentBindings.connectionIdsAt(endpoint);
        if (connectionIds.isEmpty()) {
            return p2pResult(P2PBindingDecision.STALE_CONNECTION, null, Set.of(), 0.0);
        }
        P2PBindingState candidateBindings = currentBindings.withEndpointRedstonePowered(
                endpoint, powered);
        if (candidateBindings != currentBindings) {
            Map<TransportEndpointId, TransportReservation> candidateReservations =
                    new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
            candidateReservations.putAll(data.getTransportState().getReservations());
            for (P2PDirectedBinding binding : candidateBindings.bindings()) {
                if (!binding.sender().pos().equals(endpoint)
                        && !binding.receiver().pos().equals(endpoint)) {
                    continue;
                }
                TransportReservation reservation = reservationForBinding(binding, parameters);
                if (reservation == null) {
                    return p2pResult(P2PBindingDecision.INVALID_REQUEST, null, Set.of(), 0.0);
                }
                candidateReservations.put(binding.sender().transportEndpointId(), reservation);
            }
            data.setP2PBindingState(candidateBindings);
            data.getTransportState().replaceAllReservations(candidateReservations, parameters);
            data.markP2PTransportReconciled(parameters);
            data.getDataSyncCache().markTransportStateChanged();
        }
        return p2pResult(P2PBindingDecision.ACCEPTED,
                connectionIds.iterator().next(), Set.of(), 0.0);
    }

    private TransportReservation reservationForBinding(
            P2PDirectedBinding binding,
            TransportConsumerParameters parameters
    ) {
        double metric = TransportReservationModel.p2pManhattanDistance(
                binding.sender().pos(), binding.receiver().pos());
        if (!TransportReservationModel.isFiniteNonNegative(metric)) {
            return null;
        }
        if (binding.rateItemsPerSecond() == 0) {
            return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                    0, metric, 0.0, TransportAdmissionStatus.DISABLED);
        }
        if (binding.redstonePaused()) {
            return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                    binding.rateItemsPerSecond(), metric, 0.0,
                    TransportAdmissionStatus.REDSTONE_PAUSED);
        }
        double capacity = TransportReservationModel.requiredCapacity(
                TransportEndpointKind.P2P_DIRECT_LINK,
                binding.rateItemsPerSecond(), metric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(capacity)) {
            return null;
        }
        return new TransportReservation(TransportEndpointKind.P2P_DIRECT_LINK,
                binding.rateItemsPerSecond(), metric, capacity,
                TransportAdmissionStatus.ACTIVE);
    }

    private P2PBindingResult p2pResult(
            P2PBindingDecision decision,
            UUID connectionId,
            Set<UUID> replacedConnectionIds,
            double requiredAdditionalCapacity
    ) {
        return new P2PBindingResult(decision, Optional.ofNullable(connectionId),
                replacedConnectionIds, requiredAdditionalCapacity,
                TownTransportSummary.from(getCurrentTransportCapacity(),
                        data.getTransportState()));
    }

    public TransportReservationResult refreshTransportEndpointMetric(TransportEndpointId endpointId) {
        return refreshWarehouseInterfaceMetric(endpointId);
    }

    public TransportReservationResult refreshWarehouseInterfaceMetric(TransportEndpointId endpointId) {
        Optional<TransportConsumerParameters> parametersResult = currentTransportParameters();
        if (endpointId == null || parametersResult.isEmpty()) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        TransportConsumerParameters parameters = parametersResult.get();
        prepareTransportState(parameters);
        TransportReservation old = data.getTransportState().getReservation(endpointId);
        if (old == null) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        if (old.endpointKind() != TransportEndpointKind.WAREHOUSE_INTERFACE) {
            return result(TransportReservationDecision.INVALID_BINDING, old, 0.0);
        }
        data.refreshWarehouseTopologyIfDirty(
                parameters, data.getAppliedWarehouseTopology().townDimension());
        OptionalDouble metricResult = currentWarehouseDistance(endpointId);
        if (metricResult.isEmpty()) {
            TransportReservation unavailable = new TransportReservation(
                    old.endpointKind(), old.rateItemsPerSecond(), 0.0, 0.0,
                    TransportAdmissionStatus.UNAVAILABLE);
            replaceAndMark(endpointId, unavailable);
            return result(TransportReservationDecision.ACCEPTED, unavailable, 0.0);
        }
        double scaleMetric = metricResult.getAsDouble();
        double refreshedCapacity = TransportReservationModel.capacityForStoredRate(
                old.endpointKind(), old.rateItemsPerSecond(), scaleMetric, parameters);
        if (!TransportReservationModel.isFiniteNonNegative(refreshedCapacity)) {
            return result(TransportReservationDecision.INVALID_REQUEST, old, 0.0);
        }
        TransportReservation refreshed = new TransportReservation(
                old.endpointKind(), old.rateItemsPerSecond(), scaleMetric, refreshedCapacity,
                old.rateItemsPerSecond() == 0
                        ? TransportAdmissionStatus.DISABLED
                        : TransportAdmissionStatus.ACTIVE);
        replaceAndMark(endpointId, refreshed);
        return result(TransportReservationDecision.ACCEPTED, refreshed, 0.0);
    }

    public TransportReservationResult unregisterTransportEndpoint(TransportEndpointId endpointId) {
        prepareTransportState();
        if (endpointId == null) {
            return result(TransportReservationDecision.INVALID_REQUEST, null, 0.0);
        }
        if (data.getTransportState().removeReservation(endpointId)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
        return result(TransportReservationDecision.ACCEPTED, null, 0.0);
    }

    private OptionalDouble currentWarehouseDistance(TransportEndpointId endpointId) {
        if (endpointId == null) {
            return OptionalDouble.empty();
        }
        WarehouseTopologySnapshot topology = data.getAppliedWarehouseTopology();
        if (!topology.isUsable()
                || !endpointId.endpointPos().dimension().equals(topology.townDimension())) {
            return OptionalDouble.empty();
        }
        double metric = TransportReservationModel.warehouseWeightedDistance(
                endpointId.endpointPos().pos(), topology.entries());
        return TransportReservationModel.isFiniteNonNegative(metric)
                ? OptionalDouble.of(metric)
                : OptionalDouble.empty();
    }

    private void replaceAndMark(TransportEndpointId endpointId, TransportReservation reservation) {
        if (data.getTransportState().replaceReservation(endpointId, reservation)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private TransportReservationResult result(
            TransportReservationDecision decision,
            TransportReservation reservation,
            double requiredAdditionalCapacity
    ) {
        return new TransportReservationResult(decision, Optional.ofNullable(reservation),
                TownTransportSummary.from(getCurrentTransportCapacity(), data.getTransportState()),
                requiredAdditionalCapacity);
    }

    private double getCurrentTransportCapacity() {
        return data.resources.get(VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0));
    }

    public void prepareTransportState() {
        currentTransportParameters().ifPresent(this::prepareTransportState);
    }

    private void prepareTransportState(TransportConsumerParameters parameters) {
        reconcileP2PTransportState(parameters);
        if (data.getTransportState().recalculateReservedCapacities(parameters)) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private void reconcileP2PTransportState(TransportConsumerParameters parameters) {
        if (data.isP2PTransportReconciled(parameters)) {
            return;
        }
        P2PBindingState bindings = data.getP2PBindingState();
        Map<TransportEndpointId, TransportReservation> reconciled =
                new TreeMap<>(TransportEndpointId.STABLE_COMPARATOR);
        data.getTransportState().getReservations().forEach((endpointId, reservation) -> {
            if (reservation.endpointKind() != TransportEndpointKind.P2P_DIRECT_LINK) {
                reconciled.put(endpointId, reservation);
            }
        });

        Set<UUID> invalidConnections = new TreeSet<>();
        for (P2PDirectedBinding binding : bindings.bindings()) {
            TransportEndpointId senderId = binding.sender().transportEndpointId();
            if (reconciled.containsKey(senderId)
                    || reconciled.size() >= TownTransportSnapshot.MAX_RESERVATIONS) {
                invalidConnections.add(binding.connectionId());
                continue;
            }
            TransportReservation reservation = reservationForBinding(binding, parameters);
            if (reservation == null) {
                invalidConnections.add(binding.connectionId());
                continue;
            }
            reconciled.put(senderId, reservation);
        }
        for (UUID connectionId : invalidConnections) {
            bindings = bindings.withoutConnection(connectionId);
        }
        if (!invalidConnections.isEmpty()) {
            data.setP2PBindingState(bindings);
            P2PBindingState validBindings = bindings;
            reconciled.entrySet().removeIf(entry ->
                    entry.getValue().endpointKind() == TransportEndpointKind.P2P_DIRECT_LINK
                            && validBindings.outgoing(entry.getKey().endpointPos()).isEmpty());
        }
        boolean changed = data.getTransportState().replaceAllReservations(reconciled, parameters);
        data.markP2PTransportReconciled(parameters);
        if (changed || !invalidConnections.isEmpty()) {
            data.getDataSyncCache().markTransportStateChanged();
        }
    }

    private static Optional<TransportConsumerParameters> currentTransportParameters() {
        FHConfig.Server.Town.TransportConsumers config = FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS;
        try {
            return Optional.of(new TransportConsumerParameters(
                    config.defaultRateItemsPerSecond.get(),
                    config.minimumRateItemsPerSecond.get(),
                    config.maximumRateItemsPerSecond.get(),
                    config.warehouseDistanceCostPerBlock.get(),
                    config.p2pDistanceCostPerBlock.get()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
