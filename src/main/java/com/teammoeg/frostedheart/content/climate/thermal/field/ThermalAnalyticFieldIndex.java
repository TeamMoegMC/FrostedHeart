/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.field;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Main-thread ordered non-conservative analytic field authority. */
public final class ThermalAnalyticFieldIndex {
    private static final Comparator<ThermalAnalyticField> ORDER = Comparator
            .comparingInt((ThermalAnalyticField field) -> field.combineMode().ordinal())
            .thenComparingInt(ThermalAnalyticField::priority)
            .thenComparing(ThermalAnalyticField::key);
    private final Object2ObjectOpenHashMap<ThermalFieldKey, Entry> byKey = new Object2ObjectOpenHashMap<>();
    private final ArrayList<Entry> fields = new ArrayList<>();

    public boolean isEmpty() { return fields.isEmpty(); }

    public void collectIntersecting(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, List<ThermalAnalyticField> out) {
        out.clear();
        for (int i = 0; i < fields.size(); i++) {
            ThermalAnalyticField field = fields.get(i).field;
            if (field.intersects(minX, minY, minZ, maxX, maxY, maxZ)) out.add(field);
        }
    }

    private static final class Entry {
        ThermalAnalyticField field;
        boolean seen = true;

        Entry(ThermalAnalyticField field) {
            this.field = field;
        }
    }

    public void upsert(ThermalAnalyticField field) {
        Entry entry = byKey.get(field.key());
        if (entry == null) {
            entry = new Entry(field);
            byKey.put(field.key(), entry);
        } else {
            entry.seen = true;
            if (entry.field.combineMode() == field.combineMode() && entry.field.priority() == field.priority()) {
                entry.field = field;
                return;
            }
            fields.remove(entry);
            entry.field = field;
        }
        int low = 0;
        int high = fields.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ORDER.compare(fields.get(middle).field, field) < 0) low = middle + 1;
            else high = middle;
        }
        fields.add(low, entry);
    }

    public void upsertSphere(ThermalFieldKey key, int priority, ThermalAnalyticField.CombineMode mode,
            double x, double y, double z, double radius, double value) {
        Entry entry = byKey.get(key);
        if (entry != null && entry.field.matches(priority, mode, ThermalAnalyticField.Shape.SPHERE,
                x, y, z, radius, radius, radius, value)) {
            entry.seen = true;
            return;
        }
        upsert(new ThermalAnalyticField(key, priority, mode, x, y, z, radius, value));
    }

    public boolean remove(ThermalFieldKey key) {
        Entry entry = byKey.remove(key);
        return entry != null && fields.remove(entry);
    }

    public void beginProviderRefresh(ResourceLocation provider) {
        for (int i = 0; i < fields.size(); i++) {
            Entry entry = fields.get(i);
            if (entry.field.key().provider().equals(provider)) entry.seen = false;
        }
    }

    /** Call only after completing the provider's authoritative enumeration. */
    public void endProviderRefresh(ResourceLocation provider) {
        int retained = 0;
        for (int i = 0; i < fields.size(); i++) {
            Entry entry = fields.get(i);
            if (!entry.seen && entry.field.key().provider().equals(provider)) {
                byKey.remove(entry.field.key());
            } else {
                if (retained != i) fields.set(retained, entry);
                retained++;
            }
        }
        if (retained < fields.size()) fields.subList(retained, fields.size()).clear();
    }

    public double compose(double x, double y, double z, double natural, double base) {
        double result = base;
        for (int i = 0; i < fields.size(); i++) {
            ThermalAnalyticField field = fields.get(i).field;
            if (!field.contains(x, y, z)) continue;
            result = switch (field.combineMode()) {
                case FLOOR_FROM_NATURAL -> Math.max(result, natural + field.temperatureC());
                case OVERRIDE -> field.temperatureC();
                case MAX_HEAT -> Math.max(result, field.temperatureC());
                case MIN_COOL -> Math.min(result, field.temperatureC());
                case ADD_DELTA -> result + field.temperatureC();
            };
        }
        return result;
    }

    public void sample(double x, double y, double z, Sample out) {
        out.clear();
        for (int i = 0; i < fields.size(); i++) {
            ThermalAnalyticField field = fields.get(i).field;
            if (!field.contains(x, y, z)) continue;
            out.include(field);
        }
    }

    public boolean appliesAt(double x, double y, double z) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).field.contains(x, y, z)) return true;
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
        for (int i = 0; i < fields.size(); i++) {
            ThermalAnalyticField field = fields.get(i).field;
            if (field.contains(x, y, z)) {
                result.add(field);
            }
        }
        return result;
    }

    /** Reusable reductions for a local natural query or several phase thresholds. */
    public static final class Sample {
        private boolean present;
        private boolean hasOverride;
        private double relativeFloor = Double.NEGATIVE_INFINITY;
        private double override;
        private double floor = Double.NEGATIVE_INFINITY;
        private double ceiling = Double.POSITIVE_INFINITY;
        private double delta;

        public void clear() {
            present = false;
            hasOverride = false;
            relativeFloor = Double.NEGATIVE_INFINITY;
            floor = Double.NEGATIVE_INFINITY;
            ceiling = Double.POSITIVE_INFINITY;
            delta = 0;
        }

        public boolean present() { return present; }
        public boolean requiresNatural() { return !hasOverride && relativeFloor != Double.NEGATIVE_INFINITY; }
        public boolean requiresBase() { return !hasOverride; }

        /** Fields must be included in the index's canonical order. */
        public void include(ThermalAnalyticField field) {
            present = true;
            double value = field.temperatureC();
            switch (field.combineMode()) {
                case FLOOR_FROM_NATURAL -> relativeFloor = Math.max(relativeFloor, value);
                case OVERRIDE -> { override = value; hasOverride = true; }
                case MAX_HEAT -> floor = Math.max(floor, value);
                case MIN_COOL -> ceiling = Math.min(ceiling, value);
                case ADD_DELTA -> delta += value;
            }
        }

        public double compose(double natural, double base) {
            double result = requiresNatural() ? Math.max(base, natural + relativeFloor) : base;
            if (hasOverride) result = override;
            return Math.min(Math.max(result, floor), ceiling) + delta;
        }

        public double guaranteedFloor(double natural) {
            return compose(natural, Double.NEGATIVE_INFINITY);
        }
    }
}
