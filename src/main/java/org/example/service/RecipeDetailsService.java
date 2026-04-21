package org.example.service;

import org.example.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.Optional;

@Service
public class RecipeDetailsService {

    private final String dataDir;
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public RecipeDetailsService(@Value("${app.data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    public Optional<Recipe> findById(String recipeId) throws Exception {
        Document doc = parse(new File(dataDir, "recipes.xml"));
        XPath xPath = xPathFactory.newXPath();
        xPath.setXPathVariableResolver(variableName -> "id".equals(variableName.getLocalPart()) ? recipeId : null);
        XPathExpression expr = xPath.compile("/recipes/recipe[@id = $id]");
        Element element = (Element) expr.evaluate(doc, XPathConstants.NODE);
        if (element == null) {
            return Optional.empty();
        }
        return Optional.of(new Recipe(
                element.getAttribute("id"),
                xPathString(xPath, element, "title"),
                xPathString(xPath, element, "cuisine"),
                xPathString(xPath, element, "difficulty"),
                xPathString(xPath, element, "source")
        ));
    }

    private static String xPathString(XPath xPath, Element ctx, String childName) throws Exception {
        return xPath.compile(childName + "/text()").evaluate(ctx).trim();
    }

    private static Document parse(File file) throws Exception {
        if (!file.isFile()) {
            throw new IllegalStateException("Missing XML file: " + file.getAbsolutePath());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(file);
    }
}
