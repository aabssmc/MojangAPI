package cc.aabss.mojang.objects.mojang;

import java.util.Map;

public record BannedScopes(Map<String, Ban> bannedMap) {}
