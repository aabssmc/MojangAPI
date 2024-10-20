package cc.aabss.mojang;

import cc.aabss.mojang.objects.realms.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class RealmsAPI extends MojangAPI {

    /**
     * Makes a new instance of RealmsAPI.
     * @param cookie The realms cookie needed to authenticate.
     * @param enviornment The realms api URL.
     */
    public RealmsAPI(RealmsCookie cookie, Enviornment enviornment) {
        super(cookie.sid().accessToken());
        this.cookie = cookie.toString();
        this.enviornment = enviornment.url;
    }

    /**
     * Makes a new instance of RealmsAPI.
     * @param sid The session id of the player.
     * @param user The name of the user.
     * @param version The minecraft version the user is playing on.
     * @param enviornment The realms api URL.
     */
    public RealmsAPI(SessionID sid, String user, String version, Enviornment enviornment) {
        super(sid.accessToken());
        this.cookie = new RealmsCookie(sid, user, version).toString();
        this.enviornment = enviornment.url;
    }

    /**
     * Makes a new instance of RealmsAPI.
     * @param accessToken The access token from the account.
     * @param uuid The uuid of the user.
     * @param user The name of the user.
     * @param version The minecraft version the user is playing on.
     * @param enviornment The realms api URL.
     */
    public RealmsAPI(String accessToken, String uuid, String user, String version, Enviornment enviornment) {
        super(new SessionID(accessToken, uuid).accessToken());
        this.cookie = new RealmsCookie(new SessionID(accessToken, uuid), user, version).toString();
        this.enviornment = enviornment.url;
    }

    /**
     * Makes a new instance of RealmsAPI.
     * @param accessToken The access token from the account.
     * @param nameOrUuid Either the uuid or the name of the user.
     * @param version The minecraft version the user is playing on.
     * @param enviornment The realms api URL.
     */
    public RealmsAPI(String accessToken, String nameOrUuid, String version, Enviornment enviornment) {
        super(new SessionID(accessToken, (nameOrUuid.length() > 16 ? nameOrUuid : getUUIDbyName(nameOrUuid))).accessToken());
        this.cookie = new RealmsCookie(this.sessionID, this.getProfile().name(), version).toString();
        this.enviornment = enviornment.url;
    }

    private final String cookie;
    private final String enviornment;

    /**
     * Whether the user can access realms.
     * @return True if the user can access realms, otherwise false.
     */
    public boolean isRealmsAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/available"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().equals("true");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the realms compatibility of the client.
     * @return The client compatibility.
     */
    public ClientCompatibility getClientCompatibility() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/client/compatible"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ClientCompatibility.valueOf(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the realms the user can join.
     * @return The list of realms that the user is invited to or owns.
     */
    public List<Realm> getWorlds() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("servers");
            return jsonArray.asList().stream().map(jsonElement -> gson.fromJson(jsonElement.getAsJsonObject(), Realm.class)).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a realm by its ID.
     * @param id The realm ID.
     * @return The realm.
     */
    public Realm getWorld(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+id))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Realm.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the server address.
     * @param id The realm ID.
     * @return The server object.
     */
    public Server getServerAddress(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/v1/"+id+"/join/pc"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Server.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all the backups that the realm has.
     * @param id The realm ID.
     * @return The list of backups.
     */
    public List<Backup> getBackups(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+id+"/backups"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("backups");
            return jsonArray.asList().stream().map(jsonElement -> gson.fromJson(jsonElement.getAsJsonObject(), Backup.class)).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the download of the latest realm world backup.
     * @param id The realm ID.
     * @param world The world ID. (1-4)
     * @return The download of the backup.
     */
    public BackupDownload getBackupDownload(long id, @Range(from = 1, to = 4) int world) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+id+"/slot/"+world+"/download"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), BackupDownload.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the list of operators of the realm, you must own the server.
     * @param id The realm ID.
     * @return The download of the backup.
     */
    public List<String> getOps(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/ops/"+id))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("ops");
            return jsonArray.asList().stream().map(JsonElement::getAsString).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the current life of a server subscription.
     * @param id The realm ID.
     * @return The current subscription.
     */
    public Subscription getSubscriptions(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/subscriptions/"+id))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Subscription.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays a status message to the user, along with a link to the Mojang website.
     * @param id The realm ID.
     * @return The buy object.
     */
    public Buy getBuy(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/buy"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Buy.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the amount of invites the player currently has waiting.
     * @param id The realm ID.
     * @return The pending invites count.
     */
    public Integer getInvitesPendingCount(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/buy"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Integer.valueOf(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the amount of invites the player currently has waiting.
     * @param id The realm ID.
     * @return The pending invites.
     */
    public List<Invite> getInvitesPending(long id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/buy"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("invites");
            return jsonArray.asList().stream().map(jsonElement -> gson.fromJson(jsonElement, Invite.class)).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets already existed maps in Realms.
     * @param type The world type/template.
     * @param page The page number.
     * @param pageSize The page size.
     * @return The pending invites.
     */
    public Templates getTemplates(WorldType type, int page, int pageSize) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/templates/"+type+"?page="+page+"&pageSize="+pageSize))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), Templates.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the trial boolean.
     * @return True if you can use the free trial, false otherwise.
     */
    public boolean getTrial() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/trial"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Boolean.getBoolean(response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets player activities at the moment.
     * @return The player activities.
     */
    public List<String> getActiveLivePlayerList() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/activities/liveplayerlist"))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Arrays.stream(response.body().split("\n")).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Agrees/Accepts the TOS.
     * @return True if successful, false otherwise.
     */
    public boolean agreeToTOS() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/mco/tos/agreed"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets player activities at the moment.
     * @param id The world ID.
     * @param uuid The player's uuid.
     * @return The list of operators.
     */
    public List<String> opPlayer(long id, String uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/ops/"+id+"/"+uuid))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("ops");
            return jsonArray.asList().stream().map(JsonElement::getAsString).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invites a player to the realm.
     * @param id The world ID.
     * @param player The player invite.
     * @return The new realm.
     */
    public PrivateRealm invitePlayer(long id, PlayerInvite player) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/invites/"+id))
                    .POST(HttpRequest.BodyPublishers.ofString(player.toJson().toString()))
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return gson.fromJson(response.body(), PrivateRealm.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets world to mini-games.
     * @param worldId The world ID.
     * @param minigameId The mini-game ID.
     * @return True if successful, false otherwise.
     */
    public boolean setWorld(long worldId, long minigameId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/minigames/"+minigameId+"/"+worldId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rejects an invitation to a realm.
     * @param invitationId The invitation ID.
     * @return True if successful, false otherwise.
     */
    public boolean rejectInvitation(long invitationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/invites/reject/"+invitationId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Accepts an invitation to a realm.
     * @param invitationId The invitation ID.
     * @return True if successful, false otherwise.
     */
    public boolean acceptInvitation(long invitationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/invites/accept/"+invitationId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces the world of the realm with the specified backup.
     * @param worldId The world ID.
     * @param backupId The backup ID.
     * @return True if successful, false otherwise.
     */
    public boolean replaceBackup(long worldId, long backupId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+worldId+"/backups?backupId="+backupId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the realm.
     * @param worldId The world ID.
     * @return True if successful, false otherwise.
     */
    public boolean openRealm(long worldId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+worldId+"/open"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the realm.
     * @param worldId The world ID.
     * @return True if successful, false otherwise.
     */
    public boolean closeRealm(long worldId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/worlds/"+worldId+"/close"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Kick player from your realms even if player not accept invitation.
     * @param worldId The world ID.
     * @param uuid The player uuid.
     * @return True if successful, false otherwise.
     */
    public boolean unInvite(long worldId, String uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/invites/"+worldId+"/invite/"+uuid))
                    .DELETE()
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 204;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove operator status for player by uuid.
     * @param worldId The world ID.
     * @param uuid The player uuid.
     * @return The operators.
     */
    public List<String> deop(long worldId, String uuid) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(enviornment+"/ops/"+worldId+"/"+uuid))
                    .DELETE()
                    .header("Cookie", cookie)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("ops");
            return jsonArray.asList().stream().map(JsonElement::getAsString).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
