package com.platform.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-+|-+$");

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Normalize unicode characters
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert to lowercase
        String lower = normalized.toLowerCase(Locale.ROOT);

        // Replace non-alphanumeric with hyphens
        String alphanumeric = NON_ALPHANUMERIC.matcher(lower).replaceAll("-");

        // Collapse whitespace to single hyphens
        String collapsed = WHITESPACE.matcher(alphanumeric).replaceAll("-");

        // Collapse multiple hyphens
        String singleHyphens = MULTIPLE_HYPHENS.matcher(collapsed).replaceAll("-");

        // Trim leading/trailing hyphens
        return LEADING_TRAILING_HYPHENS.matcher(singleHyphens).replaceAll("");
    }

    public static String slugifyWithMaxLength(String input, int maxLength) {
        String slug = slugify(input);
        if (slug.length() <= maxLength) {
            return slug;
        }
        // Truncate at word boundary
        int lastHyphen = slug.lastIndexOf('-', maxLength);
        if (lastHyphen > 0) {
            return slug.substring(0, lastHyphen);
        }
        return slug.substring(0, maxLength);
    }

    public static String generateUniqueSlug(String base, java.util.function.Function<String, Boolean> existsCheck) {
        String slug = slugify(base);
        String originalSlug = slug;
        int counter = 1;

        while (existsCheck.apply(slug)) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}