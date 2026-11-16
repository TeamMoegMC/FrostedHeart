/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.sensor;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class TemperatureSensorMenu extends CBlockEntityMenu<TemperatureSensorBlockEntity> {
    // Vanilla menu properties travel as signed 16-bit values: split the IEEE float bits.
    private static final CCustomMenuSlot.MultipleDataSlotConverter<Float> TEMPERATURE_SLOT =
            new CCustomMenuSlot.MultipleDataSlotConverter<>() {
                @Override public void encode(Float value, int[] data) {
                    int bits = Float.floatToIntBits(value);
                    data[0] = bits & 0xFFFF;
                    data[1] = bits >>> 16;
                }
                @Override public Float decode(int[] data) {
                    return Float.intBitsToFloat((data[0] & 0xFFFF) | ((data[1] & 0xFFFF) << 16));
                }
                @Override public int getCount() { return 2; }
                @Override public Float getDefault() { return 0F; }
            };
    private final CDataSlot<Float> target;
    private final CDataSlot<Float> current;
    private final CDataSlot<Boolean> inverted;
    private final CDataSlot<Integer> powered;
    private final Validator validity;
    public TemperatureSensorMenu(int id, Inventory inventory, TemperatureSensorBlockEntity entity) {
        super(FHMenuTypes.TEMPERATURE_SENSOR.get(), entity, id, inventory.player, 0);
        validity = buildValidator(new Validator());
        target = TEMPERATURE_SLOT.create(this);
        current = TEMPERATURE_SLOT.create(this);
        inverted = CCustomMenuSlot.SLOT_BOOL.create(this);
        powered = CCustomMenuSlot.SLOT_INT.create(this);
        if (!getPlayer().level().isClientSide) {
            target.bind(entity::getTargetTemperature);
            current.bind(entity::getTemperature);
            inverted.bind(entity::isInverted);
            powered.bind(() -> entity.isPowered() ? 15 : 0);
        }
    }
    private int value(CDataSlot<Integer> slot) { return slot.getValue() == null ? 0 : slot.getValue(); }
    public float getTarget() { return target.getValue(); }
    public float getCurrent() { return current.getValue(); }
    public boolean isInverted() { return inverted.getValue(); }
    public int getPower() { return value(powered); }
    public void setTarget(float temperature) { sendMessage(0, temperature); }
    public void toggleInverted() { sendMessage(1, 0); }
    @Override public boolean stillValid(Player user) {
        return validity.test(user);
    }
    @Override public void receiveMessage(short id, int value) {
        if (getPlayer().level().isClientSide || !stillValid(getPlayer())) return;
        if (id == 0) blockEntity.configure(Float.intBitsToFloat(value), blockEntity.isInverted());
        else if (id == 1) blockEntity.configure(blockEntity.getTargetTemperature(), !blockEntity.isInverted());
    }
}
