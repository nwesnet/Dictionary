package dictionarymicroservice.Services;

import dictionarymicroservice.Entities.FavoriteWords;
import dictionarymicroservice.Repositories.FavoriteWordsRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteWordsService {

    private final FavoriteWordsRepository favoriteWordsRepository;

    private static final Duration[] SRS = new Duration[] {
            Duration.ofMinutes(15),
            Duration.ofHours(2),
            Duration.ofDays(1),
            Duration.ofDays(7),
            Duration.ofDays(28)
    };

    private static final int DEFAULT_LIMIT = 10;

    public FavoriteWordsService(FavoriteWordsRepository favoriteWordsRepository) {
        this.favoriteWordsRepository = favoriteWordsRepository;
    }

    public void saveToFavorite(FavoriteWords favoriteWords) {
        favoriteWordsRepository.save(favoriteWords);
    }

    public Optional<FavoriteWords> findByUsernameAndWord(String username, String word) {
        return favoriteWordsRepository.findByOwnerUsernameAndWord(username, word);
    }

    public List<FavoriteWords> getFavoriteWordByUsername(String username) {
        return favoriteWordsRepository.findByOwnerUsername(username);
    }

    public List<String> getWordsForTraining(String username) {
        return getWordsForTraining(username, DEFAULT_LIMIT);
    }

    public List<String> getWordsForTraining(String username, int limit) {
        List<FavoriteWords> all = getFavoriteWordByUsername(username);
        if (limit <= 0 || all.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        List<FavoriteWords> due = new ArrayList<>();
        List<FavoriteWords> notDue = new ArrayList<>();

        for (FavoriteWords word : all) {
            int counter = Math.max(0, Math.min(word.getCounter(), SRS.length - 1));
            LocalDateTime nextDue = computeNextDue(word, counter);

            if (nextDue == null || !nextDue.isAfter(now)) {
                due.add(word);
            } else {
                notDue.add(word);
            }
        }

        due.sort(Comparator
                .comparingInt(FavoriteWords::getCounter)
                .thenComparing(FavoriteWords::getLastUsed, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(FavoriteWords::getWord, String.CASE_INSENSITIVE_ORDER)
        );

        List<String> result = due.stream()
                .map(FavoriteWords::getWord)
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new)
        );
        // If the limit is not reached, then I add more words
        // calculate the limit
        int remaining = limit - result.size();
        if (remaining > 0 && !notDue.isEmpty()) {
            // Sort not due by how soon they are due, then by counter, then alphabetical
            notDue.sort(Comparator
                    .comparing((FavoriteWords w) -> {
                        int c = Math.max(0, Math.min(w.getCounter(), SRS.length - 1));

                        return computeNextDue(w, c);
                    })
                    .thenComparing(FavoriteWords::getCounter)
                    .thenComparing(FavoriteWords::getWord, String.CASE_INSENSITIVE_ORDER)
            );
            // add the additional words to the result array
            notDue.stream()
                    .limit(remaining)
                    .map(FavoriteWords::getWord)
                    .forEach(result::add);
        }

        return result;
    }

    private LocalDateTime computeNextDue(FavoriteWords word, int c) {
        if (word.getLastUsed() == null) return null;
        Duration interval = SRS[c];
        return word.getLastUsed().plus(interval);
    }

    public void applyTrainingResult(String username, String word, boolean correct) {
        FavoriteWords fw = findByUsernameAndWord(username, word)
                .orElseThrow(() -> new RuntimeException("The word didn't found"));

        int counter = fw.getCounter();
        if (correct) {
            fw.setCounter(Math.min(counter + 1, SRS.length - 1));
        } else {
            fw.setCounter(Math.max(0, counter - 1));
        }

        fw.setLastUsed(LocalDateTime.now());
        saveToFavorite(fw);
    }

}
