import java.time.Instant;

public record Contact(
        long memberId,
        Instant lastContactUtc,
        String outcome) {
}
