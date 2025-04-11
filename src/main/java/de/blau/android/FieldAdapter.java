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

import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;

public class FieldAdapter extends RecyclerView.Adapter<FieldAdapter.FieldViewHolder> {

    private final List<Relation> fieldList;
    private final OnFieldClickListener listener;

    public interface OnFieldClickListener {
        void remove(Relation relation);
        void editMetaData(Relation relation);
        void edit(Relation relation);
        void move(Relation relation);
    }

    public FieldAdapter(List<Relation> fieldList, OnFieldClickListener listener) {
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
        Relation relation = fieldList.get(position);
        String name = getTagValue(relation, Tags.KEY_NAME) + " - " + getTagValue(relation, Tags.KEY_AREA) + " га";
        holder.name.setText(name);

        if (listener != null) {
            holder.fieldBox.setOnClickListener(v -> {
                listener.editMetaData(relation);
            });

            holder.remove.setOnClickListener(v -> {
                listener.remove(relation);
            });

            holder.edit.setOnClickListener(v -> {
                listener.edit(relation);
            });

            holder.viewOnMap.setOnClickListener(v -> {
                listener.move(relation);
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
