package de.blau.android;

import static de.blau.android.AgroConstants.REMOVE_SEASON_MESSAGE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.util.List;

public class SeasonDialog {

    public interface OnSeasonSelectedListener {
        void onSeasonSelected(Season season);
    }

    @SuppressLint("MissingInflatedId")
    public static void show(Context context, OnSeasonSelectedListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_season_picker, null);
        ListView listView = view.findViewById(R.id.season_list);
        EditText input = view.findViewById(R.id.season_name);
        EditText start = view.findViewById(R.id.season_start);
        EditText end = view.findViewById(R.id.season_end);
        Button addButton = view.findViewById(R.id.add_button);

        DatePiker.setDataPicker(start, context);
        DatePiker.setDataPicker(end, context);

        List<Season> seasons = App.getPreferences(context).getSeasons();
        ArrayAdapter<Season> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                seasons
        );
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Выберите сезон")
                .setView(view)
                .setNegativeButton("Отмена", (dialogInterface, which) -> {
                    if (!seasons.isEmpty()) {
                        listener.onSeasonSelected(seasons.get(seasons.size() - 1));
                    } else {
                        listener.onSeasonSelected(null);
                    }
                })
                .create();

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            dialog.dismiss();
            listener.onSeasonSelected(seasons.get(position));
        });

        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            Season itemAtPosition = (Season) parent.getItemAtPosition(position);
            new AlertDialog.Builder(context)
                    .setTitle("Вы уверены, что хотите удалить сезон?")
                    .setMessage(REMOVE_SEASON_MESSAGE)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Удалить", (dialogInterface, which) -> {
                        seasons.remove(itemAtPosition);
                        App.getPreferences(context).saveSeasons(seasons);
                        adapter.notifyDataSetChanged();
                        App.getDelegator().removeSeasonsByName(itemAtPosition.getName());

                        Toast.makeText(context, "Сезон удалён", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

            return true;
        });

        addButton.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            String startDate = start.getText().toString().trim();
            String endDate = end.getText().toString().trim();
            if (!name.isEmpty()) {
                Season newSeason = new Season(startDate, name, endDate);
                seasons.add(newSeason);
                App.getPreferences(context).saveSeasons(seasons);
                adapter.notifyDataSetChanged();

                input.setText("");
                start.setText("");
                end.setText("");
            } else {
                Toast.makeText(context, "Заполните необходимые поля", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

}
