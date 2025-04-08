package de.blau.android;

import static de.blau.android.AgroConstants.*;

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
import de.blau.android.osm.Tags;

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

        label.setText((getTagValue(yield, Tags.KEY_NAME) + " " + getTagValue(yield, Tags.KEY_AREA)).trim());
        toggleButton.setOnClickListener(v -> {
            if (areEditTextsVisible) {
                collapseEditTexts();
            } else {
                expandEditTexts();
            }
        });

        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, TECHNOLOGY_DATA);
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
                culture.setText(getTagValue(relation, CROP_TAG_CULTURE));

                TextView season = convertView.findViewById(R.id.season);
                List<OsmElement> parentRelations = relation.getMemberElements();
                Relation parentRelation = (Relation) parentRelations.get(0);
                season.setText("Сезон " + getTagValue(parentRelation, Tags.KEY_NAME));
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
            BsEditCropFragment cropFragment = new BsEditCropFragment(null, yield, seasons, main, true);
            cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
        });

        name.setText(getTagValue(yield, Tags.KEY_NAME));
        region.setText(getTagValue(yield, YIELD_TAG_REGION));
        district.setText(getTagValue(yield, YIELD_TAG_DISTRICT));
        aggregator.setText(getTagValue(yield, YIELD_TAG_AGGREGATOR));
        farmerName.setText(getTagValue(yield, YIELD_TAG_FARMER_NAME));
        farmerSurName.setText(getTagValue(yield, YIELD_TAG_FARMER_SURNAME));
        farmerMobile.setText(getTagValue(yield, YIELD_TAG_FARMER_MOBILE));
        cadastrNumber.setText(getTagValue(yield, YIELD_TAG_CADASTRAL_NUMBER));
        try {
            technology.setSelection(Arrays.asList(TECHNOLOGY_DATA).indexOf(yield.getTagWithKey(YIELD_TAG_TECHNOLOGY)));
        } catch (NullPointerException ignore) {
        }

        saveBtn.setOnClickListener(v -> {
            Map<String, String> map = new HashMap<>();
            map.put(Tags.KEY_NAME, name.getText().toString());
            map.put(YIELD_TAG_REGION, region.getText().toString());
            map.put(YIELD_TAG_DISTRICT, district.getText().toString());
            map.put(YIELD_TAG_AGGREGATOR, aggregator.getText().toString());
            map.put(YIELD_TAG_FARMER_NAME, farmerName.getText().toString());
            map.put(YIELD_TAG_FARMER_SURNAME, farmerSurName.getText().toString());
            map.put(YIELD_TAG_FARMER_MOBILE, farmerMobile.getText().toString());
            map.put(YIELD_TAG_CADASTRAL_NUMBER, cadastrNumber.getText().toString());
            map.put(YIELD_TAG_TECHNOLOGY, technology.getSelectedItem().toString());

            App.getDelegator().updateFieldRelationTags(yield, map);
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

        if (getParentFragment() instanceof BottomSheetFragmentAllField) {
            ((BottomSheetFragmentAllField) getParentFragment()).updateList();
        }
    }
}
