---
name: gradle
description: Run any Gradle task and report the result.
allowed-tools: Bash
model: haiku
context: fork
---

# Gradle Skill

Run a Gradle invocation and report whether it passed or failed.

## Rules

- NEVER modify, edit, or fix any source files
- ONLY run the Gradle command and report the output
- Do NOT add any commentary or interpretation
- Do NOT summarize or condense the errors
- Skip Gradle boilerplate suggestions (lines starting with `* Try:` and `>`)

## Instructions

1. Run `./gradlew` with the provided arguments. Use a timeout of 300000ms (5 minutes).
   - If no arguments provided, run `./gradlew` with no task.

2. Capture both stdout and stderr.

3. Check the exit code and report results:

   **If build PASSES (exit code 0):**
   - Report: "GRADLE [arguments]: PASS"

   **If build FAILS (exit code non-zero):**
   - Report: "GRADLE [arguments]: FAIL"
   - Extract all errors from the output and include them verbatim

## Output Format

**Success:**
```
GRADLE [arguments]: PASS
```

**Failure:**
```
GRADLE [arguments]: FAIL

[Full verbatim error output from Gradle, without boilerplate suggestions]
```

