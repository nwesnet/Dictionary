package dictionarymicroservice.Services;

import dictionarymicroservice.Entities.FavoriteWords;
import dictionarymicroservice.Repositories.FavoriteWordsRepository;
import org.springframework.stereotype.Service;

@Service
public class FavoriteWordsService {

    private final FavoriteWordsRepository favoriteWordsRepository;

    public FavoriteWordsService(FavoriteWordsRepository favoriteWordsRepository) {
        this.favoriteWordsRepository = favoriteWordsRepository;
    }

    public void saveToFavorite(FavoriteWords favoriteWords) {
        favoriteWordsRepository.save(favoriteWords);
    }


}
