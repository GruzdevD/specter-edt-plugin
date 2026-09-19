package ru.ozon.uitp.e2e.views;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Панель «Результаты» (Specter): пошаговый лог сценария моста на SWT StyledText.
 * Корректный расчет StyleRange и линейная O(N) генерация разметки.
 */
public class ResultsView extends ViewPart {

	public static final String ID = "ru.ozon.uitp.e2e.views.ResultsView";
	private final BridgeResultStore.Listener storeListener = this::showResult;
	private StyledText text;

	@Override
	public void createPartControl(Composite parent) {
		text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setEditable(false);
		BridgeResultStore.get().addListener(storeListener);
		showResult(BridgeResultStore.get().current());
		try {
			org.eclipse.jface.resource.ImageDescriptor desc = ru.ozon.uitp.e2e.Activator.imageDescriptorFromPlugin(
				ru.ozon.uitp.e2e.Activator.BUNDLE_ID, "icons/specter-results.png");
			if (desc != null) {
				setTitleImage(desc.createImage());
			}
		} catch (Throwable ignored) {
		}
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
		if (r == null || "hint".equals(r.status)) {
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

		String fullText = sb.toString();
		text.setText(fullText);

		Color green = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);

		String[] lines = fullText.split("\n", -1);
		List<StyleRange> ranges = new ArrayList<>();
		int lineStart = 0;

		for (String line : lines) {
			int statusIdx = line.indexOf("FAILED");
			boolean failed = true;
			int len = 6;
			if (statusIdx < 0) {
				statusIdx = line.indexOf("PASSED");
				failed = false;
				len = 6;
			}
			if (statusIdx >= 0) {
				StyleRange sr = new StyleRange();
				sr.start = lineStart + statusIdx; // Точное смещение слова статуса
				sr.length = len;
				sr.foreground = failed ? red : green;
				sr.fontStyle = SWT.BOLD;
				ranges.add(sr);
			}
			lineStart += line.length() + 1;
		}

		text.setStyleRanges(ranges.toArray(new StyleRange[0]));
	}

	private String statusText(String status) {
		if ("passed".equals(status)) return "PASSED";
		if ("failed".equals(status)) return "FAILED";
		return String.valueOf(status);
	}
}