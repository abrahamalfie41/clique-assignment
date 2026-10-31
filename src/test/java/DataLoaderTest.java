import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataLoaderTest {
    @Test
    void loadsHolidayDatesAndRemovesDuplicates(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("holidays.json");
        Files.writeString(file, """
                ["2025-11-07", "2025-12-25", "2025-11-07"]
                """);

        var holidays = new DataLoader().loadHolidays(file);

        assertEquals(Set.of(LocalDate.of(2025, 11, 7), LocalDate.of(2025, 12, 25)), holidays);
    }

    @Test
    void loadsEmptyHolidayArray(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("holidays.json");
        Files.writeString(file, "[]");

        assertEquals(Set.of(), new DataLoader().loadHolidays(file));
    }

    @Test
    void loadsContactsByMemberId(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("last_contacts.csv");
        Files.writeString(file, """
                member_id,last_contact_utc,outcome
                2, 2025-10-28T17:00:00Z , no_answer
                10,2025-10-30T08:00:00Z,ok
                """);

        var contacts = new DataLoader().loadContacts(file);

        assertEquals(2, contacts.size());
        assertEquals(new Contact(2, Instant.parse("2025-10-28T17:00:00Z"), "no_answer"),
                contacts.get(2L));
        assertEquals(new Contact(10, Instant.parse("2025-10-30T08:00:00Z"), "ok"),
                contacts.get(10L));
        assertNull(contacts.get(3L));
    }

    @Test
    void rejectsDuplicateContactIds(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("last_contacts.csv");
        Files.writeString(file, """
                member_id,last_contact_utc,outcome
                2,2025-10-28T17:00:00Z,no_answer
                2,2025-10-30T08:00:00Z,ok
                """);

        assertThrows(IllegalArgumentException.class, () -> new DataLoader().loadContacts(file));
    }

    @Test
    void loadsMembersWithQuotedNamesAndEmptyFields(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("members.csv");
        Files.writeString(file, """
                member_id,full_name,age,preferred_channel,risk_flags
                1,"Cohen, Ruth",82, call , lives_alone;recent_discharge
                2,Joseph Levi,76,   ,
                """);

        var members = new DataLoader().loadMembers(file);

        assertEquals(2, members.size());
        assertEquals(new Member(1, "Cohen, Ruth", 82, "call",
                Set.of("lives_alone", "recent_discharge")), members.get(0));
        assertEquals(new Member(2, "Joseph Levi", 76, "", Set.of()), members.get(1));
    }
}
