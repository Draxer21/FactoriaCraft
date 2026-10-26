package com.factorycraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** Furnace-looking screen for the crusher and industrial furnace, with a 2x2 output grid. */
public final class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    /** Light grey of vanilla GUI panels, used to paint over the furnace's single big output slot. */
    private static final int PANEL = 0xFFC6C6C6;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
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

        if (menu.isBig()) {
            extractBig(graphics, x, y);
            return;
        }

        // Replace the big single output slot with a 2x2 grid of normal slots.
        graphics.fill(x + 108, y + 22, x + 152, y + 66, PANEL);
        for (int i = 0; i < Math.min(menu.outputs(), ProcessingMenu.OUTPUT_POS.length); i++) {
            int[] pos = ProcessingMenu.OUTPUT_POS[i];
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + pos[0] - 1, y + pos[1] - 1, 55.0F, 16.0F, 18, 18, 256, 256);
        }

        // Extra input slots (the furnace texture only has one, at 56,17).
        int[][] inputs = ProcessingMenu.INPUT_POS[menu.inputs() - 1];
        if (menu.inputs() > 1) {
            graphics.fill(x + 36, y + 15, x + 94, y + 35, PANEL);
            for (int[] pos : inputs) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + pos[0] - 1, y + pos[1] - 1, 55.0F, 16.0F, 18, 18, 256, 256);
            }
        }

        if (menu.isPowered()) {
            // Energy bar where the furnace has its fuel slot and flame.
            graphics.fill(x + 54, y + 34, x + 74, y + 72, PANEL);
            graphics.fill(x + 57, y + 37, x + 71, y + 69, 0xFF373737);
            graphics.fill(x + 58, y + 38, x + 70, y + 68, 0xFF1E2124);
            int h = Math.round(menu.energy() * 30.0F);
            int color = menu.energy() > 0.25F ? 0xFF3AC8D8 : 0xFFD9443A;
            graphics.fill(x + 58, y + 68 - h, x + 70, y + 68, color);
        } else if (menu.hasFuel()) {
            if (menu.isBurning()) {
                int h = Mth.ceil(menu.fuel() * 13.0F) + 1;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_SPRITE, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
            }
        } else {
            // No fuel: hide the fuel slot and flame of the furnace texture.
            graphics.fill(x + 54, y + 34, x + 74, y + 72, PANEL);
        }

        int w = Mth.ceil(menu.progress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_SPRITE, 24, 16, 0, 0, x + 79, y + 34, w, 16);
    }

    /** Big machines: 3x2 inputs, arrow, 4x2 outputs; energy bar under the inputs when powered. */
    private void extractBig(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 7, y + 14, x + 169, y + 76, PANEL);
        for (int[] pos : ProcessingMenu.INPUT_POS[ProcessingMenu.BIG_INPUTS - 1]) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + pos[0] - 1, y + pos[1] - 1, 55.0F, 16.0F, 18, 18, 256, 256);
        }
        for (int[] pos : ProcessingMenu.BIG_OUTPUT_POS) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + pos[0] - 1, y + pos[1] - 1, 55.0F, 16.0F, 18, 18, 256, 256);
        }
        if (menu.isPowered()) {
            graphics.fill(x + 13, y + 57, x + 67, y + 65, 0xFF373737);
            graphics.fill(x + 14, y + 58, x + 66, y + 64, 0xFF1E2124);
            int w = Math.round(menu.energy() * 52.0F);
            graphics.fill(x + 14, y + 58, x + 14 + w, y + 64, menu.energy() > 0.25F ? 0xFF3AC8D8 : 0xFFD9443A);
        }
        // Empty arrow from the furnace texture, then the progress on top.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 71, y + 34, 79.0F, 34.0F, 24, 17, 256, 256);
        int w = Mth.ceil(menu.progress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_SPRITE, 24, 16, 0, 0, x + 71, y + 34, w, 16);
    }
}
