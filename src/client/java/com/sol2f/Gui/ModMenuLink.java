package com.sol2f.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuLink implements ModMenuApi {

    /**
     * 返回生产 Cloth Config 配置页面工厂。
     */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Sol2FConfigGUI::openConfigScreen;
    }
    
}
