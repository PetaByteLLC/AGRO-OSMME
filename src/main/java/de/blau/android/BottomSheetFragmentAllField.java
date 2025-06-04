package de.blau.android;

import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.AgroConstants.OTHER_CULTURE;
import static de.blau.android.AgroConstants.REMOVE_FIELD_MESSAGE;
import static de.blau.android.AgroConstants.YIELD_TAG_REGION;

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
import java.util.Objects;
import java.util.Set;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;

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
        Season currentSeason = main.currentSeason;
        if (currentSeason == null || currentSeason.getName() == null) {
            Toast.makeText(getContext(), "Сезон не выбран!", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        regions = getGroup(main.getAllFields(), currentSeason.getName());
        if (regions.isEmpty()) {
            Toast.makeText(getContext(), "По этому сезоны данных нет.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        regionAdapter = new RegionAdapter(regions, new FieldAdapter.OnFieldClickListener() {
            @Override
            public void remove(Relation relation) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Вы уверены, что хотите удалить поле?")
                        .setMessage(REMOVE_FIELD_MESSAGE)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Удалить", (dialogInterface, which) -> {
                            App.getDelegator().removeFieldRelation(relation);
                            Toast.makeText(getContext(), "Поле удалёно", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }

            @Override
            public void editMetaData(Relation relation) {
                main.editYield(relation, getChildFragmentManager(), false);
            }

            @Override
            public void edit(Relation relation) {
                BoundingBox bounds = relation.getBounds();
                if (bounds != null) {
                    final ViewBox box = new ViewBox(bounds);
                    double[] center = box.getCenter();
                    main.invalidateMap();
                    main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                    main.editor(relation);
                    dismiss();
                }
            }

            @Override
            public void move(Relation relation) {
                BoundingBox bounds = relation.getBounds();
                if (bounds != null) {
                    final ViewBox box = new ViewBox(bounds);
                    double[] center = box.getCenter();
                    main.invalidateMap();
                    main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                    dismiss();
                }
            }
        });

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(regionAdapter);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);
    }

    public void updateList() {
        regionAdapter.updateData();
    }

    private Set<String> getCulturesForYield(Relation yield, String currentSeasonName) {
        Set<String> culturesFound = new HashSet<>();
        if (yield == null) return culturesFound;
        List<Relation> seasons = yield.getParentRelations();
        if (seasons == null) return culturesFound;
        for (Relation season : seasons) {
            if (season == null) continue;
            if (currentSeasonName != null) {
                String seasonNameFromTag = season.getTagWithKey(Tags.KEY_NAME);
                if (!Objects.equals(currentSeasonName, seasonNameFromTag)) {
                    continue;
                }
            }
            List<Relation> crops = season.getParentRelations();
            if (crops == null) continue;
            for (Relation crop : crops) {
                if (crop == null) continue;
                String cultureValue = crop.getTagWithKey(CROP_TAG_CULTURE);
                if (cultureValue != null && !cultureValue.isEmpty()) {
                    culturesFound.add(cultureValue);
                }
            }
        }
        return culturesFound;
    }

    public List<Region> getGroup(List<Relation> allYieldRelations, String currentSeasonName) {
        if (allYieldRelations == null || allYieldRelations.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.Map<String, java.util.Map<String, List<Relation>>> groupedByRegionAndCulture = new HashMap<>();
        for (Relation yield : allYieldRelations) {
            if (yield == null) continue;
            String regionValue = yield.getTagWithKey(YIELD_TAG_REGION);
            if (regionValue == null || regionValue.isEmpty()) {
                continue;
            }
            Set<String> culturesForThisYield = getCulturesForYield(yield, currentSeasonName);
            if (culturesForThisYield.isEmpty()) {
                continue;
            }
            String effectiveCultureName;
            if (culturesForThisYield.size() == 1) {
                effectiveCultureName = culturesForThisYield.iterator().next();
            } else {
                effectiveCultureName = OTHER_CULTURE;
            }

            java.util.Map<String, List<Relation>> culturesMap = groupedByRegionAndCulture.get(regionValue);
            if (culturesMap == null) {
                culturesMap = new HashMap<>();
                groupedByRegionAndCulture.put(regionValue, culturesMap);
            }

            List<Relation> yieldsList = culturesMap.get(effectiveCultureName);
            if (yieldsList == null) {
                yieldsList = new ArrayList<>();
                culturesMap.put(effectiveCultureName, yieldsList);
            }
            yieldsList.add(yield);
        }

        List<Region> resultRegionList = new ArrayList<>();
        for (java.util.Map.Entry<String, java.util.Map<String, List<Relation>>> regionEntry : groupedByRegionAndCulture.entrySet()) {
            String regionName = regionEntry.getKey();
            java.util.Map<String, List<Relation>> culturesInRegionMap = regionEntry.getValue();
            List<Culture> cultureListForCurrentRegion = new ArrayList<>();
            for (java.util.Map.Entry<String, List<Relation>> cultureEntry : culturesInRegionMap.entrySet()) {
                String cultureName = cultureEntry.getKey();
                List<Relation> fieldsForCulture = cultureEntry.getValue();
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
