import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CheckinService {

    public record CheckinRecommendation(
            long memberId,
            String fullName,
            double priorityScore,
            String recommendedWindow) {
    }

    public boolean isEligible(
            Member member,
            Contact contact,
            Set<LocalDate> holidays,
            Instant asOf) {
        LocalDate today = asOf.atZone(ZoneOffset.UTC).toLocalDate();

        if (holidays.contains(today)) {
            return false;
        }
        if (member.preferredChannel().isBlank()) {
            return false;
        }
        if (contact == null) {
            return true;
        }

        long days = Duration.between(contact.lastContactUtc(), asOf).toDays();
        return days >= 7;
    }

    public double calculateScore(Member member, Contact contact, Instant asOf) {
        double score = 0;

        if (member.riskFlags().contains("recent_discharge")) {
            score += 3;
        }
        if (member.riskFlags().contains("lives_alone")) {
            score += 2;
        }
        if (member.age() >= 80) {
            score += 1;
        }

        if (contact != null) {
            if ("no_answer".equals(contact.outcome())) {
                score += 1;
            }

            long days = Duration.between(contact.lastContactUtc(), asOf).toDays();
            score += days / 7.0;
        }

        return score;
    }

    public List<CheckinRecommendation> findDueMembers(
            List<Member> members,
            Map<Long, Contact> contacts,
            Set<LocalDate> holidays,
            Instant asOf,
            int top) {
        if (top < 0) {
            throw new IllegalArgumentException("top must be nonnegative");
        }

        List<CheckinRecommendation> recommendations = new ArrayList<>();

        for (Member member : members) {
            Contact contact = contacts.get(member.id());

            if (isEligible(member, contact, holidays, asOf)) {
                double score = calculateScore(member, contact, asOf);
                String recommendedWindow = member.age() >= 80 ? "morning" : "afternoon";
                recommendations.add(new CheckinRecommendation(
                        member.id(),
                        member.fullName(),
                        score,
                        recommendedWindow));
            }
        }

        recommendations.sort(
                Comparator.comparingDouble(CheckinRecommendation::priorityScore)
                        .reversed()
                        .thenComparingLong(CheckinRecommendation::memberId));

        return recommendations.stream().limit(top).toList();
    }
}
