package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration (only loaded when Jade is installed): looking at a machine
 * shows its progress, installed upgrades and special state (reactor heat and
 * water, accelerator magnets). Jade already shows energy, fluids and items on
 * its own through the standard capabilities the machines expose.
 */
@WailaPlugin
public final class JadeCompat implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "machine_info");
    private static final String KEY = "FactoryCraftInfo";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, Block.class);
    }

    /** Server side: the block's lines, sent to the looking player. */
    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof JadeInfo info)) return;
            List<Component> lines = new ArrayList<>();
            info.jadeInfo(lines);
            ListTag list = new ListTag();
            for (Component line : lines) list.add(accessor.encodeAsNbt(ComponentSerialization.STREAM_CODEC, line));
            data.put(KEY, list);
        }

        public Identifier getUid() {
            return UID;
        }
    }

    /** Client side: prints the lines. */
    enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY)) return;
            for (Tag tag : data.getListOrEmpty(KEY)) {
                accessor.decodeFromNbt(ComponentSerialization.STREAM_CODEC, tag).ifPresent(tooltip::add);
            }
        }

        public Identifier getUid() {
            return UID;
        }
    }
}
