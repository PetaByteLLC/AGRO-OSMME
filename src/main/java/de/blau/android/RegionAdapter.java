package de.blau.android;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RegionAdapter extends RecyclerView.Adapter<RegionAdapter.RegionViewHolder> {

    private final List<Region> regionList;
    private final FieldAdapter.OnFieldClickListener listener;
    private CultureAdapter cultureAdapter;

    public RegionAdapter(List<Region> regionList, FieldAdapter.OnFieldClickListener listener) {
        this.regionList = regionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RegionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bs_all_field_region, parent, false);
        return new RegionViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData() {
        notifyDataSetChanged();
        cultureAdapter.updateData();
    }

    @Override
    public void onBindViewHolder(@NonNull RegionViewHolder holder, int position) {
        Region region = regionList.get(position);
        holder.title.setText(region.name);

        holder.cultureRecycler.setVisibility(region.isExpanded ? View.VISIBLE : View.GONE);

        holder.cultureRecycler.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        cultureAdapter = new CultureAdapter(region.cultures, listener);
        holder.cultureRecycler.setAdapter(cultureAdapter);

        holder.body.setOnClickListener(v -> {
            region.isExpanded = !region.isExpanded;
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return regionList.size();
    }

    static class RegionViewHolder extends RecyclerView.ViewHolder {
        LinearLayout body;
        TextView title;
        RecyclerView cultureRecycler;

        public RegionViewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.body);
            title = itemView.findViewById(R.id.text1);
            cultureRecycler = itemView.findViewById(R.id.cultures);
        }
    }
}
