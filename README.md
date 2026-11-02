# Nekoro Quality of Life

Many small quality of life changes in one mod: switching single enchantments on and off (Fortune and Silk Touch on one tool), inventory sorting and transfer buttons, mouse shortcuts in inventories, refilling the item in your hand, a short F3 screen, a searchable Key Binds screen, borderless fullscreen, a Brightness slider up to full brightness, cleaner tooltips, hiding popups, unlocking all recipes, instant XP pickup, double doors, faster ladders, auto walk and more.

Made mainly for my Nekoro Pokemon modpack, but it works with any mods. It replaces many mods I used there (Toggle Enchantments, parts of Quark, Mouse Tweaks, Inventory Essentials, BetterF3, Controlling, Borderless Window, Better Brightness Slider Respawn, Clean Tooltips, Toast Control, Not Enough Recipe Book, Clumps, NoCreativeDrift). Most of them probably do their job better; this mod keeps exactly what my modpack needs up to date.

Download: [CurseForge](https://www.curseforge.com/minecraft/search?search=nekoro+quality+of+life)

## Versions

| Minecraft | Loaders |
|---|---|
| 1.21.1 | NeoForge |

## Config

`config/Nekoro/QualityOfLife/common.json` changes the game itself (on a server the server's file counts), `client.json` is just for the player's own game. Every feature can be turned off there, and every option is explained in the file. None of the keys has a default key.

The default files are in [config-default](config-default/Nekoro/QualityOfLife), and the setup of my modpack is in [pack-config](pack-config/Nekoro/QualityOfLife).

## Building

```
cd source/neoforge-1.21.1
gradlew build
```

The jar is written to `build/libs`. Java 21 is needed.

## License

MIT, see [LICENSE](LICENSE). Borderless fullscreen switches the window the same way as [Borderless Window](https://www.curseforge.com/minecraft/mc-mods/borderless) by nimueller (MIT).
