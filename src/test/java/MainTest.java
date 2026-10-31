import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args, new PrintStream(output), new PrintStream(errors));
    }

    @Test
    void fixedDateProducesReproducibleResults() {
        assertEquals(0, run("--top", "2", "--as-of", "2025-11-06T12:00:00Z"));
        assertTrue(output.toString().contains("Isaac Regev"));
        assertTrue(output.toString().contains("8.6"));
        assertEquals(3, output.toString().lines().count());
        assertEquals("", errors.toString());
    }

    @Test
    void explainsHolidayResults() {
        assertEquals(0, run("--as-of", "2025-11-07T12:00:00Z"));
        assertTrue(output.toString().contains("UTC date is a holiday"));
    }

    @Test
    void explainsWhenNoMemberQualifies() {
        assertEquals(0, run("--as-of", "2020-01-01T12:00:00Z"));
        assertTrue(output.toString().contains("No members are due"));
    }

    @Test
    void explainsZeroLimit() {
        assertEquals(0, run("--top", "0"));
        assertTrue(output.toString().contains("No results requested"));
    }

    @Test
    void rejectsInvalidOrMissingDates() {
        assertEquals(1, run("--as-of", "not-a-date"));
        assertTrue(errors.toString().contains("Usage:"));
        errors.reset();
        assertEquals(1, run("--as-of"));
        assertTrue(errors.toString().contains("Missing value"));
    }

    @Test
    void exportFailureReportsPathWithoutStackTrace(@TempDir Path directory) {
        Path file = directory.resolve("missing-directory/checkins.csv");
        assertEquals(1, run("--export", file.toString()));
        assertTrue(errors.toString().contains("Cannot export to " + file));
        assertFalse(errors.toString().contains("at Main."));
    }

    @Test
    void missingInputReportsFilenameWithoutStackTrace(@TempDir Path directory) throws Exception {
        // Start in an empty directory rather than changing or removing the supplied data.
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"), "Main")
                .directory(directory.toFile()).redirectErrorStream(true).start();
        String message = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(1, process.waitFor());
        assertTrue(message.contains("Cannot load data/members.csv"));
        assertFalse(message.contains("at Main."));
    }
}
