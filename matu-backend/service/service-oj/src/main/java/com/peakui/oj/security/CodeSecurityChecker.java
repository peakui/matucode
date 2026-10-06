package com.peakui.oj.security;

import com.peakui.oj.exception.OjException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CodeSecurityChecker {

    private static final List<Pattern> COMMON_DANGEROUS_PATTERNS = List.of(
            Pattern.compile("(?i)\\bwget\\b\\s+"),
            Pattern.compile("(?i)\\bcurl\\b\\s+"),
            Pattern.compile("(?i)\\bnc\\b\\s+"),
            Pattern.compile("(?i)\\bnetcat\\b\\s+"),
            Pattern.compile("(?i)\\bchmod\\b\\s+"),
            Pattern.compile("(?i)\\brm\\s+-rf"),
            Pattern.compile("(?i)\\bmkfs\\b"),
            Pattern.compile("(?i)\\bshutdown\\b\\s+"),
            Pattern.compile("(?i)\\breboot\\b\\s*\\(")
    );

    private static final List<Pattern> JAVA_DANGEROUS_PATTERNS = List.of(
            Pattern.compile("Runtime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*exec", Pattern.CASE_INSENSITIVE),
            Pattern.compile("new\\s+ProcessBuilder\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ProcessBuilder\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("System\\s*\\.\\s*load\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("System\\s*\\.\\s*loadLibrary\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Class\\s*\\.\\s*forName\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("java\\.lang\\.reflect", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Files\\s*\\.\\s*delete", Pattern.CASE_INSENSITIVE),
            Pattern.compile("FileOutputStream\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("RandomAccessFile\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("Socket\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ServerSocket\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("URL\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("HttpClient", Pattern.CASE_INSENSITIVE),
            Pattern.compile("java\\.nio\\.file\\.Paths", Pattern.CASE_INSENSITIVE),
            Pattern.compile("java\\.io\\.File", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> PYTHON_DANGEROUS_PATTERNS = List.of(
            Pattern.compile("(?i)import\\s+os"),
            Pattern.compile("(?i)import\\s+subprocess"),
            Pattern.compile("(?i)import\\s+socket"),
            Pattern.compile("(?i)import\\s+requests"),
            Pattern.compile("(?i)os\\.system\\s*\\("),
            Pattern.compile("(?i)subprocess\\.(run|popen|call|check_output)\\s*\\("),
            Pattern.compile("(?i)eval\\s*\\("),
            Pattern.compile("(?i)exec\\s*\\("),
            Pattern.compile("(?i)open\\s*\\("),
            Pattern.compile("(?i)__import__\\s*\\(")
    );

    private static final List<Pattern> CPP_DANGEROUS_PATTERNS = List.of(
            Pattern.compile("system\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("popen\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fork\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("execv?[ep]?\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("CreateProcess\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("WinExec\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("#include\\s*<windows\\.h>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("#include\\s*<winsock", Pattern.CASE_INSENSITIVE),
            Pattern.compile("#include\\s*<sys/socket", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fstream", Pattern.CASE_INSENSITIVE)
    );

    public void checkOrThrow(String language, String code) {
        if (!StringUtils.hasText(code)) {
            throw new OjException("提交代码不能为空");
        }
        String normalizedLanguage = normalizeLanguage(language);
        for (Pattern pattern : COMMON_DANGEROUS_PATTERNS) {
            if (pattern.matcher(code).find()) {
                throw new OjException("提交代码包含危险操作，已被系统拦截");
            }
        }
        List<Pattern> patterns = switch (normalizedLanguage) {
            case "java" -> JAVA_DANGEROUS_PATTERNS;
            case "python" -> PYTHON_DANGEROUS_PATTERNS;
            case "cpp", "c", "c++" -> CPP_DANGEROUS_PATTERNS;
            default -> List.of();
        };
        for (Pattern pattern : patterns) {
            if (pattern.matcher(code).find()) {
                throw new OjException("提交代码存在高风险调用，已拒绝提交");
            }
        }
    }

    private String normalizeLanguage(String language) {
        return language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
    }
}
