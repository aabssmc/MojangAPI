package cc.aabss.mojang.objects.realms;

import java.util.List;

public record Realm(long id,
                    String remoteSubscriptionId,
                    String owner,
                    String ownerUUID,
                    String name,
                    String motd,
                    ServerState state,
                    int daysLeft,
                    boolean expired,
                    boolean expiredTrial,
                    WorldType worldType,
                    List<String> players,
                    int maxPlayers,
                    String minigameName,
                    Long minigameId,
                    String minigameImage,
                    int activeSlot,
                    int slots,
                    boolean member,
                    int parentWorldId,
                    String parentWorldName,
                    ServerCompatibility compatibility,
                    String activeVersion
) {}
