# Agent Instructions for Learnweb

Learnweb is a collaborative search and sharing platform (groups, resources, forum, glossary, surveys, activity logging).

**Stack**: Java 25, Jakarta EE 11, JSF (MyFaces + PrimeFaces + OmniFaces), CDI (Weld), MariaDB via JDBI (SQL Object) + HikariCP, Flyway, Solr, Cron4j, Log4j2, Sentry. Frontend assets (JS/SASS) bundled with esbuild. Runs on Tomcat 11 / Jetty 12. Check `pom.xml` / `package.json` for exact versions.

**External services**: Interweb (search/LLM), thumbmaker (thumbnails), OnlyOffice (document editing), Solr (indexing).

## Layout

- `src/main/java/de/l3s/learnweb/` – packages by feature (`app`, `beans`, `forum`, `group`, `resource`, `search`, `user`, `web`, …).
  - `app/`: bootstrapping – `Learnweb` (central singleton), `ConfigProvider`, `DaoProvider` (JDBI + Flyway), `JobScheduler` (cron jobs).
- `src/main/webapp/lw/` – JSF views (XHTML) by feature.
- `src/main/webapp/WEB-INF/templates/layout/` – `template.xhtml` (authenticated), `template-public.xhtml`, `dialog.xhtml`; reusable parts in `templates/blocks/`.
- `src/main/webapp/WEB-INF/` – `web.xml`, `faces-config.xml`, `beans.xml` (`bean-discovery-mode="annotated"`), `urlrewrite.xml`, `learnweb-lw.taglib.xml` (custom components).
- `src/main/webapp/resources/` – JS, SASS, images; build output goes to `resources/bundle/`.
- `src/main/resources/db/migration/` – Flyway migrations, named `V{version}__{Description}.sql`.
- `src/main/resources/application.properties` – config; override via `src/main/resources/.env` (gitignored) or env vars prefixed `learnweb_`.
- i18n is database-backed (`MessagesBundle`).

## Commands

- Frontend: `npm install`, `npm run build:dev`, `npm run start` (watch), `npm run lint:js`, `npm run lint:scss`.
- Backend: `mvn jetty:run`, `mvn test`, production build `mvn clean package -Pprod`.
- Full stack (MariaDB, Solr): `compose.yaml`.

## Conventions

- Naming: `*Dao` for data access, `*Bean` for JSF backing beans, plain nouns for entities.
- Use CDI `@Inject`; never instantiate beans manually. Session-scoped (and view-scoped) beans must be `Serializable`; mark non-serializable fields `transient`.
- DAOs: JDBI `@SqlQuery`/`@SqlUpdate` with bound parameters; never concatenate user input into SQL. Register row mappers for custom types.
- Beans extend `ApplicationBean`; use `addGrowl`/`addMessage(severity, msgKey, args...)` with i18n keys, not literal text.
- Views: keep logic in beans. Use `#{msg['key']}` for all user-facing text; `#{userBean}` for session data.
- Access control: `User.isAdmin()`, `isModerator()`, `canModerateUser()`, `Organisation.getOption(...)`. Throw `ForbiddenHttpException` / `UnauthorizedHttpException` (subclasses of `HttpException`).
- Sanitize rich text with the OWASP HTML Sanitizer.
- Optional integrations (IMAP/`BounceManager`, captcha, Sentry, …) must degrade gracefully when unconfigured.
- Logging: Log4j2 via `LogManager.getLogger()`; never log sensitive data.
- Style: 4 spaces for Java, 2 for JS/SCSS/YAML (`.editorconfig`). No wildcard imports (static imports in tests are fine), no inline/fully-qualified imports. Import order: `java.*`/`javax.*`, `jakarta.*`, third-party, `de.l3s.*`.
- Tests: JUnit Jupiter (+ Mockito, Weld JUnit). Use `de.l3s.test.LearnwebExtension` for DB-backed tests (in-memory H2 by default).

## Git

- Git is read-only for agents: use only commands that read state (`status`, `diff`, `log`, `show`, `blame`, …). Never stage, unstage, commit, stash, reset, checkout, rebase, push or change the index or working tree through git unless the user explicitly asks.
