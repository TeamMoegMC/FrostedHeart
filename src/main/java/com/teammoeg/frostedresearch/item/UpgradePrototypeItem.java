/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.item;

import com.teammoeg.frostedresearch.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Physical identity shell for a research prototype. Installation arrives in phase five. */
public final class UpgradePrototypeItem extends Item {
    public static final int CURRENT_SCHEMA = 1;
    private static final String ROOT = "frostedresearch:prototype";
    private static final String SCHEMA = "schema";
    private static final String PROFILE = "profile";
    private static final String PROFILE_REVISION = "profile_revision";
    private static final String SERIAL = "serial";
    private static final String OWNER_TEAM = "owner_team";

    public UpgradePrototypeItem(Properties properties) {
        super(properties);
    }

    public ItemStack create(ResourceLocation profile, int profileRevision, UUID ownerTeam) {
        ItemStack stack = new ItemStack(this);
        writeIdentity(stack, newIdentity(profile, profileRevision, ownerTeam));
        return stack;
    }

    public static Identity newIdentity(ResourceLocation profile, int profileRevision, UUID ownerTeam) {
        return new Identity(profile, profileRevision, UUID.randomUUID(), ownerTeam);
    }

    static void writeIdentity(ItemStack stack, Identity value) {
        CompoundTag identity = new CompoundTag();
        identity.putInt(SCHEMA, CURRENT_SCHEMA);
        identity.putString(PROFILE, value.profile().toString());
        identity.putInt(PROFILE_REVISION, value.profileRevision());
        identity.putUUID(SERIAL, value.serial());
        identity.putUUID(OWNER_TEAM, value.ownerTeam());
        stack.getOrCreateTag().put(ROOT, identity);
    }

    public static Optional<Identity> identity(ItemStack stack) {
        if (!(stack.getItem() instanceof UpgradePrototypeItem) || !stack.hasTag()) return Optional.empty();
        return readIdentityTag(stack);
    }

    static Optional<Identity> readIdentityTag(ItemStack stack) {
        if (!stack.hasTag()) return Optional.empty();
        CompoundTag root = stack.getTag().getCompound(ROOT);
        if (root.getInt(SCHEMA) != CURRENT_SCHEMA || root.getInt(PROFILE_REVISION) <= 0
                || !root.hasUUID(SERIAL) || !root.hasUUID(OWNER_TEAM)) return Optional.empty();
        ResourceLocation profile = ResourceLocation.tryParse(root.getString(PROFILE));
        if (profile == null) return Optional.empty();
        return Optional.of(new Identity(profile, root.getInt(PROFILE_REVISION),
                root.getUUID(SERIAL), root.getUUID(OWNER_TEAM)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<Identity> identity = identity(stack);
        if (identity.isEmpty()) {
            tooltip.add(Lang.translateTooltip("prototype.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        Identity value = identity.get();
        tooltip.add(Lang.translateTooltip("prototype.profile", value.profile()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Lang.translateTooltip("prototype.revision", value.profileRevision()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Lang.translateTooltip("prototype.serial", value.serial()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public record Identity(ResourceLocation profile, int profileRevision, UUID serial, UUID ownerTeam) {
    }
}
