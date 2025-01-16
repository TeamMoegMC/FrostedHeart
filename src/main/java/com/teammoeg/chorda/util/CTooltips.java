package com.teammoeg.chorda.util;

public class CTooltips {
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

    // make progress bar, filling from left to right, but with a starting offset, left of which is not filled
    public static String makeProgressBarOffset(int length, int filledLength, int offset) {
        String bar = "";
        int emptySpaces = length - filledLength - offset;
        for (int i = 0; i < offset; i++)
            bar += "\u2592";
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        return bar;
    }

    // use the method above to take two interval bounds [min, max] and a bar length, and fill the bar accordingly
    /**
     * Make a progress bar with a given length, filled from left to right, with interval bounds [min, max]
     * @param length should be greater than max
     * @param min the lower bound of the interval
     * @param max the upper bound of the interval
     * @return a string representing the progress bar
     */
    public static String makeProgressBarInterval(int length, int min, int max) {
        return makeProgressBarOffset(length, max - min, min);
    }
}
