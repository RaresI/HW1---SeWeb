package org.example.repo;

import jakarta.annotation.PostConstruct;
import org.example.model.User;
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
import java.util.Optional;

@Repository
public class UserRepository {

    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private final String dataDir;
    private final List<User> users = new ArrayList<>();

    public UserRepository(@Value("${app.data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public synchronized void load() throws Exception {
        File file = usersFile();
        if (!file.isFile()) {
            throw new IllegalStateException("users.xml not found at " + file.getAbsolutePath());
        }
        DocumentBuilderFactory factory = safeBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        NodeList nodes = doc.getElementsByTagName("user");
        users.clear();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            users.add(new User(
                    element.getAttribute("id"),
                    text(element, "name"),
                    text(element, "surname"),
                    text(element, "skillLevel"),
                    text(element, "preferredCuisine")
            ));
        }
    }

    public List<User> findAll() {
        return Collections.unmodifiableList(users);
    }

    public Optional<User> findById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        for (User user : users) {
            if (userId.equals(user.getId())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public synchronized User save(User incoming) throws Exception {
        User user = new User(
                nextId(),
                incoming.getName().trim(),
                incoming.getSurname().trim(),
                incoming.getSkillLevel(),
                incoming.getPreferredCuisine()
        );
        users.add(user);
        try {
            persist();
        } catch (Exception e) {
            users.remove(user);
            throw e;
        }
        return user;
    }

    private String nextId() {
        int max = 0;
        for (User u : users) {
            String id = u.getId();
            if (id != null && id.length() > 1) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("u%02d", max + 1);
    }

    private void persist() throws Exception {
        DocumentBuilderFactory factory = safeBuilderFactory();
        Document doc = factory.newDocumentBuilder().newDocument();
        Element root = doc.createElement("users");
        root.setAttribute("xmlns:xsi", XSI);
        root.setAttributeNS(XSI, "xsi:noNamespaceSchemaLocation", "users.xsd");
        doc.appendChild(root);
        for (User u : users) {
            Element user = doc.createElement("user");
            user.setAttribute("id", u.getId());
            appendText(doc, user, "name", u.getName());
            appendText(doc, user, "surname", u.getSurname());
            appendText(doc, user, "skillLevel", u.getSkillLevel());
            appendText(doc, user, "preferredCuisine", u.getPreferredCuisine());
            root.appendChild(user);
        }
        validateAgainstSchema(doc);
        writeDocument(doc, usersFile());
    }

    private void validateAgainstSchema(Document doc) throws Exception {
        File xsd = new File(dataDir, "users.xsd");
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

    private File usersFile() {
        return new File(dataDir, "users.xml");
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
