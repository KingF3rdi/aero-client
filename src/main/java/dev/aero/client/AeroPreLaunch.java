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
 * Aero loader step: runs before Minecraft itself starts (Fabric preLaunch). When mods are installed that do
 * the same job as an Aero module, a small window lists them (name + mod id) with Open mods folder, Quit
 * game, Play anyway and "Remove them & quit" - the game only continues once the player picked something.
 * Nothing here may touch Minecraft classes (they aren't loaded yet), so it's plain Swing + Fabric Loader.
 * macOS is skipped (AWT and GLFW both want the main thread there); the in-game ConflictScreen covers it.
 */
public final class AeroPreLaunch implements PreLaunchEntrypoint {
    private enum Choice { PLAY, QUIT, REMOVE_QUIT }

    @Override
    public void onPreLaunch() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
            if (os.contains("mac") || Boolean.getBoolean("aero.noPreLaunch") || conflictsIgnored()) {
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
            Choice choice = ask(conflicts);
            if (choice == Choice.REMOVE_QUIT) {
                conflicts.forEach(ModConflicts::remove);
            }
            if (choice != Choice.PLAY) {
                System.exit(0);
            }
        } catch (Throwable t) {
            // Never block the game because of this window.
            ModConflicts.handledBeforeStart = false;
        }
    }

    /** "Don't ask again" from the Aero config, read raw (the config class may touch game classes). */
    private static boolean conflictsIgnored() {
        try {
            Path cfg = FabricLoader.getInstance().getConfigDir().resolve("aero-client.json");
            if (!Files.isRegularFile(cfg)) {
                return false;
            }
            JsonObject o = JsonParser.parseString(Files.readString(cfg)).getAsJsonObject();
            return o.has("conflictsIgnored") && o.get("conflictsIgnored").getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    private static Choice ask(List<ModConflicts.Conflict> conflicts) throws Exception {
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
            Card card = new Card(conflicts, c -> {
                result[0] = c;
                frame.dispose();
                done.countDown();
            });
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
                        onChoice.accept(Choice.REMOVE_QUIT);
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
            String s = "Remove them for me & quit";
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
            g.drawString("Remove them from your mods folder, then start the game again.", 22, fy + 8);
            Rectangle lk = link();
            g.setColor(ACCENT);
            g.drawString("Remove them for me & quit", lk.x, lk.y + g.getFontMetrics().getAscent());
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
}
