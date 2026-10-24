package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** Generator screen: fuel slot with flame on the left, a big energy gauge on the right. */
public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int PANEL = 0xFFC6C6C6;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        // Hide the furnace's input slot, arrow and output.
        graphics.fill(x + 54, y + 15, x + 74, y + 35, PANEL);
        graphics.fill(x + 78, y + 15, x + 150, y + 70, PANEL);

        if (menu.isSolar()) {
            // Sun: brighter and bigger the stronger the light.
            int cx = x + 63;
            int cy = y + 43;
            int r = 2 + Math.round(4 * menu.burn());
            int color = menu.rate() > 0 ? 0xFFFFD23A : 0xFF6B6B6B;
            graphics.fill(cx - r, cy - r, cx + r, cy + r, color);
            if (menu.rate() > 0) {
                graphics.fill(cx - 1, cy - r - 3, cx + 1, cy - r - 1, color);
                graphics.fill(cx - 1, cy + r + 1, cx + 1, cy + r + 3, color);
                graphics.fill(cx - r - 3, cy - 1, cx - r - 1, cy + 1, color);
                graphics.fill(cx + r + 1, cy - 1, cx + r + 3, cy + 1, color);
            }
        } else if (menu.isBurning()) {
            int h = Mth.ceil(menu.burn() * 13.0F) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_SPRITE, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
        }

        // Energy gauge.
        int gx = x + 90;
        int gy = y + 18;
        int gw = 56;
        int gh = 12;
        graphics.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, 0xFF373737);
        graphics.fill(gx, gy, gx + gw, gy + gh, 0xFF1E2124);
        graphics.fill(gx, gy, gx + Math.round(gw * menu.energy()), gy + gh, 0xFF3AC8D8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        String value = String.format("%,d E", menu.energyValue());
        graphics.text(this.font, value, 90, 34, 0xFF404040, false);
        Component rate = menu.isSolar()
                ? Component.translatable(menu.rate() > 0 ? "gui.factorycraft.solar.rate" : "gui.factorycraft.solar.dark", menu.rate())
                : Component.translatable("gui.factorycraft.generator.rate", GeneratorBlockEntity.OUTPUT);
        graphics.text(this.font, rate, 90, 46, 0xFF404040, false);
    }
}
