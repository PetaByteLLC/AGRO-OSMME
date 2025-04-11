package de.blau.android;

import static de.blau.android.AgroConstants.DATE_FORMAT;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.util.Calendar;

public class DatePiker {

    @SuppressLint("DefaultLocale")
    public static void setDataPicker(EditText editText, Context context) {
        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (view1, year1, monthOfYear, dayOfMonth) -> {
                        editText.setText(String.format(DATE_FORMAT, year1, monthOfYear + 1, dayOfMonth));
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }

    public static String formatCalendarToString(Calendar calendar) {
        // Преобразуем календарь в строку в формате "yyyy-MM-dd"
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Месяцы начинаются с 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Форматируем строку как "yyyy-MM-dd"
        return String.format(DATE_FORMAT, year, month, day);
    }

    public static void createSeason(Season newSeason) {
        if (newSeason.getName().matches("^\\d{4}$") &&
                newSeason.getStartDate().isEmpty() &&
                newSeason.getEndDate().isEmpty()) {
            int yearValue = Integer.parseInt(newSeason.getName());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                newSeason.setStartDate(java.time.LocalDate.of(yearValue, 1, 1).toString());
                newSeason.setEndDate(java.time.LocalDate.of(yearValue, 12, 31).toString());
            } else {
                Calendar calendarStart = Calendar.getInstance();
                calendarStart.set(yearValue, Calendar.JANUARY, 1);
                Calendar calendarEnd = Calendar.getInstance();
                calendarEnd.set(yearValue, Calendar.DECEMBER, 31);
                newSeason.setStartDate(formatCalendarToString(calendarStart));
                newSeason.setEndDate(formatCalendarToString(calendarEnd));
            }
        }
    }
}
