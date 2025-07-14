package de.blau.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.blau.android.osm.Way;

public class PastureAdapter extends RecyclerView.Adapter<PastureAdapter.ViewHolder> {
    private final Way way;
    private final Context context;

    public interface OnItemLongClickListener {
        void onItemLongClick(String tagName);
    }

    private final OnItemLongClickListener longClickListener;

    public PastureAdapter(Context context, Way way,
                          OnItemLongClickListener longClickListener) {
        this.context = context;
        this.way = way;
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
    public PastureAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.livestock_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<Map.Entry<String, String>> datas = getEntries();
        Map.Entry<String, String> data = datas.get(position);
        if (data == null) return;
        if (data.getKey() == null) return;
        String[] split = data.getKey().split(":");
        holder.culture.setText(split.length != 2 ? data.getKey() : split[1]);
        if (holder.culture.getText() == null) {
            holder.culture.setText("Не указано");
        }

        holder.season.setText(data.getValue());
        if (holder.season.getText() == null) {
            holder.season.setText("Не указано");
        }
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(data.getKey());
                return true;
            }
            return false;
        });
    }

    @NonNull
    private List<Map.Entry<String, String>> getEntries() {
        List<Map.Entry<String, String>> datas = new ArrayList<>();
        for (Map.Entry<String, String> l : way.getTags().entrySet()) {
            if (Objects.isNull(l)) continue;
            if (Objects.isNull(l.getKey())) continue;
            if (l.getKey().startsWith("pasture:")) datas.add(l);
        }
        return datas;
    }

    @Override
    public int getItemCount() {
        return getEntries().size();
    }
}
