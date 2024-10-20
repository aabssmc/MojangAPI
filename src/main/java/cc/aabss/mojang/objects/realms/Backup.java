package cc.aabss.mojang.objects.realms;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public record Backup(String backupId, long lastModifiedDate, long size, Metadata metdata) {
    public record Metadata(@SerializedName("game_difficulty") String gameDifficulty,
                           String name,
                           @SerializedName("game_server_version") String gameServerVersion,
                           @SerializedName("enabled_packs") JsonObject enabledPacks,
                           String description,
                           @SerializedName("game_mode") String gameMode,
                           @SerializedName("world_type") WorldType worldType
    ) {}
}
