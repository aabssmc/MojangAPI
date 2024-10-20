package cc.aabss.mojang.objects.mojang;

import java.util.List;

public record PublicPlayerProfile(String id, String name, List<Property> properties, List<ProfileAction> profileActions, boolean legacy) {}
