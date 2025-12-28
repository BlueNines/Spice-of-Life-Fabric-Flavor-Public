package com.sol2f.Gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.sol2f.client.FoodClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import java.util.List;
import net.minecraft.client.MinecraftClient;


public class FoodBookScreen extends Screen {

    public FoodBookScreen() {
        super(Text.translatable("sol2f.gui.food_book.title"));
    }

    // 通用基础量定义
    private static final int GUI_WIDTH = 276;// GUI宽度（实际纹理包含主卡片和侧边选择栏）
    private static final int GUI_HEIGHT = 166;// GUI高度
    private static final int MAIN_PANEL_WIDTH = 248;// 主面板宽度，不包含侧边选择栏
    private final Identifier GUI_TEXTURE = new Identifier("sol2f", "textures/gui/food_book.png");

    private int scrollPageOffset = 0;// 当前页面偏移量
    private static final int ITEMS_PER_PAGE = 96;// 每页显示的物品数量
    private ItemStack hoveredStack = ItemStack.EMPTY;// 当前悬停物品，用于绘制tooltip


    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {// 页面循环绘制逻辑

        // 绘制基准点，后续绘制计算
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        this.renderBackground(context);// 绘制半透明黑色背景

        this.hoveredStack = ItemStack.EMPTY;// m每一帧重置悬停物品

        int totalItem = FoodClient.getConsumedCount();
        int pageCount = (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;// 计算总页数，公式：总页数 = (总物品数量 + 每页物品数量 - 1) / 每页物品数量 = 总物品数量 / 每页物品数量 的向上取整
        int maxOffset = Math.max(0, (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1);// 计算最大偏移量，保证不小于0
        scrollPageOffset = Math.min(0, Math.min(scrollPageOffset, maxOffset)); // 钳制当前偏移量，保证不小于0且不大于最大偏移量，防止在物品数为0时 -1*96 导致引爆minecraft

        //绘制页面底图
        context.drawTexture(
            GUI_TEXTURE, 
            guiLeft, guiTop,
            0, 0,
            GUI_WIDTH, GUI_HEIGHT,
            276, 342// 纹理文件图总尺寸
        );

        // 绘制标题
        Text title = Text.translatable("sol2f.gui.food_book.title");
        context.drawText(
            textRenderer,
            title,
            guiLeft + 9,
            guiTop + 5,
            0x000000,
            false
        );

        // 以下绘制滑块
        int currentSliderY;// 计算当前滑块位置 ↓
        if (pageCount <= 1) {// 如果总页数小于等于1，滑块位置固定不变
            currentSliderY = 17;
        } else {
            int trackHeight = 133;// 滑块轨道高度
            currentSliderY = 17 + (trackHeight * scrollPageOffset) / (pageCount - 1);// 计算滑块位置，公式：滑块在GUI卡片中的Y坐标 = 17（底图上边距） + (滑块轨道高度 * 当前偏移量) / (总页数 - 1) (总页数-1是因为偏移量从0开始)
        }

        int sliderX = guiLeft + 232;// 滑块对于视口的X位置
        int sliderY = guiTop + currentSliderY;// 滑块对于视口的Y位置
        context.drawTexture(// 在视口上绘制滑块
            GUI_TEXTURE,
            sliderX, sliderY,
            0, 333,
            7, 9,
            276, 342
        );
        
        renderItemGrid(context, guiLeft, guiTop , mouseX , mouseY);// 绘制物品格子

        super.render(context, mouseX, mouseY, delta);

        // 以下渲染tooltip
        if(!hoveredStack.isEmpty()) {
            List<Text> tooltip = hoveredStack.getTooltip(
                MinecraftClient.getInstance().player,
                TooltipContext.Default.ADVANCED // .ADVANCED: 显示所有数据，包括额外NBT数据；.BASIC: 只显示物品名称
            );
            if (tooltip.isEmpty()) {// 如果tooltip为空，则尝试只添加物品名称
                tooltip.add(hoveredStack.getName());
                if (tooltip.isEmpty()) {// 如果tooltip任然为空，则添加一个未知物品提示
                    tooltip.add(Text.translatable("sol2f.gui.food_book.unknown_item_text"));
                }
            } else {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    private void renderItemGrid(DrawContext context, int guiLeft, int guiTop , int mouseX , int mouseY){// 绘制物品格方法
    int totalItem = FoodClient.getConsumedCount();
    List<String> consumedItems = FoodClient.getConsumedSnapshot();// 获取已食用物品列表快照备用
    int startIndex = scrollPageOffset * ITEMS_PER_PAGE;// 计算当前页起始物品索引
    for (int row = 0; row < 8; row++) {
        for (int col = 0; col < 12; col++) {
            int index = row * 12 + col;// 当前格子对应的物品索引(0~95)
            int itemIndex = startIndex + index;// 当前格子对应的全局物品索引
            // 以下计算格当前子坐标
            int slotX = guiLeft + 9 + col * 18;
            int slotY = guiTop + 16 + row * 18;
            if (itemIndex < totalItem) {// 如果当前物品索引小于总物品数量（也就是当前格子有东西），则绘制物品图标
                String itemId = consumedItems.get(itemIndex);// 获取物品ID
                try {
                    // 从ID转换为ItemStack物品实例
                    Identifier id = new Identifier(itemId);
                    Item item = Registries.ITEM.getOrEmpty(id).orElse(Items.AIR);
                    if (item != Items.AIR) {
                        ItemStack stack = new ItemStack(item);
                        context.drawItem(stack, slotX + 1, slotY + 1);// 绘制物品图标
                        context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);// 绘制物品数量（实际上没有数量）等覆盖层，但是别的mod改可能会用到
                    if (isMouseOver(slotX, slotY, mouseX, mouseY)) {
                        hoveredStack = stack;// 设置当前悬停物品，用于后续绘制tooltip
                    }
                    }
                } catch (Exception e) {
                    // 忽略不存在的ID，以防玩家删除食物模组后残留
                }
            }
            // 否则不绘制任何东西
        }
    }
    }

    // 处理鼠标点击事件
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // 绘制基准点，后续监听计算
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2; 
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        int targetLeft = guiLeft + 248;// 计算监听范围
        int targetTop = guiTop + 44;
        int targetRight = guiLeft + 271;
        int targetBottom = guiTop + 70;

        if (mouseX >= targetLeft && mouseX < targetRight && mouseY >= targetTop && mouseY < targetBottom) {// 判断鼠标是否在指定区域内
            if (button == 0) {// 0 = 左键; 1 = 右键; 2 = 中键
                MinecraftClient.getInstance().setScreen(new FoodOverviewScreen());
                return true;// 返回 true 表示事件已处理，阻止其他 GUI 元素接收此点击
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);// 否则继续传递给父类处理
    }

    private boolean isMouseOver(int slotX, int slotY , int mouseX , int mouseY) {// 辅助方法：判断鼠标是否在指定格子内}
            return mouseX >= slotX && mouseY >= slotY && mouseX < slotX + 18 && mouseY < slotY + 18;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {// 处理鼠标滚轮事件
        int totalItem = FoodClient.getConsumedCount();
        if (amount > 0) {// amount：通过系统滚动量判断滚动方向
            scrollPageOffset = Math.max(0,scrollPageOffset - 1);// 向上滚动：-1
        } else if (amount < 0) {
            int maxOffset = (totalItem + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1;// 计算最大偏移量
            scrollPageOffset = Math.min(maxOffset, scrollPageOffset + 1);// 向下滚动：+1
        }
        return true;// 消费掉滚动事件
    }
}
