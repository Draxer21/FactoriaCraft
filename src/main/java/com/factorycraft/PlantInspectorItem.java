package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Plant inspector: right-click a generator or reactor to check whether the
 * power plant is built properly and safely, with a list of what's missing.
 */
public final class PlantInspectorItem extends Item {
    public PlantInspectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private record Check(boolean ok, Component text) {}

    private static Check check(boolean ok, String key, Object... args) {
        return new Check(ok, Component.translatable("inspector.factorycraft." + key, args));
    }

    private static boolean near(Level level, BlockPos pos, int r, Class<? extends BlockEntity> type) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r / 2, -r), pos.offset(r, r / 2, r))) {
            if (type.isInstance(level.getBlockEntity(p))) return true;
        }
        return false;
    }

    private static boolean emergencySwitch(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            var block = level.getBlockState(pos.relative(dir)).getBlock();
            if (block instanceof LeverBlock || block instanceof ButtonBlock) return true;
        }
        return level.hasNeighborSignal(pos);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        Player player = context.getPlayer();
        List<Check> checks = new ArrayList<>();
        String title;

        if (be instanceof NuclearReactorBlockEntity reactor) {
            title = "reactor";
            int shield = reactor.shielding(level);
            checks.add(check(shield >= NuclearReactorBlockEntity.SHIELDING_NEEDED, "reactor.shielding", shield, NuclearReactorBlockEntity.SHIELDING_NEEDED));
            checks.add(check(reactor.water() >= 4000, "reactor.water", reactor.water() / 1000));
            boolean pump = near(level, pos, 6, PumpBlockEntity.class);
            checks.add(check(pump, "reactor.pump"));
            int turbines = 0;
            for (Direction dir : Direction.values()) {
                if (level.getBlockEntity(pos.relative(dir)) instanceof SteamTurbineBlockEntity) turbines++;
            }
            checks.add(check(turbines > 0, "reactor.turbines", turbines));
            checks.add(check(emergencySwitch(level, pos), "reactor.scram"));
            checks.add(check(near(level, pos, 8, WasteBarrelBlockEntity.class), "reactor.barrel"));
            checks.add(check(reactor.heatPercent() < 70, "reactor.heat", reactor.heatPercent()));
        } else if (be instanceof GeneratorBlockEntity generator) {
            title = "diesel";
            boolean filter = generator.hasFilter(level);
            boolean purifier = Pollution.purified(level, pos);
            checks.add(check(filter || purifier, "diesel.filter"));
            checks.add(check(purifier, "diesel.purifier"));
        } else if (be instanceof WindmillBlockEntity) {
            title = "windmill";
            checks.add(check(pos.getY() >= 100, "windmill.height", pos.getY()));
            checks.add(check(WindmillBlockEntity.openness(level, pos) >= 0.9F, "windmill.open",
                    Math.round(WindmillBlockEntity.openness(level, pos) * 100)));
        } else if (be instanceof SolarGeneratorBlockEntity) {
            title = "solar";
            checks.add(check(level.canSeeSky(pos.above()), "solar.sky"));
        } else if (be instanceof GeothermalGeneratorBlockEntity) {
            title = "geothermal";
            int lava = GeothermalGeneratorBlockEntity.lavaAround(level, pos);
            checks.add(check(lava >= 2, "geothermal.lava", lava));
        } else if (be instanceof SteamTurbineBlockEntity) {
            title = "turbine";
            boolean reactor = false;
            for (Direction dir : Direction.values()) {
                if (level.getBlockEntity(pos.relative(dir)) instanceof NuclearReactorBlockEntity) reactor = true;
            }
            checks.add(check(reactor, "turbine.reactor"));
        } else {
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || player == null) return InteractionResult.SUCCESS;

        // Contaminated ground nearby?
        int dirty = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -3, -8), pos.offset(8, 3, 8))) {
            if (level.getBlockState(p).is(Nuclear.CONTAMINATED_SOIL.value())) dirty++;
        }
        checks.add(check(dirty == 0, "contamination", dirty));

        player.sendSystemMessage(Component.translatable("inspector.factorycraft.title",
                Component.translatable("inspector.factorycraft.type." + title)).withStyle(ChatFormatting.GOLD));
        int ok = 0;
        for (Check c : checks) {
            if (c.ok()) ok++;
            player.sendSystemMessage(Component.literal(c.ok() ? "✔ " : "✘ ")
                    .withStyle(c.ok() ? ChatFormatting.GREEN : ChatFormatting.RED).append(c.text().copy().withStyle(ChatFormatting.WHITE)));
        }
        boolean clean = ok == checks.size();
        player.sendSystemMessage(Component.translatable(clean ? "inspector.factorycraft.result.ok" : "inspector.factorycraft.result.bad",
                ok, checks.size()).withStyle(clean ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return InteractionResult.SUCCESS_SERVER;
    }
}
