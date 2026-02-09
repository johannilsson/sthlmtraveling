# ADR-0004: Feature Branch Workflow

**Date**: 2026-02-09
**Status**: Accepted

## Context

The project needs a clear branching strategy to prevent:
- Accidental commits to the main branch that bypass review
- Complex merge conflicts from branching off feature branches
- Unclear history and difficult rollbacks
- CI/CD issues when main branch is in a broken state

The main branch (`master`) serves as the stable integration point and should always be releasable. All development work should happen in isolation before merging.

## Decision

We will adopt a **feature branch workflow** with strict rules:

1. **Never commit directly to `master`** — all changes go through feature branches and pull requests
2. **Always branch from `master`** — never branch from other feature branches
3. **Use descriptive branch names** following conventions:
   - `feature/*` for new features
   - `fix/*` for bug fixes
   - `refactor/*` for code improvements
   - `docs/*` for documentation
   - `chore/*` for tooling/build changes

## Alternatives Considered

### 1. Trunk-Based Development
**Pros**: Simpler, encourages small changes, reduces merge conflicts
**Cons**: Requires very high CI/CD maturity, feature flags for incomplete work, and doesn't fit current code review practices
**Rejected**: Project doesn't have sufficient test coverage or CI infrastructure

### 2. Git Flow (develop + master + release branches)
**Pros**: Well-defined process, supports multiple release versions
**Cons**: Overly complex for single active version, adds ceremony
**Rejected**: Overkill for a single mobile app with one production version

### 3. Allow branching from feature branches
**Pros**: Enables dependent features without waiting
**Cons**: Creates cascading merge conflicts, unclear merge order, complicated history
**Rejected**: The rare dependent feature scenario doesn't justify the complexity cost

## Rationale

Feature branch workflow provides the right balance:
- **Simple enough** for a small team/solo developer with AI assistance
- **Safe enough** to prevent accidental main branch corruption
- **Flexible enough** to support parallel feature development
- **Standard enough** that CI/CD tools and GitHub work well with it

Requiring branches from `master` specifically:
- Keeps history linear and clean
- Avoids "branch of a branch" merge complexity
- Makes it clear what state each feature was based on
- Forces developers to rebase/merge main if needed, keeping features up-to-date

## Consequences

**Benefits**:
- Main branch stays stable and releasable
- All changes go through PR review (when humans are involved)
- Easy to abandon or revert features without affecting main
- Clear separation between experimental and production-ready code
- Better CI/CD integration (only build main + PRs)

**Trade-offs**:
- Slightly more ceremony than committing directly to main
- Need to stay in sync with main (requires occasional rebasing)
- Dependent features must wait or merge the dependency first

**Risks**:
- Developer might forget and commit to main accidentally
  - *Mitigation*: Could add pre-commit hook to prevent main commits (future enhancement)
- Long-lived branches can drift from main
  - *Mitigation*: Keep PRs small, merge frequently, rebase on main regularly
