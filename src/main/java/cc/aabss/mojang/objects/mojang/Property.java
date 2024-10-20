package cc.aabss.mojang.objects.mojang;

import org.jetbrains.annotations.Nullable;

public record Property(String name, String value, @Nullable String signature) {}
