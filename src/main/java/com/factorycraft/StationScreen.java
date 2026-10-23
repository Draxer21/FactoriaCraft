package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The control station's machine list. Mouse wheel scrolls. */
public final class StationScreen extends Screen {
    private static final int ROW = 12;
    private final StationDataPayload data;
    private int scroll;

    public StationScreen(StationDataPayload data) {
        super(Component.translatable("block.factorycraft.control_station"));
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 70) / ROW);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int max = Math.max(0, data.lines().size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int w = Math.min(360, this.width - 20);
        int x = (this.width - w) / 2;
        int top = 20;
        int rows = Math.min(visibleRows(), data.lines().size());
        int h = 34 + Math.max(1, rows) * ROW;
        graphics.fill(x - 2, top - 2, x + w + 2, top + h + 2, 0xFF3A4047);
        graphics.fill(x, top, x + w, top + h, 0xF0101418);
        graphics.centeredText(this.font, this.title, this.width / 2, top + 5, 0xFFE8A21C);
        int y = top + 20;
        graphics.text(this.font, Component.translatable("gui.factorycraft.station.header"), x + 6, y, 0xFF9AA3AB);
        y += ROW;
        if (data.lines().isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.factorycraft.station.empty"), x + 6, y, 0xFFBFC7CE);
            return;
        }
        for (int i = scroll; i < Math.min(data.lines().size(), scroll + rows); i++) {
            StationDataPayload.Line line = data.lines().get(i);
            graphics.fill(x + 6, y + 1, x + 12, y + 7, line.on() ? 0xFF4CD964 : 0xFF6B6B6B);
            graphics.text(this.font, Component.translatable(line.name()), x + 16, y, 0xFFE8E8E8);
            int bar = x + w / 2 - 10;
            graphics.fill(bar, y + 1, bar + 50, y + 7, 0xFF2A2D31);
            int color = line.level() > 50 ? 0xFF4CD964 : line.level() > 20 ? 0xFFE8A21C : 0xFFD9443A;
            graphics.fill(bar, y + 1, bar + line.level() / 2, y + 7, color);
            graphics.text(this.font, line.level() + "%", bar + 54, y, 0xFFBFC7CE);
            String extra = (line.fill() >= 0 ? line.fill() + "%  " : "") + line.x() + " " + line.y() + " " + line.z();
            graphics.text(this.font, extra, x + w - 6 - this.font.width(extra), y, 0xFF9AA3AB);
            y += ROW;
        }
    }
}
