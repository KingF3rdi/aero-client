package dev.aero.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final BooleanSupplier getter;
    public final Consumer<Boolean> setter;
    public final List<Setting> settings = new ArrayList<>();

    /** Generic per-module UI style, keyed by module name so it works for every module without hand-wiring. */
    public static final class ModuleStyle {
        public int toggleKey = -1;
        public String panelStyle = "Glass";
        public boolean shadow = true;
        public int accent = 0xFFC4B5FD;
    }

    public ModuleStyle style() {
        dev.aero.client.config.ClientConfig cfg = dev.aero.client.AeroClient.CONFIG;
        if (cfg == null) {
            return new ModuleStyle();
        }
        if (cfg.moduleStyles == null) {
            cfg.moduleStyles = new java.util.HashMap<>();
        }
        return cfg.moduleStyles.computeIfAbsent(name, k -> new ModuleStyle());
    }

    public Module(String name, String description, Category category, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean enabled() {
        return getter.getAsBoolean();
    }

    public void setEnabled(boolean value) {
        setter.accept(value);
    }

    public void toggle() {
        setEnabled(!enabled());
    }

    public Module setting(String name, BooleanSupplier get, Consumer<Boolean> set) {
        settings.add(Setting.bool(name, get, set));
        return this;
    }

    public Module setting(String name, IntGet get, IntSet set, int min, int max) {
        settings.add(Setting.num(name, get, set, min, max));
        return this;
    }

    public Module settingF(String name, DoubleSupplier get, DoubleConsumer set, double min, double max) {
        settings.add(Setting.num(name, get, set, min, max));
        return this;
    }

    public Module setting(String name, Supplier<String> get, Consumer<String> set, String... choices) {
        settings.add(Setting.choice(name, get, set, choices));
        return this;
    }

    /** Free-text field (no fixed choices) - e.g. a chat message or an entity-name filter. */
    public Module settingText(String name, Supplier<String> get, Consumer<String> set) {
        settings.add(Setting.text(name, get, set));
        return this;
    }

    public Module settingColor(String name, IntGet get, IntSet set) {
        settings.add(Setting.color(name, get, set));
        return this;
    }

    public Module settingAction(String name, String button) {
        settings.add(Setting.action(name, button, null));
        return this;
    }

    public Module settingAction(String name, String button, Runnable action) {
        settings.add(Setting.action(name, button, action));
        return this;
    }

    public Module nestLast(String parent) {
        if (!settings.isEmpty()) {
            settings.get(settings.size() - 1).nestUnder = parent;
        }
        return this;
    }

    public Module expandLast() {
        if (!settings.isEmpty()) {
            settings.get(settings.size() - 1).group = true;
        }
        return this;
    }

    /** Puts the last-added setting under a named tab (e.g. "Totem"/"Pop"/"Particles"). */
    public Module tabLast(String tab) {
        if (!settings.isEmpty()) {
            settings.get(settings.size() - 1).tab = tab;
        }
        return this;
    }

    @FunctionalInterface
    public interface IntGet {
        int get();
    }

    @FunctionalInterface
    public interface IntSet {
        void set(int value);
    }

    public static final class Setting {
        public enum Kind {
            BOOL, INT, FLOAT, CHOICE, TEXT, COLOR, ACTION
        }

        public String nestUnder;
        public boolean group;
        public Runnable action;
        public String actionLabel;
        public String tab;

        public final String name;
        public final Kind kind;
        public final boolean numeric;
        public final BooleanSupplier boolGet;
        public final Consumer<Boolean> boolSet;
        public final IntGet intGet;
        public final IntSet intSet;
        public final DoubleSupplier floatGet;
        public final DoubleConsumer floatSet;
        public final Supplier<String> choiceGet;
        public final Consumer<String> choiceSet;
        public final String[] choices;
        public final int min;
        public final int max;
        public final double fMin;
        public final double fMax;

        private Setting(String name, Kind kind, BooleanSupplier boolGet, Consumer<Boolean> boolSet,
                        IntGet intGet, IntSet intSet, DoubleSupplier floatGet, DoubleConsumer floatSet,
                        Supplier<String> choiceGet, Consumer<String> choiceSet, String[] choices,
                        int min, int max, double fMin, double fMax) {
            this.name = name;
            this.kind = kind;
            this.numeric = kind == Kind.INT || kind == Kind.FLOAT;
            this.boolGet = boolGet;
            this.boolSet = boolSet;
            this.intGet = intGet;
            this.intSet = intSet;
            this.floatGet = floatGet;
            this.floatSet = floatSet;
            this.choiceGet = choiceGet;
            this.choiceSet = choiceSet;
            this.choices = choices;
            this.min = min;
            this.max = max;
            this.fMin = fMin;
            this.fMax = fMax;
        }

        public static Setting bool(String name, BooleanSupplier get, Consumer<Boolean> set) {
            return new Setting(name, Kind.BOOL, get, set, null, null, null, null, null, null, null, 0, 0, 0, 0);
        }

        public static Setting num(String name, IntGet get, IntSet set, int min, int max) {
            return new Setting(name, Kind.INT, null, null, get, set, null, null, null, null, null, min, max, min, max);
        }

        public static Setting num(String name, DoubleSupplier get, DoubleConsumer set, double min, double max) {
            return new Setting(name, Kind.FLOAT, null, null, null, null, get, set, null, null, null, 0, 0, min, max);
        }

        public static Setting choice(String name, Supplier<String> get, Consumer<String> set, String[] choices) {
            return new Setting(name, Kind.CHOICE, null, null, null, null, null, null, get, set, choices, 0, 0, 0, 0);
        }

        public static Setting text(String name, Supplier<String> get, Consumer<String> set) {
            return new Setting(name, Kind.TEXT, null, null, null, null, null, null, get, set, null, 0, 0, 0, 0);
        }

        public static Setting color(String name, IntGet get, IntSet set) {
            return new Setting(name, Kind.COLOR, null, null, get, set, null, null, null, null, null, 0, 0, 0, 0);
        }

        public static Setting action(String name, String label, Runnable run) {
            Setting s = new Setting(name, Kind.ACTION, null, null, null, null, null, null, null, null, null, 0, 0, 0, 0);
            s.actionLabel = label;
            s.action = run;
            s.group = true;
            return s;
        }

        public Setting nested(String parent) {
            this.nestUnder = parent;
            return this;
        }

        public Setting expandable() {
            this.group = true;
            return this;
        }

        public void cycle() {
            if (kind != Kind.CHOICE || choices == null || choices.length == 0) {
                return;
            }
            String cur = choiceGet.get();
            int i = 0;
            for (int n = 0; n < choices.length; n++) {
                if (choices[n].equalsIgnoreCase(cur)) {
                    i = n;
                    break;
                }
            }
            choiceSet.accept(choices[(i + 1) % choices.length]);
        }

        public String label() {
            if (kind == Kind.INT) {
                return name + "  " + intGet.get();
            }
            if (kind == Kind.FLOAT) {
                double v = floatGet.getAsDouble();
                if (Math.abs(v - Math.round(v)) < 0.05) {
                    return name + "  " + Math.round(v);
                }
                return name + "  " + String.format(java.util.Locale.ROOT, "%.2f", v);
            }
            if (kind == Kind.CHOICE) {
                return name + "  " + choiceGet.get();
            }
            return name;
        }
    }
}
