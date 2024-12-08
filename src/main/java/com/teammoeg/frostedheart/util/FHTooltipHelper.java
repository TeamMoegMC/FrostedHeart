package com.teammoeg.frostedheart.util;

public class FHTooltipHelper {
    public static String makeProgressBar(int length, int filledLength) {
        String bar = " ";
        int emptySpaces = length - filledLength;
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        return bar;
    }

    // make reversed progress bar, filling from right to left
    public static String makeProgressBarReversed(int length, int filledLength) {
        String bar = " ";
        int emptySpaces = length - filledLength;
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        return bar;
    }
}
