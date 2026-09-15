package dev.aero.client;

import dev.aero.client.config.ClientConfig;
import dev.aero.client.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Applies module flags to live Minecraft options and world state.
 * Works even when a mixin target was renamed.
 */
public final class McBind {
    private static Double gammaBackup;
    private static Object cloudsBackup;
    private static Object particlesBackup;
    private static Double entityBackup;
    private static Double distortionBackup;
    private static Double fovFxBackup;
    private static Boolean bobBackup;
    private static Double fovBackup;

    private McBind() {}

    public static void apply(MinecraftClient mc) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null || mc == null || mc.options == null) {
            return;
        }
        GameOptions opt = mc.options;
        try {
            bindGamma(opt, c.fullbright);
            bindEnum(opt, c.cloudsOff, "getCloudRenderMode", "OFF", true);
            bindEnum(opt, c.particleLimiter, "getParticles", "MINIMAL", true);
            bindDouble(opt, c.entityDistance, "getEntityDistanceScaling",
                    Math.max(0.5, Math.min(5.0, c.entityRange / 64.0)), true);
            bindDouble(opt, c.guiTweaks, "getDistortionEffectScale", 0.0, true);
            bindDouble(opt, c.guiTweaks, "getFovEffectScale", 0.0, true);
            boolean bow = c.crossbowTweaks && usingRanged(mc);
            bindBool(opt, bow || c.noHurtcam || c.noBobbing, "getBobView", false, true);
            bindBool(opt, c.noShadows, "getEntityShadows", false, true);
        } catch (Throwable ignored) {
        }

        // Rain/thunder overrides are applied in WorldMixin getters so disabling the module
        // does not leave a mutated client weather value stuck on the world.

        try {
            // Zoom is applied in GameRendererMixin via Initial zoom.
        } catch (Throwable ignored) {
        }

        if ((c.toggleSprint || c.alwaysSprint) && mc.player != null && opt.forwardKey.isPressed() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }

    private static boolean usingRanged(MinecraftClient mc) {
        if (mc.player == null || !mc.player.isUsingItem()) {
            return false;
        }
        String n = mc.player.getActiveItem().getItem().toString().toLowerCase();
        return n.contains("bow") || n.contains("crossbow") || n.contains("trident");
    }

    private static void bindFov(GameOptions opt, boolean on, double value) {
        Object option = option(opt, "getFov");
        if (option == null) {
            return;
        }
        if (on) {
            Double cur = asDouble(getValue(option));
            if (fovBackup == null && cur != null) {
                fovBackup = cur;
            }
            setValue(option, value);
        } else if (fovBackup != null) {
            setValue(option, fovBackup);
            fovBackup = null;
        }
    }

    private static void bindGamma(GameOptions opt, boolean on) {
        Object option = option(opt, "getGamma");
        if (option == null) {
            return;
        }
        Double cur = asDouble(getValue(option));
        if (on) {
            if (gammaBackup == null && cur != null) {
                gammaBackup = cur;
            }
            float bright = AeroClient.CONFIG != null ? AeroClient.CONFIG.brightness : 10f;
            setValue(option, Math.min(16.0, Math.max(1.0, bright)) / 10.0);
        } else if (gammaBackup != null) {
            setValue(option, gammaBackup);
            gammaBackup = null;
        }
    }

    private static void bindEnum(GameOptions opt, boolean on, String getter, String offName, boolean backup) {
        Object option = option(opt, getter);
        if (option == null) {
            return;
        }
        Object cur = getValue(option);
        if (on) {
            if (backup && cloudsOr(getter) && cloudsBackup == null) {
                cloudsBackup = cur;
            }
            if (backup && getter.contains("Particle") && particlesBackup == null) {
                particlesBackup = cur;
            }
            Object off = enumConst(cur, offName);
            if (off != null) {
                setValue(option, off);
            }
        } else if (getter.contains("Cloud") && cloudsBackup != null) {
            setValue(option, cloudsBackup);
            cloudsBackup = null;
        } else if (getter.contains("Particle") && particlesBackup != null) {
            setValue(option, particlesBackup);
            particlesBackup = null;
        }
    }

    private static boolean cloudsOr(String getter) {
        return getter.contains("Cloud");
    }

    private static void bindDouble(GameOptions opt, boolean on, String getter, double value, boolean unused) {
        Object option = option(opt, getter);
        if (option == null) {
            return;
        }
        Double cur = asDouble(getValue(option));
        if (on) {
            if (getter.contains("Entity") && entityBackup == null) {
                entityBackup = cur;
            }
            if (getter.contains("Distortion") && distortionBackup == null) {
                distortionBackup = cur;
            }
            if (getter.contains("Fov") && fovFxBackup == null) {
                fovFxBackup = cur;
            }
            setValue(option, value);
        } else if (getter.contains("Entity") && entityBackup != null) {
            setValue(option, entityBackup);
            entityBackup = null;
        } else if (getter.contains("Distortion") && distortionBackup != null) {
            setValue(option, distortionBackup);
            distortionBackup = null;
        } else if (getter.contains("Fov") && fovFxBackup != null) {
            setValue(option, fovFxBackup);
            fovFxBackup = null;
        }
    }

    private static void bindBool(GameOptions opt, boolean forceOff, String getter, boolean offVal, boolean unused) {
        Object option = option(opt, getter);
        if (option == null) {
            return;
        }
        Object cur = getValue(option);
        if (forceOff) {
            if (bobBackup == null && cur instanceof Boolean b) {
                bobBackup = b;
            }
            setValue(option, offVal);
        } else if (bobBackup != null) {
            setValue(option, bobBackup);
            bobBackup = null;
        }
    }

    private static Object option(GameOptions opt, String getter) {
        try {
            return GameOptions.class.getMethod(getter).invoke(opt);
        } catch (Throwable ignored) {
            try {
                Field f = findField(opt.getClass(), getter.replace("get", ""));
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(opt);
                }
            } catch (Throwable ignored2) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        String n = name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(n) || f.getName().equalsIgnoreCase(name)) {
                    return f;
                }
            }
        }
        return null;
    }

    private static Object getValue(Object option) {
        for (String m : new String[]{"getValue", "get"}) {
            try {
                return option.getClass().getMethod(m).invoke(option);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void setValue(Object option, Object value) {
        for (Method m : option.getClass().getMethods()) {
            if ((m.getName().equals("setValue") || m.getName().equals("set")) && m.getParameterCount() == 1) {
                try {
                    m.invoke(option, value);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Object enumConst(Object sample, String name) {
        Class<?> type = sample == null ? null : sample.getClass();
        if (type == null || !type.isEnum()) {
            return null;
        }
        for (Object e : type.getEnumConstants()) {
            if (((Enum<?>) e).name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    private static Double asDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : null;
    }

    private static int intOption(GameOptions opt, String getter, int fallback) {
        try {
            Object v = getValue(option(opt, getter));
            if (v instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static void invoke(Object target, String name, Object... args) {
        if (target == null) {
            return;
        }
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) {
                continue;
            }
            try {
                m.invoke(target, args);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void invoke(Object target, String name, Class<?> type, Object arg) {
        try {
            target.getClass().getMethod(name, type).invoke(target, arg);
        } catch (Throwable ignored) {
        }
    }
}
