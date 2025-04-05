package de.blau.android;

import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private final Way lastSelectedWay;
    private final Main main;

    private EditText name, region, district, aggregator, farmerName, farmerMobile, cadastrNumber,
            cultureVarieties, sowingDate, cleaningDate, productivity, farmerSurName;
    private Spinner culture, technology, landCategory, irrigationType, season;

    private ImageView addSeason;

    public BottomSheetFragment(Way lastSelectedWay, Main main) {
        this.lastSelectedWay = lastSelectedWay;
        this.main = main;
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

        addSeason = view.findViewById(R.id.add_season);
        region = view.findViewById(R.id.region);
        district = view.findViewById(R.id.district);
        aggregator = view.findViewById(R.id.aggregator);
        farmerName = view.findViewById(R.id.farmerName);
        farmerSurName = view.findViewById(R.id.farmerSurName);
        farmerMobile = view.findViewById(R.id.farmerMobile);
        cadastrNumber = view.findViewById(R.id.cadastrNumber);
        season = view.findViewById(R.id.season);
        cultureVarieties = view.findViewById(R.id.cultureVarieties);
        sowingDate = view.findViewById(R.id.sowingDate);
        cleaningDate = view.findViewById(R.id.cleaningDate);
        productivity = view.findViewById(R.id.productivity);
        culture = view.findViewById(R.id.culture);
        technology = view.findViewById(R.id.technology);
        landCategory = view.findViewById(R.id.landCategory);
        irrigationType = view.findViewById(R.id.irrigationType);

        setDataPicker(sowingDate);
        setDataPicker(cleaningDate);

        List<Relation> seasons = main.getUnicalSeason();

        ArrayAdapter<Relation> seasonAdapter = new ArrayAdapter<Relation>(getActivity(), R.layout.season_dropdown_item, seasons) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.season_dropdown_item, parent, false);
                }
                Relation relation = getItem(position);
                TextView name = convertView.findViewById(R.id.text1);
                name.setText("Сезон " + getTagValue(relation, "name"));

                TextView date = convertView.findViewById(R.id.text2);
                date.setText(getTagValue(relation, "start") + " - " + getTagValue(relation, "end"));
                return convertView;
            }
        };
        seasonAdapter.setDropDownViewResource(R.layout.agro_simple_spinner_item);
        season.setAdapter(seasonAdapter);

        addSeason.setOnClickListener(v -> {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.season_create_diaglog, null);
            final EditText name = dialogView.findViewById(R.id.name);
            final EditText start = dialogView.findViewById(R.id.start);
            final EditText end = dialogView.findViewById(R.id.end);

            setDataPicker(start);
            setDataPicker(end);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Создания сезона")
                    .setView(dialogView)
                    .setPositiveButton("Создать", (dialog, which) -> {
                        Map<String, String> values = new HashMap<>();
                        String nameValue = name.getText().toString();
                        values.put("name", nameValue);
                        values.put("start", start.getText().toString());
                        values.put("end", end.getText().toString());
                        values.put("type", StorageDelegator.TYPE_SEASON);

                        if (nameValue.matches("^\\d{4}$") && start.getText().length() == 0 && end.getText().length() == 0) {
                            int yearValue = Integer.parseInt(nameValue);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                values.put("start", java.time.LocalDate.of(yearValue, 1, 1).toString());
                                values.put("end", java.time.LocalDate.of(yearValue, 12, 31).toString());
                            } else {
                                Calendar calendarStart = Calendar.getInstance();
                                calendarStart.set(yearValue, Calendar.JANUARY, 1);
                                Calendar calendarEnd = Calendar.getInstance();
                                calendarEnd.set(yearValue, Calendar.DECEMBER, 31);
                                values.put("start", formatCalendarToString(calendarStart));
                                values.put("end", formatCalendarToString(calendarEnd));
                            }
                        }
                        Relation newRelationSeason = App.getDelegator().getFactory().createRelationWithNewId();
                        newRelationSeason.addTags(values);
                        App.getDelegator().setElementCreatedStatus(newRelationSeason);
                        seasons.add(newRelationSeason);
                        seasonAdapter.notifyDataSetChanged();
                        season.setSelection(seasons.size() - 1);
                    })
                    .setNegativeButton("Отмена", null)
                    .create()
                    .show();
        });


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
            Relation seasonValue = (Relation) season.getSelectedItem();
            String cultureVarietiesValue = cultureVarieties.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
            String technologyValue = technology.getSelectedItem().toString();
            String landCategoryValue = landCategory.getSelectedItem().toString();
            String irrigationTypeValue = irrigationType.getSelectedItem().toString();

            try {
                if (culture.getSelectedItem() == null) throw new NullPointerException("Выращиваемая культура");
                String cultureValue = culture.getSelectedItem().toString();
                if (cultureValue.equals("Выращиваемая культура")) throw new NullPointerException("Выращиваемая культура");
                save(nameValue, regionValue, districtValue, aggregatorValue, farmerNameValue, farmerMobileValue,
                        cadastrNumberValue, seasonValue, cultureVarietiesValue, sowingDateValue, cleaningDateValue,
                        productivityValue, cultureValue, technologyValue, landCategoryValue, irrigationTypeValue,
                        farmerSurNameValue);
            } catch (NullPointerException exception) {
                Toast.makeText(getContext(),
                        String.format("Поле \"%s\" обязательно для заполнения", exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);

        if (getDialog() != null) {
            getDialog().setCancelable(false);
            getDialog().setCanceledOnTouchOutside(false);
        }
    }

    private void save(String name, String region, String district, String aggregator, String farmerName, String farmerMobile,
                      String cadastrNumber, Relation season, String cultureVarieties, String sowingDate,
                      String cleaningDate, String productivity, String culture, String technology, String landCategory,
                      String irrigationType, String farmerSurName) {
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
        yield.put("type", StorageDelegator.TYPE_FIELD);
        yield.put("technology", technology);

        Map<String, String> crop = new HashMap<>();
        crop.put("cultureVarieties", cultureVarieties);
        crop.put("sowingDate", sowingDate);
        crop.put("cleaningDate", cleaningDate);
        crop.put("productivity", productivity);
        crop.put("culture", culture);
        crop.put("landCategory", landCategory);
        crop.put("irrigationType", irrigationType);
        crop.put("type", StorageDelegator.TYPE_CROP);

        App.getDelegator().createNewYieldWithCrop(lastSelectedWay, yield, season, crop);
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

    private String getTagValue(OsmElement osmElement, String key) {
        if (key == null || osmElement == null) return "";
        String tagWithKey = osmElement.getTagWithKey(key);
        return tagWithKey == null ? "" : tagWithKey;
    }
}
