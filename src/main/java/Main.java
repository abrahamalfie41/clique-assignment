import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws IOException {
        int top = 5;
        Path exportFile = null;
        try {
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if (!option.equals("--top") && !option.equals("--export")) {
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
                } else {
                    if (value.isBlank()) {
                        throw new IllegalArgumentException("--export requires a file path");
                    }
                    exportFile = Path.of(value);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Usage: checkin [--top N] [--export FILE]");
            System.exit(1);
            return;
        }

        DataLoader loader = new DataLoader();
        var members = loader.loadMembers(Path.of("data/members.csv"));
        var contacts = loader.loadContacts(Path.of("data/last_contacts.csv"));
        var holidays = loader.loadHolidays(Path.of("data/holidays.json"));

        CheckinService service = new CheckinService();
        Instant asOf = Instant.now();
        var recommendations = service.findDueMembers(members, contacts, holidays, asOf, top);

        if (exportFile != null) {
            CsvExporter.export(recommendations, exportFile);
        }

        System.out.printf("%-10s %-24s %14s  %s%n",
                "member_id", "full_name", "priority_score", "recommended_window");

        for (var recommendation : recommendations) {
            System.out.printf(Locale.ROOT, "%-10d %-24s %14.1f  %s%n",
                    recommendation.memberId(),
                    recommendation.fullName(),
                    recommendation.priorityScore(),
                    recommendation.recommendedWindow());
        }
    }
}
