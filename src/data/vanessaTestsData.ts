import { convertGherkinFeatureToBslModule } from './vanessaConverterEngine';

export interface VanessaTestItem {
  id: string;
  featureFile: string;
  featureName: string;
  scenarioName: string;
  targetModuleName: string;
  tags: string[];
  stepsCount: number;
  complexity: 'low' | 'medium' | 'high';
  featureSource: string;
  generatedBslCode: string;
  status: 'pending' | 'converted';
  description: string;
}

const RAW_VANESSA_TESTS_DEFS = [
  {
    id: 'vanessa-1',
    featureFile: 'features/auth/Авторизация.feature',
    featureName: 'Авторизация и сессии пользователей',
    scenarioName: 'Успешная авторизация пользователя с ролью МенеджерПродаж',
    targetModuleName: 'СП_Тест_АвторизацияМенеджера',
    tags: ['smoke', 'auth', 'critical'],
    stepsCount: 6,
    complexity: 'low' as const,
    status: 'pending' as const,
    description: 'Проверка открытия формы авторизации, ввода учетных данных и перехода в рабочий стол',
    featureSource: `# language: ru
@smoke @auth @critical
Функционал: Авторизация и сессии пользователей

  Сценарий: Успешная авторизация пользователя с ролью МенеджерПродаж
    Дано Я открываю форму "Обработка.Авторизация.Форма"
    Когда Я заполняю поле "Логин" текстом "Администратор"
    И Я заполняю поле "Пароль" текстом "admin123"
    И Я нажимаю кнопку "Войти"
    Тогда Я жду появления элемента "РабочийСтолМенеджера" в течение 10 секунд
    И Поле "ТекущийПользователь" содержит "Администратор"`
  },
  {
    id: 'vanessa-2',
    featureFile: 'features/documents/ЗаказПокупателя.feature',
    featureName: 'Документы и торговые операции',
    scenarioName: 'Оформление нового документа ЗаказПокупателя с заполнением табличной части',
    targetModuleName: 'СП_Тест_ОформлениеЗаказаПокупателя',
    tags: ['e2e', 'documents', 'sales'],
    stepsCount: 9,
    complexity: 'high' as const,
    status: 'pending' as const,
    description: 'Полный E2E цикл создания документа Заказ покупателя, добавление товарных строк, проведение документа',
    featureSource: `# language: ru
@e2e @documents @sales
Функционал: Документы и торговые операции

  Сценарий: Оформление нового документа ЗаказПокупателя с заполнением табличной части
    Дано Я открываю навигационную ссылку "Документ.ЗаказПокупателя.ФормаОбъекта"
    Когда Я заполняю поле "Контрагент" значением "ООО Озон Маркетплейс"
    И В табличной части "Товары" добавляю строку
    И В табличной части "Товары" в текущей строке в колонке "Номенклатура" указываю "Сканер штрихкодов Honeywell"
    И В табличной части "Товары" в текущей строке в колонке "Количество" указываю "5"
    И В табличной части "Товары" в текущей строке в колонке "Цена" указываю "12500"
    И Я нажимаю на кнопку "ПровестиИЗакрыть"
    Тогда Я жду закрытия формы в течение 15 секунд`
  },
  {
    id: 'vanessa-3',
    featureFile: 'features/discounts/СкидкиИАкции.feature',
    featureName: 'Маркетинговые акции и скидки',
    scenarioName: 'Применение автоматической оптовой скидки 15% при сумме заказа свыше 50000',
    targetModuleName: 'СП_Тест_РасчетОптовойСкидки',
    tags: ['discounts', 'pricing', 'regression'],
    stepsCount: 7,
    complexity: 'medium' as const,
    status: 'pending' as const,
    description: 'Проверка калькуляции скидок, пересчета итогов и корректности округления копеек',
    featureSource: `# language: ru
@discounts @pricing @regression
Функционал: Маркетинговые акции и скидки

  Сценарий: Применение автоматической оптовой скидки 15% при сумме заказа свыше 50000
    Дано Я открываю форму "Обработка.КалькуляторСкидок.Форма"
    Когда Я устанавливаю флаг "АвтоматическийРасчетСкидок"
    И Я заполняю поле "СуммаДокумента" значением "65000"
    И Я нажимаю кнопку "Рассчитать"
    Тогда Поле "ПроцентСкидки" содержит "15"
    И Поле "СуммаСкидки" содержит "9750"
    И Поле "ИтогоКСписанию" содержит "55250"`
  },
  {
    id: 'vanessa-4',
    featureFile: 'features/warehouse/Инвентаризация.feature',
    featureName: 'Складской учет и перемещения',
    scenarioName: 'Формирование ордера на перемещение товаров между складами Ozon Hub',
    targetModuleName: 'СП_Тест_ПеремещениеТоваровХаб',
    tags: ['warehouse', 'logistics'],
    stepsCount: 6,
    complexity: 'medium' as const,
    status: 'pending' as const,
    description: 'Проверка заполнения складов отправителя/получателя, переход по вкладкам и валидации резервов',
    featureSource: `# language: ru
@warehouse @logistics
Функционал: Складской учет и перемещения

  Сценарий: Формирование ордера на перемещение товаров между складами Ozon Hub
    Дано Я открываю форму "Документ.ПеремещениеТоваров.ФормаОбъекта"
    Когда Я заполняю поле "СкладОтправитель" значением "Фулфилмент Хоругвино"
    И Я заполняю поле "СкладПолучатель" значением "СЦ Санкт-Петербург Шушары"
    И Я перехожу к вкладке "ТоварыИУслуги"
    И Я нажимаю на кнопку "ЗаполнитьПоОстаткам"
    Тогда Я жду 2 секунд`
  },
  {
    id: 'vanessa-5',
    featureFile: 'features/contractors/КонтрагентыПеременные.feature',
    featureName: 'Управление контрагентами и контекстные переменные',
    scenarioName: 'Создание контрагента с сохранением ИНН в контекстную память сценария',
    targetModuleName: 'СП_Тест_КонтрагентыСПамятью',
    tags: ['contractors', 'variables', 'context'],
    stepsCount: 6,
    complexity: 'medium' as const,
    status: 'pending' as const,
    description: 'Использование переменных $$ИНН$$ и $КодКлиента$ при сквозном прогоне шагов сценария',
    featureSource: `# language: ru
@contractors @variables @context
Функционал: Управление контрагентами и контекстные переменные

  Сценарий: Создание контрагента с сохранением ИНН в контекстную память сценария
    Дано Я открываю форму "Справочник.Контрагенты.ФормаОбъекта"
    Когда Я заполняю поле "Наименование" текстом "ПАО Мегамаркет Ритейл"
    И Я заполняю поле "ИНН" текстом "7701234567"
    И Я сохраняю значение поля "ИНН" в переменную "$$ИНН_Клиента$$"
    И Я нажимаю на кнопку "ЗаписатьИЗакрыть"
    Тогда Я жду закрытия формы "Контрагенты" в течение 10 секунд`
  }
];

export const INITIAL_VANESSA_TESTS: VanessaTestItem[] = RAW_VANESSA_TESTS_DEFS.map(def => ({
  ...def,
  generatedBslCode: convertGherkinFeatureToBslModule(def.featureSource, def.targetModuleName, def.scenarioName)
}));
