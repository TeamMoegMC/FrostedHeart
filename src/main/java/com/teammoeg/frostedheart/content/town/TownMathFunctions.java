package com.teammoeg.frostedheart.content.town;

public class TownMathFunctions {
    /** The temperature at which the house is comfortable. */
    public static final double COMFORTABLE_TEMP_HOUSE = 24;
    public static final int MAX_TEMP_HOUSE = 50;
    public static final int MIN_TEMP_HOUSE = (Town.DEBUG_MODE ? -50 : 0);

    /**
     * 用于调整数据。
     * <br>
     * 1-exp型，在x大概为20时，数值达到一半
     */
    public static double CalculatingFunction1(double num){
        if(num <= 0){
            return 0;
        }
        return 1-Math.exp(-num*0.04);
    }

    /**
     * 用于调整数据。
     * <br>
     * S型曲线，关于点(50,0.5)对称
     * <br>
     * @param num x
     * @param parameter1 这个数值越大，曲线越陡峭。一般取0.1时可得到一个陡峭度适中的曲线。
     */
    public static double CalculatingFunction2(double num, double parameter1){
        return 1/(1+Math.exp(-num * parameter1 + 50 * parameter1));
    }

    /**
     * Calculate temperature rating based on temperature difference from comfortable temperature.
     * Uses sigmoid function to map temperature difference to rating between 0 and 1.
     * 
     * @param temperature the actual temperature
     * @return temperature rating between 0 and 1
     */
    public static double calculateTemperatureRating(double temperature) {
        double tempDiff = Math.abs(COMFORTABLE_TEMP_HOUSE - temperature);
        return 0.017 + 1 / (1 + Math.exp(0.4 * (tempDiff - 10)));
    }

    /**
     * Calculate decoration rating based on decorations map and area.
     * Uses logarithmic scoring to evaluate decoration quality.
     * 
     * @param decorations map of decoration items and their counts
     * @param area the area of the space
     * @return decoration rating between 0 and 1
     */
    public static double calculateDecorationRating(java.util.Map<?, Integer> decorations, int area) {
        double score = 0;
        for (Integer num : decorations.values()) {
            if (num + 0.32 > 0) { // Ensure the argument for log is positive
                score += Math.log(num + 0.32) * 1.75 + 0.9;
            } else {
                // Handle the case where num + 0.32 <= 0
                // For example, you could add a minimal score or skip adding to the score.
                score += 0; // Or some other handling logic
            }
        }
        return Math.min(1, score / (6 + area / 16.0f));
    }

    /**
     * Calculate space rating based on volume and area.
     * Evaluates how well-proportioned the space is based on height and area.
     * 
     * @param volume the volume of the space
     * @param area the floor area of the space
     * @return space rating between 0 and 1
     */
    public static double calculateSpaceRating(int volume, int area) {
        double height = volume / (float) area;
        double score = area * (1.55 + Math.log(height - 1.6) * 0.6);
        return 1 - Math.exp(-0.024 * Math.pow(score, 1.11));
    }
}
