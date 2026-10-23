package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/**
 * Nine-row chest screen for the dump truck. Built from the vanilla 6-row chest
 * texture: the top with six rows, three more rows repeated, then the player inventory.
 */
public final class DumpTruckScreen extends AbstractContainerScreen<ChestMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROWS = DumpTruckEntity.ROWS;

    public DumpTruckScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + ROWS * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Title bar + first six rows.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, this.imageWidth, 17 + 6 * 18, 256, 256);
        // Three extra rows, copied from the slot rows of the texture.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 17 + 6 * 18, 0.0F, 17.0F, this.imageWidth, (ROWS - 6) * 18, 256, 256);
        // Player inventory.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 17 + ROWS * 18, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }
}
