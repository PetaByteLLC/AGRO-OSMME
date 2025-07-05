package de.blau.android;

import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.AgroConstants.CROP_TAG_NAME;
import static de.blau.android.AgroConstants.OTHER_CULTURE;
import static de.blau.android.AgroConstants.REMOVE_FIELD_MESSAGE;
import static de.blau.android.AgroConstants.YIELD_TAG_REGION;
import static de.blau.android.Main.SELECTED_SEASON;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.blau.android.contract.Ui;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;

public class BottomSheetFragmentAllField extends BottomSheetDialogFragment {

    private final Main main;
    private RegionAdapter regionAdapter;
    private List<Region> regions;

    public BottomSheetFragmentAllField(Main main) {
        this.main = main;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_all_field, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currentSeason = SELECTED_SEASON;
        if (currentSeason == null) {
            Toast.makeText(getContext(), "Сезон не выбран!", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        regions = getGroup(App.getLogic().getWays(), currentSeason);
        if (regions.isEmpty()) {
            Toast.makeText(getContext(), "По этому сезоны данных нет.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        regionAdapter = new RegionAdapter(regions, new FieldAdapter.OnFieldClickListener() {
            @Override
            public void remove(Way way) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Вы уверены, что хотите удалить поле?")
                        .setMessage(REMOVE_FIELD_MESSAGE)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Удалить", (dialogInterface, which) -> {
                            App.getDelegator().removeFieldRelation(way);
                            main.invalidateMap();
                            Toast.makeText(getContext(), "Поле удалёно", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }

            @Override
            public void editMetaData(Way way) {
                main.editYield(way, getChildFragmentManager(), false);
            }

            @Override
            public void edit(Way way) {
                BoundingBox bounds = way.getBounds();
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                main.editor(way);
                dismiss();
            }

            @Override
            public void move(Way way) {
                BoundingBox bounds = way.getBounds();
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
                App.getLogic().setZoom(main.getMap(), Ui.ZOOM_FOR_ZOOMTO - 2);
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                dismiss();
            }
        });

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(regionAdapter);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);
    }

    private Set<String> getCulturesForWay(Way way, String currentSeasonName) {
        Set<String> culturesFound = new HashSet<>();
        if (way == null || way.getTags() == null || currentSeasonName == null) {
            return culturesFound;
        }

        // Итерируемся по всем тегам объекта Way
        for (java.util.Map.Entry<String, String> tag : way.getTags().entrySet()) {
            String tagKey = tag.getKey();
            String tagValue = tag.getValue();

            // 1. Проверяем, что ключ тега соответствует формату "crop_plan:..."
            if (tagKey.startsWith(CROP_TAG_NAME)) {
                // 2. Извлекаем сезон из ключа (например, "2024" из "crop_plan:2024:0")
                String[] keyParts = tagKey.split(":");
                if (keyParts.length >= 2) {
                    String seasonFromTag = keyParts[1];

                    // 3. Сравниваем с текущим сезоном
                    if (currentSeasonName.equals(seasonFromTag)) {
                        // 4. Парсим значение тега (например, "culture:Клубника;variety:белая пленка")
                        String[] valueParts = tagValue.split(";");
                        for (String part : valueParts) {
                            String[] kv = part.split(":", 2); // Разделяем на ключ-значение
                            if (kv.length == 2 && CROP_TAG_CULTURE.equals(kv[0].trim())) {
                                String cultureName = kv[1].trim();
                                if (!cultureName.isEmpty()) {
                                    culturesFound.add(cultureName);
                                }
                            }
                        }
                    }
                }
            }
        }
        return culturesFound;
    }

    /**
     * Группирует список Way'ев по регионам и культурам.
     *
     * @param allWays           Список всех объектов Way (полей).
     * @param currentSeasonName Сезон, для которого производится группировка.
     * @return Список регионов, содержащих сгруппированные по культурам поля.
     */
    public List<Region> getGroup(List<Way> allWays, String currentSeasonName) {
        if (allWays == null || allWays.isEmpty()) {
            return Collections.emptyList();
        }

        java.util.Map<String, java.util.Map<String, List<Way>>> groupedByRegionAndCulture = new HashMap<>();

        for (Way way : allWays) {
            if (way == null) continue;

            String regionValue = way.getTagWithKey(YIELD_TAG_REGION);
            if (regionValue == null || regionValue.isEmpty()) {
                continue;
            }

            // Вызываем наш метод, работающий с Way
            Set<String> culturesForThisWay = getCulturesForWay(way, currentSeasonName);
            if (culturesForThisWay.isEmpty()) {
                continue;
            }

            String effectiveCultureName;
            if (culturesForThisWay.size() == 1) {
                effectiveCultureName = culturesForThisWay.iterator().next();
            } else {
                effectiveCultureName = OTHER_CULTURE;
            }

            // Логика группировки остается той же
            java.util.Map<String, List<Way>> culturesMap = groupedByRegionAndCulture.get(regionValue);
            if (culturesMap == null) {
                culturesMap = new HashMap<>();
                groupedByRegionAndCulture.put(regionValue, culturesMap);
            }
            List<Way> waysList = culturesMap.get(effectiveCultureName);
            if (waysList == null) {
                waysList = new ArrayList<>();
                culturesMap.put(effectiveCultureName, waysList);
            }waysList.add(way);
        }

        List<Region> resultRegionList = new ArrayList<>();
        for (java.util.Map.Entry<String, java.util.Map<String, List<Way>>> regionEntry : groupedByRegionAndCulture.entrySet()) {
            String regionName = regionEntry.getKey();
            java.util.Map<String, List<Way>> culturesInRegionMap = regionEntry.getValue();
            List<Culture> cultureListForCurrentRegion = new ArrayList<>();

            for (java.util.Map.Entry<String, List<Way>> cultureEntry : culturesInRegionMap.entrySet()) {
                String cultureName = cultureEntry.getKey();
                List<Way> fieldsForCulture = cultureEntry.getValue(); // Теперь это список Way'ев
                if (!fieldsForCulture.isEmpty()) {
                    cultureListForCurrentRegion.add(new Culture(cultureName, fieldsForCulture));
                }
            }

            if (!cultureListForCurrentRegion.isEmpty()) {
                resultRegionList.add(new Region(regionName, cultureListForCurrentRegion));
            }
        }
        return resultRegionList;
    }

}
