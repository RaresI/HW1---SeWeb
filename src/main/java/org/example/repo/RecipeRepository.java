package org.example.repo;

import jakarta.annotation.PostConstruct;
import org.example.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class RecipeRepository {

    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private final String dataDir;
    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeRepository(@Value("${app.data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public synchronized void load() throws Exception {
        File file = recipesFile();
        if (!file.isFile()) {
            throw new IllegalStateException("recipes.xml not found at " + file.getAbsolutePath());
        }
        DocumentBuilderFactory factory = safeBuilderFactory();
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

    public synchronized Recipe save(Recipe incoming) throws Exception {
        Recipe recipe = new Recipe(
                nextId(),
                incoming.getTitle().trim(),
                incoming.getCuisine(),
                incoming.getDifficulty(),
                incoming.getSource() == null ? null : incoming.getSource().trim()
        );
        recipes.add(recipe);
        try {
            persist();
        } catch (Exception e) {
            recipes.remove(recipe);
            throw e;
        }
        return recipe;
    }

    private String nextId() {
        int max = 0;
        for (Recipe r : recipes) {
            String id = r.getId();
            if (id != null && id.length() > 1) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("r%02d", max + 1);
    }

    private void persist() throws Exception {
        DocumentBuilderFactory factory = safeBuilderFactory();
        Document doc = factory.newDocumentBuilder().newDocument();
        Element root = doc.createElement("recipes");
        root.setAttribute("xmlns:xsi", XSI);
        root.setAttributeNS(XSI, "xsi:noNamespaceSchemaLocation", "recipes.xsd");
        doc.appendChild(root);
        for (Recipe r : recipes) {
            Element recipe = doc.createElement("recipe");
            recipe.setAttribute("id", r.getId());
            appendText(doc, recipe, "title", r.getTitle());
            appendText(doc, recipe, "cuisine", r.getCuisine());
            appendText(doc, recipe, "difficulty", r.getDifficulty());
            if (r.getSource() != null && !r.getSource().isEmpty()) {
                appendText(doc, recipe, "source", r.getSource());
            }
            root.appendChild(recipe);
        }
        validateAgainstSchema(doc);
        writeDocument(doc, recipesFile());
    }

    private void validateAgainstSchema(Document doc) throws Exception {
        File xsd = new File(dataDir, "recipes.xsd");
        if (!xsd.isFile()) return;
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(xsd);
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(doc));
    }

    private static void writeDocument(Document doc, File file) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    private static void appendText(Document doc, Element parent, String tag, String value) {
        Element child = doc.createElement(tag);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    private File recipesFile() {
        return new File(dataDir, "recipes.xml");
    }

    private static DocumentBuilderFactory safeBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? "" : list.item(0).getTextContent().trim();
    }
}
