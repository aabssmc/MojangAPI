package cc.aabss.mojang.objects.realms;

import java.util.List;

public record PrivateRealm(long id,
                    String remoteSubscriptionId,
                    String owner,
                    String ownerUUID,
                    String name,
                    String motd,
                    String defaultPermission,
                    ServerState state,
                    int daysLeft,
                    boolean expired,
                    boolean expiredTrial,
                    boolean gracePeriod,
                    WorldType worldType,
                    List<RealmPlayer> players,
                    int maxPlayers,
                    String minigameName,
                    Long minigameId,
                    String minigameImage,
                    int activeSlot,
                    int slots,
                    boolean member,
                    long clubId
) {}
