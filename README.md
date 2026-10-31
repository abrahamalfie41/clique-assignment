# Clique Check-in assignment

A Java command line program that reads `data/` and lists members due for a check-in, ordered by priority.

## Run

Requires JDK 17+ and internet on the first run.
Maven does not need to be installed separately. The included wrapper downloads Maven on the first run, and Maven downloads the project dependencies.
The launcher starts Maven and checks compilation on each run, so startup has some overhead. 

Maven manages JUnit 5, Commons CSV, and Gson, and runs all tests with one command.
Codex helped set up the Maven wrapper and launchers, implement application code,
and write tests.

Run the following commands from the repository root.

macOS/Linux:

```sh
./checkin --top 5
```

Windows (PowerShell or Command Prompt):

```powershell
.\checkin.cmd --top 5
```

Defaults to five results. Zero requests no members; negative values are rejected.
Empty results include a short explanation. File errors report a message and exit
with status 1 rather than displaying a Java stack trace.
To use `checkin` without `./`, add the repository to your shell's PATH.

## Tests

```sh
./mvnw test
```

On Windows, use `.\mvnw.cmd test`.

JUnit 5 tests cover data loading, eligibility boundaries, holidays, scoring,
sorting, recommended windows, result limits, CSV export, and CLI messages.

## Assumptions

- Use UTC and complete elapsed 24-hour days. Members qualify after at least seven
  days, or immediately if never contacted. Holidays and blank channels exclude members.
- Use the same time for every member: the current time unless `--as-of` is supplied. Recommend
  morning for members aged 80 or above, and afternoon for everyone else.
- Score: discharge +3, living alone +2, age ≥80 +1, `no_answer` +1, plus days / 7.0.
  Never-contacted members receive only age and risk points; `escalate` adds nothing.
- Sort by full score descending. If scores are equal, break ties by numeric member
  ID ascending (for example, ID 2 before ID 10). Display scores with one decimal.
- Inputs have valid values and unique member IDs; duplicate contact IDs are rejected.

`DataLoader` reads files, `CheckinService` applies the rules, and `Main` handles
arguments and output.

## Extra features

- **Date override:** Use a fixed UTC timestamp for a demo with
  the bundled 2025 data. Omit `--as-of` to use the current time.

```sh
./checkin --top 5 --as-of 2025-11-06T12:00:00Z
```

On Windows, use `.\checkin.cmd` with the same arguments.

- **CSV export:** Save and share the check-in list results while still
  displaying them in the console.

```sh
./checkin --top 5 --export checkins.csv
```

On Windows, use `.\checkin.cmd` with the same arguments. The CSV contains the same
four columns and selected results as the console, with scores to one decimal.
Relative export paths are resolved from the repository folder. Existing files
are overwritten; the destination directory must already exist.

## Next improvements

- **Filter by channel:** Show only members who prefer calls, SMS, or WhatsApp.
- **Runnable JAR:** Run the program without starting Maven each time. Reduces overhead, opens the door to a better user experience.
