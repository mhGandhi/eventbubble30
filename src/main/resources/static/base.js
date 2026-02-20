const API = "/api";
window.EventBubbleBus = new EventTarget();
const defAvatarBase = "https://ui-avatars.com/api/?name=";

async function bootstrapSession({ onAuthenticated, onAnonymous }) {
    //todo refresh savedMe

    if((accessExpiredByTime()||!accessToken)&& refreshToken)
        await refreshAccessToken();

    try{
        const me = await getMe();
        //todo store me
        if (me!==null) {
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
    let payload = null;

    if (accessToken) {
        headers["Authorization"] = "Bearer " + accessToken;
    }

    if (body instanceof FormData) {
        payload = body;
        // ❗ DO NOT SET Content-Type
    } else if (body !== null) {
        headers["Content-Type"] = "application/json";
        payload = JSON.stringify(body);
    }

    const res = await fetch(API + url, {
        method,
        headers,
        body: payload
    });

    if (retry && res.status === 419 && refreshToken) {
        await refreshAccessToken();
        return api(url, method, body, false);
    }

    const text = await res.text();
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${text}`);
    }

    try { return JSON.parse(text); }
    catch { return text; }
}

////////////////////////////////AUTH
let accessToken = localStorage.getItem("accessToken") || null;
let accessExpiry = localStorage.getItem("accessExpiry") || null;
let refreshToken = localStorage.getItem("refreshToken") || null;

function saveTokens(access, refresh, accessExp = null) {
    accessToken = access ?? null;
    refreshToken = refresh ?? null;
    accessExpiry = accessExp ?? null;

    if (refreshToken) localStorage.setItem("refreshToken", refreshToken);
    else localStorage.removeItem("refreshToken");

    if (accessToken) localStorage.setItem("accessToken", accessToken);
    else localStorage.removeItem("accessToken");

    if (accessExpiry) localStorage.setItem("accessExpiry", accessExpiry);
    else localStorage.removeItem("accessExpiry");
}

function accessExpiredByTime(skewMs = 15_000) {
    if (!accessExpiry || typeof accessExpiry !== "string") return true;

    const expiryMs = Date.parse(accessExpiry); // parses ISO-8601
    if (Number.isNaN(expiryMs)) return true;

    // Consider it expired slightly early to avoid edge cases (clock drift, network latency)
    return Date.now() + skewMs >= expiryMs;
}

function clearTokens() {
    accessToken = null;
    refreshToken = null;
    accessExpiry = null;
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("accessExpiry");
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
            saveTokens(data.accessToken, data.refreshToken, data.accessTokenExpiry);
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

    //col start
    const collapse = httpCode != null && httpCode >= 500;
    if (collapse) {
        const details = document.createElement("details");
        details.className = "notify-details";
        details.open = false; // collapsed by default

        const summary = document.createElement("summary");
        summary.className = "notify-summary";
        summary.textContent = `Server error (HTTP ${httpCode}) — click for details`;

        const pre = document.createElement("pre");
        pre.className = "notify-pre";
        pre.textContent = text;

        details.appendChild(summary);
        details.appendChild(pre);
        div.appendChild(details);
    } else {
        div.textContent = text;
    }
    //col end

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

//////////////////////////////EVENTS

async function deleteEvent(id) {
    if (!confirm("Delete event " + id + "?")) return;
    try {
        await api("/events/" + id, "DELETE");
        EventBubbleBus.dispatchEvent(
            new CustomEvent("event:deleted", { detail: { id } })
        );

    } catch (e) { notify(e); }
}

async function updateEvent(id) {
    if (!confirm("Update event " + id + "?")) return;

    try {
        const body = {
            title: document.getElementById(`title_${id}`).value.trim(),
            description: document.getElementById(`desc_${id}`).value.trim(),
            termin: buildInstantFromLocal(
                document.getElementById(`date_${id}`).value,
                document.getElementById(`time_${id}`).value
            ),
            location: getLocationFromBox(`location_event_${id}`)
        };

        await api(`/events/${id}`, "PATCH", body);
        EventBubbleBus.dispatchEvent(
            new CustomEvent("event:updated", {
                    detail: {
                        id: id,
                    },
            })
        );
    } catch (e) {
        notify(e);
    }
}

function renderEvent(ev, ownerSnippet=null, me=null){
    let canEdit = false;
    try{
        canEdit = me && (me.roles.includes("ADMIN") || me.id === ev.besitzer?.id);
    }catch{}

    return canEdit ? renderEditableEvent(ev, ownerSnippet) : renderReadOnlyEvent(ev, ownerSnippet);
}

function renderEditableEvent(ev, ownerSnippet){//todo nur auf /event??id=XX
    const div = document.createElement("div");
    div.className = "event";

    const local = splitInstantToLocal(ev.termin);
    div.innerHTML = `
                        <label>Title:</label><input id="title_${ev.id}" value="${escapeHtml(ev.title)}"> (ID ${ev.id})<br>
                        ${ownerSnippet ? `By: ${ownerSnippet}<br>`: ""}
                        <label>Date:</label>
                        <input id="date_${ev.id}" type="date" value="${local.date}">
                        <input id="time_${ev.id}" type="time" value="${local.time}"><br>
                        <label>Description:</label><textarea id="desc_${ev.id}">${escapeHtml(ev.description || "")}</textarea><br>
                        <div class="location-box" id="location_event_${ev.id}" data-location="">
                            <div class="location-summary">
                                <i>No location set</i>
                            </div>

                            <button onclick="editLocation('location_event_${ev.id}')">
                                Set / Change Location
                            </button>
                            <button onclick="clearLocation('location_event_${ev.id}')">
                                Clear
                            </button>
                        </div>

                        <hr>
                        <button onClick="deleteEvent(${ev.id})">Delete</button>
                        <button onClick="updateEvent(${ev.id})">Update</button>
                    `;

    return div;
}

function renderReadOnlyEvent(ev, ownerSnippet){
    const div = document.createElement("div");
    div.className = "event";

    div.innerHTML = `
                        <a href="/event?id=${ev.id}"><b>${escapeHtml(ev.title)}</b></a> (ID ${ev.id})<br>
                        ${ownerSnippet ? `By: ${ownerSnippet}<br>`: ""}
                        <small class="timestamp">${ev.termin || "no date"}</small><br>
                        ${escapeHtml(ev.description) || ""}<br>
                        <div class="location-box" id="location_event_${ev.id}" data-location="">
                            <div class="location-summary">
                                <i>No location set</i>
                            </div>
                        </div>
                    `;

    return div;
}


///////////////////////////////////LOCATION
function getLocationFromBox(boxId) {
    const el = document.getElementById(boxId);
    if (!el) return null;
    const raw = el.dataset.location;
    return raw ? JSON.parse(raw) : null;
}

function setLocationInBox(boxId, location) {
    const el = document.getElementById(boxId);
    if (!el) return null;
    el.dataset.location = location ? JSON.stringify(location) : "";
    renderLocationSummary(boxId);
}

function renderLocationSummary(boxId) {
    const el = document.getElementById(boxId);
    const summary = el.querySelector(".location-summary");
    const loc = getLocationFromBox(boxId);

    if (!loc) {
        summary.innerHTML = "<i>No location set</i>";
        return;
    }

    // Compact preview for "near" filter
    if (boxId === "location_event_near") {
        summary.innerHTML = `
            📍 <b>${escapeHtml(loc.displayName)}</b>
            <small style="color:#666">
                (${[loc.city, loc.country].filter(Boolean).join(", ")})
            </small><br>
            <small class="timestamp">
                lat ${loc.latitude?.toFixed(4)}, lon ${loc.longitude?.toFixed(4)}
            </small>
        `;
        return;
    }

    // default (events, editor)
    summary.innerHTML = `
        📍 <b>${escapeHtml(loc.displayName)}</b><br>
        ${[loc.street, loc.postalCode, loc.city].filter(Boolean).join(", ")}
    `;
}

////////////////////////////////////IDK
function buildInstantFromLocal(dateStr, timeStr) {
    if (!dateStr) return null;

    const time = timeStr && timeStr.trim() ? timeStr : "00:00";
    const local = `${dateStr}T${time}`;

    // Interpreted as local time → converted to UTC Instant
    return new Date(local).toISOString();
}

function splitInstantToLocal(iso) {
    if (!iso) return { date: "", time: "" };

    const d = new Date(iso);
    return {
        date: d.toISOString().slice(0, 10),
        time: d.toTimeString().slice(0, 5)
    };
}



/////////////IDK
async function buildUserSnippet(besitzer) {
    if (!besitzer) {
        return "<i>unknown user</i>";
    }

    const username = escapeHtml(besitzer.username);

    try {
        const hasProfile = await api(`/profiles/${besitzer.id}/exists`);

        if (!hasProfile) {
            return `<span title="No profile">@${username}</span>`;
        }

        const profile = await api(`/profiles/${besitzer.id}?level=card`);


        const avatar = profile.avatarURL
            ? `<img src="${profile.avatarURL}"
                    style="width:24px;height:24px;border-radius:50%;vertical-align:middle;margin-right:6px;">`
            : `<img src="https://ui-avatars.com/api/?name=${profile.name}"
                    style="width:24px;height:24px;border-radius:50%;vertical-align:middle;margin-right:6px;">`;

        const name = escapeHtml(profile.name || "Unnamed");


        return `
                <a href="/profile?id=${besitzer.id}">
                <span class="user-snippet">
                    ${avatar}
                    <b>${name}</b>
                    <small style="color:#666">(@${username})</small>
                </span>
                </a>
            `;

    } catch (e) {
        console.warn("Failed to load profile for user", besitzer.id, e);
        return `<span>@${username}</span>`;
    }
}



////////////////////////////////AUDIT
function renderAuditEntry(entry) {
    const safePayload = JSON.stringify(entry.payload);
    let userStr = "?";

    if (entry.user) {
        let userNameSnap = "";
        if (entry.user?.username !== entry.usernameSnapshot && entry.usernameSnapshot !== null) {
            userNameSnap = " (damals " + entry.usernameSnapshot + ")";
        }
        userStr = entry.user?.username + userNameSnap;
    }

    const div = document.createElement("div");
    div.classList.add("audit", entry.success ? "success" : "fail");

    div.innerHTML = `
        <details>
            <summary>
                <b>${entry.action}</b> durch <b>${userStr}</b>
                an <b>${entry.resourceType} #${entry.resourceId || "-"}</b>
                <small class="ipaddress">${entry.ipAddress}</small>
                <small class="timestamp ago" data-timestamp="${entry.timestamp}"></small>
            </summary>
            <small class="timestamp">(${entry.timestamp})</small>
            <small>[${entry.id}]</small><br>
            Endpoint: ${entry.endpoint}<br>
            Payload: ${safePayload}
        </details>
    `;

    // Initial render
    div.querySelector(".ago").textContent = `• ${timeAgo(entry.timestamp)}`;

    return div;
}

async function loadLatestAuditLogs(q = null, domId = null) {
    try {
        const url = "/admin/audit-log?page=0&size=25" + (q ? "&" + q : "");

        const resp = await api(url);
        const div = document.getElementById(domId?domId:"auditLog");

        try{
            document.getElementById("auditSection").style.display = "block";
        }catch{}

        div.innerHTML = "";
        resp.content.forEach(entry => {
            div.appendChild(renderAuditEntry(entry));
        });
    } catch (e) {
        notify(e);
    }
}

async function loadLatestAuditLogsIfAdmin(q = null, domId = null){
    try{
        const me = await getMe();
        if(me && me.roles.includes("ADMIN")){
            await loadLatestAuditLogs(q, domId);
        }
    }catch (e){
        notify(e);
    }
}
