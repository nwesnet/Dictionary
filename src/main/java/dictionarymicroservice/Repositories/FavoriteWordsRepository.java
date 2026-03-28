package dictionarymicroservice.Repositories;

import dictionarymicroservice.Entities.FavoriteWords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteWordsRepository extends JpaRepository<FavoriteWords, Long> {
    List<FavoriteWords> findByOwnerUsername(String ownerUsername);
    Optional<FavoriteWords> findByOwnerUsernameAndWord(String ownerUsername, String word);
}
