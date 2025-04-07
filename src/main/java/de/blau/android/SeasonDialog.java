package de.blau.android;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.util.Calendar;
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

        setDataPicker(start, context);
        setDataPicker(end, context);

        List<Season> seasons = App.getPreferences(context).getSeasons();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                toStringList(seasons)
        );
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Выберите сезон")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .create();

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            dialog.dismiss();
            listener.onSeasonSelected(seasons.get(position));
        });

        addButton.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            String startDate = start.getText().toString().trim();
            String endDate = end.getText().toString().trim();
            if (!name.isEmpty()) {
                Season newSeason = new Season(startDate, name, endDate);
                seasons.add(newSeason);
                App.getPreferences(context).saveSeasons(seasons);
                adapter.clear();
                adapter.addAll(toStringList(seasons));
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

    private static List<String> toStringList(List<Season> seasons) {
        List<String> names = new java.util.ArrayList<>();
        for (Season s : seasons) {
            names.add(s.getName());
        }
        return names;
    }

    private static void setDataPicker(EditText editText, Context context) {
        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (view1, year1, monthOfYear, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        editText.setText(date);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }
}
