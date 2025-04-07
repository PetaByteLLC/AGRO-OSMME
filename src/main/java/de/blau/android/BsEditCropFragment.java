package de.blau.android;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;

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

        String[] cultureData = {"Выращиваемая культура", "Пшеница", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
        ArrayAdapter<String> cultureAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, cultureData);
        cultureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        culture.setAdapter(cultureAdapter);

        String[] landCategoryData = {"Категория угодья", "орошаемая", "богара"};
        ArrayAdapter<String> landCategoryAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, landCategoryData);
        landCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landCategory.setAdapter(landCategoryAdapter);

        String[] irrigationTypeData = {"Тип полива", "каналы/гравитация", "установки"};
        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<String>(getActivity(), R.layout.agro_simple_spinner_item, irrigationTypeData);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);

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
                        seasonSelector.add(newSeason);
                        seasonAdapter.notifyDataSetChanged();
                        season.setSelection(seasonSelector.size() - 1);
                    })
                    .setNegativeButton("Отмена", null)
                    .create()
                    .show();
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

        setDataPicker(sowingDate);
        setDataPicker(cleaningDate);
        if (crop != null) {
            cultureVarieties.setText(getTagValue(crop, "cultureVarieties"));
            sowingDate.setText(getTagValue(crop, "sowingDate"));
            cleaningDate.setText(getTagValue(crop, "cleaningDate"));
            productivity.setText(getTagValue(crop, "productivity"));
            try {
                culture.setSelection(Arrays.asList(cultureData).indexOf(crop.getTagWithKey("culture")));
                landCategory.setSelection(Arrays.asList(landCategoryData).indexOf(crop.getTagWithKey("landCategory")));
                irrigationType.setSelection(Arrays.asList(irrigationTypeData).indexOf(crop.getTagWithKey("irrigationType")));
                OsmElement osmElement = crop.getMemberElements().get(0);
                Season selectedSeason = null;
                for (Season s : seasonSelector) {
                    if (Objects.equals(s.getName(), osmElement.getTagWithKey("name"))) {
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
                map.put("culture", cultureValue);
                map.put("cultureVarieties", cultureVarietiesValue);
                map.put("sowingDate", sowingDateValue);
                map.put("cleaningDate", cleaningDateValue);
                map.put("productivity", productivityValue);
                map.put("landCategory", landCategoryValue);
                map.put("irrigationType", irrigationTypeValue);
                map.put("type", StorageDelegator.TYPE_CROP);

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

    private String getTagValue(OsmElement osmElement, String key) {
        if (key == null || osmElement == null) return "";
        String tagWithKey = osmElement.getTagWithKey(key);
        return tagWithKey == null ? "" : tagWithKey;
    }

    private String formatCalendarToString(Calendar calendar) {
        // Преобразуем календарь в строку в формате "yyyy-MM-dd"
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Месяцы начинаются с 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Форматируем строку как "yyyy-MM-dd"
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private void setDataPicker(EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view1, year1, monthOfYear, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        editText.setText(date);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }
}
