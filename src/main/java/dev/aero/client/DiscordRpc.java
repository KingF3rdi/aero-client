package dev.aero.client;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Discord IPC client (local named pipe only, talks to the user's own already-running
 * Discord app - nothing leaves the machine besides what Discord itself displays). Requires the
 * user's own Discord Application client ID (from the Discord Developer Portal) in
 * ClientConfig.discordClientId; does nothing if that's left empty, since Discord's handshake
 * protocol requires a registered client ID and there is no generic/shared one to fall back to.
 */
public final class DiscordRpc {
    private static RandomAccessFile pipe;
    private static Thread thread;
    private static volatile boolean running;
    private static String connectedClientId;

    private DiscordRpc() {}

    public static void tick() {
        var cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        String clientId = cfg.discordClientId;
        boolean wantConnected = cfg.discordRpc && clientId != null && !clientId.isBlank();
        if (wantConnected && (!running || !clientId.equals(connectedClientId))) {
            start(clientId);
        } else if (!wantConnected && running) {
            stop();
        }
    }

    private static synchronized void start(String clientId) {
        stop();
        connectedClientId = clientId;
        running = true;
        thread = new Thread(() -> run(clientId), "aero-discord-rpc");
        thread.setDaemon(true);
        thread.start();
    }

    private static synchronized void stop() {
        running = false;
        try {
            if (pipe != null) {
                pipe.close();
            }
        } catch (Exception ignored) {
        }
        pipe = null;
        connectedClientId = null;
    }

    private static void run(String clientId) {
        RandomAccessFile local = null;
        for (int i = 0; i < 10 && running; i++) {
            try {
                local = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                break;
            } catch (Exception ignored) {
                local = null;
            }
        }
        pipe = local;
        if (pipe == null) {
            running = false;
            return;
        }
        try {
            handshake(clientId);
            long lastUpdate = 0;
            while (running) {
                long now = System.currentTimeMillis();
                if (now - lastUpdate > 15000) {
                    sendActivity();
                    lastUpdate = now;
                }
                Thread.sleep(500);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                pipe.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void handshake(String clientId) throws Exception {
        writeFrame(0, "{\"v\":1,\"client_id\":\"" + escape(clientId) + "\"}");
    }

    private static void sendActivity() throws Exception {
        long startedAt = System.currentTimeMillis() / 1000L;
        String json = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                + ",\"activity\":{\"state\":\"Aero Client\",\"details\":\"Using the client\","
                + "\"timestamps\":{\"start\":" + startedAt + "}}},\"nonce\":\"" + java.util.UUID.randomUUID() + "\"}";
        writeFrame(1, json);
    }

    private static void writeFrame(int opcode, String json) throws Exception {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(data.length);
        pipe.write(header.array());
        pipe.write(data);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
