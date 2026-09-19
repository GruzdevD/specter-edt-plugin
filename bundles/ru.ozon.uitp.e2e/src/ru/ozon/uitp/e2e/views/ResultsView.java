package ru.ozon.uitp.e2e.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Панель «Результаты» — детализация последнего прогона моста, по образцу
 * результатов YAxUnit в EDT. Показывает пошаговый отчёт ({@code id — action —
 * статус — detail}) с цветовой подсветкой: {@code passed} зелёным, {@code failed}
 * красным. Обновляется автоматически при публикации нового результата в
 * {@link BridgeResultStore} (панель подписывается на тот же стор, что и
 * {@link TestsView}).</p>
 */
public class ResultsView extends ViewPart {

	/** Идентификатор панели (регистрируется в plugin.xml → org.eclipse.ui.views). */
	public static final String ID = "ru.ozon.uitp.e2e.views.ResultsView";

	private final BridgeResultStore.Listener storeListener = this::showResult;

	private StyledText text;

	@Override
	public void createPartControl(Composite parent) {
		text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setEditable(false);
		BridgeResultStore.get().addListener(storeListener);
		showResult(BridgeResultStore.get().current());
	}

	@Override
	public void setFocus() {
		if (text != null && !text.isDisposed()) {
			text.setFocus();
		}
	}

	@Override
	public void dispose() {
		BridgeResultStore.get().removeListener(storeListener);
		super.dispose();
	}

	private void showResult(BridgeResult r) {
		if (text == null || text.isDisposed()) {
			return;
		}
		if (r == null || r.status.equals("hint")) {
			text.setText("Результат моста не найден. Запусти сценарий на панели «Тесты».");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Прогон моста: ").append(r.runId).append('\n');
		sb.append("Статус: ").append(statusText(r.status));
		sb.append("   (passed ").append(r.passedCount()).append(" / failed ").append(r.failedCount()).append(')');
		if (r.source != null) {
			sb.append("\nФайл: ").append(r.source.getAbsolutePath());
		}
		sb.append("\n\nШаги:\n");
		for (BridgeResult.Step s : r.steps) {
			sb.append("  #").append(s.id).append("  ").append(s.action)
					.append("  —  ").append(statusText(s.status));
			if (!s.detail.isEmpty()) {
				sb.append("\n      ").append(s.detail);
			}
			sb.append('\n');
		}
		text.setText(sb.toString());

		// Цветовая подсветка строк шагов: цвета применяем построчно к префиксу статуса.
		Color green = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
		String[] lines = sb.toString().split("\n", -1);

		// Сначала снимем все стили, затем расставим заново.
		StyleRange[] ranges = new StyleRange[0];
		// (сброс проще через setStyleRanges(new StyleRange[0]))
		for (int li = 0; li < lines.length; li++) {
			String line = lines[li];
			int colondIdx = line.indexOf("—");
			if (colondIdx < 0) {
				continue;
			}
			boolean failed = line.contains("FAILED");
			if (!failed && !line.contains("PASSED")) {
				continue;
			}
			int lineStart = offsetOf(sb, li);
			StyleRange sr = new StyleRange();
			sr.start = lineStart + colondIdx - 3;
			sr.length = 6; // "PASSED"/"FAILED" плюс пробелы
			sr.foreground = failed ? red : green;
			sr.fontStyle = SWT.BOLD;
			ranges = append(ranges, sr);
		}
		text.setStyleRanges(ranges);
	}

	/** Смещение начала строки {@code lineIndex} в многострочной строке (0-based). */
	private static int offsetOf(CharSequence cs, int lineIndex) {
		int idx = 0;
		int line = 0;
		while (line < lineIndex && idx < cs.length()) {
			if (cs.charAt(idx) == '\n') {
				line++;
			}
			idx++;
		}
		return idx;
	}

	private static StyleRange[] append(StyleRange[] arr, StyleRange sr) {
		StyleRange[] out = new StyleRange[arr.length + 1];
		System.arraycopy(arr, 0, out, 0, arr.length);
		out[arr.length] = sr;
		return out;
	}

	private String statusText(String status) {
		if ("passed".equals(status)) {
			return "PASSED";
		}
		if ("failed".equals(status)) {
			return "FAILED";
		}
		return String.valueOf(status);
	}
}
