import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CheckinServiceTest {
    private final CheckinService service = new CheckinService();
    private final Instant asOf = Instant.parse("2025-11-06T12:00:00Z");

    @Test
    void requiresSevenCompleteDays() {
        Member member = new Member(1, "Ruth Cohen", 82, "call", Set.of());
        Instant sevenDaysAgo = asOf.minus(7, ChronoUnit.DAYS);

        assertTrue(service.isEligible(member,
                new Contact(1, sevenDaysAgo, "ok"), Set.of(), asOf));
        assertFalse(service.isEligible(member,
                new Contact(1, sevenDaysAgo.plusSeconds(1), "ok"), Set.of(), asOf));
        assertFalse(service.isEligible(member,
                new Contact(1, asOf.plusSeconds(3600), "ok"), Set.of(), asOf));
    }

    @Test
    void neverContactedQualifiesButBlankChannelDoesNot() {
        Member member = new Member(1, "Ruth Cohen", 82, "call", Set.of());
        Member blankChannel = new Member(2, "Joseph Levi", 76, " \t ", Set.of());

        assertTrue(service.isEligible(member, null, Set.of(), asOf));
        assertFalse(service.isEligible(blankChannel, null, Set.of(), asOf));
    }

    @Test
    void holidayStartsAtUtcMidnight() {
        Member member = new Member(1, "Ruth Cohen", 82, "call", Set.of());
        Set<LocalDate> holidays = Set.of(LocalDate.parse("2025-11-07"));

        assertTrue(service.isEligible(member, null, holidays,
                Instant.parse("2025-11-06T23:59:59Z")));
        assertFalse(service.isEligible(member, null, holidays,
                Instant.parse("2025-11-07T00:00:00Z")));
        assertTrue(service.findDueMembers(List.of(member), Map.of(), holidays,
                Instant.parse("2025-11-07T12:00:00Z"), 5).isEmpty());
    }

    @Test
    void scoreIncludesAllBonusesAndOnlyCompleteDays() {
        Member member = new Member(1, "Ruth Cohen", 80, "call",
                Set.of("recent_discharge", "lives_alone"));
        Contact contact = new Contact(1,
                asOf.minus(10, ChronoUnit.DAYS).minusSeconds(86399), "no_answer");

        // Risk 5 + age 1 + no_answer 1 + ten complete days / 7.
        assertEquals(7 + 10 / 7.0, service.calculateScore(member, contact, asOf), 1e-10);
    }

    @Test
    void neverContactedReceivesOnlyAgeAndRiskPoints() {
        Member member = new Member(1, "Ruth Cohen", 80, "call",
                Set.of("recent_discharge", "lives_alone"));

        assertEquals(6.0, service.calculateScore(member, null, asOf));
        assertEquals(0.0, service.calculateScore(
                new Member(2, "Joseph Levi", 79, "sms", Set.of()), null, asOf));
    }

    @Test
    void escalateAddsNoOutcomeBonus() {
        Member member = new Member(1, "Joseph Levi", 79, "sms", Set.of());
        Contact contact = new Contact(1, asOf.minus(7, ChronoUnit.DAYS), "escalate");

        assertEquals(1.0, service.calculateScore(member, contact, asOf));
    }

    @Test
    void filtersSortsAndLimitsRecommendations() {
        var members = List.of(
                new Member(10, "Joseph Levi", 79, "sms", Set.of()),
                new Member(2, "Danny Israel", 79, "call", Set.of()),
                new Member(20, "Ruth Cohen", 80, "call", Set.of("recent_discharge")),
                new Member(1, "Sarah Adler", 90, "call", Set.of("recent_discharge")),
                new Member(3, "No Channel", 90, " ", Set.of("recent_discharge")));
        var contacts = Map.of(
                10L, new Contact(10, asOf.minus(8, ChronoUnit.DAYS), "ok"),
                2L, new Contact(2, asOf.minus(8, ChronoUnit.DAYS), "ok"),
                1L, new Contact(1, asOf.minus(1, ChronoUnit.DAYS), "ok"));

        var results = service.findDueMembers(members, contacts, Set.of(), asOf, 10);

        assertEquals(List.of(20L, 2L, 10L),
                results.stream().map(CheckinService.CheckinRecommendation::memberId).toList());
        assertEquals("Ruth Cohen", results.get(0).fullName());
        assertEquals("morning", results.get(0).recommendedWindow());
        assertEquals("afternoon", results.get(1).recommendedWindow());
        assertEquals(8 / 7.0, results.get(1).priorityScore(), 1e-10);
        assertEquals(results.subList(0, 2),
                service.findDueMembers(members, contacts, Set.of(), asOf, 2));
    }

    @Test
    void handlesZeroNegativeAndEmptyResults() {
        var members = List.of(new Member(1, "Ruth Cohen", 82, "call", Set.of()));

        assertTrue(service.findDueMembers(members, Map.of(), Set.of(), asOf, 0).isEmpty());
        assertTrue(service.findDueMembers(List.of(), Map.of(), Set.of(), asOf, 5).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.findDueMembers(members, Map.of(), Set.of(), asOf, -1));
    }
}
