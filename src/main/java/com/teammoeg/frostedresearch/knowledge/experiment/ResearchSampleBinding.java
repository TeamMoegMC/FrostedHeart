package com.teammoeg.frostedresearch.knowledge.experiment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Only the experiment's actual team/run/task identity; no inventory or world snapshots. */
public record ResearchSampleBinding(UUID team, UUID run, String requirement, ResourceLocation experiment) {
    public static final String TAG = "ResearchSample";
    public static final Codec<ResearchSampleBinding> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("team").forGetter(ResearchSampleBinding::team),
        UUIDUtil.CODEC.fieldOf("run").forGetter(ResearchSampleBinding::run),
        Codec.STRING.fieldOf("requirement").forGetter(ResearchSampleBinding::requirement),
        ResourceLocation.CODEC.fieldOf("experiment").forGetter(ResearchSampleBinding::experiment)
    ).apply(i, ResearchSampleBinding::new));
    public static Optional<ResearchSampleBinding> read(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG) ? CODEC.parse(NbtOps.INSTANCE, stack.getTag().get(TAG)).result() : Optional.empty();
    }
    public void write(ItemStack stack) { stack.getOrCreateTag().put(TAG, CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseThrow()); }
}
