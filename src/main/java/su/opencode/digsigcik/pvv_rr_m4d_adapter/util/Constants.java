package su.opencode.digsigcik.pvv_rr_m4d_adapter.util;

/**
 * Descriptions of constants.
 */

public interface Constants {

    String CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS = "Загрузить zip файл и отправить МЧД на регистрацию через API РР МЧД";
    String CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_REVOKE = "Загрузить zip файл и отправить на регистрацию отзыв МЧД через API РР МЧД";
    String CONTROLLER_GET_M4D_BY_STATUS = "Получить статус МЧД через API РР МЧД";
    String CONTROLLER_GET_FULL_M4D_BY_GUID_AND_INNS = "Получить полные данные по МЧД через API РР МЧД";
    String API_RESPONSE_CODE_200_MESSAGE = "Успешно"; //Successfully
    String API_RESPONSE_CODE_400_MESSAGE = "Плохой запрос"; //Not found
    String API_RESPONSE_CODE_404_MESSAGE = "Не найдено"; //Not found

    String FILE_NOT_FOUND = "Файл не найден";
    String ZIP_XML_NOT_FOUND = "XML-файл не найден в ZIP архиве";
    String ZIP_XML_MORE_TWO = "В архиве найдено более двух XML-файлов";
    String ZIP_SIG_NOT_FOUND = "SIG-файл не найден в ZIP архиве";
    String ZIP_SIG_MORE_TWO = "В архиве найдено более двух SIG-файлов";
    String NO_ZIP = "Переданный файл не является архивом";
    String ZIP_UNPACK_ERROR = "Ошибка распаковки ZIP файла";
    String CREATE_TEMP_FOLDER_ERROR = "Не удалось создать временную директорию";
    String CLEAR_TEMP_FOLDER_ERROR = "Ошибка очистки временной директории";
    String DELETE_TEMP_FILE_ERROR = "Ошибка удаления временного файла";
    String M4D_REGISTRATION_ERROR = "Ошибка регистрации МЧД в РР МЧД";
    String M4D_REVOKE_REGISTRATION_ERROR = "Ошибка регистрации отзыва МЧД в РР МЧД";

    String XML_ELEMENT_MCHD_CONFIRM = "Подтверждение";
    String XML_ATTRIBUTE_MCHD_CONFIRM_FORMAT_VERSION = "ВерсФорм";
    String XML_ATTRIBUTE_MCHD_CONFIRM_TYPE = "ТипПодтв";
    String XML_ATTRIBUTE_MCHD_CONFIRM_FILE_ID = "ИдФайл";
    String XML_ATTRIBUTE_MCHD_CONFIRM_GUID = "НомПодтв";
    String XML_ATTRIBUTE_MCHD_CONFIRM_DATE_REGISTERED = "ДатаВремВыдПодтв";
    String XML_ATTRIBUTE_MCHD_CONFIRM_OPERATOR_ID = "АдрНодОпер";
    String XML_ELEMENT_MCHD_CONFIRM_ORGANISATION = "СвЮЛПодтв";
    String XML_ATTRIBUTE_MCHD_CONFIRM_ORGANISATION_ID = "ИдЮЛ";
    String XML_ATTRIBUTE_MCHD_CONFIRM_ORGANISATION_NAME = "НаимОрг";
    String XML_ATTRIBUTE_MCHD_CONFIRM_ORGANISATION_TRADE_NAME = "ТоргНаим";
    String XML_ATTRIBUTE_MCHD_CONFIRM_ORGANISATION_INN = "ИННЮЛ";
    String XML_ATTRIBUTE_MCHD_CONFIRM_ORGANISATION_OGRN = "ОГРН";
    String XML_ELEMENT_MCHD_CONFIRM_SYSTEM_INFO = "URLПродуктОпер";
    String XML_ELEMENT_MCHD_CONFIRM_SYSTEM_VERSION = "ВерсПрог";
    String XML_ELEMENT_MCHD_CONFIRM_INFO = "СвДовер";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_FILE_ID = "ИдФайл";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_GUID = "НомДовер";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_INTERNAL_NUMBER = "ВнНомДовер";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_DATE_START = "ДатаВыдДовер";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_DATE_END = "СрокДейст";
    String XML_ATTRIBUTE_MCHD_CONFIRM_INFO_SYSTEM_INFO = "СведСист";

    String XML_ELEMENT_MCHD_REVOKE_CONFIRM = "Подтверждение";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_FORMAT_VERSION = "ВерсФорм";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_TYPE = "ТипПодтв";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_FILE_ID = "ИдФайл";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_GUID = "НомПодтв";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_DATE_REGISTERED = "ДатаВремВыдПодтв";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_OPERATOR_ID = "АдрНодОпер";
    String XML_ELEMENT_MCHD_REVOKE_CONFIRM_ORGANISATION = "СвЮЛПодтв";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_ORGANISATION_ID = "ИдЮЛ";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_ORGANISATION_NAME = "НаимОрг";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_ORGANISATION_TRADE_NAME = "ТоргНаим";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_ORGANISATION_INN = "ИННЮЛ";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_ORGANISATION_OGRN = "ОГРН";
    String XML_ELEMENT_MCHD_REVOKE_CONFIRM_SYSTEM_INFO = "URLПродуктОпер";
    String XML_ELEMENT_MCHD_REVOKE_CONFIRM_SYSTEM_VERSION = "ВерсПрог";
    String XML_ELEMENT_MCHD_REVOKE_CONFIRM_INFO = "СвЗаявлОтмена";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_INFO_FILE_HASH = "ХэшЗаявлОтмена";
    String XML_ATTRIBUTE_MCHD_REVOKE_CONFIRM_INFO_GUID = "НомДовер";
}
