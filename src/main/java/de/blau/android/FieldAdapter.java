package de.blau.android;

import static de.blau.android.TagHelper.getTagValue;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.FieldViewHolder> {

    private final List<Way> fieldList;
    private final OnFieldClickListener listener;

    public interface OnFieldClickListener {
        void remove(Way way);
        void editMetaData(Way way);
        void edit(Way way);
        void move(Way way);
    }

    public FieldAdapter(List<Way> fieldList, OnFieldClickListener listener) {
        this.fieldList = fieldList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bs_all_field_yield, parent, false);
        return new FieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        Way way = fieldList.get(position);
        String name = getTagValue(way, Tags.KEY_NAME) + " - " + getTagValue(way, Tags.KEY_AREA) + " га";
        holder.name.setText(name);

        if (listener != null) {
            holder.fieldBox.setOnClickListener(v -> {
                listener.editMetaData(way);
            });

            holder.remove.setOnClickListener(v -> {
                listener.remove(way);
            });

            holder.edit.setOnClickListener(v -> {
                listener.edit(way);
            });

            holder.viewOnMap.setOnClickListener(v -> {
                listener.move(way);
            });
        }
    }

    @Override
    public int getItemCount() {
        return fieldList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData() {
        notifyDataSetChanged();
    }

    static class FieldViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        LinearLayout fieldBox;
        ImageView remove;
        ImageView edit;
        ImageView viewOnMap;

        public FieldViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.title);
            fieldBox = itemView.findViewById(R.id.fieldBox);
            edit = itemView.findViewById(R.id.edit);
            remove = itemView.findViewById(R.id.remove);
            viewOnMap = itemView.findViewById(R.id.viewOnMap);
        }
    }
}
