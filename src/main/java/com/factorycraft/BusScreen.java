package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Bus filter screen: click a filter slot with an item to add it, empty-handed to clear it. */
public final class BusScreen extends AbstractContainerScreen<BusMenu> {
    public BusScreen(BusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = BusMenu.INVENTORY_Y - 11;
    }

    private int filterAt(double mouseX, double mouseY) {
        double rx = mouseX - leftPos - BusMenu.FILTER_X + 1;
        double ry = mouseY - topPos - BusMenu.FILTER_Y + 1;
        if (rx < 0 || ry < 0) return -1;
        int gx = (int) (rx / 18);
        int gy = (int) (ry / 18);
        return gx > 2 || gy > 2 ? -1 : gy * 3 + gx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int slot = filterAt(event.x(), event.y());
        if (slot >= 0) {
            ItemStack carried = menu.getCarried();
            ClientPacketDistributor.sendToServer(new AssemblerPatternPayload(slot, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1)));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
        graphics.fill(x + BusMenu.FILTER_X - 3, y + BusMenu.FILTER_Y - 3, x + BusMenu.FILTER_X + 55, y + BusMenu.FILTER_Y + 55, 0xFF4A6A9A);
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (filterAt(mouseX, mouseY) >= 0) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.factorycraft.bus.filter"), mouseX, mouseY);
        }
    }
}
