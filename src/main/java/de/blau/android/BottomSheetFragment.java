package de.blau.android;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static de.blau.android.AgroConstants.CROP_TAG_CLEANING_DATE;
import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.AgroConstants.CROP_TAG_CULTURE_VARIETIES;
import static de.blau.android.AgroConstants.CROP_TAG_IRRIGATION_TYPE;
import static de.blau.android.AgroConstants.CROP_TAG_LAND_CATEGORY;
import static de.blau.android.AgroConstants.CROP_TAG_PRODUCTIVITY;
import static de.blau.android.AgroConstants.CROP_TAG_SOWING_DATE;
import static de.blau.android.AgroConstants.CULTURE_DATA;
import static de.blau.android.AgroConstants.DATE_STRING_FORMAT;
import static de.blau.android.AgroConstants.IRRIGATION_TYPE_DATA;
import static de.blau.android.AgroConstants.LAND_CATEGORY_DATA;
import static de.blau.android.AgroConstants.ROLE_FARMER;
import static de.blau.android.AgroConstants.SEASON_TAG_END;
import static de.blau.android.AgroConstants.SEASON_TAG_START;
import static de.blau.android.AgroConstants.TAG_IMAGE;
import static de.blau.android.AgroConstants.TECHNOLOGY_DATA;
import static de.blau.android.AgroConstants.TYPE_CROP;
import static de.blau.android.AgroConstants.TYPE_FIELD;
import static de.blau.android.AgroConstants.TYPE_SEASON;
import static de.blau.android.AgroConstants.YIELD_TAG_AGGREGATOR;
import static de.blau.android.AgroConstants.YIELD_TAG_CADASTRAL_NUMBER;
import static de.blau.android.AgroConstants.YIELD_TAG_DISTRICT;
import static de.blau.android.AgroConstants.YIELD_TAG_FARMER_MOBILE;
import static de.blau.android.AgroConstants.YIELD_TAG_FARMER_NAME;
import static de.blau.android.AgroConstants.YIELD_TAG_FARMER_SURNAME;
import static de.blau.android.AgroConstants.YIELD_TAG_POSITION;
import static de.blau.android.AgroConstants.YIELD_TAG_REGION;
import static de.blau.android.AgroConstants.YIELD_TAG_TECHNOLOGY;
import static de.blau.android.DatePiker.createSeason;
import static de.blau.android.Main.REQUEST_IMAGE_CAPTURE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
            cultureVarieties, sowingDate, cleaningDate, productivity, farmerSurName, area;
    private Spinner culture, technology, landCategory, irrigationType, season;

    private ImageView addSeason;
    private Button btnUploadImage;

    private RecyclerView images;
    private List<File> imageFiles;
    private ImageAdapter imageAdapter;

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
        area = view.findViewById(R.id.area);

        imageFiles = new ArrayList<>();
        images = view.findViewById(R.id.images);
        images.setLayoutManager(new GridLayoutManager(getContext(), 2));
        images.setNestedScrollingEnabled(false);

        imageAdapter = new ImageAdapter(getContext(), imageFiles);
        images.setAdapter(imageAdapter);


        btnUploadImage = view.findViewById(R.id.btn_upload_image);
        btnUploadImage.setOnClickListener(v -> {
            if (getContext() == null) return;
            Intent startCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                String cameraApp = App.getPreferences(main).getCameraApp();
                if (!cameraApp.isEmpty()) {
                    startCamera.setPackage(cameraApp);
                }
                File imageFile = main.getImageFile();
                Uri photoUri = FileProvider.getUriForFile(getContext(), getString(R.string.content_provider), imageFile);
                if (photoUri != null) {
                    startCamera.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(startCamera, REQUEST_IMAGE_CAPTURE);
                }
                imageFiles.add(imageFile);
            } catch (Exception ignored) {

            }
        });

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

                        createSeason(newSeason);
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
            String areaValue = area.getText().toString();
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
                        farmerSurNameValue, areaValue);
            } catch (NullPointerException exception) {
                Toast.makeText(getContext(),
                        String.format("Поле \"%s\" обязательно для заполнения", exception.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        area.setText(getArea(lastSelectedWay));

        String userRole = main.getUserRole();
        Objects.requireNonNull(userRole);

        if (Objects.equals(userRole, ROLE_FARMER)) {
            farmerName.setVisibility(View.GONE);
            farmerSurName.setVisibility(View.GONE);
            farmerMobile.setVisibility(View.GONE);
        }

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
                      String irrigationType, String farmerSurName, String area) {
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
        yield.put(Tags.KEY_AREA, area);

        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (int i = 0; i < imageFiles.size(); i++) {
                yield.put(TAG_IMAGE + "_" + (i + 1), imageFiles.get(i).getAbsolutePath());
            }
        }

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

    public static double calculateArea(List<Node> nodes) {
        double area = 0.0;
        int n = nodes.size();

        for (int i = 0; i < n; i++) {
            double lat1 = Math.toRadians(nodes.get(i).getLat() / 10000000.0);
            double lon1 = Math.toRadians(nodes.get(i).getLon() / 10000000.0);
            double lat2 = Math.toRadians(nodes.get((i + 1) % n).getLat() / 10000000.0);
            double lon2 = Math.toRadians(nodes.get((i + 1) % n).getLon() / 10000000.0);

            area += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }

        area = area * 6378137 * 6378137 / 2.0;
        return Math.abs(area);
    }

    public static String getArea(Way way) {
        double areaSqMeters = calculateArea(way.getNodes());
        double areaHectares = areaSqMeters / 10000.0;
        return String.format(Locale.US, "%.3f", areaHectares);
    }

    private String formatCalendarToString(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Месяцы начинаются с 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(DATE_STRING_FORMAT, year, month, day);
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) imageAdapter.notifyDataSetChanged();
            if (resultCode == RESULT_CANCELED) {
                if (imageFiles != null && !imageFiles.isEmpty()) {
                    int index = imageFiles.size() - 1;
                    imageFiles.get(index).delete();
                    imageFiles.remove(index);
                }
            }
        }
//        else if (data != null) {
//            if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.SAVE_FILE) && resultCode == RESULT_OK) {
//                SelectFile.handleResult(getActivity(), requestCode, data);
//            }
//        }
    }
}
