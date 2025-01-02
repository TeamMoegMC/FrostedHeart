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

package com.teammoeg.frostedheart.content.steamenergy;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.lang.Lang;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Class HeatProviderManager.
 * <p>
 * Integrated manager for heat providers
 */
public class HeatNetwork implements MenuProvider, NBTSerializable {
    transient BlockEntity cur;
    transient boolean isBound;
    /**
     * All Endpoints of the network.
     */
    transient PriorityQueue<HeatEndpoint> endpoints = new PriorityQueue<>(Comparator.comparingInt(HeatEndpoint::getPriority).reversed().thenComparing(HeatEndpoint::getDistance));
    Map<BlockPos, Integer> propagated = new HashMap<>();
    /**
     * Interval ticks to re-scan network.
     */
    private transient int interval = 0;
    /**
     * Network connection handler.
     */
    private transient Consumer<HeatConnector> onConnect;
    private final HeatConnector connect = (level, pos, d) -> {
        BlockEntity te = Utils.getExistingTileEntity(level, pos);
        if (te instanceof NetworkConnector)
            ((NetworkConnector) te).tryConnectTo(this, d, 1);
        else if (te != null)
            FHCapabilities.HEAT_EP.getCapability(te, d).ifPresent(t -> t.reciveConnection(level, pos, this, d, 1));
        if (cur instanceof NetworkConnector) {
            ((NetworkConnector) cur).tryConnectTo(this, d.getOpposite(), 0);
        } else if (cur != null) {
            FHCapabilities.HEAT_EP.getCapability(cur, d.getOpposite()).ifPresent(t -> t.reciveConnection(level, cur.getBlockPos(), this, d.getOpposite(), 0));
        }

    };

    /**
     * Network current pressure: The
     */
    @Getter
    private float totalEndpointOutput;
    @Getter
    private float totalEndpointIntake;

    public HeatNetwork() {

    }

    public HeatNetwork(BlockEntity cur, Consumer<HeatConnector> con) {
        this();
        bind(cur, con);
    }

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        FHMain.LOGGER.debug("HeatNetwork save");
        nbt.put("pipes",
                SerializeUtil.toNBTList(propagated.entrySet(), (t, p) -> p.compound().putLong("pos", t.getKey().asLong()).putInt("len", t.getValue())));
        FHMain.LOGGER.debug("HeatNetwork pipes saved");
        nbt.putFloat("totalEndpointOutput", totalEndpointOutput);
        FHMain.LOGGER.debug("HeatNetwork totalEndpointOutput saved");
        nbt.putFloat("totalEndpointIntake", totalEndpointIntake);
        FHMain.LOGGER.debug("HeatNetwork totalEndpointIntake saved");
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        FHMain.LOGGER.debug("HeatNetwork load");
        propagated.clear();
        ListTag cn = nbt.getList("pipes", Tag.TAG_COMPOUND);
        for (Tag ccn : cn) {
            CompoundTag ccnbt = ((CompoundTag) ccn);
            propagated.put(BlockPos.of(ccnbt.getLong("pos")), ccnbt.getInt("len"));
        }
        FHMain.LOGGER.debug("HeatNetwork pipes loaded");
        totalEndpointOutput = nbt.getFloat("totalEndpointOutput");
        FHMain.LOGGER.debug("HeatNetwork totalEndpointOutput loaded");
        totalEndpointIntake = nbt.getFloat("totalEndpointIntake");
        FHMain.LOGGER.debug("HeatNetwork totalEndpointIntake loaded");
    }

    @Override
    public String toString() {
        return "HeatNetwork [endpoints=" + endpoints + "]";
    }

    public boolean shouldPropagate(BlockPos pos, int dist) {
        int odist = propagated.getOrDefault(pos, -1);
        if (odist > dist || odist == -1) {
            propagated.put(pos, dist);
            return true;
        }
        return false;
    }

    public void startPropagation(HeatPipeTileEntity hpte, Direction dir) {
        hpte.connectTo(dir, this, propagated.getOrDefault(hpte.getBlockPos(), -1));
    }

    public int getNetworkSize() {
        return endpoints.size() + propagated.size();
    }

    public int getNumEndpoints() {
        return endpoints.size();
    }

    public boolean addEndpoint(HeatEndpoint heatEndpoint, int dist, Level level, BlockPos pos) {
        if (endpoints.contains(heatEndpoint)) {
            if (dist < heatEndpoint.getDistance()) {
                heatEndpoint.connect(this, dist, pos, level);
                return true;
            }
        } else {
            heatEndpoint.connect(this, dist, pos, level);
            endpoints.add(heatEndpoint);
            return true;
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int p1, Inventory p2, Player p3) {
        return new HeatStatContainer(p1, p3, this);
    }

    public boolean hasBounded() {
        return isBound;
    }

    public void bind(BlockEntity cur, Consumer<HeatConnector> con) {
        isBound = true;
        this.cur = cur;
        this.onConnect = con;
    }

    public void requestUpdate() {
        interval = 10;
    }

    public void requestSlowUpdate() {
        interval = 20;
    }

    public boolean isUpdateRequested() {
        return interval >= 0;
    }

    /**
     * Tick.
     */
    public void tick(Level level) {
        // Do update if requested
        if (interval > 0) {
            interval--;
        } else if (interval == 0) {
            // Check pipes
            for (BlockPos bp : propagated.keySet()) {
                HeatPipeTileEntity hpte = FHUtils.getExistingTileEntity(level, bp, HeatPipeTileEntity.class);
                if (hpte != null) {
                    hpte.ntwk = null;
                }
            }
            // Clear pipes
            propagated.clear();
            // Clear endpoints
            for (HeatEndpoint bp : endpoints) {
                bp.clearConnection();
            }
            endpoints.clear();
            // Connect all pipes and endpoints again
            if (onConnect != null)
                onConnect.accept(connect);
            interval = -1;
        }

        // Heat accumulated this tick!
        int tlevel = 1;

        // Provide heat from the endpoints
        float accumulated = 0;
        totalEndpointOutput = 0;
        for (HeatEndpoint endpoint : endpoints) {
            if (endpoint.canProvideHeat()) {
                // logic
                float provided = endpoint.provideHeat();
                accumulated += provided;
                // fetch the highest tLevel
                tlevel = Math.max(endpoint.getTempLevel(), tlevel);
                // update display data
                endpoint.output = provided;
            }
        }

        totalEndpointOutput = accumulated;

        // Distribute heat to the endpoints
        totalEndpointIntake = 0;
        for (HeatEndpoint endpoint : endpoints) {
            while (accumulated > 0 && endpoint.canReceiveHeat()) {
                // logic
                float received = endpoint.receiveHeat(accumulated, tlevel);
                totalEndpointIntake += received;
                accumulated -= received;
                endpoint.intake = accumulated;
            }
        }

        // Process data
        endpoints.forEach(HeatEndpoint::pushData);

    }

    public void invalidate(Level l) {
        for (BlockPos bp : propagated.keySet()) {
            HeatPipeTileEntity hpte = FHUtils.getExistingTileEntity(l, bp, HeatPipeTileEntity.class);
            if (hpte != null) {
                hpte.ntwk = null;
            }
        }
        propagated.clear();
        for (HeatEndpoint bp : endpoints) {
            bp.clearConnection();
        }
        endpoints.clear();
        interval = -1;
    }

    @Override
    public Component getDisplayName() {
        return Lang.translateGui("heat_stat");
    }

    public static interface HeatConnector {
        void connect(Level l, BlockPos b, Direction d);
    }


}
