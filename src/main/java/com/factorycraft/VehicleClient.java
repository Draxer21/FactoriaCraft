package com.factorycraft;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the machines: the engine key, the dashboard HUD and the
 * engine / track / drill sounds that follow each machine around.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class VehicleClient {
    private VehicleClient() {}

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "vehicles"));
    public static final KeyMapping ENGINE_KEY = new KeyMapping("key.factorycraft.engine",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping TOOL_KEY = new KeyMapping("key.factorycraft.tool",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping LIGHTS_KEY = new KeyMapping("key.factorycraft.lights",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    /** Machines are heard up to this far away. */
    private static final double HEARING_RANGE = 40.0;

    /** Opens the manual (called from {@link GuideBookItem} on the client only). */
    public static void openGuide() {
        Minecraft.getInstance().gui.setScreen(new GuideScreen());
    }

    // ---------------------------------------------------------------- registration

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(ENGINE_KEY);
        event.register(LIGHTS_KEY);
        event.register(TOOL_KEY);
    }

    @SubscribeEvent
    public static void registerClientPayloads(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {
        event.register(StationDataPayload.TYPE, (payload, context) -> Minecraft.getInstance().gui.setScreen(new StationScreen(payload)));
        event.register(TerminalListPayload.TYPE, (payload, context) -> TerminalListPayload.latest = payload);
        event.register(CraneLoadPayload.TYPE, (payload, context) -> {
            var level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(payload.entityId()) instanceof CraneEntity crane) crane.setClientLoad(payload.packed());
        });
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "machine_dashboard"), VehicleClient::renderDashboard);
    }

    // ---------------------------------------------------------------- ticking

    private static final boolean GUIDE_DEMO = Boolean.getBoolean("factorycraft.guideDemo");
    private static final String[] DEMO_CHAPTERS = {"economy:1", "stats", "smog", "look"};
    private static int demoTicks;

    /** CI: once in a world, show one chapter's recipes every 5 seconds. */
    private static void guideDemo() {
        if (!GUIDE_DEMO) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return; // recipes need a world (item components, tags)
        demoTicks++;
        if (demoTicks % 160 == 0 && demoTicks / 160 <= DEMO_CHAPTERS.length) {
            String chapter = DEMO_CHAPTERS[demoTicks / 160 - 1];
            com.mojang.logging.LogUtils.getLogger().info("GUIDE_DEMO {}", chapter);
            String[] parts = chapter.split(":");
            if (parts[0].equals("look")) {
                // Just take another screenshot of whatever is in view.
                com.mojang.logging.LogUtils.getLogger().info("GUIDE_DEMO look air={} haze={}", PollutionPayload.latest.air(), EnvironmentClient.haze());
            } else if (parts[0].equals("nuke")) {
                mc.gui.setScreen(null);
                mc.player.connection.sendCommand("time set noon");
                mc.player.connection.sendCommand("gamemode spectator");
                mc.player.connection.sendCommand("summon factorycraft:mini_nuke ~ ~25 ~45 {Item:{id:\"factorycraft:mini_nuke\",count:1,"
                        + "components:{\"minecraft:custom_data\":{Yield:" + parts[1] + "}}}}");
                mc.player.connection.sendCommand("tp @s ~ ~75 ~-45 0 38");
            } else if (parts[0].equals("terminal")) {
                java.util.List<TerminalListPayload.Entry> demo = new java.util.ArrayList<>();
                net.minecraft.world.item.Item[] items = {net.minecraft.world.item.Items.IRON_INGOT, net.minecraft.world.item.Items.COAL,
                        net.minecraft.world.item.Items.COBBLESTONE, net.minecraft.world.item.Items.OAK_LOG, net.minecraft.world.item.Items.DIAMOND,
                        net.minecraft.world.item.Items.REDSTONE, net.minecraft.world.item.Items.COPPER_INGOT, net.minecraft.world.item.Items.GOLD_INGOT,
                        net.minecraft.world.item.Items.WHEAT, FactoryCraft.SALT.value(), FactoryCraft.DIESEL.value(), Materials.PLASTIC.value()};
                for (int i = 0; i < items.length; i++) demo.add(new TerminalListPayload.Entry(new net.minecraft.world.item.ItemStack(items[i]), (long) (i * 977 + 13) * (i % 3 + 1)));
                TerminalListPayload.latest = new TerminalListPayload(5, demo);
                var inv = mc.player.getInventory();
                mc.gui.setScreen(new StorageTerminalScreen(StorageTerminalMenu.client(0, inv), inv,
                        net.minecraft.network.chat.Component.translatable("block.factorycraft.storage_terminal")));
            } else if (parts[0].equals("stats")) {
                java.util.List<StatsPayload.Line> lines = new java.util.ArrayList<>();
                int[] total = new int[ProductionStats.SHOWN];
                net.minecraft.world.item.Item[] items = {Materials.PLASTIC.value(), FactoryCraft.DIESEL.value(), net.minecraft.world.item.Items.IRON_INGOT,
                        FactoryCraft.SALT.value(), net.minecraft.world.item.Items.WHEAT};
                for (int k = 0; k < items.length; k++) {
                    int[] series = new int[ProductionStats.SHOWN];
                    long sum = 0;
                    for (int m = 0; m < series.length; m++) {
                        series[m] = (int) (40 + 30 * Math.sin(m / 4.0 + k) + m * (5 - k));
                        total[m] += series[m];
                        sum += series[m];
                    }
                    lines.add(new StatsPayload.Line(new net.minecraft.world.item.ItemStack(items[k]), series, sum));
                }
                mc.gui.setScreen(new StatsScreen(new StatsPayload(total, lines)));
            } else if (parts[0].equals("smog")) {
                mc.gui.setScreen(null);
                mc.player.connection.sendCommand("time set noon");
                mc.player.connection.sendCommand("tp @s ~ ~ ~ 0 0");
                var server = mc.getSingleplayerServer();
                net.minecraft.core.BlockPos at = mc.player.blockPosition();
                if (server != null) server.execute(() -> {
                    AirQuality.spread(server.overworld(), at, 900);
                    com.mojang.logging.LogUtils.getLogger().info("GUIDE_DEMO smog air={}", AirQuality.get(server.overworld(), at));
                });
            } else if (parts[0].equals("showcase")) {
                mc.gui.setScreen(null);
                mc.player.connection.sendCommand("time set noon");
                mc.player.connection.sendCommand("gamemode spectator");
                int x = mc.player.getBlockX();
                int z = mc.player.getBlockZ();
                mc.player.connection.sendCommand("fill " + (x - 8) + " 117 " + (z + 4) + " " + (x + 8) + " 117 " + (z + 16) + " minecraft:smooth_stone");
                String[] blocks = {"turret", "alarm", "security_camera", "monitor", "lift_pad", "steel_scaffold", "reinforced_concrete",
                        "armor_plating", "hazard_block", "steel_panel", "steel_grate", "industrial_bricks", "warning_lamp"};
                for (int i = 0; i < blocks.length; i++) {
                    mc.player.connection.sendCommand("setblock " + (x - 6 + i) + " 118 " + (z + 8) + " factorycraft:" + blocks[i] + (i < 4 ? "[facing=south]" : ""));
                }
                mc.player.connection.sendCommand("setblock " + (x - 2) + " 118 " + (z + 12) + " factorycraft:armored_door[half=lower,facing=south]");
                mc.player.connection.sendCommand("setblock " + (x - 2) + " 119 " + (z + 12) + " factorycraft:armored_door[half=upper,facing=south]");
                for (int i = 0; i < 3; i++) {
                    mc.player.connection.sendCommand("setblock " + (x + 2 + i) + " 122 " + (z + 12) + " factorycraft:shutter[facing=north]");
                    mc.player.connection.sendCommand("fill " + (x + 2 + i) + " 118 " + (z + 12) + " " + (x + 2 + i) + " 121 " + (z + 12) + " factorycraft:shutter_panel[axis=x]");
                }
                mc.player.connection.sendCommand("setblock " + (x + 6) + " 118 " + (z + 12) + " factorycraft:steel_scaffold");
                mc.player.connection.sendCommand("setblock " + (x + 6) + " 119 " + (z + 12) + " factorycraft:steel_scaffold");
                mc.player.connection.sendCommand("tp @s " + x + " 121 " + (z + 1) + " 0 25");
            } else if (parts[0].equals("cablecar")) {
                mc.gui.setScreen(null);
                mc.player.connection.sendCommand("kill @e[type=!player,distance=..40]");
                int x = mc.player.getBlockX();
                int z = mc.player.getBlockZ();
                String a = (x - 12) + " 118 " + (z + 14);
                String b = (x + 12) + " 121 " + (z + 14);
                String ia = "[I;" + a.replace(" ", ",") + "]";
                String ib = "[I;" + b.replace(" ", ",") + "]";
                mc.player.connection.sendCommand("setblock " + a + " factorycraft:cable_station{Other:" + ib + ",Master:1b}");
                mc.player.connection.sendCommand("setblock " + b + " factorycraft:cable_station{Other:" + ia + ",Master:0b}");
                mc.player.connection.sendCommand("summon factorycraft:cable_car " + x + " 118 " + (z + 14) + " {From:" + ia + ",To:" + ib + ",Cycle:160}");
                mc.player.connection.sendCommand("tp @s " + x + " 122 " + (z - 4) + " 0 12");
            } else if (parts[0].equals("summon")) {
                mc.gui.setScreen(null);
                mc.player.connection.sendCommand("time set noon");
                mc.player.connection.sendCommand("kill @e[type=!player,distance=..30]");
                mc.player.connection.sendCommand("gamemode spectator");
                mc.player.connection.sendCommand("tp @s ~ 121 ~ 0 18");
                mc.player.connection.sendCommand("fill ~-8 117 ~3 ~8 117 ~18 minecraft:smooth_stone");
                mc.player.connection.sendCommand("summon factorycraft:" + parts[1] + " ~ 118 ~10 {Rotation:[60f,0f]}");
            } else if (parts[0].equals("screen")) {
                var inv = mc.player.getInventory();
                ProcessingMenu menu = switch (parts[1]) {
                    case "crusher" -> ProcessingMenu.crusher(0, inv);
                    case "washer" -> ProcessingMenu.powered(FactoryCraft.ORE_WASHER_MENU.value(), 0, inv,
                            ProcessingMenu.BIG_INPUTS, ProcessingMenu.BIG_OUTPUTS);
                    case "tannery" -> ProcessingMenu.powered(FactoryCraft.TANNERY_MENU.value(), 0, inv, 2);
                    case "quarry" -> ProcessingMenu.powered(FactoryCraft.QUARRY_MENU.value(), 0, inv, 0, ProcessingMenu.BIG_OUTPUTS);
                    case "refinery" -> ProcessingMenu.powered(FactoryCraft.REFINERY_MENU.value(), 0, inv, 1);
                    case "forge" -> ProcessingMenu.powered(FactoryCraft.TOOL_FORGE_MENU.value(), 0, inv, 3);
                    case "filler" -> ProcessingMenu.powered(FactoryCraft.FILLER_MENU.value(), 0, inv, ProcessingMenu.BIG_INPUTS, 0);
                    case "incinerator" -> ProcessingMenu.powered(FactoryCraft.INCINERATOR_MENU.value(), 0, inv, 4, 0);
                    default -> ProcessingMenu.powered(FactoryCraft.REPAIR_STATION_MENU.value(), 0, inv, 4);
                };
                mc.gui.setScreen(new ProcessingScreen(menu, inv, net.minecraft.network.chat.Component.literal(parts[1])));
            } else {
                mc.gui.setScreen(GuideScreen.atRecipes(parts[0], parts.length > 1 ? parts[1] : ""));
            }
        }
    }

    /** CI: outline every vehicle's collision segments with particles, so screenshots show the fit. */
    private static void outlineSegments() {
        Minecraft mc = Minecraft.getInstance();
        if (!GUIDE_DEMO || mc.level == null || mc.player == null || mc.level.getGameTime() % 4 != 0) return;
        var dust = new net.minecraft.core.particles.DustParticleOptions(0xFF2020, 0.7F);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof VehicleHitbox.Holder holder)) continue;
            for (var part : holder.hitbox().parts()) {
                net.minecraft.world.phys.AABB b = part.getBoundingBox();
                double[] xs = {b.minX, b.maxX};
                double[] ys = {b.minY, b.maxY};
                double[] zs = {b.minZ, b.maxZ};
                for (double y : ys) for (double z : zs) edge(mc, dust, b.minX, y, z, b.maxX, y, z);
                for (double x : xs) for (double z : zs) edge(mc, dust, x, b.minY, z, x, b.maxY, z);
                for (double x : xs) for (double y : ys) edge(mc, dust, x, y, b.minZ, x, y, b.maxZ);
            }
        }
    }

    private static void edge(Minecraft mc, net.minecraft.core.particles.ParticleOptions particle,
                             double x0, double y0, double z0, double x1, double y1, double z1) {
        double length = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int steps = Math.max(1, (int) (length / 0.2));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            mc.level.addParticle(particle, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        guideDemo();
        if (Boolean.getBoolean("factorycraft.outlineDemo")) outlineSegments();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (LIGHTS_KEY.consumeClick()) {
            if (player != null && player.getVehicle() instanceof VehicleEngine.Holder) {
                ClientPacketDistributor.sendToServer(ToggleLightsPayload.INSTANCE);
            }
        }
        while (TOOL_KEY.consumeClick()) {
            if (player != null && player.getVehicle() instanceof ToolToggle tool && tool.hasTool()) {
                ClientPacketDistributor.sendToServer(ToggleToolPayload.INSTANCE);
            }
        }
        while (ENGINE_KEY.consumeClick()) {
            if (player != null && player.getVehicle() instanceof VehicleEngine.Holder) {
                ClientPacketDistributor.sendToServer(ToggleEnginePayload.INSTANCE);
            }
        }
        if (mc.level == null || player == null || mc.isPaused()) {
            if (mc.level == null) MACHINES.clear();
            return;
        }
        updateSounds(mc, mc.level, player);
    }

    // ---------------------------------------------------------------- sounds

    /** Per machine: its looping sounds and the last engine state seen (for start/stop sounds). */
    private static final class Machine {
        LoopSound engine;
        LoopSound tracks;
        LoopSound drill;
        boolean wasRunning;
        double speed;
    }

    private static final Map<Integer, Machine> MACHINES = new HashMap<>();

    private static void updateSounds(Minecraft mc, ClientLevel level, LocalPlayer player) {
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof VehicleEngine.Holder holder) || entity.distanceToSqr(player) > HEARING_RANGE * HEARING_RANGE) {
                continue;
            }
            boolean running = holder.engine().isRunning();
            Machine machine = MACHINES.get(entity.getId());
            if (machine == null) {
                // Newly heard: don't replay the start-up sound for an engine that's already running.
                machine = new Machine();
                machine.wasRunning = running;
                MACHINES.put(entity.getId(), machine);
            }
            double moved = Math.sqrt(Mth.square(entity.getX() - entity.xo) + Mth.square(entity.getZ() - entity.zo));
            machine.speed = Mth.lerp(0.3, machine.speed, moved);

            boolean justStarted = running && !machine.wasRunning;
            if (running != machine.wasRunning) {
                SoundEvent oneShot = running ? FactoryCraft.ENGINE_START.value() : FactoryCraft.ENGINE_STOP.value();
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), oneShot, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                machine.wasRunning = running;
            }
            if (running && !isPlaying(mc, machine.engine)) {
                machine.engine = new LoopSound(FactoryCraft.ENGINE_LOOP.value(), entity, machine, LoopSound.Kind.ENGINE);
                if (justStarted) machine.engine.delayStart(25);
                mc.getSoundManager().play(machine.engine);
            }
            if (running && !isPlaying(mc, machine.tracks)) {
                machine.tracks = new LoopSound(FactoryCraft.TRACKS_LOOP.value(), entity, machine, LoopSound.Kind.TRACKS);
                mc.getSoundManager().play(machine.tracks);
            }
            if (running && entity instanceof TunnelerEntity && !isPlaying(mc, machine.drill)) {
                machine.drill = new LoopSound(FactoryCraft.DRILL_LOOP.value(), entity, machine, LoopSound.Kind.DRILL);
                mc.getSoundManager().play(machine.drill);
            }
        }
        // Forget machines that are gone or out of range (their sounds stop themselves).
        Iterator<Map.Entry<Integer, Machine>> it = MACHINES.entrySet().iterator();
        while (it.hasNext()) {
            Entity entity = level.getEntity(it.next().getKey());
            if (entity == null || !entity.isAlive() || entity.distanceToSqr(player) > HEARING_RANGE * HEARING_RANGE) it.remove();
        }
    }

    private static boolean isPlaying(Minecraft mc, LoopSound sound) {
        return sound != null && !sound.isStopped() && mc.getSoundManager().isActive(sound);
    }

    /** A looping sound attached to a machine: follows it, and its volume/pitch track the engine and speed. */
    private static final class LoopSound extends AbstractTickableSoundInstance {
        enum Kind { ENGINE, TRACKS, DRILL }

        private final Entity vehicle;
        private final Machine machine;
        private final Kind kind;
        private float level;

        LoopSound(SoundEvent event, Entity vehicle, Machine machine, Kind kind) {
            super(event, SoundSource.NEUTRAL, RandomSource.create());
            this.vehicle = vehicle;
            this.machine = machine;
            this.kind = kind;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.x = vehicle.getX();
            this.y = vehicle.getY();
            this.z = vehicle.getZ();
        }

        /** Lets the start-up sound play before the idle loop fades in. */
        void delayStart(int ticks) {
            this.level = -ticks / 20.0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            boolean running = vehicle.isAlive() && vehicle instanceof VehicleEngine.Holder holder && holder.engine().isRunning();
            if (!vehicle.isAlive() || vehicle.isRemoved()) {
                stop();
                return;
            }
            x = vehicle.getX();
            y = vehicle.getY() + 1.0;
            z = vehicle.getZ();
            float speed = (float) Math.min(1.0, machine.speed / 0.35);

            float target;
            switch (kind) {
                case ENGINE -> {
                    target = running ? 0.55F + 0.35F * speed : 0.0F;
                    pitch = 0.8F + 0.45F * speed;
                }
                case TRACKS -> {
                    target = running ? 0.9F * Mth.clamp((float) machine.speed / 0.05F, 0.0F, 1.0F) : 0.0F;
                    pitch = 0.7F + 0.6F * speed;
                }
                default -> {
                    boolean boring = running && vehicle instanceof AbstractBoat boat && boat.getPaddleState(0) && boat.getPaddleState(1)
                            && !(vehicle instanceof ToolToggle tool && !tool.toolOn());
                    target = boring ? 0.8F : 0.0F;
                    pitch = 0.9F + 0.2F * speed;
                }
            }
            // Smooth fades instead of hard cuts.
            level = Mth.approach(level, target, 0.08F);
            volume = Math.max(0.0F, level);
            if (!running && level <= 0.0F) stop();
        }
    }

    // ---------------------------------------------------------------- dashboard HUD

    private static final int PANEL = 0xB0101418;
    private static final int BORDER = 0xFF3A4047;

    private static void renderDashboard(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof VehicleEngine.Holder holder)) return;
        VehicleEngine engine = holder.engine();
        Font font = mc.font;

        int width = 92;
        if (mc.player.getVehicle() instanceof HudInfo info) width = Math.max(width, font.width(info.hudLine()) + 8);
        // Installed upgrade modules, drawn as small labelled chips that wrap.
        java.util.List<Component> chips = new java.util.ArrayList<>();
        for (Upgrade upgrade : engine.installed()) chips.add(Component.translatable(upgrade.translationKey() + ".short"));
        int chipRows = 0;
        int cx = 0;
        for (Component chip : chips) {
            int w = font.width(chip) + 4;
            if (chipRows == 0 || cx + w > width - 8) { chipRows++; cx = 0; }
            cx += w + 2;
        }
        int craneRows = mc.player.getVehicle() instanceof HudInfo ? 10 : 0;
        int height = 42 + craneRows + chipRows * 11;
        int x = graphics.guiWidth() / 2 - 91 - 6 - width;
        int y = graphics.guiHeight() - height - 2;
        if (x < 2) {
            // Narrow window: sit above the hotbar instead.
            x = graphics.guiWidth() / 2 - width / 2;
            y = graphics.guiHeight() - 22 - 40 - height;
        }
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
        graphics.fill(x, y, x + width, y + height, PANEL);

        // Engine state with the key to press.
        boolean on = engine.isRunning();
        graphics.fill(x + 4, y + 4, x + 10, y + 10, on ? 0xFF4CD964 : 0xFFD9443A);
        Component state = Component.translatable(on ? "hud.factorycraft.engine_on" : "hud.factorycraft.engine_off");
        graphics.text(font, state, x + 13, y + 3, 0xFFE8E8E8);
        String key = "[" + ENGINE_KEY.getTranslatedKeyMessage().getString() + "]";
        graphics.text(font, key, x + width - 3 - font.width(key), y + 3, 0xFF9AA3AB);

        // Fuel gauge.
        float fuel = engine.getFuelFraction();
        int barX = x + 4;
        int barY = y + 16;
        int barW = width - 8;
        int color = fuel > 0.5F ? 0xFF4CD964 : fuel > 0.2F ? 0xFFE8A21C : 0xFFD9443A;
        if (fuel <= 0.1F && (mc.player.tickCount / 10) % 2 == 0) color = 0xFF7A2A26; // low fuel blink
        graphics.fill(barX, barY, barX + barW, barY + 5, 0xFF2A2D31);
        graphics.fill(barX, barY, barX + Math.round(barW * fuel), barY + 5, color);
        Component fuelText = Component.translatable("hud.factorycraft.fuel", Math.round(fuel * 100));
        graphics.text(font, fuelText, x + 4, y + 22, 0xFFBFC7CE);
        // Headlight mode with its key.
        Component lights = Component.translatable("hud.factorycraft.lights",
                Component.translatable("hud.factorycraft.lights." + engine.lightMode().name().toLowerCase()));
        graphics.text(font, lights, x + 4, y + 32, 0xFFBFC7CE);
        String lightKey = "[" + LIGHTS_KEY.getTranslatedKeyMessage().getString() + "]";
        graphics.text(font, lightKey, x + width - 3 - font.width(lightKey), y + 32, 0xFF9AA3AB);
        if (engine.isCharging()) {
            // Solar panel charging.
            graphics.text(font, "\u2600", x + width - 12, y + 22, 0xFFFFD84A);
        }

        if (mc.player.getVehicle() instanceof HudInfo info) {
            graphics.text(font, info.hudLine(), x + 4, y + 42, 0xFFE8A21C);
        }

        int row = -1;
        int chipX = 0;
        for (Component chip : chips) {
            int w = font.width(chip) + 4;
            if (row < 0 || chipX + w > width - 8) { row++; chipX = 0; }
            int cxAbs = x + 4 + chipX;
            int cyAbs = y + 42 + craneRows + row * 11;
            graphics.fill(cxAbs, cyAbs, cxAbs + w, cyAbs + 10, 0xFF2F4A5C);
            graphics.text(font, chip, cxAbs + 2, cyAbs + 1, 0xFFDDEEFF);
            chipX += w + 2;
        }
    }
}
