package cc.aabss.mojang.objects.mojang;

import cc.aabss.mojang.objects.Pair;

public record Certificates(Pair<String, String> keys, String publicKeySignature, String publicKeySignatureV2, String expiresAt, String refreshedAfter) {}
