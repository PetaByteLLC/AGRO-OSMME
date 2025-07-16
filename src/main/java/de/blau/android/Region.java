package de.blau.android;

import java.util.List;

import de.blau.android.osm.Way;

public class Region {

    protected String name;
    protected List<Culture> cultures;
    protected List<Way> other;
    protected boolean isExpanded;

    public Region(String name, List<Culture> cultures, List<Way> other) {
        this.name = name;
        this.cultures = cultures;
        isExpanded = false;
    }
}
