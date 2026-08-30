package by.vs.erp.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TokenHasherTest {

    private final TokenHasher tokenHasher = new TokenHasher();

    @Test
    @DisplayName("hash: возвращает детерминированный SHA-256 хеш в hex-формате (64 символа)")
    void hash_ReturnsDeterministicHexDigest() {
        String rawToken = "some.jwt.refresh.token.value";

        String hash1 = tokenHasher.hash(rawToken);
        String hash2 = tokenHasher.hash(rawToken);

        assertEquals(hash1, hash2, "Хеш одного и того же токена должен быть стабильным");
        assertEquals(64, hash1.length(), "SHA-256 в hex должен занимать 64 символа");
        assertTrue(hash1.matches("^[0-9a-f]{64}$"), "Хеш должен состоять только из hex-символов в нижнем регистре");
    }

    @Test
    @DisplayName("hash: никогда не возвращает исходный токен в открытом виде")
    void hash_NeverReturnsRawToken() {
        String rawToken = "raw-refresh-token-12345";

        String hash = tokenHasher.hash(rawToken);

        assertNotEquals(rawToken, hash);
    }

    @Test
    @DisplayName("hash: разные токены дают разные хеши (устойчивость к коллизиям на практике)")
    void hash_DifferentTokens_ProduceDifferentHashes() {
        String hashA = tokenHasher.hash("token-A");
        String hashB = tokenHasher.hash("token-B");

        assertNotEquals(hashA, hashB);
    }

    @Test
    @DisplayName("hash: чувствителен к регистру и любому изменению исходной строки")
    void hash_IsCaseSensitive() {
        String hashLower = tokenHasher.hash("abc123");
        String hashUpper = tokenHasher.hash("ABC123");

        assertNotEquals(hashLower, hashUpper);
    }

    @Test
    @DisplayName("hash: соответствует известному эталонному значению SHA-256('')")
    void hash_MatchesKnownSha256ReferenceValue() {
        String expectedEmptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        String actual = tokenHasher.hash("");

        assertEquals(expectedEmptyHash, actual);
    }
}