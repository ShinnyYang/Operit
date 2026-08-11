# 3. Verification

## Required Evidence

- Review the merged schema-generation configuration against #926.
- Add or update focused restore regression tests for failure after preparation but
  before a completed replacement.
- Run the relevant JVM test target and the candidate build/check path only after
  code changes are complete.
- Confirm the final branch remains based on the current `main` and that the PR
  candidate checks are green before proposing merge.
