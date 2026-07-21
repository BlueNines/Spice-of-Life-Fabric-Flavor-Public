package com.sol2f.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

import com.sol2f.SpiceOfLifeFabricFlavorClient;

public class FoodBookScreen extends Screen {

    public FoodBookScreen() {
        super(Text.translatable("sol2f.gui.food_book.title"));
    }

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;
    private static final int MAIN_PANEL_WIDTH = 248;
    private final Identifier GUI_TEXTURE = new Identifier("sol2f", "textures/gui/food_book.png");

    private int scrollPageOffset = 0;
    private static final int ITEMS_PER_PAGE = 96;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 首先渲染背景（必须是第一行！）
        this.renderBackground(context);

        // 计算 GUI 位置
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // 重置悬停物品
        this.hoveredStack = ItemStack.EMPTY;

        // 计算分页
        int totalItem = SpiceOfLifeFabricFlavorClient.getConsumedCount();
        int pageCount = Math.max(1, (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int maxOffset = pageCount - 1;
        scrollPageOffset = Math.min(0, Math.max(-maxOffset, scrollPageOffset));

        // 绘制 GUI 纹理
        context.drawTexture(
            GUI_TEXTURE,
            guiLeft, guiTop,
            0, 0,
            GUI_WIDTH, GUI_HEIGHT,
            276, 342
        );

        // 绘制标题
        Text title = Text.translatable("sol2f.gui.food_book.title");
        context.drawText(textRenderer, title, guiLeft + 9, guiTop + 5, 0x000000, false);

        // 绘制滑块
        int currentSliderY = (pageCount <= 1) ? 17 : 17 + (133 * (-scrollPageOffset)) / (pageCount - 1);
        context.drawTexture(GUI_TEXTURE, guiLeft + 232, guiTop + currentSliderY, 0, 333, 7, 9, 276, 342);

        // 绘制物品格子
        renderItemGrid(context, guiLeft, guiTop, mouseX, mouseY);

        // 渲染 Tooltip
        if (!hoveredStack.isEmpty()) {
            List<Text> tooltip = Screen.getTooltipFromItem(MinecraftClient.getInstance(), hoveredStack);
            if (tooltip.isEmpty()) {
                tooltip.add(hoveredStack.getName());
                if (tooltip.isEmpty()) {
                    tooltip.add(Text.translatable("sol2f.gui.food_book.unknown_item_text"));
                }
            }
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
    private void renderItemGrid(DrawContext context, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int totalItem = SpiceOfLifeFabricFlavorClient.getConsumedCount();
        List<String> consumedItems = SpiceOfLifeFabricFlavorClient.getConsumedSnapshot();
        int startIndex = -scrollPageOffset * ITEMS_PER_PAGE;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 12; col++) {
                int index = row * 12 + col;
                int itemIndex = startIndex + index;
                int slotX = guiLeft + 9 + col * 18;
                int slotY = guiTop + 16 + row * 18;

                if (itemIndex < totalItem) {
                    String itemId = consumedItems.get(itemIndex);
                    try {
                        Identifier id = new Identifier(itemId);
                        Optional<Item> itemOpt = Registries.ITEM.getOrEmpty(id);
                        if (itemOpt.isPresent()) {
                            ItemStack stack = new ItemStack(itemOpt.get());
                            context.drawItem(stack, slotX + 1, slotY + 1);
                            context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);
                            if (isMouseOver(slotX, slotY, mouseX, mouseY)) {
                                hoveredStack = stack;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore invalid IDs (e.g., mod removed)
                    }
                }
            }
        }
}
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        int targetLeft = guiLeft + 248;
        int targetTop = guiTop + 44;
        int targetRight = guiLeft + 271;
        int targetBottom = guiTop + 70;

        if (mouseX >= targetLeft && mouseX < targetRight && mouseY >= targetTop && mouseY < targetBottom) {
            if (button == 0) {
                MinecraftClient.getInstance().setScreen(new FoodOverviewScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseY >= slotY && mouseX < slotX + 18 && mouseY < slotY + 18;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int totalItem = SpiceOfLifeFabricFlavorClient.getConsumedCount();
        int maxOffset = Math.max(0, (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1);

        if (amount > 0) {
            scrollPageOffset = Math.min(0, scrollPageOffset + 1);
        } else if (amount < 0) {
            scrollPageOffset = Math.max(-maxOffset, scrollPageOffset - 1);
        }
        return true;
    }
}
