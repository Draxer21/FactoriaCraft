package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The research tree: one column per tier, lines from each node to what it needs.
 * Green = researched, gold = can be researched now (click it), grey = needs
 * something else first. Hover a node to see what it unlocks. The wheel scrolls sideways.
 */
public final class ResearchScreen extends Screen {
    private static final int W = 96;
    private static final int H = 22;
    private static final int COL = 112;
    private static final int ROW = 27;
    private ResearchPayload data;
    private final Map<String, int[]> boxes = new HashMap<>();
    private int scroll;

    public ResearchScreen(ResearchPayload data) {
        super(Component.translatable("gui.factorycraft.research.title"));
        this.data = data;
    }

    void update(ResearchPayload data) {
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout() {
        boxes.clear();
        Map<Integer, Integer> rows = new HashMap<>();
        int maxTier = 0;
        for (Research.Node node : Research.TREE) maxTier = Math.max(maxTier, Research.tier(node));
        int total = (maxTier + 1) * COL - (COL - W);
        int left = Math.max(10, (this.width - total) / 2) - scroll;
        for (Research.Node node : Research.TREE) {
            int tier = Research.tier(node);
            int row = rows.merge(tier, 1, Integer::sum) - 1;
            boxes.put(node.id(), new int[] {left + tier * COL, 46 + row * ROW});
        }
    }

    private int maxScroll() {
        int maxTier = 0;
        for (Research.Node node : Research.TREE) maxTier = Math.max(maxTier, Research.tier(node));
        int total = (maxTier + 1) * COL - (COL - W) + 20;
        return Math.max(0, total - this.width);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (scrollY * 24)));
        return true;
    }

    private Research.State state() {
        return new Research.State(data.unlocked(), data.points());
    }

    private Research.Node at(double mouseX, double mouseY) {
        for (Research.Node node : Research.TREE) {
            int[] b = boxes.get(node.id());
            if (b != null && mouseX >= b[0] && mouseX < b[0] + W && mouseY >= b[1] && mouseY < b[1] + H) return node;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        Research.Node node = at(event.x(), event.y());
        if (node != null && Research.available(state(), node)) {
            ClientPacketDistributor.sendToServer(new ResearchUnlockPayload(node.id()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        layout();
        graphics.fill(0, 0, this.width, this.height, 0xC0101418);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFE8A21C);
        graphics.centeredText(this.font, Component.translatable("gui.factorycraft.research.points", data.points()), this.width / 2, 22, 0xFFBFC7CE);
        if (!data.required()) {
            graphics.centeredText(this.font, Component.translatable("gui.factorycraft.research.off"), this.width / 2, 32, 0xFF9AA3AB);
        }
        Research.State state = state();
        // Lines first, under the boxes.
        for (Research.Node node : Research.TREE) {
            int[] b = boxes.get(node.id());
            for (String req : node.requires()) {
                int[] r = boxes.get(req);
                if (r == null) continue;
                int color = state.has(req) ? 0xFF4CD964 : 0xFF4A5056;
                int x0 = r[0] + W;
                int y0 = r[1] + H / 2;
                int x1 = b[0];
                int y1 = b[1] + H / 2;
                int mid = (x0 + x1) / 2;
                graphics.fill(x0, y0, mid + 1, y0 + 1, color);
                graphics.fill(mid, Math.min(y0, y1), mid + 1, Math.max(y0, y1) + 1, color);
                graphics.fill(mid, y1, x1, y1 + 1, color);
            }
        }
        Research.Node hovered = at(mouseX, mouseY);
        for (Research.Node node : Research.TREE) {
            int[] b = boxes.get(node.id());
            boolean done = state.has(node.id());
            boolean open = Research.available(state, node);
            boolean affordable = open && data.points() >= node.cost();
            int border = done ? 0xFF4CD964 : affordable ? 0xFFE8A21C : open ? 0xFFB07A20 : 0xFF4A5056;
            graphics.fill(b[0] - 1, b[1] - 1, b[0] + W + 1, b[1] + H + 1, border);
            graphics.fill(b[0], b[1], b[0] + W, b[1] + H, node == hovered ? 0xFF2A3036 : 0xFF1A1E22);
            List<ItemStack> icons = Research.icons(node);
            if (!icons.isEmpty()) graphics.item(icons.get(0), b[0] + 3, b[1] + 3);
            Component name = Component.translatable("research.factorycraft." + node.id());
            graphics.pose().pushMatrix();
            graphics.pose().translate(b[0] + 22, b[1] + 4);
            graphics.pose().scale(0.75F, 0.75F);
            graphics.text(this.font, name, 0, 0, done ? 0xFFB8F5C4 : 0xFFE8E8E8);
            graphics.text(this.font, done ? Component.translatable("gui.factorycraft.research.done")
                    : Component.translatable("gui.factorycraft.research.cost", node.cost()), 0, 12, done ? 0xFF4CD964 : affordable ? 0xFFE8A21C : 0xFF9AA3AB);
            graphics.pose().popMatrix();
        }
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("research.factorycraft." + hovered.id()));
            for (ItemStack stack : Research.icons(hovered)) lines.add(Component.literal(" • ").append(stack.getHoverName()));
            if (!hovered.requires().isEmpty()) {
                List<Component> reqs = new ArrayList<>();
                for (String r : hovered.requires()) reqs.add(Component.translatable("research.factorycraft." + r));
                Component joined = Component.empty();
                for (int i = 0; i < reqs.size(); i++) {
                    if (i > 0) joined = joined.copy().append(", ");
                    joined = joined.copy().append(reqs.get(i));
                }
                lines.add(Component.translatable("gui.factorycraft.research.requires", joined));
            }
            int x = Math.min(mouseX + 10, this.width - 170);
            int y = Math.min(mouseY + 10, this.height - lines.size() * 10 - 6);
            graphics.fill(x - 3, y - 3, x + 165, y + lines.size() * 10 + 2, 0xF0080A0C);
            for (int i = 0; i < lines.size(); i++) graphics.text(this.font, lines.get(i), x, y + i * 10, i == 0 ? 0xFFE8A21C : 0xFFD0D6DC);
        }
    }
}
