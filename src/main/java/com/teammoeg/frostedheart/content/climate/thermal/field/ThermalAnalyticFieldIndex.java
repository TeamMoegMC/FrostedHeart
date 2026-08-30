/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.field;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Main-thread ordered non-conservative analytic field authority. */
public final class ThermalAnalyticFieldIndex {
    private static final Comparator<ThermalAnalyticField> ORDER =
            Comparator.comparingInt(
                            (ThermalAnalyticField field) ->
                                    field.combineMode().ordinal())
                    .thenComparingInt(
                            ThermalAnalyticField::priority)
                    .thenComparingLong(
                            ThermalAnalyticField::fieldId);
    private final ArrayList<ThermalAnalyticField> fields =
            new ArrayList<>();

    public void upsert(ThermalAnalyticField field) {
        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).fieldId() == field.fieldId()) {
                fields.set(index, field);
                fields.sort(ORDER);
                return;
            }
        }
        fields.add(field);
        fields.sort(ORDER);
    }

    public boolean remove(long fieldId) {
        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).fieldId() == fieldId) {
                fields.remove(index);
                return true;
            }
        }
        return false;
    }

    public double compose(double x, double y, double z, double base) {
        double result = base;
        for (ThermalAnalyticField field : fields) {
            if (!field.contains(x, y, z)) {
                continue;
            }
            result = switch (field.combineMode()) {
                case OVERRIDE -> field.temperatureC();
                case MAX_HEAT -> Math.max(result, field.temperatureC());
                case MIN_COOL -> Math.min(result, field.temperatureC());
                case ADD_DELTA -> result + field.temperatureC();
            };
        }
        return result;
    }

    public boolean appliesAt(double x, double y, double z) {
        for (ThermalAnalyticField field : fields) {
            if (field.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public List<ThermalAnalyticField> fieldsAt(
            double x,
            double y,
            double z
    ) {
        ArrayList<ThermalAnalyticField> result =
                new ArrayList<>();
        for (ThermalAnalyticField field : fields) {
            if (field.contains(x, y, z)) {
                result.add(field);
            }
        }
        return result;
    }

    public int appendInfrared(
            float[] output,
            int count,
            int maximum,
            double minimumX,
            double maximumX,
            double minimumZ,
            double maximumZ
    ) {
        for (ThermalAnalyticField field : fields) {
            if (count >= maximum) {
                break;
            }
            if (field.intersectsHorizontalBounds(
                    minimumX, maximumX, minimumZ, maximumZ)) {
                field.writeInfrared(output, count++ * 8);
            }
        }
        return count;
    }

}
