import org.apache.commons.csv.CSVFormat;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataLoader {
    public List<Member> loadMembers(Path file) throws IOException {
        List<Member> members = new ArrayList<>();
        // Reads column names from the first row, skips first row.
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        // Row by row parsing of CSV file, trims whitespaces, constructs Member record.
        try (var reader = Files.newBufferedReader(file);
             var rows = format.parse(reader)) {
            for (var row : rows) {
                Set<String> riskFlags = new HashSet<>();
                for (String flag : row.get("risk_flags").split(";")) {
                    if (!flag.isBlank()) {
                        riskFlags.add(flag.trim());
                    }
                }

                Member member = new Member(
                        Long.parseLong(row.get("member_id").trim()),
                        row.get("full_name").trim(),
                        Integer.parseInt(row.get("age").trim()),
                        row.get("preferred_channel").trim(),
                        Set.copyOf(riskFlags));
                members.add(member);
            }
        }
        return members;
    }

    public Map<Long, Contact> loadContacts(Path file) throws IOException {
        Map<Long, Contact> contacts = new HashMap<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (var reader = Files.newBufferedReader(file);
             var rows = format.parse(reader)) {
            for (var row : rows) {
                Contact contact = new Contact(
                        Long.parseLong(row.get("member_id").trim()),
                        Instant.parse(row.get("last_contact_utc").trim()),
                        row.get("outcome").trim());

                // The file contains one last-contact record per member.
                if (contacts.putIfAbsent(contact.memberId(), contact) != null) {
                    throw new IllegalArgumentException("Duplicate contact for member " + contact.memberId());
                }
            }
        }
        return contacts;
    }

    public Set<LocalDate> loadHolidays(Path file) throws IOException {
        Set<LocalDate> holidays = new HashSet<>();
        try (var reader = Files.newBufferedReader(file)) {
            // Converts JSON strings to Java dates.
            String[] dates = new Gson().fromJson(reader, String[].class);
            if (dates == null) {
                throw new IllegalArgumentException("Holiday file must contain a JSON date array");
            }
            for (String date : dates) {
                holidays.add(LocalDate.parse(date));
            }
        }
        return holidays;
    }
}
