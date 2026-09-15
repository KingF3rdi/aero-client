package dev.aero.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;

import java.lang.reflect.Proxy;

/**
 * Multiplies vertex alpha so Transparent Players can actually fade the model.
 * 1.21.11 dropped {@code RenderSystem.setShaderColor}, so this wraps the buffer instead.
 */
public final class AlphaBuffers {
    private AlphaBuffers() {}

    public static VertexConsumerProvider wrap(Entity entity, VertexConsumerProvider inner) {
        if (inner == null) {
            return inner;
        }
        float alpha = Visuals.transparentPlayerAlpha(entity, false);
        if (alpha >= 0.999f) {
            return inner;
        }
        if (alpha <= 0.01f) {
            return layer -> inner.getBuffer(layer);
        }
        try {
            return layer -> tint(inner.getBuffer(layer), alpha);
        } catch (Throwable ignored) {
            return inner;
        }
    }

    private static VertexConsumer tint(VertexConsumer delegate, float alpha) {
        if (delegate == null) {
            return null;
        }
        Class<?> type = VertexConsumer.class;
        if (!type.isInterface()) {
            return delegate;
        }
        try {
            return (VertexConsumer) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("color".equals(name) && args != null && args.length >= 4) {
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
