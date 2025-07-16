document.addEventListener("DOMContentLoaded", function () {
    const translateBtn = document.getElementById("translateBtn");
    const sourceTextArea = document.getElementById("source-text");
    const targetTextArea = document.getElementById("target-text");
    const contentInfo = document.getElementById("content-info");
    const contentExamples = document.getElementById("content-examples");

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
});