package com.sol2f.Gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FoodOverviewScreen extends Screen {
    
    public FoodOverviewScreen() {
        super(Text.translatable("screen.gui.food_overview.title"));
    }

    // 通用基础量定义again
    private static final int GUI_WIDTH = 276;// GUI宽度（实际纹理包含主卡片和侧边选择栏）
    private static final int GUI_HEIGHT = 166;// GUI高度
    private static final int MAIN_PANEL_WIDTH = 248;// 主面板宽度，不包含侧边选择栏
    private final Identifier GUI_TEXTURE = new Identifier("sol2f", "textures/gui/food_book.png");

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
            276, 342// 纹理 文件 图总尺寸
        );

        // 绘制标题
        Text title = Text.translatable("sol2f.gui.food_overview.title");
        context.drawText(
            textRenderer,
            title,
            guiLeft + 9,
            guiTop + 5,
            0xFFFFFF,
            true
        );
    }
}

