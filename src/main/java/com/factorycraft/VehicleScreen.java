package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Machine screen: chest-style storage (built from the vanilla chest texture,
 * any number of rows), and a side panel with the module slots and the fuel gauge.
 */
public final class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int PANEL_WIDTH = 30;
    private static final int PANEL_COLOR = 0xFFC6C6C6;

    public VehicleScreen(VehicleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + menu.rows() * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        int rows = menu.rows();

        // Header, then the storage rows (the texture has six, so draw them in chunks), then the inventory.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, this.imageWidth, 17, 256, 256);
        int drawn = 0;
        while (drawn < rows) {
            int chunk = Math.min(6, rows - drawn);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 17 + drawn * 18, 0.0F, 17.0F, this.imageWidth, chunk * 18, 256, 256);
            drawn += chunk;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 17 + rows * 18, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);

        // Module panel on the left.
        int px = x - PANEL_WIDTH;
        int ph = 18 + VehicleMenu.MODULE_SLOTS * 18 + 62;
        graphics.fill(px - 1, y - 1, x, y + ph + 1, 0xFF373737);
        graphics.fill(px, y, x, y + ph, PANEL_COLOR);
        for (int i = 0; i < VehicleMenu.MODULE_SLOTS; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x - 22, y + 17 + i * 18, 7.0F, 17.0F, 18, 18, 256, 256);
        }

        // Fuel gauge under the module slots.
        VehicleEngine engine = menu.engine();
        if (engine != null) {
            int gx = px + 9;
            int gy = y + 18 + VehicleMenu.MODULE_SLOTS * 18 + 6;
            int gh = 48;
            graphics.fill(gx - 1, gy - 1, gx + 11, gy + gh + 1, 0xFF373737);
            graphics.fill(gx, gy, gx + 10, gy + gh, 0xFF1E2124);
            float fuel = engine.getFuelFraction();
            int fill = Math.round(gh * fuel);
            int color = engine.isFusion() ? 0xFF3AC8D8 : fuel > 0.5F ? 0xFF4CD964 : fuel > 0.2F ? 0xFFE8A21C : 0xFFD9443A;
            graphics.fill(gx, gy + gh - fill, gx + 10, gy + gh, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(this.font, Component.translatable("gui.factorycraft.modules"), -PANEL_WIDTH + 2, 6, 0xFF404040, false);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean inPanel = mx >= xo - PANEL_WIDTH && mx < xo && my >= yo && my < yo + 18 + VehicleMenu.MODULE_SLOTS * 18 + 62;
        return !inPanel && super.hasClickedOutside(mx, my, xo, yo);
    }
}
