package de.blau.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.blau.android.osm.Node;

public class CoordinateAdapter extends RecyclerView.Adapter<CoordinateAdapter.ViewHolder> {

    private final List<Node> nodeList;

    public CoordinateAdapter(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView value;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            value = itemView.findViewById(R.id.value);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.coordinate_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Node node = nodeList.get(position);
        holder.title.setText("Точка " + (position + 1));
        holder.value.setText(node.getLat() + ", " + node.getLon());
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }
}