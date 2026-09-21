package dev.aero.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /at &lt;name&gt; (or /autotext) sends the preset message from the Auto Text module right away, in the same tick:
 * no chat screen, no delay. Presets are "name=message; name=message"; an unknown name is sent as typed.
 */
public final class AutoTextCommand {
    private AutoTextCommand() {}

    static Map<String, String> presets() {
        Map<String, String> out = new LinkedHashMap<>();
        var cfg = AeroClient.CONFIG;
        if (cfg == null || cfg.autoTextPresets == null) {
            return out;
        }
        for (String part : cfg.autoTextPresets.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0 && eq < part.length() - 1) {
                out.put(part.substring(0, eq).trim().toLowerCase(), part.substring(eq + 1).trim());
            }
        }
        return out;
    }

    public static void send(MinecraftClient mc, String arg) {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        String msg = presets().getOrDefault(arg.trim().toLowerCase(), arg.trim());
        if (msg.isEmpty()) {
            return;
        }
        if (msg.startsWith("/")) {
            mc.getNetworkHandler().sendChatCommand(msg.substring(1));
        } else {
            mc.getNetworkHandler().sendChatMessage(msg);
        }
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[]{"at", "autotext"}) {
                dispatcher.register(ClientCommandManager.literal(name)
                        .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> {
                                    presets().keySet().forEach(b::suggest);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    send(MinecraftClient.getInstance(), StringArgumentType.getString(ctx, "message"));
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("Auto Text: /at <" + String.join("|", presets().keySet()) + ">"));
                            return 1;
                        }));
            }
        });
    }
}
