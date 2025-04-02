package viettel.dac.intentanalysisservice.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Repository for caching and retrieving LLM prompts and responses.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class PromptCacheRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PROMPT_KEY_PREFIX = "prompt:";
    private static final String RESPONSE_KEY_PREFIX = "response:";
    private static final String OPTIMIZED_KEY_PREFIX = "optimized:";
    private static final long CACHE_TTL_HOURS = 24;
    private static final long OPTIMIZED_CACHE_TTL_HOURS = 72; // Longer TTL for optimized prompts

    /**
     * Cache a prompt and its response.
     *
     * @param prompt The LLM prompt
     * @param response The LLM response
     */
    public void cachePromptResponse(String prompt, String response) {
        try {
            String promptHash = getPromptHash(prompt);
            String promptKey = PROMPT_KEY_PREFIX + promptHash;
            String responseKey = RESPONSE_KEY_PREFIX + promptHash;

            redisTemplate.opsForValue().set(promptKey, prompt, CACHE_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(responseKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);

            // Add to the set of all prompts for similarity search
            redisTemplate.opsForSet().add("prompts", promptHash);

            log.debug("Cached prompt-response pair with hash: {}", promptHash);
        } catch (Exception e) {
            log.error("Error caching prompt-response: {}", e.getMessage());
        }
    }

    /**
     * Get a cached response for a prompt.
     *
     * @param prompt The LLM prompt
     * @return The cached response, or null if not found
     */
    public String getCachedResponse(String prompt) {
        try {
            String promptHash = getPromptHash(prompt);
            String responseKey = RESPONSE_KEY_PREFIX + promptHash;

            String response = redisTemplate.opsForValue().get(responseKey);
            if (response != null) {
                log.debug("Cache hit for prompt: {}", promptHash);
                return response;
            }

            log.debug("Cache miss for prompt: {}", promptHash);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving cached response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Find a similar response based on a basic similarity search.
     * In a real implementation, this would use more sophisticated methods.
     *
     * @param prompt The LLM prompt
     * @return The most similar cached response, or null if none found
     */
    public String findSimilarResponse(String prompt) {
        try {
            // Get all prompt hashes
            Set<String> promptHashes = redisTemplate.opsForSet().members("prompts");
            if (promptHashes == null || promptHashes.isEmpty()) {
                return null;
            }

            String mostSimilarHash = null;
            double highestSimilarity = 0.5; // Threshold for similarity

            // Simple similarity comparison (very basic - in production would use embeddings)
            for (String hash : promptHashes) {
                String cachedPrompt = redisTemplate.opsForValue().get(PROMPT_KEY_PREFIX + hash);
                if (cachedPrompt != null) {
                    double similarity = calculateSimilarity(prompt, cachedPrompt);
                    if (similarity > highestSimilarity) {
                        highestSimilarity = similarity;
                        mostSimilarHash = hash;
                    }
                }
            }

            if (mostSimilarHash != null) {
                String response = redisTemplate.opsForValue().get(RESPONSE_KEY_PREFIX + mostSimilarHash);
                log.debug("Found similar prompt with hash: {}, similarity: {}", mostSimilarHash, highestSimilarity);
                return response;
            }

            return null;
        } catch (Exception e) {
            log.error("Error finding similar response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Save an optimized prompt for future reference.
     *
     * @param originalPrompt The original prompt
     * @param optimizedPrompt The optimized prompt
     */
    public void saveOptimizedPrompt(String originalPrompt, String optimizedPrompt) {
        try {
            String key = OPTIMIZED_KEY_PREFIX + getPromptHash(originalPrompt);
            redisTemplate.opsForValue().set(key, optimizedPrompt, OPTIMIZED_CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error saving optimized prompt: {}", e.getMessage());
        }
    }

    /**
     * Get an optimized prompt.
     *
     * @param originalPrompt The original prompt
     * @return The optimized prompt, or null if not found
     */
    public String getOptimizedPrompt(String originalPrompt) {
        try {
            String key = OPTIMIZED_KEY_PREFIX + getPromptHash(originalPrompt);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error retrieving optimized prompt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the hash of a prompt for use as a cache key.
     *
     * @param prompt The prompt to hash
     * @return A hash string representation
     */
    private String getPromptHash(String prompt) {
        // Simple hashing using String.hashCode()
        // In production, use a more robust hashing algorithm
        return String.valueOf(prompt.hashCode());
    }

    /**
     * Calculate similarity between two strings.
     * This is a very basic implementation - in production use embeddings or similar.
     *
     * @param str1 First string
     * @param str2 Second string
     * @return Similarity score between 0 and 1
     */
    private double calculateSimilarity(String str1, String str2) {
        // Very basic similarity measure - word overlap
        // In production, use something more sophisticated like cosine similarity of embeddings
        Set<String> words1 = Set.of(str1.toLowerCase().split("\\s+"));
        Set<String> words2 = Set.of(str2.toLowerCase().split("\\s+"));

        int commonWords = 0;
        for (String word : words1) {
            if (words2.contains(word)) {
                commonWords++;
            }
        }

        return (double) commonWords / Math.max(words1.size(), words2.size());
    }

}