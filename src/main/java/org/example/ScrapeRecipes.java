package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class ScrapeRecipes {

    private static final String SOURCE_URL =
            "https://www.bbcgoodfood.com/recipes/collection/budget-autumn";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36";
    private static final List<String> CUISINE_TYPES = List.of("Italian", "Asian");
    private static final List<String> DIFFICULTY_LEVELS = List.of("Easy", "Medium", "Hard");
    private static final int MIN_RECIPES = 20;
    private static final String CARD_SELECTOR =
            "a[data-testid=card-image-container][href*=/recipes/]";
    private static final String ARIA_PREFIX = "View ";

    private ScrapeRecipes() {}

    public static void main(String[] args) throws Exception {
        Path outputDir = Path.of("").toAbsolutePath().resolve("data");
        Document document = Jsoup.connect(SOURCE_URL)
                .userAgent(USER_AGENT)
                .timeout(30_000)
                .get();
        List<Recipe> recipes = extract(document);
        if (recipes.size() < MIN_RECIPES) {
            System.err.printf("ERROR: only %d recipes found, need at least %d.%n",
                    recipes.size(), MIN_RECIPES);
            System.exit(1);
        }
        assignRandomAttributes(recipes);
        Path outputFile = outputDir.resolve("recipes.xml");
        Files.createDirectories(outputDir);
        Files.writeString(outputFile, renderXml(recipes));
        System.out.printf("Wrote %d recipes to %s%n", recipes.size(), outputFile);
    }

    private static List<Recipe> extract(Document document) {
        Elements cards = document.select(CARD_SELECTOR);
        Map<String, String> seen = new LinkedHashMap<>();
        for (Element card : cards) {
            String href = card.attr("href");
            String slug = extractSlug(href);
            if (slug.isEmpty() || seen.containsKey(slug)) continue;
            String title = card.attr("aria-label");
            if (title.startsWith(ARIA_PREFIX)) {
                title = title.substring(ARIA_PREFIX.length());
            }
            title = title.trim();
            if (title.isEmpty()) continue;
            seen.put(slug, title);
        }
        List<Recipe> recipes = new ArrayList<>(seen.size());
        int index = 1;
        for (Map.Entry<String, String> entry : seen.entrySet()) {
            recipes.add(new Recipe(
                    String.format("r%02d", index++),
                    entry.getValue(),
                    entry.getKey()));
        }
        return recipes;
    }

    private static String extractSlug(String href) {
        int marker = href.indexOf("/recipes/");
        if (marker < 0) return "";
        String tail = href.substring(marker + "/recipes/".length());
        int slash = tail.indexOf('/');
        if (slash >= 0) tail = tail.substring(0, slash);
        int query = tail.indexOf('?');
        if (query >= 0) tail = tail.substring(0, query);
        return tail;
    }

    private static void assignRandomAttributes(List<Recipe> recipes) {
        Random rng = new Random();
        for (Recipe recipe : recipes) {
            recipe.cuisine = CUISINE_TYPES.get(rng.nextInt(CUISINE_TYPES.size()));
            recipe.difficulty = DIFFICULTY_LEVELS.get(rng.nextInt(DIFFICULTY_LEVELS.size()));
        }
    }

    private static String renderXml(List<Recipe> recipes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<recipes xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:noNamespaceSchemaLocation=\"recipes.xsd\">\n");
        for (Recipe recipe : recipes) {
            sb.append("  <recipe id=\"").append(recipe.id).append("\">\n");
            sb.append("    <title>").append(xmlEscape(recipe.title)).append("</title>\n");
            sb.append("    <cuisine>").append(xmlEscape(recipe.cuisine)).append("</cuisine>\n");
            sb.append("    <difficulty>").append(xmlEscape(recipe.difficulty)).append("</difficulty>\n");
            sb.append("    <source>").append(xmlEscape(recipe.slug)).append("</source>\n");
            sb.append("  </recipe>\n");
        }
        sb.append("</recipes>\n");
        return sb.toString();
    }

    private static String xmlEscape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class Recipe {
        final String id;
        final String title;
        final String slug;
        String cuisine;
        String difficulty;

        Recipe(String id, String title, String slug) {
            this.id = id;
            this.title = title;
            this.slug = slug;
        }
    }
}
