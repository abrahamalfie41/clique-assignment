import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            execute(args, out);
            return 0;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            err.println("Error: " + e.getMessage());
            err.println("Usage: checkin [--top N] [--as-of UTC-TIMESTAMP] [--export FILE]");
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
        }
        return 1;
    }

    private static void execute(String[] args, PrintStream out) throws IOException {
        int top = 5;
        Path exportFile = null;
        Instant asOf = Instant.now();
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (!option.equals("--top") && !option.equals("--export") && !option.equals("--as-of")) {
                throw new IllegalArgumentException("Unknown option: " + option);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            String value = args[++i];
            if (option.equals("--top")) {
                top = Integer.parseInt(value);
                if (top < 0) {
                    throw new IllegalArgumentException("--top must be nonnegative");
                }
            } else if (option.equals("--as-of")) {
                asOf = Instant.parse(value);
            } else {
                if (value.isBlank()) {
                    throw new IllegalArgumentException("--export requires a file path");
                }
                exportFile = Path.of(value);
            }
        }

        DataLoader loader = new DataLoader();
        var members = readInput("data/members.csv", loader::loadMembers);
        var contacts = readInput("data/last_contacts.csv", loader::loadContacts);
        var holidays = readInput("data/holidays.json", loader::loadHolidays);

        CheckinService service = new CheckinService();
        var recommendations = service.findDueMembers(members, contacts, holidays, asOf, top);

        if (exportFile != null) {
            try {
                CsvExporter.export(recommendations, exportFile);
            } catch (IOException e) {
                throw new IOException("Cannot export to " + exportFile + ": " + e.getMessage(), e);
            }
        }

        out.printf("%-10s %-24s %14s  %s%n",
                "member_id", "full_name", "priority_score", "recommended_window");

        if (recommendations.isEmpty()) {
            if (top == 0) {
                out.println("No results requested (--top 0).");
            } else if (holidays.contains(asOf.atZone(ZoneOffset.UTC).toLocalDate())) {
                out.println("No check-ins scheduled: the UTC date is a holiday.");
            } else {
                out.println("No members are due for a check-in.");
            }
        }

        for (var recommendation : recommendations) {
            out.printf(Locale.ROOT, "%-10d %-24s %14.1f  %s%n",
                    recommendation.memberId(),
                    recommendation.fullName(),
                    recommendation.priorityScore(),
                    recommendation.recommendedWindow());
        }
    }

    // Add the filename to parsing and file errors before reporting them to the user.
    private static <T> T readInput(String filename, InputReader<T> reader) throws IOException {
        try {
            return reader.read(Path.of(filename));
        } catch (IOException | RuntimeException e) {
            throw new IOException("Cannot load " + filename + ": " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface InputReader<T> {
        T read(Path file) throws IOException;
    }
}
