package dev.aero.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** Screenshot module: saves to the vanilla screenshots folder, optionally also copies to the clipboard. */
public final class Screenshots {
    private Screenshots() {}

    public static void capture(MinecraftClient mc) {
        if (mc == null || mc.getFramebuffer() == null) {
            return;
        }
        ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(),
                text -> {
                    if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
                        mc.inGameHud.getChatHud().addMessage(text);
                    }
                });
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.screenshotAutoCopy) {
            try {
                ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), image -> {
                    try {
                        copyToClipboard(image);
                    } finally {
                        image.close();
                    }
                });
            } catch (Throwable ignored) {
            }
        }
    }

    private static void copyToClipboard(NativeImage image) {
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    buffered.setRGB(x, y, image.getColorArgb(x, y));
                }
            }
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new ImageTransferable(buffered), null);
        } catch (Throwable ignored) {
        }
    }

    private static final class ImageTransferable implements Transferable {
        private final BufferedImage image;

        private ImageTransferable(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
