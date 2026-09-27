package dev.aero.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat translation (Chat module): other players' messages get a translated line under them, and a key
 * in the chat box translates your own message before you send it. Uses Google's public translate
 * endpoint; only the message text is sent, and only while the option is on.
 */
public final class ChatTranslate {
    public static final String[] NAMES = {"English", "German", "Spanish", "French", "Italian", "Portuguese", "Dutch",
            "Polish", "Turkish", "Russian", "Ukrainian", "Swedish", "Czech", "Japanese", "Korean", "Chinese", "Arabic"};
    private static final String[] CODES = {"en", "de", "es", "fr", "it", "pt", "nl", "pl", "tr", "ru", "uk", "sv", "cs",
            "ja", "ko", "zh-CN", "ar"};
    /** "<Name> msg", "Name: msg", "Name » msg", "[Rank] Name > msg" - the common chat shapes. */
    private static final Pattern CHAT = Pattern.compile(
            "^(?:[\\[(][^\\])]{0,24}[\\])]\\s*)*(?:<([A-Za-z0-9_]{3,16})>|([A-Za-z0-9_]{3,16})\\s*[:»>|])\\s*(.{2,})$");
    public static final String MARK = "  ↳ ";

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aero-translate");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, String> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> e) {
            return size() > 200;
        }
    };
    private static boolean toggleWasDown;

    private ChatTranslate() {}

    public static String code(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(name)) {
                return CODES[i];
            }
        }
        return "en";
    }

    /** Toggle key, checked every client tick outside of screens. */
    public static void tick(MinecraftClient mc) {
        var c = AeroClient.CONFIG;
        if (c == null || c.chatTranslateToggleKey < 0 || mc.getWindow() == null || mc.currentScreen != null) {
            toggleWasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), c.chatTranslateToggleKey) == GLFW.GLFW_PRESS;
        if (down && !toggleWasDown) {
            c.chatTranslate = !c.chatTranslate;
            c.save();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Chat translation " + (c.chatTranslate ? "on" : "off")), true);
            }
        }
        toggleWasDown = down;
    }

    /** Called for every chat line; translates it when it looks like another player's message. */
    public static void onIncoming(Text message) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.chatTranslate || message == null) {
            return;
        }
        String line = message.getString();
        if (line.startsWith(MARK) || line.length() > 256) {
            return;
        }
        Matcher m = CHAT.matcher(line.replaceAll("§.", "").trim());
        if (!m.matches()) {
            return;
        }
        String sender = m.group(1) != null ? m.group(1) : m.group(2);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (sender == null || sender.equalsIgnoreCase(mc.getSession().getUsername())) {
            return;
        }
        String body = m.group(3).trim();
        if (body.codePoints().noneMatch(Character::isLetter)) {
            return;
        }
        translate(body, code(c.chatTranslateLang), out -> {
            if (out != null && mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(Text.literal(MARK + out).formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        });
    }

    /**
     * Translates text into target; the callback runs on the client thread with null when it failed or the
     * text already was in that language.
     */
    public static void translate(String text, String target, Consumer<String> done) {
        String key = target + "|" + text;
        String hit;
        synchronized (CACHE) {
            hit = CACHE.get(key);
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (hit != null) {
            mc.execute(() -> done.accept(hit.isEmpty() ? null : hit));
            return;
        }
        POOL.execute(() -> {
            String out = null;
            try {
                String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&dt=t&tl="
                        + URLEncoder.encode(target, StandardCharsets.UTF_8) + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                        .header("user-agent", "Mozilla/5.0").GET().build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    JsonArray root = JsonParser.parseString(r.body()).getAsJsonArray();
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement seg : root.get(0).getAsJsonArray()) {
                        JsonArray a = seg.getAsJsonArray();
                        if (!a.get(0).isJsonNull()) {
                            sb.append(a.get(0).getAsString());
                        }
                    }
                    String detected = root.size() > 2 && root.get(2).isJsonPrimitive() ? root.get(2).getAsString() : "";
                    String res = sb.toString().trim();
                    boolean same = detected.equalsIgnoreCase(target) || detected.equalsIgnoreCase(target.split("-")[0])
                            || res.equalsIgnoreCase(text.trim());
                    out = same || res.isEmpty() ? "" : res;
                    synchronized (CACHE) {
                        CACHE.put(key, out);
                    }
                }
            } catch (Exception ignored) {
            }
            String result = out == null || out.isEmpty() ? null : out;
            mc.execute(() -> done.accept(result));
        });
    }
}
