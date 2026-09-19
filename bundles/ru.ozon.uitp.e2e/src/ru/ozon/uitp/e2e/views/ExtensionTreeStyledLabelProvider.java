package ru.ozon.uitp.e2e.views;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import ru.ozon.uitp.e2e.views.ExtensionTestsView.ModuleNode;
import ru.ozon.uitp.e2e.views.ExtensionTestsView.TestItemNode;

/**
 * Стилизованный рендерер для дерева ExtensionTestsView:
 * - Модули отрисовываются с выделенным жирным шрифтом и счетчиком тестов
 * - Тесты показывают статус: PASSED (зеленый бейдж), FAILED (красный), RUNNING (фиолетовый с анимацией индикатора), IDLE
 * - Описание отображается приглушенным цветом (Secondary text)
 */
public class ExtensionTreeStyledLabelProvider extends StyledCellLabelProvider {

	private final Color colorPassFg;
	private final Color colorFailFg;
	private final Color colorRunningFg;
	private final Color colorModuleFg;
	private final Color colorDimText;
	private final Color colorCategoryBg;
	private final Color colorCategoryFg;

	private Font boldFont;

	public ExtensionTreeStyledLabelProvider() {
		Display d = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();

		colorPassFg = new Color(d, new RGB(5, 150, 105));     // Emerald-600
		colorFailFg = new Color(d, new RGB(220, 38, 38));     // Red-600
		colorRunningFg = new Color(d, new RGB(99, 102, 241)); // Indigo-500
		colorModuleFg = new Color(d, new RGB(30, 41, 59));    // Slate-800
		colorDimText = new Color(d, new RGB(100, 116, 139));  // Slate-500
		colorCategoryBg = new Color(d, new RGB(241, 245, 249)); // Slate-100
		colorCategoryFg = new Color(d, new RGB(71, 85, 105));   // Slate-600
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		StyledString styled = new StyledString();

		if (element instanceof ModuleNode) {
			ModuleNode m = (ModuleNode) element;
			styled.append("📦  ", StyledString.QUALIFIER_STYLER);
			styled.append(m.moduleName, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
					ts.foreground = colorModuleFg;
					ts.font = getBoldFont();
				}
			});

			styled.append("  [" + m.tests.size() + " тестов]", StyledString.COUNTER_STYLER);
			if (m.description != null && !m.description.isEmpty()) {
				styled.append(" — " + m.description, StyledString.DECORATIONS_STYLER);
			}
			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		if (element instanceof TestItemNode) {
			TestItemNode t = (TestItemNode) element;
			styled.append("🔬  ", StyledString.QUALIFIER_STYLER);
			styled.append(t.name, new Styler() {
				@Override
				public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
					ts.foreground = colorModuleFg;
				}
			});

			if ("passed".equals(t.status)) {
				styled.append("  [✓ PASSED]", new Styler() {
					@Override
					public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
						ts.foreground = colorPassFg;
						ts.font = getBoldFont();
					}
				});
			} else if ("failed".equals(t.status)) {
				styled.append("  [✗ FAILED]", new Styler() {
					@Override
					public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
						ts.foreground = colorFailFg;
						ts.font = getBoldFont();
					}
				});
			} else if ("running".equals(t.status)) {
				styled.append("  [⟳ ВЫПОЛНЯЕТСЯ...]", new Styler() {
					@Override
					public void applyStyles(org.eclipse.swt.graphics.TextStyle ts) {
						ts.foreground = colorRunningFg;
						ts.font = getBoldFont();
					}
				});
			}

			if (t.description != null && !t.description.isEmpty()) {
				styled.append(" — " + t.description, StyledString.DECORATIONS_STYLER);
			}

			cell.setText(styled.getString());
			cell.setStyleRanges(styled.getStyleRanges());
			super.update(cell);
			return;
		}

		cell.setText(String.valueOf(element));
		super.update(cell);
	}

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
		if (colorPassFg != null && !colorPassFg.isDisposed()) colorPassFg.dispose();
		if (colorFailFg != null && !colorFailFg.isDisposed()) colorFailFg.dispose();
		if (colorRunningFg != null && !colorRunningFg.isDisposed()) colorRunningFg.dispose();
		if (colorModuleFg != null && !colorModuleFg.isDisposed()) colorModuleFg.dispose();
		if (colorDimText != null && !colorDimText.isDisposed()) colorDimText.dispose();
		if (colorCategoryBg != null && !colorCategoryBg.isDisposed()) colorCategoryBg.dispose();
		if (colorCategoryFg != null && !colorCategoryFg.isDisposed()) colorCategoryFg.dispose();
		if (boldFont != null && !boldFont.isDisposed()) boldFont.dispose();
		super.dispose();
	}
}
