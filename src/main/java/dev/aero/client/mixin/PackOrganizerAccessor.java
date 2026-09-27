package dev.aero.client.mixin;

import net.minecraft.resource.ResourcePackProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reaches the profile behind a pack-screen entry (for the hover preview). */
@Mixin(targets = "net.minecraft.client.gui.screen.pack.ResourcePackOrganizer$AbstractPack")
public interface PackOrganizerAccessor {
    @Accessor("profile")
    ResourcePackProfile aero$profile();
}
