package com.teammoeg.frostedheart.capability;

public interface ITempForecastCapability {
    int getRainTime();

    int getThunderTime();

    int getClearTime();

    void setRainTime(int rainTime);

    void setThunderTime(int thunderTime);

    void setClearTime(int clearTime);
}
