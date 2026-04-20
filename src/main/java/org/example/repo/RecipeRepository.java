package org.example.repo;

import jakarta.annotation.PostConstruct;
import org.example.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class RecipeRepository {

    private final String dataDir;
    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeRepository(@Value("${app.data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void load() throws Exception {
        File file = new File(dataDir, "recipes.xml");
        if (!file.isFile()) {
            throw new IllegalStateException("recipes.xml not found at " + file.getAbsolutePath());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodes = doc.getElementsByTagName("recipe");
        recipes.clear();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            recipes.add(new Recipe(
                    element.getAttribute("id"),
                    text(element, "title"),
                    text(element, "cuisine"),
                    text(element, "difficulty"),
                    text(element, "source")
            ));
        }
    }

    public List<Recipe> findAll() {
        return Collections.unmodifiableList(recipes);
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? "" : list.item(0).getTextContent().trim();
    }
}
