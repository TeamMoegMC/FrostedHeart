/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

/**
 * Shared material energy law, referenced by nodes rather than copied into them.
 * Enthalpy is in joules relative to 0 degrees Celsius; capacity is in J/K.
 * A node owns only its enthalpy and, during a transition, the selected branch.
 */
public record MaterialThermalLaw(
        double capacityJPerK,
        double offsetJ,
        Transition heating,
        Transition cooling
) {
    public static final byte SENSIBLE = 0;
    public static final byte HEATING = 1;
    public static final byte COOLING = -1;

    public MaterialThermalLaw {
        if (!Double.isFinite(capacityJPerK) || capacityJPerK <= 0
                || !Double.isFinite(offsetJ)) {
            throw new IllegalArgumentException("invalid material energy law");
        }
    }

    public static MaterialThermalLaw sensible(double capacityJPerK) {
        return new MaterialThermalLaw(capacityJPerK, 0, null, null);
    }

    public double enthalpyAtTemperature(double temperatureC) {
        return offsetJ + capacityJPerK * temperatureC;
    }

    /** Added matter arrives at ambient; removal scales thermal excess into the target energy reference. */
    public double afterMassChange(double energyJ, MaterialThermalLaw target, double ambientC) {
        return target.capacityJPerK >= capacityJPerK
                ? energyJ + target.enthalpyAtTemperature(ambientC) - enthalpyAtTemperature(ambientC)
                : target.offsetJ + (energyJ - offsetJ) * target.capacityJPerK / capacityJPerK;
    }

    public Transition transition(byte branch) {
        return branch == HEATING ? heating : branch == COOLING ? cooling : null;
    }

    /** Retain latent progress when reversing; leave the plateau at its source end. */
    public byte selectBranch(double energyJ, byte branch, double direction) {
        Transition active = transition(branch);
        if (active != null && ((direction > 0) == active.heating()
                || (active.sourceEnthalpyJ() - energyJ) * Math.signum(direction) > 0)) return branch;
        byte next = direction > 0 ? HEATING : COOLING;
        Transition edge = transition(next);
        return edge != null && (edge.sourceEnthalpyJ() - energyJ) * Math.signum(direction) <= 0
                ? next : SENSIBLE;
    }

    /** Energy distance to the next sensible/latent boundary, after selecting a branch. */
    public double energyLimitJ(double energyJ, byte branch, double direction) {
        Transition active = transition(branch);
        if (active != null) {
            double end = (direction > 0) == active.heating()
                    ? active.targetEnthalpyJ() : active.sourceEnthalpyJ();
            return Math.max(0, (end - energyJ) * Math.signum(direction));
        }
        Transition edge = direction > 0 ? heating : cooling;
        return edge == null ? Double.POSITIVE_INFINITY
                : Math.max(0, (edge.sourceEnthalpyJ() - energyJ) * Math.signum(direction));
    }

    public double temperatureC(double enthalpyJ, byte branch) {
        Transition edge = transition(branch);
        if (edge != null && edge.contains(enthalpyJ)) {
            return edge.temperatureC();
        }
        return (enthalpyJ - offsetJ) / capacityJPerK;
    }

    /** A plateau has zero dT/dH, not zero capacity. */
    public double slopeKPerJ(double enthalpyJ, byte branch) {
        Transition edge = transition(branch);
        return edge != null && edge.contains(enthalpyJ) ? 0 : 1 / capacityJPerK;
    }

    /**
     * Target state IDs belong to the immutable Minecraft state cut, not a
     * serialized identity. Persistence writes the corresponding BlockState.
     * Both endpoints use the same energy reference, including latent energy.
     */
    public record Transition(
            int targetStateId,
            double temperatureC,
            double sourceEnthalpyJ,
            double targetEnthalpyJ
    ) {
        public Transition {
            if (targetStateId < 0 || !Double.isFinite(temperatureC)
                    || !Double.isFinite(sourceEnthalpyJ)
                    || !Double.isFinite(targetEnthalpyJ)
                    || sourceEnthalpyJ == targetEnthalpyJ) {
                throw new IllegalArgumentException("invalid material transition");
            }
        }

        public boolean contains(double enthalpyJ) {
            return enthalpyJ >= Math.min(sourceEnthalpyJ, targetEnthalpyJ)
                    && enthalpyJ <= Math.max(sourceEnthalpyJ, targetEnthalpyJ);
        }

        public double progress(double enthalpyJ) {
            return Math.max(0, Math.min(1,
                    (enthalpyJ - sourceEnthalpyJ) / (targetEnthalpyJ - sourceEnthalpyJ)));
        }

        public boolean heating() {
            return targetEnthalpyJ > sourceEnthalpyJ;
        }

        public boolean complete(double enthalpyJ) {
            return heating() ? enthalpyJ >= targetEnthalpyJ : enthalpyJ <= targetEnthalpyJ;
        }
    }
}
