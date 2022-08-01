package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHEffectProgressSyncPacket;
import com.teammoeg.frostedheart.research.*;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * "Effect" of an research: how would it becomes when a research is completed ?
 */
public abstract class Effect extends AutoIDItem implements Writeable {
    String name = "";
    String nonce;
    List<String> tooltip;
    public Supplier<Research> parent;
    FHIcon icon;
    boolean hidden;

    // Init globally
    public abstract void init();

    public abstract boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload);

    /**
     * This is not necessary to implement as this is just for debugging propose
     */
    public abstract void revoke(TeamResearchData team);

    public void sendProgressPacket(Team team) {
        FHEffectProgressSyncPacket packet = new FHEffectProgressSyncPacket(team.getId(), this);
        for (ServerPlayerEntity spe : team.getOnlineMembers())
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
    }

    public Effect(JsonObject jo) {
        if (jo.has("name"))
            name = jo.get("name").getAsString();
        if (jo.has("tooltip"))
            tooltip = SerializeUtil.parseJsonElmList(jo.get("tooltip"), JsonElement::getAsString);
        else
            tooltip = new ArrayList<>();
        if (jo.has("icon"))
            icon = FHIcons.getIcon(jo.get("icon"));
        nonce = jo.get("id").getAsString();
        if (jo.has("hidden"))
            hidden = jo.get("hidden").getAsBoolean();
    }

    Effect(PacketBuffer pb) {
        name = pb.readString();
        tooltip = SerializeUtil.readList(pb, PacketBuffer::readString);
        icon = SerializeUtil.readOptional(pb, FHIcons::readIcon).orElse(null);
        nonce = pb.readString();
        hidden = pb.readBoolean();
    }

    public Effect(String name, List<String> tooltip, FHIcon icon) {
        super();
        this.name = name;
        this.tooltip = tooltip;
        this.icon = icon;
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    public Effect(String name, List<String> tooltip, ItemStack icon) {
        this(name, tooltip, FHIcons.getIcon(icon));
    }

    public Effect(String name, List<String> tooltip, IItemProvider icon) {
        this(name, tooltip, FHIcons.getIcon(icon));
    }

    public Effect(String name, List<String> tooltip) {
        super();
        this.name = name;
        this.tooltip = tooltip;
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    public Effect() {
        this("", new ArrayList<>());
    }

    public final FHIcon getIcon() {
        if (icon == null)
            return getDefaultIcon();
        return icon;
    }

    public final IFormattableTextComponent getName() {
        if (name.isEmpty())
            return getDefaultName();
        return (IFormattableTextComponent) FHTextUtil.get(name, "effect", this::getLId);
    }

    public final List<ITextComponent> getTooltip() {
        if (tooltip.isEmpty())
            return getDefaultTooltip();
        return FHTextUtil.get(tooltip, "effect", this::getLId);
    }

    public abstract FHIcon getDefaultIcon();

    public abstract IFormattableTextComponent getDefaultName();

    public abstract List<ITextComponent> getDefaultTooltip();

    public abstract String getId();

    public abstract int getIntID();

    @Override
    public JsonObject serialize() {
        JsonObject jo = new JsonObject();
        jo.addProperty("type", getId());
        if (!name.isEmpty())
            jo.addProperty("name", name);
        if (!tooltip.isEmpty())
            jo.add("tooltip", SerializeUtil.toJsonStringList(tooltip, e -> e));
        if (icon != null)
            jo.add("icon", icon.serialize());
        jo.addProperty("id", nonce);
        if (isHidden())
            jo.addProperty("hidden", true);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(getIntID());
        buffer.writeString(name);
        SerializeUtil.writeList2(buffer, tooltip, PacketBuffer::writeString);
        SerializeUtil.writeOptional(buffer, icon, FHIcon::write);
        buffer.writeString(nonce);
        buffer.writeBoolean(isHidden());
    }

    @Override
    public final String getType() {
        return "effects";
    }

    private void deleteInTree() {
        ResearchDataManager.INSTANCE.getAllData().forEach(t -> {
            revoke(t);
            t.setGrant(this, false);
        });
    }

    public void edit() {
        deleteInTree();
    }

    public void deleteSelf() {
        deleteInTree();
        FHResearch.effects.remove(this);
    }

    public void delete() {
        deleteSelf();
        if (parent != null) {
            Research r = parent.get();
            if (r != null) {
                r.getEffects().remove(this);
            }
        }
    }

    void setNewId(String id) {
        if (!id.equals(this.nonce)) {
            delete();
            this.nonce = id;
            FHResearch.effects.register(this);
            if (parent != null) {
                Research r = parent.get();
                if (r != null) {
                    r.attachEffect(this);
                    r.doIndex();
                }
            }
        }
    }

    public boolean isGranted() {
        return ClientResearchDataAPI.getData().isEffectGranted(this);
    }

    public void setGranted(boolean b) {
        ClientResearchDataAPI.getData().setGrant(this, b);
    }

    public abstract String getBrief();

    @Override
    public String getNonce() {
        return nonce;
    }

    public boolean isHidden() {
        return hidden;
    }
}
