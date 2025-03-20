package de.blau.android;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.blau.android.osm.Node;
import de.blau.android.osm.Way;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private final Way lastSelectedWay;
    private final Main main;
    private boolean edit;

    private EditText region, district, aggregator, farmerName, farmerMobile, cadastrNumber, year,
            cultureVarieties, sowingDate, cleaningDate, productivity, secondarySowingDate,
            secondaryCropHarvestDate;
    private Spinner culture, technology, landCategory, irrigationType, secondaryCulture;

    public BottomSheetFragment(Way lastSelectedWay, Main main, boolean edit) {
        this.lastSelectedWay = lastSelectedWay;
        this.main = main;
        this.edit = edit;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bs_field_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.getActivity() == null) return;
        ListView listView = view.findViewById(R.id.coordinate_list);
        ArrayAdapter<Node> adapter = new ArrayAdapter<Node>(this.getActivity(), R.layout.coordinate_list_item, lastSelectedWay.getNodes()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.coordinate_list_item, parent, false);
                }
                Node currentNode = getItem(position);
                TextView nodeNameText = convertView.findViewById(R.id.title);
                TextView nodeCoordinatesText = convertView.findViewById(R.id.value);
                nodeNameText.setText("Точка " + (position + 1));
                nodeCoordinatesText.setText(currentNode.getLat() + ", " + currentNode.getLon());
                return convertView;
            }
        };
        listView.setAdapter(adapter);

        region = view.findViewById(R.id.region);
        district = view.findViewById(R.id.district);
        aggregator = view.findViewById(R.id.aggregator);
        farmerName = view.findViewById(R.id.farmerName);
        farmerMobile = view.findViewById(R.id.farmerMobile);
        cadastrNumber = view.findViewById(R.id.cadastrNumber);
        year = view.findViewById(R.id.year);
        cultureVarieties = view.findViewById(R.id.cultureVarieties);
        sowingDate = view.findViewById(R.id.sowingDate);
        cleaningDate = view.findViewById(R.id.cleaningDate);
        productivity = view.findViewById(R.id.productivity);
        secondarySowingDate = view.findViewById(R.id.secondarySowingDate);
        secondaryCropHarvestDate = view.findViewById(R.id.secondaryCropHarvestDate);

        culture = view.findViewById(R.id.culture);
        technology = view.findViewById(R.id.technology);
        landCategory = view.findViewById(R.id.landCategory);
        irrigationType = view.findViewById(R.id.irrigationType);
        secondaryCulture = view.findViewById(R.id.secondaryCulture);

        setDataPicker(sowingDate);
        setDataPicker(cleaningDate);
        setDataPicker(secondarySowingDate);
        setDataPicker(secondaryCropHarvestDate);


        String[] cultureData = {"Выращиваемая культура", "Пшеница", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, cultureData);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        String[] technologyData = {"яровая", "озимая"};
        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, technologyData);
        technologyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        technology.setAdapter(technologyAdapter);

        String[] landCategoryData = {"орошаемая", "богара"};
        ArrayAdapter<String> landCategoryAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, landCategoryData);
        landCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landCategory.setAdapter(landCategoryAdapter);

        String[] irrigationTypeData = {"каналы/гравитация", "установки"};
        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, irrigationTypeData);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);

        String[] cultureData2 = {"Вторичная культура", "Пшеница", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
        ArrayAdapter<String> secondaryCultureAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, cultureData2);
        secondaryCultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        secondaryCulture.setAdapter(secondaryCultureAdapter);

        // Обработка кнопки сохранения
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String regionValue = region.getText().toString();
            String districtValue = district.getText().toString();
            String aggregatorValue = aggregator.getText().toString();
            String farmerNameValue = farmerName.getText().toString();
            String farmerMobileValue = farmerMobile.getText().toString();
            String cadastrNumberValue = cadastrNumber.getText().toString();
            String yearValue = year.getText().toString();
            String cultureVarietiesValue = cultureVarieties.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
            String secondarySowingDateValue = secondarySowingDate.getText().toString();
            String secondaryCropHarvestDateValue = secondaryCropHarvestDate.getText().toString();
            String cultureValue = culture.getSelectedItem().toString();
            String technologyValue = technology.getSelectedItem().toString();
            String landCategoryValue = landCategory.getSelectedItem().toString();
            String irrigationTypeValue = irrigationType.getSelectedItem().toString();
            String secondaryCultureValue = secondaryCulture.getSelectedItem().toString();

            // Сохранение всех значений
            save(regionValue, districtValue, aggregatorValue, farmerNameValue, farmerMobileValue,
                    cadastrNumberValue, yearValue, cultureVarietiesValue, sowingDateValue, cleaningDateValue,
                    productivityValue, secondarySowingDateValue, secondaryCropHarvestDateValue, cultureValue,
                    technologyValue, landCategoryValue, irrigationTypeValue, secondaryCultureValue);
        });

        if (edit) {
            region.setText(lastSelectedWay.getTagWithKey("region"));
            district.setText(lastSelectedWay.getTagWithKey("district"));
            aggregator.setText(lastSelectedWay.getTagWithKey("aggregator"));
            farmerName.setText(lastSelectedWay.getTagWithKey("farmerName"));
            farmerMobile.setText(lastSelectedWay.getTagWithKey("farmerMobile"));
            cadastrNumber.setText(lastSelectedWay.getTagWithKey("cadastrNumber"));
            year.setText(lastSelectedWay.getTagWithKey("year"));
            cultureVarieties.setText(lastSelectedWay.getTagWithKey("cultureVarieties"));
            sowingDate.setText(lastSelectedWay.getTagWithKey("sowingDate"));
            cleaningDate.setText(lastSelectedWay.getTagWithKey("cleaningDate"));
            productivity.setText(lastSelectedWay.getTagWithKey("productivity"));
            secondarySowingDate.setText(lastSelectedWay.getTagWithKey("secondarySowingDate"));
            secondaryCropHarvestDate.setText(lastSelectedWay.getTagWithKey("secondaryCropHarvestDate"));
            try {
                culture.setSelection(Arrays.asList(cultureData).indexOf(lastSelectedWay.getTagWithKey("culture")));
                technology.setSelection(Arrays.asList(technologyData).indexOf(lastSelectedWay.getTagWithKey("technology")));
                landCategory.setSelection(Arrays.asList(landCategoryData).indexOf(lastSelectedWay.getTagWithKey("landCategory")));
                irrigationType.setSelection(Arrays.asList(irrigationTypeData).indexOf(lastSelectedWay.getTagWithKey("irrigationType")));
                secondaryCulture.setSelection(Arrays.asList(cultureData2).indexOf(lastSelectedWay.getTagWithKey("secondaryCulture")));
            } catch (NullPointerException ignore) {}
        }
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void save(String region, String district, String aggregator, String farmerName, String farmerMobile,
                      String cadastrNumber, String year, String cultureVarieties, String sowingDate,
                      String cleaningDate, String productivity, String secondarySowingDate,
                      String secondaryCropHarvestDate, String culture, String technology, String landCategory,
                      String irrigationType, String secondaryCulture) {
        Map<String, String> tags = new HashMap<>();
        tags.put("landuse", "farmland");
        tags.put("position", getPosition());
        tags.put("region", region);
        tags.put("district", district);
        tags.put("aggregator", aggregator);
        tags.put("farmerName", farmerName);
        tags.put("farmerMobile", farmerMobile);
        tags.put("cadastrNumber", cadastrNumber);
        tags.put("year", year);
        tags.put("cultureVarieties", cultureVarieties);
        tags.put("sowingDate", sowingDate);
        tags.put("cleaningDate", cleaningDate);
        tags.put("productivity", productivity);
        tags.put("secondarySowingDate", secondarySowingDate);
        tags.put("secondaryCropHarvestDate", secondaryCropHarvestDate);
        tags.put("culture", culture);
        tags.put("technology", technology);
        tags.put("landCategory", landCategory);
        tags.put("irrigationType", irrigationType);
        tags.put("secondaryCulture", secondaryCulture);
        App.getDelegator().setTags(lastSelectedWay, tags);
        dismiss();
    }

    private String getPosition() {
        de.blau.android.Map logicMap = App.getLogic().getMap();
        if (logicMap == null) return null;
        if (logicMap.getTracker() == null) return null;
        Location lastLocation = logicMap.getTracker().getLastLocation();
        if (lastLocation == null) return null;
        return String.format("%s, %s;", lastLocation.getLatitude(), lastLocation.getLongitude());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        main.invalidateMap();
        App.getLogic().deselectAll();
        App.getLogic().setLocked(true);
        main.invisibleUnlockButton();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu(); // Обновляем меню в активности
        }
    }

    private void setDataPicker(EditText editText) {
        editText.setOnClickListener(v -> {
            // Получаем текущую дату
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Открываем DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view1, int year, int monthOfYear, int dayOfMonth) {
                            // Устанавливаем выбранную дату в EditText
                            String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                            editText.setText(date);
                        }
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }
}
