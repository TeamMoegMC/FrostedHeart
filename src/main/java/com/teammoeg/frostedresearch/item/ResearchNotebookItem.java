/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.item;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.utility.SoilThermometer;
import com.teammoeg.frostedresearch.api.KnowledgeDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.ResearchWorkflowRegistry;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationContext;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationProviderRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Non-stackable, topic-free field notebook with a deliberate forty-tick capture. */
public final class ResearchNotebookItem extends FRBaseItem {
    public static final int CAPTURE_TICKS = 40;
    private static final String TARGET = "observation_target";
    private static final String TARGET_DIMENSION = "observation_dimension";
    private static final String TARGET_POS = "observation_position";
    private static final String TARGET_ENTITY = "observation_entity";
    private static final String PROFILE = "observation_profile";

    public ResearchNotebookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TARGET, "block");
        tag.putString(TARGET_DIMENSION, context.getLevel().dimension().location().toString());
        tag.putLong(TARGET_POS, context.getClickedPos().asLong());
        player.startUsingItem(context.getHand());
        player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.recording")
                .withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TARGET, "entity");
        tag.putString(TARGET_DIMENSION, player.level().dimension().location().toString());
        tag.putUUID(TARGET_ENTITY, target.getUUID());
        tag.putLong(TARGET_POS, target.blockPosition().asLong());
        player.startUsingItem(hand);
        player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.recording")
                .withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            Profile next = profile(stack).next();
            stack.getOrCreateTag().putString(PROFILE, next.name());
            player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.profile",
                    Component.translatable(next.translationKey())), true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CAPTURE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide && living instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            finishCapture(stack, serverLevel, player);
        }
        clearTarget(stack);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide && stack.hasTag() && stack.getTag().contains(TARGET)
                && living instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.cancelled")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        clearTarget(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.frostedresearch.notebook.capture", CAPTURE_TICKS / 20.0F)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.frostedresearch.notebook.profile",
                        Component.translatable(profile(stack).translationKey()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.frostedresearch.notebook.configure")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void finishCapture(ItemStack stack, ServerLevel level, ServerPlayer player) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET) || !tag.getString(TARGET_DIMENSION)
                .equals(level.dimension().location().toString())) return;
        BlockPos storedPos = BlockPos.of(tag.getLong(TARGET_POS));
        if (player.blockPosition().distSqr(storedPos) > 64.0D) {
            player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.target_lost"), true);
            return;
        }
        if ("entity".equals(tag.getString(TARGET)) && tag.hasUUID(TARGET_ENTITY)) {
            Entity entity = level.getEntity(tag.getUUID(TARGET_ENTITY));
            if (entity == null || entity.distanceToSqr(player) > 64.0D) {
                player.displayClientMessage(Component.translatable("message.frostedresearch.notebook.target_lost"), true);
                return;
            }
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId == null) return;
            ObservationContext context = captureContext(level, player, entity.blockPosition(), entityId,
                    Blocks.AIR.defaultBlockState(), ObservationContext.TargetType.ENTITY, profile(stack));
            KnowledgeRecord record = ObservationProviderRegistry.observeEntity(context, entity.getUUID());
            archive(player, record, entity.getDisplayName());
            return;
        }

        BlockState state = level.getBlockState(storedPos);
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) return;
        ObservationContext context = captureContext(level, player, storedPos, blockId, state,
                ObservationContext.TargetType.BLOCK, profile(stack));
        KnowledgeRecord record = ObservationProviderRegistry.observeBlock(level, context);
        archive(player, record, state.getBlock().getName());
    }

    private static ObservationContext captureContext(ServerLevel level, ServerPlayer player, BlockPos pos,
            ResourceLocation subject, BlockState state, ObservationContext.TargetType targetType, Profile profile) {
        ResourceLocation biome = level.getBiome(pos).unwrapKey().map(key -> key.location())
                .orElse(new ResourceLocation("minecraft", "the_void"));
        ObservationContext.Weather weather = level.isThundering() ? ObservationContext.Weather.THUNDER
                : level.isRaining() ? ObservationContext.Weather.RAIN : ObservationContext.Weather.CLEAR;
        Map<String, String> measurements = new LinkedHashMap<>();
        if (profile.fields.contains(ObservationContext.Field.TEMPERATURE)
                && SoilThermometer.isWearingSoilThermometer(player)) {
            measurements.put("temperature", Float.toString(WorldTemperature.block(level, pos)));
        }
        return new ObservationContext(targetType, level.dimension().location(), pos, subject, state,
                level.getGameTime(), level.getDayTime(), player.getUUID(),
                ObservationProviderRegistry.NOTEBOOK_CHANNEL, biome, weather, profile.fields, measurements);
    }

    private static void archive(ServerPlayer player, KnowledgeRecord record, Component subjectName) {
        boolean existed = KnowledgeDataAPI.getData(player).get().observations().stream()
                .anyMatch(existing -> existing.semanticKey().equals(record.semanticKey()));
        TeamResearchService.archiveObservation(player, record);
        Component result = Component.translatable(existed
                ? "message.frostedresearch.notebook.updated" : "message.frostedresearch.notebook.recorded",
                subjectName);
        java.util.Optional<ResourceLocation> annotation = ResearchWorkflowRegistry
                .observationAnnotations(KnowledgeDataAPI.getData(player).get(), record)
                .stream().findFirst();
        if (annotation.isPresent()) result = result.copy().append(" ").append(Component.translatable(
                annotationKey(annotation.get())));
        player.displayClientMessage(result.copy().withStyle(ChatFormatting.GOLD), true);
    }

    private static void clearTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(TARGET);
        tag.remove(TARGET_DIMENSION);
        tag.remove(TARGET_POS);
        tag.remove(TARGET_ENTITY);
    }

    public static Profile profile(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return Profile.STANDARD;
        try {
            return Profile.valueOf(tag.getString(PROFILE));
        } catch (IllegalArgumentException ignored) {
            return Profile.STANDARD;
        }
    }

    private static String annotationKey(ResourceLocation id) {
        return "message.frostedresearch.notebook.annotation." + id.getNamespace() + "."
                + id.getPath().replace('/', '.');
    }

    public enum Profile {
        STANDARD(Set.of(ObservationContext.Field.LOCATION, ObservationContext.Field.TIME,
                ObservationContext.Field.BIOME, ObservationContext.Field.WEATHER,
                ObservationContext.Field.BLOCK_STATE)),
        COMPACT(Set.of(ObservationContext.Field.LOCATION, ObservationContext.Field.TIME,
                ObservationContext.Field.BLOCK_STATE)),
        ENVIRONMENT(Set.of(ObservationContext.Field.LOCATION, ObservationContext.Field.TIME,
                ObservationContext.Field.BIOME, ObservationContext.Field.WEATHER,
                ObservationContext.Field.BLOCK_STATE, ObservationContext.Field.TEMPERATURE));

        private final Set<ObservationContext.Field> fields;

        Profile(Set<ObservationContext.Field> fields) {
            this.fields = Set.copyOf(fields);
        }

        Profile next() {
            return values()[(ordinal() + 1) % values().length];
        }

        String translationKey() {
            return "tooltip.frostedresearch.notebook.profile." + name().toLowerCase();
        }
    }
}
