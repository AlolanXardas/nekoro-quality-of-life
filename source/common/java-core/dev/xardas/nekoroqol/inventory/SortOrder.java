package dev.xardas.nekoroqol.inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/**
 * The order items are sorted in. "category" puts items of one kind next to each other no matter which mod adds
 * them: all pickaxes together (best first), then axes, and so on. Kinds come from item classes and the common
 * c: tags that mods fill in, so modded tools land next to vanilla ones.
 */
public final class SortOrder {
    public enum Mode {
        CATEGORY, NAME, MOD;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Mode previous() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Mode byId(String id) {
            for (Mode mode : values()) if (mode.id().equals(id)) return mode;
            return CATEGORY;
        }
    }

    public enum Category {
        WEAPONS, TOOLS, ARMOR, EQUIPMENT, POKEMON, FOOD, MATERIALS, BLOCKS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Category byId(String id) {
            for (Category c : values()) if (c.id().equals(id)) return c;
            return null;
        }
    }

    /** Where an item goes: its category's place in the list, a group inside it, and how good it is (higher first). */
    private record Place(int category, int group, double quality) { }

    private static final Place UNSORTED = new Place(Integer.MAX_VALUE, 0, 0);

    // the common c: tags most mods fill in (the same names on NeoForge and Fabric)
    private static final TagKey<Item> C_ARMORS = common("armors");
    private static final TagKey<Item> C_COBBLESTONES = common("cobblestones");
    private static final TagKey<Item> C_DUSTS = common("dusts");
    private static final TagKey<Item> C_FOODS = common("foods");
    private static final TagKey<Item> C_GEMS = common("gems");
    private static final TagKey<Item> C_INGOTS = common("ingots");
    private static final TagKey<Item> C_MELEE_WEAPON_TOOLS = common("tools/melee_weapon");
    private static final TagKey<Item> C_NUGGETS = common("nuggets");
    private static final TagKey<Item> C_ORES = common("ores");
    private static final TagKey<Item> C_POTIONS = common("potions");
    private static final TagKey<Item> C_RANGED_WEAPON_TOOLS = common("tools/ranged_weapon");
    private static final TagKey<Item> C_RAW_MATERIALS = common("raw_materials");
    private static final TagKey<Item> C_RODS = common("rods");
    private static final TagKey<Item> C_STONES = common("stones");
    private static final TagKey<Item> C_STORAGE_BLOCKS = common("storage_blocks");
    private static final TagKey<Item> C_TOOLS = common("tools");
    private static final TagKey<Item> C_TOOLS_BOW = common("tools/bow");
    private static final TagKey<Item> C_TOOLS_BRUSH = common("tools/brush");
    private static final TagKey<Item> C_TOOLS_CROSSBOW = common("tools/crossbow");
    private static final TagKey<Item> C_TOOLS_FISHING_ROD = common("tools/fishing_rod");
    private static final TagKey<Item> C_TOOLS_IGNITER = common("tools/igniter");
    private static final TagKey<Item> C_TOOLS_MACE = common("tools/mace");
    private static final TagKey<Item> C_TOOLS_SHEAR = common("tools/shear");
    private static final TagKey<Item> C_TOOLS_SHIELD = common("tools/shield");
    private static final TagKey<Item> C_TOOLS_SPEAR = common("tools/spear");
    private static final TagKey<Item> C_TOOLS_WRENCH = common("tools/wrench");

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private SortOrder() { }

    public static Comparator<ItemStack> comparator(Mode mode, List<Category> categories) {
        Comparator<ItemStack> byName = Comparator.comparing(SortOrder::name)
                .thenComparing(stack -> id(stack).toString())
                .thenComparingInt(ItemStack::getDamageValue)
                .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
        return switch (mode) {
            case NAME -> byName;
            case MOD -> Comparator.<ItemStack, String>comparing(stack -> id(stack).getNamespace()).thenComparing(byName);
            case CATEGORY -> {
                Comparator<ItemStack> byPlace = Comparator.comparing(stack -> place(stack, categories),
                        Comparator.comparingInt(Place::category).thenComparingInt(Place::group)
                                .thenComparing(Comparator.comparingDouble(Place::quality).reversed()));
                yield byPlace.thenComparing(byName);
            }
        };
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static Place place(ItemStack stack, List<Category> categories) {
        for (int i = 0; i < categories.size(); i++) {
            Place place = test(categories.get(i), i, stack);
            if (place != null) return place;
        }
        return UNSORTED;
    }

    private static Place test(Category category, int index, ItemStack stack) {
        int group = switch (category) {
            case WEAPONS -> weapon(stack);
            case TOOLS -> tool(stack);
            case ARMOR -> armor(stack);
            case EQUIPMENT -> equipment(stack);
            case POKEMON -> pokemon(stack);
            case FOOD -> food(stack);
            case MATERIALS -> material(stack);
            case BLOCKS -> block(stack);
        };
        return group < 0 ? null : new Place(index, group, quality(stack));
    }

    private static boolean miningTool(ItemStack s) {
        return s.is(ItemTags.PICKAXES) || s.is(ItemTags.AXES) || s.is(ItemTags.SHOVELS) || s.is(ItemTags.HOES);
    }

    private static int weapon(ItemStack s) {
        Item item = s.getItem();
        if (miningTool(s)) return -1;
        if (s.is(ItemTags.SWORDS)) return 0;
        if (item instanceof MaceItem || item instanceof TridentItem || s.is(C_TOOLS_MACE) || s.is(C_TOOLS_SPEAR)) return 1;
        if (item instanceof BowItem || s.is(C_TOOLS_BOW)) return 2;
        if (item instanceof CrossbowItem || s.is(C_TOOLS_CROSSBOW)) return 3;
        if (s.is(C_MELEE_WEAPON_TOOLS) || s.is(C_RANGED_WEAPON_TOOLS)) return 4;
        if (s.is(ItemTags.ARROWS)) return 5;
        return -1;
    }

    private static int tool(ItemStack s) {
        Item item = s.getItem();
        if (s.is(ItemTags.PICKAXES)) return 0;
        if (s.is(ItemTags.AXES)) return 1;
        if (s.is(ItemTags.SHOVELS)) return 2;
        if (s.is(ItemTags.HOES)) return 3;
        if (s.is(C_TOOLS_SHEAR)) return 4;
        if (item instanceof FishingRodItem || s.is(C_TOOLS_FISHING_ROD)) return 5;
        if (s.is(C_TOOLS_BRUSH) || s.is(C_TOOLS_IGNITER) || s.is(C_TOOLS_WRENCH)) return 6;
        boolean weaponOrShield = s.is(C_TOOLS_SHIELD) || s.is(C_MELEE_WEAPON_TOOLS)
                || s.is(C_RANGED_WEAPON_TOOLS) || s.is(ItemTags.SWORDS);
        if (!weaponOrShield && (s.is(C_TOOLS) || item instanceof TieredItem)) return 7;
        if (item instanceof BucketItem || item instanceof SolidBucketItem || item instanceof MobBucketItem || item == Items.BUCKET) return 8;
        if (s.is(ItemTags.COMPASSES) || item == Items.CLOCK || item == Items.SPYGLASS || item == Items.LEAD || item == Items.NAME_TAG) return 9;
        return -1;
    }

    private static int armor(ItemStack s) {
        if (s.getItem() instanceof ArmorItem armor) return armor.getType().ordinal();
        if (s.is(ItemTags.HEAD_ARMOR)) return 0;
        if (s.is(ItemTags.CHEST_ARMOR)) return 1;
        if (s.is(ItemTags.LEG_ARMOR)) return 2;
        if (s.is(ItemTags.FOOT_ARMOR)) return 3;
        if (s.is(C_ARMORS)) return 5;
        return -1;
    }

    private static int equipment(ItemStack s) {
        Item item = s.getItem();
        if (item instanceof ShieldItem || s.is(C_TOOLS_SHIELD)) return 0;
        if (item instanceof ElytraItem) return 1;
        if (item == Items.TOTEM_OF_UNDYING) return 2;
        return -1;
    }

    // Cobblemon's own tags; without Cobblemon they are empty and this category catches nothing.
    private static final List<TagKey<Item>> POKEMON_GROUPS = List.of(
            cobblemon("poke_balls"), cobblemon("poke_rods"), cobblemon("held/is_held_item"),
            cobblemon("evolution_items"), cobblemon("remedies"), cobblemon("potions"), cobblemon("restores"),
            cobblemon("revives"), cobblemon("ethers"), cobblemon("candies"), cobblemon("vitamins"),
            cobblemon("mints"), cobblemon("ability_changers"), cobblemon("berries"), cobblemon("apricorns"));

    private static TagKey<Item> cobblemon(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("cobblemon", path));
    }

    private static int pokemon(ItemStack s) {
        for (int i = 0; i < POKEMON_GROUPS.size(); i++) if (s.is(POKEMON_GROUPS.get(i))) return i;
        if ("cobblemon".equals(id(s).getNamespace()) && !(s.getItem() instanceof BlockItem)) return POKEMON_GROUPS.size();
        return -1;
    }

    private static int food(ItemStack s) {
        if (s.is(ItemTags.ARROWS)) return -1;
        if (s.has(DataComponents.FOOD) || s.is(C_FOODS)) return 0;
        if (s.getItem() instanceof PotionItem || s.is(C_POTIONS)) return 1;
        return -1;
    }

    private static int material(ItemStack s) {
        if (s.getItem() instanceof BlockItem) return -1;
        if (s.is(C_INGOTS)) return 0;
        if (s.is(C_GEMS)) return 1;
        if (s.is(C_RAW_MATERIALS)) return 2;
        if (s.is(C_DUSTS)) return 3;
        if (s.is(C_NUGGETS)) return 4;
        if (s.is(C_RODS)) return 5;
        return -1;
    }

    private static int block(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem)) return -1;
        if (s.is(C_ORES)) return 0;
        if (s.is(C_STORAGE_BLOCKS)) return 1;
        if (s.is(ItemTags.LOGS)) return 2;
        if (s.is(ItemTags.PLANKS)) return 3;
        if (s.is(C_STONES) || s.is(C_COBBLESTONES)) return 4;
        return 5;
    }

    /** Better items first: tool tier durability, armor protection, food value, otherwise max durability. */
    private static double quality(ItemStack s) {
        Item item = s.getItem();
        if (item instanceof ArmorItem armor) return armor.getDefense() * 100 + armor.getToughness();
        if (item instanceof TieredItem tiered) return tiered.getTier().getUses();
        FoodProperties food = s.get(DataComponents.FOOD);
        if (food != null) return food.nutrition() + food.saturation();
        return s.getMaxDamage();
    }
}
