# P08 — Stacked PR assembly

Create exactly three single-purpose branch tips:

- `agent/web-stack-min/01-original-web` from the pinned upstream branch.
- `agent/web-stack-min/02-features` from PR 1.
- `agent/web-stack-min/03-teavm-final` from PR 2.

Before publication, verify every parent is the immediately previous branch,
there are no conflict markers, and every branch is independently buildable.
Push only to the `yosef` remote. Do not push or modify `origin`.

## Completed E2E evidence (2026-08-04)

The rebuilt chain is `base-upstream-master` → `01-original-web` →
`02-features` → `03-teavm-final`, rooted at
`f2e3017b77ad9c7dda04549b1e8e7ddaddbdccf5`. The pairwise counted deltas are
71, 64, and 53 files. Parent ancestry and conflict scans pass. Publication,
when performed, is restricted to `YosefLm/unciv-web`; `haimlm/*` is not a
push target.
