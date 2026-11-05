# Nekoro Quality of Life

Many small quality of life changes in one mod: switching single enchantments on and off (Fortune and Silk Touch on one tool), inventory sorting and transfer buttons, mouse shortcuts in inventories, refilling the item in your hand, a short F3 screen, a searchable Key Binds screen, borderless fullscreen, a Brightness slider up to full brightness, cleaner tooltips, hiding popups, unlocking all recipes, instant XP pickup, double doors, faster ladders, auto walk and more.

Made for my Nekoro Pokemon modpack, but it works with any mods. It replaces many mods I used there (Toggle Enchantments, parts of Quark, Mouse Tweaks, Inventory Essentials, BetterF3, Controlling, Borderless Window, Better Brightness Slider Respawn, Clean Tooltips, Toast Control, Not Enough Recipe Book, Clumps, NoCreativeDrift). Many of those mods may be better than mine.

Download: [CurseForge](https://www.curseforge.com/minecraft/search?search=nekoro+quality+of+life)

## Versions

| Minecraft | Loaders |
|---|---|
| 1.21.1 | NeoForge, Fabric |

Fabric needs Fabric API.

## Config

`config/Nekoro/QualityOfLife/common.json` changes the game itself (on a server the server's file counts), `client.json` is just for the player's own game. Every feature can be turned off there, and every option is explained in the file. None of the keys has a default key.

The default files are in [config-default](config-default/Nekoro/QualityOfLife), and the setup of my modpack is in [pack-config](pack-config/Nekoro/QualityOfLife).

## Building

The shared code is in `source/common` (`java-core`, plus `java-neoforge` and `java-fabric` for what differs between the loaders), and each loader has a small Gradle project next to it.

```
cd source
.uild-all.ps1
```

The jars are copied to `Gotowe/<version>`. Java 21 is needed.

## License

MIT, see [LICENSE](LICENSE).
