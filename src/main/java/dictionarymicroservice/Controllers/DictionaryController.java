package dictionarymicroservice.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DictionaryController {
    @GetMapping("/training")
    public String trainingPage() {
        return "training";
    }
}
