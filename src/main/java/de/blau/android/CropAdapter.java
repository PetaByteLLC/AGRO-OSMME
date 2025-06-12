package de.blau.android;

import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.TagHelper.getTagValue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;

public class CropAdapter extends RecyclerView.Adapter<CropAdapter.ViewHolder> {
    private List<Relation> crops;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Relation relation);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Relation relation);
    }

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public CropAdapter(Context context, List<Relation> crops,
                       OnItemClickListener clickListener,
                       OnItemLongClickListener longClickListener) {
        this.context = context;
        this.crops = crops;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView culture;
        TextView season;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            culture = itemView.findViewById(R.id.culture);
            season = itemView.findViewById(R.id.season);
        }
    }

    @NonNull
    @Override
    public CropAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.crop_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Relation relation = crops.get(position);

        holder.culture.setText(getTagValue(relation, CROP_TAG_CULTURE));

        List<OsmElement> parentRelations = relation.getMemberElements();
        if (parentRelations != null && !parentRelations.isEmpty()) {
            Relation parentRelation = (Relation) parentRelations.get(0);
            holder.season.setText("Сезон " + getTagValue(parentRelation, Tags.KEY_NAME));
        } else {
            holder.season.setText("Сезон неизвестен");
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(relation);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(relation);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return crops.size();
    }

}
