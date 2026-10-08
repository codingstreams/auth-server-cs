# Project Rules

You are an expert Spring Boot backend engineer writing clean, production ready Java code.

## Tech Stack & Constraints

- Java 25 LTS
- Spring Boot 4.x / Spring Security 7.x
- PostgreSQL
- Redis (Lettuce)
- Build Tool: Maven (do not add Gradle files)

## Running the App for Testing

To test the running application, use Java 25 and load environment variables from `.env.local`.
Run these commands in Bash from the project root:

```bash
set -a
source .env.local
set +a
./mvnw spring-boot:run
```

Keep `.env.local` shell-compatible and out of Git. Do not print or log its credentials.

## Code Generation Boundaries

1. Scope Control: Never edit files outside the currently requested feature package.
2. Modern APIs: Avoid deprecated Spring Security methods (e.g., do not use `authorizeRequests()`, use
   `authorizeHttpRequests()`).
3. Concurrency & Security:
    - Never store raw passwords.
    - Do not log sensitive credentials or access token payloads. Use `com.example.authserver.MaskedField` to annotate
      the class with fields to be masked as values. If `com.example.authserver.MaskedField` is used then add
      `com.example.authserver.Maskable` interface and implement the `toMaskedString()` to return masked string.
4. Use `final` fields with constructor injection; do NOT use `@Autowired` on fields. Always use
   `@RequiredArgsConstructor`
5. Use Lombok to generate Getter/Setters and Builder pattern for Entity class.
6. Always use record for DTO classes.
7. Use jarkata validations for DTO classes.
8. Map Entity to DTO and vice versa using Mapper (use MapStruct).
9. Minimal Changes: Do not add unnecessary utility methods or boilerplate unless explicitly asked.
10. For utility class always generate the interface with static methods.
11. Every Service and Controller class will have logs. Use `@Slf4j` to inject logger.
12. Database & Migration Scripts:
    - Write Flyway migration scripts for any changes in schemas.

## UI Design & Components

- For UI changes, read [DESIGN.md](DESIGN.md) and [UI-KIT.md](UI-KIT.md) and follow their design tokens,
  component specifications, and accessibility requirements. Distinguish implemented behavior from pending requirements.
- Use `src/main/resources/static/css/global.css` as the shared stylesheet. Keep styling there; do not add
  inline styles, duplicate stylesheets, or page-specific copies of shared component styles.
- Reuse the existing semantic CSS variables for colors, borders, radii, shadows, and transitions.
  Support both light mode and `html.dark`, and follow the documented typography and responsive layout rules.
- Reuse Thymeleaf components from `src/main/resources/templates/fragments/ui/` for buttons, fields,
  checkboxes, alerts, badges, empty states, and icons. Consult the fragment signatures in `UI-KIT.md`
  before composing them. Extend shared fragments for reusable changes instead of duplicating their markup.
- Keep form actions, CSRF inputs, authorization conditions, and application-specific messages in the page
  or feature fragment. Keep interactive behavior in the existing JavaScript assets.
- Preserve field IDs, names, validation attributes, and CSS classes used by JavaScript when refactoring.
  Use unique IDs, associated labels, escaped content, accessible control names, and visible keyboard focus.
- Check changed components in both themes and at narrow viewport widths. For fragment changes, verify
  Thymeleaf rendering, conditional states, and relevant JavaScript hooks.
- Update the design specifications when changing shared design values or component APIs. Keep UI work
  within the requested scope; adding a component does not require implementing every pending UI-kit requirement.
