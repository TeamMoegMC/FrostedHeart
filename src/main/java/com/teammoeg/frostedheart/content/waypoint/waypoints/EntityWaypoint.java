package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.Lang;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 没写完，不要用
 */
public class EntityWaypoint extends Waypoint {
    private static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /**
     * 所有实体目标的 UUID，用于获取实体
     */
    public static Set<UUID> entityUUIDs = new LinkedHashSet<>(); // TODO
    /**
     * 原始颜色
     */
    protected int originalColor;
    /**
     * 接近目标时的颜色
     */
    public int approachColor;
    /**
     * 目标实体
     */
    public Entity entityTarget;
    /**
     * 目标实体的 UUID
     */
    public UUID targetUUID;

    public EntityWaypoint(Entity target, int color, int approachColor) {
        super(new Vec3(0, 0, 0), "entity_" + target.getStringUUID(), color);
        readEntity(target);

        this.originalColor = color;
        this.approachColor = approachColor;
    }

    public EntityWaypoint(CompoundTag nbt) {
        super(nbt);
    }

    public EntityWaypoint(FriendlyByteBuf buffer) {
        super(buffer);
    }

    public void readEntity(Entity target) {
        this.entityTarget = target;
        this.displayName = target.getDisplayName();
        this.dimension = target.level().dimension().location();
        this.targetUUID = entityTarget.getUUID();
        entityUUIDs.add(entityTarget.getUUID());
    }

    public void getEntityByUUID(UUID uuid) {
        //TODO
    }

    @Override
    public void render(GuiGraphics graphics) {
        super.render(graphics);
        focus = getDistance() <= 32;
        color = FastColor.ARGB32.lerp((float)(getDistance()-8)/24F, originalColor, approachColor);
        if (entityTarget != null) {
            displayName = entityTarget.getDisplayName();
            //实体死亡时无效化路径点
            //TODO 换成这个：Entity.isAlive()，需要判断实体是否在存档中
            valid = entityTarget instanceof LivingEntity && ((LivingEntity) entityTarget).getHealth() > 0;
        }
    }

    @Override
    public Vec2 getScreenPos() {
        if (entityTarget != null && entityTarget.isAlive()) {
            Vec3 entityPos = entityTarget.getEyePosition(ClientUtils.mc().getPartialTick());
            target = new Vec3((float)entityPos.x, (float)entityPos.y, (float)entityPos.z);
        }
        return super.getScreenPos();
    }

    @Override
    public void updateInfos() {
        super.updateInfos();

        if (entityTarget != null && entityTarget instanceof LivingEntity) {
            addInfoLine(Lang.waypoint("entity_health",
                    String.format("%.2f", ((LivingEntity)entityTarget).getHealth()),
                    String.format("%.2f", ((LivingEntity)entityTarget).getMaxHealth())).component(),
            1);
        }

        if (entityTarget != null && !entityTarget.isAlive()) {
            addInfoLine(null, -1);
            addInfoLine(Lang.waypoint("lost_target").component(), -1);
        }
    }

    @Override
    public void onClientRemove() {
        super.onClientRemove();
    }

    @Override
    public void onServerRemove() {
        super.onServerRemove(); //TODO
    }

    public void setColor(int color, int approachColor) {
        this.approachColor = approachColor;
        this.originalColor = color;
        this.color = color;
    }

    public int getColor() {
        return originalColor;
    }

    public void setTarget(Entity target) {
        entityTarget = target;
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = (JsonObject) super.serialize();
        json.addProperty("approachColor", approachColor);
        json.addProperty("target_uuid", entityTarget != null ? entityTarget.getUUID().toString() : EMPTY_UUID.toString());
        return json;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = super.serializeNBT();
        nbt.putInt("approachColor", approachColor);
        nbt.putUUID("target_uuid", entityTarget != null ? entityTarget.getUUID() : EMPTY_UUID);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        if (nbt.hasUUID("target_uuid")) {
            UUID uuid = nbt.getUUID("target_uuid");
            if (!uuid.equals(EMPTY_UUID)) {
                entityUUIDs.add(uuid);
                targetUUID = uuid;
            } else {
                targetUUID = EMPTY_UUID;
            }
        } else {
            targetUUID = EMPTY_UUID;
        }

        originalColor = nbt.getInt("color");
        approachColor = nbt.getInt("approachColor");
    }
}
