// Данные и метаданные 1С-форм из расширения СП_Тестирование

export interface FormElement1C {
  id: string;
  name: string;
  title: string;
  type: 'InputField' | 'Button' | 'FormGroup' | 'Table' | 'LabelDecoration' | 'CheckBox';
  groupType?: 'Vertical' | 'Horizontal' | 'Pages' | 'Page';
  dataPath?: string;
  commandName?: string;
  bslHandler?: string;
  visible: boolean;
  enabled: boolean;
  tooltip?: string;
  valueType?: string;
  defaultValue?: string | boolean | number;
  children?: FormElement1C[];
  columns?: Array<{ name: string; title: string; width?: number }>;
}

export interface Form1CDefinition {
  id: string;
  title: string;
  shortName: string;
  path: string;
  type: 'DataProcessor' | 'CatalogElement' | 'CatalogList';
  icon: string;
  description: string;
  xmlPath: string;
  bslPath: string;
  xmlContent: string;
  bslContent: string;
  elements: FormElement1C[];
  commands: Array<{
    name: string;
    title: string;
    handler: string;
    isDefault?: boolean;
    buttonType?: 'primary' | 'standard';
  }>;
  attributes: Array<{
    name: string;
    type: string;
    title: string;
  }>;
}

export const FORMS_1C_DATA: Form1CDefinition[] = [
  {
    id: 'console_tests',
    title: 'Консоль тестов (UI-платформа Specter)',
    shortName: 'Консоль тестов',
    path: 'DataProcessors.СП_КонсольТестов.Forms.ФормаСписка',
    type: 'DataProcessor',
    icon: 'Terminal',
    description: 'Интерфейс внутри 1С (R4): обнаружение, запуск и отчёт по тестовым наборам без внешней инфраструктуры.',
    xmlPath: 'extension/СП_Тестирование/src/DataProcessors/СП_КонсольТестов/Forms/ФормаСписка/Form.form',
    bslPath: 'extension/СП_Тестирование/src/DataProcessors/СП_КонсольТестов/Forms/ФормаСписка/Module.bsl',
    xmlContent: `<?xml version="1.0" encoding="UTF-8"?>
<form:Form xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:form="http://g5.1c.ru/v8/dt/form">
  <items xsi:type="form:FormField">
    <name>ПолеИтога</name>
    <id>3</id>
    <visible>true</visible>
    <enabled>true</enabled>
    <dataPath xsi:type="form:DataPath">
      <segments>Итог</segments>
    </dataPath>
    <type>InputField</type>
  </items>
  <items xsi:type="form:FormField">
    <name>ПолеОтчета</name>
    <id>6</id>
    <visible>true</visible>
    <enabled>true</enabled>
    <dataPath xsi:type="form:DataPath">
      <segments>ТекстОтчета</segments>
    </dataPath>
    <type>InputField</type>
  </items>
  <autoCommandBar>
    <items xsi:type="form:Button">
      <name>КнопкаВыполнить</name>
      <id>1</id>
      <commandName>Form.Command.Выполнить</commandName>
    </items>
    <items xsi:type="form:Button">
      <name>КнопкаОбнаружить</name>
      <id>9</id>
      <commandName>Form.Command.Обнаружить</commandName>
    </items>
  </autoCommandBar>
</form:Form>`,
    bslContent: `//©///////////////////////////////////////////////////////////////////////////©
//  СП_КонсольТестов - обработка «Консоль тестов (UI-платформа)».
//  Интерфейс внутри 1С (R4): обнаружение, запуск и отчёт по тестовым наборам
//  без внешней инфраструктуры. Форма: реквизиты Итог и ТекстОтчета.
//©///////////////////////////////////////////////////////////////////////////©

#Область ОбработчикиКомандФормы

&НаКлиенте
Процедура ВыполнитьТесты(Команда)
	ВыполнитьТестыНаСервере();
КонецПроцедуры

&НаКлиенте
Процедура ПоказатьТесты(Команда)
	ПоказатьТестыНаСервере();
КонецПроцедуры

#КонецОбласти

#Область СлужебныеПроцедурыИФункции

&НаСервере
Процедура ВыполнитьТестыНаСервере()
	Сводка = СП_Тестирование.Запустить();
	КолПройдено = 0;
	КолПровалено = 0;
	
	Текст = Новый ТекстовыйДокумент;
	Текст.ДобавитьСтроку("=== РЕЗУЛЬТАТЫ ВЫПОЛНЕНИЯ ТЕСТОВ SPECTER / UITP ===");
	Текст.ДобавитьСтроку("Время запуска: " + Формат(ТекущаяДата(), "ДФ=yyyy-MM-dd HH:mm:ss"));
	Текст.ДобавитьСтроку("");
	
	Для Каждого Тест Из Сводка Цикл
		Если Тест.Статус = "passed" Тогда
			КолПройдено = КолПройдено + 1;
			Текст.ДобавитьСтроку("[OK]   " + Тест.Имя);
		Иначе
			КолПровалено = КолПровалено + 1;
			Текст.ДобавитьСтроку("[FAIL] " + Тест.Имя + " -> " + Тест.Сообщение);
		КонецЕсли;
	КонецЦикла;
	
	Итог = СтрШаблон("Пройдено: %1, Провалено: %2 (Всего: %3)", 
		КолПройдено, КолПровалено, КолПройдено + КолПровалено);
	ТекстОтчета = Текст.ПолучитьТекст();
КонецПроцедуры

&НаСервере
Процедура ПоказатьТестыНаСервере()
	Наборы = СП_Тестирование.ОбнаружитьТесты();
	Текст = Новый ТекстовыйДокумент;
	Текст.ДобавитьСтроку("=== ОБНАРУЖЕННЫЕ ТЕСТОВЫЕ НАБОРЫ ===");
	Для Каждого Набор Из Наборы Цикл
		Текст.ДобавитьСтроку("- " + Набор.Модуль + " (" + Набор.Метод + ")");
	КонецЦикла;
	ТекстОтчета = Текст.ПолучитьТекст();
КонецПроцедуры

#КонецОбласти`,
    attributes: [
      { name: 'Объект', type: 'ОбработкаОбъект.СП_КонсольТестов', title: 'Объект обработки' },
      { name: 'Итог', type: 'Строка', title: 'Строка сводного итога' },
      { name: 'ТекстОтчета', type: 'Строка (многострочная)', title: 'Текст протокола тестов' }
    ],
    commands: [
      { name: 'Выполнить', title: 'Выполнить тесты', handler: 'ВыполнитьТесты', isDefault: true, buttonType: 'primary' },
      { name: 'Обнаружить', title: 'Обнаружить тесты', handler: 'ПоказатьТесты', buttonType: 'standard' }
    ],
    elements: [
      {
        id: '3',
        name: 'ПолеИтога',
        title: 'Итог прогона',
        type: 'InputField',
        dataPath: 'Итог',
        visible: true,
        enabled: true,
        defaultValue: 'Пройдено: 8, Провалено: 0 (Всего: 8)',
        tooltip: 'Краткий результат последнего прогона'
      },
      {
        id: '6',
        name: 'ПолеОтчета',
        title: 'Текст отчета',
        type: 'InputField',
        dataPath: 'ТекстОтчета',
        visible: true,
        enabled: true,
        defaultValue: `=== РЕЗУЛЬТАТЫ ВЫПОЛНЕНИЯ ТЕСТОВ SPECTER / UITP ===
[OK]   Тест01_ОткрытиеСписка_ФормаОткрыта
[OK]   Тест02_ПоискКонтрагента_ЭлементНайден
[OK]   Тест03_ОткрытиеКарточки_ФормаЭлементаОткрыта
[OK]   Тест04_ПроверкаРеквизитов_ИНН_Заполнен
[OK]   Тест05_ВводКонтактнойИнформации_Успешно
[OK]   Тест06_ЗаписьКонтрагента_БезОшибок
[OK]   Тест07_ПроверкаДоговоров_СписокНеПуст
[OK]   Тест08_ЗакрытиеКарточки_ВозвратВСписок

Все 8 сценариев успешно выполнены без падений UI!`,
        tooltip: 'Подробный протокол выполнения сценариев'
      }
    ]
  },
  {
    id: 'contractor_card',
    title: 'Карточка Контрагента (ФормаЭлемента)',
    shortName: 'Карточка контрагента',
    path: 'Catalogs.Контрагенты.Forms.ФормаЭлемента',
    type: 'CatalogElement',
    icon: 'Building2',
    description: 'Основная форма справочника Контрагенты со всеми группами, реквизитами ИНН/КПП, банковскими счетами и контактной информацией.',
    xmlPath: 'extension/СП_Тестирование/src/Catalogs/Контрагенты/Forms/ФормаЭлемента/Form.form',
    bslPath: 'extension/СП_Тестирование/src/Catalogs/Контрагенты/Forms/ФормаЭлемента/Module.bsl',
    xmlContent: `<?xml version="1.0" encoding="UTF-8"?>
<form:Form xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:form="http://g5.1c.ru/v8/dt/form">
  <items xsi:type="form:FormGroup">
    <name>Шапка</name>
    <group>Horizontal</group>
    <items xsi:type="form:FormField">
      <name>Наименование</name>
      <dataPath>Объект.Наименование</dataPath>
      <type>InputField</type>
    </items>
    <items xsi:type="form:FormField">
      <name>Код</name>
      <dataPath>Объект.Код</dataPath>
      <type>InputField</type>
    </items>
  </items>
  <items xsi:type="form:FormGroup">
    <name>ГруппаИНН_КПП</name>
    <group>Horizontal</group>
    <items xsi:type="form:FormField">
      <name>ИНН</name>
      <dataPath>Объект.ИНН</dataPath>
      <type>InputField</type>
    </items>
    <items xsi:type="form:FormField">
      <name>КПП</name>
      <dataPath>Объект.КПП</dataPath>
      <type>InputField</type>
    </items>
  </items>
</form:Form>`,
    bslContent: `&НаСервере
Процедура ПриСозданииНаСервере(Отказ, СтандартнаяОбработка)
	// Инициализация формы в контексте расширения СП_Тестирование
	Если Не ЗначениеЗаполнено(Объект.Ссылка) Тогда
		Объект.Код = "TEST-UL-0001";
		Объект.ИНН = "7700000000";
		Объект.КПП = "770101001";
		Объект.Наименование = "ООО Озон Ритейл Тест";
	КонецЕсли;
КонецПроцедуры

&НаКлиенте
Процедура ЗаписатьИЗакрыть(Команда)
	Отказ = Ложь;
	ПроверитьЗаполнениеСервер(Отказ);
	Если Не Отказ Тогда
		Закрыть(Истина);
	КонецЕсли;
КонецПроцедуры`,
    attributes: [
      { name: 'Объект', type: 'СправочникОбъект.Контрагенты', title: 'Объект контрагента' },
      { name: 'ОсновнойБанковскийСчет', type: 'СправочникСсылка.БанковскиеСчета', title: 'Основной счет' },
      { name: 'ЮридическоеФизическоеЛицо', type: 'ПеречислениеСсылка.ЮрФизЛицо', title: 'Юр./Физ. лицо' }
    ],
    commands: [
      { name: 'ЗаписатьИЗакрыть', title: 'Записать и закрыть', handler: 'ЗаписатьИЗакрыть', isDefault: true, buttonType: 'primary' },
      { name: 'Записать', title: 'Записать', handler: 'Записать', buttonType: 'standard' },
      { name: 'Перечитать', title: 'Перечитать', handler: 'Перечитать', buttonType: 'standard' }
    ],
    elements: [
      {
        id: 'group_header',
        name: 'Шапка',
        title: 'Основные данные',
        type: 'FormGroup',
        groupType: 'Horizontal',
        visible: true,
        enabled: true,
        children: [
          {
            id: '3',
            name: 'Наименование',
            title: 'Наименование',
            type: 'InputField',
            dataPath: 'Объект.Наименование',
            visible: true,
            enabled: true,
            defaultValue: 'ООО Озон Ритейл Тест'
          },
          {
            id: '1',
            name: 'Код',
            title: 'Код',
            type: 'InputField',
            dataPath: 'Объект.Код',
            visible: true,
            enabled: true,
            defaultValue: 'TEST-UL-0001'
          }
        ]
      },
      {
        id: 'group_inn_kpp',
        name: 'ГруппаИНН_КПП',
        title: 'Налоговые реквизиты',
        type: 'FormGroup',
        groupType: 'Horizontal',
        visible: true,
        enabled: true,
        children: [
          {
            id: '10',
            name: 'ИНН',
            title: 'ИНН',
            type: 'InputField',
            dataPath: 'Объект.ИНН',
            visible: true,
            enabled: true,
            defaultValue: '7700000000'
          },
          {
            id: '11',
            name: 'КПП',
            title: 'КПП',
            type: 'InputField',
            dataPath: 'Объект.КПП',
            visible: true,
            enabled: true,
            defaultValue: '770101001'
          }
        ]
      },
      {
        id: 'group_contacts',
        name: 'ГруппаКонтакты',
        title: 'Адреса и контакты',
        type: 'FormGroup',
        groupType: 'Vertical',
        visible: true,
        enabled: true,
        children: [
          {
            id: '12',
            name: 'ЮридическийАдрес',
            title: 'Юридический адрес',
            type: 'InputField',
            dataPath: 'Объект.ЮридическийАдрес',
            visible: true,
            enabled: true,
            defaultValue: '123112, г. Москва, Пресненская наб., д. 10'
          },
          {
            id: '13',
            name: 'ФактическийАдрес',
            title: 'Фактический адрес',
            type: 'InputField',
            dataPath: 'Объект.ФактическийАдрес',
            visible: true,
            enabled: true,
            defaultValue: '123112, г. Москва, Пресненская наб., д. 10'
          },
          {
            id: '14',
            name: 'Телефон',
            title: 'Телефон',
            type: 'InputField',
            dataPath: 'Объект.Телефон',
            visible: true,
            enabled: true,
            defaultValue: '+7 (495) 123-45-67'
          }
        ]
      },
      {
        id: 'flag_supplier',
        name: 'Поставщик',
        title: 'Является поставщиком',
        type: 'CheckBox',
        dataPath: 'Объект.Поставщик',
        visible: true,
        enabled: true,
        defaultValue: true
      },
      {
        id: 'flag_buyer',
        name: 'Покупатель',
        title: 'Является покупателем',
        type: 'CheckBox',
        dataPath: 'Объект.Покупатель',
        visible: true,
        enabled: true,
        defaultValue: true
      }
    ]
  },
  {
    id: 'contractors_list',
    title: 'Список Контрагентов (ФормаСписка)',
    shortName: 'Список контрагентов',
    path: 'Catalogs.Контрагенты.Forms.ФормаСписка',
    type: 'CatalogList',
    icon: 'ListOrdered',
    description: 'Динамический список контрагентов с поиском, отбором и быстрыми действиями.',
    xmlPath: 'extension/СП_Тестирование/src/Catalogs/Контрагенты/Forms/ФормаСписка/Form.form',
    bslPath: 'extension/СП_Тестирование/src/Catalogs/Контрагенты/Forms/ФормаСписка/Module.bsl',
    xmlContent: `<?xml version="1.0" encoding="UTF-8"?>
<form:Form xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:form="http://g5.1c.ru/v8/dt/form">
  <items xsi:type="form:Table">
    <name>Список</name>
    <id>1</id>
    <items xsi:type="form:FormField">
      <name>Наименование</name>
      <dataPath>Список.Наименование</dataPath>
    </items>
    <items xsi:type="form:FormField">
      <name>Код</name>
      <dataPath>Список.Код</dataPath>
    </items>
    <items xsi:type="form:FormField">
      <name>ИНН</name>
      <dataPath>Список.ИНН</dataPath>
    </items>
  </items>
</form:Form>`,
    bslContent: `&НаКлиенте
Процедура СписокПриАктивизацииСтроки(Элемент)
	ТекущиеДанные = Элементы.Список.ТекущиеДанные;
	Если ТекущиеДанные <> Неопределено Тогда
		// Оповещение моста Specter о фокусе в строке списка
	КонецЕсли;
КонецПроцедуры`,
    attributes: [
      { name: 'Список', type: 'ДинамическийСписок', title: 'Основной список' }
    ],
    commands: [
      { name: 'Создать', title: 'Создать', handler: 'СоздатьЭлемент', isDefault: true, buttonType: 'primary' },
      { name: 'Скопировать', title: 'Скопировать', handler: 'СкопироватьЭлемент', buttonType: 'standard' },
      { name: 'Изменить', title: 'Изменить', handler: 'ИзменитьЭлемент', buttonType: 'standard' }
    ],
    elements: [
      {
        id: 'table_list',
        name: 'Список',
        title: 'Таблица контрагентов',
        type: 'Table',
        visible: true,
        enabled: true,
        columns: [
          { name: 'Наименование', title: 'Наименование', width: 280 },
          { name: 'Код', title: 'Код', width: 120 },
          { name: 'ИНН', title: 'ИНН', width: 140 },
          { name: 'КПП', title: 'КПП', width: 110 },
          { name: 'СтатусТеста', title: 'Статус UITP', width: 140 }
        ]
      }
    ]
  },
  {
    id: 'form_manager',
    title: 'Управление Формами (Исполнитель сценария)',
    shortName: 'Управление формами',
    path: 'DataProcessors.СП_УправлениеФормами.Forms.ОсновнаяФорма',
    type: 'DataProcessor',
    icon: 'PlayCircle',
    description: 'Форма-исполнитель реального прогона сценария afm-programmatic-contractor-form (Фаза B, live afm).',
    xmlPath: 'extension/СП_Тестирование/src/DataProcessors/СП_УправлениеФормами/Forms/ОсновнаяФорма/Form.form',
    bslPath: 'extension/СП_Тестирование/src/DataProcessors/СП_УправлениеФормами/Forms/ОсновнаяФорма/Module.bsl',
    xmlContent: `<?xml version="1.0" encoding="UTF-8"?>
<form:Form xmlns:form="http://g5.1c.ru/v8/dt/form">
  <autoCommandBar>
    <name>ФормаКоманднаяПанель</name>
    <id>-1</id>
  </autoCommandBar>
  <handlers>
    <event>OnOpen</event>
    <name>ПриОткрытии</name>
  </handlers>
</form:Form>`,
    bslContent: `&НаКлиенте
Процедура ПриОткрытии(Отказ) Экспорт
	ПутьОтчёта = ПолучитьКаталогОбмена() + "/live-afm-run-raw.json";
	КонтрагентКод = "TEST-UL-0001";
	ИННОжидаемое = "7700000000";
	Шаги = Новый Массив;
	ЭтаФорма.Заголовок = "LIVE: СТАРТ сценария";

	Попытка
		СсылкаКонтрагента = УстановитьДанные(КонтрагентКод);
		Шаги.Добавить(ШагДляОтчёта("setup", "prepareContractor", "passed",
			"контрагент " + КонтрагентКод + " подготовлен"));
		ЭтаФорма.Заголовок = "LIVE: setup OK";
	Исключение
		Шаги.Добавить(ШагДляОтчёта("setup", "prepareContractor", "failed", ОписаниеОшибки()));
		ЗаписатьОтчёт(ПутьОтчёта, Шаги);
		Возврат;
	КонецПопытки;
КонецПроцедуры`,
    attributes: [
      { name: 'Объект', type: 'ОбработкаОбъект.СП_УправлениеФормами', title: 'Объект' }
    ],
    commands: [
      { name: 'ЗапуститьСценарий', title: 'Запустить сценарий', handler: 'ПриОткрытии', isDefault: true, buttonType: 'primary' }
    ],
    elements: [
      {
        id: 'status_field',
        name: 'СостояниеСценария',
        title: 'Статус прогона',
        type: 'InputField',
        dataPath: 'ЗаголовокФормы',
        visible: true,
        enabled: true,
        defaultValue: 'LIVE: setup OK | waitFor: Список OK | assert: ИНН=7700000000 PASSED'
      }
    ]
  }
];

export const MOCK_TABLE_ROWS = [
  { id: '1', name: 'ООО Озон Ритейл Тест', code: 'TEST-UL-0001', inn: '7700000000', kpp: '770101001', status: 'Passed (0.42s)' },
  { id: '2', name: 'ПАО Интернет Решения', code: 'TEST-UL-0002', inn: '7704217370', kpp: '770301001', status: 'Passed (0.35s)' },
  { id: '3', name: 'ООО Спектер Диджитал', code: 'TEST-UL-0003', inn: '7712345678', kpp: '771201001', status: 'Passed (0.51s)' },
  { id: '4', name: 'АО Логистик Платформ', code: 'TEST-UL-0004', inn: '7723456789', kpp: '772301001', status: 'Ready' }
];
