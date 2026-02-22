package com.sol2f.key;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding OPEN_FOOD_BOOK;

    public static void register() {
        OPEN_FOOD_BOOK = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.sol2f.open_food_book",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.sol2f.main"
            )
        );
    }
}