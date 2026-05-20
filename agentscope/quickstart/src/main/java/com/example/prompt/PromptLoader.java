package com.example.prompt;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt loader, used to load prompt templates from file system
 */
@Slf4j
public class PromptLoader {
	private static final String PROMPT_PATH_PREFIX = "prompts/";

	private static final ConcurrentHashMap<String, String> promptCache = new ConcurrentHashMap<>();

	/**
	 * Load prompt template from file
	 * @param promptName prompt file name (without path and extension)
	 * @return prompt content
	 */
	public static String loadPrompt(String promptName) {
		return promptCache.computeIfAbsent(promptName, name -> {
			String fileName = PROMPT_PATH_PREFIX + name + ".txt";
			// 使用本类的类加载器获取资源（避免jar包中无法获取资源）
			try (InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(fileName)) {
				return copyToString(inputStream, StandardCharsets.UTF_8);
			}
			catch (IOException e) {
				log.error("加载提示词失败！{}", e.getMessage(), e);
				throw new RuntimeException("加载提示词失败: " + name, e);
			}
		});
	}

	/**
	 * Clear prompt cache
	 */
	public static void clearCache() {
		promptCache.clear();
	}

	/**
	 * Get cache size
	 * @return number of prompts in cache
	 */
	public static int getCacheSize() {
		return promptCache.size();
	}

    private static String copyToString(InputStream in, Charset charset) throws IOException {
        if (in == null) {
            return "";
        } else {
            StringBuilder out = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(in, charset);
            char[] buffer = new char[8192];

            int charsRead;
            while((charsRead = reader.read(buffer)) != -1) {
                out.append(buffer, 0, charsRead);
            }

            return out.toString();
        }
    }
}
