package dev.aero.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hovering a resource pack in the pack screen shows what it changes: its sword, crystal, totem, pearl,
 * gapple, armour and a few blocks, read straight out of the pack (only textures the pack actually has).
 */
public final class PackPreview {
    private static final String[][] SLOTS = {
            {"textures/item/netherite_sword.png", "Sword"},
            {"textures/item/diamond_sword.png", "Sword"},
            {"textures/item/end_crystal.png", "Crystal"},
            {"textures/item/totem_of_undying.png", "Totem"},
            {"textures/item/ender_pearl.png", "Pearl"},
            {"textures/item/golden_apple.png", "Gapple"},
            {"textures/item/netherite_helmet.png", "Helmet"},
            {"textures/item/netherite_chestplate.png", "Chest"},
            {"textures/item/diamond_chestplate.png", "Chest"},
            {"textures/item/netherite_boots.png", "Boots"},
            {"textures/item/mace.png", "Mace"},
            {"textures/item/experience_bottle.png", "XP"},
            {"textures/block/obsidian.png", "Obsidian"},
            {"textures/block/respawn_anchor_side4.png", "Anchor"},
            {"textures/block/glowstone.png", "Glowstone"},
            {"textures/block/cobweb.png", "Web"},
    };
    private static final int MAX = 12;

    private record Tex(Identifier id, String label, int w, int h) {}

    private static final Map<String, List<Tex>> CACHE = new HashMap<>();
    private static ResourcePackProfile hovered;
    private static int hx;
    private static int hy;
    private static long hoveredAt;

    private PackPreview() {}

    public static void hover(ResourcePackProfile profile, int mouseX, int mouseY) {
        hovered = profile;
        hx = mouseX;
        hy = mouseY;
        hoveredAt = System.currentTimeMillis();
    }

    private static List<Tex> load(ResourcePackProfile profile) {
        return CACHE.computeIfAbsent(profile.getId(), id -> {
            List<Tex> out = new ArrayList<>();
            java.util.Set<String> labels = new java.util.HashSet<>();
            try (ResourcePack pack = profile.createResourcePack()) {
                int n = 0;
                for (String[] slot : SLOTS) {
                    if (out.size() >= MAX || labels.contains(slot[1])) {
                        continue;
                    }
                    InputSupplier<InputStream> in = pack.open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(slot[0]));
                    if (in == null) {
                        continue;
                    }
                    try (InputStream s = in.get()) {
                        NativeImage img = NativeImage.read(s);
                        Identifier tid = Identifier.of("aero", "packpreview/" + Integer.toHexString(id.hashCode()) + "_" + (n++));
                        int w = img.getWidth();
                        int h = img.getHeight();
                        MinecraftClient.getInstance().getTextureManager()
                                .registerTexture(tid, new NativeImageBackedTexture(() -> "aero pack preview", img));
                        out.add(new Tex(tid, slot[1], w, h));
                        labels.add(slot[1]);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
            return out;
        });
    }

    public static void draw(DrawContext ctx, int screenW, int screenH) {
        if (hovered == null || System.currentTimeMillis() - hoveredAt > 100) {
            hovered = null;
            return;
        }
        List<Tex> texs = load(hovered);
        var tr = MinecraftClient.getInstance().textRenderer;
        int cols = 4;
        int cell = 34;
        int rows = Math.max(1, (texs.size() + cols - 1) / cols);
        int w = cols * cell + 16;
        int h = 26 + (texs.isEmpty() ? 16 : rows * (cell + 10)) + 6;
        int x = hx + 14;
        int y = hy - 8;
        if (x + w > screenW - 4) {
            x = hx - 14 - w;
        }
        y = Math.max(4, Math.min(y, screenH - h - 4));
        ctx.createNewRootLayer();
        UiDraw.roundRect(ctx, x, y + 2, w, h, 12, 0x22000000);
        UiDraw.roundRect(ctx, x, y, w, h, 12, 0xF4F7F9FC);
        UiDraw.roundBorder(ctx, x, y, w, h, 12, 0x18000000);
        String title = "Preview · " + hovered.getDisplayName().getString();
        String t = title;
        while (tr.getWidth(t) > w - 16 && t.length() > 4) {
            t = t.substring(0, t.length() - 2);
        }
        ctx.drawText(tr, Text.literal(t.equals(title) ? t : t + "…"), x + 8, y + 8, UiDraw.TEXT, false);
        if (texs.isEmpty()) {
            ctx.drawText(tr, Text.literal("No item textures in this pack"), x + 8, y + 24, UiDraw.MUTED, false);
            return;
        }
        for (int i = 0; i < texs.size(); i++) {
            Tex tex = texs.get(i);
            int cx = x + 8 + (i % cols) * cell;
            int cy = y + 24 + (i / cols) * (cell + 10);
            UiDraw.roundRect(ctx, cx, cy, cell - 4, cell - 4, 6, 0x0C000000);
            int side = Math.min(tex.w(), tex.h()); // animated strips: first frame only
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex.id(), cx + 3, cy + 3, 0f, 0f, cell - 10, cell - 10,
                    side, side, tex.w(), tex.h());
            String l = tex.label();
            ctx.drawText(tr, Text.literal(l), cx + (cell - 4 - tr.getWidth(l)) / 2, cy + cell - 3, UiDraw.MUTED, false);
        }
    }
}
