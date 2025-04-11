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
        regions = getGroup(main.getAllFields());

        if (regions.isEmpty()) {
            Toast.makeText(getContext(), "По этому сезоны данных нету", Toast.LENGTH_SHORT).show();
            dismiss();
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
                main.editYield(relation, getChildFragmentManager());
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

    private List<Region> getGroup(List<Relation> relations) {
        Set<String> regions = new HashSet<>();
        for (Relation yield : relations) {
            String regionValue = yield.getTagWithKey(YIELD_TAG_REGION);
            if (regionValue != null && !regionValue.isEmpty()) {
                regions.add(regionValue);
            }
        }

        List<Region> regionList = new ArrayList<>();
        for (String region : regions) {
            Set<Culture> cultureList = new HashSet<>();
            Set<String> yieldCultures = new HashSet<>();
            List<Relation> yields = new ArrayList<>();
            for (Relation yield : relations) {
                if (!Objects.equals(yield.getTagWithKey(YIELD_TAG_REGION), region)) continue;
                List<Relation> seasons = yield.getParentRelations();
                if (seasons != null) {
                    for (Relation season : seasons) {
                        if (main.currentSeason != null) {
                            if (!Objects.equals(main.currentSeason.getName(), season.getTagWithKey(Tags.KEY_NAME)))
                                continue;
                        }
                        if (season == null) continue;
                        if (season.getParentRelations() == null) continue;
                        for (Relation crop : season.getParentRelations()) {
                            String cultureValue = crop.getTagWithKey(CROP_TAG_CULTURE);
                            if (cultureValue == null) continue;
                            if (cultureValue.isEmpty()) continue;
                            if (yieldCultures.add(cultureValue)) {
                                if (yields.isEmpty()) {
                                    yields.add(yield);
                                } else {
                                    for (Relation oldYield : yields) {
                                        if (Objects.equals(oldYield.getOsmId(), yield.getOsmId()))
                                            continue;
                                        yields.add(yield);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (yieldCultures.size() == 1) {
                for (String culture : yieldCultures) {
                    cultureList.add(new Culture(culture, yields));
                    break;
                }
            }
            if (yieldCultures.size() > 1) {
                cultureList.add(new Culture(OTHER_CULTURE, yields));
            }
            if (!cultureList.isEmpty())
                regionList.add(new Region(region, new ArrayList<>(cultureList)));
        }
        return regionList;
    }

}
