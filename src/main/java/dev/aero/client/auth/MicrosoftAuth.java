package dev.aero.client.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Official Microsoft → Xbox → Minecraft Services login.
 * Requires a purchased Java Edition account. No offline/cracked login.
 */
public final class MicrosoftAuth {
    public static final String CLIENT_ID = "00000000402b5328";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private MicrosoftAuth() {}

    public static DeviceCode startDeviceCode() throws Exception {
        String body = form(Map.of(
                "client_id", CLIENT_ID,
                "scope", "XboxLive.signin offline_access"
        ));
        JsonObject json = postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", body);
        DeviceCode code = new DeviceCode();
        code.userCode = text(json, "user_code");
        code.deviceCode = text(json, "device_code");
        code.verificationUri = first(json, "verification_uri", "verification_uri_complete");
        if (code.verificationUri == null || code.verificationUri.isBlank()) {
            code.verificationUri = "https://www.microsoft.com/link";
        }
        code.interval = json.has("interval") ? json.get("interval").getAsInt() : 5;
        code.expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 900;
        if (code.userCode.isBlank() || code.deviceCode.isBlank()) {
            throw new IllegalStateException("Device-Code fehlgeschlagen: " + json);
        }
        return code;
    }

    public static JsonObject pollToken(String deviceCode) throws Exception {
        String body = form(Map.of(
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", CLIENT_ID,
                "device_code", deviceCode
        ));
        return postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body);
    }

    public static SavedAccount loginWithMsAccessToken(String msAccess, String msRefresh) throws Exception {
        JsonObject xbox = xboxAuth(msAccess);
        String xboxToken = text(xbox, "Token");
        String uhs = xbox.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject xsts = xstsAuth(xboxToken);
        if (xsts.has("XErr")) {
            throw new IllegalStateException("Xbox Live hat die Anmeldung abgelehnt (XErr " + xsts.get("XErr") + ")");
        }
        String xstsToken = text(xsts, "Token");
        String xstsUhs = xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject mc = postJson(
                "https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":\"XBL3.0 x=" + xstsUhs + ";" + xstsToken + "\"}"
        );
        String mcToken = text(mc, "access_token");
        if (mcToken.isBlank()) {
            throw new IllegalStateException("Minecraft-Token fehlt");
        }
        if (!ownsJava(mcToken)) {
            throw new IllegalStateException("Dieser Microsoft-Account besitzt keine Minecraft: Java Edition.");
        }
        JsonObject profile = getJson("https://api.minecraftservices.com/minecraft/profile", mcToken);
        if (profile.has("error") || !profile.has("id") || !profile.has("name")) {
            throw new IllegalStateException("Kein Java-Profil (Account hat Minecraft Java nicht oder Profil fehlt).");
        }
        SavedAccount account = new SavedAccount();
        account.name = text(profile, "name");
        account.uuid = dashUuid(text(profile, "id"));
        account.mcToken = mcToken;
        account.msRefresh = msRefresh == null ? "" : msRefresh;
        account.xuid = uhs;
        return account;
    }

    public static SavedAccount refresh(String refreshToken) throws Exception {
        String body = form(Map.of(
                "grant_type", "refresh_token",
                "client_id", CLIENT_ID,
                "refresh_token", refreshToken
        ));
        JsonObject json = postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body);
        if (json.has("error")) {
            throw new IllegalStateException(text(json, "error_description"));
        }
        return loginWithMsAccessToken(text(json, "access_token"), text(json, "refresh_token"));
    }

    private static boolean ownsJava(String mcToken) throws Exception {
        JsonObject store = getJson("https://api.minecraftservices.com/entitlements/mcstore", mcToken);
        JsonArray items = store.has("items") ? store.getAsJsonArray("items") : new JsonArray();
        for (JsonElement el : items) {
            String name = text(el.getAsJsonObject(), "name").toLowerCase();
            if (name.contains("minecraft") || name.contains("product_minecraft") || name.contains("game_minecraft")) {
                return true;
            }
        }
        return items.isEmpty();
    }

    private static JsonObject xboxAuth(String msAccess) throws Exception {
        String rps = msAccess.startsWith("d=") ? msAccess : "d=" + msAccess;
        String payload = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\""
                + rps + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
        return postJson("https://user.auth.xboxlive.com/user/authenticate", payload);
    }

    private static JsonObject xstsAuth(String xboxToken) throws Exception {
        String payload = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
                + xboxToken + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
        return postJson("https://xsts.auth.xboxlive.com/xsts/authorize", payload);
    }

    private static JsonObject postForm(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static JsonObject postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static JsonObject getJson(String url, String bearer) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + bearer)
                .GET()
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static String form(Map<String, String> fields) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : new LinkedHashMap<>(fields).entrySet()) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return out.toString();
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String first(JsonObject o, String a, String b) {
        String va = text(o, a);
        return va.isBlank() ? text(o, b) : va;
    }

    private static UUID dashUuid(String raw) {
        String hex = raw.replace("-", "");
        return UUID.fromString(hex.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"));
    }

    public static final class DeviceCode {
        public String userCode = "";
        public String deviceCode = "";
        public String verificationUri = "";
        public int interval = 5;
        public int expiresIn = 900;
    }
}
