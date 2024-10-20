package cc.aabss.mojang;

import cc.aabss.mojang.objects.*;
import cc.aabss.mojang.objects.mojang.*;
import cc.aabss.mojang.objects.realms.SessionID;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class MojangAPI {

    final static HttpClient httpClient = HttpClient.newHttpClient();
    public final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Privileges.class, new TypeAdapter<Privileges>() {
                @Override
                public void write(JsonWriter out, Privileges value) throws IOException {
                    out.beginObject()
                            .name("onlineChat")
                                .beginObject()
                                    .name("enabled").value(value.onlineChat())
                                .endObject()

                            .name("multiplayerServer")
                                .beginObject()
                                    .name("enabled").value(value.multiplayerServer())
                                .endObject()

                            .name("multiplayerRealms")
                                .beginObject()
                                    .name("enabled").value(value.multiplayerRealms())
                                .endObject()

                            .name("telemetry")
                                .beginObject()
                                    .name("enabled").value(value.telemetry())
                                .endObject()

                            .endObject()
                            .close();
                }

                @Override
                public Privileges read(JsonReader in) {
                    JsonObject object = JsonParser.parseReader(in).getAsJsonObject();
                    return new Privileges(
                            object.get("onlineChat").getAsJsonObject().get("enabled").getAsBoolean(),
                            object.get("multiplayerServer").getAsJsonObject().get("enabled").getAsBoolean(),
                            object.get("multiplayerRealms").getAsJsonObject().get("enabled").getAsBoolean(),
                            object.get("telemetry").getAsJsonObject().get("enabled").getAsBoolean()
                    );
                }
            })
            .registerTypeAdapter(Certificates.class, new TypeAdapter<Certificates>() {
                @Override
                public void write(JsonWriter out, Certificates value) throws IOException {
                    out.beginObject()
                            .name("keyPair")
                                .beginObject()
                                    .name("privateKey").value(value.keys().getLeft())
                                    .name("publicKey").value(value.keys().getRight())
                                .endObject()
                                .name("publicKeySignature").value(value.publicKeySignature())
                                .name("publicKeySignatureV2").value(value.publicKeySignatureV2())
                                .name("expiresAt").value(value.expiresAt())
                                .name("refreshedAfter").value(value.refreshedAfter())
                            .endObject()
                            .close();
                }

                @Override
                public Certificates read(JsonReader in) {
                    JsonObject object = JsonParser.parseReader(in).getAsJsonObject();
                    JsonObject keyPair = object.getAsJsonObject("keyPair");
                    return new Certificates(
                            Pair.of(keyPair.get("privateKey").getAsString(), keyPair.get("publicKey").getAsString()),
                            object.get("publicKeySignature").getAsString(),
                            object.get("publicKeySignatureV2").getAsString(),
                            object.get("expiresAt").getAsString(),
                            object.get("refreshedAfter").getAsString()
                    );
                }
            })
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create();

    /**
     * Makes a new instance of MojangAPI.
     * @param authorization The access token of the minecraft account.
     * @see Authenticator#authenticate(String)
     */
    public MojangAPI(String authorization) {
        this.sessionID = null;
        this.authorization = authorization;
    }

    MojangAPI(SessionID sessionID) {
        this.authorization = sessionID.accessToken();
        this.sessionID = sessionID;
    }

    final SessionID sessionID;
    private final String authorization;

    /**
     * Converts a list of players into their uuid.
     * @param names The list of names.
     * @return The uuids of the players.
     */
    @Nullable
    public static Map<String, String> getUUIDbyName(Collection<String> names) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(names)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
                Map<String, String> map = new HashMap<>();
                for (JsonObject object : json.asList().stream().map(JsonElement::getAsJsonObject).toList()) {
                    map.put(object.get("name").getAsString(), object.get("id").getAsString());
                }
                return map;
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Converts a player's name to their uuid.
     * @param name The name of the player.
     * @return The uuid of the player.
     */
    @Nullable
    public static String getUUIDbyName(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/"+name))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.get("id").getAsString();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Gets a public player profile of a player by their uuid.
     * @param uuid UUID of the player.
     * @return The public player profile.
     */
    @Nullable
    public static PublicPlayerProfile getProfile(String uuid) {
        return getProfile(uuid, false);
    }

    /**
     * Gets a public player profile of a player by their uuid.
     * @param uuid UUID of the player.
     * @param unsigned If true, will include the signature.
     * @return The public player profile.
     */
    @Nullable
    public static PublicPlayerProfile getProfile(String uuid, boolean unsigned) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid+"?unsigned="+unsigned))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), PublicPlayerProfile.class);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Gets all the hashes of all the blocked servers.
     * @return All the blocked servers.
     */
    public static List<String> getBlockedServers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/blockedservers"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Arrays.stream(response.body().split("\n")).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all the public keys.
     * @return All the public keys.
     */
    public static Map<String, List<String>> getPublicKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/publickeys"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            JsonArray profilePropertyKeysJson = json.getAsJsonArray("profilePropertyKeys");
            List<String> profilePropertyKeys = new ArrayList<>();
            for (JsonElement element : profilePropertyKeysJson) {
                profilePropertyKeys.add(element.getAsJsonObject().get("publicKey").getAsString());
            }

            JsonArray playerCertificateKeysJson = json.getAsJsonArray("playerCertificateKeys");
            List<String> playerCertificateKeys = new ArrayList<>();
            for (JsonElement element : playerCertificateKeysJson) {
                playerCertificateKeys.add(element.getAsJsonObject().get("publicKey").getAsString());
            }

            HashMap<String, List<String>> map = new HashMap<>();
            map.put("profilePropertyKeys", profilePropertyKeys);
            map.put("playerCertificateKeys", playerCertificateKeys);

            return map;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // signed in -------

    /**
     * Gets the profile of the account.
     * @return The profile.
     */
    public PrivatePlayerProfile getProfile() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), PrivatePlayerProfile.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all the attributes associated with the account.
     * @return The attributes.
     */
    public Attributes getAttributes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/player/attributes"))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Attributes.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of a privilege.
     * @param privilege The privilege to set.
     * @param newBool The new value of the preference
     */
    public boolean setAttribute(Privileges.Privilege privilege, boolean newBool) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/player/attributes"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer "+authorization)
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                                "privileges": {
                                    "<privilege>": <newBool>
                                }
                            }
                            """.replaceAll("<privilege>", privilege.name).replaceAll("<newBool>", newBool+"")))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of a profanity filter preference.
     * @param profanityFilterPreference The preference to set.
     * @param newBool The new value of the preference
     */
    public boolean setAttribute(ProfanityFilterPreferences.ProfanityFilterPreference profanityFilterPreference, boolean newBool) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/player/attributes"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer "+authorization)
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                                "profanityFilterPreferences": {
                                    "<privilege>": <newBool>
                                }
                            }
                            """.replaceAll("<privilege>", profanityFilterPreference.name).replaceAll("<newBool>", newBool+"")))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the uuids of all the blocked players of the account.
     * @return The blocklist.
     */
    public List<String> getBlocklist() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/privacy/blocklist"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonObject.getAsJsonArray("blockedProfiles").asList().stream().map(JsonElement::getAsString).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the certificates of the account.
     * @return The certificates.
     */
    public Certificates getCertificates() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/player/certificates"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Certificates.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the status of a product voucher.
     * @param giftCard The gift card code to check.
     * @return True if the gift card is valid, otherwise false.
     */
    public boolean checkProductVoucher(String giftCard) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/productvoucher/"+giftCard))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the status of a name.
     * @param name The name to check.
     * @return The status of the name.
     */
    public NameStatus getNameAvailability(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/name/"+name+"/available"))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return NameStatus.valueOf(JsonParser.parseString(response.body()).getAsJsonObject().get("status").getAsString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the name of the account logged in to.
     * @param name The new name to be set.
     * @return True if successful, false otherwise
     */
    public boolean changeName(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/name/"+name))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the skin of the account logged in to.
     * @param url The skin to be set.
     * @return True if successful, false otherwise
     */
    public boolean changeSkin(String url, Variant variant) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                                "variant": "<variant>",
                                "url": "<url>"
                            }
                            """.replaceAll("<variant>", variant.name()).replaceAll("<url>", url)))
                    .header("Authorization", "Bearer "+authorization)
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the skin of the account logged in to.
     * @param image The skin to be set.
     * @return True if successful, false otherwise
     */
    public boolean uploadSkin(RenderedImage image, Variant variant) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return uploadSkin(baos.toByteArray(), variant);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the skin of the account logged in to.
     * @param file The skin to be set.
     * @return True if successful, false otherwise
     */
    public boolean uploadSkin(File file, Variant variant) {
        try {
            return uploadSkin(Files.readAllBytes(file.toPath()), variant);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean uploadSkin(byte[] skinData, Variant variant) {
        String boundary = UUID.randomUUID().toString();
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"variant\"\r\n\r\n" +
                variant.name() + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n" +
                "Content-Type: image/png\r\n\r\n" +
                new String(skinData) + "\r\n" +
                "--" + boundary + "--";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                .header("Authorization", "Bearer " + authorization)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the skin of the account logged in to.
     * @return True if successful, false otherwise
     */
    public boolean resetSkin() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins/active"))
                    .DELETE()
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Hides the cape of the account logged in to.
     * @return True if successful, false otherwise
     */
    public boolean hideCape() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                    .DELETE()
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the cape shown on the account logged in to.
     * @param id The id of the cape.
     * @return True if successful, false otherwise
     */
    public boolean showCape(String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/capes/active"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"capeId\": \""+id+"\"}"))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the migration information of the account.
     * @return The migration information.
     */
    public MigrationInformation getMigrationInformation() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/rollout/v1/msamigration"))
                    .header("Authorization", "Bearer "+authorization)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), MigrationInformation.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends an OTP email to the account.
     * @return The otp ID (needed to verify).
     */
    public String accountMigrationOTP() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/twofactorauth/migration/otp"))
                    .header("Authorization", "Bearer "+authorization)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject().get("otpId").getAsString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies an account migration OTP.
     * @param otpId the otp ID.
     * @return True if successful, otherwise false.
     */
    public boolean verifyAccountMigrationOTP(String otpId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/twofactorauth/migration/otp/"+otpId+"/verify"))
                    .header("Authorization", "Bearer "+authorization)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"otp\": \""+otpId+"\"}"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Submits a migration token
     * @param email the email.
     * @return True if successful, otherwise false.
     */
    public boolean submitMigrationToken(String email) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/migration/token"))
                    .header("Authorization", "Bearer "+authorization)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"accountEmail\": \""+email+"\"}"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
