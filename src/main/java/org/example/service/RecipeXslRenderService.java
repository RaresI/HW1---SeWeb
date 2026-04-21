package org.example.service;

import org.example.model.Recipe;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;
import java.util.List;

@Service
public class RecipeXslRenderService {

    public String render(List<Recipe> recipes, String userSkillLevel) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        Element root = document.createElement("recipes");
        document.appendChild(root);

        for (Recipe recipe : recipes) {
            Element recipeElement = document.createElement("recipe");
            recipeElement.setAttribute("id", recipe.getId());
            appendText(document, recipeElement, "title", recipe.getTitle());
            appendText(document, recipeElement, "cuisine", recipe.getCuisine());
            appendText(document, recipeElement, "difficulty", recipe.getDifficulty());
            root.appendChild(recipeElement);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer;
        try (java.io.InputStream xslStream = new ClassPathResource("xsl/recipes-table.xsl").getInputStream()) {
            transformer = transformerFactory.newTransformer(new StreamSource(xslStream));
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setParameter("userSkillLevel", userSkillLevel == null ? "" : userSkillLevel);

        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

    private static void appendText(Document document, Element parent, String tag, String value) {
        Element child = document.createElement(tag);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
    }
}
