package de.blau.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import de.blau.android.contract.Ui;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> listGroup;
    private HashMap<String, List<Way>> listChild;
    private Main main;
    private BottomSheetFragmentAllField bottomSheetFragmentAllField;

    public CustomExpandableListAdapter(Context context, List<String> listGroup, HashMap<String, List<Way>> listChild, Main main, BottomSheetFragmentAllField bottomSheetFragmentAllField) {
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
    public Way getChild(int groupPosition, int childPosition) {
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

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.bs_all_field_item, null);
        }
        Way way = getChild(groupPosition, childPosition);

        TextView title = convertView.findViewById(R.id.title);
        title.setText(way.getTagWithKey("name") + " - " + way.getTagWithKey("area") + " " + way.getTagWithKey("areaUnit"));

        String cultureData = way.getTagWithKey("culture");
        TextView culture = convertView.findViewById(R.id.culture);
        culture.setText(cultureData);

        ImageView cultureIcon = convertView.findViewById(R.id.cultureIcon);
        if (cultureData != null) {
            switch (cultureData) {
                case "Пшеницa":
                    cultureIcon.setImageResource(R.drawable.lucide_wheat);
                    break;
                case "Зерновые":
                    cultureIcon.setImageResource(R.drawable.fluent_food_grains);
                    break;
                case "Ячмень":
                    cultureIcon.setImageResource(R.drawable.mdi_wheat);
                    break;
                case "Кукуруза":
                    cultureIcon.setImageResource(R.drawable.mdi_corn);
                    break;
                case "Хлопок-сырец":
                    cultureIcon.setImageResource(R.drawable.icons_cotton_flower);
                    break;
                case "Сахарная свекла":
                    cultureIcon.setImageResource(R.drawable.sugar_beet);
                    break;
            }
        } else {
            cultureIcon.setImageResource(R.drawable.lucide_wheat);
        }

        TextView variety = convertView.findViewById(R.id.variety);
        variety.setText(way.getTagWithKey("variety"));

        ImageView viewOnMap = convertView.findViewById(R.id.viewOnMap);
        viewOnMap.setOnClickListener(v -> {
            BoundingBox bounds = way.getBounds();
            if (bounds != null) {
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
//                main.zoomToAndEdit((int) (center[0] * 1E7D), (int) (center[1] * 1E7D), way);
                App.getLogic().setZoom(main.getMap(), Ui.ZOOM_FOR_ZOOMTO);
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                dismiss();
            }
        });

        ImageView edit = convertView.findViewById(R.id.edit);
        edit.setOnClickListener(v -> {
            BoundingBox bounds = way.getBounds();
            if (bounds != null) {
                final ViewBox box = new ViewBox(bounds);
                double[] center = box.getCenter();
                main.invalidateMap();
//                main.zoomToAndEdit((int) (center[0] * 1E7D), (int) (center[1] * 1E7D), way);
                App.getLogic().setZoom(main.getMap(), Ui.ZOOM_FOR_ZOOMTO);
                main.getMap().getViewBox().moveTo(main.getMap(), (int) (center[0] * 1E7D), (int) (center[1] * 1E7D));
                main.editor(way);
                dismiss();
            }
        });

        return convertView;
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
