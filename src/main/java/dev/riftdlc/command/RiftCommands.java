package dev.riftdlc.command;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.ui.RiftScreen;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

public final class RiftCommands {
    private final RiftDLC app;
    public RiftCommands(RiftDLC app) { this.app = app; }
    public static boolean isCommand(String message) {
        return message.equalsIgnoreCase(".rift") || message.toLowerCase(Locale.ROOT).startsWith(".rift ");
    }
    public boolean handle(String message) {
        if (!isCommand(message)) return false;
        try { reply(execute(message.substring(5).trim().split("\\s+"))); }
        catch (RuntimeException e) { reply("Error: " + e.getMessage()); }
        return true;
    }
    public String execute(String[] args) {
        if (args.length == 0 || args[0].isBlank()) return "Commands: gui, modules, toggle, bind, friend, protocol, config";
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("gui")) {
            app.requestGui();
            return "Opening GUI";
        }
        if (action.equals("modules")) return String.join(", ", app.modules().all().stream().map(RiftModule::id).toList());
        if (action.equals("toggle") || action.equals("bind")) {
            if (args.length < 2) return "Usage: .rift " + action + " <module>" + (action.equals("bind") ? " <key|none>" : "");
            RiftModule module = app.modules().get(args[1].toLowerCase(Locale.ROOT));
            if (module == null) return "Unknown module: " + args[1];
            if (action.equals("toggle")) { module.setEnabled(!module.enabled()); app.save(); return module.name() + ": " + module.enabled(); }
            if (args.length < 3) return "Usage: .rift bind <module> <key|none>";
            int key = args[2].equalsIgnoreCase("none") ? -1 : 0;
            if (key != -1) {
                String name = args[2].toUpperCase(Locale.ROOT).replace("RIGHT_SHIFT", "RSHIFT").replace("LEFT_SHIFT", "LSHIFT");
                try { key = InputConstants.class.getField("KEY_" + name).getInt(null); }
                catch (ReflectiveOperationException e) { return "Unknown key: " + args[2]; }
            }
            module.bind(key); app.save(); return module.name() + " bound to " + args[2];
        }
        if (action.equals("friend")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("list")) return "Friends: " + String.join(", ", app.friends().all());
            if (args.length < 3) return "Usage: .rift friend <add|remove|list> <name>";
            boolean changed = switch (args[1].toLowerCase(Locale.ROOT)) {
                case "add" -> app.friends().add(args[2]);
                case "remove" -> app.friends().remove(args[2]);
                default -> throw new IllegalArgumentException("Unknown friend action");
            };
            app.save(); return changed ? "Friends updated" : "No change";
        }
        if (action.equals("protocol")) {
            if (args.length < 2) return app.protocols().available() ? "Protocol: " + app.protocols().selected() : "Protocol translation unavailable: ViaFabricPlus not installed";
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return app.protocols().select(name) ? "Protocol on next connection: " + name : "Protocol unavailable; install ViaFabricPlus or disconnect";
        }
        if (action.equals("config")) {
            if (args.length < 2) return "Usage: .rift config <save|reload>";
            if (args[1].equalsIgnoreCase("save")) { app.save(); return "Config saved"; }
            if (args[1].equalsIgnoreCase("reload")) { app.reload(); return "Config reloaded"; }
        }
        return "Unknown command";
    }
    private static void reply(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[RiftDLC] " + message));
    }
}
