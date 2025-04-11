package de.blau.android;

import java.util.List;

import de.blau.android.osm.Relation;

public class Culture {

    protected String name;
    protected List<Relation> yields;

    public Culture(String name, List<Relation> yields) {
        this.name = name;
        this.yields = yields;
    }
}
