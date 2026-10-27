package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Statistics terminal: total output per minute as a bar chart, then the top items with their own small charts. */
public final class StatsScreen extends Screen {
    private final StatsPayload data;

    public StatsScreen(StatsPayload data) {
        super(Component.translatable("block.factorycraft.stats_terminal"));
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void bars(GuiGraphicsExtractor graphics, int[] values, int x, int y, int w, int h, int color) {
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        int n = values.length;
        float bw = w / (float) n;
        for (int i = 0; i < n; i++) {
            int bh = Math.round(h * values[i] / (float) max);
            int x0 = x + Math.round(i * bw);
            int x1 = x + Math.round((i + 1) * bw) - 1;
            if (bh > 0) graphics.fill(x0, y + h - bh, Math.max(x0 + 1, x1), y + h, color);
        }
    }

    private static String format(long n) {
        if (n < 10_000) return Long.toString(n);
        if (n < 1_000_000) return (n / 1000) + "k";
        return String.format(java.util.Locale.ROOT, "%.1fM", n / 1_000_000.0);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int w = Math.min(380, this.width - 20);
        int x = (this.width - w) / 2;
        int top = 14;
        int rows = Math.min(data.lines().size(), Math.max(1, (this.height - 130) / 20));
        int h = 112 + Math.max(1, rows) * 20;
        graphics.fill(x - 2, top - 2, x + w + 2, top + h + 2, 0xFF3A4047);
        graphics.fill(x, top, x + w, top + h, 0xF0101418);
        graphics.centeredText(this.font, this.title, this.width / 2, top + 5, 0xFFE8A21C);
        long total = 0;
        for (int v : data.total()) total += v;
        graphics.text(this.font, Component.translatable("gui.factorycraft.stats.total", format(total)), x + 8, top + 20, 0xFFBFC7CE);
        // Total chart.
        int cx = x + 8;
        int cy = top + 34;
        int cw = w - 16;
        int ch = 60;
        graphics.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF2A2D31);
        graphics.fill(cx, cy, cx + cw, cy + ch, 0xFF15181B);
        for (int g = 1; g < 4; g++) graphics.fill(cx, cy + ch * g / 4, cx + cw, cy + ch * g / 4 + 1, 0xFF22262A);
        bars(graphics, data.total(), cx + 1, cy + 1, cw - 2, ch - 2, 0xFF4CD964);
        graphics.text(this.font, Component.translatable("gui.factorycraft.stats.axis"), cx, cy + ch + 4, 0xFF6B747C);
        // Top items.
        int y = cy + ch + 18;
        if (data.lines().isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.factorycraft.stats.empty"), x + 8, y, 0xFFBFC7CE);
            return;
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < rows; i++) {
            StatsPayload.Line line = data.lines().get(i);
            graphics.item(line.stack(), x + 8, y);
            if (mouseX >= x + 8 && mouseX < x + 24 && mouseY >= y && mouseY < y + 16) hovered = line.stack();
            graphics.text(this.font, line.stack().getHoverName(), x + 28, y + 4, 0xFFE8E8E8);
            String sum = format(line.sum());
            int barX = x + w / 2 + 10;
            int barW = w / 2 - 60;
            graphics.fill(barX, y + 1, barX + barW, y + 15, 0xFF15181B);
            bars(graphics, line.perMinute(), barX + 1, y + 2, barW - 2, 12, 0xFFE8A21C);
            graphics.text(this.font, sum, x + w - 8 - this.font.width(sum), y + 4, 0xFFBFC7CE);
            y += 20;
        }
        if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
    }
}
