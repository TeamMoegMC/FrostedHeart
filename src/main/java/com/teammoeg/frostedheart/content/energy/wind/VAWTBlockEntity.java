package com.teammoeg.frostedheart.content.energy.wind;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.utility.Lang;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.client.ClientUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

public class VAWTBlockEntity extends GeneratingKineticBlockEntity implements
        CTickableBlockEntity, IHaveGoggleInformation {

    enum ModifierType {SPEED, DAMAGE}
    record Modifier(ModifierType modifierType, Component reason, float value) {}
    private final List<Modifier> modifiers = new ArrayList<>();

    private Holder<Biome> biome;
    @Getter
    private boolean generatable = false;
    @Getter
    private boolean damaged = false;
    private boolean canSeeSky = false;
    @Getter
    private String reason = "";

    @Getter
    private long durability = 0; // ms

    private int lastCheckOffsetY = 0;
    public static final AABB AREA = new AABB(-8, 0, -8, 8, 256, 8); // TODO config

    public VAWTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof VAWTBlock vawt) {
            this.durability = vawt.type.durability;
        }
        setLazyTickRate(20);
    }

    @Override
    public void tick() {
        if (!damaged && durability <= 0) {
            damaged = true;
            if (!level.isClientSide)
                level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(VAWTBlock.DAMAGED, true));
        }
        super.tick();
    }

    @Override
    public void lazyTick() {
        checkEnvironment(false);
        updateModifier();
        this.updateGeneratedRotation();
        if (!damaged && generatable) {
            damage();
        }
    }

    private void damage() {
        float dmg = 50 * lazyTickRate;
        for (Modifier modifier : modifiers) {
            if (modifier.modifierType == ModifierType.DAMAGE) {
                dmg *= modifier.value;
            }
        }
        durability -= Math.round(dmg);
    }

    public boolean checkEnvironment(boolean immediately) { // TODO
        canSeeSky = level.canSeeSky(getBlockPos().above());
        biome = level.getBiome(getBlockPos());
        generatable = canSeeSky;
        return generatable;
    }

    private void updateModifier() {
        modifiers.clear();

        boolean flag = false;
        if (damaged) {
            reason = "damaged";
            flag = true;
        } else if (!canSeeSky) {
            reason = "blocked";
            flag = true;
        } else if (biome != null && (getBlockPos().getY() < level.getSeaLevel()-16 /*TODO config*/ || biome.containsTag(Tags.Biomes.IS_CAVE) || biome.containsTag(BiomeTags.IS_NETHER))) {
            reason = "underground";
            flag = true;
        }
        if (flag) {
            var r = Component.literal(reason);
            addModifier(ModifierType.SPEED, r, 0);
            addModifier(ModifierType.DAMAGE, r, 0);
            return;
        }
        reason = "";

        // Plain  1.25x
        // Ocean  1.35x
        // Other  1x
        if (biome != null) {
            if (biome.containsTag(BiomeTags.IS_OCEAN)) {
                addModifier(ModifierType.SPEED, Component.literal("ocean"), 1.35F);
            } else if (biome.containsTag(Tags.Biomes.IS_PLAINS)) {
                addModifier(ModifierType.SPEED, Component.literal("plain"), 1.25F);
            }
        }

        //        Gen   Dmg
        // Clear  1x    1x
        // Snow   1.5x  2x
        // Storm  2x    4x
        if (level.isThundering()) {
            addModifier(ModifierType.SPEED, Component.literal("thundering"), 2F);
            addModifier(ModifierType.DAMAGE, Component.literal("thundering"), 4F);
        } else if (level.isRaining()) {
            addModifier(ModifierType.SPEED, Component.literal("raining/snowing"), 1.5F);
            addModifier(ModifierType.DAMAGE, Component.literal("raining/snowing"), 2F);
        }

        if (getBlockState().getBlock() instanceof VAWTBlock v) {
            addModifier(ModifierType.SPEED, Component.literal("material: " + v.type.name), v.type.weight);
        }
    }

    private void addModifier(ModifierType type, Component reason, float value) {
        modifiers.add(new Modifier(type, reason, value));
    }

    private float calcEffect(ModifierType type) {
        float effect = 1;
        for (Modifier modifier : modifiers) {
            if (modifier.modifierType == type) {
                effect *= modifier.value;
            }
        }
        return effect;
    }

    @Override
    public float getGeneratedSpeed() {
        if (damaged || !generatable) return 0;
        return Math.round(16 * calcEffect(ModifierType.SPEED));
    }

    public float calculateAddedStressCapacity() {
        return this.lastCapacityProvided = 9F;
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        this.durability = compound.getLong("durability");
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putLong("durability", durability);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (!damaged) {
            float speed = getGeneratedSpeed();
            float s = calcEffect(ModifierType.SPEED);
            Lang.builder()
                    .translate("gui.speedometer.title")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);

            Lang.builder()
                    .text(speed + " RPM ")
                    .style(ChatFormatting.AQUA)
                    .add(Component.literal(" (" + Math.round(s*100) + "%)").withStyle(s < 1 ? ChatFormatting.RED : ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);
            if (!reason.isBlank()) {
                Lang.builder()
                        .text(reason) // TODO lang
                        .style(ChatFormatting.RED)
                        .forGoggles(tooltip, 1);
            }

            float time = calcEffect(VAWTBlockEntity.ModifierType.DAMAGE);
            Lang.builder()
                    .add(Component.translatable("gui.frostedheart.time_left"))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Lang.builder()
                .add(ClientUtils.asTime(durability))
                .style(ChatFormatting.AQUA)
                .add(Component.literal(" (" + Math.round(time*100) + "%)").withStyle(time > 1 ? ChatFormatting.RED : ChatFormatting.GREEN))
                .forGoggles(tooltip, 1);
        } else {
            Lang.builder()
                    .text(reason) // TODO lang
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }
        return true;
    }
}
