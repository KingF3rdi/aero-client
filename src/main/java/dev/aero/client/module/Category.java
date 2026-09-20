package dev.aero.client.module;

public enum Category {
    PVP("PvP"),
    HUD("HUD"),
    RENDER("Visuals"),
    PLAYER("Player"),
    MISC("Misc"),
    PERFORMANCE("Performance"),
    PREVIEW("Preview");

    public final String title;

    Category(String title) {
        this.title = title;
    }

    public boolean inSidebar() {
        return true;
    }
}
