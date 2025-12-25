const API = "/api";
window.EventBubbleBus = new EventTarget();

// --------------------------
// F E T C H   W R A P P E R
// --------------------------
async function api(url, method = "GET", body = null, retry = true) {
    let headers = {};
    if (accessToken) headers["Authorization"] = "Bearer " + accessToken;
    if (body) headers["Content-Type"] = "application/json";

    const res = await fetch(API + url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : null
    });

    // Unauthorized? Try refresh once
    if (res.status === 419 && retry && refreshToken) {
        try {
            await refreshAccessToken();
            return api(url, method, body, false);
        } catch (e) {
            notify(e);
            await logout();
        }
    }

    const text = await res.text();
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${text}`);
    try { return JSON.parse(text); }
    catch { return text; }
}


let accessToken = null;
let refreshToken = localStorage.getItem("refreshToken") || null;

function saveTokens(access, refresh) {
    accessToken = access;
    refreshToken = refresh;
    if (refresh) localStorage.setItem("refreshToken", refresh);
}

function clearTokens() {
    accessToken = null;
    refreshToken = null;
    localStorage.removeItem("refreshToken");
}

function hasRefreshToken(){
    return(refreshToken!==null);
}

// --------------------------
// REFRESH TOKEN
// --------------------------
let refreshPromise = null;
async function refreshAccessToken() {
    //Already refreshing? Wait for it
    if (refreshPromise) {
        return refreshPromise;
    }

    notify("Refreshing access token...", "info");
    if (!refreshToken){
        notify("No refresh Token (log in again)", "error")
        return;
    }

    refreshPromise = (async () => {
        try {
            const data = await api(
                "/auth/refresh",
                "POST",
                { refreshToken },
                false
            );

            saveTokens(data.accessToken, data.refreshToken);
            notify(`Token refreshed as ${data.benutzerDTO?.username || "user"}`);
            EventBubbleBus.dispatchEvent(
                new CustomEvent("auth:refreshed")
            );

            return data;
        } catch(e){
            notify(e);
        } finally {
            refreshPromise = null;
        }
    })();

    return refreshPromise;
}

// --------------------------
// NOTIF
// --------------------------
function getNotifyStack() {
    let stack = document.getElementById("notifyStack");

    if (!stack) {
        stack = document.createElement("div");
        stack.id = "notifyStack";
        stack.className = "notify-stack";

        document.body.appendChild(stack);
    }

    return stack;
}

function notify(e, status = null) {
    const stack = getNotifyStack();

    const div = document.createElement("div");
    div.className = "notify-item";

    let text = "";
    let type = "success";

    //Extract text
    if (e instanceof Error) {
        text = e.message || String(e);

        const match = text.match(/HTTP\s(\d+)/);
        if (match) {
            const code = parseInt(match[1], 10);
            if (code >= 500) type = "error";
            else if (code >= 400) type = "warn";
        } else {
            type = "error";
        }
    } else {
        text = typeof e === "string"
            ? e
            : JSON.stringify(e, null, 2);
    }

    //Explicit override always wins
    if (status) {
        type = status;
    }

    //Console logging (always)
    if (type === "error") {
        console.error("[notify]", text);
    } else if (type === "warn") {
        console.warn("[notify]", text);
    } else {
        console.log("[notify]", text);
    }

    //Render notification
    div.classList.add(type);
    div.textContent = text;

    const close = document.createElement("span");
    close.className = "notify-close";
    close.textContent = "×";
    close.onclick = () => div.remove();

    div.appendChild(close);

    const progress = document.createElement("div");
    progress.className = "notify-progress";
    div.appendChild(progress);
    setTimeout(() => {
        div.classList.add("expired");
    }, 5000);

    stack.prepend(div);
}

async function getMe() {
    try {
        return await api("/user/me");
    } catch {
        return null;
    }
}


////////////////////TABS
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
            document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));

            tab.classList.add("active");
            document.getElementById(tab.dataset.tab).classList.add("active");
        });
    });
});
