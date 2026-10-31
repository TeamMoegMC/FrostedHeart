/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/**
 * Immutable state of a bounded regional heat network. A detailed solver may
 * prepare this state while publishing its ordinary frame; only one model is
 * advanced after the region chooses its quality.
 */
public final class MinimumThermalState {
    public static final int POINTS_PER_AXIS = 4;
    public static final int AIR_NODES = POINTS_PER_AXIS * POINTS_PER_AXIS * POINTS_PER_AXIS;
    public static final int MATERIAL_NODES = AIR_NODES;
    public static final int POOLED_NODES = AIR_NODES + MATERIAL_NODES;
    public static final int PHASE_BODIES = 32;
    public static final int MAXIMUM_NODES = POOLED_NODES + PHASE_BODIES;
    public static final int TIME_SEGMENTS = 8;
    public static final int MAXIMUM_PAIRS = MAXIMUM_NODES * (MAXIMUM_NODES - 1) / 2;

    /** Geometry-derived background, shared by frames; it contains no previous thermal residual. */
    public static final class Background {
        private final double[] airOffsetsC, nodesC, pageNaturalC;
        private final int[] pages;
        public final double referenceC;

        public Background(double[] airOffsetsC, double[] nodesC, int[] pages, double[] pageNaturalC, double referenceC) {
            this.airOffsetsC = airOffsetsC; this.nodesC = nodesC; this.pages = pages;
            this.pageNaturalC = pageNaturalC; this.referenceC = referenceC;
        }
        public double[] borrowedAirOffsetsC() { return airOffsetsC; }
        public double nodeC(int node) { return nodesC[node]; }
        public double[] copyNodesC() { return nodesC.clone(); }
        public Background withNodesC(double[] values) { return new Background(airOffsetsC, values, pages, pageNaturalC, referenceC); }
        public double naturalRangeC() {
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            for (double value : pageNaturalC) { low = Math.min(low, value); high = Math.max(high, value); }
            return pageNaturalC.length == 0 ? 0 : high - low;
        }
        public double environmentShiftC(java.util.function.IntToDoubleFunction natural) {
            double difference = 0;
            for (int index = 0; index < pages.length; index++) difference += natural.applyAsDouble(pages[index]) - pageNaturalC[index];
            return pages.length == 0 ? 0 : difference / pages.length;
        }
        private void recordBacking(AirBackingMeter meter) {
            if (!meter.objectOnce(this, 64)) return;
            meter.array(airOffsetsC); meter.array(nodesC); meter.array(pages); meter.array(pageNaturalC);
        }
    }

    /** Geometry and coefficients transfer to this immutable descriptor at preparation completion. */
    public static final class Layout {
        public final double minX, minY, minZ, spanX, spanY, spanZ;
        private final double[] capacity, energyReference, boundaryWPerK;
        private final int[] first, second;
        private final double[] conductance;
        private final int[] phaseSlots;
        private final MaterialThermalLaw[] phaseLaws;
        private final double[] inverseCapacity, sensibleHalfSecond;
        private final Background background;
        private final Object inputMappingIdentity;

        public Layout(double minX, double minY, double minZ, double spanX, double spanY, double spanZ,
                double[] capacity, double[] energyReference, double[] boundaryWPerK,
                int[] first, int[] second, double[] conductance,
                int[] phaseSlots, MaterialThermalLaw[] phaseLaws, Background background) {
            this(minX, minY, minZ, spanX, spanY, spanZ, capacity, energyReference, boundaryWPerK,
                    first, second, conductance, phaseSlots, phaseLaws, null, null, background, null);
        }
        private Layout(double minX, double minY, double minZ, double spanX, double spanY, double spanZ,
                double[] capacity, double[] energyReference, double[] boundaryWPerK,
                int[] first, int[] second, double[] conductance, int[] phaseSlots, MaterialThermalLaw[] phaseLaws,
                double[] inverseCapacity, double[] sensibleHalfSecond, Background background, Object inputMappingIdentity) {
            if (!(spanX > 0 && spanY > 0 && spanZ > 0)
                    || capacity.length != MAXIMUM_NODES || energyReference.length != MAXIMUM_NODES
                    || boundaryWPerK.length != MAXIMUM_NODES || first.length != second.length
                    || first.length != conductance.length || first.length > MAXIMUM_PAIRS
                    || phaseSlots.length > PHASE_BODIES || phaseSlots.length != phaseLaws.length)
                throw new IllegalArgumentException("Invalid bounded thermal layout");
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.spanX = spanX; this.spanY = spanY; this.spanZ = spanZ;
            this.capacity = capacity; this.energyReference = energyReference; this.boundaryWPerK = boundaryWPerK;
            this.first = first; this.second = second; this.conductance = conductance;
            this.phaseSlots = phaseSlots; this.phaseLaws = phaseLaws;
            this.background = background;
            this.inputMappingIdentity = inputMappingIdentity == null ? new Object() : inputMappingIdentity;
            this.inverseCapacity = inverseCapacity == null ? new double[MAXIMUM_NODES] : inverseCapacity;
            if (inverseCapacity == null) for (int node = 0; node < MAXIMUM_NODES; node++)
                this.inverseCapacity[node] = capacity[node] > 0 ? 1 / capacity[node] : 0;
            this.sensibleHalfSecond = sensibleHalfSecond == null ? new double[first.length] : sensibleHalfSecond;
            if (sensibleHalfSecond == null) for (int pair = 0; pair < first.length; pair++)
                this.sensibleHalfSecond[pair] = sensibleFactor(pair, .5);
        }

        public double capacity(int node) { return capacity[node]; }
        public double reference(int node) { return energyReference[node]; }
        public int pairCount() { return first.length; }
        public int phaseCount() { return phaseSlots.length; }
        public Background background() { return background; }
        public Object inputMappingIdentity() { return inputMappingIdentity; }
        private Object mappingIdentity(double[] capacities) {
            for (int node = 0; node < MAXIMUM_NODES; node++)
                if ((capacity[node] > 0) != (capacities[node] > 0)) return null;
            return inputMappingIdentity;
        }

        /** Only the pooled enthalpy reference changes when a detailed frame is summarized. */
        public Layout withEnergyReferences(double[] references) {
            return new Layout(minX, minY, minZ, spanX, spanY, spanZ,
                    capacity, references, boundaryWPerK, first, second, conductance, phaseSlots, phaseLaws,
                    inverseCapacity, sensibleHalfSecond, background, inputMappingIdentity);
        }
        public Layout withPoolState(double[] capacities, double[] references) {
            if (java.util.Arrays.equals(capacity, capacities)) return withEnergyReferences(references);
            return new Layout(minX, minY, minZ, spanX, spanY, spanZ,
                    capacities, references, boundaryWPerK, first, second, conductance, phaseSlots, phaseLaws,
                    null, null, background, mappingIdentity(capacities));
        }
        public Layout withPoolState(double[] capacities, double[] references, Background nextBackground) {
            boolean sameCapacity = java.util.Arrays.equals(capacity, capacities);
            return new Layout(minX, minY, minZ, spanX, spanY, spanZ,
                    capacities, references, boundaryWPerK, first, second, conductance, phaseSlots, phaseLaws,
                    sameCapacity ? inverseCapacity : null, sameCapacity ? sensibleHalfSecond : null, nextBackground, mappingIdentity(capacities));
        }
        public double[] copyCapacities() { return capacity.clone(); }
        /** Same physical owner: attach transport without reprojecting its Accepted heat or phase state. */
        private Layout withTransport(Layout graph) {
            if (minX != graph.minX || minY != graph.minY || minZ != graph.minZ
                    || spanX != graph.spanX || spanY != graph.spanY || spanZ != graph.spanZ
                    || !java.util.Arrays.equals(phaseSlots, graph.phaseSlots))
                throw new IllegalArgumentException("Minimum transport changed its physical owner");
            return new Layout(minX, minY, minZ, spanX, spanY, spanZ, capacity, energyReference,
                    graph.boundaryWPerK, graph.first, graph.second, graph.conductance, phaseSlots, phaseLaws,
                    inverseCapacity, null, background, inputMappingIdentity);
        }
        private double sensibleFactor(int pair, double seconds) {
            int a = first[pair], b = second[pair];
            if (a >= POOLED_NODES || b >= POOLED_NODES || !(capacity[a] > 0 && capacity[b] > 0)) return 0;
            double slope = inverseCapacity[a] + inverseCapacity[b];
            return -Math.expm1(-conductance[pair] * slope * seconds) / slope;
        }
        private Layout shiftPool(int node, double joules, double capacityChange, double temperatureC) {
            var capacities = capacity.clone(); var references = energyReference.clone();
            capacities[node] = Math.max(0, capacities[node] + capacityChange);
            references[node] += joules - capacityChange * temperatureC;
            return withPoolState(capacities, references);
        }

        public int phaseIndex(int slot) {
            for (int index = 0; index < phaseSlots.length; index++) if (phaseSlots[index] == slot) return index;
            return -1;
        }

        /** Nearest material pool; the air query uses continuous trilinear weights instead. */
        public int materialNode(double x, double y, double z) {
            int ix = nearest(x, minX, spanX), iy = nearest(y, minY, spanY), iz = nearest(z, minZ, spanZ);
            int preferred = AIR_NODES + ix + POINTS_PER_AXIS * iz + POINTS_PER_AXIS * POINTS_PER_AXIS * iy;
            if (capacity[preferred] > 0) return preferred;
            return nearestLiveNode(x, y, z, AIR_NODES, POOLED_NODES);
        }

        public double nodeX(int node) { return minX + (node % AIR_NODES % POINTS_PER_AXIS) * spanX / 3; }
        public double nodeY(int node) { return minY + (node % AIR_NODES / 16) * spanY / 3; }
        public double nodeZ(int node) { return minZ + (node % AIR_NODES / 4 % 4) * spanZ / 3; }

        /** Static addresses and weights for repeated input; no temperature or phase state is cached. */
        public int airInputWeights(double x, double y, double z, int[] nodes, double[] weights) {
            int count = airWeights(x, y, z, minX, minY, minZ, spanX, spanY, spanZ, nodes, weights);
            int supported = 0; double total = 0;
            for (int index = 0; index < count; index++) if (capacity[nodes[index]] > 0) {
                nodes[supported] = nodes[index]; weights[supported++] = weights[index]; total += weights[index];
            }
            if (supported == 0) {
                int node = nearestLiveNode(x, y, z, 0, AIR_NODES);
                if (node < 0) return 0;
                nodes[0] = node; weights[0] = 1; return 1;
            }
            for (int index = 0; index < supported; index++) weights[index] /= total;
            return supported;
        }

        /** The same partition of unity integrates capacity, energy, input and point queries. */
        public static int airWeights(double x, double y, double z, double minX, double minY, double minZ,
                double spanX, double spanY, double spanZ, int[] nodes, double[] weights) {
            double gx = coordinate(x, minX, spanX), gy = coordinate(y, minY, spanY), gz = coordinate(z, minZ, spanZ);
            int ix = Math.min(2, (int) gx), iy = Math.min(2, (int) gy), iz = Math.min(2, (int) gz), count = 0;
            double dx = gx - ix, dy = gy - iy, dz = gz - iz;
            for (int corner = 0; corner < 8; corner++) {
                int node = ix + (corner & 1) + 4 * (iz + (corner >>> 1 & 1)) + 16 * (iy + (corner >>> 2));
                double amount = weight(corner, dx, dy, dz);
                if (amount == 0) continue;
                nodes[count] = node; weights[count++] = amount;
            }
            return count;
        }

        private int nearestLiveNode(double x, double y, double z, int from, int to) {
            int selected = -1; double distance = Double.POSITIVE_INFINITY;
            for (int node = from; node < to; node++) if (capacity[node] > 0) {
                double dx = nodeX(node) - x, dy = nodeY(node) - y, dz = nodeZ(node) - z;
                double next = dx * dx + dy * dy + dz * dz;
                if (next < distance) { distance = next; selected = node; }
            }
            return selected;
        }

        private static int nearest(double value, double minimum, double span) {
            return (int) Math.round(coordinate(value, minimum, span));
        }

        public void recordBacking(AirBackingMeter meter) {
            if (!meter.objectOnce(this, 128)) return;
            meter.array(capacity); meter.array(energyReference); meter.array(boundaryWPerK);
            meter.array(first); meter.array(second); meter.array(conductance);
            meter.array(phaseSlots); meter.array(phaseLaws);
            meter.array(inverseCapacity); meter.array(sensibleHalfSecond);
            background.recordBacking(meter);
        }
    }

    private final Layout layout;
    private final double[] energyJ;
    private final byte[] phaseBranches;
    private final boolean[] phaseWaiting;
    private final double airMeanOffsetC;

    /** The caller relinquishes the numerical arrays when this state becomes visible. */
    public MinimumThermalState(Layout layout, double[] energyJ, byte[] phaseBranches, boolean[] phaseWaiting) {
        if (energyJ.length != MAXIMUM_NODES || phaseBranches.length != layout.phaseCount()
                || phaseWaiting.length != phaseBranches.length)
            throw new IllegalArgumentException("Minimum thermal state does not match its layout");
        this.layout = layout; this.energyJ = energyJ;
        this.phaseBranches = phaseBranches; this.phaseWaiting = phaseWaiting;
        double capacity = 0, energy = 0;
        for (int node = 0; node < AIR_NODES; node++) {
            capacity += layout.capacity[node];
            energy += energyJ[node] - layout.energyReference[node] - layout.capacity[node] * layout.background.nodeC(node);
        }
        airMeanOffsetC = capacity > 0 ? energy / capacity : 0;
    }

    public Layout layout() { return layout; }
    public double energyJ(int node) { return energyJ[node]; }
    public MinimumThermalState withTransport(Layout graph) {
        return new MinimumThermalState(layout.withTransport(graph), energyJ, phaseBranches, phaseWaiting);
    }
    public MinimumThermalState withPhaseControl(int index, double enthalpy, byte branch, boolean waiting) {
        double[] next = energyJ.clone(); byte[] branches = phaseBranches.clone(); boolean[] waits = phaseWaiting.clone();
        next[POOLED_NODES + index] = enthalpy; branches[index] = branch; waits[index] = waiting;
        return new MinimumThermalState(layout, next, branches, waits);
    }
    /** External material controls change this body's H without heating its unrelated pool members. */
    public MinimumThermalState withPoolControl(int node, double joules, double capacityChange) {
        if (joules == 0 && capacityChange == 0) return this;
        var energy = energyJ.clone(); energy[node] += joules;
        return new MinimumThermalState(layout.shiftPool(node, joules, capacityChange, nodeTemperatureC(node)), energy, phaseBranches, phaseWaiting);
    }
    public byte phaseBranch(int index) { return phaseBranches[index]; }
    public boolean phaseWaiting(int index) { return phaseWaiting[index]; }
    public double totalEnergyJ() { double total = 0; for (double value : energyJ) total += value; return total; }

    public double nodeTemperatureC(int node) {
        if (node >= POOLED_NODES && node - POOLED_NODES < layout.phaseCount())
            return layout.phaseLaws[node - POOLED_NODES].temperatureC(energyJ[node], phaseBranches[node - POOLED_NODES]);
        return layout.capacity[node] > 0 ? (energyJ[node] - layout.energyReference[node]) / layout.capacity[node]
                : layout.background.nodeC(node) + airMeanOffsetC;
    }

    /** Only the thermal perturbation is interpolated; the caller adds its actual local background. */
    public double temperatureOffsetC(double x, double y, double z) {
        double gx = coordinate(x, layout.minX, layout.spanX), gy = coordinate(y, layout.minY, layout.spanY);
        double gz = coordinate(z, layout.minZ, layout.spanZ);
        int ix = Math.min(2, (int) gx), iy = Math.min(2, (int) gy), iz = Math.min(2, (int) gz);
        double dx = gx - ix, dy = gy - iy, dz = gz - iz, temperature = 0, supported = 0;
        for (int corner = 0; corner < 8; corner++) {
            int node = ix + (corner & 1) + 4 * (iz + (corner >>> 1 & 1)) + 16 * (iy + (corner >>> 2));
            if (!(layout.capacity[node] > 0)) continue;
            double amount = weight(corner, dx, dy, dz);
            supported += amount;
            temperature += amount * (nodeTemperatureC(node) - layout.background.nodeC(node));
        }
        if (supported > 0) return temperature / supported;
        int node = layout.nearestLiveNode(x, y, z, 0, AIR_NODES);
        return node < 0 ? 0 : nodeTemperatureC(node) - layout.background.nodeC(node);
    }

    private static double coordinate(double value, double minimum, double span) {
        return Math.max(0, Math.min(3, (value - minimum) * 3 / span));
    }

    private static double weight(int corner, double x, double y, double z) {
        return ((corner & 1) == 0 ? 1 - x : x) * ((corner & 2) == 0 ? 1 - z : z)
                * ((corner & 4) == 0 ? 1 - y : y);
    }

    public Object[] stateArrays() {
        return new Object[] {energyJ, phaseBranches, phaseWaiting, layout.energyReference, layout.capacity,
                layout.background.airOffsetsC, layout.background.nodesC, layout.background.pages, layout.background.pageNaturalC};
    }
    public void recordGeometryBacking(AirBackingMeter meter) { layout.recordBacking(meter); }
    /** Newly attached immutable transport belongs to solver backing, not per-frame query payload. */
    public long transportBackingBytes() {
        var meter = new AirBackingMeter(); meter.bytes(128);
        meter.array(layout.boundaryWPerK); meter.array(layout.first); meter.array(layout.second);
        meter.array(layout.conductance); meter.array(layout.sensibleHalfSecond);
        return meter.bytes();
    }

    /** A private continuation; its cost is bounded by the descriptor and at most eight time segments. */
    public final class Step {
        private final long fromTick, toTick;
        private final int segments;
        private final double[] inputJ, naturalC, nextEnergy = energyJ.clone();
        private final byte[] nextBranches = phaseBranches.clone();
        private final boolean[] nextWaiting = phaseWaiting.clone();
        private int segment, phase, cursor;
        private int factorCursor;
        private final double smallSeconds, largeSeconds;
        private final double[] smallFactors, largeFactors;
        private double deliveredJ, unacceptedJ;
        private MinimumThermalState result;

        public Step(long fromTick, long toTick, int segments, double[] inputJ, double[] naturalC) {
            if (fromTick > toTick || segments <= 0 || segments > TIME_SEGMENTS
                    || inputJ.length != segments * MAXIMUM_NODES || naturalC.length != MAXIMUM_NODES)
                throw new IllegalArgumentException("Invalid minimum thermal time interval");
            this.fromTick = fromTick; this.toTick = toTick; this.segments = segments;
            this.inputJ = inputJ; this.naturalC = naturalC;
            long ticks = toTick - fromTick, smallTicks = ticks / segments;
            smallSeconds = smallTicks / 40.0;
            largeSeconds = (smallTicks + (ticks % segments == 0 ? 0 : 1)) / 40.0;
            smallFactors = smallSeconds == .5 || smallSeconds == 0 ? layout.sensibleHalfSecond : new double[layout.pairCount()];
            largeFactors = largeSeconds == smallSeconds ? smallFactors : largeSeconds == .5 ? layout.sensibleHalfSecond : new double[layout.pairCount()];
            if (smallFactors != layout.sensibleHalfSecond || largeFactors != layout.sensibleHalfSecond) phase = -1;
        }

        public boolean run(long deadline) {
            if (result != null) return true;
            do {
                if (phase == -1) {
                    int stop = Math.min(layout.pairCount(), factorCursor + 256);
                    for (; factorCursor < stop; factorCursor++) {
                        if (smallFactors != layout.sensibleHalfSecond) smallFactors[factorCursor] = layout.sensibleFactor(factorCursor, smallSeconds);
                        if (largeFactors != smallFactors && largeFactors != layout.sensibleHalfSecond)
                            largeFactors[factorCursor] = layout.sensibleFactor(factorCursor, largeSeconds);
                    }
                    if (factorCursor == layout.pairCount()) phase = 0;
                    continue;
                }
                long begin = fromTick + (toTick - fromTick) * segment / segments;
                long end = fromTick + (toTick - fromTick) * (segment + 1) / segments;
                double halfSeconds = (end - begin) / 40.0;
                int count = phase == 0 || phase == 3 ? MAXIMUM_NODES : halfSeconds == 0 ? 0 : layout.pairCount();
                int stop = Math.min(count, cursor + 256);
                for (; cursor < stop; cursor++) {
                    if (phase == 0 || phase == 3) advanceNode(cursor, halfSeconds,
                            inputJ[segment * MAXIMUM_NODES + cursor] * .5);
                    else {
                        int pair = phase == 2 ? layout.pairCount() - 1 - cursor : cursor;
                        exchange(pair, halfSeconds);
                    }
                }
                if (cursor == count) {
                    cursor = 0;
                    if (++phase == 4) {
                        phase = 0;
                        if (++segment == segments) {
                            result = new MinimumThermalState(layout, nextEnergy, nextBranches, nextWaiting);
                            return true;
                        }
                    }
                }
            } while (System.nanoTime() < deadline);
            return false;
        }

        private double temperature(int node) {
            if (node >= POOLED_NODES)
                return layout.phaseLaws[node - POOLED_NODES].temperatureC(nextEnergy[node], nextBranches[node - POOLED_NODES]);
            return (nextEnergy[node] - layout.energyReference[node]) * layout.inverseCapacity[node];
        }

        private void advanceNode(int node, double seconds, double sourceJ) {
            double capacity = layout.capacity[node];
            if (!(capacity > 0)) { unacceptedJ += sourceJ; return; }
            if (node >= POOLED_NODES) {
                applyPhaseSource(node, sourceJ);
                exchangeWithBath(node, layout.boundaryWPerK[node], naturalC[node], seconds);
                return;
            }
            double g = layout.boundaryWPerK[node];
            if (!(g > 0) || seconds == 0) nextEnergy[node] += sourceJ;
            else {
                double fraction = -Math.expm1(-g * seconds / capacity);
                nextEnergy[node] += capacity * (naturalC[node] - temperature(node)) * fraction
                        + sourceJ * fraction / (g * seconds / capacity);
            }
            deliveredJ += sourceJ;
        }

        private void applyPhaseSource(int node, double joules) {
            int index = node - POOLED_NODES;
            double left = joules;
            var law = layout.phaseLaws[index];
            for (int part = 0; part < 3 && left != 0 && !nextWaiting[index]; part++) {
                double direction = Math.signum(left);
                byte branch = law.selectBranch(nextEnergy[node], nextBranches[index], direction);
                nextBranches[index] = branch;
                double portion = direction * Math.min(Math.abs(left), law.energyLimitJ(nextEnergy[node], branch, direction));
                nextEnergy[node] += portion; left -= portion;
                var transition = law.transition(branch);
                nextWaiting[index] = transition != null && transition.complete(nextEnergy[node]);
                if (portion == 0) break;
            }
            deliveredJ += joules - left; unacceptedJ += left;
        }

        private void exchangeWithBath(int node, double g, double bathC, double seconds) {
            if (!(g > 0) || !(seconds > 0)) return;
            int index = node - POOLED_NODES;
            var law = layout.phaseLaws[index];
            for (int part = 0; part < 4 && seconds > 0 && !nextWaiting[index]; part++) {
                double difference = bathC - temperature(node), direction = Math.signum(difference);
                if (direction == 0) return;
                byte branch = law.selectBranch(nextEnergy[node], nextBranches[index], direction);
                nextBranches[index] = branch;
                double slope = law.slopeKPerJ(nextEnergy[node], branch);
                double requested = slope == 0 ? g * Math.abs(difference) * seconds
                        : Math.abs(difference) * -Math.expm1(-g * slope * seconds) / slope;
                double transfer = Math.min(requested, law.energyLimitJ(nextEnergy[node], branch, direction));
                nextEnergy[node] += direction * transfer;
                var transition = law.transition(branch);
                nextWaiting[index] = transition != null && transition.complete(nextEnergy[node]);
                if (transfer == requested || transfer == 0) return;
                seconds -= elapsed(transfer, Math.abs(difference), g, slope);
            }
        }

        private void exchange(int pair, double seconds) {
            int first = layout.first[pair], second = layout.second[pair]; double g = layout.conductance[pair];
            if (!(g > 0) || !(seconds > 0) || !(layout.capacity[first] > 0 && layout.capacity[second] > 0)) return;
            if (first < POOLED_NODES && second < POOLED_NODES) {
                double factor = (seconds == smallSeconds ? smallFactors : largeFactors)[pair];
                double transfer = perturbationDifference(first, second) * factor;
                nextEnergy[first] += transfer; nextEnergy[second] -= transfer;
                return;
            }
            for (int part = 0; part < 4 && seconds > 0; part++) {
                if (waiting(first) || waiting(second)) return;
                double difference = perturbationDifference(first, second), direction = Math.signum(difference);
                if (direction == 0) return;
                double firstLimit = select(first, direction), secondLimit = select(second, -direction);
                double slope = slope(first) + slope(second);
                double requested = slope == 0 ? g * Math.abs(difference) * seconds
                        : Math.abs(difference) * -Math.expm1(-g * slope * seconds) / slope;
                double transfer = Math.min(requested, Math.min(firstLimit, secondLimit));
                nextEnergy[first] += direction * transfer; nextEnergy[second] -= direction * transfer;
                noteWaiting(first); noteWaiting(second);
                if (transfer == requested || transfer == 0) return;
                seconds -= elapsed(transfer, Math.abs(difference), g, slope);
            }
        }

        private double perturbationDifference(int first, int second) {
            return (temperature(second) - layout.background.nodeC(second)) - (temperature(first) - layout.background.nodeC(first));
        }

        private boolean waiting(int node) { return node >= POOLED_NODES && nextWaiting[node - POOLED_NODES]; }
        private double slope(int node) {
            return node < POOLED_NODES ? 1 / layout.capacity[node]
                    : layout.phaseLaws[node - POOLED_NODES].slopeKPerJ(nextEnergy[node], nextBranches[node - POOLED_NODES]);
        }
        private double select(int node, double direction) {
            if (node < POOLED_NODES) return Double.POSITIVE_INFINITY;
            int index = node - POOLED_NODES;
            var law = layout.phaseLaws[index];
            byte branch = law.selectBranch(nextEnergy[node], nextBranches[index], direction);
            nextBranches[index] = branch;
            return law.energyLimitJ(nextEnergy[node], branch, direction);
        }
        private void noteWaiting(int node) {
            if (node < POOLED_NODES) return;
            int index = node - POOLED_NODES;
            var transition = layout.phaseLaws[index].transition(nextBranches[index]);
            nextWaiting[index] = transition != null && transition.complete(nextEnergy[node]);
        }
        private double elapsed(double transfer, double difference, double g, double slope) {
            return slope == 0 ? transfer / (g * difference)
                    : -Math.log1p(-transfer * slope / difference) / (g * slope);
        }

        public MinimumThermalState result() {
            if (result == null) throw new IllegalStateException("Minimum thermal step is still private");
            return result;
        }
        public double deliveredEnergyJ() { return deliveredJ; }
        public double unacceptedEnergyJ() { return unacceptedJ; }
    }
}
