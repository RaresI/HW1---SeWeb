package org.example.service;

import org.example.model.Recipe;
import org.example.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RecommendationService {

    private final String dataDir;
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public RecommendationService(@Value("${app.data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    public Optional<User> firstUser() throws Exception {
        Document doc = parse(new File(dataDir, "users.xml"));
        XPath xPath = xPathFactory.newXPath();
        XPathExpression expr = xPath.compile("/users/user[1]");
        Element element = (Element) expr.evaluate(doc, XPathConstants.NODE);
        if (element == null) {
            return Optional.empty();
        }
        return Optional.of(new User(
                element.getAttribute("id"),
                xPathString(xPath, element, "name"),
                xPathString(xPath, element, "surname"),
                xPathString(xPath, element, "skillLevel"),
                xPathString(xPath, element, "preferredCuisine")
        ));
    }

    public List<Recipe> recipesForSkillLevel(String skillLevel) throws Exception {
        Document doc = parse(new File(dataDir, "recipes.xml"));
        XPath xPath = xPathFactory.newXPath();
        xPath.setXPathVariableResolver(new SingleVarResolver("skill", skillLevel));
        XPathExpression expr = xPath.compile("/recipes/recipe[difficulty = $skill]");
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<Recipe> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            out.add(new Recipe(
                    e.getAttribute("id"),
                    xPathString(xPath, e, "title"),
                    xPathString(xPath, e, "cuisine"),
                    xPathString(xPath, e, "difficulty"),
                    xPathString(xPath, e, "source")
            ));
        }
        return out;
    }

    public List<Recipe> recipesForSkillAndCuisine(String skillLevel, String preferredCuisine) throws Exception {
        Document doc = parse(new File(dataDir, "recipes.xml"));
        XPath xPath = xPathFactory.newXPath();
        xPath.setXPathVariableResolver(variableName -> {
            String localName = variableName.getLocalPart();
            if ("skill".equals(localName)) {
                return skillLevel;
            }
            if ("cuisine".equals(localName)) {
                return preferredCuisine;
            }
            return null;
        });

        XPathExpression expr = xPath.compile("/recipes/recipe[difficulty = $skill and cuisine = $cuisine]");
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<Recipe> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            out.add(new Recipe(
                    e.getAttribute("id"),
                    xPathString(xPath, e, "title"),
                    xPathString(xPath, e, "cuisine"),
                    xPathString(xPath, e, "difficulty"),
                    xPathString(xPath, e, "source")
            ));
        }
        return out;
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

    private static final class SingleVarResolver implements javax.xml.xpath.XPathVariableResolver {
        private final String name;
        private final Object value;

        SingleVarResolver(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public Object resolveVariable(javax.xml.namespace.QName variableName) {
            return name.equals(variableName.getLocalPart()) ? value : null;
        }
    }
}
