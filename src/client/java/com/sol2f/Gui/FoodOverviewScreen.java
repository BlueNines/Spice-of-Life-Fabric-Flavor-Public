package com.sol2f.Gui;

import net.minecraft.client.gui.screen.Screen;

import com.sol2f.client.FoodClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FoodOverviewScreen extends Screen {
    
    public FoodOverviewScreen() {
        super(Text.translatable("screen.gui.food_overview.title"));
    }

    // 通用基础量定义
    private static final int GUI_WIDTH = 276;// GUI宽度（实际纹理包含主卡片和侧边选择栏）
    private static final int GUI_HEIGHT = 166;// GUI高度
    private static final int MAIN_PANEL_WIDTH = 248;// 主面板宽度，不包含侧边选择栏
    private final Identifier GUI_TEXTURE = new Identifier("sol2f", "textures/gui/food_book.png");

    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);// 绘制半透明黑色背景

        // 绘制基准点，便于后续绘制计算
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2; 
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        //绘制页面底图
        context.drawTexture(
            GUI_TEXTURE, 
            guiLeft, guiTop,
            0, 167,
            GUI_WIDTH, GUI_HEIGHT,
            276, 342// 纹理文件的尺寸
        );

        // 绘制标题
        Text title = Text.translatable("sol2f.gui.food_overview.title");
        context.drawText(
            textRenderer,
            title,
            guiLeft + 23,
            guiTop + 16,
            0x000000,
            false
        );

        Text OverviewText1 = Text.translatable("sol2f.gui.food_overview.text_FoodCount").formatted(Formatting.BOLD);// 绘制食物数概述标题
        context.drawText(
            textRenderer,
            OverviewText1,
            guiLeft + 40,
            guiTop + 29,
            0x000000,
            false
        );
        Text OverviewText2 = Text.translatable("sol2f.gui.food_overview.text_HealthyCount").formatted(Formatting.BOLD);// 绘制血量概述标题
        context.drawText(
            textRenderer,
            OverviewText2,
            guiLeft + 40,
            guiTop + 62,
            0x000000,
            false
        );

        int OverviewDeawWidth = 88;// 定义数字绘制区域
        Text OverviewFoodCount = Text.literal(FoodClient.getConsumedCount() + "/" + FoodClient.getAllCount());// 绘制食物数
        int FCFontWidth = textRenderer.getWidth(OverviewFoodCount);// 获取HC（OverviewFoodCount）文字宽度
        int FCDrawX = guiLeft + 23 + (OverviewDeawWidth - FCFontWidth) / 2;
        context.drawText(
            textRenderer,
            OverviewFoodCount,
            FCDrawX,
            guiTop + 44,
            0x000000,
            false
        );

        Text OverviewHealthyCount = Text.literal(FoodClient.getCurrentHealth() + "/" + FoodClient.getMaxHealth());// 绘制血量
        int HCFontWidth = textRenderer.getWidth(OverviewHealthyCount);
        int HCDrawX = guiLeft + 23 + (OverviewDeawWidth - HCFontWidth) / 2;
        context.drawText(
            textRenderer,
            OverviewHealthyCount,
            HCDrawX,
            guiTop + 76,
            0x000000,
            false
        );
    }
    // 处理鼠标点击事件
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // 绘制基准点，后续监听计算
        int guiLeft = (this.width - MAIN_PANEL_WIDTH) / 2; 
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        int targetLeft = guiLeft + 248;// 计算监听范围
        int targetTop = guiTop + 16;
        int targetRight = guiLeft + 271;
        int targetBottom = guiTop + 42;

        if (mouseX >= targetLeft && mouseX < targetRight && mouseY >= targetTop && mouseY < targetBottom) {// 判断鼠标是否在指定区域内
            if (button == 0) {// 0 = 左键; 1 = 右键; 2 = 中键
                MinecraftClient.getInstance().setScreen(new FoodBookScreen());
                return true;// 返回 true 表示事件已处理，阻止其他 GUI 元素接收此点击
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);// 否则继续传递给父类处理
    }

}

