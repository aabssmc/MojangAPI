package cc.aabss.mojang.objects.mojang;

import java.util.List;

public record PrivatePlayerProfile(String id, String name, List<Skin> skins, List<Cape> capes) {}
