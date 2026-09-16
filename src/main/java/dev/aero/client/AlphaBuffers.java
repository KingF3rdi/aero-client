package dev.aero.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;

import java.lang.reflect.Proxy;

/**
 * Multiplies vertex alpha (Transparent Players / Custom End Crystals) and blends vertex RGB toward
 * a tint color (Damage Tint) by wrapping every buffer an entity's render() call asks for - this
 * covers its whole model in one place (body, armor, held item) with no per-layer bookkeeping needed.
 * 1.21.11 dropped {@code RenderSystem.setShaderColor}, so this wraps the buffer instead.
 */
public final class AlphaBuffers {
    private AlphaBuffers() {}

    public static VertexConsumerProvider wrap(Entity entity, VertexConsumerProvider inner) {
        if (inner == null) {
            return inner;
        }
        float alpha = Math.min(Math.min(Visuals.transparentPlayerAlpha(entity, false), Visuals.crystalAlpha(entity)),
                Visuals.deathFadeAlpha(entity));
        float tintStrength = Visuals.damageTintStrength(entity);
        if (alpha >= 0.999f && tintStrength <= 0f) {
            return inner;
        }
        if (alpha <= 0.01f) {
            return layer -> inner.getBuffer(layer);
        }
        int tintRgb = tintStrength > 0f ? Visuals.damageTintRgb() : 0;
        try {
            return layer -> tint(inner.getBuffer(layer), alpha, tintStrength, tintRgb);
        } catch (Throwable ignored) {
            return inner;
        }
    }

    private static VertexConsumer tint(VertexConsumer delegate, float alpha, float tintStrength, int tintRgb) {
        if (delegate == null) {
            return null;
        }
        Class<?> type = VertexConsumer.class;
        if (!type.isInterface()) {
            return delegate;
        }
        int tr = (tintRgb >> 16) & 0xFF;
        int tg = (tintRgb >> 8) & 0xFF;
        int tb = tintRgb & 0xFF;
        try {
            return (VertexConsumer) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("color".equals(name) && args != null && args.length >= 4) {
                            if (tintStrength > 0f && args[0] instanceof Integer r && args[1] instanceof Integer g
                                    && args[2] instanceof Integer b) {
                                args[0] = Math.round(r + (tr - r) * tintStrength);
                                args[1] = Math.round(g + (tg - g) * tintStrength);
                                args[2] = Math.round(b + (tb - b) * tintStrength);
                            }
                            Object a = args[args.length - 1];
                            if (a instanceof Integer i) {
                                args[args.length - 1] = Math.max(0, Math.min(255, Math.round(i * alpha)));
                            } else if (a instanceof Float f) {
                                args[args.length - 1] = Math.max(0f, Math.min(1f, f * alpha));
                            }
                        }
                        Object result = method.invoke(delegate, args);
                        return result == delegate ? proxy : result;
                    });
        } catch (Throwable ignored) {
            return delegate;
        }
    }
}
