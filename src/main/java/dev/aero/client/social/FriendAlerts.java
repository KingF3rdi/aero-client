package dev.aero.client.social;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.Notifications;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * "A friend comes online": a toast (and a sound) when a friend appears in the server's player list.
 * Friends already there when you join are the baseline, so joining a server doesn't flood you.
 */
public final class FriendAlerts {
    private static Set<String> seen;
    private static int ticks;

    private FriendAlerts() {}

    public static void tick(MinecraftClient mc) {
        var cfg = AeroClient.CONFIG;
        if (mc.getNetworkHandler() == null || cfg == null) {
            seen = null; // left the server: the next join takes a new baseline
            return;
        }
        if (++ticks % 20 != 0) {
            return;
        }
        Set<String> now = new HashSet<>();
        for (var entry : mc.getNetworkHandler().getPlayerList()) {
            var profile = entry.getProfile();
            if (profile != null && profile.name() != null && FriendStore.isFriend(profile.name())) {
                now.add(profile.name());
            }
        }
        if (seen != null && cfg.notifyFriendOnline) {
            for (String name : now) {
                if (!seen.contains(name.toLowerCase(Locale.ROOT))) {
                    Notifications.show(name + " is online", true);
                    if (cfg.notifySound && mc.player != null) {
                        mc.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.6f, 1.6f);
                    }
                }
            }
        }
        Set<String> lower = new HashSet<>();
        now.forEach(n -> lower.add(n.toLowerCase(Locale.ROOT)));
        seen = lower;
    }
}
