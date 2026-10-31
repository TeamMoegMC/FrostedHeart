/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * All faces of a material body share H. Air contact and internal conduction
 * use private, symmetric pair sweeps without writing the arena.
 */
final class LocalMaterialBoundary {
    final boolean[] waiting;
    final boolean[] trialWaiting;
    final double[] stepH;
    private final int airCount;
    private final int[] slots;
    private final MaterialThermalLaw[] laws;
    private final int[] contactBodies;
    private final int surfaceContactCount;
    private final int[] internalFirst, internalSecond;
    private final double[] internalConductance;
    private final CoupledThermalOperator operator;
    private long internalNanos, pairUpdates;

    /** Overload-only face-average exchange; ordinary fine faces retain four Gauss points. */
    static int surfacePoints(AirFieldLayout layout) {
        return (layout.aggregationSpan() == 4) ? 1 : 4;
    }

    record DiagnosticCost(int bodies, int edges, long nanos, long updates) { }
    DiagnosticCost diagnosticCost() { return new DiagnosticCost(stepH.length, internalFirst.length, internalNanos, pairUpdates); }
    /** The formal pairwise path needs no implicit rows or reverse surface graph. */
    static final class Preparation {
        final AirFieldLayout layout;
        final int[] slots;
        final Int2IntOpenHashMap unknownBySlot;
        final ThermalCellArena arena;
        final CoupledThermalOperator.Contacts contacts;
        final CoupledThermalOperator operator;
        final ThermalFragment[] fragments;
        final MaterialThermalLaw[] laws;
        final int[] contactBodies;
        int[] first, second;
        double[] conductance;
        int phase, cursor, fragment, edge, count, surfaceCount;
        LocalMaterialBoundary result;
        Preparation(AirFieldLayout layout, int[] slots, Int2IntOpenHashMap unknownBySlot, ThermalCellArena arena,
                CoupledThermalOperator.Contacts contacts, CoupledThermalOperator operator, ThermalFragment[] fragments) {
            this.layout = layout; this.slots = slots; this.unknownBySlot = unknownBySlot; this.arena = arena;
            this.contacts = contacts; this.operator = operator; this.fragments = fragments;
            laws = new MaterialThermalLaw[slots.length]; contactBodies = new int[contacts.localBodies.size()];
            surfaceCount = operator.surfaceContactCount();
        }
        boolean run(long deadline) {
            do {
                if (phase == 0) {
                    int end = Math.min(slots.length, cursor + 256);
                    for (; cursor < end; cursor++) laws[cursor] = arena.materialLaw(slots[cursor]);
                    if (cursor == slots.length) { phase++; cursor = 0; }
                } else if (phase == 1) {
                    int end = Math.min(contactBodies.length, cursor + 1024);
                    for (; cursor < end; cursor++) { int body = contactBodies[cursor] = contacts.localBodies.getInt(cursor); if (body >= 0) surfaceCount++; }
                    if (cursor == contactBodies.length) { phase++; cursor = 0; }
                } else if (phase == 2 || phase == 3) {
                    int remaining = 1024;
                    while (remaining-- > 0 && fragment < fragments.length) {
                        var item = fragments[fragment].materialContacts();
                        if (edge == item.size()) { fragment++; edge = 0; continue; }
                        int a = unknownBySlot.get(item.first(edge)) - layout.coefficientCount();
                        int b = unknownBySlot.get(item.second(edge)) - layout.coefficientCount();
                        if (a >= 0 && b >= 0) {
                            if (phase == 2) count++;
                            else { first[cursor] = a; second[cursor] = b; conductance[cursor++] = item.conductance(edge); }
                        }
                        edge++;
                    }
                    if (fragment == fragments.length) {
                        if (phase == 2) { first = new int[count]; second = new int[count]; conductance = new double[count]; }
                        phase++; cursor = fragment = edge = 0;
                    }
                } else { result = new LocalMaterialBoundary(this); return true; }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }
    private LocalMaterialBoundary(Preparation prepared) {
        airCount = prepared.layout.coefficientCount(); slots = prepared.slots;
        operator = prepared.operator; laws = prepared.laws;
        waiting = new boolean[slots.length]; trialWaiting = new boolean[slots.length]; stepH = new double[slots.length];
        contactBodies = prepared.contactBodies;
        surfaceContactCount = prepared.surfaceCount;
        internalFirst = prepared.first; internalSecond = prepared.second; internalConductance = prepared.conductance;
    }

    void resetMetrics() { internalNanos = pairUpdates = 0; }

    MaterialThermalLaw law(int body) { return laws[body]; }

    boolean contactWaiting(int contact) {
        int body = contactBody(contact);
        return body >= 0 && trialWaiting[body];
    }

    int contactBody(int contact) {
        return contact < operator.surfaceContactCount() ? operator.surfaceBody(contact)
                : contactBodies[contact - operator.surfaceContactCount()];
    }

    /** Private source kick, preserving branch boundaries and once-only accepted source energy. */
    double addPairwisePower(int body, double[] h, byte[] branches, double energy) {
        double remaining = energy;
        MaterialThermalLaw law = laws[body];
        for (int segment = 0; segment < 6 && remaining != 0 && !trialWaiting[body]; segment++) {
            double direction = Math.signum(remaining);
            branches[body] = law.selectBranch(h[body], branches[body], direction);
            double accepted = direction * Math.min(Math.abs(remaining), law.energyLimitJ(h[body], branches[body], direction));
            h[body] += accepted; remaining -= accepted;
            var transition = law.transition(branches[body]);
            trialWaiting[body] |= transition != null && transition.complete(h[body]);
            if (accepted == 0) break;
        }
        return energy - remaining;
    }

    /** Air trace and material form a finite-capacity pair during each law segment. */
    void exchangeAirContact(int contact, double[] temperatures, double[] h, byte[] branches,
            double duration, double referenceC) {
        int body = contactBody(contact);
        if (trialWaiting[body]) return;
        MaterialThermalLaw law = laws[body];
        double g = operator.activeContactConductance(contact), inverseAir = 0;
        for (int entry = operator.contactStart(contact); entry < operator.contactEnd(contact); entry++) {
            double weight = operator.contactWeight(entry);
            inverseAir += weight * weight / operator.massJPerK[operator.contactRow(entry)];
        }
        double remaining = duration;
        for (int segment = 0; segment < 6 && remaining > 0; segment++) {
            double trace = 0;
            for (int entry = operator.contactStart(contact); entry < operator.contactEnd(contact); entry++)
                trace += operator.contactWeight(entry) * temperatures[operator.contactRow(entry)];
            double delta = trace - (law.temperatureC(h[body], branches[body]) - referenceC);
            if (delta == 0) return;
            double direction = Math.signum(delta);
            branches[body] = law.selectBranch(h[body], branches[body], direction);
            delta = trace - (law.temperatureC(h[body], branches[body]) - referenceC);
            if (delta == 0 || Math.signum(delta) != direction) return;
            double limit = law.energyLimitJ(h[body], branches[body], direction);
            if (limit == 0) return;
            double inverse = inverseAir + law.slopeKPerJ(h[body], branches[body]);
            double requested = Math.abs(delta) * -Math.expm1(-g * inverse * remaining) / inverse;
            double transferred = Math.min(requested, limit), signed = direction * transferred;
            h[body] += signed;
            for (int entry = operator.contactStart(contact); entry < operator.contactEnd(contact); entry++) {
                int row = operator.contactRow(entry);
                temperatures[row] -= operator.contactWeight(entry) * signed / operator.massJPerK[row];
            }
            pairUpdates++;
            var transition = law.transition(branches[body]);
            trialWaiting[body] |= transition != null && transition.complete(h[body]);
            if (transferred == requested || trialWaiting[body]) return;
            double elapsed = -Math.log1p(-transferred * inverse / Math.abs(delta)) / (g * inverse);
            remaining = Math.max(0, remaining - elapsed);
        }
    }

    void copyStepState(double[] enthalpyJ, byte[] acceptedBranches, byte[] trialBranches, int from, int to) {
        System.arraycopy(enthalpyJ, from, stepH, from, to - from);
        System.arraycopy(acceptedBranches, from, trialBranches, from, to - from);
        System.arraycopy(waiting, from, trialWaiting, from, to - from);
    }

    int internalPairCount() { return internalFirst.length; }
    int internalPairFirst(int pair) {
        return internalFirst[pair];
    }
    int internalPairSecond(int pair) {
        return internalSecond[pair];
    }
    double internalPairConductance(int pair) {
        return internalConductance[pair];
    }

    /** Retain the compiled material-edge order in both directions of the symmetric sweep. */
    void advanceInternalRange(double[] enthalpyJ, byte[] branches, double duration, boolean reverse, int from, int to) {
        long started = System.nanoTime();
        int internalEnd = Math.min(to, internalFirst.length);
        for (int ordinal = from; ordinal < internalEnd; ordinal++) {
            int edge = reverse ? internalFirst.length - 1 - ordinal : ordinal;
            exchange(enthalpyJ, branches, internalFirst[edge], internalSecond[edge], internalConductance[edge], duration);
        }
        internalNanos += System.nanoTime() - started;
    }

    private void exchange(double[] h, byte[] branches, int first, int second, double g, double duration) {
        if (g == 0 || trialWaiting[first] || trialWaiting[second]) return;
        MaterialThermalLaw a = laws[first], b = laws[second];
        double remaining = duration;
        for (int segment = 0; segment < 6 && remaining > 0; segment++) {
            double delta = a.temperatureC(h[first], branches[first]) - b.temperatureC(h[second], branches[second]);
            if (delta == 0) return;
            double direction = Math.signum(delta);
            branches[first] = a.selectBranch(h[first], branches[first], -direction);
            branches[second] = b.selectBranch(h[second], branches[second], direction);
            double limit = Math.min(a.energyLimitJ(h[first], branches[first], -direction), b.energyLimitJ(h[second], branches[second], direction));
            if (limit == 0) return;
            delta = a.temperatureC(h[first], branches[first]) - b.temperatureC(h[second], branches[second]);
            if (delta == 0 || Math.signum(delta) != direction) return;
            double slope = a.slopeKPerJ(h[first], branches[first]) + b.slopeKPerJ(h[second], branches[second]);
            double requested = slope == 0 ? Math.abs(delta) * g * remaining
                    : Math.abs(delta) / slope * -Math.expm1(-g * slope * remaining);
            double transferred = Math.min(requested, limit);
            if (transferred == 0) return;
            h[first] -= direction * transferred;
            h[second] += direction * transferred;
            pairUpdates++;
            var firstTransition = a.transition(branches[first]);
            var secondTransition = b.transition(branches[second]);
            trialWaiting[first] |= firstTransition != null && firstTransition.complete(h[first]);
            trialWaiting[second] |= secondTransition != null && secondTransition.complete(h[second]);
            if (transferred == requested || trialWaiting[first] || trialWaiting[second]) return;
            double elapsed = slope == 0 ? transferred / (g * Math.abs(delta))
                    : -Math.log1p(-transferred * slope / Math.abs(delta)) / (g * slope);
            remaining = Math.max(0, remaining - elapsed);
        }
    }

    String describe() {
        return "materialTrial=local,airSolveUnknowns=" + airCount + ",localMaterials=" + slots.length
                + ",surfaceContacts=" + surfaceContactCount
                + ",materialInternalUs=" + internalNanos / 1000
                + ",materialPairUpdates=" + pairUpdates;
    }

    /** Retained arrays only, with the same alignment/header convention as AirBackingMeter. */
    private static long arrayBytes(int length, int width) { return (16L + (long) length * width + 7L) & ~7L; }

    String preparationSummary() {
        return "materialSurfaceContacts=" + surfaceContactCount
                + ",materialInternalEdges=" + internalFirst.length
                + ",materialInternalArrayBytes=" + (arrayBytes(internalFirst.length, 4) * 2 + arrayBytes(internalConductance.length, 8));
    }

    void recordBacking(AirBackingMeter meter) {
        meter.array(slots); meter.array(laws);
        meter.array(waiting); meter.array(trialWaiting); meter.array(stepH);
        meter.array(contactBodies);
        meter.array(internalFirst); meter.array(internalSecond); meter.array(internalConductance);
    }
}
