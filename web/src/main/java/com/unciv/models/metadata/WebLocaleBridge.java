package com.unciv.models.metadata;

import org.teavm.jso.JSBody;

public final class WebLocaleBridge {
    private WebLocaleBridge() {
    }

    @JSBody(
            params = {"languageTag", "first", "second"},
            script = "if (first == null && second == null) return 0;"
                    + "if (first == null) return -1;"
                    + "if (second == null) return 1;"
                    + "if (typeof Intl !== 'undefined' && Intl.Collator) "
                    + "return new Intl.Collator(languageTag || undefined, {usage:'sort', sensitivity:'accent'}).compare(first, second);"
                    + "return String(first).localeCompare(String(second));")
    public static int compare(String languageTag, String first, String second) {
        return 0;
    }
}
