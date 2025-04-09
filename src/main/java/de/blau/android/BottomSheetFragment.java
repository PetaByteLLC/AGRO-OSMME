package de.blau.android;

import static de.blau.android.AgroConstants.*;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.util.Locale;
import java.util.Map;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.util.LatLon;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private final Way lastSelectedWay;
    private final Main main;
    private final List<Season> seasons;

    private EditText name, region, district, aggregator, farmerName, farmerMobile, cadastrNumber,
            cultureVarieties, sowingDate, cleaningDate, productivity, farmerSurName;
    private Spinner culture, technology, landCategory, irrigationType, season;

    private ImageView addSeason;

    public BottomSheetFragment(Way lastSelectedWay, Main main) {
        this.lastSelectedWay = lastSelectedWay;
        this.main = main;
        this.seasons = App.getPreferences(getContext()).getSeasons();
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

        DatePiker.setDataPicker(sowingDate, getContext());
        DatePiker.setDataPicker(cleaningDate, getContext());

        ArrayAdapter<Season> seasonAdapter = getSeasonArrayAdapter(seasons);
        season.setAdapter(seasonAdapter);

        if (main.currentSeason != null) {
            Season currentSeason = seasons.get(seasons.size() - 1);
            for (Season s : seasons) {
                if (main.currentSeason.equals(s)) {
                    currentSeason = s;
                    break;
                }
            }
            season.setSelection(seasons.indexOf(currentSeason));
        }

        addSeason.setOnClickListener(v -> {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.season_create_diaglog, null);
            final EditText name = dialogView.findViewById(R.id.name);
            final EditText start = dialogView.findViewById(R.id.start);
            final EditText end = dialogView.findViewById(R.id.end);

            DatePiker.setDataPicker(start, getContext());
            DatePiker.setDataPicker(end, getContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Создания сезона")
                    .setView(dialogView)
                    .setPositiveButton("Создать", (dialog, which) -> {
                        Season newSeason = new Season(name.getText().toString());
                        newSeason.setStartDate(start.getText().toString());
                        newSeason.setEndDate(end.getText().toString());

                        if (newSeason.getName().matches("^\\d{4}$") &&
                                newSeason.getStartDate().isEmpty() &&
                                newSeason.getEndDate().isEmpty()) {
                            int yearValue = Integer.parseInt(newSeason.getName());
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                newSeason.setStartDate(java.time.LocalDate.of(yearValue, 1, 1).toString());
                                newSeason.setEndDate(java.time.LocalDate.of(yearValue, 12, 31).toString());
                            } else {
                                Calendar calendarStart = Calendar.getInstance();
                                calendarStart.set(yearValue, Calendar.JANUARY, 1);
                                Calendar calendarEnd = Calendar.getInstance();
                                calendarEnd.set(yearValue, Calendar.DECEMBER, 31);
                                newSeason.setStartDate(formatCalendarToString(calendarStart));
                                newSeason.setEndDate(formatCalendarToString(calendarEnd));
                            }
                        }
                        seasons.add(newSeason);
                        seasonAdapter.notifyDataSetChanged();
                        season.setSelection(seasons.size() - 1);
                    })
                    .setNegativeButton("Отмена", null)
                    .create()
                    .show();
        });


        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, CULTURE_DATA);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        ArrayAdapter<String> technologyAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, TECHNOLOGY_DATA);
        technologyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        technology.setAdapter(technologyAdapter);

        ArrayAdapter<String> landCategoryAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, LAND_CATEGORY_DATA);
        landCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landCategory.setAdapter(landCategoryAdapter);

        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, IRRIGATION_TYPE_DATA);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);

        setRegionAndDistrict(lastSelectedWay);

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
            Season seasonValue = (Season) season.getSelectedItem();
            String cultureVarietiesValue = cultureVarieties.getText().toString();
            String sowingDateValue = sowingDate.getText().toString();
            String cleaningDateValue = cleaningDate.getText().toString();
            String productivityValue = productivity.getText().toString();
            String technologyValue = technology.getSelectedItem().toString();
            String landCategoryValue = landCategory.getSelectedItem().toString();
            String irrigationTypeValue = irrigationType.getSelectedItem().toString();

            try {
                if (culture.getSelectedItem() == null)
                    throw new NullPointerException("Выращиваемая культура");
                String cultureValue = culture.getSelectedItem().toString();
                if (cultureValue.equals("Выращиваемая культура"))
                    throw new NullPointerException("Выращиваемая культура");
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

//        if (getDialog() != null) {
//            getDialog().setCancelable(false);
//            getDialog().setCanceledOnTouchOutside(false);
//        }
    }

    private @NonNull ArrayAdapter<Season> getSeasonArrayAdapter(List<Season> seasons) {
        ArrayAdapter<Season> seasonAdapter = new ArrayAdapter<Season>(getActivity(), R.layout.season_dropdown_item, seasons) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.season_dropdown_item, parent, false);
                }
                Season season = getItem(position);
                TextView name = convertView.findViewById(R.id.text1);
                name.setText("Сезон " + TagHelper.getText(season.getName()));

                TextView date = convertView.findViewById(R.id.text2);
                date.setText(season.getStartDate() + " - " + season.getEndDate());
                return convertView;
            }
        };
        seasonAdapter.setDropDownViewResource(R.layout.agro_simple_spinner_item);
        return seasonAdapter;
    }

    private void save(String name, String region, String district, String aggregator, String farmerName, String farmerMobile,
                      String cadastrNumber, Season season, String cultureVarieties, String sowingDate,
                      String cleaningDate, String productivity, String culture, String technology, String landCategory,
                      String irrigationType, String farmerSurName) {
        Map<String, String> yield = new HashMap<>();
        yield.put(Tags.KEY_NAME, name);
        yield.put(YIELD_TAG_REGION, region);
        yield.put(YIELD_TAG_DISTRICT, district);
        yield.put(YIELD_TAG_AGGREGATOR, aggregator);
        yield.put(YIELD_TAG_FARMER_NAME, farmerName);
        yield.put(YIELD_TAG_FARMER_SURNAME, farmerSurName);
        yield.put(YIELD_TAG_FARMER_MOBILE, farmerMobile);
        yield.put(YIELD_TAG_CADASTRAL_NUMBER, cadastrNumber);
        yield.put(YIELD_TAG_POSITION, getPosition());
        yield.put(Tags.KEY_TYPE, TYPE_FIELD);
        yield.put(YIELD_TAG_TECHNOLOGY, technology);
        yield.put(Tags.KEY_AREA, getArea(lastSelectedWay));

        Map<String, String> seasonTags = new HashMap<>();
        seasonTags.put(Tags.KEY_NAME, season.getName());
        seasonTags.put(SEASON_TAG_START, season.getStartDate());
        seasonTags.put(SEASON_TAG_END, season.getEndDate());
        seasonTags.put(Tags.KEY_TYPE, TYPE_SEASON);

        Map<String, String> crop = new HashMap<>();
        crop.put(CROP_TAG_CULTURE_VARIETIES, cultureVarieties);
        crop.put(CROP_TAG_SOWING_DATE, sowingDate);
        crop.put(CROP_TAG_CLEANING_DATE, cleaningDate);
        crop.put(CROP_TAG_PRODUCTIVITY, productivity);
        crop.put(CROP_TAG_CULTURE, culture);
        crop.put(CROP_TAG_LAND_CATEGORY, landCategory);
        crop.put(CROP_TAG_IRRIGATION_TYPE, irrigationType);
        crop.put(Tags.KEY_TYPE, TYPE_CROP);

        App.getDelegator().createFieldRelationWithSeasonAndCrop(lastSelectedWay, yield, seasonTags, crop);
        dismiss();
    }

    private void setRegionAndDistrict(Way way) {
        BoundingBox bounds = way.getBounds();
        if (bounds.isValid()) {
            final ViewBox box = new ViewBox(bounds);
            double[] centerCoords = box.getCenter(); // Предполагаем, что это double градусы

            if (centerCoords.length >= 2) {
                // Предполагаем порядок [lat, lon] и что это double градусы
                double centerLat = centerCoords[1];
                double centerLon = centerCoords[0];
                // Создаем LatLon с double градусами
                LatLon location = new LatLon(centerLat, centerLon); // Убедитесь, что конструктор принимает double!
                ReferenceDataManager.ReferenceFeature matchingFeature = ReferenceDataManager.findFeatureContainingPoint(location);
                if (matchingFeature == null) return;
                region.setText(matchingFeature.getAdm1Ky());
                district.setText(matchingFeature.getAdm2Ky());
            }
        }
    }

    public static String getArea(Way way) {
        List<Node> nodes = way.getNodes();
        int n = nodes.size();

        // Проверка на валидный замкнутый полигон
        if (n < 4 || !nodes.get(0).equals(nodes.get(n - 1))) {
            return ""; // Не полигон или не замкнут
        }

        // Массивы для координат в метрах (относительно первой точки)
        double[] x_meters = new double[n];
        double[] y_meters = new double[n];
        x_meters[0] = 0.0;
        y_meters[0] = 0.0;

        // Константы для перевода градусов в метры
        final double METERS_PER_DEGREE_LAT = 111132.954;
        final double METERS_PER_DEGREE_LON_BASE = 111319.488; // На экваторе

        // Рассчитываем координаты в метрах для каждой вершины относительно первой
        for (int i = 0; i < n - 1; i++) {
            Node p1 = nodes.get(i);
            Node p2 = nodes.get(i + 1);

            double deltaLat = p2.getLat() - p1.getLat();
            double deltaLon = p2.getLon() - p1.getLon();

            // Используем среднюю широту сегмента для расчета длины градуса долготы
            double avgLatSegmentRad = Math.toRadians((p1.getLat() + p2.getLat()) / 2.0);

            double deltaY_meters = deltaLat * METERS_PER_DEGREE_LAT;
            double deltaX_meters = deltaLon * METERS_PER_DEGREE_LON_BASE * Math.cos(avgLatSegmentRad);

            // Координаты следующей точки = коорд. предыдущей + дельта
            x_meters[i + 1] = x_meters[i] + deltaX_meters;
            y_meters[i + 1] = y_meters[i] + deltaY_meters;
        }

        // Рассчитываем площадь по формуле Гаусса (Shoelace) для координат в метрах
        double areaSqMeters = 0.0;
        for (int i = 0; i < n - 1; i++) {
            areaSqMeters += (x_meters[i] * y_meters[i + 1]) - (x_meters[i + 1] * y_meters[i]);
        }
        areaSqMeters = Math.abs(areaSqMeters / 2.0);

        // Конвертируем в гектары
        double areaHaApprox = areaSqMeters / 10000.0;

        // Возвращаем отформатированную строку
        // Используем Locale.US для точки в качестве десятичного разделителя
        return String.format(Locale.US, "%.3f", areaHaApprox);
    }

    private String formatCalendarToString(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Месяцы начинаются с 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(DATE_FORMAT, year, month, day);
    }

    private String getPosition() {
        de.blau.android.Map logicMap = App.getLogic().getMap();
        if (logicMap == null) return "";
        if (logicMap.getTracker() == null) return "";
        Location lastLocation = logicMap.getTracker().getLastLocation();
        if (lastLocation == null) return "";
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

        if (lastSelectedWay.getParentRelations() == null || lastSelectedWay.getParentRelations().isEmpty()) {
            if (lastSelectedWay.getState() == OsmElement.STATE_CREATED) {
                App.getDelegator().removeWay(lastSelectedWay);
                for (Node node : lastSelectedWay.getNodes()) {
                    App.getDelegator().removeNode(node);
                }
            }
        }

        App.getPreferences(getContext()).saveSeasons(seasons);
    }

}
