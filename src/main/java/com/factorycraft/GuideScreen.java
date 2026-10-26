package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The FactoryCraft manual: an index of chapters (one per machine or system)
 * and paged text for each. The book fills most of the screen. Texts live in
 * the language files (guide.factorycraft.<chapter>.title / .text).
 */
public final class GuideScreen extends Screen {
    private static final String[] CHAPTERS = {"intro", "engine", "upgrades", "excavator", "tunneler", "dump_truck", "paver", "harvester", "bulldozer", "tanker", "forester", "crane", "tower_crane", "locomotive", "drone", "belts", "processing", "well_drill", "power", "powered", "automation", "station", "mechanic", "tools", "nuclear", "reactor", "weapons", "clean_power", "logistics", "aircraft", "terrain"};
    private static final int HEADER = 24;
    private static final int FOOTER = 30;
    private static final int LINE = 11;

    /** -1 = index. */
    private int chapter = -1;
    private int page;
    /** A page is either text lines or a set of recipes. */
    private record Page(List<FormattedCharSequence> lines, List<GuideRecipes.Display> recipes) {}

    private List<Page> pages = List.of();

    public GuideScreen() {
        super(Component.translatable("item.factorycraft.guide_book"));
    }

    /** Opens a chapter at its first recipe page (used by the CI screenshot demo). */
    /** {@code which}: "" first recipe page, "last" last page, or "N" the N-th recipe page. */
    static GuideScreen atRecipes(String chapterName, String which) {
        GuideScreen screen = new GuideScreen();
        screen.chapter = java.util.Arrays.asList(CHAPTERS).indexOf(chapterName);
        screen.page = which.equals("last") ? Integer.MAX_VALUE : which.isEmpty() ? -1 : -Integer.parseInt(which);
        return screen;
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
        List<Page> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            result.add(new Page(lines.subList(i, Math.min(lines.size(), i + perPage)), List.of()));
        }
        // Recipe pages follow the text, as many blocks as fit.
        List<GuideRecipes.Display> recipes = GuideRecipes.forChapter(CHAPTERS[chapter]);
        int perRecipePage = recipeColumns() * recipeRows();
        for (int i = 0; i < recipes.size(); i += perRecipePage) {
            result.add(new Page(List.of(), recipes.subList(i, Math.min(recipes.size(), i + perRecipePage))));
        }
        if (result.isEmpty()) result.add(new Page(List.of(), List.of()));
        if (page < 0) { // jump to the first (or -page-th) recipe page
            int skip = -page - 1;
            page = 0;
            while (page < result.size() - 1 && result.get(page).recipes().isEmpty()) page++;
            page = Math.min(result.size() - 1, page + skip);
        }
        pages = result;
        page = Math.min(page, pages.size() - 1);
    }

    private int recipeColumns() {
        return Math.max(1, (bookWidth() - 16 + 6) / (GuideRecipes.WIDTH + 6));
    }

    private int recipeRows() {
        return Math.max(1, (bookHeight() - HEADER - FOOTER - 17 + 5) / (GuideRecipes.HEIGHT + 5));
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
            Page current = pages.get(page);
            int lineY = y + HEADER + 10;
            for (FormattedCharSequence line : current.lines()) {
                graphics.text(this.font, line, x + 16, lineY, 0xFF2A2D31, false);
                lineY += LINE;
            }
            if (!current.recipes().isEmpty()) {
                graphics.text(this.font, Component.translatable("gui.factorycraft.guide.recipes"), x + 16, y + HEADER + 5, 0xFF8A5A00, false);
                int columns = recipeColumns();
                int used = Math.min(columns, current.recipes().size());
                int startX = x + (w - (used * GuideRecipes.WIDTH + (used - 1) * 6)) / 2;
                ItemStack hovered = ItemStack.EMPTY;
                for (int i = 0; i < current.recipes().size(); i++) {
                    int bx = startX + (i % columns) * (GuideRecipes.WIDTH + 6);
                    int by = y + HEADER + 17 + (i / columns) * (GuideRecipes.HEIGHT + 5);
                    ItemStack over = GuideRecipes.draw(graphics, this.font, current.recipes().get(i), bx, by, mouseX, mouseY);
                    if (!over.isEmpty()) hovered = over;
                }
                if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
            }
            String number = (page + 1) + " / " + pages.size();
            graphics.text(this.font, number, x + w / 2 - this.font.width(number) / 2, y + h - 20, 0xFF6B6B6B, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
