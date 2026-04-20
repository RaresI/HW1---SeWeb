# Semantic Web — Big HW1

Java web app for recipe recommendations built around XML, DTD/XSD, XSL and XPath/XQuery. The assignment brief lives in `Semantic Web Big HW1.pdf`; a summary of the tasks is in `Semantic_Web_HW1.md`.

## Team

- George-Alexandru Petre
- Ionescu Rares-Andrei

## Task 1 — George

> Create input data for at least 20 recipes in XML format and 1 user. You can add the recipes manually in your xml file, or you can scrape them from https://www.bbcgoodfood.com/recipes/collection/budget-autumn
> - 0.5 points for manual input.
> - 1.5 points for scraping from a website (scrape only the title, the cuisine types and difficulty levels will be added by you randomly e.g. arrays with predefined values that will be assigned randomly for each recipe)

`org.example.ScrapeRecipes` fetches the BBC Good Food "Budget autumn" collection page
(`https://www.bbcgoodfood.com/recipes/collection/budget-autumn`) with **Jsoup**, selects
each recipe card with the CSS selector `a[data-testid=card-image-container][href*=/recipes/]`,
reads the title from the anchor's `aria-label`, and writes `data/recipes.xml`. Each recipe
is assigned:

- one cuisine from the predefined set `["Italian", "Asian"]`
- one difficulty from the predefined set `["Easy", "Medium", "Hard"]`

Assignments use `new Random()` so the cuisine/difficulty values differ on every run.

The scrape currently yields 21 recipes, satisfying the "at least 20" requirement. A single
user is stored in `data/users.xml` with `name`, `surname`, `skillLevel` and
`preferredCuisine` fields matching the schema the app will consume in later tasks.

Example:

```xml
<recipe id="r14">
    <title>Mushroom &amp; sausage pasta</title>
    <cuisine>Italian</cuisine>
    <difficulty>Medium</difficulty>
    <source>mushroom-sausage-pasta</source>
</recipe>
```

## Task 2 — George

> Create DTD/XSD for your data. (0.5 points)

We went with **XSD** rather than DTD for two reasons:

1. **Enumerations.** The cuisine (`Italian`/`Asian`) and difficulty / skill level
   (`Easy`/`Medium`/`Hard`) sets are closed. XSD enforces this with `xs:enumeration`;
   DTD can only declare the element exists but not restrict its text.
2. **Reuse in later tasks.** Tasks 4 and 5 require validating user-submitted form
   input before writing to XML — we can feed the same `.xsd` straight into
   `javax.xml.validation.SchemaFactory` instead of hand-rolling a validator, and
   IntelliJ gives real-time autocomplete / error squiggles on the XML files while
   we author them.

Schemas live next to the data:

- `data/recipes.xsd` — enforces the recipe structure, the cuisine and difficulty enums,
  required `id` attribute matching `r[0-9]{2,}`, and uniqueness of recipe ids.
- `data/users.xsd` — same idea for users: required `id` matching `u[0-9]{2,}`,
  `skillLevel` and `preferredCuisine` restricted to their enumerations.

Each XML file points at its schema via `xsi:noNamespaceSchemaLocation`, so
`xmllint --noout --schema data/recipes.xsd data/recipes.xml` (and the users equivalent)
both pass, and IntelliJ auto-associates them for live validation.

## Task 3 — George

> Read in memory the list of recipes from your local XML file and also show them in your developed UI. (0.5 points)

The webapp is **Spring Boot 3 + Thymeleaf** (embedded Tomcat — no external servlet
container to install). Layout:

- `org.example.Application` — Spring Boot entry point.
- `org.example.model.Recipe` — POJO.
- `org.example.repo.RecipeRepository` — `@Repository` that parses `data/recipes.xml` via
  `DocumentBuilderFactory` once at startup (`@PostConstruct`) and keeps the list in memory.
  Exposes `findAll()` (unmodifiable) so later tasks layer on top.
- `org.example.web.RecipeController` — `@Controller` mapping `GET /` and `GET /recipes`
  to the `recipes.html` Thymeleaf template.
- `src/main/resources/application.properties` — `app.data.dir=./data` (externally
  configurable path, so tasks 4 & 5 can write back to the same XML).

### Run

From the repo root:

```
mvn spring-boot:run
```

Then open <http://localhost:8080/>.

## Task 4 — George

> Create a form in which users can add a recipe. Add the recipe to your XML list (in memory) and save it to your local XML file. Validate the input before saving. (0.5 points)

- `GET /recipes/new` serves an HTML form (`templates/recipe-form.html`) with a text input
  for the title, dropdowns for cuisine and difficulty (populated from the same
  `Recipe.CUISINES` / `Recipe.DIFFICULTIES` constants the scraper uses), and an optional
  source field.
- `POST /recipes` runs **Bean Validation** (`@Valid` on the `Recipe` model —
  `@NotBlank`/`@Size`/`@Pattern`) via `spring-boot-starter-validation`. On validation
  errors the form is re-rendered with field-level messages and the user's input preserved;
  on success the controller uses PRG (Post-Redirect-Get) with a flash message back on the
  list page.
- `RecipeRepository.save` assigns the next `rNN` id, appends to the in-memory list, rebuilds
  the XML DOM, **validates the new document against `data/recipes.xsd`** (reusing the Task 2
  schema), and only then writes to disk — so both runtime and on-disk state stay in sync
  with the schema. The operation is `synchronized` and rolls back the in-memory insert if
  the write or schema check fails.

## Task 5 — George

> Create a form to insert user data and save it to your XML. (1 point)

- `GET /users` lists the users loaded from `data/users.xml` (parsed once at startup
  by `org.example.repo.UserRepository` via `DocumentBuilderFactory`, same pattern as
  `RecipeRepository` from Task 3).
- `GET /users/new` serves an HTML form (`templates/user-form.html`) with text inputs
  for name and surname, plus dropdowns for `skillLevel` and `preferredCuisine` populated
  from `User.SKILL_LEVELS` / `User.PREFERRED_CUISINES` (which reuse the same enumerations
  as the recipe model so the two never drift apart).
- `POST /users` runs **Bean Validation** (`@Valid` on the `User` model —
  `@NotBlank`/`@Size`/`@Pattern`). On validation errors the form is re-rendered with
  field-level messages and the submitted values preserved; on success the controller
  uses PRG (Post-Redirect-Get) with a flash message back on the list page.
- `UserRepository.save` assigns the next `uNN` id, appends to the in-memory list,
  rebuilds the XML DOM, **validates the new document against `data/users.xsd`** (reusing
  the Task 2 schema, so the enumerations and id pattern are enforced a second time at
  the XML layer), and only then writes to disk. The operation is `synchronized` and
  rolls back the in-memory insert if the write or schema check fails.
- The recipes and users pages are cross-linked via a top nav so both lists are
  reachable from either screen.

## Task 6

> Recommend recipes to the user based on their cooking skill level. Use XPath/XQuery and select the first user from your XML file. (1 point)

## Task 7

> Recommend recipes based on both cooking skill level and preferred cuisine type. Use XPath/XQuery and select the first user from your XML file. (1 point)

## Task 8

> Display the list of recipes from your local XML file/database on the web interface. First, read it into memory and then display it using XSL.
> - Recipes that match the user's cooking skill level should have a yellow background, and others should have a green background. (1 point)

## Task 9

> Allow the user to see all the details of a specific recipe. Use XPath/XQuery to filter and display the data. (1 point)

## Task 10

> Provide the list of recipes matching a specific cuisine type. The user can select a cuisine type from a given set of options. Use XPath/XQuery to retrieve the recipes. (1 point)

## Task 11

> Create a nice and intuitive graphical interface for your application. (For full grade the UI must be intuitive: in exercise 8 I want to be able to select a certain user — just an example, you can add other details you consider nice to have.) (1 point)
