package dictionarymicroservice.Controllers;


import dictionarymicroservice.Services.TranslatorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TranslatorController {
    private final TranslatorService translatorService;

    TranslatorController(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }

    @PostMapping("/translate")
    public Map<String, Object> translate(@RequestBody Map<String, List<String>> payload) {
        List<String> words = payload.get("words");
        Map<String, Object> result = translatorService.translateWords(words);
        System.out.println("Returning result: " + result);  // <--- ADD THIS LINE
        return result;
    }
}
