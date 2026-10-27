package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Storage terminal screen: a searchable grid of everything on the network.
 * Left-click takes a stack, right-click half a stack (or one more), shift-click
 * sends a stack straight to your inventory; clicking with an item puts it in
 * (right-click: just one). The mouse wheel scrolls.
 */
public final class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {
    private static final int COLUMNS = StorageTerminalMenu.COLUMNS;
    private static final int ROWS = StorageTerminalMenu.ROWS;
    private EditBox search;
    private int scroll;
    private final List<TerminalListPayload.Entry> shown = new ArrayList<>();

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StorageTerminalMenu.WIDTH, StorageTerminalMenu.HEIGHT);
        this.inventoryLabelX = StorageTerminalMenu.GRID_X + 9;
        this.inventoryLabelY = StorageTerminalMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(this.font, leftPos + 80, topPos + 5, 106, 12, Component.translatable("gui.factorycraft.terminal.search"));
        search.setMaxLength(40);
        search.setHint(Component.translatable("gui.factorycraft.terminal.search"));
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);
    }

    private void filter() {
        shown.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (TerminalListPayload.Entry entry : TerminalListPayload.latest.entries()) {
            if (query.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString().contains(query)) {
                shown.add(entry);
            }
        }
        shown.sort((a, b) -> Long.compare(b.count(), a.count()));
        int maxScroll = Math.max(0, (shown.size() + COLUMNS - 1) / COLUMNS - ROWS);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private static String format(long count) {
        if (count < 1000) return Long.toString(count);
        if (count < 1_000_000) return String.format(Locale.ROOT, count < 10_000 ? "%.1fk" : "%.0fk", count / 1000.0);
        return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
    }

    /** Index into {@link #shown} under the mouse, or -1. */
    private int entryAt(double mouseX, double mouseY) {
        int gx = (int) Math.floor((mouseX - leftPos - StorageTerminalMenu.GRID_X) / 18.0);
        int gy = (int) Math.floor((mouseY - topPos - StorageTerminalMenu.GRID_Y) / 18.0);
        if (mouseX < leftPos + StorageTerminalMenu.GRID_X || mouseY < topPos + StorageTerminalMenu.GRID_Y) return -1;
        if (gx < 0 || gx >= COLUMNS || gy < 0 || gy >= ROWS) return -1;
        int index = (scroll + gy) * COLUMNS + gx;
        return index < shown.size() ? index : -1;
    }

    private boolean inGrid(double mouseX, double mouseY) {
        double x = mouseX - leftPos - StorageTerminalMenu.GRID_X;
        double y = mouseY - topPos - StorageTerminalMenu.GRID_Y;
        return x >= 0 && y >= 0 && x < COLUMNS * 18 && y < ROWS * 18;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inGrid(event.x(), event.y())) {
            boolean single = event.button() == 1;
            if (!menu.getCarried().isEmpty()) {
                ClientPacketDistributor.sendToServer(new TerminalActionPayload(TerminalActionPayload.INSERT_CARRIED,
                        net.minecraft.world.item.ItemStack.EMPTY, single, false));
                return true;
            }
            int index = entryAt(event.x(), event.y());
            if (index >= 0) {
                ClientPacketDistributor.sendToServer(new TerminalActionPayload(TerminalActionPayload.EXTRACT,
                        shown.get(index).stack(), single, event.hasShiftDown()));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search != null && search.isFocused() && event.key() != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, (shown.size() + COLUMNS - 1) / COLUMNS - ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        filter();
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
        graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFC6C6C6);
        // Network grid.
        int gx = x + StorageTerminalMenu.GRID_X;
        int gy = y + StorageTerminalMenu.GRID_Y;
        graphics.fill(gx - 1, gy - 1, gx + COLUMNS * 18 + 1, gy + ROWS * 18 + 1, 0xFF373737);
        graphics.fill(gx, gy, gx + COLUMNS * 18, gy + ROWS * 18, 0xFF8B8B8B);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int sx = gx + col * 18;
                int sy = gy + row * 18;
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF9C9C9C);
                int index = (scroll + row) * COLUMNS + col;
                if (index >= shown.size()) continue;
                TerminalListPayload.Entry entry = shown.get(index);
                graphics.item(entry.stack(), sx + 1, sy + 1);
            }
        }
        // Scroll bar.
        int rows = Math.max(1, (shown.size() + COLUMNS - 1) / COLUMNS);
        int barX = gx + COLUMNS * 18 + 3;
        graphics.fill(barX, gy, barX + 6, gy + ROWS * 18, 0xFF373737);
        int knob = Math.max(8, ROWS * 18 * Math.min(ROWS, rows) / rows);
        int maxScroll = Math.max(1, rows - ROWS);
        int knobY = gy + (ROWS * 18 - knob) * Math.min(scroll, maxScroll) / maxScroll;
        graphics.fill(barX + 1, knobY, barX + 5, knobY + knob, 0xFFC6C6C6);
        // Player inventory slots.
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
        int gx = leftPos + StorageTerminalMenu.GRID_X;
        int gy = topPos + StorageTerminalMenu.GRID_Y;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scroll + row) * COLUMNS + col;
                if (index >= shown.size()) continue;
                String text = format(shown.get(index).count());
                int sx = gx + col * 18;
                int sy = gy + row * 18;
                graphics.text(this.font, text, sx + 18 - this.font.width(text), sy + 10, 0xFFFFFFFF);
            }
        }
        int hovered = entryAt(mouseX, mouseY);
        if (hovered >= 0 && menu.getCarried().isEmpty()) {
            int col = hovered % COLUMNS;
            int row = hovered / COLUMNS - scroll;
            int sx = gx + col * 18;
            int sy = gy + row * 18;
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF);
            graphics.setTooltipForNextFrame(this.font, shown.get(hovered).stack(), mouseX, mouseY);
        }
        String info = Component.translatable("gui.factorycraft.terminal.info", TerminalListPayload.latest.inventories(),
                TerminalListPayload.latest.entries().size()).getString();
        graphics.text(this.font, info, leftPos + 8, topPos + 20, 0xFF404040);
    }
}
