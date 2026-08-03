# P08 — Stacked PR assembly

Create exactly three single-purpose branch tips:

- `agent/web-stack-min/01-original-web` from the pinned upstream branch.
- `agent/web-stack-min/02-features` from PR 1.
- `agent/web-stack-min/03-teavm-final` from PR 2.

Before publication, verify every parent is the immediately previous branch,
there are no conflict markers, and every branch is independently buildable.
Push only to the `yosef` remote. Do not push or modify `origin`.
