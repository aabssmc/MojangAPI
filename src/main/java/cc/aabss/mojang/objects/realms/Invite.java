package cc.aabss.mojang.objects.realms;

public record Invite(String invitationId, String worldName, String worldDescription, String worldOwnerName, String worldOwnerUuid, Long date) {}
