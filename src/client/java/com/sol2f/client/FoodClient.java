package com.sol2f.client;

import com.sol2f.Gui.FoodBookScreen;
import com.sol2f.Key.KeyBindings;
import com.sol2f.network.FoodPackets;
import com.sol2f.network.S2CFoodListSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodClient implements ClientModInitializer {

    private static final Set<String> consumed = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onInitializeClient() {
        // 1. 网络包：接收已食用食物列表
        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_FOOD_LIST, (client, handler, buf, responseSender) -> {
            List<String> list = S2CFoodListSync.readList(buf);
            synchronized (consumed) {
                consumed.clear();
                consumed.addAll(list);
            }
        });

        // 2. Tooltip：标记未食用食物
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.getItem().getFoodComponent() == null) return;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (!consumed.contains(id)) {
                lines.add(Text.translatable("sol2f.tooltip.unconsumed")
                    .formatted(Formatting.AQUA)
                    .formatted(Formatting.ITALIC));
            }
        });

        // 3. 注册快捷键（必须在初始化时调用）
        KeyBindings.register();

        // 4. 监听按键事件（每帧末尾检查）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.OPEN_FOOD_BOOK.wasPressed()) {
                client.setScreen(new FoodBookScreen());
            }
        });
    }

    // 以下为公共访问方法

    public static int getConsumedCount() {// 获取已食用食物数量（给GUI用）
        synchronized (consumed) {
            return consumed.size();
        }
    }

    public static boolean isConsumed(String id) {
        return consumed.contains(id);
    }

    public static List<String> getConsumedSnapshot() {
        synchronized (consumed) {
            return new ArrayList<>(consumed);
        }
    }
}