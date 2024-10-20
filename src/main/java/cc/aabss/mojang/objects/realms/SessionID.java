package cc.aabss.mojang.objects.realms;

public record SessionID(String accessToken, String uuid) {

    @Override
    public String toString() {
        return "token:"+accessToken+":"+uuid;
    }
}
