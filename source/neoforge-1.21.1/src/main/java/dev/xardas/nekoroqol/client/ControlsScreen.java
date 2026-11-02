package dev.xardas.nekoroqol.client;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.mixin.client.KeyBindsScreenAccessor;

/**
 * The Key Binds screen with a search box (by name, or by pressing the key itself, which shows only what
 * is really on that key), a filter (bound, unbound, conflicts, changed) and a category switch.
 * Everything else, rebinding included, is the vanilla screen.
 */
public final class ControlsScreen extends KeyBindsScreen {
    private enum Show { ALL, BOUND, UNBOUND, CONFLICTS, CHANGED }

    private FilteredKeyBindsList keyList;
    private EditBox search;
    private Button keyButton;
    private Button modeButton;
    private Button showButton;
    private Button categoryButton;
    private List<String> categories = List.of();

    private boolean byKey;
    private boolean capturing;
    private InputConstants.Key filterKey;
    private Show show = Show.ALL;
    /** 0 = all categories, otherwise index + 1 in {@link #categories}. */
    private int category;

    public ControlsScreen(Screen parent, Options options) {
        super(parent, options);
    }

    static void onScreenOpening(ScreenEvent.Opening event) {
        if (!QolConfig.get().betterControls || event.getNewScreen() == null) return;
        if (event.getNewScreen().getClass() != KeyBindsScreen.class) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        event.setNewScreen(new ControlsScreen(event.getCurrentScreen(), mc.options));
    }

    @Override
    protected void addTitle() {
        layout.setHeaderHeight(56);
        LinearLayout column = LinearLayout.vertical().spacing(6);
        column.defaultCellSetting().alignHorizontallyCenter();
        column.addChild(new StringWidget(title, font));

        LinearLayout row = LinearLayout.horizontal().spacing(4);
        search = new EditBox(font, 0, 0, 120, 20, Component.translatable("controls.nekoro_qol.search"));
        search.setHint(Component.translatable("controls.nekoro_qol.search"));
        search.setResponder(text -> refilter());
        row.addChild(search);
        modeButton = row.addChild(Button.builder(Component.empty(), b -> {
            byKey = !byKey;
            capturing = false;
            filterKey = null;
            updateButtons();
            refilter();
        }).width(70).tooltip(Tooltip.create(Component.translatable("controls.nekoro_qol.mode.tooltip"))).build());
        showButton = row.addChild(Button.builder(Component.empty(), b -> {
            show = Show.values()[(show.ordinal() + 1) % Show.values().length];
            updateButtons();
            refilter();
        }).width(100).build());
        categoryButton = row.addChild(new ScrollButton(130, this::changeCategory));
        column.addChild(row);
        layout.addToHeader(column);

        // sits where the search box is while searching by key
        keyButton = Button.builder(Component.empty(), b -> {
            capturing = true;
            updateButtons();
        }).width(120).build();
    }

    @Override
    protected void addContents() {
        keyList = layout.addToContents(new FilteredKeyBindsList(this, minecraft));
        ((KeyBindsScreenAccessor) (Object) this).nekoroQol$setKeyBindsList(keyList);
        categories = keyList.categories();
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(keyButton);
        updateButtons();
        refilter();
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (keyButton != null && search != null) keyButton.setPosition(search.getX(), search.getY());
    }

    private void changeCategory(int step) {
        category = Math.floorMod(category + step, categories.size() + 1);
        updateButtons();
        refilter();
    }

    private void updateButtons() {
        search.visible = !byKey;
        keyButton.visible = byKey;
        keyButton.setMessage(capturing ? Component.translatable("controls.nekoro_qol.waiting")
                : filterKey == null ? Component.translatable("controls.nekoro_qol.press_key")
                : Component.translatable("controls.nekoro_qol.key", filterKey.getDisplayName()));
        modeButton.setMessage(Component.translatable(byKey ? "controls.nekoro_qol.by_key" : "controls.nekoro_qol.by_name"));
        showButton.setMessage(Component.translatable("controls.nekoro_qol.show." + show.name().toLowerCase(Locale.ROOT)));
        categoryButton.setMessage(category == 0 ? Component.translatable("controls.nekoro_qol.all_categories")
                : Component.translatable(categories.get(category - 1)));
    }

    private void refilter() {
        if (keyList == null || search == null) return;
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        String onlyCategory = category == 0 ? null : categories.get(category - 1);
        keyList.filter(key -> shown(key) && (byKey ? filterKey == null || key.getKey().equals(filterKey)
                : query.isEmpty() || matchesText(key, query)), onlyCategory);
    }

    private static boolean matchesText(KeyMapping key, String query) {
        return key.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(query)
                || Component.translatable(key.getCategory()).getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean shown(KeyMapping key) {
        return switch (show) {
            case ALL -> true;
            case BOUND -> !key.isUnbound();
            case UNBOUND -> key.isUnbound();
            case CHANGED -> !key.isDefault();
            case CONFLICTS -> conflicts(key);
        };
    }

    private boolean conflicts(KeyMapping key) {
        if (key.isUnbound()) return false;
        for (KeyMapping other : options.keyMappings) {
            if ((other != key && key.same(other)) || other.hasKeyModifierConflict(key)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            capturing = false;
            filterKey = keyCode == GLFW.GLFW_KEY_ESCAPE ? null : InputConstants.getKey(keyCode, scanCode);
            updateButtons();
            refilter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (capturing) {
            capturing = false;
            filterKey = InputConstants.Type.MOUSE.getOrCreate(button);
            updateButtons();
            refilter();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** A button that steps forward on left click, back on right click, and both ways with the mouse wheel. */
    private static final class ScrollButton extends Button {
        private final java.util.function.IntConsumer step;

        ScrollButton(int width, java.util.function.IntConsumer step) {
            super(0, 0, width, 20, Component.empty(), b -> step.accept(1), DEFAULT_NARRATION);
            this.step = step;
            setTooltip(Tooltip.create(Component.translatable("controls.nekoro_qol.category.tooltip")));
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && active && visible && isMouseOver(mouseX, mouseY)) {
                playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                step.accept(-1);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (scrollY == 0 || !isMouseOver(mouseX, mouseY)) return false;
            step.accept(scrollY > 0 ? -1 : 1);
            return true;
        }
    }
}
