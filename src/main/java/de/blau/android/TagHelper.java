package de.blau.android;

import de.blau.android.osm.OsmElement;

public class TagHelper {

    public static String getTagValue(OsmElement osmElement, String key) {
        if (key == null || osmElement == null) return "";
        String tagWithKey = osmElement.getTagWithKey(key);
        return tagWithKey == null ? "" : tagWithKey;
    }

    public static String getText(String string) {
        return string == null ? "" : string;
    }
}
