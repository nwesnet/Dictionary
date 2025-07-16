package dictionarymicroservice.Services;


import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranslatorService {


    public Map<String, Object> translateWords(List<String> words) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String word : words) {
            String originalWord = word;
            String dictOutput = callDict(word);
            System.out.println(dictOutput);
            if (dictOutput.contains("No definitions found for")) {
                if (dictOutput.contains("perhaps you mean:")) {
                    word = extractSuggestion(dictOutput);
                    dictOutput = callDict(word);
                } else {
                    result.put(word, Collections.singletonMap("Not found", null));
                }
            }
            System.out.println(dictOutput);
            Map<String, Object> parsed = parseDictOutput(dictOutput);
            result.put(originalWord, parsed);
        }
        return result;
    }

    private String extractSuggestion(String dictOutput) {
        Pattern pattern = Pattern.compile("mueller-[^:]*:\\s*(\\w+)");
        Matcher matcher = pattern.matcher(dictOutput);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String callDict(String word) {
        StringBuilder dictOutput = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("dict", word);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                dictOutput.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            dictOutput.append("Error: ").append(e.getMessage());
        }
        return dictOutput.toString();
    }

    private Map<String, Object> parseDictOutput(String dictOutput) {
        Map<String, Object> result = new HashMap<>();
        String transcription = null;
        String partOfSpeech = null;
        List<String> irregularVerbs = new ArrayList<>();
        String translation = null;

        Pattern transcriptionPattern = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher transcriptionMatcher = transcriptionPattern.matcher(dictOutput);
        int transcEnd = 0;
        while (transcriptionMatcher.find()) {
            String found = transcriptionMatcher.group(1);
            if (!found.toLowerCase().contains("mueller")) {
                transcription = "[" + found + "]";
                transcEnd = transcriptionMatcher.end();
                break;
            }
        }
        result.put("transcription", transcription);

        Pattern partOfSpeechPattern = Pattern.compile("_(\\w+)\\.");
        Matcher partOfSpeechMatcher = partOfSpeechPattern.matcher(dictOutput.substring(transcEnd));
        int posEnd = 0;
        while (partOfSpeechMatcher.find()) {
            String found = partOfSpeechMatcher.group(1);
            partOfSpeech = " " + found + " ";
            posEnd = partOfSpeechMatcher.end();
            break;
        }
        result.put("partOfSpeech", partOfSpeech);
        if (partOfSpeech.equals("v")) {
            Pattern irregularVerbPatter = Pattern.compile("\\(([^)]*)\\)");
            Matcher irregularVerbMatcher = irregularVerbPatter.matcher(dictOutput.substring(posEnd));

            if (irregularVerbMatcher.find()) {
                String insidePattern = irregularVerbMatcher.group(1);

                String[] parts = insidePattern.split("[,;]");
                for (String part : parts) {
                    String word = part.trim();

                    if (!word.startsWith("_") && word.matches("[A-Za-z'-]+")) {
                        irregularVerbs.add(word);
                    }
                }
            }
            result.put("irregularVerbsForms", irregularVerbs);
        }

        // Assume dictOutput is your text, and posEnd is the index just after "_n."
        int translationWordStart = posEnd;
        Pattern translationPattern = Pattern.compile("1\\)");
        Matcher translationMatcher = translationPattern.matcher(dictOutput.substring(posEnd));
        if (translationMatcher.find()) {
            translationWordStart = posEnd + translationMatcher.end(); // add posEnd for absolute index!
        }

// Get the substring from translationWordStart to the end
        String afterOne = dictOutput.substring(translationWordStart);

// Find comma, semicolon, or line break
        int commaIndex = afterOne.indexOf(",");
        int semicolonIndex = afterOne.indexOf(";");
        int lineBreakIndex = afterOne.indexOf("\n");

// Set initial endIndex to the length of the string
        int endIndex = afterOne.length();

// Only consider positive indices (i.e., if the symbol is present)
        if (commaIndex >= 0 && commaIndex < endIndex) endIndex = commaIndex;
        if (semicolonIndex >= 0 && semicolonIndex < endIndex) endIndex = semicolonIndex;
        if (lineBreakIndex >= 0 && lineBreakIndex < endIndex) endIndex = lineBreakIndex;

// Now extract the translation
        translation = afterOne.substring(0, endIndex).trim();
        result.put("translation", translation);

        // last thing

        List<String> majorBlocks = new ArrayList<>();
        List<String> subBlocks = new ArrayList<>();
        List<String> subBlocksWithPOS = new ArrayList<>(); // NEW: Sub-blocks marked with POS

// 1. Find all major blocks (e.g., "2. _v.")
        Pattern majorBlockPattern = Pattern.compile("(?m)(^\\s*\\d+\\.\\s*_?.*?$)");
        Matcher majorBlockMatcher = majorBlockPattern.matcher(dictOutput);

// Store indices for splitting
        List<Integer> majorBlockIndices = new ArrayList<>();
        List<String> posHeaders = new ArrayList<>(); // NEW: To store headers
        while (majorBlockMatcher.find()) {
            majorBlockIndices.add(majorBlockMatcher.start());
            posHeaders.add(majorBlockMatcher.group().trim());
        }
        majorBlockIndices.add(dictOutput.length());

        if (majorBlockIndices.size() > 1) {
            // === ORIGINAL LOGIC FOR MAJOR BLOCKS ===
            for (int i = 0; i < majorBlockIndices.size() - 1; i++) {
                int start = majorBlockIndices.get(i);
                int end = majorBlockIndices.get(i + 1);
                String majorBlock = dictOutput.substring(start, end).trim();
                String header = posHeaders.get(i); // e.g. "2. _v."
                if (!majorBlock.isEmpty()) {
                    majorBlocks.add(majorBlock);

                    // 3. For each major block, find sub-blocks (n), *), #))
                    Pattern subBlockPattern = Pattern.compile("(?m)^\\s*(\\d+|\\*|#)\\)");
                    Matcher subBlockMatcher = subBlockPattern.matcher(majorBlock);

                    List<Integer> subBlockIndices = new ArrayList<>();
                    while (subBlockMatcher.find()) {
                        subBlockIndices.add(subBlockMatcher.start());
                    }
                    subBlockIndices.add(majorBlock.length());

                    for (int j = 0; j < subBlockIndices.size() - 1; j++) {
                        int sStart = subBlockIndices.get(j);
                        int sEnd = subBlockIndices.get(j + 1);
                        String subBlock = majorBlock.substring(sStart, sEnd).trim();
                        if (!subBlock.isEmpty()) {
                            subBlocks.add(subBlock);
                            // Add POS header in front of sub-block
                            subBlocksWithPOS.add(header + "\n" + subBlock);
                        }
                    }
                }
            }
        } else {
            // === NO MAJOR BLOCKS FOUND: JUST AFTER FIRST PART OF SPEECH ===

            // Try to find the first POS (_n., _v., etc.)
            Pattern posPattern = Pattern.compile("_(\\w+)\\.");
            Matcher posMatcher = posPattern.matcher(dictOutput);
            if (posMatcher.find()) {
                posEnd = posMatcher.end();
                String afterPOS = dictOutput.substring(posEnd);

                // Find all sub-blocks like 1) ... 2) ... etc.
                Pattern subBlockPattern = Pattern.compile("(?m)^\\s*(\\d+|\\*|#)\\)([\\s\\S]*?)(?=^\\s*(\\d+|\\*|#)\\)|\\z)", Pattern.MULTILINE);
                Matcher subBlockMatcher = subBlockPattern.matcher(afterPOS);

                while (subBlockMatcher.find()) {
                    String subBlock = subBlockMatcher.group().trim();
                    if (!subBlock.isEmpty()) {
                        subBlocks.add(subBlock);
                        // Add POS header in front of sub-block
                        String header = "_" + posMatcher.group(1) + ".";
                        subBlocksWithPOS.add(header + "\n" + subBlock);
                    }
                }
                // The whole block as a "major block"
                majorBlocks.add(afterPOS.trim());
            }
        }
        result.put("usageInText", majorBlocks);
        return result;
    }
}
