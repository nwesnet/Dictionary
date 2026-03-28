package dictionarymicroservice.Controllers;

import dictionarymicroservice.Entities.FavoriteWords;
import dictionarymicroservice.Services.FavoriteWordsService;
import dictionarymicroservice.Services.TranslatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class TrainingController {
    TranslatorService translatorService;
    FavoriteWordsService favoriteWordsService;
    TrainingController(TranslatorService translatorService, FavoriteWordsService favoriteWordsService) {
        this.translatorService = translatorService;
        this.favoriteWordsService = favoriteWordsService;
    }
    @PostMapping("/getWordsForTraining")
    public Map<String, Object> startEnTraining(@RequestBody String username) {
        List<String> words = favoriteWordsService.getWordsForTraining(username);

        return translatorService.translateWords(words);
    }

    @PostMapping("/updateWordCounter")
    public ResponseEntity<Void> updateWordCounter(@RequestBody Map<String, Object> body) {
        String word = (String) body.get("word");
        String username = (String) body.get("username");
        Object correctObj = body.get("correct");
        boolean correct = (correctObj instanceof Boolean) ? (Boolean) correctObj
            : Boolean.parseBoolean(String.valueOf(correctObj));

        favoriteWordsService.applyTrainingResult(username, word, correct);

        return ResponseEntity.ok().build();
    }
}
