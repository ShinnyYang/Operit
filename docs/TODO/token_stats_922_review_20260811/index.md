---
fork_repository: https://github.com/AAswordman/Operit.git
source_pr: https://github.com/AAswordman/Operit/pull/922
working_branch: fix/token-stats-922-review
---

# Token Statistics PR 922 Review

## Background

PR #922 introduces token usage tracking, pricing management, durable statistics
spooling, Room schema changes, backup/restore coordination, and their settings and
UI. The feature is useful, but the source PR is too large to merge directly into
`main` while its candidate checks fail and its database replacement path can remove
the active database before replacement succeeds.

## Intent

Preserve #922 as a normal merge on an isolated repair branch, then make the merged
result safe to validate and submit as a focused follow-up PR. `main` remains
unchanged until the repair branch has passing checks and a reviewed data-integrity
path.

## Scope

- Preserve the #922 commit topology through merge commit `663a3a59`.
- Reproduce and resolve the current candidate-check blocker against the current
  `main` baseline.
- Make database restore replacement preserve the existing database when a final
  filesystem operation fails.
- Add focused regression coverage for the repaired restore behavior and run the
  relevant repository checks requested for the follow-up PR.

## Steps

1. [DONE] [Merge baseline](1_merge_baseline_and_reproduction.md)
2. [Restore integrity](2_restore_integrity.md)
3. [Verification](3_verification.md)
