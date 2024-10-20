package cc.aabss.mojang.objects.mojang;

public record Privileges(boolean onlineChat, boolean multiplayerServer, boolean multiplayerRealms, boolean telemetry) {
    public enum Privilege {
        ONLINE_CHAT("onlineChat"),
        MULTIPLAYER_SERVER("multiplayerServer"),
        MULTIPLAYER_REALMS("multiplayerRealms"),
        TELEMETRY("telemetry");

        Privilege(String name) {
            this.name = name;
        }

        public final String name;
    }
}
