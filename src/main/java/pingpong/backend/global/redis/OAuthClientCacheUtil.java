package pingpong.backend.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuthClientCacheUtil {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CLIENT_PREFIX = "oauth:client:";
    private static final Duration CLIENT_TTL = Duration.ofDays(30);

    public void saveClient(String clientId, List<String> redirectUris) {
        try {
            String value = objectMapper.writeValueAsString(redirectUris);
            redisTemplate.opsForValue().set(CLIENT_PREFIX + clientId, value, CLIENT_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OAuth client data", e);
        }
    }

    public Optional<List<String>> getRedirectUris(String clientId) {
        String value = redisTemplate.opsForValue().get(CLIENT_PREFIX + clientId);
        if (value == null) return Optional.empty();
        try {
            List<String> uris = objectMapper.readValue(value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return Optional.of(uris);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
