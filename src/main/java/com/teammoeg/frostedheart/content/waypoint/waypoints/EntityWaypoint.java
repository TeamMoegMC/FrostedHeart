package com.teammoeg.frostedheart.content.waypoint.waypoints;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 没写完，不要用
 */
public class EntityWaypoint extends waypoint {
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
        super(new Vector3f(0, 0, 0), "entity_" + target.getCachedUniqueIdString(), color);
        readEntity(target);

        this.originalColor = color;
        this.approachColor = approachColor;
    }

    public EntityWaypoint(CompoundNBT nbt) {
        super(nbt);
    }

    public EntityWaypoint(PacketBuffer buffer) {
        super(buffer);
    }

    public void readEntity(Entity target) {
        this.entityTarget = target;
        this.displayName = target.getDisplayName();
        this.dimension = target.world.getDimensionKey().getLocation();
        this.targetUUID = entityTarget.getUniqueID();
        entityUUIDs.add(entityTarget.getUniqueID());
    }

    public void getEntityByUUID(UUID uuid) {
        //TODO
    }

    @Override
    public void render(MatrixStack ms) {
        super.render(ms);
        focus = getDistance() <= 32;
        color = FHColorHelper.blendColor(originalColor, approachColor, (float)(getDistance()-8)/24F);
        if (entityTarget != null) {
            displayName = entityTarget.getDisplayName();
            //实体死亡时无效化路径点
            //TODO 换成这个：Entity.isAlive()，需要判断实体是否在存档中
            valid = entityTarget instanceof LivingEntity && ((LivingEntity) entityTarget).getHealth() > 0;
        }
    }

    @Override
    public Vector2f getScreenPos() {
        if (entityTarget != null && entityTarget.isAlive()) {
            Vector3d entityPos = entityTarget.getClientEyePosition(ClientUtils.mc().getRenderPartialTicks());
            target = new Vector3f((float)entityPos.x, (float)entityPos.y, (float)entityPos.z);
        }
        return super.getScreenPos();
    }

    @Override
    public void updateInfos() {
        super.updateInfos();

        if (entityTarget != null && entityTarget instanceof LivingEntity) {
            addInfoLine(TranslateUtils.translateWaypoint("entity_health",
                    String.format("%.2f", ((LivingEntity)entityTarget).getHealth()),
                    String.format("%.2f", ((LivingEntity)entityTarget).getMaxHealth())),
            1);
        }

        if (entityTarget != null && !entityTarget.isAlive()) {
            addInfoLine(null, -1);
            addInfoLine(TranslateUtils.translateWaypoint("lost_target"), -1);
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
        json.addProperty("target_uuid", entityTarget != null ? entityTarget.getUniqueID().toString() : EMPTY_UUID.toString());
        return json;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = super.serializeNBT();
        nbt.putInt("approachColor", approachColor);
        nbt.putUniqueId("target_uuid", entityTarget != null ? entityTarget.getUniqueID() : EMPTY_UUID);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        super.deserializeNBT(nbt);
        if (nbt.hasUniqueId("target_uuid")) {
            UUID uuid = nbt.getUniqueId("target_uuid");
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
