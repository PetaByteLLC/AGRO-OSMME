package de.blau.android;

import static de.blau.android.Main.YEARS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SeasonDialog {

    public interface OnSeasonSelectedListener {
        void onSeasonSelected(String season);
    }

    @SuppressLint("MissingInflatedId")
    public static void show(Context context, OnSeasonSelectedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_season_picker, null);
        ListView listView = view.findViewById(R.id.season_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                YEARS
        );
        listView.setAdapter(adapter);
        listView.setSelection(YEARS.size() - 1);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Выберите сезон")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .create();

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            dialog.dismiss();
            listener.onSeasonSelected(YEARS.get(position));
        });

        dialog.show();
    }

}
