package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The FactoryCraft manual: an index of chapters (one per machine or system)
 * and paged text for each. Texts live in the language files
 * (guide.factorycraft.<chapter>.title / .text).
 */
public final class GuideScreen extends Screen {
    private static final String[] CHAPTERS = {"intro", "engine", "upgrades", "excavator", "tunneler", "dump_truck", "paver", "harvester", "bulldozer", "tanker", "forester", "crane", "tower_crane", "locomotive", "drone", "belts", "processing", "well_drill", "power", "powered", "automation", "station", "mechanic", "tools"};
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int TEXT_TOP = 30;
    private static final int TEXT_BOTTOM = 172;

    /** -1 = index. */
    private int chapter = -1;
    private int page;
    private List<List<FormattedCharSequence>> pages = List.of();

    public GuideScreen() {
        super(Component.translatable("item.factorycraft.guide_book"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return (this.height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();
        if (chapter < 0) {
            // Index: two columns of chapter buttons.
            int perColumn = (CHAPTERS.length + 1) / 2;
            for (int i = 0; i < CHAPTERS.length; i++) {
                int column = i / perColumn;
                int row = i % perColumn;
                final int target = i;
                addRenderableWidget(Button.builder(Component.translatable("guide.factorycraft." + CHAPTERS[i] + ".title"),
                                button -> open(target))
                        .bounds(x + 8 + column * 146, y + 26 + row * 14, 138, 13).build());
            }
            return;
        }
        paginate();
        addRenderableWidget(Button.builder(Component.translatable("gui.factorycraft.guide.index"), button -> open(-1))
                .bounds(x + 8, y + HEIGHT - 24, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(x + WIDTH - 88, y + HEIGHT - 24, 36, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(x + WIDTH - 46, y + HEIGHT - 24, 36, 18).build());
    }

    private void open(int target) {
        chapter = target;
        page = 0;
        rebuildWidgets();
    }

    /** Next/previous page, flowing into the neighbouring chapter at the ends. */
    private void turn(int step) {
        int next = page + step;
        if (next >= 0 && next < pages.size()) {
            page = next;
        } else if (step > 0 && chapter < CHAPTERS.length - 1) {
            chapter++;
            page = 0;
            rebuildWidgets();
        } else if (step < 0 && chapter > 0) {
            chapter--;
            page = Integer.MAX_VALUE; // clamped to the last page by paginate()
            rebuildWidgets();
        }
    }

    private void paginate() {
        List<FormattedCharSequence> lines = this.font.split(
                Component.translatable("guide.factorycraft." + CHAPTERS[chapter] + ".text"), WIDTH - 24);
        int perPage = (TEXT_BOTTOM - TEXT_TOP) / (this.font.lineHeight + 1);
        List<List<FormattedCharSequence>> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            result.add(lines.subList(i, Math.min(lines.size(), i + perPage)));
        }
        if (result.isEmpty()) result.add(List.of());
        pages = result;
        page = Math.min(page, pages.size() - 1);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = left();
        int y = top();
        graphics.fill(x - 3, y - 3, x + WIDTH + 3, y + HEIGHT + 3, 0xFF15181B);
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, 0xFFE8A21C);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFFEDE6D3);
        graphics.fill(x, y, x + WIDTH, y + 20, 0xFF3A4047);

        Component heading = chapter < 0 ? Component.translatable("gui.factorycraft.guide.contents")
                : Component.translatable("guide.factorycraft." + CHAPTERS[chapter] + ".title");
        graphics.centeredText(this.font, heading, x + WIDTH / 2, y + 6, 0xFFE8A21C);

        if (chapter >= 0 && !pages.isEmpty()) {
            int lineY = y + TEXT_TOP;
            for (FormattedCharSequence line : pages.get(page)) {
                graphics.text(this.font, line, x + 12, lineY, 0xFF2A2D31, false);
                lineY += this.font.lineHeight + 1;
            }
            String number = (page + 1) + " / " + pages.size();
            graphics.text(this.font, number, x + WIDTH / 2 - this.font.width(number) / 2, y + HEIGHT - 18, 0xFF6B6B6B, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
