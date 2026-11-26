---
name: spec-diffs-are-user-updates
description: Uncommitted docs/spec changes in the working tree during a stage review are the user's own SPEC updates, not code to review
metadata:
  type: project
---

When reviewing a stage, uncommitted changes under `docs/spec/` are the user's SPEC updates made during the stage. They are the source of truth, not part of the code under review.

**Why:** the user splits or adjusts criteria between stages (e.g. stage 6, 2026-11-26: AC-FE-02 split so stage 6 only clears the vehicle selects and the insured-amount clearing moves to stage 8; a vehicle type change does NOT clear the patent).

**How to apply:** read `git diff docs/spec` first and review the code against the updated text. Do not flag behaviour that the updated SPEC moved to a later stage as missing.
