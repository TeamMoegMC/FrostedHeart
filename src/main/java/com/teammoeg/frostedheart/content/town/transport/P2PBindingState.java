/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.GlobalPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable town authority for P2P connections and their derived indexes. */
public final class P2PBindingState {
    public static final int MAX_DIRECTED_BINDINGS = 4096;
    public static final P2PBindingState EMPTY = new P2PBindingState(List.of(), true);

    private static final Codec<List<P2PDirectedBinding>> TOLERANT_BINDING_LIST_CODEC = new Codec<>() {
        private final Codec<List<P2PDirectedBinding>> delegate = P2PDirectedBinding.CODEC.listOf();

        @Override
        public <T> DataResult<T> encode(
                List<P2PDirectedBinding> input,
                DynamicOps<T> ops,
                T prefix
        ) {
            if (input.size() > MAX_DIRECTED_BINDINGS) {
                return DataResult.error(() -> "P2P binding state exceeds "
                        + MAX_DIRECTED_BINDINGS + " directed bindings.");
            }
            return delegate.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<List<P2PDirectedBinding>, T>> decode(
                DynamicOps<T> ops,
                T input
        ) {
            List<T> encoded = ops.getStream(input)
                    .resultOrPartial(message -> FHMain.LOGGER.warn(
                            "Discarding invalid P2P binding list: {}", message))
                    .map(stream -> stream.limit(MAX_DIRECTED_BINDINGS + 1L).toList())
                    .orElse(List.of());
            if (encoded.size() > MAX_DIRECTED_BINDINGS) {
                FHMain.LOGGER.warn("Truncating P2P binding list to {} entries.",
                        MAX_DIRECTED_BINDINGS);
                encoded = encoded.subList(0, MAX_DIRECTED_BINDINGS);
            }
            List<P2PDirectedBinding> valid = new ArrayList<>(encoded.size());
            for (T entry : encoded) {
                P2PDirectedBinding.CODEC.parse(ops, entry)
                        .resultOrPartial(message -> FHMain.LOGGER.warn(
                                "Discarding invalid P2P binding entry: {}", message))
                        .ifPresent(valid::add);
            }
            return DataResult.success(Pair.of(List.copyOf(valid), ops.empty()));
        }
    };

    public static final Codec<P2PBindingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TOLERANT_BINDING_LIST_CODEC.optionalFieldOf("bindings", List.of())
                    .forGetter(P2PBindingState::bindings)
    ).apply(instance, bindings -> new P2PBindingState(bindings, true)));

    private final List<P2PDirectedBinding> bindings;
    private final Map<UUID, List<P2PDirectedBinding>> byConnection;
    private final Map<GlobalPos, P2PDirectedBinding> outgoingBySender;
    private final Map<GlobalPos, List<P2PDirectedBinding>> incomingByReceiver;
    private final Map<GlobalPos, Set<UUID>> connectionIdsByEndpoint;

    private P2PBindingState(Collection<P2PDirectedBinding> bindings, boolean tolerant) {
        Normalized normalized = normalize(bindings, tolerant);
        this.bindings = normalized.bindings();
        this.byConnection = normalized.byConnection();
        this.outgoingBySender = normalized.outgoingBySender();
        this.incomingByReceiver = normalized.incomingByReceiver();
        Map<GlobalPos, Set<UUID>> endpointConnections = new HashMap<>();
        this.bindings.forEach(binding -> {
            endpointConnections.computeIfAbsent(binding.sender().pos(), ignored -> new TreeSet<>())
                    .add(binding.connectionId());
            endpointConnections.computeIfAbsent(binding.receiver().pos(), ignored -> new TreeSet<>())
                    .add(binding.connectionId());
        });
        Map<GlobalPos, Set<UUID>> immutableEndpointConnections = new HashMap<>();
        endpointConnections.forEach((endpoint, ids) -> immutableEndpointConnections.put(
                endpoint, Collections.unmodifiableSet(new TreeSet<>(ids))));
        this.connectionIdsByEndpoint = Collections.unmodifiableMap(immutableEndpointConnections);
    }

    public List<P2PDirectedBinding> bindings() {
        return bindings;
    }

    public Optional<P2PDirectedBinding> outgoing(GlobalPos sender) {
        return Optional.ofNullable(outgoingBySender.get(sender));
    }

    public List<P2PDirectedBinding> incoming(GlobalPos receiver) {
        return incomingByReceiver.getOrDefault(receiver, List.of());
    }

    public Optional<List<P2PDirectedBinding>> connection(UUID connectionId) {
        return Optional.ofNullable(byConnection.get(connectionId));
    }

    public Set<UUID> connectionIdsAt(GlobalPos endpoint) {
        if (endpoint == null) {
            return Set.of();
        }
        return connectionIdsByEndpoint.getOrDefault(endpoint, Set.of());
    }

    public Optional<UUID> connectionIdBetween(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second
    ) {
        if (first == null || second == null || first.pos().equals(second.pos())) {
            return Optional.empty();
        }
        Set<UUID> secondConnections = connectionIdsAt(second.pos());
        return connectionIdsAt(first.pos()).stream()
                .filter(secondConnections::contains)
                .filter(connectionId -> connection(connectionId)
                        .map(bindings -> bindings.stream().allMatch(binding ->
                                endpointMatchesEither(binding.sender(), first, second)
                                        && endpointMatchesEither(
                                                binding.receiver(), first, second)))
                        .orElse(false))
                .findFirst();
    }

    private static boolean endpointMatchesEither(
            P2PTerminalEndpoint endpoint,
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second
    ) {
        return endpoint.equals(first) || endpoint.equals(second);
    }

    public BindingPlan planConnection(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second,
            int defaultRateItemsPerSecond,
            UUID connectionId
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(connectionId, "connectionId");
        if (defaultRateItemsPerSecond < 0) {
            throw new IllegalArgumentException("Default P2P rate must be non-negative.");
        }
        if (first.pos().equals(second.pos())) {
            throw new IllegalArgumentException("A P2P endpoint cannot bind to itself.");
        }
        if (!first.pos().dimension().equals(second.pos().dimension())) {
            throw new IllegalArgumentException("P2P endpoints must share a dimension.");
        }

        List<Direction> directions = new ArrayList<>(2);
        if (first.role().canSend() && second.role().canReceive()) {
            directions.add(new Direction(first, second));
        }
        if (second.role().canSend() && first.role().canReceive()) {
            directions.add(new Direction(second, first));
        }
        if (directions.isEmpty()) {
            throw new IllegalArgumentException("P2P terminal roles are incompatible.");
        }

        Set<UUID> removals = conflictingConnectionIds(first);
        removals.addAll(conflictingConnectionIds(second));
        List<P2PDirectedBinding> additions = directions.stream()
                .map(direction -> new P2PDirectedBinding(
                        connectionId,
                        direction.sender(),
                        direction.receiver(),
                        outgoing(direction.sender().pos())
                                .map(P2PDirectedBinding::rateItemsPerSecond)
                                .orElse(defaultRateItemsPerSecond),
                        false))
                .sorted(P2PDirectedBinding.STABLE_COMPARATOR)
                .toList();
        return new BindingPlan(connectionId, removals, additions);
    }

    public P2PBindingState apply(BindingPlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<P2PDirectedBinding> candidate = bindings.stream()
                .filter(binding -> !plan.removedConnectionIds().contains(binding.connectionId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        candidate.addAll(plan.newBindings());
        return new P2PBindingState(candidate, false);
    }

    public P2PBindingState withoutConnection(UUID connectionId) {
        if (connectionId == null || !byConnection.containsKey(connectionId)) {
            return this;
        }
        return new P2PBindingState(bindings.stream()
                .filter(binding -> !binding.connectionId().equals(connectionId))
                .toList(), false);
    }

    public P2PBindingState withRate(GlobalPos sender, int rateItemsPerSecond) {
        if (rateItemsPerSecond < 0) {
            throw new IllegalArgumentException("P2P rate must be non-negative.");
        }
        P2PDirectedBinding old = outgoingBySender.get(sender);
        if (old == null) {
            throw new IllegalArgumentException("P2P sender has no outgoing binding.");
        }
        List<P2PDirectedBinding> changed = bindings.stream()
                .map(binding -> binding.equals(old) ? binding.withRate(rateItemsPerSecond) : binding)
                .toList();
        return new P2PBindingState(changed, false);
    }

    public P2PBindingState withEndpointRedstonePowered(GlobalPos endpoint, boolean powered) {
        if (endpoint == null || connectionIdsAt(endpoint).isEmpty()) {
            return this;
        }
        List<P2PDirectedBinding> changed = bindings.stream()
                .map(binding -> binding.withEndpointRedstonePowered(endpoint, powered))
                .toList();
        return changed.equals(bindings) ? this : new P2PBindingState(changed, false);
    }

    private Set<UUID> conflictingConnectionIds(P2PTerminalEndpoint endpoint) {
        Set<UUID> removals = new TreeSet<>();
        for (P2PDirectedBinding binding : bindings) {
            boolean senderMatch = binding.sender().pos().equals(endpoint.pos());
            boolean receiverMatch = binding.receiver().pos().equals(endpoint.pos());
            if (!senderMatch && !receiverMatch) {
                continue;
            }
            P2PTerminalRole storedRole = senderMatch
                    ? binding.sender().role() : binding.receiver().role();
            if (storedRole != endpoint.role()
                    || endpoint.role() == P2PTerminalRole.BIDIRECTIONAL
                    || senderMatch) {
                removals.add(binding.connectionId());
            }
        }
        return removals;
    }

    private static Normalized normalize(
            Collection<P2PDirectedBinding> source,
            boolean tolerant
    ) {
        TreeMap<UUID, List<P2PDirectedBinding>> groups = new TreeMap<>();
        if (source != null) {
            for (P2PDirectedBinding binding : source) {
                if (binding == null) {
                    if (!tolerant) {
                        throw new IllegalArgumentException("P2P binding state contains null.");
                    }
                    continue;
                }
                groups.computeIfAbsent(binding.connectionId(), ignored -> new ArrayList<>())
                        .add(binding);
            }
        }
        if (groups.values().stream().mapToInt(List::size).sum() > MAX_DIRECTED_BINDINGS) {
            throw new IllegalArgumentException("P2P binding state exceeds its size limit.");
        }

        List<P2PDirectedBinding> accepted = new ArrayList<>();
        Map<UUID, List<P2PDirectedBinding>> acceptedGroups = new LinkedHashMap<>();
        Map<GlobalPos, P2PDirectedBinding> outgoing = new HashMap<>();
        Map<GlobalPos, List<P2PDirectedBinding>> incoming = new HashMap<>();
        Map<GlobalPos, P2PTerminalRole> roles = new HashMap<>();
        Map<GlobalPos, UUID> occupiedBidirectional = new HashMap<>();

        for (Map.Entry<UUID, List<P2PDirectedBinding>> entry : groups.entrySet()) {
            List<P2PDirectedBinding> group = entry.getValue().stream()
                    .sorted(P2PDirectedBinding.STABLE_COMPARATOR).toList();
            try {
                validateConnectionGroup(group);
                for (P2PDirectedBinding binding : group) {
                    validateEndpointRole(roles, binding.sender());
                    validateEndpointRole(roles, binding.receiver());
                    if (outgoing.containsKey(binding.sender().pos())) {
                        throw new IllegalArgumentException("P2P sender has multiple targets.");
                    }
                    validateBidirectionalOccupancy(occupiedBidirectional,
                            binding.sender(), binding.connectionId());
                    validateBidirectionalOccupancy(occupiedBidirectional,
                            binding.receiver(), binding.connectionId());
                }
                accepted.addAll(group);
                acceptedGroups.put(entry.getKey(), List.copyOf(group));
                for (P2PDirectedBinding binding : group) {
                    outgoing.put(binding.sender().pos(), binding);
                    incoming.computeIfAbsent(binding.receiver().pos(), ignored -> new ArrayList<>())
                            .add(binding);
                    roles.put(binding.sender().pos(), binding.sender().role());
                    roles.put(binding.receiver().pos(), binding.receiver().role());
                    if (binding.sender().role() == P2PTerminalRole.BIDIRECTIONAL) {
                        occupiedBidirectional.put(binding.sender().pos(), binding.connectionId());
                    }
                    if (binding.receiver().role() == P2PTerminalRole.BIDIRECTIONAL) {
                        occupiedBidirectional.put(binding.receiver().pos(), binding.connectionId());
                    }
                }
            } catch (IllegalArgumentException exception) {
                if (!tolerant) {
                    throw exception;
                }
                FHMain.LOGGER.warn("Discarding invalid P2P connection {}: {}",
                        entry.getKey(), exception.getMessage());
            }
        }
        accepted.sort(P2PDirectedBinding.STABLE_COMPARATOR);
        Map<GlobalPos, List<P2PDirectedBinding>> immutableIncoming = new HashMap<>();
        incoming.forEach((pos, values) -> immutableIncoming.put(pos,
                values.stream().sorted(P2PDirectedBinding.STABLE_COMPARATOR).toList()));
        return new Normalized(List.copyOf(accepted),
                Collections.unmodifiableMap(acceptedGroups),
                Collections.unmodifiableMap(outgoing),
                Collections.unmodifiableMap(immutableIncoming));
    }

    private static void validateConnectionGroup(List<P2PDirectedBinding> group) {
        if (group.size() == 1) {
            P2PDirectedBinding binding = group.get(0);
            if (binding.sender().role() == P2PTerminalRole.BIDIRECTIONAL
                    && binding.receiver().role() == P2PTerminalRole.BIDIRECTIONAL) {
                throw new IllegalArgumentException(
                        "A bidirectional pair requires both directed bindings.");
            }
            return;
        }
        if (group.size() != 2) {
            throw new IllegalArgumentException("A P2P connection must contain one or two directions.");
        }
        P2PDirectedBinding first = group.get(0);
        P2PDirectedBinding second = group.get(1);
        boolean opposite = first.sender().pos().equals(second.receiver().pos())
                && first.receiver().pos().equals(second.sender().pos());
        boolean bidirectional = first.sender().role() == P2PTerminalRole.BIDIRECTIONAL
                && first.receiver().role() == P2PTerminalRole.BIDIRECTIONAL
                && second.sender().role() == P2PTerminalRole.BIDIRECTIONAL
                && second.receiver().role() == P2PTerminalRole.BIDIRECTIONAL;
        if (!opposite || !bidirectional) {
            throw new IllegalArgumentException(
                    "Two-direction connections require opposite bidirectional terminals.");
        }
    }

    private static void validateEndpointRole(
            Map<GlobalPos, P2PTerminalRole> roles,
            P2PTerminalEndpoint endpoint
    ) {
        P2PTerminalRole existing = roles.get(endpoint.pos());
        if (existing != null && existing != endpoint.role()) {
            throw new IllegalArgumentException("A P2P endpoint has conflicting roles.");
        }
    }

    private static void validateBidirectionalOccupancy(
            Map<GlobalPos, UUID> occupied,
            P2PTerminalEndpoint endpoint,
            UUID connectionId
    ) {
        if (endpoint.role() != P2PTerminalRole.BIDIRECTIONAL) {
            return;
        }
        UUID existing = occupied.get(endpoint.pos());
        if (existing != null && !existing.equals(connectionId)) {
            throw new IllegalArgumentException("A bidirectional terminal has multiple peers.");
        }
    }

    public record BindingPlan(
            UUID connectionId,
            Set<UUID> removedConnectionIds,
            List<P2PDirectedBinding> newBindings
    ) {
        public BindingPlan {
            Objects.requireNonNull(connectionId, "connectionId");
            removedConnectionIds = Collections.unmodifiableSet(new LinkedHashSet<>(
                    removedConnectionIds == null ? Set.of() : new TreeSet<>(removedConnectionIds)));
            List<P2PDirectedBinding> sorted = new ArrayList<>(
                    newBindings == null ? List.of() : newBindings);
            sorted.sort(P2PDirectedBinding.STABLE_COMPARATOR);
            if (sorted.isEmpty() || sorted.stream()
                    .anyMatch(binding -> !connectionId.equals(binding.connectionId()))) {
                throw new IllegalArgumentException("Binding plan requires one consistent connection.");
            }
            newBindings = List.copyOf(sorted);
        }
    }

    private record Direction(P2PTerminalEndpoint sender, P2PTerminalEndpoint receiver) {
    }

    private record Normalized(
            List<P2PDirectedBinding> bindings,
            Map<UUID, List<P2PDirectedBinding>> byConnection,
            Map<GlobalPos, P2PDirectedBinding> outgoingBySender,
            Map<GlobalPos, List<P2PDirectedBinding>> incomingByReceiver
    ) {
    }
}
