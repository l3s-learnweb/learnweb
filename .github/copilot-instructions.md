# Copilot Instructions for Learnweb

## Project Overview

Learnweb is a collaborative search and sharing system that brings together different online services under one umbrella.
It provides advanced features for organizing and sharing distributed resources with groups of people.

-   **Tech Stack**: Java 25, Jakarta EE 11, JSF (MyFaces 4.1/PrimeFaces 15), MariaDB (JDBI 3.50), esbuild, Cron4j scheduler, Apache Solr.
-   **Server**: Tomcat 11 / Jetty 12.
-   **Key Features**: Collaborative learning, resource search and sharing, group management, forum, glossary, user activity tracking, i18n, scheduled jobs.
-   **External Components**: Interweb (search/LLM), ThumbEngine (thumbnails), OnlyOffice (document editing), Apache Solr (resource indexing).

## Architecture

-   **Backend**: CDI (Weld 6.0) for dependency injection. JDBI 3.50 with SQL object pattern for data access. Flyway 11 for DB migrations.
-   **Frontend**: JSF views (XHTML) with PrimeFaces components. Uses Facelets templating (`ui:composition`, `ui:define`). Main templates: `template.xhtml` (authenticated users), `template-public.xhtml` (public pages), `dialog.xhtml` (modals). JavaScript/SASS bundled with esbuild to `resources/bundle/`. OmniFaces 5.0 provides versioned resources and enhanced exception handling. URL rewriting via `urlrewrite.xml`.
-   **Internationalization (i18n)**: Database-backed ResourceBundle (`MessagesBundle`) with locale-specific overrides.
-   **Configuration**: Properties file (`application.properties`) + environment variables (prefixed `learnweb_`). Override via `.env` file for local development.
-   **Scheduled Jobs**: Cron4j scheduler managed by `JobScheduler`. Jobs include: forum notifications, TED/speech repository crawlers, bounce email handling, ban cleanup, request aggregation.
-   **Search & Indexing**: Apache Solr for resource search. Interweb integration for external search and LLM features.
-   **Custom Components**: JSF component library (`learnweb-lw`) for specialized rendering.

## Development Guidelines

### Project Structure
**Core packages**:
-   `app/`: Application bootstrapping (`Learnweb.java`, `ConfigProvider`, `DaoProvider`, `JobScheduler`)
-   `beans/`: JSF backing beans (named `*Bean`) - ViewScoped, SessionScoped, RequestScoped
-   `component/`: Custom JSF components
-   `dashboard/`: Dashboard functionality
-   `exceptions/`: Custom HTTP exceptions (`HttpException`, `UnauthorizedHttpException`, `ForbiddenHttpException`)
-   `forum/`: Forum features and notifications
-   `gdpr/`: GDPR compliance features
-   `group/`: Group management (courses, workgroups)
-   `i18n/`: Internationalization support (`MessagesBundle`)
-   `logging/`: Activity and action logging
-   `resource/`: Resource management (files, links, videos, glossary, surveys, TED talks, Office documents)
-   `search/`: Search functionality and history
-   `searchhistory/`: Search history tracking
-   `sentry/`: Error tracking integration
-   `user/`: User management, authentication, profiles, organizations
-   `web/`: Web utilities (bounce handling, ban management, request tracking)

**Resources**:
-   `src/main/webapp/lw/`: JSF views (XHTML) organized by feature
-   `src/main/webapp/WEB-INF/templates/`: Page templates
-   `src/main/webapp/resources/`: Frontend assets (JS, SASS, images, fonts)
-   `src/main/resources/db/migration/`: Flyway migration scripts
-   `src/main/resources/application.properties`: Main configuration file
-   `src/main/resources/.env`: Local development overrides (gitignored)

### Coding Standards
-   **Naming**: Use `*Dao` for data access, `*Bean` for JSF backing beans, plain nouns for entities (e.g., `User`, `Group`, `Resource`).
-   **State Management**: Session-scoped beans must be `Serializable`. Use `transient` for non-serializable fields.
-   **Logging**: Use Log4j2 via `LogManager.getLogger()`. Avoid logging sensitive data.
-   **Error Handling**: Use `HttpException` subclasses for HTTP-related errors. Use `ForbiddenHttpException` and `UnauthorizedHttpException` for access control.
-   **Code Style**: Use 4-space indentation (see `.editorconfig`). No wildcard imports. Follow import order: Java SE (`java.*`, `javax.*`), Jakarta EE (`jakarta.*`), third-party (`io.*`, `org.*`), internal (`de.l3s.*`).
-   **Testing**: Write JUnit 5 unit tests. Use Testcontainers for integration tests. Weld JUnit for CDI testing.

## Workflow & Configuration

-   **Setup**: Run `npm install` and `npm run build:dev`. For hot-reloading, use an `exploded` WAR deployment in your IDE (IntelliJ). Run `npm run watch` for frontend changes.
-   **Docker**: Use `compose.yaml` to run the full stack locally (includes MariaDB, Solr).
-   **Profiles**: Use the `prod` Maven profile for production builds (`mvn clean package -Pprod`).
-   **Database Migrations**: Add new migration files to `src/main/resources/db/migration/`. Naming: `V{version}__{Description}.sql`.
-   **Development Server**: Use Tomcat 11+ or Jetty 12 Maven plugin (`mvn jetty:run`). Default servlet container port varies.
-   **Environment Variables**: Prefix with `learnweb_` (e.g., `learnweb_datasource_url`). Use `.env` file for local overrides.

### Important Configuration Files
-   **WEB-INF/web.xml**: Servlet configuration, session timeout (100 min), PrimeFaces settings, OmniFaces configuration (versioned resources, exception handling).
-   **WEB-INF/faces-config.xml**: JSF configuration with custom resource handlers, exception handlers, locale settings, and MessagesBundle registration.
-   **WEB-INF/beans.xml**: CDI configuration with `bean-discovery-mode="annotated"`.
-   **WEB-INF/learnweb-lw.taglib.xml**: Custom JSF component tag library definitions.
-   **WEB-INF/urlrewrite.xml**: URL rewriting rules.
-   **log4j2.xml**: Logging configuration for Log4j2.
-   **application.properties**: Application configuration (database, Solr, SMTP, file storage, integrations, Sentry DSN). Override with `.env` file.
-   **weld.properties**: CDI/Weld configuration.
-   **release.properties**: Version and build stage information (managed by Maven).

## Key Dependencies & Libraries

-   **Jakarta EE**: Servlet 6.1 API, EL 6.0 API, Faces (MyFaces 4.1), CDI (Weld Servlet 6.0), Validation (Hibernate Validator 9.1), Mail (Angus Mail 2.0)
-   **JSF Components**: PrimeFaces 15 (jakarta classifier), OmniFaces 5.0 for utilities and enhanced exception handling
-   **Database**: MariaDB JDBC driver 3.5, HikariCP 7.0 connection pool, JDBI 3.50 for SQL access, Flyway 11.17 for migrations
-   **Search & Indexing**: Apache Solr client, Interweb client 4.5 (custom)
-   **Image Processing**: TwelveMonkeys ImageIO (JPEG, TIFF, BMP, WebP, HDR support)
-   **Document Processing**: Apache POI 5.4 (Excel/XLSX), OpenPDF 1.4 (PDF export)
-   **Utilities**: Apache Commons Lang3 3.19, Commons IO 2.21, Jackson (JSON), OWASP HTML Sanitizer
-   **Logging**: Log4j2 2.25 with SLF4J and JUL bindings
-   **Monitoring**: Sentry 8.26 for error tracking
-   **Scheduling**: Cron4j with CDI integration via `JobScheduler`
-   **Charts**: ChartJS Java Model 2.9
-   **Testing**: JUnit 5.14, Weld JUnit, Testcontainers, Mockito 5.20

## Key Application Components

-   **Learnweb** (`@ApplicationScoped`, `@Eager`): Central singleton providing access to core services. Initializes `ConfigProvider`, `DaoProvider`, `SolrClient`, `Interweb`, `ResourcePreviewMaker`, `ResourceMetadataExtractor`.
-   **ConfigProvider** (`@ApplicationScoped`, `@Named("config")`): Centralized configuration management. Loads `application.properties`, `.env` file, and environment variables (prefix `learnweb_`). Detects development vs. production mode. Provides server URL, app name, support email, captcha keys, etc.
-   **DaoProvider** (`@ApplicationScoped`, `@Eager`): Provides all DAO instances. Initializes JDBI, runs Flyway migrations, manages database connection pool.
-   **UserBean** (`@SessionScoped`, `@Named`): User session management. Stores current user, locale, color theme, preferences. Provides login/logout, language switching, moderator mode.
-   **JobScheduler** (`@ApplicationScoped`): Manages Cron4j scheduler. Schedules: `ExpiredBansCleaner` (weekly), `RequestsTaskHandler` (hourly), `BounceFetcher` (every 5 min), `ForumNotificator` (daily 8am), `TedCrawlerSimple` & `SpeechRepositoryCrawler` (monthly, production only).
-   **MessagesBundle**: Custom ResourceBundle for database-backed i18n with locale-specific overrides.
-   **SolrClient**: Apache Solr integration for resource indexing and search.
-   **Interweb**: Integration with Interweb service for external search and LLM features.
-   **BounceManager** (`@Dependent`): Email bounce detection and handling via IMAP. Optional - gracefully disabled if IMAP not configured.

## Security & Best Practices

-   **Authentication**: Session-based via `UserBean`. Login handled by `LoginBean`.
-   **Authorization**: Organization-based permissions with role levels. Check permissions via `User.isAllowedTo()` methods.
-   **SQL Injection**: Always use JDBI's prepared statements (SQL Object pattern). Never concatenate user input into queries.
-   **XSS Protection**: PrimeFaces auto-escapes output. Use OWASP HTML Sanitizer for rich text (TextEditor).
-   **File Uploads**: Validate file types and sizes. Store in configured `file_manager_folder`.
-   **Dependencies**: Always use CDI `@Inject`; never manually instantiate beans.
-   **Views**: Keep business logic in backing beans, not XHTML. Use `#{userBean}` for session data, `#{msg['key']}` for i18n.
-   **Internationalization**: Always use `#{msg['key']}` from MessagesBundle; never hardcode user-facing text.
-   **Optional Features**: Components like `BounceManager`, Captcha, Sentry, IMAP should gracefully disable if not configured.
