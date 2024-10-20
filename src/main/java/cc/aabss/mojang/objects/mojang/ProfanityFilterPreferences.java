package cc.aabss.mojang.objects.mojang;

public record ProfanityFilterPreferences(boolean profanityFilterOn) {
    public enum ProfanityFilterPreference {
        PROFANITY_FILTER_PREFERENCE("profanityFilterOn");

        ProfanityFilterPreference(String name) {
            this.name = name;
        }

        public final String name;
    }
}
