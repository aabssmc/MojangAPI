package cc.aabss.mojang.objects.mojang;

import cc.aabss.mojang.MojangAPI;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Base64;
import java.util.List;

public record Value(String rawValue, long timestamp, String profileId, String profileName, boolean signatureRequired, List<Texture> textures) {
    /**
     * Converts a base64 value string into a java value object.
     * @param value The raw value.
     * @return A value object.
     */
    public static Value fromBase64(String value) {
        String string = new String(Base64.getDecoder().decode(value));
        JsonObject jsonObject = JsonParser.parseString(string).getAsJsonObject();
        jsonObject.addProperty("rawValue", value);
        return MojangAPI.gson.fromJson(jsonObject, Value.class);
    }
}
