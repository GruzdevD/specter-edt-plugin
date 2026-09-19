package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;

/**
 * Определение открытого в редакторе EDT тестового набора — по образцу YAxUnit,
 * где тесты можно запускать прямо из окна редактирования модуля-набора.
 *
 * <p>Из активного редактора извлекается метамодель {@link CommonModule} через
 * {@link IEditorInput#getAdapter(CommonModule.class)} (адаптер предоставляет сам
 * EDT-редактор модуля; в нашем коде держим только интерфейс {@link IEditorInput},
 * поэтому internal-класс редактора не нужен). Модуль считается тестовым набором
 * по конвенции имён ({@code СП_Тесты_*} / {@code Тесты_*}), а отдельные
 * тесты — это экспортные методы BSL-модуля {@code getModule().getMethods()}.</p>
 *
 * <p>Запуск конкретного набора из панели идёт через мост: имя модуля набора
 * передаётся движку ({@code СП_Тестирование}), который сопоставляет его с
 * {@code КаталогНаборов} по {@code ИмяМодуля → ИмяНабора} и исполняет
 * {@code Запустить({Набор, ИмяТеста})} (см. {@link BridgeScenario#runSetJson}).</p>
 */
public final class EditorModuleSupport {

	private EditorModuleSupport() {
	}

	/** Тестовый ли это набор по имени BSL-модуля (конвенция имён). */
	public static boolean isTestSetName(String moduleName) {
		if (moduleName == null) {
			return false;
		}
		// Исключаем служебные модули движка СП_
		if (moduleName.equals("СП_Тестирование")
				|| moduleName.equals("СП_ТестовыйКлиент")
				|| moduleName.equals("СП_ТестовыеДанные")
				|| moduleName.equals("СП_Утверждения")
				|| moduleName.equals("СП_Адаптер")
				|| moduleName.equals("СП_Протокол")
				|| moduleName.equals("СП_СправочникИмен")
				|| moduleName.equals("СП_ГенераторДанных")
				|| moduleName.equals("СП_ДействияКлиент")
				|| moduleName.equals("СП_ИнспекторКлиент")
				|| moduleName.equals("СП_ОжиданияКлиент")
				|| moduleName.equals("СП_ПроксиФормыКлиент")) {
			return false;
		}
		String lower = moduleName.toLowerCase();
		return lower.startsWith("сп_тест")
				|| lower.startsWith("тест_")
				|| lower.startsWith("тесты_")
				|| lower.contains("_тест")
				|| lower.contains("test");
	}

	/** Активный редактор рабочей страницы (или null, если нет/неполадка). */
	public static IEditorPart activeEditor() {
		try {
			IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (win == null) {
				return null;
			}
			IWorkbenchPage page = win.getActivePage();
			return page == null ? null : page.getActiveEditor();
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Метамодель общего модуля активного редактора, если он открыт как модуль. */
	public static CommonModule activeCommonModule(IEditorPart editor) {
		if (editor == null) {
			return null;
		}
		IEditorInput input = editor.getEditorInput();
		if (input == null) {
			return null;
		}
		try {
			return input.getAdapter(CommonModule.class);
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Имя модуля набора (например {@code OZON_UI_Тесты_Контрагенты}). */
	public static String moduleName(CommonModule cm) {
		try {
			return cm.getName();
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Имена тестов (экспортные методы BSL-модуля) набора. */
	public static List<String> testNames(CommonModule cm) {
		List<String> names = new ArrayList<>();
		try {
			Module m = cm.getModule();
			if (m != null) {
				for (Method method : m.getMethods()) {
					names.add(method.getName());
				}
			}
		} catch (RuntimeException e) {
			// модель может быть ещё не готова — отдаём пусто
		}
		return names;
	}
}
