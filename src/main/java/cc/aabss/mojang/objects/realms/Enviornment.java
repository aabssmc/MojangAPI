package cc.aabss.mojang.objects.realms;

public enum Enviornment {
    PRODUCTION("https://pc.realms.minecraft.net"),
    STAGE("https://pc-stage.realms.minecraft.net"),
    LOCAL("http://localhost:8080");

    public final String url;

    Enviornment(String url) {
        this.url = url;
    }
}
