package de.blau.android;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static de.blau.android.AgroConstants.*;
import static de.blau.android.BottomSheetFragment.getArea;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.util.LatLon;

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
    private EditText area;
    private EditText region;
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

    public BsEditYieldFragment(Relation yield, List<Relation> seasons, List<Relation> crops, Main main, boolean areEditTextsVisible) {
        this.yield = yield;
        this.seasons = seasons;
        this.crops = crops;
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

        List<RelationMember> membersWithRole = yield.getMembersWithRole(ROLE_FIELD_GEOMETRY);
        if (membersWithRole.isEmpty()) return;
        Way way = (Way) membersWithRole.get(0).getElement();

        imagePanel();

        yieldPanel();

        cropPanel();

        setArea(way);

        editLogic();

        saveBtnLogic();

        imageBtnLogic();

        roleCheck();

        coordinate(view, way);

        setRegionAndDistrict(way);

        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.setDraggable(false);
//
//        if (getDialog() != null) {
//            getDialog().setCancelable(false);
//            getDialog().setCanceledOnTouchOutside(false);
//        }
    }

    private void coordinate(@NonNull View view, Way way) {
        if (way == null) return;
        RecyclerView recyclerView = view.findViewById(R.id.coordinate_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        CoordinateAdapter adapter = new CoordinateAdapter(way.getNodes());
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
    }

    private void setArea(Way way) {
        String areaValue = getTagValue(yield, Tags.KEY_AREA);
        if (way == null) return;
        if (areaValue.isEmpty()) {
            area.setText(getArea(way));
        } else {
            String newArea = getArea(way);
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
            } catch (Exception ignored) {

            }
        });
    }

    private void saveBtnLogic() {
        saveBtn.setOnClickListener(v -> {

            if (Objects.equals(yield.getState(), OsmElement.STATE_CREATED)) {
                if (crops == null || crops.isEmpty()) {
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
            map.put(YIELD_TAG_AGGREGATOR, aggregator.getText().toString());
            map.put(YIELD_TAG_FARMER_NAME, farmerName.getText().toString());
            map.put(YIELD_TAG_FARMER_SURNAME, farmerSurName.getText().toString());
            map.put(YIELD_TAG_FARMER_MOBILE, farmerMobile.getText().toString());
            map.put(YIELD_TAG_CADASTRAL_NUMBER, cadastrNumber.getText().toString());
            map.put(YIELD_TAG_ADDITIONAL_INFORMATION, additionalInformation.getText().toString());

            if (urls != null && !urls.isEmpty()) {
                for (int i = 0; i < urls.size(); i++) {
                    map.put(TAG_IMAGE + "_" + (i + 1), urls.get(i));
                }
            }

            App.getDelegator().updateFieldRelationTags(yield, map);
            dismiss();
        });
    }

    private void cropPanel() {
        cropList.setLayoutManager(new LinearLayoutManager(getContext()));
        cropList.setNestedScrollingEnabled(false);

        cropAdapter = new CropAdapter(
                getContext(),
                crops,
                relation -> {
                    // onItemClick
                    BsEditCropFragment cropFragment = new BsEditCropFragment(relation, yield, seasons, main, false);
                    cropFragment.show(getChildFragmentManager(), cropFragment.getTag());
                },
                relation -> {
                    // onItemLongClick
                    new AlertDialog.Builder(getContext())
                            .setTitle("Вы уверены, что хотите удалить посев?")
                            .setMessage(REMOVE_CROP_MESSAGE)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Удалить", (dialogInterface, which) -> {
                                App.getDelegator().removeRelation(relation);
                                Toast.makeText(getContext(), "Посев удалён", Toast.LENGTH_SHORT).show();
                                crops.remove(relation);
                                updateCropList();
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                }
        );
        cropList.setAdapter(cropAdapter);

        cropAdd.setOnClickListener(v -> {
            BsEditCropFragment cropFragment = new BsEditCropFragment(null, yield, seasons, main, true);
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
        App.getLogic().setState(0);
        main.invisibleUnlockButton();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        if (getParentFragment() instanceof BottomSheetFragmentAllField) {
            ((BottomSheetFragmentAllField) getParentFragment()).dismiss();
        }

        if (Objects.equals(yield.getState(), OsmElement.STATE_CREATED)) {
            if (crops == null || crops.isEmpty()) {
                App.getDelegator().removeFieldRelation(yield);
                Toast.makeText(getContext(), "Поле удалёно", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setRegionAndDistrict(Way way) {
        if (way == null) return;
        BoundingBox bounds = way.getBounds();
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
