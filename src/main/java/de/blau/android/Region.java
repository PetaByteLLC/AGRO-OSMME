package de.blau.android;

import java.util.List;

public class Region {

    protected String name;
    protected List<Culture> cultures;
    protected boolean isExpanded;

    public Region(String name, List<Culture> cultures) {
        this.name = name;
        this.cultures = cultures;
        isExpanded = false;
    }

}
