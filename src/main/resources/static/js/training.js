// /js/training.js
document.addEventListener("DOMContentLoaded", () => {
    const startEnBtn = document.getElementById("startEnBtn");
    const startRuBtn = document.getElementById("startRuBtn");
    const sourceTextArea = document.getElementById("source-text");
    const targetTextArea = document.getElementById("target-text");
    const contentInfo = document.getElementById("content-info");
    const contentExamples = document.getElementById("content-examples");
    const usernameEl = document.getElementById("current-username");

    const username = usernameEl ? usernameEl.textContent.trim() : "";

    /** Internal training queue: [{ word, info }, ...] */
    let queue = [];
    /** Currently displayed item: { word, info } */
    let current = null;



    init();

    function init() {
        attachHandlers();
        loadTrainingSet(username);
    }

    function attachHandlers() {
        if (startEnBtn) {
            startEnBtn.addEventListener("click", onStartEn);
        }
        if (startRuBtn) {
            startRuBtn.addEventListener("click", onStartRu);
        }
    }

    function onStartEn(event) {
        event.preventDefault();
        // move to next word from queue
        function showNextFromQueue() {
            if (!queue || queue.length === 0) {
                current = null;
                if (sourceTextArea) sourceTextArea.value = "";
                if (targetTextArea) targetTextArea.value = "";
                renderInfo(null);
                renderExamples(null);
                return;
            }

            current = queue.shift();

            // Prepare UI for answering
            if (sourceTextArea) sourceTextArea.value = current.word || "";
            if (targetTextArea) {
                targetTextArea.value = "";
                targetTextArea.focus();
            }
            renderInfo(null);       // clear side info until answer is checked
            renderExamples(null);   // clear examples until answer is checked
        }
        if (!queue || queue.length === 0) {
            loadTrainingSet(username).then(() => {
                showNextFromQueue();
            });

        } else {
            showNextFromQueue();
        }

    }

    function onStartRu(event) {
        event.preventDefault();

        if (!current) return;

        const userAnswerRaw = (targetTextArea?.value || "").trim();
        const expected = safeString(current.info?.translation);
        // 1. Base check: quals main translation
        let isCorrect = compareAnswers(userAnswerRaw, expected);
        // 2. FallBack: if not equal to base translation, check in major block
        if (!isCorrect && Array.isArray(current.info?.usageInText)) {
            const matchesMajorBlock = matchesInUsageBlocks(userAnswerRaw, current.info.usageInText);
            if (matchesMajorBlock) {
                isCorrect = true;
            }
        }

        // Render additional info about the word
        renderInfo(current.info);
        renderExamples(current.info?.usageInText);

        // Tell backend we practiced this word
        updateWordCounter(username, current.word, isCorrect)
            .catch(err => {
                // Non-fatal; just log it
                console.error("Failed to update word counter:", err);
            });
        // Do NOT advance automatically; next word appears when user clicks the left “S” again.
    }
    /**
     * Second-level check: if user answer is not equal to base translation,
     * see if it appears inside the major blocks (usageInText).
     *
     * usageInText is an array of strings (your "major blocks").
     * For example for "example" it may contain:
     *  "1) пример; ... 2) примерное наказание, урок; ... 3) образец"
     */
    function matchesInUsageBlocks(userAnswer, usageInText) {
        if (!userAnswer && Array.isArray(usageInText) || usageInText === 0) {
            return false;
        }

        const normUser = normalize(userAnswer);
        if (!normUser) return false;

        const joined = usageInText.join(" ");
        const normJoined = normalize(joined);

        const paddedText = " " + normJoined + " ";
        const paddedUser = " " + normUser + " ";

        return paddedText.includes(paddedUser);
    }


    /**
     * Loads the training set map: { word: info, ... } and fills the queue.
     * The controller expects raw text for @RequestBody String username.
     */
    function loadTrainingSet(ownerUsername) {
        if (!ownerUsername) {
            console.warn("No username found in #current-username");
            return Promise.resolve([]);
        }
        fetch("/getWordsForTraining", {
            method: "POST",
            headers: { "Content-Type": "text/plain" },
            body: ownerUsername
        })
            .then(r => {
                if (!r.ok) throw new Error("Failed to fetch training set");
                return r.json();
            })
            .then(resultMap => {
                // resultMap shape: { [word]: { translation, transcription, partOfSpeech, irregularVerbsForms, usageInText, ... } }
                queue = Object.keys(resultMap).map(w => ({ word: w, info: resultMap[w] || {} }));
                shuffle(queue);
                // Optional: auto-start first word
                // onStartEn(new Event('click'));
            })
            .catch(err => console.error(err));
    }

    /**
     * POST /updateWordCounter with { username, word }
     */
    function updateWordCounter(ownerUsername, word, correct) {
        return fetch("/updateWordCounter", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: ownerUsername, word, correct })
        }).then(r => {
            if (!r.ok) throw new Error("updateWordCounter failed with status " + r.status);
        });
    }

    /** Compare user answer to expected translation (case-insensitive; lenient to punctuation and minor spacing). */
    function compareAnswers(userAnswer, expected) {
        if (!expected) return false;

        // Normalize both answers
        const normUser = normalize(userAnswer);
        // Many dictionary outputs can contain splits like "," ";" "/" — allow any of them.
        const candidates = splitVariants(expected).map(normalize);

        return candidates.some(c => c && c === normUser);
    }

    function splitVariants(text) {
        // split by ',', ';', '/', ' or ', ' | '
        return text
            .split(/,\s*|;\s*|\/|\s+\bor\b\s+|\s+\|\s+/i)
            .map(s => s.trim())
            .filter(Boolean);
    }

    function normalize(s) {
        return safeString(s)
            .toLowerCase()
            .replace(/[()[\]{}"“”'’`.,;:!?]/g, "") // remove punctuation
            .replace(/\s+/g, " ")
            .trim();
    }

    function safeString(v) {
        return (v == null) ? "" : String(v);
    }

    /** Render the info panel (right column). */
    function renderInfo(info) {
        if (!contentInfo) return;
        if (!info) {
            contentInfo.innerHTML = "";
            return;
        }

        let html = "";

        if (info.partOfSpeech) {
            html += `<div><strong>Part of speech:</strong> ${escapeHTML(info.partOfSpeech)}</div>`;
        }
        if (info.transcription) {
            html += `<div><strong>Transcription:</strong> ${escapeHTML(info.transcription)}</div>`;
        }
        // Note: your TranslatorService puts this as "irregularVerbsForms"
        if (Array.isArray(info.irregularVerbsForms) && info.irregularVerbsForms.length > 0) {
            html += `<div><strong>Irregular verb:</strong> ${escapeHTML(info.irregularVerbsForms.join(", "))}</div>`;
        }
        if (info.translation) {
            html += `<div><strong>Base translation:</strong> ${escapeHTML(info.translation)}</div>`;
        }

        contentInfo.innerHTML = html;
    }

    /** Render usage examples (left column bottom). */
    function renderExamples(usageInText) {
        if (!contentExamples) return;
        if (!Array.isArray(usageInText) || usageInText.length === 0) {
            contentExamples.innerHTML = "";
            return;
        }
        let examplesHTML = "";
        for (const block of usageInText) {
            examplesHTML += `<div>${escapeHTML(block)}</div>`;
        }
        contentExamples.innerHTML = examplesHTML;
    }

    /** Fisher–Yates shuffle */
    function shuffle(arr) {
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
        }
        return arr;
    }

    /** Basic HTML escaping to keep raw dictionary lines safe */
    function escapeHTML(s) {
        return String(s)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }
});
