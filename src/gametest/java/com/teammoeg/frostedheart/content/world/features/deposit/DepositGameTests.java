package com.teammoeg.frostedheart.content.world.features.deposit;

import java.util.List;
import java.util.HashSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises the production codecs and normal RandomState lifecycle in an actual Forge server. */
@GameTestHolder("frostedheart_deposits")
@PrefixGameTestTemplate(false)
public final class DepositGameTests {
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void seededRegions(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var base = access.registryOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var noises = access.lookupOrThrow(Registries.NOISE);
        var all = Climate.Parameter.span(-2, 2);
        var biomes = access.registryOrThrow(Registries.BIOME);
        var points = new Climate.ParameterList<>(List.of(
                Pair.of(new Climate.ParameterPoint(Climate.Parameter.span(-2, 0), all, all, all, all, all, 0), biomes.getHolderOrThrow(Biomes.PLAINS)),
                Pair.of(new Climate.ParameterPoint(Climate.Parameter.span(0, 2), all, all, all, all, all, 0), biomes.getHolderOrThrow(Biomes.FOREST))));
        var wrapped = new DepositBiomeSource(points, biomes.getHolderOrThrow(Biomes.DRIPSTONE_CAVES));
        var nativeSource = MultiNoiseBiomeSource.createFromList(points);
        var bridge = new DepositRegions(base.noiseRouter().depth(), new DensityFunction.NoiseHolder(noises.getOrThrow(Noises.TEMPERATURE)),
                new DepositRegions.Settings(512, 32, .35, -40, -32, 48151623));
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        var json = DensityFunction.DIRECT_CODEC.encodeStart(ops, bridge).result().orElseThrow();
        var decoded = DensityFunction.DIRECT_CODEC.parse(ops, json).result().orElseThrow();
        helper.assertTrue(decoded instanceof DepositRegions, "Production density type must round-trip through Forge registries");
        var r = base.noiseRouter();
        var router = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(), r.lavaNoise(),
                r.temperature(), r.vegetation(), r.continents(), r.erosion(), decoded, r.ridges(),
                r.initialDensityWithoutJaggedness(), r.finalDensity(), r.veinToggle(), r.veinRidged(), r.veinGap());
        var settings = new NoiseGeneratorSettings(base.noiseSettings(), base.defaultBlock(), base.defaultFluid(), router,
                base.surfaceRule(), base.spawnTarget(), base.seaLevel(), base.disableMobGeneration(), base.isAquifersEnabled(),
                base.oreVeinsEnabled(), base.useLegacyRandomSource());
        var fingerprints = new HashSet<Long>();
        int regional = 0, delegated = 0;
        for (long seed = 0; seed < 8; seed++) {
            var original = RandomState.create(base, noises, seed);
            var state = RandomState.create(settings, noises, seed);
            var regions = DepositRegions.resolve(state.sampler().depth());
            helper.assertTrue(regions != null, "Seeded region carrier must survive the sampler visitor");
            long fingerprint = 0;
            for (int x = -1024; x <= 1024; x += 64) for (int z = -1024; z <= 1024; z += 64) {
                var layout = regions.layout(x, z);
                if (layout != null) fingerprint = DepositLayout.mix(fingerprint ^ layout.seed());
                for (int y : new int[]{-48, -32, 0, 32}) {
                    int qx = x >> 2, qy = y >> 2, qz = z >> 2;
                    helper.assertTrue(original.sampler().sample(qx, qy, qz).equals(state.sampler().sample(qx, qy, qz)), "All six climate values must remain unchanged");
                    var actual = wrapped.getNoiseBiome(qx, qy, qz, state.sampler());
                    if (layout != null && layout.containsBiome(x, y, z)) {
                        helper.assertTrue(actual.is(Biomes.DRIPSTONE_CAVES), "Biome ownership must agree with terrain layout"); regional++;
                    } else {
                        helper.assertTrue(actual.equals(nativeSource.getNoiseBiome(qx, qy, qz, original.sampler())), "Outside regions must delegate unchanged"); delegated++;
                    }
                }
            }
            fingerprints.add(fingerprint);
        }
        helper.assertTrue(fingerprints.size() == 8 && regional > 0 && delegated > 0, "Exercise distinct seeds, regional and delegated positions");
        helper.succeed();
    }
}
