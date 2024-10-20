package cc.aabss.mojang.objects.realms;

public record RealmsCookie(SessionID sid, String user, String version) {
    @Override
    public String toString() {
        return "sid="+sid+";user="+user+";version="+version;
    }
}
