package dev.aero.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Aero loader step: runs before Minecraft itself starts (Fabric preLaunch). First the auto-updater: when
 * GitHub has a newer Aero release, a small window downloads and installs it before the game loads. Then, when
 * mods are installed that do
 * the same job as an Aero module, a small window lists them (name + mod id) with Open mods folder, Quit
 * game, Play anyway and "Disable them & quit" - the game only continues once the player picked something.
 * Nothing here may touch Minecraft classes (they aren't loaded yet), so it's plain Swing + Fabric Loader.
 * macOS is skipped (AWT and GLFW both want the main thread there); the in-game ConflictScreen covers it.
 */
public final class AeroPreLaunch implements PreLaunchEntrypoint {
    private enum Choice { PLAY, QUIT, DISABLE_QUIT }

    @Override
    public void onPreLaunch() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("mac") || Boolean.getBoolean("aero.noPreLaunch")) {
            return;
        }
        update();
        checkConflicts();
    }

    /** Auto-update before start: nothing is shown when Aero is current, offline or a dev build. */
    private static void update() {
        try {
            if (!configFlag("autoUpdateBeforeStart", true)) {
                return;
            }
            ModUpdater.Release release = ModUpdater.latestIfNewer();
            if (release == null) {
                return;
            }
            System.setProperty("java.awt.headless", "false");
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            if (showWindow(done -> new UpdateCard(release, done)) == Choice.QUIT) {
                System.exit(0);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void checkConflicts() {
        try {
            if (configFlag("conflictsIgnored", false)) {
                return;
            }
            List<ModConflicts.Conflict> conflicts = ModConflicts.find();
            if (conflicts.isEmpty()) {
                return;
            }
            System.setProperty("java.awt.headless", "false");
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                return;
            }
            ModConflicts.handledBeforeStart = true;
            Choice choice = showWindow(done -> new Card(conflicts, done));
            if (choice == Choice.DISABLE_QUIT) {
                conflicts.forEach(ModConflicts::disable);
            }
            if (choice != Choice.PLAY) {
                System.exit(0);
            }
        } catch (Throwable t) {
            // Never block the game because of this window.
            ModConflicts.handledBeforeStart = false;
        }
    }

    /** A boolean from the Aero config, read raw (the config class may touch game classes). */
    private static boolean configFlag(String key, boolean fallback) {
        try {
            Path cfg = FabricLoader.getInstance().getConfigDir().resolve("aero-client.json");
            if (!Files.isRegularFile(cfg)) {
                return fallback;
            }
            JsonObject o = JsonParser.parseString(Files.readString(cfg)).getAsJsonObject();
            return o.has(key) ? o.get(key).getAsBoolean() : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    /** Shows a borderless Aero window and blocks until its content reports a choice. */
    private static Choice showWindow(java.util.function.Function<java.util.function.Consumer<Choice>, JComponent> content) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Choice[] result = {Choice.PLAY};
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Aero Client");
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            try {
                var icon = AeroPreLaunch.class.getResource("/assets/aero/icon.png");
                if (icon != null) {
                    frame.setIconImage(javax.imageio.ImageIO.read(icon));
                }
            } catch (Throwable ignored) {
            }
            JComponent card = content.apply(c -> SwingUtilities.invokeLater(() -> {
                result[0] = c;
                frame.dispose();
                done.countDown();
            }));
            frame.setContentPane(card);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
        });
        done.await();
        return result[0];
    }

    /** The card, painted by hand in the white Aero look. */
    private static final class Card extends JComponent {
        private static final int W = 440;
        private static final int ROW = 30;
        private static final int MAX_ROWS = 9;
        private static final Color BG = new Color(0xF6F8FC);
        private static final Color LINE = new Color(0xE1E5EE);
        private static final Color TEXT = new Color(0x161922);
        private static final Color MUTED = new Color(0x6A7182);
        private static final Color ROW_BG = new Color(0xEDF0F6);
        private static final Color ACCENT = new Color(0x4F8EFF);

        private final List<ModConflicts.Conflict> conflicts;
        private final java.util.function.Consumer<Choice> onChoice;
        private final Font title;
        private final Font body;
        private final Font small;
        private int scroll;
        private Rectangle hover;
        private int dragX;
        private int dragY;

        Card(List<ModConflicts.Conflict> conflicts, java.util.function.Consumer<Choice> onChoice) {
            this.conflicts = conflicts;
            this.onChoice = onChoice;
            String face = System.getProperty("os.name", "").toLowerCase().contains("win") ? "Segoe UI" : Font.SANS_SERIF;
            title = new Font(face, Font.BOLD, 16);
            body = new Font(face, Font.PLAIN, 13);
            small = new Font(face, Font.PLAIN, 12);
            setOpaque(false);
            setPreferredSize(new Dimension(W, height()));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragX = e.getX();
                    dragY = e.getY();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    var w = SwingUtilities.getWindowAncestor(Card.this);
                    w.setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Rectangle h = null;
                    for (Rectangle r : buttons()) {
                        if (r.contains(e.getPoint())) {
                            h = r;
                        }
                    }
                    if (link().contains(e.getPoint())) {
                        h = link();
                    }
                    if (!java.util.Objects.equals(h, hover)) {
                        hover = h;
                        setCursor(Cursor.getPredefinedCursor(h != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    Rectangle[] b = buttons();
                    if (b[0].contains(e.getPoint())) {
                        openModsFolder();
                    } else if (b[1].contains(e.getPoint())) {
                        onChoice.accept(Choice.QUIT);
                    } else if (b[2].contains(e.getPoint())) {
                        onChoice.accept(Choice.PLAY);
                    } else if (link().contains(e.getPoint())) {
                        onChoice.accept(Choice.DISABLE_QUIT);
                    }
                }

                @Override
                public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                    int max = Math.max(0, conflicts.size() - MAX_ROWS) * ROW;
                    scroll = Math.max(0, Math.min(max, scroll + e.getWheelRotation() * ROW));
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            addMouseWheelListener(m);
        }

        private int listH() {
            return Math.min(conflicts.size(), MAX_ROWS) * ROW;
        }

        private int height() {
            return 78 + listH() + 96;
        }

        private Rectangle[] buttons() {
            int y = height() - 50;
            int bw = (W - 40 - 16) / 3;
            return new Rectangle[]{new Rectangle(20, y, bw, 32), new Rectangle(20 + bw + 8, y, bw, 32),
                    new Rectangle(20 + (bw + 8) * 2, y, bw, 32)};
        }

        private Rectangle link() {
            FontMetrics fm = getFontMetrics(small);
            String s = "Disable them for me & quit";
            return new Rectangle(22, 78 + listH() + 26, fm.stringWidth(s), fm.getHeight());
        }

        private void openModsFolder() {
            try {
                Desktop.getDesktop().open(FabricLoader.getInstance().getGameDir().resolve("mods").toFile());
            } catch (Throwable ignored) {
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BG);
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 22, 22));
            g.setColor(LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 22, 22));

            // Header: Aero mark + title
            g.setColor(new Color(79, 142, 255, 40));
            g.fillOval(20, 20, 34, 34);
            g.setColor(ACCENT);
            g.setFont(new Font(title.getName(), Font.BOLD, 20));
            FontMetrics fa = g.getFontMetrics();
            g.drawString("A", 37 - fa.stringWidth("A") / 2, 44);
            g.setColor(TEXT);
            g.setFont(title);
            g.drawString("Aero can't run alongside these mods", 66, 36);
            g.setColor(MUTED);
            g.setFont(small);
            g.drawString("Each one does the same job as an Aero feature", 66, 54);

            // Mod list
            Graphics2D list = (Graphics2D) g.create();
            list.clipRect(16, 72, w - 32, listH() + 2);
            int y = 74 - scroll;
            for (ModConflicts.Conflict c : conflicts) {
                list.setColor(ROW_BG);
                list.fill(new RoundRectangle2D.Float(20, y, w - 40, ROW - 5, 10, 10));
                list.setFont(body);
                list.setColor(TEXT);
                FontMetrics fm = list.getFontMetrics();
                list.drawString(c.name, 32, y + 17);
                list.setFont(small);
                list.setColor(MUTED);
                FontMetrics fs = list.getFontMetrics();
                list.drawString(c.id, w - 32 - fs.stringWidth(c.id), y + 17);
                y += ROW;
            }
            list.dispose();

            g.setFont(small);
            g.setColor(MUTED);
            int fy = 78 + listH() + 12;
            g.drawString("Disable or remove them, then start the game again.", 22, fy + 8);
            Rectangle lk = link();
            g.setColor(ACCENT);
            g.drawString("Disable them for me & quit", lk.x, lk.y + g.getFontMetrics().getAscent());
            if (lk.equals(hover)) {
                g.fillRect(lk.x, lk.y + g.getFontMetrics().getAscent() + 2, lk.width, 1);
            }

            String[] labels = {"Open mods folder", "Quit game", "Play anyway"};
            Rectangle[] b = buttons();
            for (int i = 0; i < 3; i++) {
                Rectangle r = b[i];
                boolean hv = r.equals(hover);
                RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, r.height, r.height);
                if (i == 2) {
                    g.setColor(hv ? ACCENT.darker() : ACCENT);
                    g.fill(shape);
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(hv ? Color.WHITE : new Color(0xFBFCFE));
                    g.fill(shape);
                    g.setColor(LINE);
                    g.draw(shape);
                    g.setColor(TEXT);
                }
                g.setFont(body);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(labels[i], r.x + (r.width - fm.stringWidth(labels[i])) / 2,
                        r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /** Update window: download progress, then "Quit & restart" / "Play now". */
    private static final class UpdateCard extends JComponent {
        private static final int W = 420;
        private static final int H = 150;
        private static final Color BG = new Color(0xF6F8FC);
        private static final Color LINE = new Color(0xE1E5EE);
        private static final Color TEXT = new Color(0x161922);
        private static final Color MUTED = new Color(0x6A7182);
        private static final Color ACCENT = new Color(0x4F8EFF);

        private final ModUpdater.Release release;
        private final java.util.function.Consumer<Choice> onChoice;
        private final Font title;
        private final Font body;
        private volatile double progress;
        private volatile int phase; // 0 downloading, 1 done, 2 failed
        private Rectangle hover;
        private int dragX;
        private int dragY;

        UpdateCard(ModUpdater.Release release, java.util.function.Consumer<Choice> onChoice) {
            this.release = release;
            this.onChoice = onChoice;
            String face = System.getProperty("os.name", "").toLowerCase().contains("win") ? "Segoe UI" : Font.SANS_SERIF;
            title = new Font(face, Font.BOLD, 16);
            body = new Font(face, Font.PLAIN, 13);
            setOpaque(false);
            setPreferredSize(new Dimension(W, H));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragX = e.getX();
                    dragY = e.getY();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    SwingUtilities.getWindowAncestor(UpdateCard.this).setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Rectangle h = null;
                    if (phase == 1) {
                        for (Rectangle r : buttons()) {
                            if (r.contains(e.getPoint())) {
                                h = r;
                            }
                        }
                    }
                    if (!java.util.Objects.equals(h, hover)) {
                        hover = h;
                        setCursor(Cursor.getPredefinedCursor(h != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (phase != 1) {
                        return;
                    }
                    Rectangle[] b = buttons();
                    if (b[0].contains(e.getPoint())) {
                        onChoice.accept(Choice.QUIT);
                    } else if (b[1].contains(e.getPoint())) {
                        onChoice.accept(Choice.PLAY);
                    }
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            Thread worker = new Thread(() -> {
                boolean ok = ModUpdater.installBlocking(release, p -> {
                    progress = p;
                    repaint();
                });
                phase = ok ? 1 : 2;
                repaint();
                if (!ok) {
                    try {
                        Thread.sleep(1600);
                    } catch (InterruptedException ignored) {
                    }
                    onChoice.accept(Choice.PLAY); // failed: just start with the current version
                }
            }, "aero-prelaunch-update");
            worker.setDaemon(true);
            worker.start();
        }

        private Rectangle[] buttons() {
            int bw = (W - 40 - 8) / 2;
            return new Rectangle[]{new Rectangle(20, H - 50, bw, 32), new Rectangle(20 + bw + 8, H - 50, bw, 32)};
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BG);
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 22, 22));
            g.setColor(LINE);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 22, 22));
            g.setColor(new Color(79, 142, 255, 40));
            g.fillOval(20, 20, 34, 34);
            g.setColor(ACCENT);
            g.setFont(new Font(title.getName(), Font.BOLD, 20));
            FontMetrics fa = g.getFontMetrics();
            g.drawString("A", 37 - fa.stringWidth("A") / 2, 44);

            String head = phase == 1 ? "Aero Client updated to " + release.tag()
                    : phase == 2 ? "Update failed" : "Updating Aero Client to " + release.tag();
            String sub = phase == 1 ? "Restart the game to play the new version"
                    : phase == 2 ? "Starting with your current version" : "Downloading before the game starts...";
            g.setColor(TEXT);
            g.setFont(title);
            g.drawString(head, 66, 36);
            g.setColor(MUTED);
            g.setFont(body);
            g.drawString(sub, 66, 54);

            if (phase == 0) {
                int bx = 20;
                int by = H - 40;
                int bw = W - 40;
                g.setColor(new Color(0xE3E7EE));
                g.fill(new RoundRectangle2D.Float(bx, by, bw, 8, 8, 8));
                g.setColor(ACCENT);
                g.fill(new RoundRectangle2D.Float(bx, by, Math.max(8, (float) (bw * progress)), 8, 8, 8));
                g.setColor(MUTED);
                String pct = Math.round(progress * 100) + " %";
                g.drawString(pct, W - 20 - g.getFontMetrics().stringWidth(pct), by - 8);
            } else if (phase == 1) {
                String[] labels = {"Quit & restart", "Play now"};
                Rectangle[] b = buttons();
                for (int i = 0; i < 2; i++) {
                    Rectangle r = b[i];
                    boolean hv = r.equals(hover);
                    RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, r.height, r.height);
                    if (i == 0) {
                        g.setColor(hv ? ACCENT.darker() : ACCENT);
                        g.fill(shape);
                        g.setColor(Color.WHITE);
                    } else {
                        g.setColor(hv ? Color.WHITE : new Color(0xFBFCFE));
                        g.fill(shape);
                        g.setColor(LINE);
                        g.draw(shape);
                        g.setColor(TEXT);
                    }
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(labels[i], r.x + (r.width - fm.stringWidth(labels[i])) / 2,
                            r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
                }
            }
            g.dispose();
        }
    }
}
