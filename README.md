# Clique check-in assignment

A Java command line program that reads `data/` and lists members due for a check-in, ordered by priority.

## Run

Requires JDK 17+ and internet on the first run.
The included Maven wrapper handles Maven installation and dependencies.

Maven manages JUnit 5, Commons CSV, and Gson, and runs all tests with one command.
Codex was used to add the Maven wrapper and help write the tests.

macOS/Linux:

```sh
./checkin --top 5
```

Windows (PowerShell or Command Prompt):

```powershell
.\checkin.cmd --top 5
```

Defaults to five results. Zero prints only the header; negative values are rejected.
To use `checkin` without `./`, add the repository to your shell's PATH.

## Tests

```sh
./mvnw test
```

On Windows, use `.\mvnw.cmd test`.

JUnit 5 tests cover data loading, eligibility boundaries, holidays, scoring,
sorting, recommended windows, and result limits.

## Assumptions

- Use UTC and complete elapsed 24-hour days. Members qualify after at least seven
  days, or immediately if never contacted. Holidays and blank channels exclude members.
- Score: discharge +3, living alone +2, age ≥80 +1, `no_answer` +1, plus days / 7.0.
  Never-contacted members receive only age and risk points; `escalate` adds nothing.
- Sort by full score descending. If scores are equal, break ties by numeric member
  ID ascending (for example, ID 2 before ID 10). Display scores with one decimal.
- Recommend morning for age ≥80, otherwise afternoon.
- Inputs have valid values and unique member IDs; duplicate contact IDs are rejected.

`DataLoader` reads files, `CheckinService` applies the rules, and `Main` handles
arguments and output. The CLI currently uses the current time.

## Next improvements

Add an optional `--as-of` argument for reproducible demos,
clearer input errors, and a runnable JAR to avoid starting Maven on each run.
