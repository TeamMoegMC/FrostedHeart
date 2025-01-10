package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.core.BlockPos;

import java.util.Collection;

public class ClientHeatNetworkData {
    public boolean invalid;
    public BlockPos pos;
    public float totalEndpointOutput;
    public float totalEndpointIntake;
    public Collection<HeatEndpoint> endpoints;

    public ClientHeatNetworkData(BlockPos pos) {
        this.pos = pos;
        this.totalEndpointOutput = 0;
        this.totalEndpointIntake = 0;
        this.endpoints = null;
        this.invalid = true;
    }

    public ClientHeatNetworkData(BlockPos pos, HeatNetwork network) {
        this.pos = pos;
        this.totalEndpointOutput = network.getTotalEndpointOutput();
        this.totalEndpointIntake = network.getTotalEndpointIntake();
        this.endpoints = network.getEndpoints();
        this.invalid = false;
    }

    public ClientHeatNetworkData(BlockPos pos, float totalEndpointOutput, float totalEndpointIntake, Collection<HeatEndpoint> endpoints) {
        this.pos = pos;
        this.totalEndpointOutput = totalEndpointOutput;
        this.totalEndpointIntake = totalEndpointIntake;
        this.endpoints = endpoints;
        this.invalid = false;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ClientHeatNetworkData)) {
            return false;
        } else {
            ClientHeatNetworkData that = (ClientHeatNetworkData)o;
            if (Float.compare(that.totalEndpointOutput, this.totalEndpointOutput) != 0) {
                return false;
            } else if (Float.compare(that.totalEndpointIntake, this.totalEndpointIntake) != 0) {
                return false;
            } else {
                return this.endpoints != null ? this.endpoints.equals(that.endpoints) : that.endpoints == null;
            }
        }
    }

    public boolean invalid() {
        return this.invalid;
    }
}
