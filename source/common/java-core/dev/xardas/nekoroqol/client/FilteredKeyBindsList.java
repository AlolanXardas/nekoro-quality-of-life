package dev.xardas.nekoroqol.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

/**
 * The vanilla key list with a filter. The vanilla rows are kept (so rebinding, reset and conflict marks work
 * as usual); filtering only changes which of them are shown. A category heading shows only above matches.
 */
final class FilteredKeyBindsList extends KeyBindsList {
    /** One vanilla row: a key, or a category heading (key == null). */
    private record Row(Entry entry, KeyMapping key, String category) { }

    private final List<Row> rows = new ArrayList<>();

    FilteredKeyBindsList(KeyBindsScreen screen, Minecraft minecraft) {
        super(screen, minecraft);
        // the rows were built from the sorted mappings with a heading before each category; walk them the same way
        KeyMapping[] sorted = minecraft.options.keyMappings.clone();
        Arrays.sort(sorted);
        int next = 0;
        for (Entry entry : List.copyOf(children())) {
            if (entry instanceof KeyBindsList.CategoryEntry) {
                rows.add(new Row(entry, null, next < sorted.length ? sorted[next].getCategory() : ""));
            } else if (next < sorted.length) {
                rows.add(new Row(entry, sorted[next], sorted[next].getCategory()));
                next++;
            }
        }
    }

    /** Categories in the order they are listed. */
    List<String> categories() {
        List<String> out = new ArrayList<>();
        for (Row row : rows) if (row.key == null) out.add(row.category);
        return out;
    }

    /** Shows the keys that pass, in one category or all of them (category null). */
    void filter(Predicate<KeyMapping> test, String category) {
        clearEntries();
        Entry heading = null;
        for (Row row : rows) {
            boolean inCategory = category == null || category.equals(row.category);
            if (row.key == null) {
                heading = inCategory ? row.entry : null;
                continue;
            }
            if (!inCategory || !test.test(row.key)) continue;
            if (heading != null) {
                addEntry(heading);
                heading = null;
            }
            addEntry(row.entry);
        }
        setScrollAmount(0);
    }
}
