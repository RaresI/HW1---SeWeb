# Semantic Web — Big HW1

Java web app for recipe recommendations built around XML, DTD/XSD, XSL and XPath/XQuery. 

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

## Task 6 — George

> Recommend recipes to the user based on their cooking skill level. Use XPath/XQuery and select the first user from your XML file. (1 point)

- New page at `GET /recommendations` (`templates/recommendations.html`) with a top card
  for the selected user and a table below of the recipes that match their skill level.
  A nav entry is added on the recipes and users pages so the view is reachable from
  every screen.
- `org.example.service.RecommendationService` does the XPath work via
  `javax.xml.xpath` (no extra dependencies — `XPathFactory` / `XPath` ship with the JDK,
  the same plumbing Task 2 already uses for schema validation):
  - **First user** — XPath `/users/user[1]` against `data/users.xml`, evaluated as
    `XPathConstants.NODE`. The resulting `<user>` element is unwrapped into the existing
    `User` POJO (id, name, surname, skillLevel, preferredCuisine) by running
    `name/text()` etc. as sub-XPaths on the node context.
  - **Recipes matching skill level** — XPath `/recipes/recipe[difficulty = $skill]`
    against `data/recipes.xml`. The `$skill` variable is bound at runtime via a small
    `XPathVariableResolver`, so the user's skill value is never string-concatenated into
    the expression (i.e. no XPath injection risk if we later expose a free-form input).

## Task 7 — Rares

> Recommend recipes based on both cooking skill level and preferred cuisine type. Use XPath/XQuery and select the first user from your XML file. (1 point)

- `GET /recommendations` keeps selecting the first user via XPath `/users/user[1]`
  (same entry point as Task 6), but the recipe filtering is now upgraded to use
  **both** user attributes: `skillLevel` and `preferredCuisine`.
- `org.example.service.RecommendationService` adds a dedicated XPath query:
  - `/recipes/recipe[difficulty = $skill and cuisine = $cuisine]`
  - both variables are bound at runtime through `XPathVariableResolver`
    (`$skill` from `user.skillLevel`, `$cuisine` from `user.preferredCuisine`),
    so values are injected safely without string concatenation.
- `RecommendationController` now calls `recipesForSkillAndCuisine(...)` and the
  `recommendations.html` view was updated to show that results match both filters
  and to display an empty-state message when no recipe satisfies the combined criteria.

## Task 8 — Rares

> Display the list of recipes from your local XML file/database on the web interface. First, read it into memory and then display it using XSL.
> - Recipes that match the user's cooking skill level should have a yellow background, and others should have a green background. (1 point)

- `RecipeRepository` still loads `data/recipes.xml` into memory at startup (`findAll()`),
  and the recipes page now renders that in-memory list through an XSL transform.
- Added `org.example.service.RecipeXslRenderService`:
  - builds an in-memory XML DOM from `List<Recipe>` (not directly from file),
  - applies `src/main/resources/xsl/recipes-table.xsl`,
  - passes the first user's skill level as XSL parameter `userSkillLevel`.
- `RecipeController` (`GET /recipes`) now gets the first user skill level from
  `UserRepository` and injects transformed HTML into the view.
- `xsl/recipes-table.xsl` renders the recipes table and assigns per-row CSS class:
  - `match-skill` when `difficulty = $userSkillLevel` (yellow background)
  - `other-skill` otherwise (green background)
- `templates/recipes.html` includes the XSL output and a legend showing yellow/green
  semantics and the currently selected first-user skill level.

## Task 9 — Rares

> Allow the user to see all the details of a specific recipe. Use XPath/XQuery to filter and display the data. (1 point)

- Added a dedicated recipe details page at `GET /recipes/{id}`.
- `org.example.service.RecipeDetailsService` performs the filtering with XPath on
  `data/recipes.xml` using the expression `/recipes/recipe[@id = $id]`, where
  `$id` is bound through `XPathVariableResolver`.
- The selected XML node is mapped into the existing `Recipe` model
  (`id`, `title`, `cuisine`, `difficulty`, `source`) and rendered in
  `templates/recipe-details.html`.
- The recipes list (XSL output) now links each title to its details page, so users
  can navigate from list view to single-recipe detail view directly.

## Task 10 — Rares

> Provide the list of recipes matching a specific cuisine type. The user can select a cuisine type from a given set of options. Use XPath/XQuery to retrieve the recipes. (1 point)

- Added a dedicated cuisine filter page at `GET /recipes/cuisine` with a dropdown
  for allowed cuisine values (`Italian`, `Asian`) and a submit button.
- Filtering logic uses XPath in `RecommendationService` with expression:
  - `/recipes/recipe[cuisine = $cuisine]`
  - `$cuisine` is bound through `XPathVariableResolver` (no string concatenation).
- `RecipeController` now handles the selected cuisine and renders the filtered list
  in `templates/recipes-by-cuisine.html`.
- The recipes page now links to the cuisine filter screen, and filtered rows keep
  links to individual recipe details (`/recipes/{id}`).

## Task 11 — Rares

> Create a nice and intuitive graphical interface for your application. (For full grade the UI must be intuitive: in exercise 8 I want to be able to select a certain user — just an example, you can add other details you consider nice to have.) (1 point)

- Upgraded the UI flow to be more intuitive across pages by adding clearer actions,
  consistent navigation, and direct cross-links between views.
- For Exercise 8 specifically, recipes highlighting is now user-selectable:
  - `GET /recipes?userId=uNN` lets the user choose which user drives yellow/green
    difficulty highlighting.
  - The recipes page contains a selector (`Highlight by user`) showing user name,
    id and skill level, then re-renders the XSL table against the selected skill.
- The Users page now has a one-click action (`Use in recipe highlights`) for each
  user, opening Recipes with that user preselected.
- Overall result: the interface supports faster navigation and explicit context
  switching, instead of forcing first-user-only behavior.
