package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Industrial chest screen, drawn in the vanilla container style. */
public final class IndustrialChestScreen extends AbstractContainerScreen<IndustrialChestMenu> {
    public IndustrialChestScreen(IndustrialChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IndustrialChestMenu.WIDTH, IndustrialChestMenu.HEIGHT);
        this.inventoryLabelX = IndustrialChestMenu.INV_X + 1;
        this.inventoryLabelY = IndustrialChestMenu.INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            graphics.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
            graphics.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
            graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
            graphics.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
        }
    }
}
