package com.teammoeg.frostedheart.capability;

public class TempForecastCapability implements ITempForecastCapability {

    private int rainTime, thunderTime, clearTime;

    public TempForecastCapability(int r, int t, int c) {
        this.rainTime = r;
        this.thunderTime = t;
        this.clearTime = c;
    }

    @Override
    public int getRainTime() {
        return rainTime;
    }

    @Override
    public int getThunderTime() {
        return thunderTime;
    }

    @Override
    public int getClearTime() {
        return clearTime;
    }

    @Override
    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public void setClearTime(int clearTime) {
        this.clearTime = clearTime;
    }
}
