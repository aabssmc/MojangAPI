package cc.aabss.mojang;

import cc.aabss.mojang.objects.Pair;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static cc.aabss.mojang.MojangAPI.httpClient;

public class Authenticator {
    /**
     * Gets a Minecraft access token by a Microsoft OAuth2 flow.
     * <a href="https://wiki.vg/Microsoft_Authentication_Scheme">GUIDE</a>
     * @param accessToken The Microsoft OAuth2 flow.
     * @return The Minecraft access token,
     */
    public static String authenticate(String accessToken) {
        try {
            Pair<String, String> pair = xboxLive(accessToken);
            String xbl = pair.getLeft();
            String userHash = pair.getRight();
            String xsts = xsts(xbl);
            return minecraft(userHash, xsts);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pair<String, String> xboxLive(String accessToken) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                                 {
                                    "Properties": {
                                        "AuthMethod": "RPS",
                                        "SiteName": "user.auth.xboxlive.com",
                                        "RpsTicket": "d=<access token>"
                                    },
                                    "RelyingParty": "http://auth.xboxlive.com",
                                    "TokenType": "JWT"
                                 }
                                """.replaceAll("<access token>", accessToken)))
                .build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return Pair.of(json.get("token").getAsString(), json
                .getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString()
        );
    }

    private static String xsts(String xblToken) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": [
                            "<xbl_token>"
                        ]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com/",
                    "TokenType": "JWT"
                 }""".replaceAll("<xbl_token>", xblToken)))
                .build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("Token").getAsString();
    }

    private static String minecraft(String userHash, String xstsToken) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"identityToken\": \"XBL3.0 x=<userhash>;<xsts_token>\"}"
                                .replaceAll("<userhash>", userHash)
                                .replaceAll("<xsts_token>", xstsToken))
                )
                .build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("access_token").getAsString();
    }
}