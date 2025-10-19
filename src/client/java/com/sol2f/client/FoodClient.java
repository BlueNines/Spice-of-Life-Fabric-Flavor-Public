package com.sol2f.client;

import com.sol2f.network.FoodPackets;
import com.sol2f.network.S2CFoodListSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.registry.Registries;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodClient implements ClientModInitializer {

    private static final Set<String> consumed = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FoodPackets.S2C_FOOD_LIST, (client, handler, buf, responseSender) -> {
            List<String> list = S2CFoodListSync.readList(buf);
            consumed.clear();
            consumed.addAll(list);
            // 移除了冗余的客户端到服务器的食物列表请求
        });

        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.getItem().getFoodComponent() == null) return;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            if (!consumed.contains(id)) {
                lines.add(Text.translatable("sol2f.tooltip.unconsumed").formatted(Formatting.AQUA).formatted(Formatting.ITALIC));
            }
        });
    }

    public static boolean isConsumed(String id) {
        return consumed.contains(id);
    }

    // 返回一个稳定的快照用于客户端屏幕渲染
    public static java.util.List<String> getConsumedSnapshot() {
        synchronized (consumed) {
            return new java.util.ArrayList<>(consumed);
        }
    }
}