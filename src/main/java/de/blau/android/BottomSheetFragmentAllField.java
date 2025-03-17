package de.blau.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.blau.android.osm.Way;

public class BottomSheetFragmentAllField extends BottomSheetDialogFragment {

    private List<Way> ways;
    private Main main;

    public BottomSheetFragmentAllField(List<Way> ways, Main main) {
        this.ways = ways;
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
        List<String> groups = getGroup();
        CustomExpandableListAdapter newAdapter = new CustomExpandableListAdapter(getContext(), groups, getChildren(groups), main, this);
        ExpandableListView expandableListView = view.findViewById(R.id.expandableList);
        expandableListView.setAdapter(newAdapter);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private List<String> getGroup() {
        ArrayList<String> list = new ArrayList<>();
        for (Way way : ways) {
            String region = way.getTagWithKey("region");
            if (region != null) {
                list.add(region);
            }
        }
        list.add("Все");
        return list;
    }

    private HashMap<String, List<Way>> getChildren(List<String> groups) {
        HashMap<String, List<Way>> result = new HashMap<>();

        for (String group : groups) {
            ArrayList<Way> value = new ArrayList<>();
            for (Way way : ways) {
                if (Objects.equals(group, way.getTagWithKey("region"))) value.add(way);
            }
            result.put(group, value);
        }

        result.put("Все", ways);
        return result;
    }

}
