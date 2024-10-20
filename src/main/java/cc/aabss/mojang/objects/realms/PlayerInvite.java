package cc.aabss.mojang.objects.realms;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record PlayerInvite(String name, String uuid, @Nullable Boolean operator, @Nullable Boolean accepted, @Nullable Boolean online) {
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("uuid", uuid);
        jsonObject.addProperty("operator", operator);
        jsonObject.addProperty("accepted", accepted);
        jsonObject.addProperty("online", online);
        return jsonObject;
    }
}
