# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java 17 multi-module Maven library (`io.hqwu.commons`) providing enterprise utilities: connection pooling, MyBatis-Plus extensions, Spring MVC enhancements, and email services. Built on Spring Boot 3.5.9. Published to GitHub Packages.

## Build Commands

```bash
# Build all modules (skip tests)
mvn clean install -DskipTests

# Run all tests
mvn test

# Run tests for a single module
mvn test -pl hq-utils

# Run a single test class
mvn test -pl hq-spring-webmvc -Dtest=BeanConverterTest

# Run a single test method
mvn test -pl hq-spring-webmvc -Dtest=BeanConverterTest#testConvert

# Check code coverage (JaCoCo runs during test phase)
mvn verify
```

No Maven wrapper — requires system Maven installation.

## Module Dependency Graph

```
hq-utils                    ← Base library, zero Spring dependency
├── hq-utils-spring         ← Spring SPI adapter for hq-utils
├── hq-spring-webmvc        ← Spring MVC converters, validators, filters
├── hq-cp                   ← Custom DB connection pool (HQCP)
│   └── hq-cp-boot-starter  ← Spring Boot 3 auto-config for HQCP
├── hq-mybatis-plus-extension ← Also depends on hq-spring-webmvc
├── hq-email-boot-starter   ← Jakarta Mail based email service
└── hq-dubbo-ext            ← DEPRECATED (commented out of build)
```

`report-aggregate` is a JaCoCo report aggregation module, not a library.

## Architecture Notes

- **Auto-configuration** uses Spring Boot 3 style: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (not `spring.factories`)
- **hq-cp** is decoupled from Spring — it can be used standalone. The boot-starter wraps it with auto-configuration
- **hq-spring-webmvc** contains a `BeanConverter` type-conversion framework using `@ValueOf` annotations and cglib `BeanCopier`
- **hq-utils** provides SPI extension points (e.g., `SecurityService`) that `hq-utils-spring` bridges to Spring beans
- **hq-mybatis-plus-extension** provides `MainLambdaQueryWrapper` and `JoinQueryWrapper` for complex join queries beyond standard MyBatis-Plus

## Testing

- **JUnit 5** + **Mockito** (primary), **EasyMock** (hq-cp only)
- **Greenmail** for email integration tests
- Integration tests (`*IntTest.java`) are excluded from `mvn test` by surefire config
- Test log level controlled by `test.logger.level` property (default: WARN)

## Code Coverage Requirements (JaCoCo)

Enforced via `mvn verify`:
- **Bundle**: 90% instruction, 85% branch coverage
- **Package**: 85% line coverage
- **Class**: 80% line coverage, max 20 missed complexity paths
- **Excluded**: `**/entity/**`, `**/dto/**`, `**/enums/**`, `**/config/**`, `**/*Exception.class`

## Conventions

- Commit messages: short English, focused on main (non-test) code changes; only describe test changes if there are no main code changes
- Code comments: Chinese
- All modules use Lombok
- Base package: `io.hqwu.commons`
