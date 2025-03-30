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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.time.LocalDate;
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

    private EditText name, region, district, aggregator, farmerName, farmerMobile, cadastrNumber, year,
            cultureVarieties, sowingDate, cleaningDate, productivity, secondarySowingDate,
            secondaryCropHarvestDate, farmerSurName;
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

        TextView label = view.findViewById(R.id.label);
        label.setText("Создание поля");

        name = view.findViewById(R.id.name);
        name.setText("Поле " + App.getLogic().getWays().size());

        region = view.findViewById(R.id.region);
        district = view.findViewById(R.id.district);
        aggregator = view.findViewById(R.id.aggregator);
        farmerName = view.findViewById(R.id.farmerName);
        farmerSurName = view.findViewById(R.id.farmerSurName);
        farmerMobile = view.findViewById(R.id.farmerMobile);
        cadastrNumber = view.findViewById(R.id.cadastrNumber);
        year = view.findViewById(R.id.year);
        cultureVarieties = view.findViewById(R.id.cultureVarieties);
        sowingDate = view.findViewById(R.id.sowingDate);
        cleaningDate = view.findViewById(R.id.cleaningDate);
        productivity = view.findViewById(R.id.productivity);
//        secondarySowingDate = view.findViewById(R.id.secondarySowingDate);
//        secondaryCropHarvestDate = view.findViewById(R.id.secondaryCropHarvestDate);

        culture = view.findViewById(R.id.culture);
        technology = view.findViewById(R.id.technology);
        landCategory = view.findViewById(R.id.landCategory);
        irrigationType = view.findViewById(R.id.irrigationType);
//        secondaryCulture = view.findViewById(R.id.secondaryCulture);

        setDataPicker(sowingDate);
        setDataPicker(cleaningDate);
//        setDataPicker(secondarySowingDate);
//        setDataPicker(secondaryCropHarvestDate);


        String[] cultureData = {"Выращиваемая культура", "Пшеница", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, cultureData);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        String[] technologyData = {"Технология возделывания", "яровая", "озимая"};
        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, technologyData);
        technologyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        technology.setAdapter(technologyAdapter);

        String[] landCategoryData = {"Категория угодья", "орошаемая", "богара"};
        ArrayAdapter<String> landCategoryAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, landCategoryData);
        landCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landCategory.setAdapter(landCategoryAdapter);

        String[] irrigationTypeData = {"Тип полива", "каналы/гравитация", "установки"};
        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, irrigationTypeData);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);

//        String[] cultureData2 = {"Вторичная культура", "Пшеница", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
//        ArrayAdapter<String> secondaryCultureAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, cultureData2);
//        secondaryCultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        secondaryCulture.setAdapter(secondaryCultureAdapter);

        // Обработка кнопки сохранения
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            int size = App.getLogic().getWays().size();
            String nameValue = name.getText() == null || name.getText().toString().trim().isEmpty()
                    ? "Поле " + size : name.getText().toString();
            String regionValue = region.getText().toString();
            String districtValue = district.getText().toString();
            String aggregatorValue = aggregator.getText().toString();
            String farmerNameValue = farmerName.getText().toString();
            String farmerSurNameValue = farmerSurName.getText().toString();
            String farmerMobileValue = farmerMobile.getText().toString();
            String cadastrNumberValue = cadastrNumber.getText().toString();
            String yearValue = year.getText().toString();
            String cultureVarietiesValue = cultureVarieties.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
//            String secondarySowingDateValue = secondarySowingDate.getText().toString();
//            String secondaryCropHarvestDateValue = secondaryCropHarvestDate.getText().toString();
            String technologyValue = technology.getSelectedItem().toString();
            String landCategoryValue = landCategory.getSelectedItem().toString();
            String irrigationTypeValue = irrigationType.getSelectedItem().toString();
//            String secondaryCultureValue = secondaryCulture.getSelectedItem().toString();

            try {
                if (culture.getSelectedItem() == null) throw new NullPointerException("Выращиваемая культура");
                String cultureValue = culture.getSelectedItem().toString();
                if (cultureValue.equals("Выращиваемая культура")) throw new NullPointerException("Выращиваемая культура");
                // Сохранение всех значений
                save(nameValue, regionValue, districtValue, aggregatorValue, farmerNameValue, farmerMobileValue,
                        cadastrNumberValue, yearValue, cultureVarietiesValue, sowingDateValue, cleaningDateValue,
                        productivityValue, null, null, cultureValue,
                        technologyValue, landCategoryValue, irrigationTypeValue, null, farmerSurNameValue);
            } catch (NullPointerException exception) {
                Toast.makeText(getContext(),
                        String.format("Поле \"%s\" обязательно для заполнения", exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (edit) {
            label.setText("Редактирование поля");
            name.setText(lastSelectedWay.getTagWithKey("name"));
            region.setText(lastSelectedWay.getTagWithKey("region"));
            district.setText(lastSelectedWay.getTagWithKey("district"));
            aggregator.setText(lastSelectedWay.getTagWithKey("aggregator"));
            farmerName.setText(lastSelectedWay.getTagWithKey("farmerName"));
            farmerSurName.setText(lastSelectedWay.getTagWithKey("farmerSurName"));
            farmerMobile.setText(lastSelectedWay.getTagWithKey("farmerMobile"));
            cadastrNumber.setText(lastSelectedWay.getTagWithKey("cadastrNumber"));
            year.setText(lastSelectedWay.getTagWithKey("year"));
            cultureVarieties.setText(lastSelectedWay.getTagWithKey("cultureVarieties"));
            sowingDate.setText(lastSelectedWay.getTagWithKey("sowingDate"));
            cleaningDate.setText(lastSelectedWay.getTagWithKey("cleaningDate"));
            productivity.setText(lastSelectedWay.getTagWithKey("productivity"));
//            secondarySowingDate.setText(lastSelectedWay.getTagWithKey("secondarySowingDate"));
//            secondaryCropHarvestDate.setText(lastSelectedWay.getTagWithKey("secondaryCropHarvestDate"));
            try {
                culture.setSelection(Arrays.asList(cultureData).indexOf(lastSelectedWay.getTagWithKey("culture")));
                technology.setSelection(Arrays.asList(technologyData).indexOf(lastSelectedWay.getTagWithKey("technology")));
                landCategory.setSelection(Arrays.asList(landCategoryData).indexOf(lastSelectedWay.getTagWithKey("landCategory")));
                irrigationType.setSelection(Arrays.asList(irrigationTypeData).indexOf(lastSelectedWay.getTagWithKey("irrigationType")));
//                secondaryCulture.setSelection(Arrays.asList(cultureData2).indexOf(lastSelectedWay.getTagWithKey("secondaryCulture")));
            } catch (NullPointerException ignore) {
            }
        }
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);

        if (getDialog() != null) {
            getDialog().setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
        }
    }

    private void save(String name, String region, String district, String aggregator, String farmerName, String farmerMobile,
                      String cadastrNumber, String year, String cultureVarieties, String sowingDate,
                      String cleaningDate, String productivity, String secondarySowingDate,
                      String secondaryCropHarvestDate, String culture, String technology, String landCategory,
                      String irrigationType, String secondaryCulture, String farmerSurName) {
        Map<String, String> yield = new HashMap<>();
        yield.put("name", name);
        yield.put("region", region);
        yield.put("district", district);
        yield.put("aggregator", aggregator);
        yield.put("farmerName", farmerName);
        yield.put("farmerSurName", farmerSurName);
        yield.put("farmerMobile", farmerMobile);
        yield.put("cadastrNumber", cadastrNumber);
        yield.put("position", getPosition());
        yield.put("landuse", "farmland");
        yield.put("type", "multipolygon");

        Map<String, String> season = new HashMap<>();
        season.put("name", year);
        season.put("type", "site");
        season.put("technology", technology);
        if (year.matches("^\\d{4}$")) {
            int yearValue = Integer.parseInt(year);

            // Для современных версий Android (API 26 и выше)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                season.put("start", java.time.LocalDate.of(yearValue, 1, 1).toString());
                season.put("end", java.time.LocalDate.of(yearValue, 12, 31).toString());
            } else {
                // Для старых версий Android
                Calendar calendarStart = Calendar.getInstance();
                calendarStart.set(yearValue, Calendar.JANUARY, 1);
                Calendar calendarEnd = Calendar.getInstance();
                calendarEnd.set(yearValue, Calendar.DECEMBER, 31);

                // Преобразуем в строку в формате "yyyy-MM-dd"
                season.put("start", formatCalendarToString(calendarStart));
                season.put("end", formatCalendarToString(calendarEnd));
            }
        } else {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // Для современных версий Android (API 26 и выше)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                season.put("start", java.time.LocalDate.of(currentYear, 1, 1).toString());
                season.put("end", java.time.LocalDate.of(currentYear, 12, 31).toString());
            } else {
                // Для старых версий Android
                Calendar calendarStart = Calendar.getInstance();
                calendarStart.set(currentYear, Calendar.JANUARY, 1);
                Calendar calendarEnd = Calendar.getInstance();
                calendarEnd.set(currentYear, Calendar.DECEMBER, 31);

                // Преобразуем в строку в формате "yyyy-MM-dd"
                season.put("start", formatCalendarToString(calendarStart));
                season.put("end", formatCalendarToString(calendarEnd));
            }
        }
        Map<String, String> crop = new HashMap<>();
        crop.put("cultureVarieties", cultureVarieties);
        crop.put("sowingDate", sowingDate);
        crop.put("cleaningDate", cleaningDate);
        crop.put("productivity", productivity);
        crop.put("culture", culture);
        crop.put("landCategory", landCategory);
        crop.put("irrigationType", irrigationType);
        crop.put("landuse", "farmland");
        crop.put("name", "Посев 1");

        App.getDelegator().createYield(lastSelectedWay, yield, season, crop);
        dismiss();
    }

    private String formatCalendarToString(Calendar calendar) {
        // Преобразуем календарь в строку в формате "yyyy-MM-dd"
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Месяцы начинаются с 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Форматируем строку как "yyyy-MM-dd"
        return String.format("%04d-%02d-%02d", year, month, day);
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
