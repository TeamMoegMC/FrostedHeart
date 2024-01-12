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

package com.teammoeg.frostedheart.research.effects;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Effect on numerical stats of the team's machines or abilities
 */
public class EffectStats extends Effect {
    private static FHIcon addIcon = FHIcons.getDelegateIcon("plus");
    String vars;
    double val;
    boolean isPercentage = false;

    EffectStats() {
        this.vars = "";
        this.val = 0;
    }

    public EffectStats(JsonObject jo) {
        super(jo);
        vars = jo.get("vars").getAsString();
        val = jo.get("val").getAsDouble();
        if (jo.has("percent"))
            isPercentage = jo.get("percent").getAsBoolean();
    }

    public EffectStats(PacketBuffer pb) {
        super(pb);
        vars = pb.readString();
        val = pb.readDouble();
        isPercentage = pb.readBoolean();
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
    public FHIcon getDefaultIcon() {
        return addIcon;
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.stats");
    }


    @Override
    public List<ITextComponent> getDefaultTooltip() {
        List<ITextComponent> tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateGui("effect.stats." + vars));
        String vtext;
        if (isPercentage) {
            vtext = NumberFormat.getPercentInstance().format(val / 100);
        } else
            vtext = NumberFormat.getInstance().format(val);
        if (val > 0) {
            tooltip.add(new StringTextComponent("+" + vtext));
        } else
            tooltip.add(new StringTextComponent(vtext));
        return tooltip;
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        if (isload) return false;
        double var = team.getVariants().getDouble(vars);
        if (isPercentage)
            var += val / 100;
        else
            var += val;
        team.getVariants().putDouble(vars, var);
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

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("vars", vars);
        jo.addProperty("val", val);
        if (isPercentage)
            jo.addProperty("percent", true);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeString(vars);
        buffer.writeDouble(val);
        buffer.writeBoolean(isPercentage);
    }

}
