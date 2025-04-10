package de.blau.android;

import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.AgroConstants.CROP_TAG_CULTURE_VARIETIES;
import static de.blau.android.AgroConstants.REMOVE_FIELD_MESSAGE;
import static de.blau.android.TagHelper.getText;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> listGroup;
    private HashMap<String, List<Relation>> listChild;
    private Main main;
    private BottomSheetFragmentAllField bottomSheetFragmentAllField;

    public CustomExpandableListAdapter(Context context, List<String> listGroup, HashMap<String, List<Relation>> listChild, Main main, BottomSheetFragmentAllField bottomSheetFragmentAllField) {
        this.context = context;
        this.listGroup = listGroup;
        this.listChild = listChild;
        this.main = main;
        this.bottomSheetFragmentAllField = bottomSheetFragmentAllField;
    }

    @Override
    public int getGroupCount() {
        return listGroup.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return listChild.get(listGroup.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listGroup.get(groupPosition);
    }

    @Override
    public Relation getChild(int groupPosition, int childPosition) {
        return listChild.get(listGroup.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.bs_all_field_group, null);
        }

        TextView textView = convertView.findViewById(R.id.text1);
        textView.setText((String) getGroup(groupPosition));

        ImageView imageView = convertView.findViewById(R.id.image1);

        if (isExpanded) {
            imageView.setImageResource(R.drawable.baseline_arrow_drop_up_24);
        } else {
            imageView.setImageResource(R.drawable.baseline_arrow_drop_down_24);
        }

        return convertView;
    }

    private String getTagValue(OsmElement osmElement, String key) {
        if (key == null || osmElement == null) return "";
        String tagWithKey = osmElement.getTagWithKey(key);
        return tagWithKey == null ? "" : tagWithKey;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.bs_all_field_item, null);
        }
        Relation relation = getChild(groupPosition, childPosition);

        TextView title = convertView.findViewById(R.id.title);
        String text = getTagValue(relation, Tags.KEY_NAME) + " - " + getTagValue(relation, Tags.KEY_AREA) + " га";
        title.setText(text.trim());

        TextView cropTextView = convertView.findViewById(R.id.crops);

        List<Relation> seasons = relation.getParentRelations();
        if (seasons != null) {
            List<Relation> crops = new ArrayList<>();
            for (Relation season : seasons) {
                if (season == null) continue;
                if (season.getParentRelations() == null) continue;
                crops.addAll(season.getParentRelations());
            }

            String cropValues = getRelationArrayAdapter(crops);
            cropTextView.setText(cropValues);
        }

        ImageView viewOnMap = convertView.findViewById(R.id.viewOnMap);
        viewOnMap.setOnClickListener(v -> {
            BoundingBox bounds = relation.getBounds();
            if (bounds != null) {
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                dismiss();
            }
        });

        ImageView edit = convertView.findViewById(R.id.edit);
        edit.setOnClickListener(v -> {
            BoundingBox bounds = relation.getBounds();
            if (bounds != null) {
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                main.editor(relation);
                dismiss();
            }
        });

        ImageView remove = convertView.findViewById(R.id.remove);
        remove.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Вы уверены, что хотите удалить поле?")
                    .setMessage(REMOVE_FIELD_MESSAGE)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Удалить", (dialogInterface, which) -> {
                        App.getDelegator().removeFieldRelation(relation);
                        Toast.makeText(context, "Поле удалёно", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        return convertView;
    }

    @NonNull
    private String getRelationArrayAdapter(List<Relation> crops) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Relation crop : crops) {
            stringBuilder.append(getText(crop.getTagWithKey(CROP_TAG_CULTURE))).append("\t");
            stringBuilder.append(getText(crop.getTagWithKey(CROP_TAG_CULTURE_VARIETIES))).append("\n");
        }
        return stringBuilder.toString();
    }

    private void dismiss() {
        if (bottomSheetFragmentAllField != null && bottomSheetFragmentAllField.isVisible()) {
            bottomSheetFragmentAllField.dismiss();
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
