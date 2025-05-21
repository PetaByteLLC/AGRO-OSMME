package de.blau.android;

import static de.blau.android.AgroConstants.DATE_STRING_FORMAT;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

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
                        editText.setText(String.format(DATE_STRING_FORMAT, year1, monthOfYear + 1, dayOfMonth));
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
        return String.format(DATE_STRING_FORMAT, year, month, day);
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

    /**
     * Парсит строку в объект java.util.Date, используя указанный формат и локаль.
     * Локаль важна для форматов, содержащих названия месяцев или дней недели.
     *
     * @param dateString Строка, представляющая дату.
     * @param formatPattern Паттерн формата даты (например, "dd MMMM yyyy").
     * @return Объект Date в случае успеха, или null, если парсинг не удался.
     */
    public static Date parseStringToDate(String dateString, String formatPattern) throws ParseException {
        Objects.requireNonNull(dateString);
        Objects.requireNonNull(formatPattern);

        SimpleDateFormat formatter = new SimpleDateFormat(formatPattern, Locale.ENGLISH);
        formatter.setLenient(false);

        return formatter.parse(dateString);
    }
}
