package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.filter.Filter;
import de.blau.android.filter.PresetFilter;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;

/**
 * ModeConfig for modes that allow filters to be set
 */
public class FilterModeConfig implements ModeConfig {

    @Override
    public void setup(Main main, Logic logic) {
        // empty stub
    }

    @Override
    public void teardown(Main main, Logic logic) {
        // empty stub
    }

    @Override
    public HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
        HashMap<String, String> map = new HashMap<>();
        Map logicMap = logic.getMap();
        if (logicMap == null) return null;
        if (logicMap.getTracker() == null) return null;
        Location lastLocation = logicMap.getTracker().getLastLocation();
        if (lastLocation == null) return null;
        String position = e.getTagWithKey("position");
        if (position != null) return null;
        String newPosition = String.format("%s, %s;", lastLocation.getLatitude(), lastLocation.getLongitude());
//        map.put("position", position != null ?
//                (Objects.equals(newPosition, position) ? position : newPosition + position) : newPosition);
        map.put("position", newPosition);
        return map;
    }

    @Override
    @Nullable
    public ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
        // if we have a PresetFilter set for a single PresetItem, apply that automatically
        Logic logic = App.getLogic();
        Filter filter = logic.getFilter();
        if (filter instanceof PresetFilter) {
            PresetElement presetElement = ((PresetFilter) filter).getPresetElement();
            if (presetElement instanceof PresetItem || presetElement instanceof PresetGroup) {
                Preset preset = App.getCurrentRootPreset(ctx);
                ArrayList<PresetElementPath> result = new ArrayList<>();
                PresetElementPath path = presetElement.getPath(preset.getRootGroup());
                if (path != null) {
                    result.add(path);
                    return result;
                }
            }
        }
        return null;
    }
}
