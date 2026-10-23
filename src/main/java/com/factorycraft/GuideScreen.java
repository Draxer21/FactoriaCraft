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
 * and paged text for each. The book fills most of the screen. Texts live in
 * the language files (guide.factorycraft.<chapter>.title / .text).
 */
public final class GuideScreen extends Screen {
    private static final String[] CHAPTERS = {"intro", "engine", "upgrades", "excavator", "tunneler", "dump_truck", "paver", "harvester", "bulldozer", "tanker", "forester", "crane", "tower_crane", "locomotive", "drone", "belts", "processing", "well_drill", "power", "powered", "automation", "station", "mechanic", "tools"};
    private static final int HEADER = 24;
    private static final int FOOTER = 30;
    private static final int LINE = 11;

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

    private int bookWidth() {
        return Math.max(300, Math.min(this.width - 30, 520));
    }

    private int bookHeight() {
        return Math.max(200, Math.min(this.height - 24, 340));
    }

    private int left() {
        return (this.width - bookWidth()) / 2;
    }

    private int top() {
        return (this.height - bookHeight()) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();
        int w = bookWidth();
        int h = bookHeight();
        if (chapter < 0) {
            // Index: two columns of chapter buttons filling the page.
            int perColumn = (CHAPTERS.length + 1) / 2;
            int rowHeight = Math.min(20, (h - HEADER - 12) / perColumn);
            int columnWidth = (w - 30) / 2;
            for (int i = 0; i < CHAPTERS.length; i++) {
                int column = i / perColumn;
                int row = i % perColumn;
                final int target = i;
                addRenderableWidget(Button.builder(Component.translatable("guide.factorycraft." + CHAPTERS[i] + ".title"),
                                button -> open(target))
                        .bounds(x + 10 + column * (columnWidth + 10), y + HEADER + 6 + row * rowHeight, columnWidth, rowHeight - 2)
                        .build());
            }
            return;
        }
        paginate();
        addRenderableWidget(Button.builder(Component.translatable("gui.factorycraft.guide.index"), button -> open(-1))
                .bounds(x + 10, y + h - 26, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(x + w - 110, y + h - 26, 46, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(x + w - 58, y + h - 26, 46, 20).build());
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
                Component.translatable("guide.factorycraft." + CHAPTERS[chapter] + ".text"), bookWidth() - 32);
        int perPage = Math.max(1, (bookHeight() - HEADER - FOOTER - 12) / LINE);
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
        int w = bookWidth();
        int h = bookHeight();
        graphics.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFF15181B);
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFFE8A21C);
        graphics.fill(x, y, x + w, y + h, 0xFFEDE6D3);
        graphics.fill(x, y, x + w, y + HEADER, 0xFF3A4047);
        graphics.fill(x, y + HEADER, x + w, y + HEADER + 2, 0xFFE8A21C);

        Component heading = chapter < 0 ? Component.translatable("gui.factorycraft.guide.contents")
                : Component.translatable("guide.factorycraft." + CHAPTERS[chapter] + ".title");
        graphics.centeredText(this.font, heading, x + w / 2, y + 8, 0xFFE8A21C);

        if (chapter >= 0 && !pages.isEmpty()) {
            int lineY = y + HEADER + 10;
            for (FormattedCharSequence line : pages.get(page)) {
                graphics.text(this.font, line, x + 16, lineY, 0xFF2A2D31, false);
                lineY += LINE;
            }
            String number = (page + 1) + " / " + pages.size();
            graphics.text(this.font, number, x + w / 2 - this.font.width(number) / 2, y + h - 20, 0xFF6B6B6B, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
