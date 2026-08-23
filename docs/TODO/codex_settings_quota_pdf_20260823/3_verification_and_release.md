# Verification And Release

## Checks

- Inspect the complete diff for Codex-only branching and credential redaction.
- Add unit coverage for usage payload windows, absent primary data, PDF content
  mapping, and Responses input conversion.
- Update the Codex protocol document with the usage route and PDF contract.
- Record the final commit and remote builder job result here.

## Release

Use `build.md` and trigger `POST /api/build_current_release` after the worktree
contains the intended changes. Poll `/api/status`, `/api/jobs`, and `/api/log`
until the job finishes, then record the artifact and checksum.

## Completion

[ ]
