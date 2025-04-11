package de.blau.android;

import static de.blau.android.AgroConstants.OTHER_CULTURE;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CultureAdapter extends RecyclerView.Adapter<CultureAdapter.CultureViewHolder> {

    private final List<Culture> cultureList;
    private final FieldAdapter.OnFieldClickListener listener;
    private FieldAdapter adapter;

    @SuppressLint("NotifyDataSetChanged")
    public void updateData() {
        notifyDataSetChanged();
        adapter.updateData();
    }

    public CultureAdapter(List<Culture> cultureList, FieldAdapter.OnFieldClickListener listener) {
        this.cultureList = cultureList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CultureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bs_all_field_culture, parent, false);
        return new CultureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CultureViewHolder holder, int position) {
        Culture culture = cultureList.get(position);
        holder.title.setText(culture.name);

        if (culture.name != null) {
            switch (culture.name) {
                case "Пшеницa":
                    holder.icon.setImageResource(R.drawable.lucide_wheat);
                    break;
                case "Зерновые":
                    holder.icon.setImageResource(R.drawable.fluent_food_grains);
                    break;
                case "Ячмень":
                    holder.icon.setImageResource(R.drawable.mdi_wheat);
                    break;
                case "Кукуруза":
                    holder.icon.setImageResource(R.drawable.mdi_corn);
                    break;
                case "Хлопок-сырец":
                    holder.icon.setImageResource(R.drawable.icons_cotton_flower);
                    break;
                case "Сахарная свекла":
                    holder.icon.setImageResource(R.drawable.sugar_beet);
                    break;
                case OTHER_CULTURE:
                    holder.icon.setImageResource(R.drawable.bug_light);
                    break;
                default:
                    holder.icon.setImageResource(R.drawable.no_image);
                    break;
            }
        }

        holder.fieldRecycler.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        adapter = new FieldAdapter(culture.yields, listener);
        holder.fieldRecycler.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return cultureList.size();
    }

    static class CultureViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        RecyclerView fieldRecycler;

        public CultureViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            fieldRecycler = itemView.findViewById(R.id.fieldRecycler);
        }
    }
}
