package com.peakui.mcp.webfetch;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Crude HTML-to-text conversion: plenty for feeding a model, no parser dependency. */
public final class HtmlTextExtractor {

    private static final Pattern TITLE = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script\\b[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style\\b[^>]*>.*?</style>");
    private static final Pattern COMMENT = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern BLOCK_END = Pattern.compile("(?i)</(p|div|li|tr|h[1-6]|section|article|header|footer|blockquote)\\s*>");
    private static final Pattern BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]*>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");
    private static final Pattern TRAILING_SPACE = Pattern.compile("[ \\t]+\\n");
    private static final Pattern LEADING_SPACE = Pattern.compile("\\n[ \\t]+");

    private HtmlTextExtractor() {
    }

    public static String extract(byte[] body, Charset charset) {
        String html = new String(body, charset);
        String title = firstGroup(TITLE, html);

        String text = COMMENT.matcher(html).replaceAll(" ");
        text = SCRIPT.matcher(text).replaceAll(" ");
        text = STYLE.matcher(text).replaceAll(" ");
        text = BREAK.matcher(text).replaceAll("\n");
        text = BLOCK_END.matcher(text).replaceAll("\n");
        text = TAG.matcher(text).replaceAll(" ");
        text = decodeEntities(text);
        text = TRAILING_SPACE.matcher(text).replaceAll("\n");
        text = LEADING_SPACE.matcher(text).replaceAll("\n");
        text = BLANK_LINES.matcher(text).replaceAll("\n\n");
        text = text.trim();

        if (title != null && !title.isBlank() && !text.startsWith(title)) {
            return title.trim() + "\n\n" + text;
        }
        return text;
    }

    private static String firstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? decodeEntities(matcher.group(1)).trim() : null;
    }

    private static String decodeEntities(String input) {
        String text = NUMERIC_ENTITY.matcher(input).replaceAll(match -> {
            String digits = match.group(2);
            int radix = match.group(1).isEmpty() ? 10 : 16;
            try {
                return String.valueOf(Character.toChars(Integer.parseInt(digits, radix)));
            } catch (NumberFormatException e) {
                return " ";
            }
        });
        return text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }
}
