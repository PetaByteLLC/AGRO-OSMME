package de.blau.android;

import static de.blau.android.AgroConstants.*;
import static de.blau.android.Main.YEARS;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.blau.android.osm.Way;

public class BsEditCropFragment extends BottomSheetDialogFragment {

    private final Way yield;
    private final Main main;
    private final String key;

    private Spinner season;

    private Spinner culture;
    private Spinner technology;
    private EditText variety;
    private EditText sowingDate;
    private EditText cleaningDate;
    private EditText productivity;

    public BsEditCropFragment(String key, Way yield, Main main) {
        this.key = key;
        this.yield = yield;
        this.main = main;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_crop_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.getActivity() == null) return;

        Button saveButton = view.findViewById(R.id.btn_save);

        culture = view.findViewById(R.id.culture);
        variety = view.findViewById(R.id.variety);
        technology = view.findViewById(R.id.technology);
        sowingDate = view.findViewById(R.id.sowingDate);
        cleaningDate = view.findViewById(R.id.cleaningDate);
        productivity = view.findViewById(R.id.productivity);

        season = view.findViewById(R.id.season);

        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, CULTURE_DATA);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, TECHNOLOGY_DATA);
        technologyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        technology.setAdapter(technologyAdapter);

        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, YEARS);
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        season.setAdapter(seasonAdapter);
        season.setSelection(YEARS.size() - 1);

        DatePiker.setDataPicker(sowingDate, getContext());
        DatePiker.setDataPicker(cleaningDate, getContext());

        editValues();

        saveButton.setOnClickListener(v -> {
            String cultureVarietiesValue = variety.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
            String technologyValue = technology.getSelectedItemPosition() < 1 ? "" : technology.getSelectedItem().toString();
            String seasonValue = season.getSelectedItem().toString();

            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            try {
                sowingDate.setError(null);
                cleaningDate.setError(null);
                if (!sowingDateValue.isEmpty() && !cleaningDateValue.isEmpty()) {
                    Date startDate = sdf.parse(sowingDateValue);
                    Date endDate = sdf.parse(cleaningDateValue);
                    assert startDate != null;
                    if (startDate.after(endDate)) {
                        sowingDate.setError("дата посева не должна быть позже даты сбора");
                        cleaningDate.setError("дата сбора не должна быть раньше даты посева");
                        return;
                    }
                }
            } catch (ParseException e) {
                Toast.makeText(getContext(),
                        "Даты не правильно заполнены",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (culture.getSelectedItemPosition() < 1)
                    throw new NullPointerException("Выращиваемая культура");
                String cultureValue = culture.getSelectedItem().toString();

                Map<String, String> map = new HashMap<>();
                map.put(CROP_TAG_CULTURE, cultureValue);
                map.put(CROP_TAG_TECHNOLOGY, technologyValue);
                map.put(CROP_TAG_CULTURE_VARIETIES, cultureVarietiesValue);
                map.put(CROP_TAG_SOWING_DATE, sowingDateValue);
                map.put(CROP_TAG_CLEANING_DATE, cleaningDateValue);
                map.put(CROP_TAG_PRODUCTIVITY, productivityValue);

                StringBuilder cropData = new StringBuilder();
                int count = 0;
                for (Map.Entry<String, String> prop : map.entrySet()){
                    cropData.append(prop.getKey())
                            .append(":")
                            .append(prop.getValue());
                    if (count < map.size()) {
                        cropData.append(";");
                    }
                }

                String tag = CROP_TAG_NAME + ":" + seasonValue + ":" + getNumber(seasonValue);
                if (key == null) {
                    App.getDelegator().addTag(tag, cropData.toString(), yield);
                } else {
                    if (Objects.equals(seasonValue, key.split(":")[1])) {
                        App.getDelegator().updateTags(yield, Map.of(key, cropData.toString()));
                    } else {
                        App.getDelegator().deleteTag(key, yield);
                        App.getDelegator().addTag(tag, cropData.toString(), yield);
                    }
                }
                if (getParentFragment() instanceof BsEditYieldFragment) {
                    ((BsEditYieldFragment) getParentFragment()).updateCropList();
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

    private int getNumber(String season) {
        List<Map.Entry<String, String>> crops = getCrops(yield);
        int max = 0;
        for (Map.Entry<String, String> crop : crops) {
            String cropKey = crop.getKey();
            if (cropKey != null && cropKey.contains(season)) {
                int val = Integer.parseInt(cropKey.substring(cropKey.length() - 1));
                if (val > max) max = val;
            };
        }
        return max + 1;
    }

    private void editValues() {
        if (key != null) {
            String dataString = yield.getTagWithKey(key);
            if (dataString == null) return;
            sowingDate.setText(getSubData(dataString, CROP_TAG_SOWING_DATE));
            cleaningDate.setText(getSubData(dataString, CROP_TAG_CLEANING_DATE));
            productivity.setText(getSubData(dataString, CROP_TAG_PRODUCTIVITY));
            variety.setText(getSubData(dataString, CROP_TAG_CULTURE_VARIETIES));
            try {
                culture.setSelection(Arrays.asList(CULTURE_DATA).indexOf(getSubData(dataString, CROP_TAG_CULTURE)));
                technology.setSelection(Arrays.asList(TECHNOLOGY_DATA).indexOf(getSubData(dataString, CROP_TAG_TECHNOLOGY)));
                season.setSelection(YEARS.indexOf(key.split(":")[1]));
            } catch (Exception ignore) {}
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        main.invalidateMap();
        assert App.getLogic() != null;
        App.getLogic().deselectAll();
        App.getLogic().setLocked(true);
        main.invisibleUnlockButton();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public static List<java.util.Map.Entry<String, String>> getCrops(Way way) {
        ArrayList<java.util.Map.Entry<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String> tag : way.getTags().entrySet()) {
            if (tag.getKey().startsWith(CROP_TAG_NAME)) list.add(tag);
        }
        return list;
    }

    public static String getSubData(String data, String fieldName) {
        String[] split = data.split(";");
        for (String prop : split) {
            String[] kv = prop.split(":");
            if (Objects.equals(kv[0], fieldName)) {
                return kv.length > 1 ? kv[1] : "";
            }
        }
        return "";
    }
}
