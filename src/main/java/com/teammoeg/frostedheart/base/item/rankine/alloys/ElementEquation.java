package com.teammoeg.frostedheart.base.item.rankine.alloys;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.List;

public class ElementEquation {

    private static final Codec<List<Integer>> INTEGER_LIST_CODEC = Codec.INT.listOf();
    private static final Codec<List<Float>> FLOAT_LIST_CODEC = Codec.FLOAT.listOf();
    private static final Codec<List<String>> STRING_LIST_CODEC = Codec.STRING.listOf();
    private static final Codec<FormulaModifier> FORMULA_MODIFIER_CODEC = Codec.STRING.comapFlatMap((s) -> {
        try {
            return DataResult.success(FormulaModifier.valueOf(s));
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Invalid Formula Modifier " + s + ": " + illegalargumentexception.getMessage());
        }
    }, FormulaModifier::toString);
    private static final Codec<FormulaType> FORMULA_TYPE_CODEC = Codec.STRING.comapFlatMap((s) -> {
        try {
            return DataResult.success(FormulaType.valueOf(s));
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Invalid Formula Type " + s + ": " + illegalargumentexception.getMessage());
        }
    }, FormulaType::toString);

    private static final Codec<List<FormulaModifier>> FORMULA_MODIFIER_LIST_CODEC = FORMULA_MODIFIER_CODEC.listOf();
    private static final Codec<List<FormulaType>> FORMULA_TYPE_LIST_CODEC = FORMULA_TYPE_CODEC.listOf();

    public static final Codec<ElementEquation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    INTEGER_LIST_CODEC.fieldOf("breaks").forGetter(l -> l.breaks),
                    FORMULA_TYPE_LIST_CODEC.fieldOf("formulas").forGetter(l -> l.formulaTypes),
                    FLOAT_LIST_CODEC.fieldOf("a").forGetter(l -> l.a),
                    FLOAT_LIST_CODEC.fieldOf("b").forGetter(l -> l.b),
                    FORMULA_MODIFIER_LIST_CODEC.fieldOf("modifiers").forGetter(l -> l.formulaModifiers),
                    FLOAT_LIST_CODEC.fieldOf("limit").forGetter(l -> l.limit)
            ).apply(instance, ElementEquation::new));
    List<Integer> breaks;
    List<FormulaType> formulaTypes;
    List<FormulaModifier> formulaModifiers;
    List<Float> a;
    List<Float> b;
    List<Float> limit;
    public ElementEquation(List<Integer> breaksIn, List<FormulaType> formulaTypesIn, List<Float> aIn, List<Float> bIn, List<FormulaModifier> formulaModifiersIn, List<Float> limitIn) {
        this.breaks = breaksIn;
        this.formulaTypes = formulaTypesIn;
        this.a = aIn;
        this.b = bIn;
        this.formulaModifiers = formulaModifiersIn;
        this.limit = limitIn;
    }

    public ElementEquation() {
        this(Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
    }

    public float calculateFloat(int x) {
        int index = 0;
        for (int br : breaks) {
            if (x <= br) {
                return applyFormulaModifier(constructFormula(x,index),index);
            }
            index += 1;
        }
        return 0;
    }


    public int calculateRounded(int x) {
        int index = 0;
        for (int br : breaks) {
            if (x <= br) {
                return Math.round(applyFormulaModifier(constructFormula(x,index),index));
            }
            index += 1;
        }
        return 0;
    }

    public float applyFormulaModifier(float output, int index) {
        switch (formulaModifiers.get(index)) {
            case ABSOLUTE_VALUE:
                return Math.abs(output);
            case CEILING:
                return (float) Math.ceil(output);
            case FLOOR:
                return (float) Math.floor(output);
            case MAX:
                return Math.max(output, limit.get(index));
            case MIN:
                return Math.min(output, limit.get(index));
            default:
            case NONE:
                return output;
        }
    }

    public float constructFormula(int x, int index) {
        float percent = x;
        if (formulaModifiers.get(index).equals(FormulaModifier.MULTIPLIER)) {
            percent = x * limit.get(index);
        } else if (formulaModifiers.get(index).equals(FormulaModifier.ADDITION)) {
            percent = x + limit.get(index);
        }
        int modulo = 2;
        switch (formulaTypes.get(index)) {
            case LINEAR:
            default:
                return a.get(index) * percent + b.get(index);
            case POWER:
                return (float) (Math.pow(percent, a.get(index)) + b.get(index));
            case EXPONENTIAL:
                return (float) (Math.pow(a.get(index),percent) + b.get(index));
            case LOGARITHMIC:
                return (float) (a.get(index) *Math.log(percent) + b.get(index));
            case LOG10:
                return (float) (a.get(index) *Math.log10(percent) + b.get(index));
            case QUADRATIC:
                return (float) (a.get(index) *Math.pow(percent,2) + b.get(index) *percent);
            case SIN:
                return (float) (a.get(index) *Math.sin(b.get(index) *percent));
            case COS:
                return (float) (a.get(index) *Math.cos(b.get(index) *percent));
            case ALTERNATING:
                if (formulaModifiers.get(index).equals(FormulaModifier.ALTERNATING_MODULO)) {
                    modulo = Math.round(limit.get(index));
                }
                if (x % modulo == 0) {
                    return a.get(index) * percent;
                } else {
                    return b.get(index) * percent;
                }
            case CONSTANT:
                return (a.get(index));
            case CONSTANT_ALTERNATING:
                if (formulaModifiers.get(index).equals(FormulaModifier.ALTERNATING_MODULO)) {
                    modulo = Math.round(limit.get(index));
                }
                if (x % modulo == 0) {
                    return a.get(index);
                } else {
                    return b.get(index);
                }
        }

    }

    public boolean isEmpty() {
        return breaks.size() == 0;
    }

    public List<Integer> getBreaks() {
        return breaks;
    }

    public List<FormulaType> getFormulaTypes() {
        return formulaTypes;
    }

    public List<Float> getA() {
        return a;
    }

    public List<Float> getB() {
        return b;
    }

    public List<FormulaModifier> getFormulaModifiers() {
        return formulaModifiers;
    }

    public List<Float> getLimit() {
        return limit;
    }

    public enum FormulaType {
        LINEAR,
        POWER,
        EXPONENTIAL,
        LOGARITHMIC,
        LOG10,
        QUADRATIC,
        SIN,
        COS,
        ALTERNATING,
        CONSTANT,
        CONSTANT_ALTERNATING

    }

    public enum FormulaModifier {
        ABSOLUTE_VALUE,
        FLOOR,
        CEILING,
        MAX,
        MIN,
        ADDITION,
        MULTIPLIER,
        ALTERNATING_MODULO,
        NONE
    }
}
