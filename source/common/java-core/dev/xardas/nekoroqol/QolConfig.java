package dev.xardas.nekoroqol;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import dev.xardas.nekoroqol.inventory.SortOrder;

/**
 * Settings from config/Nekoro/QualityOfLife/common.json (game rules, works on the server) and client.json
 * (only the player's own game). Each load builds a new snapshot.
 */
public final class QolConfig {
    private static volatile QolConfig current;

    // common.json
    public final boolean toggleEnchantments;
    public final boolean unlockAllRecipes;
    public final boolean doubleDoors;
    public final boolean instantXp;
    public final boolean mergeXpOrbs;
    public final boolean refillHand;
    public final boolean inventoryButtons;
    public final List<SortOrder.Category> sortCategories;

    // client.json
    public final boolean hideTutorials;
    public final boolean hideAdvancements;
    public final boolean hideRecipeToasts;
    public final List<String> hiddenToastClasses;
    public final boolean hiddenToastsToChat;
    public final boolean hideRecipeBook;
    public final boolean noCreativeDrift;
    public final boolean fasterLadders;
    public final double ladderSpeed;
    public final int brightnessMax;
    public final boolean shortDebugScreen;
    public final boolean borderlessFullscreen;
    public final boolean betterControls;
    public final boolean sortButton;
    public final boolean transferButtons;
    public final List<String> noButtonsIn;
    public final List<String> noSortIn;
    public final boolean scrollMovesItems;
    public final boolean shiftDragMoves;
    public final boolean ctrlClickMovesSame;
    public final boolean spaceClickMovesAll;
    public final boolean tooltipEnchantments;
    public final String curseColor;
    public final String enchantmentColor;
    public final String maxLevelColor;
    public final boolean tooltipDurability;
    public final boolean tooltipDyeColor;

    private QolConfig(JsonObject common, JsonObject client) {
        toggleEnchantments = JsonFile.bool(common, "toggle_enchantments", true);
        unlockAllRecipes = JsonFile.bool(common, "unlock_all_recipes", false);
        doubleDoors = JsonFile.bool(common, "double_doors", true);
        instantXp = JsonFile.bool(common, "instant_xp", true);
        mergeXpOrbs = JsonFile.bool(common, "merge_xp_orbs", true);
        refillHand = JsonFile.bool(common, "refill_hand", true);
        inventoryButtons = JsonFile.bool(common, "inventory_buttons", true);
        sortCategories = categories(JsonFile.strings(common, "sort_categories"));

        JsonObject toasts = JsonFile.obj(client, "toasts");
        hideTutorials = JsonFile.bool(toasts, "hide_tutorials", false);
        hideAdvancements = JsonFile.bool(toasts, "hide_advancements", false);
        hideRecipeToasts = JsonFile.bool(toasts, "hide_recipes", false);
        hiddenToastClasses = JsonFile.strings(toasts, "hide_classes");
        hiddenToastsToChat = JsonFile.bool(toasts, "send_hidden_to_chat", true);
        hideRecipeBook = JsonFile.bool(client, "hide_recipe_book", false);
        noCreativeDrift = JsonFile.bool(client, "no_creative_drift", true);
        fasterLadders = JsonFile.bool(client, "faster_ladders", true);
        ladderSpeed = Math.max(0.15, Math.min(1.0, JsonFile.number(client, "ladder_speed", 0.3)));
        brightnessMax = Math.max(100, Math.min(10000, JsonFile.integer(client, "brightness_max", 1200)));
        shortDebugScreen = JsonFile.bool(client, "short_debug_screen", true);
        borderlessFullscreen = JsonFile.bool(client, "borderless_fullscreen", true);
        betterControls = JsonFile.bool(client, "better_controls", true);

        JsonObject inventory = JsonFile.obj(client, "inventory");
        sortButton = JsonFile.bool(inventory, "sort_button", true);
        transferButtons = JsonFile.bool(inventory, "transfer_buttons", true);
        noButtonsIn = inventory.has("no_buttons_in") ? JsonFile.strings(inventory, "no_buttons_in") : List.of("projecte");
        noSortIn = inventory.has("no_sort_in") ? JsonFile.strings(inventory, "no_sort_in")
                : List.of("sophisticatedbackpacks", "sophisticatedstorage");
        scrollMovesItems = JsonFile.bool(inventory, "scroll_moves_items", true);
        shiftDragMoves = JsonFile.bool(inventory, "shift_drag_moves", true);
        ctrlClickMovesSame = JsonFile.bool(inventory, "ctrl_click_moves_same", true);
        spaceClickMovesAll = JsonFile.bool(inventory, "space_click_moves_all", true);

        JsonObject tooltips = JsonFile.obj(client, "tooltips");
        tooltipEnchantments = JsonFile.bool(tooltips, "enchantments", true);
        curseColor = JsonFile.string(tooltips, "curse_color", "red");
        enchantmentColor = JsonFile.string(tooltips, "enchantment_color", "green");
        maxLevelColor = JsonFile.string(tooltips, "max_level_color", "gold");
        tooltipDurability = JsonFile.bool(tooltips, "durability", true);
        tooltipDyeColor = JsonFile.bool(tooltips, "dye_color", true);
    }

    private static List<SortOrder.Category> categories(List<String> names) {
        if (names.isEmpty()) return List.of(SortOrder.Category.values());
        List<SortOrder.Category> out = new ArrayList<>();
        for (String name : names) {
            SortOrder.Category category = SortOrder.Category.byId(name);
            if (category == null) NekoroQol.LOGGER.warn("Unknown sort category \"{}\" in common.json, skipped", name);
            else if (!out.contains(category)) out.add(category);
        }
        return out;
    }

    public static QolConfig get() {
        if (current == null) load();
        return current;
    }

    public static Path dir() {
        return Platform.INSTANCE.configDir().resolve("Nekoro").resolve("QualityOfLife");
    }

    public static synchronized void load() {
        Path dir = dir();
        current = new QolConfig(JsonFile.read(dir.resolve("common.json"), DEFAULT_COMMON),
                JsonFile.read(dir.resolve("client.json"), DEFAULT_CLIENT));
    }

    private static final String DEFAULT_COMMON = """
            // Nekoro Quality of Life: changes to the game itself. On a server they are set by the server's file.
            // /reload applies changes.
            {
              // switch single enchantments on an item off and on again (keys in Controls, "Nekoro Quality of Life": a menu for the
              // item in your hand, and quick keys for Silk Touch, Frost Walker, Knockback and others that look in both
              // hands and the armor).
              // A switched-off enchantment does nothing until it is switched on again, so Fortune and Silk Touch
              // can be on one tool. Curses can never be switched off.
              "toggle_enchantments": true,
              // every player knows every recipe (the recipe book shows all of them, no "new recipe" toasts needed)
              "unlock_all_recipes": false,
              // opening one door of a double door opens the other one too (sneak to open only one)
              "double_doors": true,
              // XP orbs are picked up at once instead of one by one, with Mending working as usual
              "instant_xp": true,
              // XP orbs lying close together join into one orb with all their experience (fewer entities, less lag)
              "merge_xp_orbs": true,
              // when the item in your hand is used up (a tool breaks, the last block is placed, the last food is
              // eaten, the last Poke Ball is thrown), the same item from your inventory takes its place
              "refill_hand": true,
              // the sort and transfer buttons in inventories and containers (each player can still hide them in client.json)
              "inventory_buttons": true,
              // the order of item kinds when sorting by category. Remove a kind and its items are sorted with the
              // next kind they fit, or at the end. Kinds: weapons, tools, armor, equipment (shields, elytra, totems),
              // pokemon (Cobblemon items, empty without Cobblemon), food (also potions), materials (ingots, gems,
              // raw ores, dusts, nuggets, rods), blocks. Everything else comes last.
              "sort_categories": ["weapons", "tools", "armor", "equipment", "pokemon", "food", "materials", "blocks"]
            }
            """;

    private static final String DEFAULT_CLIENT = """
            // Nekoro Quality of Life: changes only for your own game. Restart the game after editing.
            {
              "toasts": {
                // the tutorial hints at the start (how to move, punch trees, open the inventory...)
                "hide_tutorials": false,
                // "Advancement Made!" popups (advancements still show in chat)
                "hide_advancements": false,
                // "New Recipes Unlocked!" popups
                "hide_recipes": false,
                // other popups by their Java class, e.g. "com.cobblemon.mod.common.client.gui.toast.CobblemonToast"
                "hide_classes": [],
                // show the text of popups from "hide_classes" in chat instead (each text at most once every 5 minutes)
                "send_hidden_to_chat": true
              },
              // hides the green recipe book button; recipes are in JEI or similar mods.
              // Also skips building the recipe book at login, which saves time in big modpacks.
              "hide_recipe_book": false,
              // stop at once when you let go of the movement keys while flying in creative
              "no_creative_drift": true,
              // climb down ladders and vines faster while looking down (sneak to stop as usual)
              "faster_ladders": true,
              // blocks per tick when climbing down faster, vanilla is 0.15
              "ladder_speed": 0.3,
              // the highest value of the Brightness slider in Video Settings, in percent (vanilla: 100).
              // At 1200 everything is lit as if by full light, also in caves at night.
              "brightness_max": 1200,
              // F3 shows only FPS, coordinates, direction, biome, day and time, and light. A key in Controls
              // ("Nekoro Quality of Life") switches to the full F3 screen, coloured so the useful lines stand out.
              "short_debug_screen": true,
              // fullscreen as a borderless window over the whole monitor: alt-tab and a second monitor work
              // without the game minimizing or the screen going black
              "borderless_fullscreen": true,
              // Key Binds screen with a search box (by name, or press a key to see only what is on that key),
              // a filter (bound, unbound, conflicts, changed) and a category switch
              "better_controls": true,
              "inventory": {
                // a sort button above containers and your inventory. Each click sorts in the next order (right click: the previous one):
                // by category (all pickaxes together, best first, then axes...), by name, or by mod.
                // The hotbar is never sorted.
                "sort_button": true,
                // with a container open, buttons above it and above your inventory: move matching items here (only kinds
                // that side already has) and move everything here. Items never come from or go into the hotbar.
                "transfer_buttons": true,
                // screens without these buttons: a mod id, or a screen's Java class
                // (e.g. "net.minecraft.client.gui.screens.inventory.ContainerScreen")
                "no_buttons_in": [ "projecte" ],
                // screens without the sort button (mods that sort their own storage)
                "no_sort_in": [ "sophisticatedbackpacks", "sophisticatedstorage" ],
                // the mouse wheel over an item moves one at a time: down into the other inventory, up back into this slot
                "scroll_moves_items": true,
                // hold Shift and the left button and move over items to move all of them to the other inventory
                "shift_drag_moves": true,
                // Ctrl + click moves every stack of that item to the other inventory
                "ctrl_click_moves_same": true,
                // Space + click moves everything from that inventory (or from that part: the hotbar or the three rows above it)
                "space_click_moves_all": true
              },
              "tooltips": {
                // enchantments listed under an "Enchantments:" line, coloured: curses, normal, and at max level or higher.
                // Colours: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue,
                // green, aqua, red, light_purple, yellow, white, or a hex colour like "#FF8800"
                "enchantments": true,
                "curse_color": "red",
                "enchantment_color": "green",
                "max_level_color": "gold",
                // "Durability: 120 / 250" on every item that can break, coloured from green to red
                "durability": true,
                // dyed items show their colour instead of just "Dyed"
                "dye_color": true
              }
            }
            """;
}
