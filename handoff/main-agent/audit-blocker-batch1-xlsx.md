# Main Agent Audit Blocker — Batch 1 xlsx/multipart Import

Batch 1 is not accepted yet.

The current file `handoff/dev-agent/current-prompt.md` explicitly requires:

- `POST /api/data-steward/assets/projects:import`
- `POST /api/data-steward/assets/path-mappings:import`
- `multipart/form-data`
- file formats: CSV and xlsx
- xlsx reads the first sheet

Current implementation only supports `@RequestBody String` CSV/text-csv for both import endpoints.

## Required Fix

Backend only. Do not expand frontend. Do not implement Batch 2/3. Do not modify old Flyway migrations.

1. Keep existing `text/csv` request body support so current script compatibility is preserved.
2. Add `multipart/form-data` upload support for both import endpoints.
3. Support `.csv` / `.txt` as UTF-8 text.
4. Support `.xlsx` by reading the first sheet.
5. Prefer Apache POI `poi-ooxml`; add dependency only in the correct Maven module.
6. Reuse the existing row-level import logic so permissions, audit logs, import jobs, import rows, and row errors stay identical.
7. Extend `scripts/dev/check-bim-asset-batch1.sh`:
   - create a tiny xlsx test file using reliable local tooling
   - upload it as multipart to project import
   - upload another xlsx as multipart to path-mapping import
   - assert imported projects are visible
   - assert imported path mappings succeed and are visible
8. Rebuild, restart if needed, rerun `scripts/dev/check-bim-asset-batch1.sh`.
9. Update `handoff/dev-agent/latest-report.md` with this fix and validation result.
10. Output `<promise>BATCH1_DEV_COMPLETE</promise>` only after all validation passes.

Continue using Ralph Loop discipline: small verifiable story, inspect scope, implement, validate, report.
