package com.sol2f.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "spice-of-life-fabric-flavor-client")
public class SOL2FClientConfig implements ConfigData {
    public GUISetting GUISetting = new GUISetting();

    public static class GUISetting {
        public boolean ShowUnconsumedTooltips = true;
        public boolean ShowConsumedTooltips = true;
    }
}