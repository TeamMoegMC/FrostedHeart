package com.teammoeg.frostedheart.mixin.minecraft.fix;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;

@Mixin(LevelStorageSource.class)
public class LevelStorageSourceMixin_FixBiome  {
	private static final ImmutableList<String> OLD_SETTINGS_KEYS = ImmutableList.of("RandomSeed", "generatorName",
			"generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");

	@Overwrite
	private static <T> DataResult<WorldGenSettings> readWorldGenSettings(Dynamic<T> pDynamic, DataFixer pFixer,
			int pVersion) {
		Dynamic<T> dynamic = pDynamic.get("WorldGenSettings").orElseEmptyMap();

		for (String s : OLD_SETTINGS_KEYS) {
			Optional<Dynamic<T>> optional = pDynamic.get(s).result();
			if (optional.isPresent()) {
				dynamic = dynamic.set(s, optional.get());
			}
		}
		Optional<Dynamic<T>> dimensionsO=dynamic.get("dimensions").result();
		if(dimensionsO.isPresent()) {
			Dynamic<T> dimensions=dimensionsO.get();
			DataResult<Stream<Pair<Dynamic<T>, Dynamic<T>>>> dimensionsMap=dimensions.asMapOpt();
			Map<Dynamic<T>,Dynamic<T>> output=new HashMap<>();
			Optional<Stream<Pair<Dynamic<T>, Dynamic<T>>>> mapped=dimensionsMap.map(p->p.map(i->{
				Dynamic<T> key=i.getFirst();
				Dynamic<T> value=i.getSecond();
				Optional<Dynamic<T>> gen=value.get("generator").result();
				if(gen.isPresent()) {
					Optional<Dynamic<T>> biomeSource=gen.get().get("biome_source").result();
					if(biomeSource.isPresent()) {
						Optional<Dynamic<T>> biome=biomeSource.get().get("biomes").result();
						if(biome.isPresent()) {
							value=value.set("generator",gen.get().set("biome_source",biomeSource.get().set("biomes",  biomeSource.get().createList(
							biome.get().asStream().map(n->{
								Optional<String> s=n.get("biome").result().get().asString().result();
								if(s.isPresent()&&s.get().contains("terralith"))
									return null;
								return n;
							}).filter(t->t!=null)))));
						}
					}
				}
				
				return Pair.of(key, value);
			})).result();
			if(mapped.isPresent()) {
				mapped.get().forEach(p->output.put(p.getFirst(), p.getSecond()));
				dynamic=dynamic.set("dimensions", dynamic.createMap(output));
			}
		}
		Dynamic<T> dynamic1 = DataFixTypes.WORLD_GEN_SETTINGS.updateToCurrentVersion(pFixer, dynamic, pVersion);
		return WorldGenSettings.CODEC.parse(dynamic1);
	}
}
