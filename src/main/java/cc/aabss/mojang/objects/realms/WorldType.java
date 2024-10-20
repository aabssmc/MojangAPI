package cc.aabss.mojang.objects.realms;

import com.google.gson.annotations.SerializedName;

public enum WorldType {
    NORMAL, MINIGAME, @SerializedName("ADVENTUREMAP")ADVENTURE_MAP, EXPERIENCE, INSPIRATION
}
