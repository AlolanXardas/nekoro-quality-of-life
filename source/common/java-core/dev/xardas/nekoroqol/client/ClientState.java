package dev.xardas.nekoroqol.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.xardas.nekoroqol.NekoroQol;
import dev.xardas.nekoroqol.QolConfig;
import dev.xardas.nekoroqol.inventory.SortOrder;

/** Things the game remembers for the player between sessions, in config/Nekoro/QualityOfLife/client_state.json. */
final class ClientState {
    private static SortOrder.Mode sortMode;

    private ClientState() { }

    private static Path file() {
        return QolConfig.dir().resolve("client_state.json");
    }

    static SortOrder.Mode sortMode() {
        if (sortMode == null) {
            sortMode = SortOrder.Mode.CATEGORY;
            try {
                if (Files.exists(file())) {
                    JsonObject json = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8)).getAsJsonObject();
                    if (json.has("sort_mode")) sortMode = SortOrder.Mode.byId(json.get("sort_mode").getAsString());
                }
            } catch (Exception e) {
                NekoroQol.LOGGER.warn("Could not read {}", file(), e);
            }
        }
        return sortMode;
    }

    static void setSortMode(SortOrder.Mode mode) {
        sortMode = mode;
        JsonObject json = new JsonObject();
        json.addProperty("sort_mode", mode.id());
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), json.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            NekoroQol.LOGGER.warn("Could not save {}", file(), e);
        }
    }
}
