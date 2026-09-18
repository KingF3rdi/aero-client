# Aero Client (Fabric mod)

Client-side PvP/quality-of-life mod for Minecraft: Java Edition 1.21.11 (Fabric). It is the in-game half of
[Aero Client Launcher](https://github.com/KingF3rdi/aero-client-launcher), which keeps it up to date automatically.

## Features

- ClickGUI (Right Shift): modules for HUD, visuals, PvP and performance, with colour pickers and rebindable keys
- Wardrobe: capes (real Mojang cape textures), wings, headwear, pets, trails, kill/mace effects, shield tints
- Totem pop counter next to names, TierTagger with real HT/LT ranks and mode icons, ping next to names
- Client badge for Aero Client users (public list: https://github.com/KingF3rdi/aero-client-users)
- Music player that reads Spotify and other Windows media sessions, potion HUD, saturation overlay, motion blur
- Performance: UI Boost, Max FPS, particle and entity limits

## Build

```bash
./gradlew build
```

The jar is written to `build/libs/`. Releases contain the ready-made `aero-client.jar`, which the launcher downloads on start.

## License

MIT, see [LICENSE](LICENSE). Not affiliated with Mojang or Microsoft. Bundled libraries (Player Animation Library,
Bendable Cuboids Library) are MIT licensed by their authors. Official cape images belong to Mojang.
