/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.research.research.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.chorda.util.io.registry.TypedCodecRegistry;
import com.teammoeg.frostedheart.content.research.AutoIDItem;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;


/**
 * "Effect" of research: how would it become when research is completed ?.
 *
 * @author khjxiaogu
 * file: Effect.java
 * @date 2022/09/02
 */
public abstract class Effect extends AutoIDItem {
    public static final MapCodec<BaseData> BASE_CODEC = RecordCodecBuilder.mapCodec(t ->
            t.group(
                    Codec.STRING.optionalFieldOf("name", "").forGetter(o -> o.name),
                    Codec.list(Codec.STRING).optionalFieldOf("tooltip", Arrays.asList()).forGetter(o -> o.tooltip),
                    FHIcons.CODEC.optionalFieldOf("icon").forGetter(o -> Optional.ofNullable(o.icon)),
                    Codec.STRING.fieldOf("id").forGetter(o -> o.nonce),
                    Codec.BOOL.optionalFieldOf("hidden", false).forGetter(o -> o.hidden)).apply(t, BaseData::new));
    private static TypedCodecRegistry<Effect> registry = new TypedCodecRegistry<>();
    public static final Codec<Effect> CODEC = registry.codec();

    static {
        registerEffectType(EffectBuilding.class, "multiblock", EffectBuilding.CODEC);
        registerEffectType(EffectCrafting.class, "recipe", EffectCrafting.CODEC);
        registerEffectType(EffectItemReward.class, "item", EffectItemReward.CODEC);
        registerEffectType(EffectStats.class, "stats", EffectStats.CODEC);
        registerEffectType(EffectUse.class, "use", EffectUse.CODEC);
        registerEffectType(EffectShowCategory.class, "category", EffectShowCategory.CODEC);
        registerEffectType(EffectCommand.class, "command", EffectCommand.CODEC);
        registerEffectType(EffectExperience.class, "experience", EffectExperience.CODEC);
    }

    String name = "";
    List<String> tooltip;
    FHIcon icon;
    String nonce;
    boolean hidden;

    /**
     * Instantiates a new Effect.<br>
     */
    public Effect() {
        this("", new ArrayList<>());
    }

    /**
     * Instantiates a new Effect with a JsonObject object.<br>
     *
     * @param jo the jo<br>
     */
    public Effect(BaseData data) {
        name = data.name;
        tooltip = new ArrayList<>(data.tooltip);
        icon = data.icon;
        nonce = data.nonce;
        hidden = data.hidden;
    }

    /**
     * Instantiates a new Effect.<br>
     *
     * @param name    the name<br>
     * @param tooltip the tooltip<br>
     */
    public Effect(String name, List<String> tooltip) {
        super();
        this.name = name;
        this.tooltip = tooltip;
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    /**
     * Instantiates a new Effect.<br>
     *
     * @param name    the name<br>
     * @param tooltip the tooltip<br>
     * @param icon    the icon<br>
     */
    public Effect(String name, List<String> tooltip, FHIcon icon) {
        super();
        this.name = name;
        this.tooltip = tooltip;
        this.icon = icon;
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    /**
     * Instantiates a new Effect.<br>
     *
     * @param name    the name<br>
     * @param tooltip the tooltip<br>
     * @param icon    the icon<br>
     */
    public Effect(String name, List<String> tooltip, ItemLike icon) {
        this(name, tooltip, FHIcons.getIcon(icon));
    }

    /**
     * Instantiates a new Effect.<br>
     *
     * @param name    the name<br>
     * @param tooltip the tooltip<br>
     * @param icon    the icon<br>
     */
    public Effect(String name, List<String> tooltip, ItemStack icon) {
        this(name, tooltip, FHIcons.getIcon(icon));
    }

    public static <T extends Effect> void registerEffectType(Class<T> cls, String type, MapCodec<T> json) {
        registry.register(cls, type, json);
    }

    public BaseData getBaseData() {
        BaseData bd = new BaseData(name, tooltip, icon, nonce, hidden);
//    	System.out.println(bd);
        return bd;
    }

    /**
     * Called when effect is edited.
     */
    public void edit() {
    }

    /**
     * Get brief string describe this effect for show in editor.
     *
     * @return brief<br>
     */
    public abstract String getBrief();

    /**
     * Get default icon.
     * use this when no icon is set.
     *
     * @return default icon<br>
     */
    public abstract FHIcon getDefaultIcon();

    /**
     * Get default name.
     * use this when no name is set.
     *
     * @return default name<br>
     */
    public abstract MutableComponent getDefaultName();

    /**
     * Get default tooltip.
     * use this when no tooltip is set.
     *
     * @return default tooltip<br>
     */
    public abstract List<Component> getDefaultTooltip();

    /**
     * Get icon.
     *
     * @return icon<br>
     */
    public final FHIcon getIcon() {
        if (icon == null)
            return getDefaultIcon();
        return icon;
    }

    public final Icon getFtbIcon() {
        if (icon == null)
            return getDefaultIcon().asFtbIcon();
        return icon.asFtbIcon();
    }

    /**
     * Get name.
     *
     * @return name<br>
     */
    public final MutableComponent getName() {
        if (name.isEmpty())
            return getDefaultName();
        return (MutableComponent) FHTextUtil.get(name, "effect", this::getId);
    }

    /**
     * Get nonce.
     *
     * @return nonce<br>
     */
    @Override
    public String getNonce() {
        return nonce;
    }

    public void setNonce(String text) {
        this.nonce = text;
    }

    /**
     * Get tooltip.
     *
     * @return tooltip<br>
     */
    public final List<Component> getTooltip() {
        if (tooltip.isEmpty())
            return getDefaultTooltip();
        return FHTextUtil.get(tooltip, "effect", this::getId);
    }

    /**
     * Get type of this effect.
     *
     * @return type<br>
     */
    @Override
    public final String getType() {
        return "effects";
    }

    /**
     * Grant effect to a team.<br>
     * This would be call multiple times, especially when team data loaded from disk if this effect is marked granted.
     *
     * @param team          the team<br>
     * @param triggerPlayer the player trigger the grant, null if this is not triggered by player, typically press "claim" button.<br>
     * @param isload        true if this is run when loaded from disk<br>
     * @return true, if
     */
    public abstract boolean grant(TeamDataHolder team, TeamResearchData trd, @Nullable Player triggerPlayer, boolean isload);

    /**
     * Inits this effect globally.
     * Runs when research is loaded.
     */
    public abstract void init();


    /**
     * Checks if is hidden.<br>
     *
     * @return if is hidden,true.
     */
    public boolean isHidden() {
        return hidden;
    }

    @OnlyIn(Dist.CLIENT)
    public void onClick(ResearchData data) {
    }

    public void reload() {

    }

    /**
     * This is not necessary to implement as this is just for debugging propose.
     * Called only by research command or admin tools.
     *
     * @param team the team<br>
     */
    public abstract void revoke(TeamResearchData team);

    public static record BaseData(String name, List<String> tooltip, FHIcon icon, String nonce, boolean hidden) {

        public BaseData(String name, List<String> tooltip, Optional<FHIcon> icon, String nonce, boolean hidden) {
            this(name, tooltip, icon.orElse(null), nonce, hidden);
        }

    }

}
