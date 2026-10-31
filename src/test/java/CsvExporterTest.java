import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExporterTest {
    @Test
    void exportsQuotedNamesAndOneDecimalScores(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("checkins.csv");
        var recommendation = new CheckinService.CheckinRecommendation(
                2, "Cohen, \"Ruth\"", 8.0 / 7, "morning");

        CsvExporter.export(List.of(recommendation), file);

        try (var reader = Files.newBufferedReader(file);
                var parser = CSVFormat.DEFAULT.builder().setHeader()
                        .setSkipHeaderRecord(true).build().parse(reader)) {
            assertEquals(List.of("member_id", "full_name", "priority_score", "recommended_window"),
                    parser.getHeaderNames());
            var rows = parser.getRecords();
            assertEquals(1, rows.size());
            assertEquals("2", rows.get(0).get("member_id"));
            assertEquals("Cohen, \"Ruth\"", rows.get(0).get("full_name"));
            assertEquals("1.1", rows.get(0).get("priority_score"));
            assertEquals("morning", rows.get(0).get("recommended_window"));
        }
    }

    @Test
    void emptyResultsExportOnlyTheHeader(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("empty.csv");

        CsvExporter.export(List.of(), file);

        assertEquals(List.of("member_id,full_name,priority_score,recommended_window"),
                Files.readAllLines(file));
    }
}
