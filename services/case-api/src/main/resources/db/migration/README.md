# Flyway migrations (case-api)

Versioned SQL (Spring Boot default: `classpath:db/migration`):

| File | Purpose |
|------|---------|
| `V1__create_spike_tables.sql` | DDL — `spike`, `spike_failed_result`, `spike_product_lock` |

Do not edit applied `V*` files on a shared DB; add a new version instead.

Contract: `case-docs/projects/scored-eol-cause-list/design/02-contract-store-the-spike.md`

`spike_failed_result.id` is the copied MES manufacturing-result id. It is not auto-increment. Tech note: `case-docs/projects/scored-eol-cause-list/tech-notes/03-spike-failed-result-id-is-the-mes-result-id.md`
