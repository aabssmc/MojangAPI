package cc.aabss.mojang.objects.mojang;

import org.jetbrains.annotations.Nullable;

public record Texture(String name, String url, @Nullable Metadata metadata) {
    public record Metadata(Variant model) { }
}
