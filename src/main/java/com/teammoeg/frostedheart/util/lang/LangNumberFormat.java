package com.teammoeg.frostedheart.util.lang;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.text.NumberFormat;
import java.util.Locale;

public class LangNumberFormat {

    private NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
    public static com.simibubi.create.foundation.utility.LangNumberFormat numberFormat = new com.simibubi.create.foundation.utility.LangNumberFormat();

    public NumberFormat get() {
        return format;
    }

    public void update() {
        format = NumberFormat.getInstance(Minecraft.getInstance()
                .getLanguageManager()
                .getJavaLocale());
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        format.setGroupingUsed(true);
    }

    public static String format(double d) {
        if (Mth.equal(d, 0))
            d = 0;
        return numberFormat.get()
                .format(d)
                .replace("\u00A0", " ");
    }

}
