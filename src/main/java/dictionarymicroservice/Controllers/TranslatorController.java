package dictionarymicroservice.Controllers;


import dictionarymicroservice.Entities.FavoriteWords;
import dictionarymicroservice.Services.FavoriteWordsService;
import dictionarymicroservice.Services.TranslatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TranslatorController {
    private final TranslatorService translatorService;
    private final FavoriteWordsService favoriteWordsService;
    TranslatorController(TranslatorService translatorService, FavoriteWordsService favoriteWordsService) {
        this.translatorService = translatorService;
        this.favoriteWordsService = favoriteWordsService;
    }

    @PostMapping("/translate")
    public Map<String, Object> translate(@RequestBody Map<String, List<String>> payload) {
        List<String> words = payload.get("words");
        Map<String, Object> result = translatorService.translateWords(words);
        System.out.println("Returning result: " + result);  // <--- ADD THIS LINE
        return result;
    }

    @PostMapping("/addFavorite")
    public ResponseEntity<Void> addFavorite(@RequestBody Map<String, String> payload) {
        String word = payload.get("word");
        String ownerUsername = payload.get("ownerUsername");

        FavoriteWords newWord = new FavoriteWords();
        newWord.setCounter(0);
        newWord.setWord(word);
        newWord.setOwnerUsername(ownerUsername);
        newWord.setDateAdded(LocalDateTime.now());
        newWord.setLastUsed(LocalDateTime.now());

        favoriteWordsService.saveToFavorite(newWord);

        return ResponseEntity.ok().build();
    }
}
