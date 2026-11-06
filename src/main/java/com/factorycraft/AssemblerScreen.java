package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Assembler screen. Click a pattern slot with an item to set it (the item stays
 * in your hand), with an empty hand to clear it.
 */
public final class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    public AssemblerScreen(AssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 172);
        this.inventoryLabelY = AssemblerMenu.INVENTORY_Y - 11;
    }

    private int patternAt(double mouseX, double mouseY) {
        int gx = (int) Math.floor((mouseX - leftPos - AssemblerMenu.PATTERN_X + 1) / 18.0);
        int gy = (int) Math.floor((mouseY - topPos - AssemblerMenu.GRID_Y + 1) / 18.0);
        if (mouseX < leftPos + AssemblerMenu.PATTERN_X - 1 || mouseY < topPos + AssemblerMenu.GRID_Y - 1) return -1;
        if (gx < 0 || gx > 2 || gy < 0 || gy > 2) return -1;
        return gy * 3 + gx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = patternAt(event.x(), event.y());
        if (slot >= 0) {
            ItemStack carried = menu.getCarried();
            ClientPacketDistributor.sendToServer(new AssemblerPatternPayload(slot, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1)));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static void slotFrame(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
        graphics.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
        graphics.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
        graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
        graphics.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFC6C6C6);
        for (Slot slot : menu.slots) slotFrame(graphics, x + slot.x - 1, y + slot.y - 1);
        // Pattern grid tinted blue: ghost items, not real ones.
        int px = x + AssemblerMenu.PATTERN_X - 1;
        int py = y + AssemblerMenu.GRID_Y - 1;
        graphics.fill(px - 2, py - 2, px + 56, py + 56, 0xFF4A6A9A);
        for (int i = 0; i < 9; i++) slotFrame(graphics, px + (i % 3) * 18, py + (i / 3) * 18);
        // Progress arrow.
        int ax = x + 126 - 2;
        int ay = y + 72;
        graphics.fill(x + 128 - 2, y + 18, x + 128, y + 66, 0xFF373737);
        int h = (int) (48 * menu.energy());
        graphics.fill(x + 128 - 2, y + 66 - h, x + 128, y + 66, 0xFFE0C040);
        graphics.fill(ax - 70, ay, ax - 70 + 60, ay + 4, 0xFF373737);
        graphics.fill(ax - 70, ay, ax - 70 + (int) (60 * menu.progress()), ay + 4, 0xFF5AC85A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int slot = patternAt(mouseX, mouseY);
        if (slot >= 0) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.factorycraft.assembler.pattern"), mouseX, mouseY);
        }
    }
}
