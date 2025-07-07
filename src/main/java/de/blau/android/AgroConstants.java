package de.blau.android;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.blau.android.osm.Node;
import de.blau.android.osm.Way;

public class AgroConstants {

    public static final String BASE_URL = "https://agrotest.brisklyminds.com";
    public static final String URL = BASE_URL + "/agroadmin";
    public static final String EXPORT_URL = URL + "/ws/fields/export/geojson";
    // --- КОНСТАНТЫ ---

    public static final String YIELD_TAG_REGION = "region";
    public static final String YIELD_TAG_DISTRICT = "district";
    public static final String YIELD_TAG_AGGREGATOR = "aggregator";
    public static final String YIELD_TAG_FARMER_NAME = "farmerName";
    public static final String YIELD_TAG_FARMER_SURNAME = "farmerSurName";
    public static final String YIELD_TAG_FARMER_MOBILE = "farmerMobile";
    public static final String YIELD_TAG_CADASTRAL_NUMBER = "cadastrNumber";
    public static final String YIELD_TAG_POSITION = "position";
    public static final String YIELD_TAG_IRRIGATION_TYPE = "irrigationType";
    public static final String YIELD_TAG_UNDER_TYPE_LAND = "underTypeLand";
    public static final String YIELD_TAG_TYPE_LAND = "typeLand";
    public static final String YIELD_TAG_ADDITIONAL_INFORMATION = "additionalInformation";

    public static final String TAG_IMAGE = "image";

    public static final String CROP_TAG_CULTURE = "culture";
    public static final String CROP_TAG_TECHNOLOGY = "technology";
    public static final String CROP_TAG_CULTURE_VARIETIES = "variety";
    public static final String CROP_TAG_SOWING_DATE = "sowingDate";
    public static final String CROP_TAG_CLEANING_DATE = "cleaningDate";
    public static final String CROP_TAG_PRODUCTIVITY = "productivity";

    public static final String CROP_TAG_NAME = "crop_plan";

    public static final String OTHER_CULTURE = "Несколько культур";
    public static final String[] CULTURE_DATA = {"Выращиваемая культура", "Пшеницa", "Зерновые", "Ячмень", "Кукуруза", "Кукуруза на зерно", "Кукуруза на силос", "Кукуруза пачаткой", "Хлопок-сырец", "Сахарная свекла", "Свекла", "Люцерна", "Фасоль", "Клубника", "Картофель", "Рис", "Эспарцет", "Соя", "Малина", "Ежемалина", "Черешня", "Смородина", "Тыква", "Яблоко", "Арбуз", "Лук", "Пара", "Чеснок", "Сафлор", "Подсолнух", "Клевер", "Морковь", "Ежевика", "Валериана", "Рожь", "Рапс", "Овес", "Ячмень с подсевом люцерны", "Сад", "Помидор"};
    public static final String[] TECHNOLOGY_DATA = {"Технология возделывания", "Яровая", "Озимая"};
    public static final String[] TYPE_LAND_DATA = {"Вид угодия", "Пашня", "Сенокосы", "Пастбища", "Залежи", "Многолетние насаждения"};
    public static final String[] IRRIGATION_TYPE_DATA = {"Тип полива", "не поливается", "Арычный", "Капельный", "Насосы", "Каналы", "Природный полив", "Дождевые машины", "Дождевальные установки", "Комбинированные системы", "Системы умного полива"};
    public static final String[] FIELD_TAG_CATEGORY_TYPE_DATA = {"Земли сельскохозяйственного назначения", "Земли населенных пунктов", "Земли промышленности, транспорта, связи, энергетик", "Земли особо охраняемых природных территорий", "Земли лесного фонда", "Земли водного фонда", "Земли запаса"};

    public static final Map<String, String[]> UNDER_TYPE_LAND_DATA = Map.of(
            "Пашня", new String[]{"Богара", "Условно богара", "Орошаемая", "Условно орошаемая"},
            "Пастбища", new String[]{"Зимние пастбища", "Летние пастбища", "Весенние и осенние пастбища"},
            "Многолетние насаждения", new String[]{"Сады", "Виноградники"}
    );

    public static final String DATE_STRING_FORMAT = "%04d-%02d-%02d";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String REMOVE_SEASON_MESSAGE = "После удаления будут также удалены все связанные с ним данные, включая поля, участки, зоны и другие объекты, которые были частью этого сезона." +
            "\nЭто действие необратимо — восстановить данные после удаления будет невозможно." +
            "\nВы уверены, что хотите продолжить?";
    public static final String REMOVE_FIELD_MESSAGE = "После удаления будут также удалены все связанные с ним данные, включая поля, участки, зоны и другие объекты, которые были частью этого поле." +
            "\nЭто действие необратимо — восстановить данные после удаления будет невозможно." +
            "\nВы уверены, что хотите продолжить?";
    public static final String REMOVE_CROP_MESSAGE = "После удаления будут также удалены все связанные с ним данные, включая поля, участки, зоны и другие объекты, которые были частью этого посев." +
            "\nЭто действие необратимо — восстановить данные после удаления будет невозможно." +
            "\nВы уверены, что хотите продолжить?";

    public static final String ROLE_ADMIN = "admins";
    public static final String ROLE_BANK = "bank";
    public static final String ROLE_FARMER = "farmer";
    public static final String ROLE_GAZR = "gazr";
    public static final String ROLE_MINISTRY = "ministry";
    public static final String ROLE_SCOUT = "scout";
    public static final String ROLE_ZEM_BALANCE = "zemBalance";

    public static double calculateArea(List<Node> nodes) {
        double area = 0.0;
        int n = nodes.size();

        for (int i = 0; i < n; i++) {
            double lat1 = Math.toRadians(nodes.get(i).getLat() / 10000000.0);
            double lon1 = Math.toRadians(nodes.get(i).getLon() / 10000000.0);
            double lat2 = Math.toRadians(nodes.get((i + 1) % n).getLat() / 10000000.0);
            double lon2 = Math.toRadians(nodes.get((i + 1) % n).getLon() / 10000000.0);

            area += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }

        area = area * 6378137 * 6378137 / 2.0;
        return Math.abs(area);
    }

    public static String getArea(Way way) {
        double areaSqMeters = calculateArea(way.getNodes());
        double areaHectares = areaSqMeters / 10000.0;
        return String.format(Locale.US, "%.3f", areaHectares);
    }
}
