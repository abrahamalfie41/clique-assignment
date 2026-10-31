import java.util.Set;

public record Member(
        long id,
        String fullName,
        int age,
        String preferredChannel,
        Set<String> riskFlags) {
}
