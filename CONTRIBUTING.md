# Contributing

Thanks for your interest in improving devmix-acto.

## Building

Requirements: JDK 21+. Everything else (Maven, plugins) is provided by the wrapper.

```bash
./mvnw verify          # compile, unit tests, static analysis, coverage gate
```

The `benchmarks` module is not part of the default build; see `docs/benchmarks/README.md`.

## Running the stress suite

The stress specs are excluded from `verify`. Run them explicitly:

```bash
./mvnw test -Dtest.excludes=none -Dtest='com.github.devmix.process.acto.stress.*Spec' -Dsurefire.failIfNoSpecifiedTests=false
```

## Conventions

- Every bug fix ships with a spec that fails without the fix. New features ship with specs too.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat(core): ...`, `fix(api): ...`, `docs: ...`, `chore: ...`. Reference the issue in the
  commit body (`Closes #123`).
- New source files need the GPL header (copy it from any existing file).
- Static analysis runs on every build (SpotBugs + findsecbugs, ErrorProne, JaCoCo line-coverage
  gate on `core`). If a suppression is truly needed, add it to `spotbugs-exclude.xml` with a
  justification comment.
- Public API changes are checked against the previous release by the japicmp CI gate; binary
  incompatibilities fail the build and need an explicit release note in `CHANGELOG.md`.

## Submitting

Fork, branch from `main`, keep changes focused, and open a pull request. CI must be green;
maintainers review and merge.
