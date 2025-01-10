package com.teammoeg.frostedheart.content.steamenergy.creative;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

import static net.minecraft.ChatFormatting.GRAY;

/**
 * A BlockEntity that maintains a HeatEndpoint.
 * It handles default client effects and tooltips and capabilities.
 */
public class HeatBlockEntity extends SmartBlockEntity implements HeatNetworkProvider, IHaveGoggleInformation {
    HeatEndpoint endpoint = new HeatEndpoint(0, 0, 0, 0);
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> endpoint);
    public HeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float output = 0;
        float intake = 0;

        Lang.tooltip("heat_stats").forGoggles(tooltip);

        if (getNetwork() != null) {
            output = getNetwork().getTotalEndpointOutput();
            intake = getNetwork().getTotalEndpointIntake();
            Lang.translate("tooltip", "pressure")
                    .style(GRAY)
                    .forGoggles(tooltip);
        } else {
            Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        Lang.number(intake)
                .translate("generic", "unit.pressure")
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("tooltip", "pressure.intake")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        Lang.number(output)
                .translate("generic", "unit.pressure")
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("tooltip", "pressure.output")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;

    }

    @Override
    public HeatNetwork getNetwork() {
        return endpoint.getNetwork();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == FHCapabilities.HEAT_EP.capability())
            return heatcap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        heatcap.invalidate();
        super.invalidateCaps();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        endpoint.unload();
    }
}
