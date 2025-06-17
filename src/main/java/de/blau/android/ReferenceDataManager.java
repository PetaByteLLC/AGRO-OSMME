package de.blau.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import de.blau.android.osm.BoundingBox;
import de.blau.android.util.LatLon;

public class ReferenceDataManager {

    private static final String TAG = "RefDataManager_Simple";
    private static final String GEOJSON_FILE = "districts2.geojson";

    private static volatile List<ReferenceFeature> referenceFeatures = null;
    private static volatile boolean loadAttempted = false;
    private static volatile boolean loadSuccessful = false;

    public static class ReferenceFeature {
        final Map<String, String> properties;
        final BoundingBox boundingBox;

        public ReferenceFeature(Map<String, String> props, BoundingBox bbox) {
            this.properties = props != null ? props : new HashMap<>();
            this.boundingBox = bbox;
        }


        public boolean containsPoint(LatLon point) {
            if (this.boundingBox == null || !this.boundingBox.isValid() || point == null) {
                return false;
            }

            double latDouble = point.getLat();
            double lonDouble = point.getLon();
            return this.boundingBox.contains(latDouble, lonDouble);
        }

        public String getAdm1Ky() { return properties.get("pname_r"); }
        public String getAdm2Ky() { return properties.get("dname_r"); }
    }

    public static boolean isDataLoadedSuccessfully() {
        return loadSuccessful && referenceFeatures != null;
    }


    public static synchronized void loadData(Context context) {
        if (loadSuccessful) return;
        if (loadAttempted && !loadSuccessful) return;

        Log.i(TAG, "Attempting to load reference data (simple bbox)...");
        loadAttempted = true;
        loadSuccessful = false;
        referenceFeatures = null;

        List<ReferenceFeature> loadedFeatures = new ArrayList<>();
        AssetManager assetManager = context.getApplicationContext().getAssets();

        try (InputStream is = assetManager.open(GEOJSON_FILE)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject geoJson = new JSONObject(jsonString);
            JSONArray features = geoJson.getJSONArray("features");
            Log.d(TAG, "Found " + features.length() + " features total.");

            for (int i = 0; i < features.length(); i++) {
                JSONObject featureJson = features.getJSONObject(i);
                JSONObject propertiesJson = featureJson.optJSONObject("properties");
                JSONObject geometryJson = featureJson.optJSONObject("geometry");

                // 1. Парсим атрибуты
                Map<String, String> parsedProperties = parseProperties(propertiesJson);

                // 2. Рассчитываем BoundingBox (только если есть геометрия)
                BoundingBox calculatedBbox = calculateBoundsFromGeometry(geometryJson);

                // 3. Создаем Feature, только если есть BBox
                if (parsedProperties != null && calculatedBbox != null) {
                    ReferenceFeature feature = new ReferenceFeature(parsedProperties, calculatedBbox);
                    loadedFeatures.add(feature);
                }
                // Объекты без геометрии или с ошибкой геометрии пропускаются
            }

            referenceFeatures = loadedFeatures;
            loadSuccessful = true;
            Log.i(TAG, "Reference data loaded. " + referenceFeatures.size() + " features with valid BBox parsed.");

        } catch (Exception e) {
            Log.e(TAG, "Error loading/parsing reference data", e);
            referenceFeatures = null;
            loadSuccessful = false;
        }
    }

    public static ReferenceFeature findFeatureContainingPoint(LatLon point) {
        if (!isDataLoadedSuccessfully()) {
            Log.w(TAG, "findFeature failed: Data not loaded.");
            return null;
        }
        if (point == null) {
            Log.w(TAG, "findFeature failed: Input point is null.");
            return null;
        }
        if (referenceFeatures.isEmpty()) {
            Log.w(TAG, "findFeature failed: No reference features to search.");
            return null;
        }

        Log.d(TAG, "Searching for BBox containing: " + point);
        for (ReferenceFeature feature : referenceFeatures) {
            if (feature.containsPoint(point)) {
                Log.i(TAG, "MATCH (BBox): Found feature " + feature.getAdm2Ky());
                return feature; // Возвращаем первый найденный
            }
        }

        Log.i(TAG, "NO MATCH (BBox): No feature contains point " + point);
        return null;
    }

    private static Map<String, String> parseProperties(JSONObject propertiesJson) {
        if (propertiesJson == null) return new HashMap<>();
        Map<String, String> props = new HashMap<>();
        Iterator<String> keys = propertiesJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            props.put(key, propertiesJson.optString(key, ""));
        }
        return props;
    }

    private static BoundingBox calculateBoundsFromGeometry(JSONObject geometryJson) {
        if (geometryJson == null) return null;
        double[] bbox_ref = {90.0, -90.0, 180.0, -180.0, 0.0}; // minLat, maxLat, minLon, maxLon, foundFlag
        try {
            processCoordsForBounds(geometryJson.optJSONArray("coordinates"), bbox_ref);
            boolean coordsFound = (bbox_ref[4] > 0.5);
            if (coordsFound) {
                double minLat = bbox_ref[0], maxLat = bbox_ref[1], minLon = bbox_ref[2], maxLon = bbox_ref[3];
                return new BoundingBox(minLat, minLon, maxLat, maxLon);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error calculating bounds from geometry", e);
        }
        return null; // Не удалось рассчитать BBox
    }

    private static void processCoordsForBounds(JSONArray coords, double[] bbox_ref) throws JSONException {
        if (coords == null || coords.length() == 0) return;
        Object first = coords.opt(0);
        if (!(first instanceof JSONArray)) {
            if (coords.length() >= 2) {
                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);
                bbox_ref[0] = Math.min(bbox_ref[0], lat); // minLat
                bbox_ref[1] = Math.max(bbox_ref[1], lat); // maxLat
                bbox_ref[2] = Math.min(bbox_ref[2], lon); // minLon
                bbox_ref[3] = Math.max(bbox_ref[3], lon); // maxLon
                bbox_ref[4] = 1.0;
            }
            return;
        }
        for (int i = 0; i < coords.length(); i++) {
            processCoordsForBounds(coords.optJSONArray(i), bbox_ref);
        }
    }
}