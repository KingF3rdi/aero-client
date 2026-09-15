package dev.aero.client.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/** 1.21.9+ input wrappers. Loaded only when Click/KeyInput exist. */
public class ClickGuiModern extends ClickGuiScreenLegacy {
    public ClickGuiModern(Screen parent, boolean pauseGame) {
        super(parent, pauseGame);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return handleClick(click.x(), click.y(), click.button());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return handleKey(input.key());
    }

    @Override
    public boolean charTyped(CharInput input) {
        return handleChar(input.codepoint());
    }
}
