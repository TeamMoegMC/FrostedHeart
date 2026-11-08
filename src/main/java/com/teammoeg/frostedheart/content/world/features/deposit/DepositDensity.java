package com.teammoeg.frostedheart.content.world.features.deposit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/** A bounded local field; regions outside its influence return the original density exactly. */
public record DepositDensity(DensityFunction input, DensityFunction regions, DensityFunction shape, String field) implements DensityFunction {
    public static final KeyDispatchDataCodec<DepositDensity> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(DepositDensity::input),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("regions").forGetter(DepositDensity::regions),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("shape", DensityFunctions.zero()).forGetter(DepositDensity::shape),
            Codec.STRING.fieldOf("field").forGetter(DepositDensity::field)
    ).apply(i, DepositDensity::new)));
    @Override public double compute(FunctionContext context) {
        var source = DepositRegions.resolve(regions);
        boolean cavity = field.equals("cavity");
        if (source == null || !source.affectsHeight(context.blockY(), -36, 60))
            return cavity ? .8 : input.compute(context);
        var plan = source.layout(context.blockX(), context.blockZ());
        if (plan == null || Math.abs((long) context.blockX() - plan.center().getX()) > 144
                || Math.abs((long) context.blockZ() - plan.center().getZ()) > 144
                || context.blockY() < plan.center().getY() - 36 || context.blockY() > plan.center().getY() + 60)
            return cavity ? .8 : input.compute(context);
        var column = plan.column(context.blockX(), context.blockZ());
        if (cavity) return column.density(context.blockY());
        boolean aquifer = field.equals("floodedness");
        double weight = aquifer ? column.aquiferWeight(context.blockY()) : column.terrainWeight(context.blockY());
        if (weight <= 0) return input.compute(context);
        double local = aquifer ? -1 : shape.compute(context);
        return weight >= 1 ? local : input.compute(context) * (1 - weight) + local * weight;
    }
    @Override public void fillArray(double[] values, ContextProvider provider) { provider.fillAllDirectly(values, this); }
    @Override public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new DepositDensity(input.mapAll(visitor), regions.mapAll(visitor), shape.mapAll(visitor), field));
    }
    @Override public double minValue() { return Math.min(input.minValue(), -4); }
    @Override public double maxValue() { return Math.max(input.maxValue(), .8); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
