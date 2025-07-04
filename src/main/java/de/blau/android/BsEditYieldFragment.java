package de.blau.android;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static de.blau.android.AgroConstants.*;
import static de.blau.android.BsEditCropFragment.getCrops;
import static de.blau.android.Main.REQUEST_IMAGE_CAPTURE;
import static de.blau.android.TagHelper.getTagValue;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.util.LatLon;

public class BsEditYieldFragment extends BottomSheetDialogFragment {

    private final Way yield;
    private final Main main;

    private TextView label;

    private Button toggleButton;
    private Button saveBtn;
    private LinearLayout editTextContainer;

    private EditText name;
    private EditText area;
    private EditText region;
    private Spinner landType;
    private Spinner underLandType;
    private Spinner irrigationType;
    private EditText district;
    private EditText farmerSurName;
    private EditText farmerName;
    private EditText farmerMobile;
    private EditText cadastrNumber;
    private EditText aggregator;
    private EditText additionalInformation;

    private boolean areEditTextsVisible;

    private RecyclerView cropList;
    private Button cropAdd;
    private CropAdapter cropAdapter;

    private RecyclerView images;
    private Button btnUploadImage;
    private ImageStringAdapter imageStringAdapter;
    private List<String> urls;

    public BsEditYieldFragment(Way yield, Main main, boolean areEditTextsVisible) {
        this.yield = yield;
        this.main = main;
        this.areEditTextsVisible = !areEditTextsVisible;
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
        landType = view.findViewById(R.id.landType);
        underLandType = view.findViewById(R.id.underLandType);
        irrigationType = view.findViewById(R.id.irrigationType);
        farmerSurName = view.findViewById(R.id.farmerSurName);
        farmerName = view.findViewById(R.id.farmerName);
        farmerMobile = view.findViewById(R.id.farmerMobile);
        cadastrNumber = view.findViewById(R.id.cadastrId);
        aggregator = view.findViewById(R.id.aggregator);
        additionalInformation = view.findViewById(R.id.additionalInformation);
        cropList = view.findViewById(R.id.crop_list);
        cropAdd = view.findViewById(R.id.crop_add);
        area = view.findViewById(R.id.area);
        images = view.findViewById(R.id.images);
        btnUploadImage = view.findViewById(R.id.btn_upload_image);

        imagePanel();

        yieldPanel();

        cropPanel();

        setArea();

        editLogic();

        saveBtnLogic();

        imageBtnLogic();

        roleCheck();

        coordinate(view);

        setRegionAndDistrict();

        setSpinnerData();

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);
//
//        if (getDialog() != null) {
//            getDialog().setCancelable(false);
//            getDialog().setCanceledOnTouchOutside(false);
//        }
    }

    private void setSpinnerData() {
        ArrayAdapter<String> landTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, TYPE_LAND_DATA);
        landTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        landType.setAdapter(landTypeAdapter);

        landType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String typeLand = TYPE_LAND_DATA[position];
                String[] strings = UNDER_TYPE_LAND_DATA.get(typeLand);
                if (strings != null) {
                    ArrayAdapter<String> underLandTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, strings);
                    underLandTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    underLandType.setAdapter(underLandTypeAdapter);
                    underLandType.setVisibility(View.VISIBLE);
                } else {
                    underLandType.setAdapter(null);
                    underLandType.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> irrigationTypeAdapter = new ArrayAdapter<>(getActivity(), R.layout.agro_simple_spinner_item, IRRIGATION_TYPE_DATA);
        irrigationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        irrigationType.setAdapter(irrigationTypeAdapter);
    }

    private void coordinate(@NonNull View view) {
        if (yield == null) return;
        RecyclerView recyclerView = view.findViewById(R.id.coordinate_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        CoordinateAdapter adapter = new CoordinateAdapter(yield.getNodes());
        recyclerView.setAdapter(adapter);
    }

    private void imagePanel() {
        urls = new ArrayList<>();
        images.setLayoutManager(new GridLayoutManager(getContext(), 2));
        images.setNestedScrollingEnabled(false);

        SortedMap<String, String> tags = yield.getTags();
        for (Map.Entry<String, String> rec : tags.entrySet()) {
            if (rec == null) continue;
            if (rec.getValue() == null) continue;
            if (rec.getKey() == null) continue;
            if (rec.getKey().startsWith(TAG_IMAGE)) {
                urls.add(rec.getValue());
            }
        }
        imageStringAdapter = new ImageStringAdapter(getContext(), urls);
        images.setAdapter(imageStringAdapter);
    }

    private void editLogic() {
        name.setText(getTagValue(yield, Tags.KEY_NAME));
        region.setText(getTagValue(yield, YIELD_TAG_REGION));
        district.setText(getTagValue(yield, YIELD_TAG_DISTRICT));
        aggregator.setText(getTagValue(yield, YIELD_TAG_AGGREGATOR));
        farmerName.setText(getTagValue(yield, YIELD_TAG_FARMER_NAME));
        farmerSurName.setText(getTagValue(yield, YIELD_TAG_FARMER_SURNAME));
        farmerMobile.setText(getTagValue(yield, YIELD_TAG_FARMER_MOBILE));
        cadastrNumber.setText(getTagValue(yield, YIELD_TAG_CADASTRAL_NUMBER));
        additionalInformation.setText(getTagValue(yield, YIELD_TAG_ADDITIONAL_INFORMATION));
        landType.setSelection(Arrays.asList(TYPE_LAND_DATA).indexOf(getTagValue(yield, YIELD_TAG_TYPE_LAND)));
        underLandType.setSelection(Arrays.asList(UNDER_TYPE_LAND_DATA).indexOf(getTagValue(yield, YIELD_TAG_UNDER_TYPE_LAND)));
        irrigationType.setSelection(Arrays.asList(IRRIGATION_TYPE_DATA).indexOf(getTagValue(yield, YIELD_TAG_IRRIGATION_TYPE)));
    }

    private void setArea() {
        String areaValue = getTagValue(yield, Tags.KEY_AREA);
        if (yield == null) return;
        if (areaValue.isEmpty()) {
            area.setText(getArea(yield));
        } else {
            String newArea = getArea(yield);
            double oldVal = Double.parseDouble(areaValue);
            double newVal = Double.parseDouble(newArea);
            area.setText(oldVal == newVal ? getTagValue(yield, Tags.KEY_AREA) : newArea);
        }
    }

    private void roleCheck() {
        String userRole = main.getUserRole();
        Objects.requireNonNull(userRole);

        if (Objects.equals(userRole, ROLE_FARMER)) {
            farmerName.setVisibility(View.GONE);
            farmerSurName.setVisibility(View.GONE);
            farmerMobile.setVisibility(View.GONE);
        }
    }

    private void imageBtnLogic() {
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
                urls.add(imageFile.getAbsolutePath());
            } catch (Exception ignored) {}
        });
    }

    private void saveBtnLogic() {
        saveBtn.setOnClickListener(v -> {

            if (Objects.equals(yield.getState(), OsmElement.STATE_CREATED)) {
                if (getCrops(yield).isEmpty()) {
                    Toast.makeText(getContext(), "Добавьте хотя бы один элемент севооборота.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Map<String, String> map = new HashMap<>();
            map.put(YIELD_TAG_POSITION, getPosition());
            map.put(Tags.KEY_NAME, name.getText().toString());
            map.put(Tags.KEY_AREA, area.getText().toString());
            map.put(YIELD_TAG_REGION, region.getText().toString());
            map.put(YIELD_TAG_DISTRICT, district.getText().toString());
            map.put(YIELD_TAG_TYPE_LAND, landType.getSelectedItem() != null ? landType.getSelectedItem().toString() : "");
            map.put(YIELD_TAG_UNDER_TYPE_LAND, underLandType.getSelectedItem() != null ? underLandType.getSelectedItem().toString() : "");
            map.put(YIELD_TAG_IRRIGATION_TYPE, irrigationType.getSelectedItem() != null ? irrigationType.getSelectedItem().toString() : "");
            map.put(YIELD_TAG_AGGREGATOR, aggregator.getText().toString());
            map.put(YIELD_TAG_FARMER_NAME, farmerName.getText().toString());
            map.put(YIELD_TAG_FARMER_SURNAME, farmerSurName.getText().toString());
            map.put(YIELD_TAG_FARMER_MOBILE, farmerMobile.getText().toString());
            map.put(YIELD_TAG_CADASTRAL_NUMBER, cadastrNumber.getText().toString());
            map.put(YIELD_TAG_ADDITIONAL_INFORMATION, additionalInformation.getText().toString());

            int selectedItemPosition = landType.getSelectedItemPosition();
            if (Objects.equals(selectedItemPosition, 2) || Objects.equals(selectedItemPosition, 3)) {
                map.put("landuse", "meadow");
            } else {
                map.put("landuse", "farmland");
            }

            if (urls != null && !urls.isEmpty()) {
                for (int i = 0; i < urls.size(); i++) {
                    map.put(TAG_IMAGE + "_" + (i + 1), urls.get(i));
                }
            }

            App.getDelegator().updateOsmElementTags(yield, map);
            dismiss();
        });
    }

    private void cropPanel() {
        cropList.setLayoutManager(new LinearLayoutManager(getContext()));
        cropList.setNestedScrollingEnabled(false);

        cropAdapter = new CropAdapter(
                getContext(),
                yield,
                key -> {
                    // onItemClick
                    BsEditCropFragment cropFragment = new BsEditCropFragment(key, yield, main);
                    cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
                },
                key -> {
                    // onItemLongClick
                    new AlertDialog.Builder(getContext())
                            .setTitle("Вы уверены, что хотите удалить посев?")
                            .setMessage(REMOVE_CROP_MESSAGE)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Удалить", (dialogInterface, which) -> {
                                App.getDelegator().deleteTag(key, yield);
                                Toast.makeText(getContext(), "Посев удалён", Toast.LENGTH_SHORT).show();
                                updateCropList();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                }
        );
        cropList.setAdapter(cropAdapter);

        cropAdd.setOnClickListener(v -> {
            BsEditCropFragment cropFragment = new BsEditCropFragment(null, yield, main);
            cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
        });
    }

    private void yieldPanel() {
        if (!areEditTextsVisible) {
            label.setText("Создание поля");
        } else {
            label.setText("Редактирование поля");
        }
        toggleButton.setOnClickListener(v -> showHidePanel());
        showHidePanel();
    }

    private void showHidePanel() {
        if (areEditTextsVisible) {
            collapseEditTexts();
        } else {
            expandEditTexts();
        }
    }

    public void updateCropList() {
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
        App.getLogic().setState(0);
        main.invisibleUnlockButton();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        if (getParentFragment() instanceof BottomSheetFragmentAllField) {
            ((BottomSheetFragmentAllField) getParentFragment()).dismiss();
        }

        if (Objects.equals(yield.getState(), OsmElement.STATE_CREATED)) {
            if (getCrops(yield).isEmpty()) {
                App.getDelegator().removeWay(yield);
                Toast.makeText(getContext(), "Поле удалёно", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setRegionAndDistrict() {
        if (yield == null) return;
        BoundingBox bounds = yield.getBounds();
        if (bounds.isValid()) {
            final ViewBox box = new ViewBox(bounds);
            double[] centerCoords = box.getCenter();
            if (centerCoords.length >= 2) {
                double centerLat = centerCoords[1];
                double centerLon = centerCoords[0];
                LatLon location = new LatLon(centerLat, centerLon);
                ReferenceDataManager.ReferenceFeature matchingFeature = ReferenceDataManager.findFeatureContainingPoint(location);
                if (matchingFeature == null) return;
                region.setText(matchingFeature.getAdm1Ky());
                district.setText(matchingFeature.getAdm2Ky());
            }
        }
    }

    private String getPosition() {
        de.blau.android.Map logicMap = App.getLogic().getMap();
        if (logicMap == null) return "";
        if (logicMap.getTracker() == null) return "";
        Location lastLocation = logicMap.getTracker().getLastLocation();
        if (lastLocation == null) return "";
        return String.format("%s, %s;", lastLocation.getLatitude(), lastLocation.getLongitude());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) imageStringAdapter.notifyDataSetChanged();
            if (resultCode == RESULT_CANCELED) {
                if (urls != null && !urls.isEmpty()) {
                    int index = urls.size() - 1;
                    new File(urls.get(index)).delete();
                    urls.remove(index);
                }
            }
        }
    }
}
