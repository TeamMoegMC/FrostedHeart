package com.teammoeg.frostedheart.client.screen;

import com.teammoeg.frostedheart.common.container.ElectrolyzerContainer;
import com.teammoeg.frostedheart.common.tile.ElectrolyzerTile;
import electrodynamics.api.electricity.formatting.ChatFormatter;
import electrodynamics.api.electricity.formatting.ElectricUnit;
import electrodynamics.common.item.subtype.SubtypeProcessorUpgrade;
import electrodynamics.prefab.inventory.container.slot.SlotRestricted;
import electrodynamics.prefab.screen.GenericScreen;
import electrodynamics.prefab.screen.component.ScreenComponentElectricInfo;
import electrodynamics.prefab.screen.component.ScreenComponentFluid;
import electrodynamics.prefab.screen.component.ScreenComponentInfo;
import electrodynamics.prefab.screen.component.ScreenComponentSlot;
import electrodynamics.prefab.tile.GenericTile;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.ComponentElectrodynamic;
import electrodynamics.prefab.tile.components.type.ComponentFluidHandler;
import electrodynamics.prefab.tile.components.type.ComponentProcessor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ElectrolyzerScreen extends GenericScreen<ElectrolyzerContainer> {

    public ElectrolyzerScreen(ElectrolyzerContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        components.add(new ScreenComponentFluid(() -> {
            ElectrolyzerTile boiler = container.getHostFromIntArray();
            if (boiler != null) {
                ComponentFluidHandler handler = boiler.getComponent(ComponentType.FluidHandler);
                for (Fluid fluid : handler.getInputFluids()) {
                    FluidTank tank = handler.getTankFromFluid(fluid);
                    if (tank.getFluidAmount() > 0) {
                        return handler.getTankFromFluid(tank.getFluid().getFluid());
                    }
                }
            }
            return null;
        }, this, 21, 18));
        components.add(new ScreenComponentFluid(() -> {
            ElectrolyzerTile boiler = container.getHostFromIntArray();
            if (boiler != null) {
                ComponentFluidHandler handler = boiler.getComponent(ComponentType.FluidHandler);
                for (Fluid fluid : handler.getOutputFluids()) {
                    FluidTank tank = handler.getTankFromFluid(fluid);
                    if (tank.getFluidAmount() > 0) {
                        return handler.getTankFromFluid(tank.getFluid().getFluid());
                    }
                }
            }
            return null;
        }, this, 127, 18));
        components.add(new ScreenComponentElectricInfo(this::getEnergyInformation, this, -ScreenComponentInfo.SIZE + 1, 2));
    }

    @Override
    protected ScreenComponentSlot createScreenSlot(Slot slot) {
        return new ScreenComponentSlot(slot instanceof SlotRestricted && ((SlotRestricted) slot)
                .isItemValid(new ItemStack(electrodynamics.DeferredRegisters.SUBTYPEITEM_MAPPINGS.get(SubtypeProcessorUpgrade.basicspeed)))
                ? ScreenComponentSlot.EnumSlotType.SPEED
                : slot instanceof SlotRestricted ? ScreenComponentSlot.EnumSlotType.LIQUID : ScreenComponentSlot.EnumSlotType.NORMAL,
                this, slot.xPos - 1, slot.yPos - 1);
    }

    private List<? extends ITextProperties> getEnergyInformation() {
        ArrayList<ITextProperties> list = new ArrayList<>();
        GenericTile box = container.getHostFromIntArray();
        if (box != null) {
            ComponentElectrodynamic electro = box.getComponent(ComponentType.Electrodynamic);
            ComponentProcessor processor = box.getComponent(ComponentType.Processor);

            list.add(new TranslationTextComponent("gui.electrolyzer.usage",
                    new StringTextComponent(ChatFormatter.getElectricDisplayShort(processor.getUsage() * 20, ElectricUnit.WATT))
                            .mergeStyle(TextFormatting.GRAY)).mergeStyle(TextFormatting.DARK_GRAY));
            list.add(new TranslationTextComponent("gui.electrolyzer.voltage",
                    new StringTextComponent(ChatFormatter.getElectricDisplayShort(electro.getVoltage(), ElectricUnit.VOLTAGE))
                            .mergeStyle(TextFormatting.GRAY)).mergeStyle(TextFormatting.DARK_GRAY));
        }
        return list;
    }
}
