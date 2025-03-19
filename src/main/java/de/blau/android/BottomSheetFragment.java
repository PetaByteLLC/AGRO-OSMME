package de.blau.android;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import java.util.HashMap;
import java.util.Map;

import de.blau.android.osm.Node;
import de.blau.android.osm.Way;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    private final Way lastSelectedWay;
    private final Main main;
    private boolean edit;

    private EditText field1, field3;
    private Spinner field2;

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

        field1 = view.findViewById(R.id.field1);
        field2 = view.findViewById(R.id.field2);
        field3 = view.findViewById(R.id.field3);

        String[] data = {"Культура *", "Пшеницa", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        field2.setAdapter(spinnerAdapter);

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String field1Value = field1.getText().toString();
            String field2Value = field2.getSelectedItem() == null ? "" : field2.getSelectedItem().toString();
            String field3Value = field3.getText().toString();
            save(field1Value, field2Value, field3Value);
        });

        if (edit) {
            field1.setText(lastSelectedWay.getTagWithKey("name"));
            field3.setText(lastSelectedWay.getTagWithKey("variety"));
            try {
                field2.setSelection(Arrays.asList(data).indexOf(lastSelectedWay.getTagWithKey("culture")));
            } catch (Exception ignore) {
            }
        }
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void save(String name, String culture, String variety) {
        Map<String, String> tags = new HashMap<>();
        tags.put("landuse", "farmland");
        tags.put("position", getPosition());
        tags.put("name", name);
        tags.put("culture", culture);
        tags.put("variety", variety);
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
}
