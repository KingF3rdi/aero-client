package dev.aero.client.hud;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the track any Windows app (Spotify, browsers, Apple Music, VLC, ...) publishes to the system
 * media controls, through one long-running PowerShell process that prints "status|artist|title|app"
 * every 2 seconds. Windows only; does nothing elsewhere.
 */
public final class MediaSession {
    private static final String SCRIPT = String.join("\n",
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
            "$g = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]",
            "function Await($task, $type) { $m = $g.MakeGenericMethod($type); $t = $m.Invoke($null, @($task)); $t.Wait(-1) | Out-Null; $t.Result }",
            "[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]",
            "[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]",
            "$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
            "while ($true) {",
            "  $out = ''",
            "  try {",
            "    $pick = $null",
            "    foreach ($s in $mgr.GetSessions()) {",
            "      if ($s.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing') { $pick = $s; break }",
            "    }",
            "    if ($pick) {",
            "      $p = Await ($pick.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
            "      $out = 'Playing|' + $p.Artist + '|' + $p.Title",
            "    }",
            "  } catch {}",
            "  [Console]::Out.WriteLine($out); [Console]::Out.Flush()",
            "  Start-Sleep -Seconds 2",
            "}");

    private static Process proc;
    private static volatile String track = "";

    private MediaSession() {}

    public static String track() {
        return track;
    }

    public static synchronized void ensureStarted() {
        if (proc != null && proc.isAlive()) {
            return;
        }
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try {
            Path file = Files.createTempFile("aero_media", ".ps1");
            file.toFile().deleteOnExit();
            Files.writeString(file, SCRIPT, StandardCharsets.UTF_8);
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", file.toString()).redirectErrorStream(false).start();
            proc = p;
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
            Thread reader = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        track = parse(line);
                    }
                } catch (Exception ignored) {
                }
                track = "";
            }, "aero-media");
            reader.setDaemon(true);
            reader.start();
        } catch (Exception ignored) {
        }
    }

    public static synchronized void stop() {
        if (proc != null) {
            proc.destroyForcibly();
            proc = null;
            track = "";
        }
    }

    private static String parse(String line) {
        String[] parts = line.split("[|]", 3);
        if (parts.length < 3 || !"Playing".equals(parts[0]) || parts[2].isBlank()) {
            return "";
        }
        return parts[1].isBlank() ? parts[2].trim() : parts[1].trim() + " - " + parts[2].trim();
    }
}
