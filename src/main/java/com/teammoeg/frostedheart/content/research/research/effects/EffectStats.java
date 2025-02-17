/*
 * Copyright (c) 2024 TeamMoeg
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
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {
    public static final MapCodec<EffectStats> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(Effect.BASE_CODEC.forGetter(Effect::getBaseData),
            Codec.STRING.fieldOf("vars").forGetter(o -> o.vars),
            Codec.DOUBLE.fieldOf("val").forGetter(o -> o.val),
            Codec.BOOL.fieldOf("percent").forGetter(o -> o.isPercentage)
    ).apply(t, EffectStats::new));
    private static CIcon addIcon = CIcons.getDelegateIcon("plus");
    String vars;
    double val;
    boolean isPercentage = false;
    public EffectStats(BaseData data, String vars, double val, boolean isPercentage) {
        super(data);
        this.vars = vars;
        this.val = val;
        this.isPercentage = isPercentage;
    }

    EffectStats() {
        this.vars = "";
        this.val = 0;
    }

    public EffectStats(String vars, double add) {
        super();

        val = add;
        this.vars = vars;


    }

    @Override
    public String getBrief() {
        return "Stat " + vars + " += " + val;
    }

    @Override
    public CIcon getDefaultIcon() {
        return addIcon;
    }

    @Override
    public MutableComponent getDefaultName() {
        return Lang.translateGui("effect.stats");
    }


    @Override
    public List<Component> getDefaultTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Lang.translateGui("effect.stats." + vars));
        String vtext;
        if (isPercentage) {
            vtext = NumberFormat.getPercentInstance().format(val / 100);
        } else
            vtext = NumberFormat.getInstance().format(val);
        if (val > 0) {
            tooltip.add(Components.str("+" + vtext));
        } else
            tooltip.add(Components.str(vtext));
        return tooltip;
    }

    @Override
    public boolean grant(TeamDataHolder team, TeamResearchData trd, Player triggerPlayer, boolean isload) {
        if (isload) return false;
        double var = trd.getVariants().getDouble(vars);
        if (isPercentage)
            var += val / 100;
        else
            var += val;
        trd.getVariants().putDouble(vars, var);
        return true;
    }

    @Override
    public void init() {

    }

    @Override
    public void revoke(TeamResearchData team) {
        double var = team.getVariants().getDouble(vars);
        if (isPercentage)
            var -= val / 100;
        else
            var -= val;
        team.getVariants().putDouble(vars, var);
    }

}
