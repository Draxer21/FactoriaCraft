package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Reactor screen: rod in/out, fuel left, and energy / heat / water gauges. */
public final class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final int PANEL = 0xFFC6C6C6;

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    private static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
        graphics.fill(x - 1, y - 1, x, y + 16, 0xFF373737);
        graphics.fill(x, y + 16, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x + 16, y, x + 17, y + 17, 0xFFFFFFFF);
    }

    private static void gauge(GuiGraphicsExtractor graphics, int x, int y, int w, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + 8, 0xFF373737);
        graphics.fill(x, y, x + w, y + 7, 0xFF1E2124);
        graphics.fill(x, y, x + Math.round(w * fraction), y + 7, color);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.fill(x + 7, y + 14, x + 169, y + 74, PANEL);
        slot(graphics, x + 26, y + 35);
        slot(graphics, x + 62, y + 35);

        // Core between the slots: glows with the remaining fuel.
        int glow = Math.round(menu.burn() * 12);
        graphics.fill(x + 45, y + 37, x + 59, y + 51, 0xFF2A2D31);
        if (glow > 0) graphics.fill(x + 46, y + 50 - glow, x + 58, y + 50, menu.rate() > 0 ? 0xFF6CFF4A : 0xFF3C7A30);

        int gx = x + 92;
        gauge(graphics, gx, y + 26, 72, menu.energyFraction(), 0xFF3AC8D8);
        float heat = menu.heat();
        int heatColor = heat > 0.9F ? 0xFFFF2A2A : heat > 0.7F ? 0xFFFF8A1E : 0xFFE8C21C;
        gauge(graphics, gx, y + 45, 72, heat, heatColor);
        gauge(graphics, gx, y + 64, 72, menu.waterFraction(), 0xFF3A6CF0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int color = 0xFF404040;
        graphics.text(this.font, String.format("%,d E", menu.energy()), 92, 17, color, false);
        graphics.text(this.font, Component.translatable("gui.factorycraft.reactor.heat", Math.round(menu.heat() * 100)), 92, 36,
                menu.heat() > 0.7F ? 0xFFB01010 : color, false);
        graphics.text(this.font, Component.translatable("gui.factorycraft.reactor.water", menu.water() / 1000), 92, 55, color, false);
        graphics.text(this.font, Component.translatable("gui.factorycraft.reactor.rate", menu.rate()), 10, 58, color, false);
    }
}
