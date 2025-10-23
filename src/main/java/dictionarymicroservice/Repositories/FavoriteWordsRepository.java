package dictionarymicroservice.Repositories;

import dictionarymicroservice.Entities.FavoriteWords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteWordsRepository extends JpaRepository<FavoriteWords, String> {
    List<FavoriteWords> findByOwnerUsername(String ownerUsername);
}
