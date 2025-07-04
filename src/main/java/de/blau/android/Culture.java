package de.blau.android;

import java.util.List;

import de.blau.android.osm.Way;

public class Culture {

    protected String name;
    protected List<Way> yields;

    public Culture(String name, List<Way> yields) {
        this.name = name;
        this.yields = yields;
    }
}
