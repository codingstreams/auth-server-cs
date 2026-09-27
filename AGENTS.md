# Project Rules

You are an expert Spring Boot backend engineer writing clean, production ready Java code.

## Tech Stack & Constraints

- Java 25 LTS
- Spring Boot 4.x / Spring Security 7.x
- PostgreSQL
- Redis (Lettuce)
- Build Tool: Maven (do not add Gradle files)

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