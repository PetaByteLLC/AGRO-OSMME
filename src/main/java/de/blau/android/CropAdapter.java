package de.blau.android;

import static de.blau.android.AgroConstants.CROP_TAG_CULTURE;
import static de.blau.android.BsEditCropFragment.getCrops;
import static de.blau.android.BsEditCropFragment.getSubData;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import de.blau.android.osm.Way;

public class CropAdapter extends RecyclerView.Adapter<CropAdapter.ViewHolder> {
    private Way way;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(String tagName);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(String tagName);
    }

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public CropAdapter(Context context, Way way,
                       OnItemClickListener clickListener,
                       OnItemLongClickListener longClickListener) {
        this.context = context;
        this.way = way;
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
        List<Map.Entry<String, String>> crops = getCrops(way);
        Map.Entry<String, String> data = crops.get(position);

        holder.culture.setText(getSubData(data.getValue(), CROP_TAG_CULTURE));
        if (holder.culture.getText() == null) {
            holder.culture.setText("Не указано");
        }

        String[] keySplit = data.getKey().split(":");
        if (keySplit.length > 1) {
            holder.season.setText("Сезон " + keySplit[1]);
        } else {
            holder.season.setText("Сезон не указан");
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(data.getKey());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(data.getKey());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return getCrops(way).size();
    }
}
