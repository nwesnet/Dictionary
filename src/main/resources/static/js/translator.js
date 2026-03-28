document.addEventListener("DOMContentLoaded", function () {
    const translateBtn = document.getElementById("translateBtn");
    const sourceTextArea = document.getElementById("source-text");
    const targetTextArea = document.getElementById("target-text");
    const contentInfo = document.getElementById("content-info");
    const contentExamples = document.getElementById("content-examples");
    // translate button "T"
    translateBtn.addEventListener("click", function (event) {
        event.preventDefault();

        const text = sourceTextArea.value.trim();

        if (text === "") {
            alert("Please enter a word");
            return;
        }

        const words = text.split(/\s+/).filter(Boolean);

        sendToBackend(words);
    })
    // the method that ask the server for translate
    function sendToBackend(data) {
        fetch('/api/translate', {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ words: data})
        })
        .then(response => response.json())
        .then(result => {
            const firstWord = data[0];
            const info = result[firstWord];
            if (!info) return;

            if (targetTextArea) targetTextArea.value = info.translation || "";

            let infoHTML = "";
            if (info.partOfSpeech) {
                infoHTML += `<div><strong>Part of speech</strong> ${info.partOfSpeech}</div>`;
            }
            if (info.transcription) {
                infoHTML += `<div><strong>Transcription:</strong> ${info.transcription}</div>`;
            }
            if (info.irregularVerbs != null) {
                infoHTML += `<div><strong>Irregular verb:</strong> ${info.irregularVerbsForms}</div>`
            }
            if (contentInfo) contentInfo.innerHTML = infoHTML;

            let examplesHTML = "";
            if (info.usageInText && info.usageInText.length > 0) {
                for (const uit of info.usageInText) {
                    examplesHTML += `<div>${uit}</div>`;
                    }
            }

            if (contentExamples) contentExamples.innerHTML = examplesHTML;
        })
        .catch (error => {
            console.log('Error', error);
        })
    }
    // Copy source text button "C"
    copyBtn.addEventListener("click", function () {
        const textToCopy = sourceTextArea.value.trim();

        if (textToCopy === "") {
            alert("Nothing to copy");
            return;
        }

        navigator.clipboard.writeText(textToCopy)
            .then(() => {
                alert("Copied to clipboard");
            })
            .catch(err => {
                console.error("Failed to copy text: ", err);
            });
    });
    // Clear all fields button "R"
    clearBtn.addEventListener("click", function() {
        sourceTextArea.value = "";
        targetTextArea.value = "";
        contentExamples.innerHTML = "";
        contentInfo.innerHTML = "";
    });

    // Copy output button "C" in target part
    copyOutputBtn.addEventListener("click", function() {
        const targetTextToCopy = targetTextArea.value.trim();

        if (targetTextToCopy === "") {
            alert("Nothing to copy");
            return;
        }

        navigator.clipboard.writeText(targetTextToCopy)
            .then(() => {
                alert("Copied to clipboard");
            })
            .catch(err => {
                console.error("Failed to copy text: ", err);
            });
    });
    // Add favorite word button "+"
    addFavoriteBtn.addEventListener("click", function() {
        const usernameEl = document.getElementById("current-username");
        const ownerUsername = usernameEl ? usernameEl.textContent.trim() : "";

        const newFavoriteWord = sourceTextArea.value.trim();

        if (newFavoriteWord === "") {
            alert("Nothing to add");
            return;
        }

        addFavoriteWordEndPoint(newFavoriteWord, ownerUsername);
    });
    // POST to server to store favorite word
    function addFavoriteWordEndPoint(word, ownerUsername) {
        fetch('/api/addFavorite', {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ word, ownerUsername })
        })
        .then(res => {
            if (!res.ok) throw new Error("Failed to save favorite");
            return res.text();
        })
        .catch(err => {
            console.error(err);
            alert("couldn't save the favorite word. Please try again");
        });
    }
});