package com.teammoeg.frostedheart.content.climate.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

class PlayerThermalModelHotSpringTest {
    @Test
    void oneBlockDeepSpringWarmsTheLowerBodyWithoutReplacingAirContact() {
        double heightRatio = (8.0D / 9.0D) / 1.8D;
        assertEquals(1.0D, PlayerThermalModel.bandFraction(BodyPart.FEET, heightRatio), 1.0E-6D);
        assertTrue(PlayerThermalModel.bandFraction(BodyPart.LEGS, heightRatio) > 0.8D);
        assertTrue(PlayerThermalModel.bandFraction(BodyPart.LEGS, heightRatio) < 1.0D);
        assertEquals(0.0D, PlayerThermalModel.bandFraction(BodyPart.TORSO, heightRatio), 1.0E-6D);
        assertTrue(PlayerThermalModel.bandFraction(BodyPart.HANDS, heightRatio) > 0.0D);
        assertEquals(0.0D, PlayerThermalModel.bandFraction(BodyPart.HEAD, heightRatio), 1.0E-6D);

        double hNatural = PlayerThermalModel.naturalConvectionCoefficientWPerM2K(-20.0D);
        double dry = equivalent(-20.0D, hNatural, 0.0D, 0.0D);
        double ordinaryWater = equivalent(-20.0D, hNatural, heightRatio, 0.0D);
        double spring = equivalent(-20.0D, hNatural, heightRatio, heightRatio);
        double springOnly = equivalent(-20.0D, hNatural, 0.0D, heightRatio);
        double fullSpring = equivalent(-20.0D, hNatural, 1.0D, 1.0D);
        assertEquals(springOnly, spring, 1.0E-9D,
                "water's overlapping height must not count as cold water too");
        assertTrue(spring > ordinaryWater);
        assertTrue(spring > dry);
        assertTrue(fullSpring > spring);

        double mixed = equivalent(-20.0D, hNatural, 1.0D, heightRatio);
        assertTrue(mixed < fullSpring);
        assertTrue(mixed > equivalent(-20.0D, hNatural, 1.0D, 0.0D));
    }

    @Test
    void ordinaryWaterTemperatureRuleIsUnchanged() {
        assertEquals(0.0D, PlayerThermalModel.waterTemperatureC(-20.0D));
        assertEquals(20.0D, PlayerThermalModel.waterTemperatureC(20.0D));
        assertEquals(35.0D, PlayerThermalModel.waterTemperatureC(40.0D));
        assertEquals(38.0D, PlayerThermalModel.HOT_SPRING_TEMPERATURE_C);
    }

    private static double equivalent(double airC, double hNatural, double waterRatio, double springRatio) {
        return PlayerThermalModel.environmentalEquivalentTemperatureC(
                airC, hNatural, 0.0D, 0.0D, PlayerThermalModel.waterTemperatureC(airC),
                waterRatio, springRatio, 0.0D, false, false);
    }
}
