package com.teammoeg.frostedheart.content.steamenergy;

public class NetworkHolder {
    SteamEnergyNetwork sen;
    int dist;
    int counter;

    public NetworkHolder() {
    }

    public void connect(SteamEnergyNetwork sen, int dist) {
        this.counter = 10;
        this.sen = sen;
        this.dist = dist;
    }

    public void tick() {
        counter--;
        if (counter < 0) {
            this.sen = null;
            this.dist = Integer.MAX_VALUE;
        }
    }

    public float drainHeat(float val) {
        if (!isValid()) return 0;
        return sen.drainHeat(val);
    }

    public float getTemperatureLevel() {
        if (!isValid()) return 0;
        return sen.getTemperatureLevel();
    }

    public SteamEnergyNetwork getNetwork() {
        return sen;
    }

    public boolean isValid() {
        return sen != null && sen.isValid();
    }

    public int getDistance() {
        return dist;
    }

    @Override
    public String toString() {
        return "NetworkHolder [sen=" + sen + ", dist=" + dist + ", counter=" + counter + "]";
    }
}
