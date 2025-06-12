package de.blau.android;

import static de.blau.android.AgroConstants.*;
import static de.blau.android.DatePiker.createSeason;
import static de.blau.android.TagHelper.getTagValue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;

public class BsEditCropFragment extends BottomSheetDialogFragment {

    private final Relation crop;
    private final Relation yield;
    private final List<Relation> seasons;
    private final List<Season> seasonSelector;
    private final Main main;

    private Button saveButton;
    private ImageView addSeason;

    private Spinner culture;
    private EditText cultureVarieties;
    private Spinner season;
    private Spinner landCategory;
    private Spinner irrigationType;
    private EditText sowingDate;
    private EditText cleaningDate;
    private EditText productivity;

    private final boolean isNew;

    public BsEditCropFragment(Relation crop, Relation yield, List<Relation> seasons, Main main, boolean isNew) {
        this.crop = crop;
        this.yield = yield;
        this.main = main;
        this.seasons = seasons;
        this.seasonSelector = App.getPreferences(getContext()).getSeasons();
        this.isNew = isNew;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_crop_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.getActivity() == null) return;

        saveButton = view.findViewById(R.id.btn_save);
        culture = view.findViewById(R.id.culture);
        cultureVarieties = view.findViewById(R.id.cultureVarieties);

        season = view.findViewById(R.id.season);
        landCategory = view.findViewById(R.id.landCategory);
        irrigationType = view.findViewById(R.id.irrigationType);
        sowingDate = view.findViewById(R.id.sowingDate);
        cleaningDate = view.findViewById(R.id.cleaningDate);
        productivity = view.findViewById(R.id.productivity);
        addSeason = view.findViewById(R.id.add_season);

        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, CULTURE_DATA);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        ArrayAdapter<String> landCategoryAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, LAND_CATEGORY_DATA);
        landCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landCategory.setAdapter(landCategoryAdapter);

        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, IRRIGATION_TYPE_DATA);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);

        ArrayAdapter<Season> seasonAdapter = getSeasonArrayAdapter();
        season.setAdapter(seasonAdapter);

        addSeason.setOnClickListener(v -> {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.season_create_diaglog, null);
            final EditText name = dialogView.findViewById(R.id.name);
            final EditText start = dialogView.findViewById(R.id.start);
            final EditText end = dialogView.findViewById(R.id.end);

            DatePiker.setDataPicker(start, getContext());
            DatePiker.setDataPicker(end, getContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Создание сезона")
                    .setView(dialogView)
                    .setPositiveButton("Создать", null)
                    .setNegativeButton("Отмена", null);

            AlertDialog dialog = builder.create();
            dialog.show();

            Button createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            createButton.setOnClickListener(view1 -> {
                String startStr = start.getText().toString();
                String endStr = end.getText().toString();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                try {
                    if (!startStr.isEmpty() && !endStr.isEmpty()) {
                        Date startDate = sdf.parse(startStr);
                        Date endDate = sdf.parse(endStr);

                        if (startDate.after(endDate)) {
                            start.setError("Дата начала позже даты конца");
                            end.setError("Дата конца раньше даты начала");
                            return;
                        }
                    } else {
                        if (startStr.isEmpty()) start.setError("Заполните поле");
                        if (endStr.isEmpty()) end.setError("Заполните поле");
                        return;
                    }
                    Season newSeason = new Season(name.getText().toString());
                    newSeason.setStartDate(startStr);
                    newSeason.setEndDate(endStr);
//                    createSeason(newSeason);
                    seasonSelector.add(newSeason);
                    seasonAdapter.notifyDataSetChanged();
                    season.setSelection(seasonSelector.size() - 1);
                    dialog.dismiss();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

        });

        if (main.currentSeason != null) {
            Season currentSeason = seasonSelector.get(seasonSelector.size() - 1);
            for (Season s : seasonSelector) {
                if (main.currentSeason.equals(s)) {
                    currentSeason = s;
                    break;
                }
            }
            season.setSelection(seasonSelector.indexOf(currentSeason));
        }

        DatePiker.setDataPicker(sowingDate, getContext());
        DatePiker.setDataPicker(cleaningDate, getContext());
        if (crop != null) {
            cultureVarieties.setText(getTagValue(crop, CROP_TAG_CULTURE_VARIETIES));
            sowingDate.setText(getTagValue(crop, CROP_TAG_SOWING_DATE));
            cleaningDate.setText(getTagValue(crop, CROP_TAG_CLEANING_DATE));
            productivity.setText(getTagValue(crop, CROP_TAG_PRODUCTIVITY));
            try {
                culture.setSelection(Arrays.asList(CULTURE_DATA).indexOf(crop.getTagWithKey(CROP_TAG_CULTURE)));
                landCategory.setSelection(Arrays.asList(LAND_CATEGORY_DATA).indexOf(crop.getTagWithKey(CROP_TAG_LAND_CATEGORY)));
                irrigationType.setSelection(Arrays.asList(IRRIGATION_TYPE_DATA).indexOf(crop.getTagWithKey(CROP_TAG_IRRIGATION_TYPE)));
                OsmElement osmElement = crop.getMemberElements().get(0);
                Season selectedSeason = null;
                for (Season s : seasonSelector) {
                    if (Objects.equals(s.getName(), osmElement.getTagWithKey(Tags.KEY_NAME))) {
                        selectedSeason = s;
                        break;
                    }
                }
                season.setSelection(seasonSelector.indexOf(selectedSeason));
            } catch (NullPointerException ignore) {
            }
        }

        saveButton.setOnClickListener(v -> {
            String cultureVarietiesValue = cultureVarieties.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
            String landCategoryValue = landCategory.getSelectedItem().toString();
            String irrigationTypeValue = irrigationType.getSelectedItem().toString();
            Season seasonValue = (Season) season.getSelectedItem();
            try {
                if (culture.getSelectedItem() == null)
                    throw new NullPointerException("Выращиваемая культура");
                String cultureValue = culture.getSelectedItem().toString();
                if (cultureValue.equals("Выращиваемая культура"))
                    throw new NullPointerException("Выращиваемая культура");

                Map<String, String> map = new HashMap<>();
                map.put(CROP_TAG_CULTURE, cultureValue);
                map.put(CROP_TAG_CULTURE_VARIETIES, cultureVarietiesValue);
                map.put(CROP_TAG_SOWING_DATE, sowingDateValue);
                map.put(CROP_TAG_CLEANING_DATE, cleaningDateValue);
                map.put(CROP_TAG_PRODUCTIVITY, productivityValue);
                map.put(CROP_TAG_LAND_CATEGORY, landCategoryValue);
                map.put(CROP_TAG_IRRIGATION_TYPE, irrigationTypeValue);
                map.put(Tags.KEY_TYPE, TYPE_CROP);

                Relation seasonForFieldRelation = App.getDelegator().createSeasonForFieldRelation(yield, seasonValue, seasons);
                if (isNew) {
                    Relation newCrop = App.getDelegator().createCropForSeasonRelation(seasonForFieldRelation, map);
                    if (getParentFragment() instanceof BsEditYieldFragment) {
                        ((BsEditYieldFragment) getParentFragment()).updateCropList(newCrop);
                    }
                } else {
                    if (crop == null) return;
                    App.getDelegator().updateCropAndSeasonRelation(crop, map, seasonForFieldRelation);
                    if (getParentFragment() instanceof BsEditYieldFragment) {
                        ((BsEditYieldFragment) getParentFragment()).updateCropList();
                    }
                }
            } catch (NullPointerException exception) {
                Toast.makeText(getContext(),
                        String.format("Поле \"%s\" обязательно для заполнения", exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            dismiss();
        });

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);

//        if (getDialog() != null) {
//            getDialog().setCancelable(false);
//            getDialog().setCanceledOnTouchOutside(false);
//        }
    }

    @NonNull
    private ArrayAdapter<Season> getSeasonArrayAdapter() {
        ArrayAdapter<Season> seasonAdapter = new ArrayAdapter<Season>(getActivity(), R.layout.season_dropdown_item, seasonSelector) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.season_dropdown_item, parent, false);
                }
                Season season = getItem(position);
                TextView name = convertView.findViewById(R.id.text1);
                name.setText("Сезон " + season.getName());

                TextView date = convertView.findViewById(R.id.text2);
                date.setText(season.getStartDate() + " - " + season.getEndDate());
                return convertView;
            }
        };
        seasonAdapter.setDropDownViewResource(R.layout.agro_simple_spinner_item);
        return seasonAdapter;
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

        App.getPreferences(getContext()).saveSeasons(seasonSelector);
    }

}
