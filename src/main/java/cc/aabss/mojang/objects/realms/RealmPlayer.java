package cc.aabss.mojang.objects.realms;

public record RealmPlayer(String name, String uuid, boolean operator, boolean accepted, boolean online, String permission) {}
