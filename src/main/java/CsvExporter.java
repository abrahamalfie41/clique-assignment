import org.apache.commons.csv.CSVFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CsvExporter {
    public static void export(
            List<CheckinService.CheckinRecommendation> recommendations,
            Path file) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("member_id", "full_name", "priority_score", "recommended_window")
                .build();

        // Commons CSV quotes names containing commas, quotes, or line breaks.
        try (var writer = Files.newBufferedWriter(file);
                var printer = format.print(writer)) {
            for (var recommendation : recommendations) {
                printer.printRecord(
                        recommendation.memberId(),
                        recommendation.fullName(),
                        String.format(Locale.ROOT, "%.1f", recommendation.priorityScore()),
                        recommendation.recommendedWindow());
            }
        }
    }
}
