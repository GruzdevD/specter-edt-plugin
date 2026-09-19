package ru.ozon.uitp.e2e.converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ru.ozon.uitp.e2e.Activator;

/**
 * Менеджер сканирования каталога Vanessa Automation и конвертации сценариев
 * в новые общие модули расширения СП_Тестирование в 1C:EDT.
 */
public class VanessaConversionManager {

	private final VanessaFeatureParser parser = new VanessaFeatureParser();
	private final VanessaToBslConverter converter = new VanessaToBslConverter();

	/**
	 * Сканирует каталог, заданный в настройках SpecterPreferencePage, на наличие .feature тестов.
	 */
	public List<VanessaScenario> scanConfiguredVanessaTests() {
		String configuredPath = Activator.getVanessaTestsPath();
		if (configuredPath == null || configuredPath.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return scanDirectory(new File(configuredPath));
	}

	/**
	 * Сканирует произвольный каталог на наличие .feature тестов.
	 */
	public List<VanessaScenario> scanDirectory(File directory) {
		List<VanessaScenario> result = new ArrayList<>();
		if (directory == null || !directory.exists() || !directory.isDirectory()) {
			return result;
		}

		scanDirectoryRecursive(directory, result);
		return result;
	}

	private void scanDirectoryRecursive(File dir, List<VanessaScenario> result) {
		File[] files = dir.listFiles();
		if (files == null) return;

		for (File f : files) {
			if (f.isDirectory()) {
				// Пропускаем служебные папки
				if (!f.getName().startsWith(".") && !f.getName().equalsIgnoreCase("node_modules")) {
					scanDirectoryRecursive(f, result);
				}
			} else if (f.isFile() && f.getName().toLowerCase().endsWith(".feature")) {
				try {
					List<VanessaScenario> scenarios = parser.parseFeatureFile(f);
					result.addAll(scenarios);
				} catch (Exception e) {
					Activator.logError("Ошибка разбора Vanessa feature файла: " + f.getAbsolutePath(), e);
				}
			}
		}
	}

	/**
	 * Конвертирует выбранные сценарии в общие модули расширения 1С (CommonModules).
	 *
	 * @param scenarios список сценариев
	 * @param extensionProjectRoot корень проекта расширения 1С (где src/CommonModules или CommonModules)
	 * @return количество успешно созданных модулей
	 */
	public int convertSelectedScenarios(List<VanessaScenario> scenarios, File extensionProjectRoot) throws IOException {
		if (extensionProjectRoot == null) {
			throw new IOException("Не указан каталог целевого проекта расширения 1С:EDT.");
		}

		if (!extensionProjectRoot.exists()) {
			extensionProjectRoot.mkdirs();
		}

		int convertedCount = 0;
		File commonModulesDir = resolveCommonModulesDir(extensionProjectRoot);
		if (!commonModulesDir.exists()) {
			commonModulesDir.mkdirs();
		}

		File configMdoFile = resolveConfigurationMdoFile(extensionProjectRoot);
		List<String> newModuleNames = new ArrayList<>();

		for (VanessaScenario scenario : scenarios) {
			if (!scenario.isSelected()) {
				continue;
			}

			String moduleName = scenario.getTargetModuleName();
			File moduleDir = new File(commonModulesDir, moduleName);
			if (!moduleDir.exists()) {
				moduleDir.mkdirs();
			}

			// 1. Создаем Module.bsl
			File bslFile = new File(moduleDir, "Module.bsl");
			String bslCode = converter.generateBslModuleCode(scenario);
			writeFile(bslFile, bslCode);

			// 2. Создаем <ModuleName>.mdo
			File mdoFile = new File(moduleDir, moduleName + ".mdo");
			String mdoXml = converter.generateMdoMetadata(moduleName);
			writeFile(mdoFile, mdoXml);

			newModuleNames.add(moduleName);
			convertedCount++;
		}

		// 3. Регистрируем созданные модули в Configuration.mdo если файл конфигурации найден
		if (configMdoFile != null && configMdoFile.exists() && !newModuleNames.isEmpty()) {
			registerModulesInConfiguration(configMdoFile, newModuleNames);
		}

		return convertedCount;
	}

	private File resolveCommonModulesDir(File projectRoot) {
		File srcCommonModules = new File(projectRoot, "src/CommonModules");
		if (srcCommonModules.exists()) {
			return srcCommonModules;
		}
		File directCommonModules = new File(projectRoot, "CommonModules");
		if (directCommonModules.exists()) {
			return directCommonModules;
		}
		// По умолчанию для 1C:EDT используем src/CommonModules
		return srcCommonModules;
	}

	private File resolveConfigurationMdoFile(File projectRoot) {
		File f1 = new File(projectRoot, "src/Configuration/Configuration.mdo");
		if (f1.exists()) return f1;
		File f2 = new File(projectRoot, "Configuration/Configuration.mdo");
		if (f2.exists()) return f2;
		File f3 = new File(projectRoot, "src/Configuration.mdo");
		if (f3.exists()) return f3;
		return f1;
	}

	private void registerModulesInConfiguration(File configMdoFile, List<String> moduleNames) {
		try {
			String content = new String(Files.readAllBytes(configMdoFile.toPath()), StandardCharsets.UTF_8);
			StringBuilder modulesXml = new StringBuilder();

			for (String mod : moduleNames) {
				String entry = "<commonModules>CommonModule." + mod + "</commonModules>";
				if (!content.contains(entry)) {
					modulesXml.append("  ").append(entry).append("\n");
				}
			}

			if (modulesXml.length() > 0) {
				if (content.contains("</mdclass:Configuration>")) {
					content = content.replace("</mdclass:Configuration>", modulesXml.toString() + "</mdclass:Configuration>");
					writeFile(configMdoFile, content);
				}
			}
		} catch (Exception e) {
			Activator.logError("Не удалось обновить Configuration.mdo", e);
		}
	}

	private void writeFile(File file, String content) throws IOException {
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(content);
		}
	}

	public VanessaToBslConverter getConverter() {
		return converter;
	}

	public VanessaFeatureParser getParser() {
		return parser;
	}
}
