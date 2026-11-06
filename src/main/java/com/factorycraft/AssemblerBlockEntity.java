package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Assembler: crafts any crafting-table recipe on its own. The 3x3 pattern
 * (set in its screen, items are not used up) picks the recipe; ingredients go
 * into its 9 buffer slots (hoppers, conveyors, robot arms) and results come out
 * of 4 output slots.
 *
 * Touching a data cable or storage terminal it joins the storage network: the
 * results go into the network, and crafts requested from a storage terminal
 * pull their ingredients from the network.
 */
public final class AssemblerBlockEntity extends PoweredMachineBlockEntity {
    public static final int PATTERN = 9;
    public static final int BUFFER = 9;
    public static final int TIME = 40;
    public static final int ENERGY = 20;
    /** Most crafts that can be waiting at once. */
    public static final int MAX_PENDING = 4096;

    private final SimpleContainer pattern = new SimpleContainer(PATTERN) {
        @Override
        public void setChanged() {
            super.setChanged();
            recipeDirty = true;
            AssemblerBlockEntity.this.setChanged();
        }
    };
    private boolean recipeDirty = true;
    private @Nullable RecipeHolder<CraftingRecipe> recipe;
    private ItemStack resultPreview = ItemStack.EMPTY;
    /** Crafts requested from a storage terminal, still to do. */
    private int pending;

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(Automation.ASSEMBLER_BLOCK_ENTITY.value(), pos, state, BUFFER);
    }

    @Override protected int time() { return TIME; }
    @Override protected int energyPerTick() { return ENERGY; }
    @Override protected MenuType<ProcessingMenu> menuType() { return null; }
    @Override protected String nameKey() { return "block.factorycraft.assembler"; }

    public SimpleContainer pattern() {
        return pattern;
    }

    public int pending() {
        return pending;
    }

    /** One pattern slot set from the screen (a copy of one item, or empty to clear). */
    public void setPattern(int slot, ItemStack stack) {
        if (slot < 0 || slot >= PATTERN) return;
        pattern.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** A storage terminal asks for {@code crafts} more crafts. */
    public void request(int crafts) {
        pending = Math.min(MAX_PENDING, pending + Math.max(0, crafts));
        setChanged();
    }

    /** What one craft gives (for the terminal), or empty if the pattern is no recipe. */
    public ItemStack result(Level level) {
        refreshRecipe(level);
        return resultPreview;
    }

    private CraftingInput input() {
        List<ItemStack> grid = new ArrayList<>(PATTERN);
        for (int i = 0; i < PATTERN; i++) grid.add(pattern.getItem(i));
        return CraftingInput.of(3, 3, grid);
    }

    private void refreshRecipe(Level level) {
        if (!recipeDirty || !(level instanceof ServerLevel server)) return;
        recipeDirty = false;
        CraftingInput input = input();
        Optional<RecipeHolder<CraftingRecipe>> found = input.isEmpty() ? Optional.empty()
                : server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
        recipe = found.orElse(null);
        resultPreview = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input);
    }

    /** Items one craft needs, by item. */
    private Map<Item, Integer> needs() {
        Map<Item, Integer> needs = new HashMap<>();
        for (int i = 0; i < PATTERN; i++) {
            ItemStack stack = pattern.getItem(i);
            if (!stack.isEmpty()) needs.merge(stack.getItem(), 1, Integer::sum);
        }
        return needs;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (slot >= BUFFER) return false;
        for (int i = 0; i < PATTERN; i++) {
            if (!pattern.getItem(i).isEmpty() && pattern.getItem(i).is(stack.getItem())) return true;
        }
        return false;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        refreshRecipe(level);
        if (recipe == null || resultPreview.isEmpty()) return null;
        int[] consume = new int[BUFFER];
        for (Map.Entry<Item, Integer> need : needs().entrySet()) {
            int left = need.getValue();
            for (int i = 0; i < BUFFER && left > 0; i++) {
                ItemStack stack = items.get(i);
                if (!stack.is(need.getKey())) continue;
                int take = Math.min(left, stack.getCount() - consume[i]);
                consume[i] += take;
                left -= take;
            }
            if (left > 0) return null;
        }
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(resultPreview.copy());
        for (ItemStack rest : recipe.value().getRemainingItems(input())) {
            if (!rest.isEmpty()) outputs.add(rest.copy());
        }
        return new Operation(consume, outputs);
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        if (pending > 0) pending--;
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 20 == 0) serveNetwork(level);
        return super.work(level);
    }

    /** Network side: push results into storage and, for requested crafts, pull ingredients out. */
    void serveNetwork(Level level) {
        BlockPos link = null;
        for (Direction dir : Direction.values()) {
            BlockPos next = worldPosition.relative(dir);
            if (level.getBlockState(next).getBlock() instanceof DataCableBlock
                    || level.getBlockEntity(next) instanceof StorageTerminalBlockEntity) {
                link = next;
                break;
            }
        }
        if (link == null) return;
        StorageNetwork network = StorageNetwork.scan(level, link);
        boolean changed = false;
        for (int i = BUFFER; i < items.size(); i++) {
            ItemStack out = items.get(i);
            if (out.isEmpty()) continue;
            ItemStack left = network.insert(out.copy());
            if (left.getCount() != out.getCount()) {
                items.set(i, left);
                changed = true;
            }
        }
        if (pending > 0) {
            for (Map.Entry<Item, Integer> need : needs().entrySet()) {
                int have = 0;
                for (int i = 0; i < BUFFER; i++) if (items.get(i).is(need.getKey())) have += items.get(i).getCount();
                int want = need.getValue() * Math.min(pending, 4) - have;
                if (want <= 0) continue;
                ItemStack got = network.extract(ItemResource.of(new ItemStack(need.getKey())), want);
                if (got.isEmpty()) continue;
                for (int i = 0; i < BUFFER && !got.isEmpty(); i++) {
                    ItemStack slot = items.get(i);
                    if (slot.isEmpty()) {
                        items.set(i, got.split(got.getMaxStackSize()));
                    } else if (ItemStack.isSameItemSameComponents(slot, got) && slot.getCount() < slot.getMaxStackSize()) {
                        int move = Math.min(got.getCount(), slot.getMaxStackSize() - slot.getCount());
                        slot.grow(move);
                        got.shrink(move);
                    }
                }
                if (!got.isEmpty()) network.insert(got);
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AssemblerMenu(id, inventory, this, pattern, data());
    }

    @Override
    public void jadeInfo(List<Component> lines) {
        if (!resultPreview.isEmpty()) lines.add(Component.translatable("jade.factorycraft.assembler.makes", resultPreview.getHoverName()));
        if (pending > 0) lines.add(Component.translatable("jade.factorycraft.assembler.pending", pending));
        super.jadeInfo(lines);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pending = input.getIntOr("Pending", 0);
        NonNullList<ItemStack> saved = NonNullList.withSize(PATTERN, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(input.childOrEmpty("Pattern"), saved);
        for (int i = 0; i < PATTERN; i++) pattern.getItems().set(i, saved.get(i));
        recipeDirty = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Pending", pending);
        NonNullList<ItemStack> saved = NonNullList.withSize(PATTERN, ItemStack.EMPTY);
        for (int i = 0; i < PATTERN; i++) saved.set(i, pattern.getItem(i));
        net.minecraft.world.ContainerHelper.saveAllItems(output.child("Pattern"), saved);
    }
}
