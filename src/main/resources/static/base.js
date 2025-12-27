const API = "/api";
window.EventBubbleBus = new EventTarget();

async function bootstrapSession({ onAuthenticated, onAnonymous }) {
    //todo refresh savedMe

    if(refreshToken)
        await refreshAccessToken();

    try{
        if (await getMe()!==null) {
            onAuthenticated?.();
        } else {
            onAnonymous?.();
        }
    }catch(e){
        notify(e);
    }

    EventBubbleBus.dispatchEvent(new CustomEvent("bootstrapped"));
}

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
    if (retry && res.status === 419 && refreshToken) {
        try {
            await refreshAccessToken();
            return api(url, method, body, false);
        } catch (e) {
            notify(e);
            EventBubbleBus.dispatchEvent(
                new CustomEvent("auth:refresh_error")
            );
        }
    }

    const text = await res.text();
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${text}`);
    }
    try { return JSON.parse(text); }
    catch { return text; }
}

////////////////////////////////AUTH
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

//todo track last refresh/expiry time
let refreshPromise = null;
async function refreshAccessToken() {
    //Already refreshing? Wait for it
    if (refreshPromise) {
        return refreshPromise;
    }
    notify("Refreshing access token...", "info", true);
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
            notify(`Token refreshed as ${data.benutzerDTO?.username || "user"}`, "success", true);
            EventBubbleBus.dispatchEvent(
                new CustomEvent("auth:refreshed")
            );
            return data;
        } catch(e){
            notify(e);
            EventBubbleBus.dispatchEvent(new CustomEvent("auth:refresh_error"));
            logout();
        } finally {
            refreshPromise = null;
        }
    })();
    return refreshPromise;
}


///////////////////////////////////HELPER
function logout(){
    clearTokens();
    notify("Logged out!");
    EventBubbleBus.dispatchEvent(
        new CustomEvent("auth:logout")
    );
}

let getMePromise = null;
async function getMe() {
    if(getMePromise)return getMePromise;
    getMePromise = (async () => {
        try {
            return await api("/user/me");
        } catch {
            return null;
        } finally {
            getMePromise = null;
        }
    })();
    return getMePromise;
}


////////////////////////////////////NOTIF
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

function notify(e, status = null, expire = false) {
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
    if(expire){
        setTimeout(() => {
            div.classList.add("deleted");
        }, 5000);

        setTimeout(() => {
            div.remove();
        }, 6000);
    }else{
        setTimeout(() => {
            div.classList.add("expired");
        }, 5000);
    }

    stack.prepend(div);
}


//////////////////////////////TABS
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


/////////////////////////////UTIL
function escapeHtml(str) {
    if (!str) return "";
    return str.replace(/[&<>"']/g, c => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    }[c]));
}


///////////////////////////////TIME AGO
function formatTimeAgo(now, isoTimestamp){
    const then = new Date(isoTimestamp);
    const seconds = Math.floor((now - then) / 1000);

    if (seconds < 10) return "just now";
    if (seconds < 60) return `${seconds}s ago`;

    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;

    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;

    const weeks = Math.floor(days / 7);
    if (weeks < 5) return `${weeks}w ago`;

    const months = Math.floor(days / 30);
    if (months < 12) return `${months}mo ago`;

    const years = Math.floor(days / 365);
    return `${years}y ago`;
}

function timeAgo(isoTimestamp) {
    ensureTimeAgoRefresh()

    const now = new Date();
    return formatTimeAgo(now, isoTimestamp);
}

let timeAgoIntervalId = null;
function ensureTimeAgoRefresh(){
    if(timeAgoIntervalId!==null)return;

    timeAgoIntervalId = setInterval(() => {
        const elements = document.querySelectorAll(".ago[data-timestamp]");

        if (elements.length === 0) {
            stopTimeAgoRefresh();
            return;
        }

        const now = new Date();
        elements.forEach(el => {
            const ts = el.dataset.timestamp;
            if (ts) {
                el.textContent = `• ${formatTimeAgo(now, ts)}`;
            }else {
                el.textContent = "-";
            }
        });
    }, 60_000);
}

function stopTimeAgoRefresh() {
    if (timeAgoIntervalId === null) return;

    clearInterval(timeAgoIntervalId);
    timeAgoIntervalId = null;
}


