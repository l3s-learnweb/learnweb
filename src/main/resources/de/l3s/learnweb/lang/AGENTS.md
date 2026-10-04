# Agent Instructions for translations

Message bundles for `de.l3s.learnweb.lang.messages`, loaded by `de.l3s.learnweb.i18n.MessagesBundle` / `DynamicControl`.

## Files

- `messages.properties` – English, the source of truth and the fallback for every locale. Its key order and blank-line grouping define the layout of all other files.
- `messages_{de,es,it,pt,uk}.properties` – translations. A locale must also be listed in `faces-config.xml` (`<supported-locale>`).
- `messages_xy.properties` – **not a language**: one note per key telling translators where/how the key is used (UI element, page, meaning of `{0}`, `{1}`). Keep it in sync whenever you add, rename or repurpose a key.
- Locale `xx` returns a debug bundle (shows keys instead of text), useful to find a string on a page.
- `de.l3s.maintenance.messages.ExportUntranslated` exports the keys missing in each locale to `missing_keys_<lang>.csv` (with English text and the `xy` note). `LanguageFileComparison` only lists keys absent from a hard-coded `test.csv` of reviewed keys; it does not compare locale files.

## Format rules

- Plain UTF-8, one `key = value` per line, no `\uXXXX` escapes, no line continuations. Never leave a value empty – omit the key instead so the English fallback is used.
- Values that are rendered with arguments (`h:outputFormat`, `o:format1`, `getLocaleMessage(key, args)`, `addMessage(..., args)`) go through `java.text.MessageFormat`:
  - A single `'` starts a quoted section: the apostrophe disappears and placeholders after it are not substituted. In any value containing `{0}`-style placeholders write `''` (e.g. `l''indirizzo`, `won''t`). Ukrainian uses the typographic apostrophe `’` everywhere.
  - Keep every `{n}` of the English value; translate only the text parts inside `{0, choice, 0#…|1#…|1<…}`.
- Keep HTML tags/entities (`<b>`, `<br/>`, `&quot;`) of the English value; several keys are rendered with `escape="false"`.
- Tests in `MessagesBundleTest` pin `homepageTitle` for `de`/`pt`, and `greeting`, `user.gender.*`, `page_number` and the date format of `survey.answer_restricted_dates_between` for `de`.

## Finding where a key is used

A plain grep for the key is not enough; keys are often composed dynamically:

- XHTML: `#{msg.key}`, `#{msg['key']}`, and prefixes like `msg['search_filters.' += value]`, `msg['theme.' += t]`, `msg['user.gender.' += gender]`, `msg['frontpage.course_description_' += …]`, `msg['policy.option.' += …]`.
- Bare lookups: `msg[filter.type]` (search filter enum names), `msg[stat.key]` (public statistics), `msg[entry.key]` / `msg[field.label]` / `msg[field.name]` (resource metadata, see `Organisation` metadata fields), `msg[Column.x]` (glossary columns), `msg[message]` on error pages (exception message used as key, e.g. `BadRequestHttpException("key")`, `BeanAssert.*`).
- Java: `getLocaleMessage("key")`, `addMessage/addGrowl(severity, "key", …)`, enum-based keys such as `"question_type." + name()`, `"question_type.desc_" + name()`, `notification_type.*`, `rating.star_N` (`TitledRatingRenderer`), log entries (`LogEntry`, `log_*` keys).
- Emails: `de.l3s.mail` / `MailFactory` (`email*` keys).

A missing key is not an error: `MessagesBundle` falls back to the key itself, so users see the raw key (or a literal text passed as key, with unsubstituted `{0}`). Therefore:

- Before removing a key as unused, check all the patterns above; after removing keys, grep the code for every `"some.key"` literal passed to `addMessage`/`addGrowl`/`getLocaleMessage` and make sure it still exists in `messages.properties`.
- When adding a key used in code, add it to `messages.properties` and `messages_xy.properties` (and translations) in the same change.

## Gotchas

- Glossary Excel import (`GlossaryRowBuilder.isEqualForAnyLocale`) matches column headers against the translations of `glossary.*` column keys, `language`, `source` and `glossary.total_entries` in **all** locales. Changing these translations can break re-importing files exported earlier in that language.
- `log_*` messages are sentence fragments appended after a user name (`<user> has deleted …`); use third person and keep `<b>{0}</b>`.
- `by`, `of`, `on_date`, `with`, `with_query` are short connectors concatenated with other text – translate them to fit those combinations. `of` is only used for "3 of 10" (members); "Groups of <user>" has its own key `groups_of_user`.
- Same key, different contexts: `email.you_can_ignore` ends both the password reset and the email confirmation mail; `options` is both the generic "Options" and the answer options list in the survey editor. Choose wording that fits all usages.
- Misleading names: `search_groups_*` describe the carousel of alternative search services on the search page (not groups); `rating.star_*` are the star tooltips of every rating widget.
- Ukrainian has three plural forms, which `choice` cannot express; prefer wording like "Додано ресурсів: {0}".

## Style decisions per language

- **de**: informal *du* everywhere (incl. emails); Ressource, Schlagwort (tag), Ordner, Anmerkung, Protokoll, Sitzung, sperren/Sperre (ban), Lehrkraft/Studierende, Gruppenleitung (group leader), Beitrag (forum post), Suchbegriff (not Schlüsselwort); buttons in the infinitive; quotes „…“.
- **es**: informal *tú*; añadir (not agregar), enlace, panel (dashboard), bloquear (ban), líder (group leader), valorar/valoración, Inicio (home).
- **it**: informal *tu*, no impersonal forms ("Si prega di…", "Selezionare…"); risorsa, dashboard, cronologia, e-mail, eliminare/Elimina (delete; "Cancella" only for clearing filters), bloccare/blocco (ban), foglio di calcolo.
- **pt**: Brazilian Portuguese, *você*, sentence case; Recurso, Marcação (tag), Pasta, Questionário (survey, not Inquérito), Painel, Excluir, Cadastro, Idioma.
- **uk**: formal *ви*; ресурс, група, курс, папка, глосарій (not «словник»), допис (post), тег, опитування, керівник (group leader), учасник, обліковий запис, завантажити, заблокувати (not «бан»), панель (dashboard); quotes «…», apostrophe `’`; log verbs in inclusive form «створив(-ла)».

## Conventions and known issues

- English labels use sentence case ("Edit group", "Total entries"), including error page titles.
- No gendered pronouns (the user's gender is unknown); use "their".
- `ip_banned` / `username_banned` are shown via `BeanAssert.hasPermission(…, key)` → 403 page `msg[message]`, which cannot pass arguments, so they must not reference a date.
- Glossary *Source* values are stored as fixed English strings (`GlossaryBean.SOURCES`) and only translated for display; never change the stored values (existing terms, Excel import/export). `glossary.web_blog` still stores `patients' websites and blogs` (from the original medical-terminology course) but is labelled "Personal websites and blogs".
- Glossary import headers: do not change translations of the column keys listed in the Gotchas above (e.g. pt `language` stays "Linguagem").
- `forum.field_category_*` (research coding scheme), YELL/TELL texts (`share_yell`, `frontpage.course_description_yell`, `yell_*`), `glossary.fields_description` and the service list in `homepage` are intentionally kept as they are; category 3 has the same heading as category 1 since 2015.
- `log_add_resource_metadata` and `log_group_deleting_link` are still rendered for old log entries, but their actions (`adding_resource_metadata`, `group_deleting_link`) are no longer logged.
