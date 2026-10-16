
(() => {
const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];

const fmt = n => new Intl.NumberFormat("es-CO", {
    maximumFractionDigits: 2
}).format(Number(n) || 0);

const money = n => "$" + new Intl.NumberFormat("es-CO", {
    maximumFractionDigits: 0
}).format(Number(n) || 0);

const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
}[c]));

const api = window.CacaoApi;
let data = { lots: [], transforms: [], audit: [] };
let currentRole = '', account = null, selectedLoginRole = 'Administrador', loggedIn = false;
let toastTimer, pendingOperation = null, sessionVersion = 0;
const today = () => new Intl.DateTimeFormat('en-CA', {timeZone:'America/Bogota',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const label = s => ({PENDIENTE:'Pendiente', LIBERADO:'Liberado', BLOQUEADO:'Bloqueado'})[s] || s;
try { localStorage.removeItem('cacao-vivo-registros-demo'); } catch {}
async function busy(form, fn) {
    if (form.dataset.busy === 'true') return;
    form.dataset.busy = 'true';
    const buttons = [...form.querySelectorAll('button')]; buttons.forEach(b => b.disabled = true);
    try { await fn(); } catch (error) { toast(error.message); if (form.id === "transformForm") $("#transformFeedback").textContent = error.message; }
    finally { delete form.dataset.busy; buttons.forEach(b => b.disabled = false); }
}
function acceptAccount(cuenta) {
    account = cuenta; loggedIn = true; sessionVersion++;
    currentRole = cuenta.permisos.includes('ROLE_ADMINISTRADOR') ? 'Administrador' : 'Operador';
    $('#userMenu').textContent = currentRole === 'Administrador' ? 'AD' : 'OP';
    $('#userMenu').title = 'Cerrar sesión';
    $('#userMenu').setAttribute('aria-label', 'Cerrar sesión');
    $('#logoutButton').hidden = false;
    $('#transformResponsible').value = cuenta.correo.slice(0,120);
    $('#transformDate').value = today();
    applyRoleView();
}
function clearSession() {
    sessionVersion++; loggedIn = false; currentRole = ''; account = null; pendingOperation = null;
    data = {lots:[], transforms:[], audit:[]};
    $('#logoutButton').hidden = true;
    $('#userMenu').textContent = 'AD';
    $('#userMenu').title = 'Iniciar sesión';
    $('#userMenu').setAttribute('aria-label', 'Iniciar sesión');
    $$('.modal').forEach(m => m.hidden = true);
    $('#detailContent').replaceChildren(); $('#traceOutput').replaceChildren();
    $('#transformForm').reset(); $('#lotForm').reset(); $('#loginPassword').value = '';
    renderAll(); home();
}
async function refresh() {
    const version = sessionVersion;
    const lots = await api.request('/api/lotes');
    const traces = await Promise.all(lots.map(l => api.request('/api/lotes/' + encodeURIComponent(l.codigo) + '/trazabilidad')));
    if (version !== sessionVersion || !loggedIn) return;
    data.lots = traces.map(t => ({ code:t.lote.codigo, producer:t.lote.productor, origin:t.lote.origen, date:t.lote.fecha,
        received:Number(t.recibidoKg), available:Number(t.disponibleKg), produced:Number(t.salidaProducidaKg),
        status:label(t.lote.estadoLote), notes:t.lote.observacion || '', reason:t.lote.motivoEstado }));
    data.transforms = traces.flatMap(t => t.operaciones.map(o => ({code:o.datos.loteCodigo, stage:o.datos.etapa,
        input:Number(o.datos.cantidadEntradaKg), output:Number(o.datos.cantidadSalidaKg), cost:o.datos.costoTotal,
        responsible:o.datos.responsable, date:o.datos.fechaOperacion, merma:o.mermaPorcentaje, alert:o.alertaMerma,
        cause:o.causaAlerta, unitCost:o.costoUnitarioCopKg, margin:o.margenSobreVentaPorcentaje})));
    renderAll();
}
async function saved(message) {
    try { await refresh(); toast(message); }
    catch(error) { toast(message + ' No se pudo actualizar la vista: ' + error.message); }
}
async function loadAudit() {
    const version = sessionVersion;
    try {
        const rows = await Promise.all(data.lots.map(l => api.request('/api/lotes/' + encodeURIComponent(l.code) + '/auditoria')));
        if (version !== sessionVersion) return;
        data.audit = rows.flat().sort((a,b) => b.id-a.id).map(a => ({date:a.fecha,user:a.usuario,action:a.accion,entity:a.registroId,
            detail:a.campo + ': ' + (a.valorAnterior ?? 'Sin valor anterior') + ' → ' + a.valorNuevo + '. ' + (a.motivo || '')}));
        renderAudit();
    } catch(error) { toast(error.message); }
}
window.addEventListener('cacao:session-expired', () => { clearSession(); modal('#loginModal'); toast('Tu sesión terminó. Inicia sesión de nuevo.'); });

const toast = m => {
    const el = $("#toast");
    el.textContent = m;
    el.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove("show"), 3000);
};

const lot = code => data.lots.find(
    l => l.code.toLowerCase() === String(code || "").trim().toLowerCase()
);



const status = l =>
    `<span class="status ${l.status === "Bloqueado" ? "blocked" : ""}">${esc(l.status)}</span>`;

function go(page) {
    if (!loggedIn) {
        modal("#loginModal");
        return;
    }

    if (currentRole === "Operador" && ["audit"].includes(page)) {
        toast("Esta sección está disponible solo para el administrador.");
        return;
    }

    $("#landingScreen").classList.remove("active");
    $("#appScreen").classList.add("active");

    $$(".app-page").forEach(p =>
        p.classList.toggle("active", p.id === "page-" + page)
    );

    $$(".side-link").forEach(b =>
        b.classList.toggle("active", b.dataset.page === page)
    );

    $("#pageCrumb").textContent = ({
        dashboard: "Resumen",
        lots: "Lotes e inventario",
        transform: "Transformaciones",
        trace: "Trazabilidad",
        public: "Ficha pública / QR",
        alerts: "Alertas",
        audit: "Historial de auditoría"
    })[page] || "Resumen";

    $(".sidebar").classList.remove("open");
    window.scrollTo({ top: 0, behavior: "smooth" });

    if (page === "audit") loadAudit();
    if (page === "trace") {
        trace($("#traceCode").value);
    }

    if (page === "public") {
        renderPublic($("#publicCode").value || "CAC001");
    }
}

function home() {
    $("#appScreen").classList.remove("active");
    $("#landingScreen").classList.add("active");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function getAlerts() {
    let a = [];

    data.transforms.forEach(t => {
        if (t.alert) a.push({title:'Merma superior al 15% · ' + t.code, detail:t.cause});
    });

    data.lots.forEach(l => {
        if (l.status === "Bloqueado") {
            a.push({
                title: `Lote bloqueado · ${l.code}`,
                detail: "El producto no debe ofrecerse como disponible hasta ser liberado por el administrador."
            });
        }

        if (!l.origin || !l.producer || !l.date) {
            a.push({
                title: `Registro incompleto · ${l.code}`,
                detail: "Falta uno o más campos obligatorios."
            });
        }
    });

    if (!a.length) {
        a.push({
            title: "Sin alertas activas",
            detail: "No se detectaron alertas con las reglas actuales."
        });
    }

    return a;
}

function renderAlerts() {
    const alerts = getAlerts();

    const html = alerts.map(a => `
        <div class="alert-item">
            <span class="alert-icon">!</span>
            <div>
                <b>${esc(a.title)}</b>
                <p>${esc(a.detail)}</p>
            </div>
        </div>
    `).join("");

    $("#dashboardAlerts").innerHTML = html;
    $("#fullAlerts").innerHTML = html;

    const n = alerts.filter(
        a => a.title !== "Sin alertas activas"
    ).length;

    $("#statAlerts").textContent = String(n).padStart(2, "0");
    $("#alertNumber").textContent = String(n).padStart(2, "0");
}

function renderInventory() {
    const max = Math.max(1, ...data.lots.map(l => l.received));

    $("#dashboardInventory").innerHTML = data.lots.map(l => `
        <div class="inventory-row">
            <b>${esc(l.code)}</b>
            <div class="inventory-track">
                <span style="width:${Math.max(0, Math.min(100, l.available / max * 100))}%"></span>
            </div>
            <span>${fmt(l.available)} kg</span>
        </div>
    `).join("");
}

function renderLots() {
    const q = ($("#lotSearch")?.value || "").toLowerCase();
    const st = $("#lotStatus")?.value || "";

    const rows = data.lots.filter(l =>
        `${l.code} ${l.producer} ${l.origin}`.toLowerCase().includes(q) &&
        (!st || l.status === st)
    );

    $("#lotsTable").innerHTML = rows.map(l => `
        <tr>
            <td>${esc(l.code)}</td>
            <td>${esc(l.producer)}</td>
            <td>${esc(l.origin)}</td>
            <td>${fmt(l.received)} kg</td>
            <td>${fmt(l.available)} kg</td>
            <td>${status(l)}</td>
            <td>
                <button class="table-action" data-detail="${esc(l.code)}">
                    Abrir ↗
                </button>
            </td>
        </tr>
    `).join("") || `
        <tr>
            <td colspan="7">No hay lotes coincidentes.</td>
        </tr>
    `;
}

function renderTransform() {
    const sel = $("#transformLot");
    const old = sel.value;

    sel.innerHTML = '<option value="">Selecciona un lote</option>' + data.lots.map(l => `
        <option value="${esc(l.code)}">
            ${esc(l.code)} · ${esc(l.origin)} (${fmt(l.available)} kg)
        </option>
    `).join("");

    if (data.lots.some(l => l.code === old)) {
        sel.value = old;
    }

    const scenarioSel = $("#scenarioLot");
    const oldScenarioLot = scenarioSel.value;
    const lotsWithInventory = data.lots.filter(l => l.available > 0);
    scenarioSel.innerHTML = '<option value="">Selecciona un lote con inventario</option>' + lotsWithInventory.map(l => `
        <option value="${esc(l.code)}">${esc(l.code)} · ${esc(l.origin)} (${fmt(l.available)} kg disponibles)</option>
    `).join("");
    if (lotsWithInventory.some(l => l.code === oldScenarioLot)) {
        scenarioSel.value = oldScenarioLot;
    } else if (lotsWithInventory.length) {
        scenarioSel.value = lotsWithInventory[0].code;
    }

    $("#transformTable").innerHTML = data.transforms.map(t => `
        <tr>
            <td>${esc(t.code)}</td>
            <td>${esc(t.stage)}</td>
            <td>${fmt(t.input)} kg</td>
            <td>${fmt(t.output)} kg</td>
            <td>${t.merma == null ? "No comparable" : fmt(t.merma) + "%"}</td>
            <td>${t.cost == null ? "Solo administrador" : money(t.cost) + " COP"}</td>
        </tr>
    `).join("") || `
        <tr>
            <td colspan="6">Todavía no hay transformaciones registradas.</td>
        </tr>
    `;
    simulateScenario();
}

function renderAudit() {
    $("#auditTable").innerHTML = data.audit.map(a => `
        <tr>
            <td>${esc(new Date(a.date).toLocaleString("es-CO"))}</td>
            <td>${esc(a.user)}</td>
            <td>${esc(a.action)}</td>
            <td>${esc(a.entity)}</td>
            <td>${esc(a.detail)}</td>
        </tr>
    `).join("");
}

function renderStats() {
    $("#statLots").textContent =
        String(data.lots.length).padStart(2, "0");

    $("#statAvailable").innerHTML = `
        ${fmt(data.lots.reduce((s, l) => s + l.available, 0))}
        <i>kg</i>
    `;

    $("#welcomeRole").textContent = currentRole.toLowerCase() + ".";
    $("#sidebarUser").textContent = account?.correo || "Sin sesión";
}

function renderAll() {
    renderStats();
    renderInventory();
    renderAlerts();
    renderLots();
    renderTransform();
    renderAudit();
    calc();
}

function calc() {
    if (!$('#transformInput').value || !$('#transformOutput').value || !$('#transformCost').value) {
        $('#calcMerma').textContent = 'Completa las cantidades'; $('#calcUnitCost').textContent = 'Completa el costo';
        $('#calcMargin').textContent = 'Precio opcional'; $('#calcMermaBar').style.width = '0%'; $('#calcWarning').hidden = true; return;
    }
    const i = Number($("#transformInput").value);
    const o = Number($("#transformOutput").value);
    const c = Number($("#transformCost").value);
    const p = Number($("#salePrice").value);

    const m = $('#transformBalance').value !== 'si' && i > 0 && o <= i ? (i - o) / i * 100 : null;
    const u = o > 0 ? c / o : 0;
    const margin = p > 0 && o > 0 ? (p - u) / p * 100 : null;

    $("#calcMerma").textContent = m != null ? fmt(m) + "%" : "No comparable";
    $("#calcMermaBar").style.width =
        Math.max(0, Math.min(100, m)) + "%";

    $("#calcUnitCost").textContent =
        o > 0 ? money(u) + "/kg" : "No calculable";

    $("#calcMargin").textContent =
        margin === null ? "No calculable" : fmt(margin) + "%";

    const w = $("#calcWarning");

    if (i <= 0 || o < 0 || o > i) {
        w.hidden = false;
        w.textContent = i <= 0
            ? "La entrada debe ser mayor que cero."
            : o < 0
                ? "La salida no puede ser negativa."
                : "La salida supera la entrada; registra una justificación por adición de insumos.";
    } else if (m > 15) {
        w.hidden = false;
        w.textContent =
            `Vista previa: merma de ${fmt(m)}%, superior al umbral del 15%.`;
    } else {
        w.hidden = true;
    }
}

function simulateScenario() {
    const selected = data.lots.find(l => l.code === $("#scenarioLot").value && l.available > 0);
    const wasteRaw = $("#scenarioWaste").value;
    const priceRaw = $("#scenarioPrice").value;
    const costRaw = $("#scenarioCost").value;
    const results = $("#scenarioResults");
    const status = $("#scenarioStatus");

    if (!selected) {
        results.hidden = true;
        status.textContent = data.lots.length
            ? "No hay inventario disponible para simular. Registra un lote o elige uno con saldo."
            : "Inicia sesión para cargar lotes con inventario.";
        return;
    }
    if (wasteRaw === "" || priceRaw === "" || costRaw === "") {
        results.hidden = true;
        status.textContent = "Ingresa la merma, el precio y el costo estimados para ver la proyección.";
        return;
    }

    const wastePercent = Number(wasteRaw);
    const pricePerKg = Number(priceRaw);
    const costPerKg = Number(costRaw);
    if (!Number.isFinite(wastePercent) || wastePercent < 0 || wastePercent > 100 ||
        !Number.isFinite(pricePerKg) || pricePerKg < 0 ||
        !Number.isFinite(costPerKg) || costPerKg < 0) {
        results.hidden = true;
        status.textContent = "Usa una merma entre 0 y 100 %, y valores de precio y costo iguales o mayores que cero.";
        return;
    }

    const inputKg = selected.available;
    const outputKg = inputKg * (1 - wastePercent / 100);
    const revenue = outputKg * pricePerKg;
    const totalCost = inputKg * costPerKg;
    const profit = revenue - totalCost;
    const margin = revenue > 0 ? profit / revenue * 100 : null;

    $("#scenarioYield").textContent = fmt(100 - wastePercent) + "%";
    $("#scenarioOutput").textContent = fmt(outputKg) + " kg";
    $("#scenarioRevenue").textContent = money(revenue) + " COP";
    $("#scenarioTotalCost").textContent = money(totalCost) + " COP";
    $("#scenarioProfit").textContent = money(profit) + " COP";
    $("#scenarioMargin").textContent = margin === null ? "No calculable (ingreso cero)" : fmt(margin) + "%";
    status.textContent = "Proyección basada en " + selected.code + " con " + fmt(inputKg) + " kg disponibles. No se ha guardado ningún dato.";
    results.hidden = false;
}

let traceVersion = 0;
async function trace(code) {
    const version = ++traceVersion;
    const session = sessionVersion;
    let t;
    try {
        t = await api.request('/api/lotes/' + encodeURIComponent(code.trim()) + '/trazabilidad');
    } catch (error) {
        if (session === sessionVersion && version === traceVersion) $('#traceOutput').textContent = error.message;
        return;
    }
    if (session !== sessionVersion || version !== traceVersion) return;
    const l = {code:t.lote.codigo, origin:t.lote.origen, producer:t.lote.productor, date:t.lote.fecha,
        received:t.recibidoKg, available:t.disponibleKg, status:label(t.lote.estadoLote)};
    const box = $("#traceOutput");

    if (!l) {
        box.innerHTML = `
            <h2>No encontramos ese lote</h2>
            <p class="muted">Prueba con CAC001, CAC002 o CAC003.</p>
        `;
        return;
    }

    const ts = t.operaciones.map(o => ({stage:o.datos.etapa, input:o.datos.cantidadEntradaKg,
        output:o.datos.cantidadSalidaKg, merma:o.mermaPorcentaje}));

    box.innerHTML = `
        <span class="overline">RECORRIDO DEL LOTE</span>
        <h2>${esc(l.code)} · Origen ${esc(l.origin)}</h2>
        <p class="trace-meta">
            Productor: ${esc(l.producer)} ·
            Recepción: ${esc(l.date)} ·
            Cantidad inicial: ${fmt(l.received)} kg ·
            Estado: ${esc(l.status)}
        </p>

        <div class="timeline">
            <div class="timeline-item">
                <b>01 · Recepción</b>
                <p>${fmt(l.received)} kg recibidos desde ${esc(l.origin)}.</p>
            </div>

            ${ts.length ? ts.map(t => `
                <div class="timeline-item">
                    <b>02 · ${esc(t.stage)}</b>
                    <p>
                        Entrada ${fmt(t.input)} kg ·
                        salida ${fmt(t.output)} kg ·
                        merma ${t.merma == null ? "No comparable" : fmt(t.merma) + "%"}.
                    </p>
                </div>
            `).join("") : `
                <div class="timeline-item">
                    <b>02 · Transformación</b>
                    <p>No hay operaciones registradas para este lote.</p>
                </div>
            `}

            <div class="timeline-item">
                <b>03 · Estado actual</b>
                <p>${esc(l.status)} · ${fmt(l.available)} kg disponibles registrados.</p>
            </div>
        </div>
    `;
}

async function renderPublic(code, target = '#publicCard') {
    const box = $(target); box.hidden = false; box.textContent = 'Consultando lote...';
    const requestId = String(Date.now()) + Math.random(); box.dataset.requestId = requestId;
    try {
        const productCode = code.trim();
        if (!productCode) {
            box.textContent = 'Escribe el código del lote para consultar su ficha pública.';
            return;
        }
        const p = await api.request('/api/publico/productos/' + encodeURIComponent(productCode), {quiet:true});
        if (box.dataset.requestId !== requestId) return;
        const estado = String(p.estado || '').toUpperCase();
        const estadoLabel = label(estado);
        const disponibilidad = p.disponibleParaVenta
            ? 'Disponible para comercialización'
            : estado === 'BLOQUEADO'
                ? 'No disponible para comercialización: lote bloqueado'
                : 'No disponible para comercialización';
        const etapas = Array.isArray(p.trazabilidad) ? p.trazabilidad : [];
        const recepcion = `<li class="public-trace-step">
            <span class="public-trace-marker" aria-hidden="true">01</span>
            <div><h4>Recepción del lote</h4>
                <p>${fmt(p.cantidadRecibida)} ${esc(p.unidad)} recibidos el ${esc(p.fechaRecepcion)}.</p>
            </div>
        </li>`;
        const transformaciones = etapas.map((etapa, index) => `<li class="public-trace-step">
            <span class="public-trace-marker" aria-hidden="true">${String(index + 2).padStart(2, '0')}</span>
            <div><h4>${esc(etapa.etapa)}</h4>
                <p>${esc(etapa.fecha)} · Entrada ${fmt(etapa.cantidadEntradaKg)} kg → salida ${fmt(etapa.cantidadSalidaKg)} kg.</p>
            </div>
        </li>`).join('');
        const avisoEtapas = etapas.length
            ? ''
            : '<p class="public-trace-empty">El lote fue recibido. Aún no hay etapas de transformación registradas.</p>';
        box.innerHTML = `<article class="public-product-card">
            <header class="public-product-heading">
                <div><span class="overline">FICHA PÚBLICA · LOTE ${esc(p.codigo)}</span>
                    <h3>Producto de cacao</h3></div>
                <span class="public-status public-status-${esc(estado.toLowerCase())}">${esc(estadoLabel)}</span>
            </header>
            <dl class="public-product-details">
                <div><dt>Productor</dt><dd>${esc(p.productor)}</dd></div>
                <div><dt>Origen</dt><dd>${esc(p.origen)}</dd></div>
                <div><dt>Estado comercial</dt><dd>${esc(disponibilidad)}</dd></div>
            </dl>
            <section class="public-trace" aria-label="Trazabilidad del lote">
                <h4>Recorrido del producto</h4>
                ${avisoEtapas}
                <ol>${recepcion}${transformaciones}</ol>
            </section>
            <p class="fine-print">Esta ficha muestra datos públicos del lote y sus etapas registradas; no incluye costos ni información administrativa.</p>
        </article>`;
    } catch(error) {
        if (box.dataset.requestId !== requestId) return;
        box.textContent = error.status === 404
            ? 'No se encontró un lote con ese código. Verifica el identificador e inténtalo de nuevo.'
            : 'No se pudo consultar la ficha pública: ' + error.message;
    }
}

function modal(id, show = true) {
    $(id).hidden = !show;
}

async function exportCsv() {
    if (currentRole !== 'Administrador') return toast('Solo el administrador puede exportar costos.');
    try { await refresh(); } catch(error) { return toast(error.message); }
    const rows = [
        [
            "codigo",
            "productor",
            "origen",
            "fecha",
            "recibido_kg",
            "disponible_kg",
            "estado",
            "costo_transformaciones_cop"
        ],
        ...data.lots.filter(l => {
            const q = $('#lotSearch').value.toLowerCase(); const st = $('#lotStatus').value;
            return (l.code + ' ' + l.producer + ' ' + l.origin).toLowerCase().includes(q) && (!st || st === l.status);
        }).map(l => [
            l.code,
            l.producer,
            l.origin,
            l.date,
            l.received,
            l.available,
            l.status,
            data.transforms
                .filter(t => t.code === l.code)
                .reduce((s, t) => s + t.cost, 0)
        ])
    ];

    const csv = rows.map(r =>
        r.map(v => `"${String(v ?? "").replace(/^[=+@\-\t\r\n]/, c => "'" + c).replace(/"/g, '""')}"`).join(",")
    ).join("\r\n");

    const u = URL.createObjectURL(
        new Blob(["\ufeff" + csv], {
            type: "text/csv;charset=utf-8"
        })
    );

    const a = document.createElement("a");
    a.href = u;
    a.download = "cacao-vivo-lotes.csv";
    a.click();

    URL.revokeObjectURL(u);
    toast("Archivo CSV exportado.");
}

// NAVEGACIÓN Y MODALES

$$("[data-enter]").forEach(b =>
    b.addEventListener("click", () => loggedIn ? go("dashboard") : modal("#loginModal"))
);

$("#backHome").addEventListener("click", e => {
    e.preventDefault();
    home();
});

$$(".side-link").forEach(b =>
    b.addEventListener("click", () => go(b.dataset.page))
);

$$("[data-page-go]").forEach(b =>
    b.addEventListener("click", () => go(b.dataset.pageGo))
);

$("#registerTop").addEventListener("click", () =>
    modal("#registerModal")
);

$$("[data-close-modal]").forEach(b =>
    b.addEventListener("click", () =>
        modal("#" + b.dataset.closeModal, false)
    )
);

$$(".modal").forEach(m =>
    m.addEventListener("click", e => {
        if (e.target === m) {
            modal("#" + m.id, false);
        }
    })
);

// INICIO DE SESIÓN API

$("#loginForm").dataset.ready = "true";
$$("[data-login-role]").forEach(button => button.addEventListener("click", () => {
    selectedLoginRole = button.dataset.loginRole;
    $$("[data-login-role]").forEach(choice => {
        const selected = choice === button;
        choice.classList.toggle("selected", selected);
        choice.setAttribute("aria-pressed", String(selected));
    });
}));

$('#loginForm').addEventListener('submit', e => {
    e.preventDefault(); busy(e.target, async () => {
        const password = $('#loginPassword').value; $('#loginPassword').value = '';
        const cuenta = await api.login($('#loginEmail').value.trim(), password);
        acceptAccount(cuenta); modal('#loginModal', false);
        await refresh(); go('dashboard');
        toast(currentRole === selectedLoginRole
            ? 'Sesión iniciada como ' + currentRole + '. El rol proviene del servidor.'
            : 'La cuenta inició sesión como ' + currentRole + '; el servidor asigna los permisos, no el perfil seleccionado.');
    });
});

function applyRoleView() {
    const operator = currentRole !== "Administrador";
    $("#exportCsv").hidden = operator;

    $$('[data-page="audit"]').forEach(b => b.hidden = operator);
    $$('[data-page="alerts"]').forEach(b => b.hidden = operator);
    $$('[data-page="public"]').forEach(b => b.hidden = false);
    $$('[data-page="lots"]').forEach(b => b.hidden = false);
    $$('[data-page="transform"]').forEach(b => b.hidden = false);

    const adminOnly = $$("#page-dashboard .stat-grid article");

    adminOnly.forEach((el, i) => {
        el.hidden = operator && i === 3;
    });

    const adminAudit = $("#page-audit");

    if (adminAudit) {
        adminAudit.hidden = operator;
    }
}

// Registro de compradores real; no concede acceso a la gestion interna.
$('#registerForm').addEventListener('submit', e => {
    e.preventDefault(); busy(e.target, async () => {
        if ($('#registerPassword').value !== $('#registerPasswordConfirm').value) throw new Error('Las contraseñas no coinciden.');
        const body = {nombre:$('#registerName').value.trim(), correo:$('#registerEmail').value.trim(), telefono:$('#registerPhone').value.trim(),
            tipoComprador:$('#registerBuyerType').value, ciudad:$('#registerCity').value.trim(), password:$('#registerPassword').value};
        $('#registerPassword').value = $('#registerPasswordConfirm').value = '';
        await api.request('/api/publico/compradores', {method:'POST', body});
        e.target.reset(); modal('#registerModal',false); toast('Registro de comprador recibido. La consulta pública no requiere sesión.');
    });
});
$('#landingLookup').addEventListener('submit', e => { e.preventDefault(); renderPublic($('#landingCode').value, '#landingResult'); });

// REGISTRO DE LOTES API

$("#openLotForm").addEventListener("click", () => {
    if (!loggedIn || !["Administrador", "Operador"].includes(currentRole)) {
        toast("Inicia sesión con un perfil técnico para registrar lotes.");
        return;
    }

    $("#newDate").value = today();
    modal("#lotModal");
});

$("#openLotForm2").addEventListener("click", () =>
    $("#openLotForm").click()
);

$('#lotForm').addEventListener('submit', e => {
    e.preventDefault(); busy(e.target, async () => {
        const l = await api.request('/api/lotes', {method:'POST',body:{codigo:$('#newCode').value.trim(), productor:$('#newProducer').value.trim(),
            origen:$('#newOrigin').value.trim(), fecha:$('#newDate').value, cantidad:Number($('#newQty').value), unidad:'kg', observacion:$('#newNotes').value.trim() || null}});
        modal('#lotModal',false); e.target.reset(); await saved('Lote ' + l.codigo + ' guardado en MySQL.');
    });
});

$("#lotSearch").addEventListener("input", renderLots);
$("#lotStatus").addEventListener("change", renderLots);

// DETALLE Y ESTADOS API

$("#lotsTable").addEventListener("click", e => {
    const b = e.target.closest("[data-detail]");

    if (!b) return;

    const l = lot(b.dataset.detail);

    $("#detailContent").innerHTML = `
        <p class="eyebrow"><span></span> DETALLE DEL LOTE</p>
        <h2>${esc(l.code)} · <em>${esc(l.origin)}</em></h2>
        <p class="muted">
            Productor: ${esc(l.producer)} · Fecha: ${esc(l.date)}
        </p>
        <p>
            Recibido: ${fmt(l.received)} kg ·
            Disponible: ${fmt(l.available)} kg
        </p>
        <p>Estado: ${status(l)}</p>
        ${currentRole === 'Administrador' ? `<form id="stateForm" class="form-stack" hidden>
            <label>Nuevo estado<select id="stateChoice"><option value="LIBERADO">Liberado</option><option value="BLOQUEADO">Bloqueado</option></select></label>
            <label>Motivo<textarea id="stateReason" required></textarea></label>
            <button class="btn btn-green" type="submit">Confirmar cambio</button>
            <button class="btn btn-outline" type="button" id="stateCancel">Cancelar</button></form>` : ''}

        <div class="hero-buttons">
            <button class="btn btn-outline" id="detailTrace">
                Ver trazabilidad
            </button>

            ${currentRole === "Administrador" ? `
                <button class="btn btn-green" id="detailStatus">
                    ${l.status === "Bloqueado" ? "Liberar" : l.status === "Pendiente" ? "Liberar / bloquear" : "Bloquear"} lote
                </button>
            ` : ""}
        </div>
    `;

    modal("#detailModal");

    $("#detailTrace").addEventListener("click", () => {
        modal("#detailModal", false);
        go("trace");
        $("#traceCode").value = l.code;
        trace(l.code);
    });

    $('#detailStatus')?.addEventListener('click', () => { $('#stateForm').hidden = false; });
    $('#stateCancel')?.addEventListener('click', () => { $('#stateForm').hidden = true; });
    $('#stateForm')?.addEventListener('submit', e => {
        e.preventDefault();
        const estado = $('#stateChoice').value, motivo = $('#stateReason').value.trim();
        if (!motivo) return toast('Escribe el motivo del cambio.');
        busy(e.target, async () => {
            await api.request('/api/lotes/' + encodeURIComponent(l.code) + '/estado', {method:'PATCH',body:{estado,motivo}});
            modal('#detailModal',false); await saved('Estado actualizado y auditado.');
        });
    });
});

// TRANSFORMACIONES API

$('#transformDate').value = today(); $('#transformDate').max = today();
$('#transformBalance').addEventListener('change', () => {
    const changed = $('#transformBalance').value === 'si';
    $('#balanceReasonLabel').hidden = !changed; $('#transformBalanceReason').required = changed; calc();
});
$('#transformLot').addEventListener('change', () => {
    $('#transformFeedback').textContent = 'Lote seleccionado: ' + $('#transformLot').value + '. Revisa las cantidades y costos antes de guardar.';
});
$("#transformForm").addEventListener("input", calc);
$("#salePrice").addEventListener("input", calc);

$('#transformForm').addEventListener('submit', e => {
    e.preventDefault(); busy(e.target, async () => {
        const body = {loteCodigo:$('#transformLot').value, etapa:$('#transformStage').value.trim(), responsable:$('#transformResponsible').value.trim(),
            cantidadEntradaKg:Number($('#transformInput').value), cantidadSalidaKg:Number($('#transformOutput').value), costoTotal:Number($('#transformCost').value),
            precioVentaKg:$('#salePrice').value ? Number($('#salePrice').value) : null, observacion:$('#transformIncident').value.trim() || null};
        body.fechaOperacion = $('#transformDate').value;
        body.costosIncluidos = $('#transformCosts').value.trim();
        body.justificacionBalance = $('#transformBalance').value === 'si' ? $('#transformBalanceReason').value.trim() : null;
        if (!body.loteCodigo || !body.etapa || !body.responsable || !body.fechaOperacion || !body.costosIncluidos)
            throw new Error('Completa lote, etapa, responsable, fecha y costos incluidos.');
        if (body.fechaOperacion > today()) throw new Error('La fecha no puede ser futura.');
        if (($('#transformBalance').value === 'si' || body.cantidadSalidaKg > body.cantidadEntradaKg) && !body.justificacionBalance)
            throw new Error('Selecciona y explica el cambio de base de masa si hay insumos adicionales o ajustes de humedad.');
        const signature = JSON.stringify(body);
        if (!pendingOperation || pendingOperation.signature !== signature)
            pendingOperation = {signature,body:{...body,codigoOperacion:crypto.randomUUID()}};
        let r;
        try {
            r = await api.request('/api/operaciones',{method:'POST',body:pendingOperation.body});
        } catch (error) {
            // Un rechazo de validación permite corregir los campos del formulario.
            // Ante un fallo de red conservamos el código para un reintento idempotente.
            if (error.status === 400) pendingOperation = null;
            throw error;
        }
        $('#transformFeedback').textContent = 'Operación guardada para ' + body.loteCodigo + '. Consulta su trazabilidad. Reenviar estos mismos datos no crea otra operación.';
        await saved('Operación guardada correctamente.');
        $('#calcMerma').textContent = r.mermaPorcentaje == null ? 'No comparable' : fmt(r.mermaPorcentaje) + '%';
        $('#calcUnitCost').textContent = r.costoUnitarioCopKg == null ? 'No calculable' : money(r.costoUnitarioCopKg) + '/kg';
        $('#calcMargin').textContent = r.margenSobreVentaPorcentaje == null ? 'No calculable' : fmt(r.margenSobreVentaPorcentaje) + '%';
        $('#calcWarning').hidden = !r.alertaMerma; $('#calcWarning').textContent = r.causaAlerta || '';
    });
});

["change", "input"].forEach(eventName => {
    ["scenarioLot", "scenarioWaste", "scenarioPrice", "scenarioCost"].forEach(id => {
        $("#" + id).addEventListener(eventName, simulateScenario);
    });
});

// OTRAS FUNCIONALIDADES

$("#traceForm").addEventListener("submit", e => {
    e.preventDefault();
    trace($("#traceCode").value);
});

$("#publicForm").addEventListener("submit", e => {
    e.preventDefault();
    renderPublic($("#publicCode").value);
});

$('#generateQr').addEventListener('click', async () => {
    try {
        const p = await api.request('/api/publico/productos/' + encodeURIComponent($('#publicCode').value.trim()),{quiet:true});
        const url = new URL('/',location.origin); url.searchParams.set('lote',p.codigo);
        $('#qrMessage').replaceChildren(); const a = document.createElement('a'); a.href = url.href; a.textContent = 'Abrir enlace público del lote';
        $('#qrMessage').append('Patrón ilustrativo. Enlace público: ',a);
    } catch(error) { toast(error.message); }
});

$("#exportCsv").addEventListener("click", exportCsv);

async function logout() {
    try { await api.logout(); clearSession(); toast('Sesión cerrada en el servidor.'); }
    catch(error) { toast(error.message); }
}
$('#userMenu').addEventListener('click', logout);
$('#logoutButton').addEventListener('click', logout);
$('#switchRole').title = 'Cambiar usuario o rol';
$('#switchRole').setAttribute('aria-label', 'Cambiar usuario o rol');
$('#switchRole').addEventListener('click', async () => { await logout(); if (!loggedIn) modal('#loginModal'); });

$("#mobileMenu").addEventListener("click", () =>
    $(".sidebar").classList.toggle("open")
);

$("#appScreen").addEventListener("click", e => {
    if (e.target.closest(".side-link") && window.innerWidth <= 760) {
        $(".sidebar").classList.remove("open");
    }
});

const video = $(".landing-video");

if (video) {
    video.muted = true;
    video.volume = 0;
    video.play().catch(() => {});
}

renderAll();
const publicCode = new URLSearchParams(location.search).get('lote');
if (publicCode) { $('#landingCode').value = publicCode; renderPublic(publicCode, '#landingResult'); }
api.request('/api/cuenta', {quiet:true}).then(async cuenta => { acceptAccount(cuenta); await refresh(); })
    .catch(error => { if (error.status !== 401) toast(error.message); });

})();
