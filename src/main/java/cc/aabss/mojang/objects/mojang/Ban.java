package cc.aabss.mojang.objects.mojang;

import com.google.gson.annotations.SerializedName;

public record Ban(String banId, Long expires, Reason reason, String reasonMessage) {
    public enum Reason {
        @SerializedName("false_reporting") FALSE_REPORTING,
        @SerializedName("hate_speech") HATE_SPEECH,
        @SerializedName("terrorism_or_violent_extremism") TERRORISM_OR_VIOLENT_EXTREMISM,
        @SerializedName("imminent_harm") IMMINENT_HARM,
        @SerializedName("non_consensual_intimate_imagery") NON_CONSENSUAL_INTIMATE_IMAGERY,
        @SerializedName("harassment_or_bullying") HARASSMENT_OR_BULLYING,
        @SerializedName("defamation_impersonation_false_information") DEFAMATION_IMPERSONATION_FALSE_INFORMATION,
        @SerializedName("self_harm_or_suicide") SELF_HARM_OR_SUICIDE,
        @SerializedName("alcohol_tobacco_drugs") ALCOHOL_TOBACCO_DRUGS
    }
}
