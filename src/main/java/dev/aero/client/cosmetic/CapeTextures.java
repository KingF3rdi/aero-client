package dev.aero.client.cosmetic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Real Minecraft cape textures (64x32 PNGs, design in the top-left 10x16). Mojang's cape images are
 * not shipped with the game, so they're picked up from <gameDir>/aero-capes/<id>.png (or from a
 * resource pack / this mod's assets/aero/textures/cape/<id>.png). Capes without a file fall back to a
 * plain colored cape.
 */
public final class CapeTextures {
    /** A loaded cape texture and its pixel size (legacy capes are 22x17, newer ones 64x32). */
    public record Tex(Identifier id, int w, int h) {}

    private static final Map<String, Tex> CACHE = new HashMap<>();

    private CapeTextures() {}

    public static Path folder() {
        return FabricLoader.getInstance().getGameDir().resolve("aero-capes");
    }

    /** The texture for a cape id, or null when no file for it exists. */
    public static Tex get(String capeId) {
        if (CACHE.containsKey(capeId)) {
            return CACHE.get(capeId);
        }
        Tex found = load(capeId);
        CACHE.put(capeId, found);
        return found;
    }

    private static Tex load(String capeId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier packed = Identifier.of("aero", "textures/cape/" + capeId + ".png");
        try {
            var res = mc.getResourceManager().getResource(packed);
            if (res.isPresent()) {
                try (InputStream in = res.get().getInputStream()) {
                    NativeImage img = NativeImage.read(in);
                    Tex tex = new Tex(packed, img.getWidth(), img.getHeight());
                    img.close();
                    return tex;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Path file = folder().resolve(capeId + ".png");
            if (Files.isRegularFile(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    NativeImage image = NativeImage.read(in);
                    Identifier id = Identifier.of("aero", "dyncape/" + capeId);
                    mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "aero cape " + capeId, image));
                    return new Tex(id, image.getWidth(), image.getHeight());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
