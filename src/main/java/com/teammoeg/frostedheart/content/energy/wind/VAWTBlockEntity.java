package com.teammoeg.frostedheart.content.energy.wind;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VAWTBlockEntity extends GeneratingKineticBlockEntity implements
        CTickableBlockEntity, IHaveGoggleInformation {

    enum ModifierType {SPEED, DAMAGE}
    record Modifier(ModifierType modifierType, String reason, float value) {}
    private final List<Modifier> modifiers = new ArrayList<>();

    @Getter
    private boolean generatable = false;
    @Getter
    private boolean damaged = false;

    @Getter
    private float speedEffect = 0;
    @Getter
    private float damageEffect = 0;

    @Getter
    private String reason = "";
    @Getter
    private long durability = 0; // ms

    public VAWTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof VAWTBlock vawt) {
            this.durability = vawt.type.getDurability();
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
        if (!level.isClientSide) {
            speedEffect = calcEffect(ModifierType.SPEED);
            damageEffect = calcEffect(ModifierType.DAMAGE);
        }
        super.tick();
    }

    @Override
    public void lazyTick() {
        if (level.isClientSide) return;

        updateModifier();
        this.updateGeneratedRotation();
        if (!damaged && generatable) {
            damage();
        }
    }

    private void damage() {
        float dmg = 50 * lazyTickRate * damageEffect;
        durability -= Math.round(dmg);
    }

    private int successTime = 0;
    private int superLazyTickRate = 0;
    private int superLazyTickCounter = 0;
    private boolean lastEnvResult = false;
    /**
     * @param immediately if true check the whole area immediately
     * @return is environment valid
     */
    public boolean checkEnvironment(boolean immediately) { // TODO
        reason = "detecting";

        boolean shouldCheck = false;
        if (successTime == 0 || immediately) {
            shouldCheck = true;
            // 防止全部在同一 tick 更新
            superLazyTickCounter = level.random.nextInt(20);
        } else if (superLazyTickCounter-- <= 0) {
            superLazyTickCounter = superLazyTickRate;
            shouldCheck = true;
        }
        superLazyTickRate = Math.min(successTime, FHConfig.SERVER.VAWT.vawtEmptyAreaMaxDetectCooldown.get());

        if (!shouldCheck) return lastEnvResult;

        collectPos();
        double sc = 0;
        double lambda = -Math.log(0.5) / 8.0;
        float threshold = FHConfig.SERVER.VAWT.vawtEmptyAreaAllowsBlockCount.get();
        for (BlockPos pos : collectedPos) {
            if (level.canSeeSky(pos)) continue;

            var pos2 = pos;
            var block = level.getBlockState(pos2);
            while (pos2.getY() <= level.getMaxBuildHeight()) {
                if (block.getBlock() instanceof VAWTBlock) {
                    reason = "neighbor";
                    successTime = 0;
                    return lastEnvResult = false;
                } else if (!(block.getBlock() instanceof AirBlock)) {
                    double distance = Math.max(pos2.getCenter().distanceTo(getBlockPos().getCenter())-0.5f, 1);
                    sc += Math.max(threshold * Math.exp(-lambda * distance), 0.1F);
                }
                pos2 = pos2.above();
                block = level.getBlockState(pos2);
            }

            if (sc >= threshold) {
                reason = "blocked";
                successTime = 0;
                return lastEnvResult = false;
            }
        }
        successTime++;
        return lastEnvResult = true;
    }

    public static int emptyAreaRange = -1;
    private List<BlockPos> collectedPos;
    private void collectPos() {
        if (collectedPos != null && emptyAreaRange == FHConfig.SERVER.VAWT.vawtEmptyAreaRange.get()) {
            return;
        }
        emptyAreaRange = FHConfig.SERVER.VAWT.vawtEmptyAreaRange.get();
        var pos = getBlockPos();
        collectedPos = BlockPos.betweenClosedStream(
                    getBlockPos().offset(-emptyAreaRange, 0, -emptyAreaRange),
                    getBlockPos().offset(+emptyAreaRange, 0, +emptyAreaRange))
                .filter(p -> !p.equals(pos))
                .map(BlockPos::immutable)
                .collect(Collectors.toList());
    }

    private void updateModifier() {
        modifiers.clear();
        generatable = true;
        reason = "detecting";
        var biome = level.getBiome(getBlockPos());
        boolean canSeeSky = level.canSeeSky(getBlockPos().above());

        if (damaged) {
            reason = "damaged";
            generatable = false;
        } else if (!canSeeSky) {
            reason = "blocked";
            generatable = false;
        } else if ((getBlockPos().getY() < level.getSeaLevel()-16 || biome.containsTag(Tags.Biomes.IS_CAVE) || biome.containsTag(BiomeTags.IS_NETHER))) {
            reason = "underground";
            generatable = false;
        } else if (!checkEnvironment(false)) {
            generatable = false;
        }
        if (!generatable) {
            addModifier(ModifierType.SPEED, reason, 0);
            addModifier(ModifierType.DAMAGE, reason, 0);
            return;
        }
        reason = "";

        // Plain  1.25x
        // Ocean  1.35x
        // Other  1x
        if (biome.containsTag(BiomeTags.IS_OCEAN)) {
            addModifier(ModifierType.SPEED, "ocean", 1.35F);
        } else if (biome.containsTag(Tags.Biomes.IS_PLAINS)) {
            addModifier(ModifierType.SPEED, "plain", 1.25F);
        }

        //        Gen   Dmg
        // Clear  1x    1x
        // Snow   1.5x  2x
        // Storm  2x    4x
        if (level.isThundering()) {
            addModifier(ModifierType.SPEED, "thundering", 2F);
            addModifier(ModifierType.DAMAGE, "thundering", 4F);
        } else if (level.isRaining()) {
            addModifier(ModifierType.SPEED, "raining/snowing", 1.5F);
            addModifier(ModifierType.DAMAGE, "raining/snowing", 2F);
        }

        if (getBlockState().getBlock() instanceof VAWTBlock v) {
            addModifier(ModifierType.SPEED, "material: " + v.type.name, v.type.weight);
        }
    }

    private void addModifier(ModifierType type, String reason, float value) {
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
        if (level.isClientSide) return getTheoreticalSpeed();
        if (damaged || !generatable) return 0;
        return Math.round(16 * speedEffect);
    }

    public float calculateAddedStressCapacity() {
        return this.lastCapacityProvided = FHConfig.SERVER.VAWT.vawtCapacity.get().floatValue();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        this.durability = compound.getLong("durability");
        this.reason = compound.getString("reason");
        this.speedEffect = compound.getFloat("speed_effect");
        this.damageEffect = compound.getFloat("damage_effect");
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putLong("durability", durability);
        compound.putString("reason", reason);
        compound.putFloat("speed_effect", speedEffect);
        compound.putFloat("damage_effect", damageEffect);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        if (reason.isBlank()) {
            float speed = getGeneratedSpeed();
            Lang.translate("gui.speedometer.title")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Lang.builder()
                    .text(speed + " RPM ")
                    .style(ChatFormatting.AQUA)
                    .add(Component.literal(" (" + Math.round(speedEffect*100) + "%)").withStyle(speedEffect < 1 ? ChatFormatting.RED : ChatFormatting.GREEN))
                    .forGoggles(tooltip, 1);

            Lang.builder()
                    .add(Component.translatable("gui.frostedheart.durability_left"))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            Lang.builder()
                .add(ClientUtils.msToTime(durability))
                .style(ChatFormatting.AQUA)
                .add(Component.literal(" (" + Math.round(damageEffect*100) + "%)").withStyle(damageEffect > 1 ? ChatFormatting.RED : ChatFormatting.GREEN))
                .forGoggles(tooltip, 1);
        } else {
            tooltip.add(Component.empty());
            Lang.builder()
                    .add(Component.translatable("message.frostedheart.vawt.state.cannotwork"))
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip);
            for (Component c : TooltipHelper.cutTextComponent(Component.translatable("message.frostedheart.vawt.state." + reason), TooltipHelper.Palette.GRAY_AND_WHITE)) {
                Lang.builder()
                        .add(c.copy())
                        .forGoggles(tooltip);
            }
        }
        return true;
    }
}
