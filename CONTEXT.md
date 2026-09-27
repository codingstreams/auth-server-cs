```text
.
├── AGENTS.md
├── CONTEXT.md
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── authserver
│   │   │               ├── AuthServerApplication.java
│   │   │               ├── config -> SecurityFilterChain & Bean setups
│   │   │               ├── controller
│   │   │               │   └── dto --> Request and Response DTOs
│   │   │               ├── exception --> Custom exceptions and Global Exception Handler
│   │   │               ├── mapper --> JPA Entities to DTO and vice versa mappers.
│   │   │               ├── Maskable.java
│   │   │               ├── MaskedField.java
│   │   │               ├── model -> JPA Entities
│   │   │               ├── repo --> Repositories
│   │   │               ├── security --> separate packages for RSA token utilities, Redis token services, OncePerRequestFilter implementations
│   │   │               ├── service --> separate packages for AuthService, UserService, etc. with Implementations
│   │   │               └── util --> Utility interfaces with static methods. Eg. JWTService
│   │   └── resources
│   │       ├── application.yaml
│   │       ├── db
│   │       │   └── migration
│   │       ├── static
│   │       └── templates
│   └── test
```
