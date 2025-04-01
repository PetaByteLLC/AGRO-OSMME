package de.blau.android;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;

public class BsEditYieldFragment extends BottomSheetDialogFragment {

    private final Relation yield;
    private final List<Relation> seasons;
    private final List<Relation> crops;
    private final Main main;

    private TextView label;

    private Button toggleButton;
    private Button saveBtn;
    private LinearLayout editTextContainer;

    private EditText name;
    private EditText region;
    private EditText district;
    private EditText farmerSurName;
    private EditText farmerName;
    private EditText farmerMobile;
    private EditText cadastrNumber;
    private EditText aggregator;
    private Spinner technology;

    private boolean areEditTextsVisible;

    private ListView cropList;
    private Button cropAdd;
    private ArrayAdapter<Relation> cropAdapter;

    public BsEditYieldFragment(Relation yield, List<Relation> seasons, List<Relation> crops, Main main) {
        this.yield = yield;
        this.seasons = seasons;
        this.crops = crops;
        this.main = main;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_field_editor2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.getActivity() == null) return;

        label = view.findViewById(R.id.label);
        toggleButton = view.findViewById(R.id.toggleButton);
        saveBtn = view.findViewById(R.id.btn_save);
        editTextContainer = view.findViewById(R.id.editTextContainer);

        name = view.findViewById(R.id.name);
        region = view.findViewById(R.id.region);
        district = view.findViewById(R.id.district);
        farmerSurName = view.findViewById(R.id.farmerSurName);
        farmerName = view.findViewById(R.id.farmerName);
        farmerMobile = view.findViewById(R.id.farmerMobile);
        cadastrNumber = view.findViewById(R.id.cadastrNumber);
        aggregator = view.findViewById(R.id.aggregator);
        technology = view.findViewById(R.id.technology);
        cropList = view.findViewById(R.id.crop_list);
        cropAdd = view.findViewById(R.id.crop_add);

        label.setText((getTagValue(yield, "name") + " " + getTagValue(yield, "area")).trim());
        toggleButton.setOnClickListener(v -> {
            if (areEditTextsVisible) {
                collapseEditTexts();
            } else {
                expandEditTexts();
            }
        });

        String[] technologyData = {"Технология возделывания", "яровая", "озимая"};
        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, technologyData);
        technologyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        technology.setAdapter(technologyAdapter);

        cropAdapter = new ArrayAdapter<Relation>(this.getActivity(), R.layout.crop_list_item, crops) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.crop_list_item, parent, false);
                }
                Relation relation = getItem(position);

                TextView culture = convertView.findViewById(R.id.culture);
                culture.setText(getTagValue(relation, "culture"));

                TextView season = convertView.findViewById(R.id.season);
                List<Relation> parentRelations = relation.getParentRelations();
                Relation parentRelation = parentRelations.get(0);
                season.setText("Сезон " + getTagValue(parentRelation, "name"));
                return convertView;
            }
        };
        cropList.setAdapter(cropAdapter);
        cropList.setOnItemClickListener((p, v, pos, id) -> {
            Relation clickedRelation = (Relation) p.getItemAtPosition(pos);
            BsEditCropFragment cropFragment = new BsEditCropFragment(clickedRelation, yield, seasons, main, false);
            cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
        });

        cropAdd.setOnClickListener(v -> {
            Relation relationWithNewId = App.getDelegator().getFactory().createRelationWithNewId();
            BsEditCropFragment cropFragment = new BsEditCropFragment(relationWithNewId, yield, seasons, main, true);
            cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
        });

        name.setText(getTagValue(yield, "name"));
        region.setText(getTagValue(yield, "region"));
        district.setText(getTagValue(yield, "district"));
        aggregator.setText(getTagValue(yield, "aggregator"));
        farmerName.setText(getTagValue(yield, "farmerName"));
        farmerSurName.setText(getTagValue(yield, "farmerSurName"));
        farmerMobile.setText(getTagValue(yield, "farmerMobile"));
        cadastrNumber.setText(getTagValue(yield, "cadastrNumber"));
        try {
            technology.setSelection(Arrays.asList(technologyData).indexOf(yield.getTagWithKey("technology")));
        } catch (NullPointerException ignore) {
        }

        saveBtn.setOnClickListener(v -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", name.getText().toString());
            map.put("region", region.getText().toString());
            map.put("district", district.getText().toString());
            map.put("aggregator", aggregator.getText().toString());
            map.put("farmerName", farmerName.getText().toString());
            map.put("farmerSurName", farmerSurName.getText().toString());
            map.put("farmerMobile", farmerMobile.getText().toString());
            map.put("cadastrNumber", cadastrNumber.getText().toString());
            map.put("technology", technology.getSelectedItem().toString());

            if (getParentFragment() instanceof BottomSheetFragmentAllField) {
                ((BottomSheetFragmentAllField) getParentFragment()).updateList();
            }

            App.getDelegator().updateTags(yield, map);
            dismiss();
        });

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);
//
//        if (getDialog() != null) {
//            getDialog().setCancelable(false);
//            getDialog().setCanceledOnTouchOutside(false);
//        }
    }

    public void updateCropList() {
        cropAdapter.notifyDataSetChanged();
    }

    public void updateCropList(Relation newCrop) {
        crops.add(newCrop);
        cropAdapter.notifyDataSetChanged();
    }

    private void collapseEditTexts() {
        Animation collapseAnimation = AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.fade_out);
        collapseAnimation.setDuration(300);
        editTextContainer.startAnimation(collapseAnimation);
        editTextContainer.setVisibility(View.GONE);
        areEditTextsVisible = false;
        toggleButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this.getActivity(), R.drawable.baseline_arrow_drop_down_24), null);
    }

    private void expandEditTexts() {
        editTextContainer.setVisibility(View.VISIBLE);
        Animation expandAnimation = AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.fade_in);
        expandAnimation.setDuration(300);
        editTextContainer.startAnimation(expandAnimation);
        areEditTextsVisible = true;
        toggleButton.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this.getActivity(), R.drawable.baseline_arrow_drop_up_24), null);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        main.invalidateMap();
        App.getLogic().deselectAll();
        App.getLogic().setLocked(true);
        main.invisibleUnlockButton();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private String getTagValue(OsmElement osmElement, String key) {
        if (key == null || osmElement == null) return "";
        String tagWithKey = osmElement.getTagWithKey(key);
        return tagWithKey == null ? "" : tagWithKey;
    }
}
