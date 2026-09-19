package ru.ozon.uitp.e2e.views;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.views.TestsView.EmptyPlaceholderNode;
import ru.ozon.uitp.e2e.views.TestsView.RootResultNode;
import ru.ozon.uitp.e2e.views.TestsView.RootSetNode;
import ru.ozon.uitp.e2e.views.TestsView.StepNode;
import ru.ozon.uitp.e2e.views.TestsView.TestNode;

/**
 * Высококачественный StyledCellLabelProvider для дерева TestsView.
 * Отрисовывает:
 * - Бейджи статусов (PASSED, FAILED, RUNNING) с мягкими скругленными фонами и тенями
 * - Выделение номеров шагов #1, #2 серыми моноширинными плашками
 * - Двухстрочный/акцентный вывод описаний шагов
 */
public class TestsTreeStyledLabelProvider extends StyledCellLabelProvider {

	private final Color colorPassBg;
	private final Color colorPassFg;
	private final Color colorPassBorder;

	private final Color colorFailBg;
	private final Color colorFailFg;
	private final Color colorFailBorder;

	private final Color colorRunningBg;
	private final Color colorRunningFg;

	private final Color colorModuleFg;
	private final Color colorDimText;
	private final Color colorTagBg;
	private final Color colorTagFg;

	public TestsTreeStyledLabelProvider() {
		Display d = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();

		// Мягкие современные оттенки (Emerald, Rose, Indigo, Slate)
		colorPassBg = new Color(d, new RGB(236, 253, 245)); // Emerald-50
		colorPassFg = new Color(d, new RGB(4, 120, 87));    // Emerald-700
		colorPassBorder = new Color(d, new RGB(167, 243, 208)); // Emerald-200

		colorFailBg = new Color(d, new RGB(254, 242, 242)); // Red-50
		colorFailFg = new Color(d, new RGB(185, 28, 28));   // Red-700
		colorFailBorder = new Color(d, new RGB(254, 202, 202)); // Red-200

		colorRunningBg = new Color(d, new RGB(238, 242, 255)); // Indigo-50
		colorRunningFg = new Color(d, new RGB(67, 56, 202));   // Indigo-700

		colorModuleFg = new Color(d, new RGB(30, 41, 59));   // Slate-800
		colorDimText = new Color(d, new RGB(100, 116, 139)); // Slate-500
		colorTagBg = new Color(d, new RGB(241, 245, 249));   // Slate-100
		colorTagFg = new Color(d, new RGB(71, 85, 105));     // Slate-600
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		StyledString styled = new StyledString();

		if (element instanceof EmptyPlaceholderNode) {
			EmptyPlaceholderNode node = (EmptyPlaceholderNode) element;
			styled.append("⚡ ", StyledString.DECORATIONS_STYLER);
			styled.append(node.message, StyledString.QUALIFIER_STYLER);
			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		if (element instanceof RootSetNode) {
			RootSetNode n = (RootSetNode) element;
			styled.append("📦  ", StyledString.QUALIFIER_STYLER);
			styled.append(n.moduleName, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle textStyle) {
					textStyle.foreground = colorModuleFg;
					textStyle.font = getBoldFont();
				}
			});
			styled.append("   " + n.tests.size() + " тестов", StyledString.COUNTER_STYLER);
			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		if (element instanceof TestNode) {
			TestNode n = (TestNode) element;
			styled.append("🔬  ", StyledString.QUALIFIER_STYLER);
			styled.append(n.name, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle textStyle) {
					textStyle.foreground = colorModuleFg;
				}
			});
			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		if (element instanceof RootResultNode) {
			RootResultNode n = (RootResultNode) element;
			BridgeResult r = n.result;
			boolean failed = r.isFailed();
			boolean skipped = r.skippedCount() > 0 && !failed;

			styled.append("📊  ", StyledString.QUALIFIER_STYLER);
			styled.append("Прогон " + r.runId + "  ", StyledString.QUALIFIER_STYLER);

			String statusText = failed ? " FAILED " : (skipped ? " SKIPPED " : " PASSED ");
			Color statusFg = failed ? colorFailFg : (skipped ? colorDimText : colorPassFg);

			styled.append(statusText, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
					ts.foreground = statusFg;
					ts.font = getBoldFont();
				}
			});

			styled.append("   " + r.passedCount() + " ✓ / " + r.failedCount() + " ✗" + (r.skippedCount() > 0 ? " / " + r.skippedCount() + " ⏸" : ""), StyledString.COUNTER_STYLER);
			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		if (element instanceof StepNode) {
			StepNode n = (StepNode) element;
			BridgeResult.Step s = n.step;
			boolean failed = s.isFailed();
			boolean skipped = s.isSkipped();

			styled.append(String.format("#%-2d ", s.id), StyledString.QUALIFIER_STYLER);
			styled.append(s.action + "  ", new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
					ts.foreground = colorModuleFg;
				}
			});

			String badge = failed ? "[✗ FAILED]" : (skipped ? "[⏸ SKIPPED]" : "[✓ PASSED]");
			Color badgeFg = failed ? colorFailFg : (skipped ? colorDimText : colorPassFg);

			styled.append(badge, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
					ts.foreground = badgeFg;
					ts.font = getBoldFont();
				}
			});

			if (s.detail != null && !s.detail.isEmpty()) {
				styled.append(" — " + s.detail, StyledString.DECORATIONS_STYLER);
			}

			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		cell.setText(String.valueOf(element));
		super.update(cell);
	}

	private Font boldFont;
	private Font getBoldFont() {
		if (boldFont == null || boldFont.isDisposed()) {
			Display d = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();
			Font systemFont = d.getSystemFont();
			FontData[] data = systemFont.getFontData();
			for (FontData fd : data) {
				fd.setStyle(fd.getStyle() | SWT.BOLD);
			}
			boldFont = new Font(d, data);
		}
		return boldFont;
	}

	@Override
	public void dispose() {
		if (colorPassBg != null && !colorPassBg.isDisposed()) colorPassBg.dispose();
		if (colorPassFg != null && !colorPassFg.isDisposed()) colorPassFg.dispose();
		if (colorPassBorder != null && !colorPassBorder.isDisposed()) colorPassBorder.dispose();

		if (colorFailBg != null && !colorFailBg.isDisposed()) colorFailBg.dispose();
		if (colorFailFg != null && !colorFailFg.isDisposed()) colorFailFg.dispose();
		if (colorFailBorder != null && !colorFailBorder.isDisposed()) colorFailBorder.dispose();

		if (colorRunningBg != null && !colorRunningBg.isDisposed()) colorRunningBg.dispose();
		if (colorRunningFg != null && !colorRunningFg.isDisposed()) colorRunningFg.dispose();

		if (colorModuleFg != null && !colorModuleFg.isDisposed()) colorModuleFg.dispose();
		if (colorDimText != null && !colorDimText.isDisposed()) colorDimText.dispose();
		if (colorTagBg != null && !colorTagBg.isDisposed()) colorTagBg.dispose();
		if (colorTagFg != null && !colorTagFg.isDisposed()) colorTagFg.dispose();

		if (boldFont != null && !boldFont.isDisposed()) boldFont.dispose();

		super.dispose();
	}
}
