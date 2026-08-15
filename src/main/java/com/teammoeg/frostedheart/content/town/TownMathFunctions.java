package com.teammoeg.frostedheart.content.town;

public class TownMathFunctions {
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
    public static double calculateTemperatureRating(
            double temperature,
            double comfortableTemperature,
            double minimumRating,
            double sigmoidSlopePerCelsius,
            double halfPointTemperatureDifferenceCelsius
    ) {
        double tempDiff = Math.abs(comfortableTemperature - temperature);
        return minimumRating + 1 / (1 + Math.exp(sigmoidSlopePerCelsius
                * (tempDiff - halfPointTemperatureDifferenceCelsius)));
    }

    /**
     * Calculate decoration rating based on decorations map and area.
     * Uses logarithmic scoring to evaluate decoration quality.
     * 
     * @param decorations map of decoration items and their counts
     * @param area the area of the space
     * @return decoration rating between 0 and 1
     */
    public static double calculateDecorationRating(
            java.util.Map<?, Integer> decorations,
            int area,
            double countLogOffset,
            double countLogMultiplier,
            double typeBaseScore,
            double baseDemand,
            double floorBlocksPerDemand
    ) {
        double score = 0;
        for (Integer num : decorations.values()) {
            if (num + countLogOffset > 0) { // Ensure the argument for log is positive
                score += Math.log(num + countLogOffset) * countLogMultiplier + typeBaseScore;
            } else {
                // Handle the case where num + 0.32 <= 0
                // For example, you could add a minimal score or skip adding to the score.
                score += 0; // Or some other handling logic
            }
        }
        return Math.min(1, score / (baseDemand + area / floorBlocksPerDemand));
    }

    /**
     * Calculate space rating based on volume and area.
     * Evaluates how well-proportioned the space is based on height and area.
     * 
     * @param volume the volume of the space
     * @param area the floor area of the space
     * @return space rating between 0 and 1
     */
    public static double calculateSpaceRating(
            int volume,
            int area,
            double areaCoefficient,
            double heightLogCoefficient,
            double heightLogOffset,
            double responseScale,
            double responseExponent
    ) {
        double height = volume / (float) area;
        double score = area * (areaCoefficient
                + Math.log(height - heightLogOffset) * heightLogCoefficient);
        return 1 - Math.exp(-responseScale * Math.pow(score, responseExponent));
    }

    public static double attributeScore(double value) {
        if (value <= 0) return 0.0;
        if (value >= 100) return 1.0;
        if (value <= 40) {
            // 0～40 线性: 0 → 0.5
            return 0.5 * (value / 40.0);
        } else {
            // 40～100 线性: 0.5 → 1.0
            return 0.5 + 0.5 * ((value - 40) / 60.0);
        }
    }

    /**
     * Calculates a weighted arithmetic average for resident attributes.
     * Attribute values are clamped to the standard resident range [0, 100].
     * Non-positive weights are ignored; when every weight is non-positive,
     * all attributes receive equal weight.
     */
    public static double weightedAttributeAverage(double[] attributes, double[] weights) {
        if (attributes == null || weights == null || attributes.length == 0 || attributes.length != weights.length) {
            throw new IllegalArgumentException("Attributes and weights must have the same non-zero length.");
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        double unweightedSum = 0.0;
        for (int i = 0; i < attributes.length; i++) {
            double attribute = clamp(attributes[i], 0.0, 100.0);
            unweightedSum += attribute;
            if (weights[i] > 0.0) {
                weightedSum += attribute * weights[i];
                totalWeight += weights[i];
            }
        }
        return totalWeight > 0.0 ? weightedSum / totalWeight : unweightedSum / attributes.length;
    }

    /**
     * Converts resident attributes and profession proficiency into a directly
     * readable number of standard workers.
     * <p>
     * Attribute productivity is linearly interpolated between the configured
     * values at attribute 0 and 100. Proficiency adds a separate linear bonus
     * from 0 to the configured maximum proficiency. The result is then clamped
     * to the configured final range.
     */
    public static double linearResidentProductivity(
            double[] attributes,
            double[] weights,
            double proficiency,
            double productivityAtAttributeZero,
            double productivityAtAttributeHundred,
            double maximumProficiency,
            double bonusAtMaximumProficiency,
            double minimumProductivity,
            double maximumProductivity
    ) {
        double averageAttribute = weightedAttributeAverage(attributes, weights);
        double attributeProductivity = productivityAtAttributeZero
                + (productivityAtAttributeHundred - productivityAtAttributeZero) * averageAttribute / 100.0;
        double proficiencyRatio = maximumProficiency > 0.0
                ? clamp(proficiency, 0.0, maximumProficiency) / maximumProficiency
                : 0.0;
        double productivity = attributeProductivity + bonusAtMaximumProficiency * proficiencyRatio;
        double lowerBound = Math.min(minimumProductivity, maximumProductivity);
        double upperBound = Math.max(minimumProductivity, maximumProductivity);
        return clamp(productivity, lowerBound, upperBound);
    }

    /**
     * Adds an expected fractional amount to a persistent carry and returns the
     * whole units available for this settlement together with the new carry.
     */
    public static FractionalSettlement settleFractionalAmount(double carry, double expectedAmount) {
        double safeCarry = Double.isFinite(carry) ? Math.max(0.0, carry) : 0.0;
        double safeExpectedAmount = Double.isFinite(expectedAmount) ? Math.max(0.0, expectedAmount) : 0.0;
        double budget = safeCarry + safeExpectedAmount;
        long wholeAmount = (long) Math.floor(budget);
        return new FractionalSettlement(wholeAmount, budget - wholeAmount);
    }

    public record FractionalSettlement(long wholeAmount, double carry) {
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
