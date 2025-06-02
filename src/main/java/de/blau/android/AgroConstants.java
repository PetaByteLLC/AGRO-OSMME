package de.blau.android;

import de.blau.android.osm.Tags;

public class AgroConstants {

    public static final String URL = "https://agrotest.brisklyminds.com/agroadmin";
    // --- КОНСТАНТЫ ---
    public static final String TYPE_FIELD = "agromap_field"; // Тип для Relation Поля
    public static final String TYPE_SEASON = "agricultural_season";
    public static final String TYPE_CROP = "crop_planting";

    public static final String ROLE_FIELD_GEOMETRY = Tags.ROLE_OUTER; // Роль Way (Геометрии) в Relation Поля (стандартная)
    public static final String ROLE_FIELD = "field";         // Роль Relation Поля в Relation Сезона
    public static final String ROLE_SEASON = "season";

    public static final String YIELD_TAG_REGION = "region";
    public static final String YIELD_TAG_DISTRICT = "district";
    public static final String YIELD_TAG_AGGREGATOR = "aggregator";
    public static final String YIELD_TAG_FARMER_NAME = "farmerName";
    public static final String YIELD_TAG_FARMER_SURNAME = "farmerSurName";
    public static final String YIELD_TAG_FARMER_MOBILE = "farmerMobile";
    public static final String YIELD_TAG_CADASTRAL_NUMBER = "cadastrNumber";
    public static final String YIELD_TAG_POSITION = "position";
    public static final String YIELD_TAG_TECHNOLOGY = "technology";
    public static final String YIELD_TAG_ADDITIONAL_INFORMATION = "additionalInformation";
    public static final String YIELD_TAG_CATEGORY_TYPE = "categoryType";

    public static final String SEASON_TAG_START = "start";
    public static final String SEASON_TAG_END = "end";

    public static final String TAG_IMAGE = "image";

    public static final String CROP_TAG_CULTURE_VARIETIES = "cultureVarieties";
    public static final String CROP_TAG_SOWING_DATE = "sowingDate";
    public static final String CROP_TAG_CLEANING_DATE = "cleaningDate";
    public static final String CROP_TAG_PRODUCTIVITY = "productivity";
    public static final String CROP_TAG_CULTURE = "culture";
    public static final String CROP_TAG_LAND_CATEGORY = "landCategory";
    public static final String CROP_TAG_IRRIGATION_TYPE = "irrigationType";

    public static final String OTHER_CULTURE = "Несколько культур";
    public static final String[] CULTURE_DATA = {"Выращиваемая культура", "Пшеницa", "Зерновые", "Ячмень", "Кукуруза", "Хлопок-сырец", "Сахарная свекла"};
    public static final String[] TECHNOLOGY_DATA = {"Технология возделывания", "яровая", "озимая"};
    public static final String[] LAND_CATEGORY_DATA = {"Категория угодья", "орошаемая", "богара"};
    public static final String[] IRRIGATION_TYPE_DATA = {"Тип полива", "каналы/гравитация", "установки"};
    public static final String[] FIELD_TAG_CATEGORY_TYPE_DATA = {"Земли сельскохозяйственного назначения", "Земли населенных пунктов", "Земли промышленности, транспорта, связи, энергетик", "Земли особо охраняемых природных территорий", "Земли лесного фонда", "Земли водного фонда", "Земли запаса"};

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
}
