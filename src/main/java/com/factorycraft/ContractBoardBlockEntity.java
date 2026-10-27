package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contract board: every Minecraft day it posts three industrial contracts
 * ("deliver 48 steel ingots"). Right-click it holding the goods to deliver
 * (partial deliveries count); a finished contract pays out emeralds (sometimes
 * diamonds or upgrade parts) and experience. Right-click empty-handed to read them.
 */
public final class ContractBoardBlockEntity extends MachineBlockEntity {
    /** What can be asked for: item id, smallest and largest amount, emeralds per 16 delivered. */
    private record Demand(String id, int min, int max, int value) {}

    private static final Demand[] DEMANDS = {
            new Demand("factorycraft:steel_ingot", 16, 64, 3),
            new Demand("factorycraft:plastic_sheet", 16, 48, 3),
            new Demand("factorycraft:copper_wire", 32, 96, 2),
            new Demand("factorycraft:circuit_board", 8, 24, 6),
            new Demand("factorycraft:advanced_circuit", 4, 12, 12),
            new Demand("factorycraft:diesel", 4, 16, 8),
            new Demand("factorycraft:biofuel", 8, 24, 5),
            new Demand("factorycraft:salt", 32, 96, 1),
            new Demand("factorycraft:asphalt", 32, 128, 1),
            new Demand("factorycraft:tin_ingot", 16, 48, 2),
            new Demand("factorycraft:lead_ingot", 16, 48, 2),
            new Demand("factorycraft:iron_gear", 16, 48, 2),
            new Demand("factorycraft:tempered_glass", 16, 64, 2),
            new Demand("factorycraft:reinforced_concrete", 32, 96, 1),
            new Demand("minecraft:leather", 16, 48, 2),
            new Demand("minecraft:wheat", 64, 192, 1),
            new Demand("minecraft:bread", 32, 96, 1),
            new Demand("minecraft:iron_ingot", 32, 96, 1),
            new Demand("minecraft:redstone", 64, 192, 1),
    };

    /** One posted contract. */
    static final class Contract {
        Item item;
        int wanted;
        int delivered;
        ItemStack reward;

        Contract(Item item, int wanted, int delivered, ItemStack reward) {
            this.item = item;
            this.wanted = wanted;
            this.delivered = delivered;
            this.reward = reward;
        }

        boolean done() {
            return delivered >= wanted;
        }
    }

    private final List<Contract> contracts = new ArrayList<>();
    private long day = -1;

    public ContractBoardBlockEntity(BlockPos pos, BlockState state) {
        super(Economy.CONTRACT_BOARD_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    List<Contract> contracts() {
        return contracts;
    }

    private void post(Level level) {
        contracts.clear();
        RandomSource random = RandomSource.create(level.getGameTime() / 24000L * 31 + worldPosition.asLong());
        List<Integer> used = new ArrayList<>();
        while (contracts.size() < 3 && used.size() < DEMANDS.length) {
            int pick = random.nextInt(DEMANDS.length);
            if (used.contains(pick)) continue;
            used.add(pick);
            Demand demand = DEMANDS[pick];
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(demand.id()));
            if (item == Items.AIR) continue;
            int wanted = demand.min() + random.nextInt(demand.max() - demand.min() + 1);
            int emeralds = Math.max(1, wanted * demand.value() / 16);
            ItemStack reward = random.nextInt(10) == 0 ? new ItemStack(Items.DIAMOND, Math.max(1, emeralds / 4))
                    : new ItemStack(Items.EMERALD, Math.min(64, emeralds));
            contracts.add(new Contract(item, wanted, 0, reward));
        }
        setChanged();
    }

    @Override
    protected boolean work(Level level) {
        long today = level.getGameTime() / 24000L;
        if (today != day) {
            day = today;
            post(level);
        }
        return contracts.stream().anyMatch(c -> !c.done());
    }

    public void read(Player player) {
        if (level != null && contracts.isEmpty()) work(level);
        player.sendSystemMessage(Component.translatable("message.factorycraft.contracts.header"));
        for (Contract c : contracts) {
            player.sendSystemMessage(Component.translatable(c.done() ? "message.factorycraft.contracts.done" : "message.factorycraft.contracts.line",
                    new ItemStack(c.item).getHoverName(), Math.min(c.delivered, c.wanted), c.wanted, c.reward.getCount(), c.reward.getHoverName()));
        }
    }

    /** Delivers what the player holds to the matching contract. False if nothing matched. */
    public boolean deliver(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        for (Contract c : contracts) {
            if (c.done() || !held.is(c.item)) continue;
            int give = Math.min(held.getCount(), c.wanted - c.delivered);
            held.shrink(give);
            c.delivered += give;
            if (c.done()) {
                ItemStack reward = c.reward.copy();
                if (!player.getInventory().add(reward)) player.drop(reward, false);
                if (level instanceof ServerLevel server) {
                    ExperienceOrb.award(server, player.position(), 20 + c.wanted / 4);
                    server.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);
                }
                player.sendOverlayMessage(Component.translatable("message.factorycraft.contracts.paid", c.reward.getCount(), c.reward.getHoverName()));
            } else {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.contracts.progress",
                        new ItemStack(c.item).getHoverName(), c.delivered, c.wanted));
            }
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        day = input.getLongOr("Day", -1);
        contracts.clear();
        int n = input.getIntOr("Contracts", 0);
        for (int i = 0; i < n; i++) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(input.getStringOr("Item" + i, "minecraft:air")));
            ItemStack reward = input.read("Reward" + i, ItemStack.CODEC).orElse(new ItemStack(Items.EMERALD));
            contracts.add(new Contract(item, input.getIntOr("Wanted" + i, 1), input.getIntOr("Delivered" + i, 0), reward));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Day", day);
        output.putInt("Contracts", contracts.size());
        for (int i = 0; i < contracts.size(); i++) {
            Contract c = contracts.get(i);
            output.putString("Item" + i, BuiltInRegistries.ITEM.getKey(c.item).toString());
            output.putInt("Wanted" + i, c.wanted);
            output.putInt("Delivered" + i, c.delivered);
            output.store("Reward" + i, ItemStack.CODEC, c.reward);
        }
    }
}
