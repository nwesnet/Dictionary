function getOrCreateMonitorId() {
    let match = document.cookie.match('(^|;)\\s*monitor_id\\s*=\\s*([^;]+)');
    let monitorId = match ? match.pop() : null;
    if (!monitorId) {
        monitorId = crypto.randomUUID();
        document.cookie = "monitor_id=" + monitorId + "; path=/; max-age=" + (60*60*24*365);
    }
    return monitorId;
}

function getCurrentUsernameFromDOM() {
    let span = document.getElementById('current-username');
    if (span && span.textContent) {
        return span.textContent.trim();
    }
    return null;
}


function collectSessionInfo() {
    return new Promise((resolve) => {
        let base = {
            identifier: getOrCreateMonitorId(),
            username: getCurrentUsernameFromDOM(),
            resolution: `${window.screen.width}x${window.screen.height}`,
            maxResolution: `${window.screen.availWidth}x${window.screen.availHeight}`,
            os: navigator.userAgent,
            language: navigator.language,
            event: document.hasFocus() ? "window_active" : "window_inactive"
        };
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition((position) => {
                base.geo = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                };
                resolve(base);
            }, () => {
                resolve(base); // No permission
            });
        } else {
            resolve(base);
        }
    });
}

function sendSessionInfo() {
    collectSessionInfo().then((info) => {
        fetch("http://localhost:8081/monitor/session-info", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(info)
        });
    });
}

window.addEventListener('load', sendSessionInfo);
window.addEventListener('focus', sendSessionInfo);
window.addEventListener('blur', sendSessionInfo);