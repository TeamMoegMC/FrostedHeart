package com.teammoeg.frostedheart.util;

public interface VariantProvider {
    Double get(String k);

    default double getOrDefault(String k, double def) {
        Double d = get(k);
        if (d == null)
            return def;
        return d;
    }

    ;
}
