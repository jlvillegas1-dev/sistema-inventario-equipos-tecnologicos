(function () {
    const body = document.body;
    const page = body.dataset.page || "login";
    const role = body.dataset.role || null;
    const currentPath = window.location.pathname;
    const isUserRole = role === "usuario" || role === "tecnico" || role === "custodio";
    const pathDepth = currentPath.split("/").filter(Boolean).length;
    const basePrefix = page === "login" || pathDepth <= 2 ? "." : (isUserRole ? "../.." : "..");
    const API_BASE = `${basePrefix}/resources`;
    const STORAGE_SESSION = "inventario.session.demo";
    const PAGE_SIZE = 100;
    const VALID_STATES = [
        "OPERATIVO",
        "NO OPERATIVO",
        "REPORTADO PARA DAR DE BAJA"
    ];
    const TYPE_CONFIG = {
        pc: { label: "PC" },
        laptop: { label: "Laptop" },
        periferico: { label: "Periférico" },
        impresora: { label: "Impresora" },
        escaner: { label: "Escáner" },
        telefono: { label: "Teléfono" },
        proyector: { label: "Proyector" },
        infraestructura: { label: "Infraestructura" },
        licencia: { label: "Licencia" },
        modem: { label: "Módem" },
        bien_control_admin: { label: "Control admin." }
    };
    const CATEGORY_CONFIG = {
        laptops: {
            label: "Laptop",
            hint: "Equipo portátil para trabajo administrativo o técnico.",
            fields: ["procesador", "ram", "discoDuro", "sistemaOperativo", "numeroSerie"]
        },
        desktop: {
            label: "Desktop",
            hint: "Estación fija con foco en rendimiento de oficina o laboratorio.",
            fields: ["procesador", "ram", "discoDuro", "sistemaOperativo", "numeroSerie"]
        },
        telefonos: {
            label: "Teléfonos",
            hint: "Terminal telefónica o móvil asignada a un custodio o dependencia.",
            fields: ["numeroSerie", "linea", "imei"]
        },
        escaners: {
            label: "Escáner",
            hint: "Equipo de digitalización con datos de resolución y conexión.",
            fields: ["numeroSerie", "resolucion", "conexion"]
        },
        impresoras: {
            label: "Impresora",
            hint: "Equipo de impresión con tecnología y tipo de conexión.",
            fields: ["numeroSerie", "tecnologia", "conexion"]
        },
        perifericos: {
            label: "Periféricos",
            hint: "Accesorios como teclados, mouse, bases o monitores auxiliares.",
            fields: ["numeroSerie", "conexion", "compatibilidad"]
        },
        proyectores: {
            label: "Proyector",
            hint: "Equipo audiovisual con datos de resolución y brillo.",
            fields: ["numeroSerie", "resolucion", "lumenes"]
        },
        infraestructura: {
            label: "Infraestructura",
            hint: "Equipamiento de red, comunicaciones o soporte tecnológico institucional.",
            fields: ["numeroSerie", "ip", "caracteristicas"]
        },
        licencias: {
            label: "Licencia",
            hint: "Activos lógicos o licencias de software asociadas al inventario.",
            fields: ["numeroSerie", "caracteristicas"]
        },
        modem: {
            label: "Módem",
            hint: "Equipos de conectividad móvil o fija con plan y servicio asociado.",
            fields: ["numeroSerie", "ip", "caracteristicas"]
        }
    };
    const SEARCH_QUICK_PRESETS = [
        { label: "Operativos", criteria: [{ field: "estado", value: "OPERATIVO" }] },
        { label: "No operativos", criteria: [{ field: "estado", value: "NO OPERATIVO" }] },
        { label: "PC", criteria: [{ field: "tipo", value: "pc" }] },
        { label: "Dell", criteria: [{ field: "marca", value: "Dell" }] }
    ];
    const NAV_ICONS = {
        dashboard: iconMarkup("dashboard"),
        inventario: iconMarkup("inventory"),
        // busqueda: iconMarkup("search"),  // COMENTADO: redundante con filtros de inventario
        "nuevo-equipo": iconMarkup("plusBox"),
        actas: iconMarkup("clipboard")
    };
    const INVENTORY_AUTOCOMPLETE = {
        filterCodigoSbai: "codigoSbai",
        filterCodigoMegan: "codigoMegan",
        filterDescripcion: "descripcion",
        filterMarca: "marca",
        filterModelo: "modelo",
        filterSerie: "numeroSerie",
        filterCustodio: "custodio",
        filterEdificio: "ubicacionEdificio",
        filterPiso: "ubicacionPiso",
        filterDireccion: "ubicacionDireccion",
        actaFilterCodigoSbai: "codigoSbai",
        actaFilterCodigoMegan: "codigoMegan",
        actaFilterDescripcion: "descripcion",
        actaFilterMarca: "marca",
        actaFilterModelo: "modelo",
        actaFilterSerie: "numeroSerie",
        actaFilterCustodio: "custodio",
        actaFilterEdificio: "ubicacionEdificio",
        actaFilterPiso: "ubicacionPiso",
        actaFilterDireccion: "ubicacionDireccion"
    };
    const state = {
        session: null,
        inventory: [],
        filteredInventory: [],
        inventorySearchPerformed: false,
        inventoryLastCriteria: [],
        inventoryPage: 1,
        inventorySort: { key: "codigoSbai", direction: "asc" },
        searchCriteria: [],
        searchResults: [],
        equipmentFieldCatalog: {},
        exportSelection: null,
        loginCredentials: null,
        sidebarOpen: false,
        actaSwCurrentPage: 1,
        actaSwLastFiltered: [],
        actaSwLastCriteria: {},
        actaEquipoLastFiltered: [],
        actaEquipoLimit: 50
    };

    document.addEventListener("DOMContentLoaded", () => {
        if (page === "login") {
            initLogin();
            return;
        }
        initShell();
    });

    function apiFetch(path, options) {
        return fetch(`${API_BASE}${path}`, Object.assign({ credentials: "same-origin" }, options || {}));
    }

    function setDemoSession(session) {
        localStorage.setItem(STORAGE_SESSION, JSON.stringify(session));
    }

    function getDemoSession() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_SESSION) || "null");
        } catch (error) {
            return null;
        }
    }

    function clearDemoSession() {
        localStorage.removeItem(STORAGE_SESSION);
    }

    async function handleLogin(event) {
        event.preventDefault();
        const submit = document.getElementById("loginSubmit");
        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value.trim();
        submit.disabled = true;
        hideLoginError();
        try {
            const payload = await requestLogin(username, password, "");
            handleAuthenticatedLogin(payload.data, { username, password });
            return;
        } catch (error) {
            const demo = buildDemoSession(username, password);
            if (demo) {
                handleAuthenticatedLogin(demo, { username, password, demo: true });
                return;
            }
            showLoginError(error.message || "No fue posible iniciar sesión");
        } finally {
            submit.disabled = false;
        }
    }

    function requestLogin(username, password, rolElegido) {
        // Envia credenciales al backend; rolElegido solo viaja cuando el usuario ya selecciono perfil.
        return apiFetch("/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, rolElegido })
        }).then(async (response) => {
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "Credenciales invalidas");
            }
            return payload;
        });
    }

    function handleAuthenticatedLogin(data, credentials) {
        // Si hay varios roles disponibles, detenemos el ingreso para pedir el perfil explicito.
        const roles = normalizeAvailableRoles(data?.rolesDisponibles);
        state.loginCredentials = credentials;
        if (roles.length > 1 && !credentials?.rolElegido) {
            showProfileSelection(data, roles);
            return;
        }
        const selectedData = Object.assign({}, data, { rol: credentials?.rolElegido || data?.rol || roles[0] });
        const session = mapSession(selectedData);
        setDemoSession(session);
        redirectForRole(session);
    }

    function normalizeAvailableRoles(roles) {
        const values = Array.isArray(roles) ? roles : [];
        return Array.from(new Set(values.map((rol) => String(rol || "").trim().toUpperCase()).filter(Boolean)));
    }

    function roleLabelFromAccessRole(accessRole) {
        if (accessRole === "ADMINISTRADOR") {
            return "Administrador";
        }
        if (accessRole === "CUSTODIO") {
            return "Custodio";
        }
        return "Tecnico";
    }

    function roleHelpText(accessRole) {
        if (accessRole === "ADMINISTRADOR") {
            return "Gestion completa del inventario y usuarios.";
        }
        if (accessRole === "CUSTODIO") {
            return "Consulta de equipos asignados a su custodia.";
        }
        return "Gestion operativa y soporte tecnico.";
    }

    function showProfileSelection(data, roles) {
        const form = document.getElementById("loginForm");
        const panel = document.getElementById("profileSelection");
        const options = document.getElementById("profileOptions");
        if (!form || !panel || !options) {
            return;
        }
        form.classList.add("is-hidden");
        panel.classList.remove("is-hidden");
        options.innerHTML = roles.map((rol) => `
            <button type="button" class="profile-option" data-profile-role="${rol}">
                <strong>${escapeHtml(roleLabelFromAccessRole(rol))}</strong>
                <span>${escapeHtml(roleHelpText(rol))}</span>
            </button>
        `).join("");
        options.querySelectorAll("[data-profile-role]").forEach((button) => {
            button.addEventListener("click", () => selectLoginRole(data, button.dataset.profileRole));
        });
        document.getElementById("profileBackButton")?.addEventListener("click", resetLoginProfileSelection, { once: true });
    }

    async function selectLoginRole(data, rolElegido) {
        const credentials = state.loginCredentials || {};
        try {
            if (credentials.demo) {
                handleAuthenticatedLogin(Object.assign({}, data, { rol: rolElegido }), Object.assign({}, credentials, { rolElegido }));
                return;
            }
            const payload = await requestLogin(credentials.username, credentials.password, rolElegido);
            handleAuthenticatedLogin(payload.data, Object.assign({}, credentials, { rolElegido }));
        } catch (error) {
            resetLoginProfileSelection();
            showLoginError(error.message || "No fue posible seleccionar el perfil.");
        }
    }

    function resetLoginProfileSelection() {
        document.getElementById("profileSelection")?.classList.add("is-hidden");
        document.getElementById("loginForm")?.classList.remove("is-hidden");
        state.loginCredentials = null;
    }

    function showLoginError(message) {
        const error = document.getElementById("loginError");
        if (!error) {
            return;
        }
        error.textContent = message;
        error.classList.remove("is-hidden");
    }

    function hideLoginError() {
        document.getElementById("loginError")?.classList.add("is-hidden");
    }

    function startClock() {
        const dateEl = document.getElementById("liveDate");
        const clockEl = document.getElementById("liveClock");
        if (!dateEl || !clockEl) {
            return;
        }
        const refresh = () => {
            const now = new Date();
            dateEl.textContent = now.toLocaleDateString("es-EC", {
                weekday: "short",
                day: "2-digit",
                month: "short",
                year: "numeric"
            });
            clockEl.textContent = now.toLocaleTimeString("es-EC");
        };
        refresh();
        setInterval(refresh, 1000);
    }

    function pageTitle(pageName) {
        const titles = {
            dashboard: "Bienvenido",
            inventario: "Inventario",
            busqueda: "Búsqueda avanzada",
            "nuevo-equipo": "Nuevo equipo",
            actas: "Actas",
            "acta-equipos": "Acta de Equipos",
            "acta-software": "Acta de Software",
            "acta-rc": "Acta de Redes y Comunicaciones",
            "acta-licencias": "Acta de Licencias"
        };
        return titles[pageName] || "Sistema de Inventario";
    }

    function renderPage() {
        if (page === "dashboard") {
            return renderDashboard();
        }
        if (page === "inventario") {
            return renderInventoryPage();
        }
        if (page === "busqueda") {
            return renderSearchPage();
        }
        if (page === "nuevo-equipo") {
            return renderNewEquipmentPage();
        }
        if (page === "actas") {
            return renderActasHubPage();
        }
        if (page === "acta-equipos" || page === "acta-software" || page === "acta-licencias") {
            return renderActaFormPage(page);
        }
        if (page === "acta-rc") {
            return renderActaRedesComunicacionesPage();
        }
        return "";
    }

    function renderSearchPage() {
        const criteriaOptions = ["tipo", "codigoMegan", "codigoSbai", "descripcion", "numeroSerie", "marca", "modelo", "custodio", "ubicacion", "estado", "procesador", "caracteristicas"];
        return `
            <section class="panel">
                <div class="tag-builder">
                    <div class="field-group">
                        <label for="criteriaField">Campo</label>
                        <select id="criteriaField">${criteriaOptions.map((field) => `<option value="${field}">${labelForColumn(field)}</option>`).join("")}</select>
                    </div>
                    <div class="field-group">
                        <label for="criteriaValue">Valor</label>
                        <input id="criteriaValue" type="text" placeholder="Ingresa un valor">
                    </div>
                    <div class="field-group">
                        <label>&nbsp;</label>
                        <button class="btn btn-primary" id="addCriteria">Agregar criterio</button>
                    </div>
                </div>
                <div class="search-tags" id="searchQuickPresets">
                    ${SEARCH_QUICK_PRESETS.map((item, index) => `<button class="search-tag search-tag--button" type="button" data-search-preset="${index}">${item.label}</button>`).join("")}
                </div>
                <div class="search-tags" id="searchTags"></div>
                <div class="toolbar" style="margin-top:16px;">
                    <button class="btn btn-primary" id="runSearch">Buscar</button>
                    <button class="btn btn-secondary" id="clearSearch">Limpiar</button>
                </div>
                <div id="searchMeta" class="search-results-meta"></div>
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Código Megan</th>
                                <th>Código SBYE</th>
                                <th>Descripción</th>
                                <th>Marca</th>
                                <th>Modelo</th>
                                <th>Custodio</th>
                                <th>ubicación</th>
                                <th>Estado</th>
                                <th>Detalle</th>
                            </tr>
                        </thead>
                        <tbody id="searchBody"></tbody>
                    </table>
                </div>
                <div class="mobile-cards" id="searchMobile"></div>
                <div class="empty-state hidden" id="searchEmpty">Carga los datos y aplica uno o varios criterios para filtrar.</div>
            </section>
        `;
    }

    function bindShellEvents() {
        document.getElementById("sidebarToggle")?.addEventListener("click", () => {
            state.sidebarOpen = !state.sidebarOpen;
            document.getElementById("sidebar")?.classList.toggle("is-open", state.sidebarOpen);
        });
        document.getElementById("logoutButton")?.addEventListener("click", logout);
        document.getElementById("modalRoot")?.addEventListener("click", (event) => {
            if (event.target.classList.contains("modal-backdrop")) {
                closeModal();
            }
        });

        if (page === "inventario") {
            bindInventoryEvents();
        }
        if (page === "busqueda") {
            // COMENTADO: redundante con filtros del módulo de inventario
            // return renderSearchPage();
            return renderInventoryPage();
        }
        if (page === "busqueda") {
            // COMENTADO: redundante con filtros del módulo de inventario
            // bindSearchEvents();
        }
        if (page === "nuevo-equipo") {
            bindNewEquipmentEvents();
        }
        if (isActasPage(page)) {
            bindActasEvents();
        }
    }
// Funciones relacionadas con el módulo de inventario
    async function loadInventory() {
        const items = await fetchInventoryType("todos");
        state.inventory = normalizeItems(items);
        state.filteredInventory = state.inventory.slice();
    }

    async function fetchInventoryType(type) {
        try {
            const response = await apiFetch(`/inventario/${type}`);
            if (response.status === 401) {
                clearDemoSession();
                window.location.href = `${basePrefix}/index.html`.replace("/pages/index.html", "/index.html");
                return [];
            }
            if (!response.ok) {
                let message = "No se pudo cargar el inventario desde el backend.";
                try {
                    const payload = await response.json();
                    if (payload?.message) {
                        message = payload.message;
                    }
                } catch (ignored) {
                    // Si la respuesta no es JSON, mantenemos el mensaje genérico.
                }
                throw new Error(message);
            }
            const payload = await response.json();
            return Array.isArray(payload.data) ? payload.data : [];
        } catch (error) {
            showToast("Error", error.message || "No se pudo cargar el inventario desde el backend.", "danger");
            return [];
        }
    }

    function normalizeItems(items) {
        return items.map((item) => ({
            id: item.id,
            tipo: item.tipo || "",
            subtipo: item.subtipo || "",
            codigoSbai: item.codigoSbai || "",
            codigoMegan: item.codigoMegan || "",
            descripcion: item.descripcion || "",
            numeroSerie: item.numeroSerie || "",
            marca: item.marca || "",
            modelo: item.modelo || "",
            custodio: item.custodio || "",
            ubicacion: item.ubicacion || "",
            ubicacionEdificio: item.ubicacionEdificio || "",
            ubicacionPiso: item.ubicacionPiso || "",
            ubicacionDireccion: item.ubicacionDireccion || "",
            estado: normalizeState(item.estado || ""),
            sistemaOperativo: item.sistemaOperativo || "",
            procesador: item.procesador || "",
            caracteristicas: item.caracteristicas || "",
            observacion: item.observacion || "",
            ip: item.ip || "",
            fechaIngreso: item.fechaIngreso || "",
            ultimaActualizacion: item.ultimaActualizacion || "",
            ultimoMantenimiento: item.ultimoMantenimiento || "",
            idComputador: item.idComputador ?? null,
            raw: item
        }));
    }

    function updateDashboardStats() {
        if (page !== "dashboard") {
            return;
        }
        const total = state.inventory.length;
        const active = state.inventory.filter((item) => item.estado === "OPERATIVO").length;
        const locations = new Set(state.inventory.map((item) => item.ubicacion).filter(Boolean)).size;
        setText("statTotal", total);
        setText("statActive", active);
        setText("statLocations", locations);
    }

    function inventoryFilterIds() {
        return [
            "filterTipo",
            "filterCodigoSbai",
            "filterCodigoMegan",
            "filterDescripcion",
            "filterMarca",
            "filterModelo",
            "filterSerie",
            "filterCustodio",
            "filterEdificio",
            "filterPiso",
            "filterDireccion",
            "filterEstado"
        ];
    }

    function readInventoryFilters() {
        return {
            tipo: document.getElementById("filterTipo")?.value.trim().toLowerCase() || "",
            codigoSbai: document.getElementById("filterCodigoSbai")?.value.trim().toLowerCase() || "",
            codigoMegan: document.getElementById("filterCodigoMegan")?.value.trim().toLowerCase() || "",
            descripcion: document.getElementById("filterDescripcion")?.value.trim().toLowerCase() || "",
            marca: document.getElementById("filterMarca")?.value.trim().toLowerCase() || "",
            modelo: document.getElementById("filterModelo")?.value.trim().toLowerCase() || "",
            numeroSerie: document.getElementById("filterSerie")?.value.trim().toLowerCase() || "",
            custodio: document.getElementById("filterCustodio")?.value.trim().toLowerCase() || "",
            edificio: document.getElementById("filterEdificio")?.value.trim().toLowerCase() || "",
            piso: document.getElementById("filterPiso")?.value.trim().toLowerCase() || "",
            direccion: document.getElementById("filterDireccion")?.value.trim().toLowerCase() || "",
            estado: document.getElementById("filterEstado")?.value.trim().toLowerCase() || ""
        };
    }
    // Filtrado de inventario sin hacer nuevas consultas a la base.
    function applyInventoryFilters() {
        const filters = readInventoryFilters();
        state.inventoryPage = 1;
        state.inventorySearchPerformed = true;
        state.inventoryLastCriteria = buildInventoryCriteria(filters);
        state.filteredInventory = state.inventory.filter((item) => {
            return matchesFilter(item.tipo, filters.tipo)
                && matchesFilter(item.codigoSbai, filters.codigoSbai)
                && matchesFilter(item.codigoMegan, filters.codigoMegan)
                && matchesFilter(item.descripcion, filters.descripcion)
                && matchesFilter(item.marca, filters.marca)
                && matchesFilter(item.modelo, filters.modelo)
                && matchesFilter(item.numeroSerie, filters.numeroSerie)
                && matchesFilter(item.custodio, filters.custodio)
                && matchesFilter(item.ubicacionEdificio, filters.edificio)
                && matchesFilter(item.ubicacionPiso, filters.piso)
                && matchesFilter(item.ubicacionDireccion, filters.direccion)
                && matchesFilter(item.estado, filters.estado);
        });
        sortInventory(state.inventorySort.key, false);
        refreshInventoryAutocompletes();
    }

    function clearInventoryFilters() {
        inventoryFilterIds().forEach((id) => {
            const element = document.getElementById(id);
            if (element) {
                element.value = "";
                if (element.tagName === "SELECT") {
                    element.selectedIndex = 0;
                }
            }
        });
        state.inventoryPage = 1;
        state.inventorySearchPerformed = false;
        renderInventory();
    }

    function changePage(delta) {
        const totalPages = Math.max(1, Math.ceil(state.filteredInventory.length / PAGE_SIZE));
        const nextPage = Math.min(totalPages, Math.max(1, state.inventoryPage + delta));
        if (nextPage === state.inventoryPage) {
            return;
        }
        state.inventoryPage = nextPage;
        renderInventory();
    }

    function matchesFilter(value, filter) {
        return !filter || String(value || "").toLowerCase().includes(filter);
    }

    function buildInventoryCriteria(filters) {
        const filterLabels = {
            tipo: "Tipo",
            codigoSbai: "Código SBYE",
            codigoMegan: "Código Megan",
            descripcion: "Descripción",
            marca: "Marca",
            modelo: "Modelo",
            numeroSerie: "Serie",
            custodio: "Custodio",
            edificio: "Edificio",
            piso: "Piso",
            direccion: "Dirección",
            estado: "Estado"
        };
        return Object.entries(filters)
            .filter(([, value]) => value)
            .map(([key, value]) => `${filterLabels[key] || key}: ${value}`);
    }

    function sortInventory(key, toggleDirection = true) {
        if (toggleDirection && state.inventorySort.key === key) {
            state.inventorySort.direction = state.inventorySort.direction === "asc" ? "desc" : "asc";
        } else {
            state.inventorySort = { key, direction: "asc" };
        }
        const factor = state.inventorySort.direction === "asc" ? 1 : -1;
        state.filteredInventory.sort((a, b) => String(a[key] || "").localeCompare(String(b[key] || ""), "es", { numeric: true }) * factor);
        renderInventory();
    }

    function updateInventoryStats() {
        setText("inventoryStatTotal", state.filteredInventory.length);
    }

    function bindSearchEvents() {
        document.getElementById("addCriteria")?.addEventListener("click", () => {
            const field = document.getElementById("criteriaField").value;
            const value = document.getElementById("criteriaValue").value.trim();
            if (!value) {
                return;
            }
            state.searchCriteria.push({ field, value });
            document.getElementById("criteriaValue").value = "";
            renderSearchTags();
        });
        document.getElementById("runSearch")?.addEventListener("click", runSearch);
        document.getElementById("clearSearch")?.addEventListener("click", () => {
            state.searchCriteria = [];
            renderSearchTags();
            renderSearchResults([]);
        });
        document.querySelectorAll("[data-search-preset]").forEach((button) =>
            button.addEventListener("click", () => applySearchPreset(Number(button.dataset.searchPreset)))
        );
    }

    function renderSearchTags() {
        const container = document.getElementById("searchTags");
        if (!container) {
            return;
        }
        container.innerHTML = state.searchCriteria.map((criteria, index) => `
            <span class="search-tag">${labelForColumn(criteria.field)}: ${escapeHtml(criteria.value)} <button class="table-sort" data-remove="${index}">—</button></span>
        `).join("");
        container.querySelectorAll("[data-remove]").forEach((button) => button.addEventListener("click", () => {
            state.searchCriteria.splice(Number(button.dataset.remove), 1);
            renderSearchTags();
        }));
    }

    function runSearch() {
        state.searchResults = state.inventory.filter((item) => state.searchCriteria.every((criteria) =>
            String(item[criteria.field] || "").toLowerCase().includes(String(criteria.value || "").toLowerCase())
        ));
        renderSearchResults(state.searchResults);
    }

    function renderSearchResults(results) {
        const tbody = document.getElementById("searchBody");
        const mobile = document.getElementById("searchMobile");
        const empty = document.getElementById("searchEmpty");
        if (!tbody || !mobile || !empty) {
            return;
        }
        const hasCriteria = state.searchCriteria.length > 0;
        empty.classList.toggle("hidden", hasCriteria);
        setText("searchMeta", hasCriteria ? `${results.length} resultados encontrados.` : "Añade criterios para iniciar la búsqueda.");
        if (!hasCriteria) {
            tbody.innerHTML = "";
            mobile.innerHTML = "";
            return;
        }
        tbody.innerHTML = results.length ? results.map((item) => `
            <tr>
                <td>${escapeHtml(item.codigoMegan || "-")}</td>
                <td>${escapeHtml(item.codigoSbai || "-")}</td>
                <td>${escapeHtml(item.descripcion || "-")}</td>
                <td>${escapeHtml(item.marca || "-")}</td>
                <td>${escapeHtml(item.modelo || "-")}</td>
                <td>${escapeHtml(item.custodio || "-")}</td>
                <td>${escapeHtml(item.ubicacion || "-")}</td>
                <td>${stateBadge(item.estado)}</td>
                <td>${escapeHtml(item.procesador || item.caracteristicas || "-")}</td>
            </tr>
        `).join("") : '<tr><td colspan="9">No se encontraron coincidencias.</td></tr>';
        mobile.innerHTML = results.map((item) => `
            <article class="mobile-card">
                <strong>${escapeHtml(item.codigoSbai || "-")}</strong>
                <span>Descripción: ${escapeHtml(item.descripcion || "-")}</span>
                <span>Marca / Modelo: ${escapeHtml(item.marca || "-")} ${escapeHtml(item.modelo || "")}</span>
                <span>Custodio: ${escapeHtml(item.custodio || "-")}</span>
                <span>Estado: ${stripHtml(stateBadge(item.estado))}</span>
            </article>
        `).join("");
    }

    function applySearchPreset(index) {
        const preset = SEARCH_QUICK_PRESETS[index];
        if (!preset) {
            return;
        }
        state.searchCriteria = preset.criteria.map((item) => Object.assign({}, item));
        renderSearchTags();
        runSearch();
    }

    function renderEquipmentSummary(categoryConfig, fields) {
        const summary = document.getElementById("equipmentFieldSummary");
        if (!summary) {
            return;
        }
        summary.innerHTML = `
            <article class="report-history__item">
                <strong>${categoryConfig.label}</strong>
                <div class="muted">${categoryConfig.hint || ""}</div>
            </article>
            <article class="report-history__item">
                <strong>Detalle de campos</strong>
                <div class="muted">${fields.map((field) => labelForColumn(field)).join(" · ")}</div>
            </article>
        `;
    }

    async function logout() {
        try {
            await apiFetch("/login/logout", { method: "POST" });
        } catch (error) {
            // noop
        }
        clearDemoSession();
        window.location.href = `${basePrefix}/index.html`.replace("/pages/index.html", "/index.html");
    }

    function openModal(title, bodyHtml, actions, extraClass) {
        const modalRoot = document.getElementById("modalRoot");
        modalRoot.innerHTML = `
            <div class="modal-backdrop"></div>
            <div class="modal${extraClass ? " " + extraClass : ""}">
                <div class="modal__header">
                    <h3>${title}</h3>
                    <button class="modal-close" id="modalClose" aria-label="Cerrar">&#x2715;</button>
                </div>
                <div class="modal__body">${bodyHtml}</div>
                <div class="modal__footer form-actions" id="modalFooter"></div>
            </div>
        `;
        modalRoot.classList.add("is-open");
        document.getElementById("modalClose").addEventListener("click", closeModal);
        const footer = document.getElementById("modalFooter");
        (actions || [{ label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }]).forEach((action) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = action.className;
            button.textContent = action.label;
            if (action.id) button.id = action.id;
            if (action.disabled) button.disabled = true;
            button.addEventListener("click", action.onClick);
            footer.appendChild(button);
        });
    }

    function closeModal() {
        const modalRoot = document.getElementById("modalRoot");
        modalRoot.classList.remove("is-open");
        modalRoot.innerHTML = "";
    }

    // Errores y advertencias: cuadro que solo se cierra al presionar "Entendido"
    function showAlertDialog(title, message, type) {
        const overlay = document.createElement("div");
        overlay.className = `alert-dialog-root alert-dialog-${type}`;
        overlay.innerHTML = `
            <div class="alert-dialog" role="alertdialog" aria-modal="true">
                <strong class="alert-dialog__title">${title}</strong>
                <p class="alert-dialog__message">${message}</p>
                <div class="alert-dialog__actions">
                    <button type="button" class="btn btn-primary">Entendido</button>
                </div>
            </div>`;
        const button = overlay.querySelector("button");
        button.addEventListener("click", () => overlay.remove());
        document.body.appendChild(overlay);
        button.focus();
    }

    function showToast(title, message, type) {
        if (type === "danger" || type === "error" || type === "warning") {
            showAlertDialog(title, message, type === "warning" ? "warning" : "danger");
            return;
        }
        const stack = document.getElementById("toastStack");
        if (!stack) {
            return;
        }
        const toast = document.createElement("div");
        toast.className = `toast toast-${type || "info"}`;
        toast.innerHTML = `<strong>${title}</strong><span>${message}</span>`;
        stack.appendChild(toast);
        setTimeout(() => toast.remove(), 3200);
    }

    function buildTypeOptions() {
        return Object.keys(TYPE_CONFIG)
            .map((key) => `<option value="${key}">${TYPE_CONFIG[key].label}</option>`)
            .join("");
    }

    const ACTA_EQUIPO_TYPES = ["pc", "laptop", "impresora", "proyector", "escaner", "telefono"];
    // Tipos de equipo a los que se asignan licencias (licencia.id_computador)
    const ACTA_LICENCIAS_COMPUTADOR_TYPES = ["pc", "laptop"];

    function buildActaTypeOptions() {
        return ACTA_EQUIPO_TYPES
            .filter((key) => TYPE_CONFIG[key])
            .map((key) => `<option value="${key}">${TYPE_CONFIG[key].label}</option>`)
            .join("");
    }

    function typeLabel(type) {
        return TYPE_CONFIG[type]?.label || type || "-";
    }

    function displayInventoryType(item) {
        const base = typeLabel(item?.tipo);
        const subtipo = String(item?.subtipo || "").trim();
        return item?.tipo === "infraestructura" && subtipo ? `${base} - ${subtipo}` : base;
    }

    function normalizeState(value) {
        const normalized = String(value || "").trim().toUpperCase();
        if (VALID_STATES.includes(normalized)) {
            return normalized;
        }
        if (normalized === "ACTIVO") {
            return "OPERATIVO";
        }
        if (normalized === "INACTIVO" || normalized === "DAÑADO" || normalized === "DANADO") {
            return "NO OPERATIVO";
        }
        if (normalized === "REPORTADO PARA BAJA" || normalized === "BAJA") {
            return "REPORTADO PARA DAR DE BAJA";
        }
        return "NO OPERATIVO";
    }

    function stateBadge(status) {
        const label = normalizeState(status);
        if (label === "OPERATIVO") {
            return `<span class="badge badge-success">${label}</span>`;
        }
        if (label === "REPORTADO PARA DAR DE BAJA") {
            return `<span class="badge badge-danger">${label}</span>`;
        }
        return `<span class="badge badge-warning">${label}</span>`;
    }

    function editField(name, label, value, disabled, type) {
        if (type === "textarea") {
            return `<label class="field-group"><span>${label}</span><textarea name="${name}" ${disabled ? "disabled" : ""}>${escapeHtml(value || "")}</textarea></label>`;
        }
        if (type === "select") {
            return `<label class="field-group"><span>${label}</span><select name="${name}" ${disabled ? "disabled" : ""}>
                ${VALID_STATES.map((item) => `<option value="${item}">${item}</option>`).join("")}
            </select></label>`;
        }
        return `<label class="field-group"><span>${label}</span><input name="${name}" value="${escapeHtml(value || "")}" ${disabled ? "readonly" : ""}></label>`;
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function stripHtml(html) {
        const div = document.createElement("div");
        div.innerHTML = html;
        return div.textContent || div.innerText || "";
    }

    function formatDate(value) {
        return new Date(value).toLocaleString("es-EC");
    }

    function setText(id, value) {
        const el = document.getElementById(id);
        if (el) {
            el.textContent = value;
        }
    }

    function timestampForFile() {
        const now = new Date();
        return [
            now.getFullYear(),
            String(now.getMonth() + 1).padStart(2, "0"),
            String(now.getDate()).padStart(2, "0"),
            String(now.getHours()).padStart(2, "0"),
            String(now.getMinutes()).padStart(2, "0")
        ].join("");
    }

    function downloadBlob(blob, filename) {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    }

    function iconMarkup(type) {
        const icons = {
            dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="8" rx="2"></rect><rect x="14" y="3" width="7" height="5" rx="2"></rect><rect x="14" y="12" width="7" height="9" rx="2"></rect><rect x="3" y="15" width="7" height="6" rx="2"></rect></svg>',
            inventory: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7.5 12 4l8 3.5-8 3.5L4 7.5Z"></path><path d="M4 12.5 12 16l8-3.5"></path><path d="M4 17.5 12 21l8-3.5"></path><path d="M4 7.5v10"></path><path d="M20 7.5v10"></path></svg>',
            search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="6"></circle><path d="m20 20-4.2-4.2"></path></svg>',
            plusBox: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"></rect><path d="M12 8v8"></path><path d="M8 12h8"></path></svg>',
            clipboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="4" width="14" height="17" rx="2"></rect><path d="M9 4.5h6"></path><path d="M9 9h6"></path><path d="M9 13h6"></path><path d="M9 17h4"></path></svg>',
            spark: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7L12 3Z"></path><path d="M19 17l.8 2.2L22 20l-2.2.8L19 23l-.8-2.2L16 20l2.2-.8L19 17Z"></path></svg>',
            edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5Z"></path></svg>',
            repeat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 1l4 4-4 4"></path><path d="M3 11V9a4 4 0 0 1 4-4h14"></path><path d="M7 23l-4-4 4-4"></path><path d="M21 13v2a4 4 0 0 1-4 4H3"></path></svg>',
            history: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"></path><path d="M3.05 13a9 9 0 1 0 .5-4.5"></path><path d="M12 7v5l4 2"></path></svg>',
            delete: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"></path><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path></svg>',
            circle: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="5"></circle></svg>'
        };
        return icons[type] || icons.circle;
    }

    function collectAutocompleteValues(itemKey, term) {
        const values = state.inventory
            .map((item) => String(item[itemKey] || "").trim())
            .filter(Boolean)
            .filter((value) => !term || value.toLowerCase().includes(term));
        return Array.from(new Set(values))
            .sort((a, b) => a.localeCompare(b, "es", { numeric: true, sensitivity: "base" }))
            .slice(0, 12);
    }

    function updateAutocompleteForInput(fieldId) {
        const input = document.getElementById(fieldId);
        const list = document.getElementById(`${fieldId}List`);
        const itemKey = INVENTORY_AUTOCOMPLETE[fieldId];
        if (!input || !list || !itemKey) {
            return;
        }
        const term = input.value.trim().toLowerCase();
        const options = collectAutocompleteValues(itemKey, term);
        list.innerHTML = options.map((value) => `<option value="${escapeHtml(value)}"></option>`).join("");
    }

    function refreshInventoryAutocompletes() {
        Object.keys(INVENTORY_AUTOCOMPLETE).forEach((fieldId) => updateAutocompleteForInput(fieldId));
    }

    function isActasPage(pageName) {
        return ["actas", "acta-equipos", "acta-software", "acta-rc", "acta-licencias"].includes(pageName);
    }

    function actasBasePath() {
        if (role === "admin") {
            return `${basePrefix}/pages`;
        }
        if (role === "custodio") {
            return `${basePrefix}/pages/custodio`;
        }
        return `${basePrefix}/pages/usuario`;
    }

    function renderActasHubPage() {
        const basePath = actasBasePath();
        const esCustodio = state.session?.role === "custodio";
        if (window.SIActasView?.renderHub) {
            return window.SIActasView.renderHub({ basePath, iconMarkup, esCustodio });
        }
        let formsHtml = "";
        if (!esCustodio) {
            const cards = [
                ["Equipos", "Acta de mantenimiento preventivo de equipos.", `${basePath}/acta-equipos.html`],
                ["Software", "Acta de programas y aplicaciones instaladas.", `${basePath}/acta-software.html`],
                ["Redes y Comunicaciones", "Acta de mantenimiento preventivo de redes y comunicaciones.", `${basePath}/acta-rc.html`],
                ["Licencias", "Acta de licencias de software.", `${basePath}/acta-licencias.html`]
            ];
            formsHtml = `
                <div class="section-heading">
                    <div>
                        <h2>Selecciona un formato</h2>
                        <p>Accesos directos a las actas disponibles dentro del módulo.</p>
                    </div>
                </div>
                <section class="quick-grid">
                    ${cards.map(([title, text, href]) => `
                        <a class="mini-card mini-card--action" href="${href}">
                            <span class="mini-card__icon" aria-hidden="true">${iconMarkup("clipboard")}</span>
                            <strong>${title}</strong>
                            <span>${text}</span>
                            <small>Abrir formulario</small>
                        </a>
                    `).join("")}
                </section>
            `;
        }

        return `
            <section class="hero hero--actas">
                <div class="hero__grid">
                    <div class="hero-copy">
                        <div class="eyebrow">Módulo ACTAS</div>
                        <h2>${esCustodio ? "Mis Actas de Mantenimiento" : "Formatos para documentar mantenimientos y revisiones."}</h2>
                        <p>${esCustodio ? "Consulte e imprima sus actas generadas por el personal técnico." : "Selecciona el tipo de acta que se requiere completar o consulte el historial."}</p>
                    </div>
                </div>
            </section>
            ${formsHtml}
            <div class="section-heading" style="margin-top: 32px;">
                <div>
                    <h2>${esCustodio ? "Listado de Actas" : "Historial de Actas Generadas"}</h2>
                    <p>${esCustodio ? "Documentos de entrega-recepción y soporte técnico asociados a sus equipos." : "Historial de todos los documentos emitidos y registrados en el sistema."}</p>
                </div>
            </div>
            <section class="panel" style="margin-top: 16px;">
                <div id="actasListaContainer">Cargando actas...</div>
            </section>
        `;
    }

    function actaConfig(pageName) {
        const basePath = actasBasePath();
        const commonFooter = `
            <div class="form-actions acta-form__actions">
                <button class="btn btn-primary" type="submit">Guardar acta</button>
                <button class="btn btn-secondary" type="reset">Limpiar</button>
                <a class="btn btn-secondary" href="${basePath}/actas.html">Volver</a>
            </div>
        `;

        const configs = {
            "acta-equipos": {
                eyebrow: "Mantenimiento preventivo",
                title: "Formulario de equipos",
                description: "Registro para documentar la revision preventiva realizada sobre equipos tecnológicos.",
                hint: "Completa los datos principales del equipo, la revision aplicada y las observaciones finales.",
                fields: `
                    <div class="field-group"><label for="actaFechaEquipo">Fecha</label><input id="actaFechaEquipo" name="fecha" type="date" required></div>
                    <div class="field-group"><label for="actaTecnicoEquipo">Técnico responsable</label><input id="actaTecnicoEquipo" name="tecnico" type="text" required></div>
                    <div class="field-group"><label for="actaCustodioEquipo">Custodio</label><input id="actaCustodioEquipo" name="custodio" type="text"></div>
                    <div class="field-group"><label for="actaAreaEquipo">Área</label><input id="actaAreaEquipo" name="area" type="text"></div>
                    <div class="field-group"><label for="actaCodigoEquipo">Código del equipo</label><input id="actaCodigoEquipo" name="codigoEquipo" type="text"></div>
                    <div class="field-group"><label for="actaSerieEquipo">Número de serie</label><input id="actaSerieEquipo" name="numeroSerie" type="text"></div>
                    <div class="field-group"><label for="actaTipoEquipo">Tipo de equipo</label><input id="actaTipoEquipo" name="tipoEquipo" type="text"></div>
                    <div class="field-group"><label for="actaMarcaEquipo">Marca y modelo</label><input id="actaMarcaEquipo" name="marcaModelo" type="text"></div>
                    <div class="field-group"><label for="actaLimpiezaEquipo">Limpieza</label><select id="actaLimpiezaEquipo" name="limpieza"><option value="">Seleccione</option><option>Realizada</option><option>No aplica</option></select></div>
                    <div class="field-group"><label for="actaDiagnosticoEquipo">Diagnostico</label><select id="actaDiagnosticoEquipo" name="diagnostico"><option value="">Seleccione</option><option>Operativo</option><option>Con novedad</option><option>Requiere seguimiento</option></select></div>
                    <div class="field-group field-group--wide"><label for="actaTrabajoEquipo">Trabajo realizado</label><textarea id="actaTrabajoEquipo" name="trabajoRealizado" rows="4"></textarea></div>
                    <div class="field-group field-group--wide"><label for="actaObservacionEquipo">Observaciones</label><textarea id="actaObservacionEquipo" name="observaciones" rows="4"></textarea></div>
                    ${commonFooter}
                `
            },
            "acta-software": null,
            "acta-rc": null,
            "acta-licencias": null
        };

        return configs[pageName];
    }

function renderActaLicenciasPage() {
    return `
        <section class="panel panel--narrow acta-form-panel">
            <div class="inventory-header inventory-header--form">
                <div>
                    <div class="eyebrow">Licencias de software</div>
                    <h2>Formulario de Licencias</h2>
                    <p>Acta de reunión y entrega-recepción de licencias de software.</p>
                </div>

                <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
                    <div class="inventory-header__badge">
                        <span>${iconMarkup("clipboard")}</span>
                        <strong>ACTAS</strong>
                    </div>

                    <a class="btn btn-secondary" href="${actasBasePath()}/actas.html" style="width: 100%; text-align: center; display: inline-flex; justify-content: center; align-items: center; border-radius: 12px; font-size: 13px; padding: 10px 16px; background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; text-decoration: none; box-shadow: 0 2px 4px rgba(21, 101, 192, 0.1); cursor: pointer; transition: all 0.2s;">
                        Regresar a Actas
                    </a>
                </div>
            </div>

            ${renderActaLicenciasSearchPanel()}

                <form id="actaLicenciasForm" class="acta-pc-form hidden">
                    <div class="acta-sheet" style="width:794px; min-height:1123px; max-width:100%; margin:0 auto; box-sizing:border-box; display:flex; flex-direction:column;">

                    <div class="acta-sheet__header">
                        <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador" style="width:145px; max-width:28%; height:auto; margin:0; object-fit:contain; visibility:visible; flex-shrink:1;">
                        <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi" style="width:175px; max-width:34%; height:auto; margin:0; object-fit:contain; flex-shrink:1;">
                    </div>

                    <div class="acta-sheet__titles">
                        <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                        <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                        <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                    </div>

                    <table class="acta-table">
                        <tr>
                            <th colspan="6">DATOS DEL FUNCIONARIO SENADI</th>
                        </tr>

                        <tr>
                            <td class="acta-table__label">NOMBRE</td>
                            <td colspan="2">
                                <input id="actaLicFuncionarioNombre" class="acta-input" type="text" required>
                            </td>

                            <td class="acta-table__label">CARGO</td>
                            <td>
                                <input id="actaLicFuncionarioCargo" class="acta-input" type="text" required>
                            </td>

                            <td class="acta-table__label-value">
                                <input id="actaLicFuncionarioExtension" class="acta-input" type="text" placeholder="Nº EXT.">
                            </td>
                        </tr>

                        <tr>
                            <td class="acta-table__label">CORREO</td>
                            <td colspan="2">
                                <input id="actaLicFuncionarioCorreo" class="acta-input" type="email" required>
                            </td>

                            <td class="acta-table__label">ÁREA</td>
                            <td>
                                <input id="actaLicFuncionarioArea" class="acta-input" type="text" required>
                            </td>

                            <td class="acta-table__label-value">
                                <input id="actaLicFuncionarioEdificio" class="acta-input" type="text" placeholder="EDIFICIO" required>
                            </td>
                        </tr>
                    </table>

                    <table class="acta-table acta-table--equipos">
                        <tr>
                            <th colspan="5">EQUIPOS</th>
                        </tr>

                        <tr>
                            <th>TIPO</th>
                            <th>MARCA</th>
                            <th>MODELO</th>
                            <th>SERIAL</th>
                            <th>CÓDIGO</th>
                        </tr>

                        <tbody id="actaLicEquiposBody">
                            ${renderActaLicEquipoRow()}
                        </tbody>
                    </table>

                    ${renderCertificationHtml(ACTA_LICENCIAS_CERTIFICATION_TEXT)}

                    
                    <table class="acta-table acta-table--firma">
                        <tr>
                            <th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th>
                        </tr>
                        <tr>
                            <th>ENTREGA</th>
                            <th>RECIBE</th>
                        </tr>
                        <tr>
                            <td>
                                <label class="acta-signature-field">Nombre:
                                    <select id="actaLicEntregaNombre" class="acta-input" required>
                                        <option value="">-- Seleccionar --</option>
                                        <option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option>
                                        <option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option>
                                        <option value="MICHAEL J. YANANGOMEZ SUAREZ">MICHAEL J. YANANGOMEZ SUAREZ</option>
                                        <option value="OTRO">OTRO</option>
                                    </select>
                                    <input id="actaLicEntregaNombreOtro"
                                        class="acta-input hidden"
                                        placeholder="Escriba el nombre completo"
                                        style="margin-top: 8px;">
                                </label>
                            </td>
                            <td>
                                <label class="acta-signature-field">Nombre:
                                    <input id="actaLicRecibeNombre" class="acta-input" required>
                                </label>
                            </td>
                        </tr>
                        <tr class="acta-table__row--firma">
                            <td>
                                <label class="acta-signature-field">Firma:
                                    <input id="actaLicEntregaFirma"
                                        class="acta-input acta-input--readonly"
                                        readonly tabindex="-1"
                                        placeholder="Firma electrónica">
                                </label>
                            </td>
                            <td>
                                <label class="acta-signature-field">Firma:
                                    <input id="actaLicRecibeFirma"
                                        class="acta-input acta-input--readonly"
                                        readonly tabindex="-1"
                                        placeholder="Firma electrónica">
                                </label>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label class="acta-signature-field">Fecha:
                                    <input id="actaLicEntregaFecha"
                                        class="acta-input" type="date" required>
                                </label>
                            </td>
                            <td>
                                <label class="acta-signature-field">Fecha:
                                    <input id="actaLicRecibeFecha"
                                        class="acta-input" type="date" required>
                                </label>
                            </td>
                        </tr>
                    </table>


                    <div class="acta-sheet__footer" style="margin-top:auto; width:100%; max-width:100%; box-sizing:border-box; overflow:hidden;">
                        <div class="acta-sheet__footer-text">
                            <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                            <span>Código postal: 170518 / Quito — Ecuador</span>
                            <span>Teléfono: +593-2 394 0000</span>
                            <span>www.derechosintelectuales.gob.ec</span>
                        </div>

                        <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="Gobierno del Ecuador" class="acta-sheet__footer-logo" style="width:100%; max-width:100%; height:auto; margin:0; object-fit:contain; flex-shrink:1;">
                    </div>

                </div>

                <div class="acta-form-actions" style="margin-top:24px;">
                    <button type="button" class="btn btn-primary" id="actaLicPreviewButton">Previsualizar</button>
                    <button type="button" class="btn btn-secondary" id="actaLicExportButton">Exportar</button>
                    <button type="button" class="btn btn-secondary" id="actaLicExportPdfButton">Exportar PDF</button>
                    <button type="button" class="btn btn-secondary" id="actaLicDeleteButton">Eliminar</button>
                </div>
            </form>
        </section>
    `;
}

    function renderActaFormPage(pageName) {
        if (pageName === "acta-equipos") {
            return renderActaEquiposPage();
        }
        if (pageName === "acta-software") {
            return renderActaSoftwarePage();
        }
        if (pageName === "acta-licencias") {
            return renderActaLicenciasPage();
        }

        const config = actaConfig(pageName);
        if (!config) {
            return "";
        }

        return `
            <section class="panel panel--narrow acta-form-panel">
                <div class="inventory-header inventory-header--form">
                    <div>
                        <div class="eyebrow">${config.eyebrow}</div>
                        <h2>${config.title}</h2>
                        <p>${config.description}</p>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
                        <div class="inventory-header__badge">
                            <span>${iconMarkup("clipboard")}</span>
                            <strong>ACTAS</strong>
                        </div>
                        <a class="btn btn-secondary" href="${basePath}/actas.html" style="width: 100%; text-align: center; display: inline-flex; justify-content: center; align-items: center; border-radius: 12px; font-size: 13px; padding: 10px 16px; background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; text-decoration: none; box-shadow: 0 2px 4px rgba(21, 101, 192, 0.1); cursor: pointer; transition: all 0.2s;">Regresar a Actas</a>
                    </div>
                </div>
                <div class="helper-banner">${config.hint}</div>
                <form class="acta-form" data-acta-form="${pageName}">
                    <div class="form-grid acta-form__grid">
                        ${config.fields}
                    </div>
                </form>
            </section>
        `;
    }

    const ACTA_PC_SUBSECTIONS = [
        { key: "pc", label: "PC", active: true },
        { key: "laptop", label: "Laptop", active: false },
        { key: "impresora", label: "Impresora", active: false },
        { key: "escaner", label: "Escáner", active: false },
        { key: "otros", label: "Otros", active: false }
    ];

        const ACTA_PC_ACTIVITY_ROWS = [
        "DESFRAGMENTACIÓN DE DISCOS",
        "DEPURACIÓN DE SOFTWARE",
        "ANÁLISIS Y LIMPIEZA DE VIRUS",
        "INSTALACIÓN DE ACTUALIZACIONES (PARCHES) DEL SISTEMA OPERATIVO Y DE APLICACIONES.",
        "LIMPIEZA DE PARTES Y PIEZAS",
        "REORGANIZACIÓN DE CABLES DE CONEXIÓN",
        "OTRAS ACCIONES REALIZADAS"
    ];

    const ACTA_PC_CERTIFICATION_TEXT = "Certifico que los elementos detallados en el presente documento me han sido entregados para mi cuidado y custodia con el propósito de cumplir con las tareas y asignaciones propias de mi cargo en la Institución, siendo estos de mi única y exclusiva responsabilidad. Me comprometo a usar correctamente los recursos, y solo para los fines establecidos, a no instalar ni permitir la instalación de software por personal ajeno al área de soporte de DTIC; ante cualquier novedad daré conocimiento a los técnicos de DTIC.";
    // Dos párrafos separados por una línea en blanco ("\n\n")
    const ACTA_LICENCIAS_CERTIFICATION_TEXT = "Certifico que las licencias detalladas en el presente documento me han sido entregadas para mi uso y responsabilidad, con el propósito de cumplir con las tareas y asignaciones propias de mi cargo en la Institución, y que las mismas se encuentran instaladas en el equipo informático asignado a mi cargo.\n\nMe comprometo a utilizar correctamente estos recursos únicamente para los fines institucionales establecidos, a no ceder, compartir ni permitir su uso por personal no autorizado, y a notificar oportunamente a la Dirección de Tecnologías de la Información y Comunicación (DTIC) ante cualquier novedad relacionada con las mismas.";

    function certificationParagraphs(texto) {
        return String(texto || "").split(/\n\s*\n/).map((parte) => parte.trim()).filter(Boolean);
    }

    function renderCertificationHtml(texto) {
        return certificationParagraphs(texto)
            .map((parte) => `<p class="acta-certification">${escapeHtml(parte)}</p>`)
            .join("");
    }

    const ACTA_RC_GROUPS = [
        {
            key: "centralTelefonica",
            title: "CENTRAL TELEFONICA",
            label: "Central Telefónica",
            keywords: ["telefono", "telef", "central", "pbx", "call", "extension", "cisco", "ata"]
        },
        {
            key: "equiposCisco",
            title: "EQUIPOS CISCO",
            label: "Equipos Cisco",
            keywords: ["cisco"]
        },
        {
            key: "controladoraWifiAccessPoints",
            title: "CONTROLADORA WIFI Y ACCESS POINTS",
            label: "Controladora Wifi y Access Points",
            keywords: ["access point", "wifi", "wireless", "wlc", "controladora", "ap"]
        },
        {
            key: "servidores",
            title: "SERVIDORES",
            label: "Servidores",
            keywords: ["server", "servidor", "nas", "storage"]
        },
        {
            key: "switches",
            title: "SWITCHES",
            label: "Switches",
            keywords: ["switch", "stack", "catalyst"]
        }
    ];

    const ACTA_RC_ACTIVITY_GROUPS = {
        centralTelefonica: [
            "MONITOREO CONTINUO",
            "RESPALDO DE CONFIGURACION",
            "ACTUALIZACIONES Y PARCHES",
            "PLAN DE CONTINUIDAD"
        ],
        equiposCisco: [
            "CONFIGURACION OPTIMA",
            "GESTION DE TRAFICO",
            "MONITOREO AVANZADO"
        ],
        controladoraWifiAccessPoints: [
            "OPTIMIZACION DE COBERTURA",
            "GESTION DE USUARIOS",
            "BALANCEO DE CARGAS"
        ],
        servidores: [
            "SUPERVISION Y MANTENIMIENTO",
            "GESTION DE RECURSOS",
            "SEGURIDAD"
        ],
        switches: [
            "GESTION DE REDES",
            "SUPERVISION DE TRAFICO",
            "SEGURIDAD"
        ]
    };

    const ACTA_RC_CERTIFICATION_TEXT = "Certifico que los elementos detallados en el presente documento, me han sido instalados para mi cuidado y custodia con el propósito de cumplir con las tareas y asignaciones propias de mi cargo en la Institución, siendo estos de mi única y exclusiva responsabilidad. Me comprometo a usar correctamente los recursos, y solo para los fines establecidos, a no instalar ni permitir la instalación de software por personal ajeno al área de soporte de DTIC, dado cualquier novedad dar conocimiento a los técnicos de DTIC.";

    const ACTA_IMPRESORA_ACTIVITY_ROWS = [
        "LIMPIEZA DE EQUIPO",
        "CALIBRACIÓN",
        "VERIFICACIÓN DE ALIMENTACIÓN",
        "ASPIRACIÓN Y SOPLETEO DE PARTES ELECTRÓNICAS",
        "ESTADO DE CONSUMIBLE"
    ];

    const ACTA_ESCANER_ACTIVITY_ROWS = [
        "LIMPIEZA DE EQUIPO",
        "CALIBRACIÓN",
        "VERIFICACIÓN DE ALIMENTACIÓN",
        "ASPIRACIÓN Y SOPLETEO DE PARTES ELECTRÓNICAS"
    ];

    const ACTA_PROYECTOR_ACTIVITY_ROWS = [
        "LIMPIEZA DE EQUIPO",
        "COMPROBACIÓN DE LÁMPARA",
        "ALINEACIÓN Y ENFOQUE",
        "VERIFICACIÓN DE ALIMENTACIÓN",
        "ASPIRACIÓN Y SOPLETEO DE PARTES ELECTRÓNICAS"
    ];

    const ACTA_TELEFONO_ACTIVITY_ROWS = [
        "LIMPIEZA EXTERNA DE EQUIPO",
        "ASPIRACIÓN Y SOPLETEO DE PARTES ELECTRÓNICAS",
        "LIMPIEZA DE PANTALLA LED",
        "LIMPIEZA DE PUERTOS RJ45"
    ];

    const ACTA_SOFTWARE_PAGE1 = [
        { categoria: "SISTEMA OPERATIVO",               programa: "MICROSOFT WINDOWS 10 PRO" },
        { categoria: "SISTEMA OPERATIVO",               programa: "MICROSOFT WINDOWS 11 PRO" },
        { categoria: "PAQUETE OFIMÁTICO",               programa: "MICROSOFT OFFICE 365 PRO PLUS" },
        { categoria: "SOFTWARE ANTIVIRUS",              programa: "ESET ENDPOINT SECURITY" },
        { categoria: "NAVEGADORES",                     programa: "MOZILLA FIREFOX" },
        { categoria: "NAVEGADORES",                     programa: "GOOGLE CHROME" },
        { categoria: "NAVEGADORES",                     programa: "MICROSOFT EDGE" },
        { categoria: "NAVEGADORES",                     programa: "SAFARI" },
        { categoria: "SOFTWARE SOPORTE REMOTO",         programa: "TIGHT VNC SERVICE" },
        { categoria: "SOFTWARE SOPORTE REMOTO",         programa: "ANYDESK" },
        { categoria: "SOFTWARE DE FIRMA ELECTRÓNICA",   programa: "FIRMA EC" },
        { categoria: "SOFTWARE DE FIRMA ELECTRÓNICA",   programa: "FIRMA MASIVA SENADI" },
        { categoria: "SOFTWARE PARA VISUALIZACIÓN PDF", programa: "PDF 24" },
        { categoria: "SOFTWARE PARA VISUALIZACIÓN PDF", programa: "ADOBE ACROBAT READER" },
        { categoria: "SOFTWARE PARA VISUALIZACIÓN PDF", programa: "NITRO PDF" },
        { categoria: "COMPRESIÓN",                      programa: "WINRAR" },
        { categoria: "CORREOS",                         programa: "ZIMBRA DESKTOP" },
        { categoria: "VIDEO CONFERENCIA",               programa: "ZOOM MEETINGS" }
    ];

    const ACTA_SOFTWARE_DRIVERS = [
        "HP LASER JET COLOR",
        "HP LASER JET B/N",
        "LEXMARK LASER B/N",
        "ESCÁNER EPSON",
        "ESCÁNER HP",
        "ESCÁNER KODAK"
    ];

    // Versión de los logos de actas: cambiarla cada vez que se reemplacen las
    // imágenes (mismo nombre) para que el navegador no muestre las antiguas.
    const ACTA_ASSETS_VERSION = "20261022";

    function actaAssetPath(name) {
        return `${basePrefix}/assets/actas/${name}?v=${ACTA_ASSETS_VERSION}`;
    }

    function renderActaSoftwarePage() {
        const swRows = ACTA_SOFTWARE_PAGE1.map((item, index) => `
            <tr>
                <td class="acta-table__activity">${item.categoria}</td>
                <td class="acta-table__activity">${item.programa}</td>
                <td class="acta-sw-radio">
                    <label><input type="radio" name="sw${index}" value="SI"> SI</label>
                    <label><input type="radio" name="sw${index}" value="NO"> NO</label>
                </td>
            </tr>
        `).join("");

        const drRows = ACTA_SOFTWARE_DRIVERS.map((drv, i) => `
            <tr>
                <td class="acta-table__activity">CONTROLADORES / DRIVERS</td>
                <td class="acta-table__activity">${drv}</td>
                <td class="acta-sw-radio">
                    <label><input type="radio" name="drv${i}" value="SI"> SI</label>
                    <label><input type="radio" name="drv${i}" value="NO"> NO</label>
                </td>
            </tr>
        `).join("");

        const adicRows = [0].map((i) => `
            <tr data-acta-adicional-row="${i}">
                <td><input id="actaSwAdicional${i}" class="acta-input" placeholder="Descripción del software adicional"></td>
                <td class="acta-sw-radio">
                    <label><input type="radio" name="adic${i}" value="SI"> SI</label>
                    <label><input type="radio" name="adic${i}" value="NO"> NO</label>
                </td>
            </tr>
        `).join("");

        return `
            <section class="panel panel--narrow acta-form-panel">
                <div class="inventory-header inventory-header--form">
                    <div>
                        <div class="eyebrow">Programas y aplicaciones</div>
                        <h2>Formulario de software</h2>
                        <p>Registra los programas y aplicaciones instaladas en un equipo institucional.</p>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
                        <div class="inventory-header__badge">
                            <span>${iconMarkup("clipboard")}</span>
                            <strong>ACTAS</strong>
                        </div>
                        <a class="btn btn-secondary" href="${actasBasePath()}/actas.html" style="width: 100%; text-align: center; display: inline-flex; justify-content: center; align-items: center; border-radius: 12px; font-size: 13px; padding: 10px 16px; background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; text-decoration: none; box-shadow: 0 2px 4px rgba(21, 101, 192, 0.1); cursor: pointer; transition: all 0.2s;">Regresar a Actas</a>
                    </div>
                </div>

                <div class="acta-search-panel">
                    <div>
                        <div class="eyebrow">Búsqueda inicial</div>
                        <h3>Seleccione el equipo para generar el acta de software</h3>
                        <p>Busque el PC o Laptop del funcionario por custodio, código, marca, modelo, serie o edificio.</p>
                    </div>
                    <div class="acta-search-panel__filters">
                        <div class="field-group">
                            <label for="actaSwFilterTipo">Tipo de equipo</label>
                            <select id="actaSwFilterTipo">
                                <option value="">Todos (PC / Laptop)</option>
                                <option value="pc">PC</option>
                                <option value="laptop">Laptop</option>
                            </select>
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterSO">Sistema Operativo</label>
                            <select id="actaSwFilterSO">
                                <option value="">Todos</option>
                                <option value="WIN 10">Windows 10</option>
                                <option value="WIN 11">Windows 11</option>
                                <option value="WIN 7">Windows 7</option>
                                <option value="UBUNTU">Ubuntu</option>
                            </select>
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterMarca">Marca del equipo</label>
                            <select id="actaSwFilterMarca">
                                <option value="">Todas</option>
                                <option value="HP">HP</option>
                                <option value="DELL">Dell</option>
                                <option value="ACER">Acer</option>
                                <option value="LENOVO">Lenovo</option>
                            </select>
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterEstado">Estado del equipo</label>
                            <select id="actaSwFilterEstado">
                                <option value="">Todos</option>
                                <option value="OPERATIVO">Operativo</option>
                                <option value="NO OPERATIVO">No operativo</option>
                                <option value="REPORTADO PARA DAR DE BAJA">Reportado para baja</option>
                            </select>
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterCustodio">Custodio / Usuario</label>
                            <input id="actaSwFilterCustodio" type="search" placeholder="Nombre del funcionario...">
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterArea">Área / Dirección</label>
                            <input id="actaSwFilterArea" type="search" placeholder="Dirección o área...">
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterEdificio">Edificio</label>
                            <input id="actaSwFilterEdificio" type="search" placeholder="Edificio...">
                        </div>
                        <div class="field-group">
                            <label for="actaSwFilterCodigoSbai">Código SBYE</label>
                            <input id="actaSwFilterCodigoSbai" type="search" placeholder="SBYE-...">
                        </div>
                    </div>
                    <div class="acta-search-panel__actions">
                        <button type="button" class="btn btn-primary" id="actaSwEquipoBuscarButton">Buscar</button>
                        <button type="button" class="btn btn-secondary" id="actaSwEquipoLimpiarButton">Limpiar filtros</button>
                    </div>
                    <div id="actaSwEquipoResultados" class="acta-search-results hidden"></div>
                </div>

                <form id="actaSoftwareForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaSwSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE PROGRAMAS Y APLICACIONES INSTALADAS</h2>
                        </div>

                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaSwFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaSwFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaSwFuncionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaSwFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaSwFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaSwFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>

                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th>
                                <th>MARCA</th>
                                <th>MODELO</th>
                                <th>SERIAL</th>
                                <th>CÓDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaSwEquipoTipo" class="acta-input" value="PC"></td>
                                <td><input id="actaSwEquipoMarca" class="acta-input"></td>
                                <td><input id="actaSwEquipoModelo" class="acta-input"></td>
                                <td><input id="actaSwEquipoSerial" class="acta-input"></td>
                                <td><input id="actaSwEquipoCodigo" class="acta-input" required></td>
                            </tr>
                        </table>

                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="3" class="acta-table__title-dark">PROGRAMAS Y APLICACIONES INSTALADAS</th></tr>
                            <tr>
                                <th class="acta-table__label-large">CATEGORÍA</th>
                                <th class="acta-table__label-large">PROGRAMA / APLICACIÓN</th>
                                <th>INSTALADO</th>
                            </tr>
                            ${swRows}
                        </table>

                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="3" class="acta-table__title-dark">CONTROLADORES / DRIVERS</th></tr>
                            <tr>
                                <th class="acta-table__label-large">CATEGORÍA</th>
                                <th class="acta-table__label-large">PROGRAMA / APLICACIÓN</th>
                                <th>INSTALADO</th>
                            </tr>
                            ${drRows}
                        </table>

                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="2" class="acta-table__title-dark">SOFTWARE Y DRIVERS ADICIONAL</th></tr>
                            <tr>
                                <th class="acta-table__label-large">DESCRIPCIÓN</th>
                                <th>INSTALADO</th>
                            </tr>
                            <tbody id="actaSwAdicionalesTable">
                                ${adicRows}
                            </tbody>
                        </table>
                        <div style="margin-bottom: 20px;">
                            <button type="button" class="btn btn-secondary" id="actaSwAgregarAdicionalButton">+ Agregar programa adicional</button>
                        </div>

                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCIÓN DEL EQUIPO</th></tr>
                            <tr>
                                <th>ENTREGA</th>
                                <th>RECIBE</th>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <select id="actaSwEntregaNombre" class="acta-input" required>
                                            <option value="">-- Seleccionar --</option>
                                            <option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option>
                                            <option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option>
                                            <option value="OTRO">OTRO</option>
                                        </select>
                                        <input id="actaSwEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <input id="actaSwRecibeNombre" class="acta-input" required>
                                    </label>
                                </td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaSwEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaSwRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaSwEntregaFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaSwRecibeFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                            </tr>
                        </table>

                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>

                    <div class="acta-form-actions">
                        <button type="button" class="btn btn-secondary" id="actaSwPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-primary" id="actaSwExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaSwExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaSwResetButton">Restablecer</button>
                    </div>
                </form>
            </section>
        `;
    }

    function autofillActaSwForm(item) {
        setInputValue("actaSwFuncionarioNombre", item.custodio || "");
        setInputValue("actaSwFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaSwFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaSwEquipoTipo", String(item.tipo || "PC").toUpperCase());
        setInputValue("actaSwEquipoMarca", item.marca || "");
        setInputValue("actaSwEquipoModelo", item.modelo || "");
        setInputValue("actaSwEquipoSerial", item.numeroSerie || "");
        setInputValue("actaSwEquipoCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaSwRecibeNombre", item.custodio || "");
    }

    function loadActaSwInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaSwEntregaFecha", "actaSwRecibeFecha"].forEach((id) => {
            const el = document.getElementById(id);
            if (el && !el.value) el.value = today;
        });
    }

    function resetActaSwForm() {
        document.getElementById("actaSoftwareForm")?.reset();
        const sel = document.getElementById("actaSwSelector");
        if (sel) sel.value = "";
        document.querySelectorAll(".acta-search-card").forEach((c) => c.classList.remove("is-selected"));
        document.getElementById("actaSoftwareForm")?.classList.add("hidden");
        loadActaSwInitialData();
    }

    function buildActaSwPayload() {
        const swItems = ACTA_SOFTWARE_PAGE1.map((item, i) => {
            const checked = document.querySelector(`input[name="sw${i}"]:checked`);
            return { categoria: item.categoria, programa: item.programa, instalado: checked ? checked.value : "" };
        });
        const drvItems = ACTA_SOFTWARE_DRIVERS.map((drv, i) => {
            const checked = document.querySelector(`input[name="drv${i}"]:checked`);
            return { categoria: "CONTROLADORES / DRIVERS", programa: drv, instalado: checked ? checked.value : "" };
        });
        const adicionales = [0, 1, 2].map((i) => {
            return document.getElementById(`actaSwAdicional${i}`)?.value.trim() || "";
        });

        return {
            funcionario: {
                nombre:    document.getElementById("actaSwFuncionarioNombre")?.value.trim() || "",
                cargo:     document.getElementById("actaSwFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaSwFuncionarioExtension")?.value.trim() || "",
                correo:    document.getElementById("actaSwFuncionarioCorreo")?.value.trim() || "",
                area:      document.getElementById("actaSwFuncionarioArea")?.value.trim() || "",
                edificio:  document.getElementById("actaSwFuncionarioEdificio")?.value.trim() || ""
            },
            equipo: {
                tipo:   document.getElementById("actaSwEquipoTipo")?.value.trim() || "",
                marca:  document.getElementById("actaSwEquipoMarca")?.value.trim() || "",
                modelo: document.getElementById("actaSwEquipoModelo")?.value.trim() || "",
                serial: document.getElementById("actaSwEquipoSerial")?.value.trim() || "",
                codigo: document.getElementById("actaSwEquipoCodigo")?.value.trim() || ""
            },
            softwareItems: [...swItems, ...drvItems],
            driversAdicionales: adicionales,
            entrega: {
                nombre: getEntregaNombreValue("actaSwEntregaNombre", "actaSwEntregaNombreOtro"),
                firma:  document.getElementById("actaSwEntregaFirma")?.value.trim() || "",
                fecha:  document.getElementById("actaSwEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaSwRecibeNombre")?.value.trim() || "",
                firma:  document.getElementById("actaSwRecibeFirma")?.value.trim() || "",
                fecha:  document.getElementById("actaSwRecibeFecha")?.value || ""
            }
        };
    }

    function renderActaSwPreview(payload) {
        const f = payload.funcionario || {};
        const eq = payload.equipo || {};
        const swItems = Array.isArray(payload.softwareItems) ? payload.softwareItems : [];
        const page1 = swItems.slice(0, ACTA_SOFTWARE_PAGE1.length);
        const drivers = swItems.slice(ACTA_SOFTWARE_PAGE1.length, ACTA_SOFTWARE_PAGE1.length + ACTA_SOFTWARE_DRIVERS.length);
        const adicionales = Array.isArray(payload.driversAdicionales) ? payload.driversAdicionales : [];

        // Only render items marked SI (checklist — unmarked items do not appear)
        const swRowsHtml = ACTA_SOFTWARE_PAGE1
            .map((item, i) => ({ item, ins: page1[i] ? (page1[i].instalado || "") : "" }))
            .filter(({ ins }) => ins === "SI")
            .map(({ item }) =>
                `<tr><td>${escapeHtml(item.categoria)}</td><td>${escapeHtml(item.programa)}</td><td style="text-align:center;font-weight:bold">SI</td></tr>`
            ).join("");

        const drvRowsHtml = ACTA_SOFTWARE_DRIVERS
            .map((drv, i) => ({ drv, ins: drivers[i] ? (drivers[i].instalado || "") : "" }))
            .filter(({ ins }) => ins === "SI")
            .map(({ drv }) =>
                `<tr><td>CONTROLADORES / DRIVERS</td><td>${escapeHtml(drv)}</td><td style="text-align:center;font-weight:bold">SI</td></tr>`
            ).join("");

        // Adicionales: only show rows with a real description — 3 columns match the table header
        const adicHtml = adicionales
            .filter((v) => v && v.trim())
            .map((v) =>
                `<tr><td>${escapeHtml(v)}</td><td style="text-align:center;font-weight:bold">SI</td></tr>`
            ).join("");

        return `
            <div class="acta-sheet acta-sheet--preview" style="width:100%; max-width:760px; aspect-ratio:210 / 297; margin:0 auto; box-sizing:border-box; display:flex; flex-direction:column; overflow:hidden;">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE PROGRAMAS Y APLICACIONES INSTALADAS</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td><td>${escapeHtml(f.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td><td>${escapeHtml(f.cargo || "")}</td>
                        <td class="acta-table__label">N&#176; EXT.</td><td>${escapeHtml(f.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td><td>${escapeHtml(f.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td><td>${escapeHtml(f.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(f.edificio || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                    <tr>
                        <td>${escapeHtml(eq.tipo || "")}</td><td>${escapeHtml(eq.marca || "")}</td>
                        <td>${escapeHtml(eq.modelo || "")}</td><td>${escapeHtml(eq.serial || "")}</td>
                        <td>${escapeHtml(eq.codigo || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="3" class="acta-table__title-dark">PROGRAMAS Y APLICACIONES INSTALADAS</th></tr>
                    <tr><th>CATEGORÍA</th><th>PROGRAMA / APLICACIÓN</th><th>INSTALADO</th></tr>
                    ${swRowsHtml}
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="3" class="acta-table__title-dark">CONTROLADORES / DRIVERS</th></tr>
                    <tr><th>CATEGORÍA</th><th>PROGRAMA / APLICACIÓN</th><th>INSTALADO</th></tr>
                    ${drvRowsHtml}
                </table>
                ${adicHtml ? `<table class="acta-table acta-table--actividades">
                    <tr><th colspan="2" class="acta-table__title-dark">SOFTWARE Y DRIVERS ADICIONAL</th></tr>
                    <tr><th>DESCRIPCIÓN</th><th>INSTALADO</th></tr>
                    ${adicHtml}
                </table>` : ""}
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCIÓN DEL EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer" style="margin-top:auto; width:100%; max-width:100%; box-sizing:border-box; overflow:hidden;">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +593-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>

                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="Gobierno del Ecuador" class="acta-sheet__footer-logo" style="width:100%; max-width:100%; height:auto; margin:0; object-fit:contain; flex-shrink:1;">
                </div>
            </div>
        `;
    }

    function openActaSwPreview() {
        const form = document.getElementById("actaSoftwareForm");
        const selector = document.getElementById("actaSwSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaSwPayload();
        openModal(
            "Previsualización del Acta de Software",
            `<div class="acta-preview">${renderActaSwPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaSoftware("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaSoftware("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaSoftware(format) {
        const form = document.getElementById("actaSoftwareForm");
        const selector = document.getElementById("actaSwSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaSwPayload();

        if (format === "pdf") {
            await exportActaSwPdfAuto(payload);
            return;
        }

        try {
            const response = await apiFetch(`/actas/software/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (e) { /* ignore */ }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename="?([^";]+)"?/i);
            const filename = match?.[1] || `acta_software.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function loadScriptOnce(src) {
        return new Promise((resolve, reject) => {
            if (document.querySelector(`script[src="${src}"]`)) { resolve(); return; }
            const s = document.createElement("script");
            s.src = src;
            s.onload = resolve;
            s.onerror = () => reject(new Error("No se pudo cargar: " + src));
            document.head.appendChild(s);
        });
    }

    async function exportActaSwPdfAuto(payload) {
        let container = null;
        try {
            showToast("Generando PDF", "Preparando el documento...", "info");

            // Load html2canvas and jsPDF independently for full control
            await loadScriptOnce("https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js");
            await loadScriptOnce("https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js");

            const previewHtml = renderActaSwPreview(payload);

            // Container must be FULLY VISIBLE in the DOM — html2canvas cannot capture
            // hidden/visibility:hidden/off-screen elements. Use z-index overlay approach.
            container = document.createElement("div");
            container.className = "acta-preview";
            container.style.cssText = [
                "position:fixed", "top:0", "left:0",
                "width:800px", "min-height:200px",
                "background:#fff", "padding:24px 28px",
                "box-sizing:border-box",
                "z-index:2147483647",    // maximum z-index — on top of everything
                "overflow:visible"
            ].join(";");
            container.innerHTML = previewHtml;
            document.body.appendChild(container);

            // Wait for every image inside to fully load (logos, etc.)
            const imgs = Array.from(container.querySelectorAll("img"));
            await Promise.all(imgs.map((img) => new Promise((resolve) => {
                if (img.complete && img.naturalWidth > 0) { resolve(); return; }
                img.addEventListener("load",  resolve, { once: true });
                img.addEventListener("error", resolve, { once: true });
            })));

            // Let browser finish layout pass
            await new Promise((r) => setTimeout(r, 400));

            // Sanity checks
            const w = container.offsetWidth;
            const h = container.offsetHeight;
            if (!w || !h) throw new Error(`Contenedor sin dimensiones (${w}x${h}). El DOM no renderizó el contenido.`);

            // Capture the real rendered DOM with html2canvas
            const canvas = await window.html2canvas(container, {
                scale:           2,
                useCORS:         true,
                allowTaint:      true,
                backgroundColor: "#ffffff",
                logging:         false,
                width:           w,
                height:          h,
                scrollX:         0,
                scrollY:         0
            });

            if (canvas.width === 0 || canvas.height === 0) {
                throw new Error("html2canvas devolvió un canvas vacío.");
            }

            // Build PDF page-by-page
            const { jsPDF } = window.jspdf;
            const pdf      = new jsPDF({ unit: "mm", format: "a4", orientation: "portrait" });
            const pageW    = pdf.internal.pageSize.getWidth();
            const pageH    = pdf.internal.pageSize.getHeight();
            const margin   = 8;
            const contentW = pageW - margin * 2;
            const pxPerMm  = canvas.width / contentW;
            const pageHpx  = (pageH - margin * 2) * pxPerMm;

            let srcY = 0;
            while (srcY < canvas.height) {
                if (srcY > 0) pdf.addPage();
                const srcH     = Math.min(pageHpx, canvas.height - srcY);
                const slice    = document.createElement("canvas");
                slice.width    = canvas.width;
                slice.height   = srcH;
                slice.getContext("2d").drawImage(canvas, 0, srcY, canvas.width, srcH, 0, 0, canvas.width, srcH);
                const sliceH   = srcH / pxPerMm;
                pdf.addImage(slice.toDataURL("image/jpeg", 0.97), "JPEG", margin, margin, contentW, sliceH);
                srcY += srcH;
            }

            pdf.save("acta_programas_aplicaciones.pdf");
            showToast("Exportación lista", "PDF generado correctamente.", "success");

        } catch (err) {
            console.error("[exportActaSwPdfAuto]", err);
            showToast("Error", err.message || "No se pudo generar el PDF.", "danger");
        } finally {
            if (container?.parentNode) container.parentNode.removeChild(container);
        }
    }

    function collectActaSwSearchCriteria() {
        return {
            tipo:               document.getElementById("actaSwFilterTipo")?.value.trim() || "",
            sistemaOperativo:   document.getElementById("actaSwFilterSO")?.value.trim() || "",
            marca:              document.getElementById("actaSwFilterMarca")?.value.trim() || "",
            estado:             document.getElementById("actaSwFilterEstado")?.value.trim() || "",
            custodio:           document.getElementById("actaSwFilterCustodio")?.value.trim() || "",
            ubicacionDireccion: document.getElementById("actaSwFilterArea")?.value.trim() || "",
            ubicacionEdificio:  document.getElementById("actaSwFilterEdificio")?.value.trim() || "",
            codigoSbai:         document.getElementById("actaSwFilterCodigoSbai")?.value.trim() || ""
        };
    }

    function clearActaSwFilters() {
        ["actaSwFilterTipo", "actaSwFilterSO", "actaSwFilterMarca", "actaSwFilterEstado"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });
        ["actaSwFilterCustodio", "actaSwFilterArea", "actaSwFilterEdificio", "actaSwFilterCodigoSbai"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });
        // Resetear estado de paginación
        state.actaSwCurrentPage = 1;
        state.actaSwLastFiltered = [];
        state.actaSwLastCriteria = {};
        const results = document.getElementById("actaSwEquipoResultados");
        if (results) {
            results.classList.add("hidden");
            results.innerHTML = "";
        }
    }

    function runActaSwEquipoSearch() {
        const criteria = collectActaSwSearchCriteria();
        const results = document.getElementById("actaSwEquipoResultados");
        if (!results) return;

        const allPcLaptop = (state.inventory || []).filter((item) => ["pc", "laptop"].includes(String(item.tipo || "").toLowerCase()));

        const filtered = allPcLaptop.filter((item) => {
            const tipo = String(item.tipo || "").toLowerCase();

            if (criteria.tipo && tipo !== criteria.tipo.toLowerCase()) return false;
            if (criteria.sistemaOperativo && !String(item.sistemaOperativo || "").toUpperCase().includes(criteria.sistemaOperativo.toUpperCase())) return false;
            if (criteria.marca && !String(item.marca || "").toUpperCase().includes(criteria.marca.toUpperCase())) return false;
            if (criteria.estado && !String(item.estado || "").toUpperCase().includes(criteria.estado.toUpperCase())) return false;
            if (criteria.custodio && !String(item.custodio || "").toLowerCase().includes(criteria.custodio.toLowerCase())) return false;
            if (criteria.ubicacionDireccion && !String(item.ubicacionDireccion || "").toLowerCase().includes(criteria.ubicacionDireccion.toLowerCase())) return false;
            if (criteria.ubicacionEdificio && !String(item.ubicacionEdificio || "").toLowerCase().includes(criteria.ubicacionEdificio.toLowerCase())) return false;
            if (criteria.codigoSbai && !String(item.codigoSbai || "").toLowerCase().includes(criteria.codigoSbai.toLowerCase())) return false;
            return true;
        });

        const hasFilters = Object.values(criteria).some(Boolean);
        
        // Guardar para paginación
        state.actaSwLastFiltered = filtered;
        state.actaSwLastCriteria = criteria;
        state.actaSwCurrentPage = 1;
        
        // Si no hay filtros (TODOS), mostrar todos. Si hay filtros, mostrar 30 con paginación
        const pageSize = 30;
        const items = hasFilters ? filtered.slice(0, pageSize) : filtered;

        results.classList.remove("hidden");

        if (!items.length) {
            results.innerHTML = '<div class="empty-state">Sin resultados. Ajuste los filtros e intente de nuevo.</div>';
            if (hasFilters) showToast("Sin resultados", "No se encontraron equipos con esos criterios.", "info");
            return;
        }

        let htmlContent = `
            <div class="acta-search-results__meta">
                ${items.length} coincidencia(s) encontrada(s)${filtered.length > items.length ? ` de ${filtered.length} total` : ""}. Seleccione un equipo para autocompletar el acta.
            </div>
            <div class="acta-search-results__list">
                ${items.map((item) => `
                    <button type="button" class="acta-search-card" data-acta-sw-select="${item.id}">
                        <strong>${escapeHtml(item.codigoSbai || item.codigoMegan || `ID ${item.id}`)} · ${escapeHtml(displayInventoryType(item))}</strong>
                        <span>${escapeHtml([item.marca, item.modelo].filter(Boolean).join(" / ") || "Sin marca/modelo")} · SO: ${escapeHtml(item.sistemaOperativo || "-")}</span>
                        <span>Custodio: ${escapeHtml(item.custodio || "-")} · Área: ${escapeHtml(item.ubicacionDireccion || "-")} · Estado: ${escapeHtml(item.estado || "-")}</span>
                    </button>
                `).join("")}
            </div>
        `;
        
        // Agregar botón "Siguientes" solo si hay más resultados y hay filtros activos
        if (hasFilters && filtered.length > items.length) {
            htmlContent += `
                <div class="acta-search-results__pagination">
                    <button type="button" class="btn btn-secondary" id="actaSwLoadMoreButton">Cargar siguientes 30</button>
                </div>
            `;
        }
        
        results.innerHTML = htmlContent;
        results.querySelectorAll("[data-acta-sw-select]").forEach((btn) => {
            btn.addEventListener("click", () => selectActaSwEquipo(btn.dataset.actaSwSelect));
        });
        
        // Agregar listener al botón "Siguientes" si existe
        const loadMoreBtn = document.getElementById("actaSwLoadMoreButton");
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener("click", loadMoreActaSwResults);
        }
    }

    function loadMoreActaSwResults() {
        const results = document.getElementById("actaSwEquipoResultados");
        if (!results) return;
        
        const pageSize = 30;
        state.actaSwCurrentPage += 1;
        const start = (state.actaSwCurrentPage - 1) * pageSize;
        const end = start + pageSize;
        const newItems = state.actaSwLastFiltered.slice(start, end);
        
        if (!newItems.length) return;
        
        // Obtener la lista de resultados actual
        const list = results.querySelector(".acta-search-results__list");
        if (!list) return;
        
        // Agregar los nuevos elementos
        const newHtml = newItems.map((item) => `
            <button type="button" class="acta-search-card" data-acta-sw-select="${item.id}">
                <strong>${escapeHtml(item.codigoSbai || item.codigoMegan || `ID ${item.id}`)} · ${escapeHtml(displayInventoryType(item))}</strong>
                <span>${escapeHtml([item.marca, item.modelo].filter(Boolean).join(" / ") || "Sin marca/modelo")} · SO: ${escapeHtml(item.sistemaOperativo || "-")}</span>
                <span>Custodio: ${escapeHtml(item.custodio || "-")} · Área: ${escapeHtml(item.ubicacionDireccion || "-")} · Estado: ${escapeHtml(item.estado || "-")}</span>
            </button>
        `).join("");
        
        list.insertAdjacentHTML("beforeend", newHtml);
        
        // Agregar listeners a los nuevos elementos
        newItems.forEach((item) => {
            const btn = document.querySelector(`[data-acta-sw-select="${item.id}"]`);
            if (btn) btn.addEventListener("click", () => selectActaSwEquipo(item.id));
        });
        
        // Actualizar el meta info
        const meta = results.querySelector(".acta-search-results__meta");
        if (meta) {
            const currentCount = results.querySelectorAll("[data-acta-sw-select]").length;
            const total = state.actaSwLastFiltered.length;
            meta.textContent = `${currentCount} coincidencia(s) encontrada(s) de ${total} total. Seleccione un equipo para autocompletar el acta.`;
        }
        
        // Actualizar o remover el botón "Siguientes"
        const pagination = results.querySelector(".acta-search-results__pagination");
        if (pagination) {
            const currentCount = results.querySelectorAll("[data-acta-sw-select]").length;
            if (currentCount >= state.actaSwLastFiltered.length) {
                pagination.remove();
            } else {
                const btn = pagination.querySelector("button");
                if (btn) {
                    const remaining = state.actaSwLastFiltered.length - currentCount;
                    btn.textContent = `Cargar siguientes ${Math.min(30, remaining)}`;
                }
            }
        }
    }

    function addActaSwAdicionalRow() {
        const tbody = document.getElementById("actaSwAdicionalesTable");
        if (!tbody) return;
        
        // Obtener el siguiente índice disponible
        const rows = tbody.querySelectorAll("[data-acta-adicional-row]");
        const nextIndex = Math.max(...Array.from(rows).map(r => {
            const idx = r.getAttribute("data-acta-adicional-row");
            return idx ? parseInt(idx) : 0;
        })) + 1;
        
        // Crear nueva fila
        const newRow = document.createElement("tr");
        newRow.setAttribute("data-acta-adicional-row", nextIndex);
        newRow.innerHTML = `
            <td><input id="actaSwAdicional${nextIndex}" class="acta-input" placeholder="Descripción del software adicional"></td>
            <td class="acta-sw-radio">
                <label><input type="radio" name="adic${nextIndex}" value="SI"> SI</label>
                <label><input type="radio" name="adic${nextIndex}" value="NO"> NO</label>
            </td>
        `;
        
        // Agregar la fila al tbody
        tbody.appendChild(newRow);
    }

    function selectActaSwEquipo(id) {
        const item = (state.inventory || []).find((i) => String(i.id) === String(id));
        if (!item) return;
        document.querySelectorAll("[data-acta-sw-select]").forEach((c) => c.classList.remove("is-selected"));
        document.querySelector(`[data-acta-sw-select="${id}"]`)?.classList.add("is-selected");
        const selector = document.getElementById("actaSwSelector");
        if (selector) selector.value = id;
        autofillActaSwForm(item);
        loadActaSwInitialData();
        document.getElementById("actaSoftwareForm")?.classList.remove("hidden");
        const results = document.getElementById("actaSwEquipoResultados");
        if (results) {
            results.classList.add("hidden");
            results.innerHTML = "";
        }
        showToast("Equipo seleccionado", "El acta se autocompletó con los datos del inventario.", "success");
    }

    // RC simplificado: búsqueda solo por custodio, formulario manual

    function autofillActaRcForm(item) {
        setInputValue("actaRcFuncionarioNombre", item?.nombre || item?.custodio || "");
        setInputValue("actaRcFuncionarioCargo", item?.cargo || "");
        setInputValue("actaRcFuncionarioExtension", item?.extension || "");
        setInputValue("actaRcFuncionarioCorreo", item?.correo || "");
        setInputValue("actaRcFuncionarioArea", item?.area || item?.ubicacionDireccion || item?.ubicacion || "");
        setInputValue("actaRcFuncionarioEdificio", item?.edificio || item?.ubicacionEdificio || "");
        setInputValue("actaRcRecibeNombre", item?.nombre || item?.custodio || "");
    }

    function loadActaRcInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaRcEntregaFecha", "actaRcRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) {
                input.value = today;
            }
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaRcEntregaNombre");
            if (entrega && !entrega.value) {
                entrega.value = state.session.displayName;
            }
        }
    }

    function collectActaRcSearchCriteria() {
        return {
            custodio: document.getElementById("actaRcFilterCustodio")?.value.trim() || ""
        };
    }

    function normalizeText(value) {
        return String(value || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9\s]/g, " ").replace(/\s+/g, " ").trim();
    }

    function matchesActaRcCriteria(item, criteria) {
        if (criteria.custodio && !normalizeText(item?.custodio).includes(normalizeText(criteria.custodio))) return false;
        return true;
    }

    function renderActaRcSearchResults(items) {
        const container = document.getElementById("actaRcEquipoResultados");
        if (!container) return;
        const form = document.getElementById("actaRcForm");
        const preview = document.getElementById("actaRcPreviewActions");
        if (preview) preview.classList.add("hidden");
        if (form) form.classList.add("hidden");
        if (!items.length) {
            container.classList.remove("hidden");
            container.innerHTML = '<div class="empty-state">No se encontraron coincidencias para los filtros indicados.</div>';
            return;
        }
        container.classList.remove("hidden");
        container.innerHTML = `
            <div class="acta-search-results__meta">${items.length} coincidencia(s). Seleccione un custodio para autocompletar el formulario.</div>
            <div class="acta-search-results__list">
                ${items.map((item) => {
                    return `
                        <button type="button" class="acta-search-card" data-acta-rc-select="${item.id}">
                            <strong>${escapeHtml(item.custodio || "Sin nombre")} · ${escapeHtml(item.cargo || "Sin cargo")}</strong>
                            <span>Extensión: ${escapeHtml(item.extension || "-")} · Email: ${escapeHtml(item.correo || "-")}</span>
                            <span>Área: ${escapeHtml(item.ubicacion || item.ubicacionDireccion || "-")} · Edificio: ${escapeHtml(item.ubicacionEdificio || "-")}</span>
                        </button>
                    `;
                }).join("")}
            </div>
        `;
        container.querySelectorAll("[data-acta-rc-select]").forEach((button) => {
            button.addEventListener("click", () => selectActaRcItem(button.dataset.actaRcSelect));
        });
    }

    async function runActaRcSearch() {
        try {
            const input = document.getElementById("actaRcFilterCustodio");
            const dropdown = document.getElementById("actaRcAutocompleteDropdown");
            if (!input || !dropdown) return;
            const term = input.value.trim();
            if (term.length < 2) {
                dropdown.innerHTML = "";
                dropdown.classList.add("hidden");
                return;
            }
            const custodios = await loadCustodios(term, 100);
            if (!custodios.length) {
                dropdown.innerHTML = '<div style="padding: 8px; color: #999; font-size: 13px;">No se encontraron custodios</div>';
                dropdown.classList.remove("hidden");
                return;
            }
            dropdown.innerHTML = custodios.map((c) => `
                <div class="autocomplete-item" data-id="${c.id}" style="padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee; font-size: 13px; color: #333;" onmouseover="this.style.background='#f0f4f9'" onmouseout="this.style.background='white'">
                    <strong>${escapeHtml(c.nombre || "Sin nombre")}</strong> <span style="color: #666;">(${escapeHtml(c.cargo || "Sin cargo")})</span>
                </div>
            `).join("");
            dropdown.classList.remove("hidden");
            dropdown.querySelectorAll(".autocomplete-item").forEach((item) => {
                item.addEventListener("click", () => {
                    const cid = item.dataset.id;
                    const selected = custodios.find((row) => String(row.id) === String(cid));
                    if (selected) {
                        input.value = selected.nombre || "";
                        dropdown.classList.add("hidden");
                        selectActaRcItemByCustodio(selected);
                    }
                });
            });
        } catch (error) {
            showToast("Error en búsqueda", error.message || "No se pudieron cargar los custodios.", "danger");
        }
    }

    function selectActaRcItemByCustodio(item) {
        if (!item) return;
        const selector = document.getElementById("actaRcSelector");
        if (selector) selector.value = item.id;
        autofillActaRcForm(item);
        loadActaRcInitialData();
        document.getElementById("actaRcForm")?.classList.remove("hidden");
        document.getElementById("actaRcPreviewActions")?.classList.remove("hidden");
        const hint = document.getElementById("actaRcEquipmentHint");
        if (hint) {
            hint.textContent = `Custodio seleccionado: ${item.nombre || "RC"}.`;
        }
        showToast("Custodio seleccionado", "El formulario se autocompletó con datos del custodio.", "success");
    }

    function selectActaRcItem(id) {
        // Obsoleto pero conservado por compatibilidad
        const item = (state.inventory || []).find((row) => String(row.id) === String(id));
        if (item) selectActaRcItemByCustodio(item);
    }

    function resetActaRcForm() {
        document.getElementById("actaRcForm")?.reset();
        if (document.getElementById("actaRcSelector")) {
            document.getElementById("actaRcSelector").value = "";
        }
        const tableBody = document.getElementById("actaRcEquiposTableBody");
        if (tableBody) tableBody.innerHTML = "";
        const activitiesContainer = document.getElementById("actaRcDynamicActivitiesContainer");
        if (activitiesContainer) activitiesContainer.innerHTML = "";

        loadActaRcInitialData();
        document.getElementById("actaRcForm")?.classList.add("hidden");
        document.getElementById("actaRcPreviewActions")?.classList.add("hidden");
        document.getElementById("actaRcEquipmentHint")?.classList.remove("hidden");
        document.getElementById("actaRcIniciarButton")?.classList.remove("hidden");
    }

    function addActaRcEquipmentType(tipoName) {
        if (!tipoName) return;
        const normalizedTipo = tipoName.trim().toUpperCase();
        if (!normalizedTipo) return;

        const tableBody = document.getElementById("actaRcEquiposTableBody");
        if (!tableBody) return;

        const existingRow = Array.from(tableBody.querySelectorAll("tr")).find(row => {
            const val = row.querySelector(".acta-rc-tipo-input")?.value || "";
            return val.toUpperCase() === normalizedTipo;
        });
        if (existingRow) {
            showToast("Tipo existente", "Ya se ha agregado este tipo de equipo.", "warning");
            return;
        }

        const slug = "rc_" + normalizedTipo.replace(/[^A-Z0-9]/gi, "_").toLowerCase() + "_" + Date.now();

        const tr = document.createElement("tr");
        tr.id = `row_eq_${slug}`;
        tr.innerHTML = `
            <td><input class="acta-input acta-rc-tipo-input" value="${escapeHtml(normalizedTipo)}" readonly tabindex="-1" style="font-weight: bold; background: #f5f5f5;"></td>
            <td><input class="acta-input acta-rc-marca-input" placeholder="Marca"></td>
            <td><input class="acta-input acta-rc-modelo-input" placeholder="Modelo"></td>
            <td><input class="acta-input acta-rc-serial-input" placeholder="Serial"></td>
            <td><input class="acta-input acta-rc-codigo-input" placeholder="Código"></td>
            <td style="text-align: center;">
                <button type="button" class="btn btn-danger btn-sm" style="padding: 4px 8px; font-size: 11px; border-radius: 6px;">Eliminar</button>
            </td>
        `;
        tr.querySelector(".btn-danger").addEventListener("click", () => {
            tr.remove();
            document.getElementById(`sec_act_${slug}`)?.remove();
        });
        tableBody.appendChild(tr);

        const activitiesContainer = document.getElementById("actaRcDynamicActivitiesContainer");
        if (activitiesContainer) {
            const sec = document.createElement("div");
            sec.id = `sec_act_${slug}`;
            sec.className = "acta-rc-activities-section";
            sec.style.marginBottom = "24px";
            sec.innerHTML = `
                <table class="acta-table acta-table--actividades">
                    <thead>
                        <tr><th colspan="5" class="acta-table__title-dark" style="background: #2e4057; color: white;">ACTIVIDADES DE ${escapeHtml(normalizedTipo)}</th></tr>
                        <tr>
                            <th>Actividad</th>
                            <th>Fecha</th>
                            <th>Estado</th>
                            <th>Observación</th>
                            <th style="width: 80px; text-align: center;">Acción</th>
                        </tr>
                    </thead>
                    <tbody class="acta-rc-act-body" data-tipo="${escapeHtml(normalizedTipo)}">
                    </tbody>
                </table>
                <div style="margin-top: 8px; text-align: left;">
                    <button type="button" class="btn btn-secondary btn-sm btn-agregar-actividad" style="background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: bold; border-radius: 6px; padding: 4px 10px; cursor: pointer;">+ Agregar Actividad</button>
                </div>
            `;
            sec.querySelector(".btn-agregar-actividad").addEventListener("click", () => {
                const actBody = sec.querySelector(".acta-rc-act-body");
                addActaRcActivityRow(actBody);
            });
            activitiesContainer.appendChild(sec);
        }
    }

    function addActaRcActivityRow(tbody) {
        if (!tbody) return;
        const tr = document.createElement("tr");
        const today = new Date().toISOString().slice(0, 10);
        tr.innerHTML = `
            <td><input class="acta-input acta-rc-act-nombre" placeholder="Nombre de la actividad" required></td>
            <td><input class="acta-input acta-rc-act-fecha" type="date" value="${today}" required></td>
            <td><input class="acta-input acta-rc-act-estado" placeholder="Estado" required></td>
            <td><textarea class="acta-input acta-input--textarea acta-rc-act-obs" rows="2" placeholder="Observaciones"></textarea></td>
            <td style="text-align: center; vertical-align: middle;">
                <button type="button" class="btn btn-danger btn-sm" style="padding: 4px 8px; font-size: 11px; border-radius: 6px;">Eliminar</button>
            </td>
        `;
        tr.querySelector(".btn-danger").addEventListener("click", () => {
            tr.remove();
        });
        tbody.appendChild(tr);
    }

    function buildActaRcPayload() {
        const payload = {
            equipoSeleccionado: document.getElementById("actaRcSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaRcFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaRcFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaRcFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaRcFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaRcFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaRcFuncionarioEdificio")?.value.trim() || ""
            },
            equiposManuales: [],
            actividades: [],
            certificacion: ACTA_RC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaRcEntregaNombre", "actaRcEntregaNombreOtro"),
                firma: document.getElementById("actaRcEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaRcEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaRcRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaRcRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaRcRecibeFecha")?.value || ""
            }
        };

        document.querySelectorAll("#actaRcEquiposTableBody tr").forEach(row => {
            const tipo = row.querySelector(".acta-rc-tipo-input")?.value || "";
            const marca = row.querySelector(".acta-rc-marca-input")?.value.trim() || "";
            const modelo = row.querySelector(".acta-rc-modelo-input")?.value.trim() || "";
            const serial = row.querySelector(".acta-rc-serial-input")?.value.trim() || "";
            const codigo = row.querySelector(".acta-rc-codigo-input")?.value.trim() || "";
            if (tipo) {
                payload.equiposManuales.push({ tipo, marca, modelo, serial, codigo });
            }
        });

        document.querySelectorAll(".acta-rc-act-body tr").forEach(row => {
            const tbody = row.closest(".acta-rc-act-body");
            const tipoEquipo = tbody ? (tbody.dataset.tipo || "") : "";
            const actividad = row.querySelector(".acta-rc-act-nombre")?.value.trim() || "";
            const fecha = row.querySelector(".acta-rc-act-fecha")?.value || "";
            const estado = row.querySelector(".acta-rc-act-estado")?.value.trim() || "";
            const observacion = row.querySelector(".acta-rc-act-obs")?.value.trim() || "";
            if (actividad || fecha || estado || observacion) {
                payload.actividades.push({ tipoEquipo, actividad, fecha, estado, observacion });
            }
        });

        return payload;
    }

    function renderActaRcPreview(payload) {
        const funcionario = payload.funcionario || {};

        let equiposHtml = "";
        if (payload.equiposManuales && payload.equiposManuales.length > 0) {
            equiposHtml = `
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>Tipo</th><th>Marca</th><th>Modelo</th><th>Serial</th><th>Código</th></tr>
                    ${payload.equiposManuales.map(eq => `
                        <tr>
                            <td>${escapeHtml(eq.tipo)}</td>
                            <td>${escapeHtml(eq.marca || "")}</td>
                            <td>${escapeHtml(eq.modelo || "")}</td>
                            <td>${escapeHtml(eq.serial || "")}</td>
                            <td>${escapeHtml(eq.codigo || "")}</td>
                        </tr>
                    `).join("")}
                </table>
            `;
        }

        let actividadesHtml = "";
        if (payload.equiposManuales && payload.equiposManuales.length > 0) {
            payload.equiposManuales.forEach(eq => {
                const match = (payload.actividades || []).filter(a => a.tipoEquipo === eq.tipo);
                if (match.length > 0) {
                    actividadesHtml += `
                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark" style="background: #2e4057; color: white;">ACTIVIDADES DE ${escapeHtml(eq.tipo)}</th></tr>
                            <tr>
                                <th>Actividad</th>
                                <th>Fecha</th>
                                <th>Estado</th>
                                <th>Observación</th>
                            </tr>
                            ${match.map(act => `
                                <tr>
                                    <td class="acta-table__activity">${escapeHtml(act.actividad)}</td>
                                    <td>${escapeHtml(act.fecha || "")}</td>
                                    <td>${escapeHtml(act.estado || "")}</td>
                                    <td class="acta-preview__observation">${escapeHtml(act.observacion || "")}</td>
                                </tr>
                            `).join("")}
                        </table>
                    `;
                }
            });
        }

        return `
            <div class="acta-sheet acta-sheet--preview" style="width:794px; min-height:1123px; max-width:100%; margin:0 auto; box-sizing:border-box; display:flex; flex-direction:column;">
                <div class="acta-sheet__header">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS DE REDES Y COMUNICACIONES</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td><td>${escapeHtml(funcionario.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td><td>${escapeHtml(funcionario.cargo || "")}</td>
                        <td class="acta-table__label">N° EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td><td>${escapeHtml(funcionario.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td><td>${escapeHtml(funcionario.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                    </tr>
                </table>
                ${equiposHtml}
                ${actividadesHtml}
                <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_RC_CERTIFICATION_TEXT)}</p>
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer" style="margin-top:auto;">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +539-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>
                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                </div>
            </div>
        `;
    }

    function openActaRcPreview() {
        const form = document.getElementById("actaRcForm");
        if (!form) return;
        if (!form.reportValidity()) {
            return;
        }
        const payload = buildActaRcPayload();
        openModal(
            "Previsualización del Acta de Redes y Comunicaciones",
            `<div class="acta-preview">${renderActaRcPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaRc("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaRc("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaRcPdfAuto(payload) {
        let container = null;
        try {
            showToast("Generando PDF", "Preparando el documento...", "info");

            await loadScriptOnce("https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js");
            await loadScriptOnce("https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js");

            const previewHtml = renderActaRcPreview(payload);

            container = document.createElement("div");
            container.className = "acta-preview";
            container.style.cssText = [
                "position:fixed", "top:0", "left:0",
                "width:800px", "min-height:200px",
                "background:#fff", "padding:24px 28px",
                "box-sizing:border-box",
                "z-index:2147483647",
                "overflow:visible"
            ].join(";");
            container.innerHTML = previewHtml;
            document.body.appendChild(container);

            const imgs = Array.from(container.querySelectorAll("img"));
            await Promise.all(imgs.map((img) => new Promise((resolve) => {
                if (img.complete && img.naturalWidth > 0) { resolve(); return; }
                img.addEventListener("load",  resolve, { once: true });
                img.addEventListener("error", resolve, { once: true });
            })));

            await new Promise((r) => setTimeout(r, 400));

            const w = container.offsetWidth;
            const h = container.offsetHeight;
            if (!w || !h) throw new Error(`Contenedor sin dimensiones (${w}x${h}). El DOM no renderizó el contenido.`);

            const canvas = await window.html2canvas(container, {
                scale:           2,
                useCORS:         true,
                allowTaint:      true,
                backgroundColor: "#ffffff",
                logging:         false,
                width:           w,
                height:          h,
                scrollX:         0,
                scrollY:         0
            });

            if (canvas.width === 0 || canvas.height === 0) {
                throw new Error("html2canvas devolvió un canvas vacío.");
            }

            const { jsPDF } = window.jspdf;
            const pdf      = new jsPDF({ unit: "mm", format: "a4", orientation: "portrait" });
            const pageW    = pdf.internal.pageSize.getWidth();
            const pageH    = pdf.internal.pageSize.getHeight();
            const margin   = 8;
            const contentW = pageW - margin * 2;
            const pxPerMm  = canvas.width / contentW;
            const pageHpx  = (pageH - margin * 2) * pxPerMm;

            let srcY = 0;
            while (srcY < canvas.height) {
                if (srcY > 0) pdf.addPage();
                const srcH     = Math.min(pageHpx, canvas.height - srcY);
                const slice    = document.createElement("canvas");
                slice.width    = canvas.width;
                slice.height   = srcH;
                slice.getContext("2d").drawImage(canvas, 0, srcY, canvas.width, srcH, 0, 0, canvas.width, srcH);
                const sliceH   = srcH / pxPerMm;
                pdf.addImage(slice.toDataURL("image/jpeg", 0.97), "JPEG", margin, margin, contentW, sliceH);
                srcY += srcH;
            }

            pdf.save("acta_redes_comunicaciones.pdf");
            showToast("Exportación lista", "PDF generado correctamente.", "success");

        } catch (err) {
            console.error("[exportActaRcPdfAuto]", err);
            showToast("Error", err.message || "No se pudo generar el PDF.", "danger");
        } finally {
            if (container?.parentNode) container.parentNode.removeChild(container);
        }
    }

    async function exportActaRc(format) {
        try {
            const form = document.getElementById("actaRcForm");
            if (!form) return;
            if (!form.reportValidity()) {
                return;
            }
            const payload = buildActaRcPayload();

            if (format === "pdf") {
                await exportActaRcPdfAuto(payload);
                return;
            }

            const response = await apiFetch(`/actas/rc/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore fallback parsing errors
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_redes_comunicaciones.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function renderActaRedesComunicacionesPage() {
        return `
            <section class="panel panel--narrow acta-form-panel">
                <div class="inventory-header inventory-header--form">
                    <div>
                        <div class="eyebrow">Mantenimiento preventivo</div>
                        <h2>Actas de Redes y Comunicaciones</h2>
                        <p>Formulario institucional basado en el mantenimiento preventivo oficial para redes y comunicaciones.</p>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
                        <div class="inventory-header__badge">
                            <span>${iconMarkup("clipboard")}</span>
                            <strong>ACTAS</strong>
                        </div>
                        <a class="btn btn-secondary" href="${actasBasePath()}/actas.html" style="width: 100%; text-align: center; display: inline-flex; justify-content: center; align-items: center; border-radius: 12px; font-size: 13px; padding: 10px 16px; background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; text-decoration: none; box-shadow: 0 2px 4px rgba(21, 101, 192, 0.1); cursor: pointer; transition: all 0.2s;">Regresar a Actas</a>
                    </div>
                </div>
                <div class="acta-search-panel" style="text-align: center; padding: 24px;">
                    <div style="margin-bottom: 16px;">
                        <p>Haga clic en el siguiente botón para iniciar el llenado del acta de Redes y Comunicaciones de manera manual.</p>
                    </div>
                    <button type="button" class="btn btn-primary" id="actaRcIniciarButton">Iniciar Acta de Redes y Comunicaciones</button>
                </div>
                <div id="actaRcPreviewActions" class="toolbar hidden" style="margin-bottom:16px;">
                    <button type="button" class="btn btn-primary" id="actaRcPreviewButton">Vista previa</button>
                    <button type="button" class="btn btn-secondary" id="actaRcExportDocxButton">Exportar DOCX</button>
                    <button type="button" class="btn btn-secondary" id="actaRcExportPdfButton">Exportar PDF</button>
                    <button type="button" class="btn btn-secondary" id="actaRcResetButton">Limpiar</button>
                </div>
                <div id="actaRcEquipmentHint" class="helper-banner">Pulse el botón de arriba para iniciar el llenado del acta.</div>
                <form id="actaRcForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaRcSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS DE REDES Y COMUNICACIONES</h2>
                        </div>
                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaRcFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaRcFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaRcFuncionarioExtension" class="acta-input" placeholder="N° EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaRcFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaRcFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaRcFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--equipos">
                            <thead>
                                <tr><th colspan="6">EQUIPOS</th></tr>
                                <tr>
                                    <th>Tipo</th>
                                    <th>Marca</th>
                                    <th>Modelo</th>
                                    <th>Serial</th>
                                    <th>Código</th>
                                    <th style="width: 80px; text-align: center;">Acción</th>
                                </tr>
                            </thead>
                            <tbody id="actaRcEquiposTableBody">
                            </tbody>
                        </table>
                        <div style="margin: 12px 0 24px 0; text-align: left;">
                            <button type="button" class="btn btn-secondary" id="actaRcAgregarTipoBtn" style="background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; border-radius: 8px; padding: 6px 14px; cursor: pointer; transition: all 0.2s;">+ Agregar Tipo</button>
                        </div>
                        <div id="actaRcDynamicActivitiesContainer"></div>
                        <p class="acta-certification">${ACTA_RC_CERTIFICATION_TEXT}</p>
                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th></tr>
                            <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <select id="actaRcEntregaNombre" class="acta-input" required>
                                            <option value="">-- Seleccionar --</option>
                                            <option value="MARÍA FERNANDA PILCO CARRIÓN">MARÍA FERNANDA PILCO CARRIÓN</option>
                                            <option value="MIGUEL CABRERA">MIGUEL CABRERA</option>
                                            <option value="MARCO M. MOLINA MENDIETA">MARCO M. MOLINA MENDIETA</option>
                                            <option value="OTRO">OTRO</option>
                                        </select>
                                        <input id="actaRcEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <input id="actaRcRecibeNombre" class="acta-input" required>
                                    </label>
                                </td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaRcEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaRcRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaRcEntregaFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaRcRecibeFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                            </tr>
                        </table>
                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
            </section>
        `;
    }

    function renderActaEquiposPage() {
        return `
            <section class="panel panel--narrow acta-form-panel">
                <div class="inventory-header inventory-header--form">
                    <div>
                        <div class="eyebrow">Mantenimiento preventivo</div>
                        <h2>Formulario de equipos</h2>
                        <p>El subapartado PC replica el formato oficial del acta institucional y reutiliza datos existentes del inventario para autocompletar el documento.</p>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 8px; align-items: flex-end;">
                        <div class="inventory-header__badge">
                            <span>${iconMarkup("clipboard")}</span>
                            <strong>ACTAS</strong>
                        </div>
                        <a class="btn btn-secondary" href="${actasBasePath()}/actas.html" style="width: 100%; text-align: center; display: inline-flex; justify-content: center; align-items: center; border-radius: 12px; font-size: 13px; padding: 10px 16px; background: #eef4fc; border: 1px solid #1565c0; color: #1565c0; font-weight: 700; text-decoration: none; box-shadow: 0 2px 4px rgba(21, 101, 192, 0.1); cursor: pointer; transition: all 0.2s;">Regresar a Actas</a>
                    </div>
                </div>

                <div class="acta-subtabs" aria-label="Subapartados de mantenimiento preventivo de equipos">
                    ${ACTA_PC_SUBSECTIONS.map((item) => `
                        <button type="button" class="acta-subtab${item.key === "pc" ? " is-active" : ""}" ${item.key === "pc" || item.key === "laptop" ? `data-acta-type="${item.key}"` : "disabled"}>${item.label}</button>
                    `).join("")}
                </div>

                <div class="acta-search-panel">
                    <div>
                        <div class="eyebrow">Búsqueda inicial</div>
                        <h3>Seleccione el equipo para generar el acta</h3>
                        <p>Busque por custodio, Código SBYE, Código Megan, marca, modelo, serie, edificio o estado.</p>
                    </div>
                    <div class="acta-search-panel__grid">
                        <div class="field-group">
                            <label for="actaEquipoTipoBusqueda">Tipo</label>
                            <select id="actaEquipoTipoBusqueda">
                                <option value="pc">PC</option>
                                <option value="laptop">Laptop</option>
                            </select>
                        </div>
                        <div class="field-group acta-search-panel__query">
                            <label for="actaEquipoBusqueda">Campo de búsqueda</label>
                            <input id="actaEquipoBusqueda" type="search" placeholder="Ej. custodio, SBYE, marca, serie...">
                        </div>
                        <button type="button" class="btn btn-primary" id="actaEquipoBuscarButton">Buscar</button>
                    </div>
                    <div id="actaEquipoResultados" class="acta-search-results hidden"></div>
                </div>
                <form id="actaPcForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaPcSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>

                        <div class="acta-sheet__titles">
                            <h3>${"SERVICIO NACIONAL DE DERECHOS INTELECTUALES"}</h3>
                            <h4>${"DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN"}</h4>
                            <h2>${"FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS"}</h2>
                        </div>

                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaFuncionarioNombre" name="funcionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaFuncionarioCargo" name="funcionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaFuncionarioExtension" name="funcionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaFuncionarioCorreo" name="funcionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaFuncionarioArea" name="funcionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaFuncionarioEdificio" name="funcionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>

                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th>
                                <th>MARCA</th>
                                <th>MODELO</th>
                                <th>SERIAL</th>
                                <th>CDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaDesktopTipo" class="acta-input" value="PC"></td>
                                <td><input id="actaDesktopMarca" class="acta-input"></td>
                                <td><input id="actaDesktopModelo" class="acta-input"></td>
                                <td><input id="actaDesktopSerial" class="acta-input"></td>
                                <td><input id="actaDesktopCodigo" class="acta-input" required></td>
                            </tr>
                            <tr>
                                <td><input id="actaLaptopTipo" class="acta-input" value="LAPTOP"></td>
                                <td><input id="actaLaptopMarca" class="acta-input"></td>
                                <td><input id="actaLaptopModelo" class="acta-input"></td>
                                <td><input id="actaLaptopSerial" class="acta-input"></td>
                                <td><input id="actaLaptopCodigo" class="acta-input"></td>
                            </tr>
                        </table>

                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark">COMPUTADORA</th></tr>
                            <tr>
                                <th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS</th>
                                <th colspan="3">INSTALADO</th>
                            </tr>
                            <tr>
                                <th>FECHA <button type="button" class="acta-borrar-estados" data-borrar-fechas="actaActividadFecha" title="Borra todas las fechas de esta tabla">Borrar todo</button></th>
                                <th>ESTADO <button type="button" class="acta-borrar-estados" data-borrar-estados="actaActividadEstado" title="Borra todos los estados de esta tabla">Borrar todo</button></th>
                                <th>OBSERVACIÓN</th>
                            </tr>
                            ${ACTA_PC_ACTIVITY_ROWS.map((activity, index) => `
                                <tr class="${index === 3 ? "acta-table__row--tall" : ""}">
                                    <td class="acta-table__activity">${activity}</td>
                                    <td><input id="actaActividadFecha${index}" class="acta-input" type="date"></td>
                                    <td><input id="actaActividadEstado${index}" class="acta-input"></td>
                                    <td><textarea id="actaActividadObservacion${index}" class="acta-input acta-input--textarea" rows="${index === 3 ? 4 : 2}"></textarea></td>
                                </tr>
                            `).join("")}
                        </table>

                        <p class="acta-certification">${ACTA_PC_CERTIFICATION_TEXT}</p>

                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th></tr>
                            <tr>
                                <th>ENTREGA</th>
                                <th>RECIBE</th>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <select id="actaEntregaNombre" class="acta-input" required>
                                            <option value="">-- Seleccionar --</option>
                                            <option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option>
                                            <option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option>
                                            <option value="OTRO">OTRO</option>
                                        </select>
                                        <input id="actaEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <input id="actaRecibeNombre" class="acta-input" required>
                                    </label>
                                </td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaEntregaFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaRecibeFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                            </tr>
                        </table>

                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
                <form id="actaImpresoraForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaImpresoraSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                        </div>
                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaImpresoraFuncionarioNombre" name="impresoraFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaImpresoraFuncionarioCargo" name="impresoraFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaImpresoraFuncionarioExtension" name="impresoraFuncionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaImpresoraFuncionarioCorreo" name="impresoraFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaImpresoraFuncionarioArea" name="impresoraFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaImpresoraFuncionarioEdificio" name="impresoraFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th>
                                <th>MARCA</th>
                                <th>MODELO</th>
                                <th>SERIAL</th>
                                <th>CÓDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaImpresoraEquipoTipo" class="acta-input" value="IMPRESORA"></td>
                                <td><input id="actaImpresoraEquipoMarca" class="acta-input"></td>
                                <td><input id="actaImpresoraEquipoModelo" class="acta-input"></td>
                                <td><input id="actaImpresoraEquipoSerial" class="acta-input"></td>
                                <td><input id="actaImpresoraEquipoCodigo" class="acta-input" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark">IMPRESORA</th></tr>
                            <tr>
                                <th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th>
                                <th colspan="3">INSTALADO</th>
                            </tr>
                            <tr>
                                <th>FECHA <button type="button" class="acta-borrar-estados" data-borrar-fechas="actaImpresoraActividadFecha" title="Borra todas las fechas de esta tabla">Borrar todo</button></th>
                                <th>ESTADO <button type="button" class="acta-borrar-estados" data-borrar-estados="actaImpresoraActividadEstado" title="Borra todos los estados de esta tabla">Borrar todo</button></th>
                                <th>OBSERVACIÓN</th>
                            </tr>
                            ${ACTA_IMPRESORA_ACTIVITY_ROWS.map((activity, index) => `
                                <tr>
                                    <td class="acta-table__activity">${activity}</td>
                                    <td><input id="actaImpresoraActividadFecha${index}" class="acta-input" type="date"></td>
                                    <td><input id="actaImpresoraActividadEstado${index}" class="acta-input"></td>
                                    <td><textarea id="actaImpresoraActividadObservacion${index}" class="acta-input acta-input--textarea" rows="2"></textarea></td>
                                </tr>
                            `).join("")}
                        </table>
                        <p class="acta-certification">${ACTA_PC_CERTIFICATION_TEXT}</p>
                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                            <tr>
                                <th>ENTREGA</th>
                                <th>RECIBE</th>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <select id="actaImpresoraEntregaNombre" class="acta-input" required>
                                            <option value="">-- Seleccionar --</option>
                                            <option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option>
                                            <option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option>
                                            <option value="OTRO">OTRO</option>
                                        </select>
                                        <input id="actaImpresoraEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <input id="actaImpresoraRecibeNombre" class="acta-input" required>
                                    </label>
                                </td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaImpresoraEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaImpresoraRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaImpresoraEntregaFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaImpresoraRecibeFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                            </tr>
                        </table>
                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
                <form id="actaEscanerForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaEscanerSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                        </div>
                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaEscanerFuncionarioNombre" name="escanerFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaEscanerFuncionarioCargo" name="escanerFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaEscanerFuncionarioExtension" name="escanerFuncionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaEscanerFuncionarioCorreo" name="escanerFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaEscanerFuncionarioArea" name="escanerFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaEscanerFuncionarioEdificio" name="escanerFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaEscanerEquipoTipo" class="acta-input" value="ESCÁNER"></td>
                                <td><input id="actaEscanerEquipoMarca" class="acta-input"></td>
                                <td><input id="actaEscanerEquipoModelo" class="acta-input"></td>
                                <td><input id="actaEscanerEquipoSerial" class="acta-input"></td>
                                <td><input id="actaEscanerEquipoCodigo" class="acta-input" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark">ESCÁNER</th></tr>
                            <tr>
                                <th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th>
                                <th colspan="3">INSTALADO</th>
                            </tr>
                            <tr><th>FECHA <button type="button" class="acta-borrar-estados" data-borrar-fechas="actaEscanerActividadFecha" title="Borra todas las fechas de esta tabla">Borrar todo</button></th><th>ESTADO <button type="button" class="acta-borrar-estados" data-borrar-estados="actaEscanerActividadEstado" title="Borra todos los estados de esta tabla">Borrar todo</button></th><th>OBSERVACIÓN</th></tr>
                            ${ACTA_ESCANER_ACTIVITY_ROWS.map((activity, index) => `
                                <tr>
                                    <td class="acta-table__activity">${activity}</td>
                                    <td><input id="actaEscanerActividadFecha${index}" class="acta-input" type="date"></td>
                                    <td><input id="actaEscanerActividadEstado${index}" class="acta-input"></td>
                                    <td><textarea id="actaEscanerActividadObservacion${index}" class="acta-input acta-input--textarea" rows="2"></textarea></td>
                                </tr>
                            `).join("")}
                        </table>
                        <p class="acta-certification">${ACTA_PC_CERTIFICATION_TEXT}</p>
                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                            <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                            <tr>
                                <td><label class="acta-signature-field">Nombre:<select id="actaEscanerEntregaNombre" class="acta-input" required><option value="">-- Seleccionar --</option><option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option><option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option><option value="OTRO">OTRO</option></select><input id="actaEscanerEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;"></label></td>
                                <td><label class="acta-signature-field">Nombre:<input id="actaEscanerRecibeNombre" class="acta-input" required></label></td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td><label class="acta-signature-field">Firma:<input id="actaEscanerEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica"></label></td>
                                <td><label class="acta-signature-field">Firma:<input id="actaEscanerRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica"></label></td>
                            </tr>
                            <tr>
                                <td><label class="acta-signature-field">Fecha:<input id="actaEscanerEntregaFecha" class="acta-input" type="date" required></label></td>
                                <td><label class="acta-signature-field">Fecha:<input id="actaEscanerRecibeFecha" class="acta-input" type="date" required></label></td>
                            </tr>
                        </table>
                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
                <form id="actaTelefonoForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaTelefonoSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                        </div>
                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaTelefonoFuncionarioNombre" name="telefonoFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaTelefonoFuncionarioCargo" name="telefonoFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaTelefonoFuncionarioExtension" name="telefonoFuncionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaTelefonoFuncionarioCorreo" name="telefonoFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaTelefonoFuncionarioArea" name="telefonoFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaTelefonoFuncionarioEdificio" name="telefonoFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaTelefonoEquipoTipo" class="acta-input" value="TELÉFONO IP"></td>
                                <td><input id="actaTelefonoEquipoMarca" class="acta-input"></td>
                                <td><input id="actaTelefonoEquipoModelo" class="acta-input"></td>
                                <td><input id="actaTelefonoEquipoSerial" class="acta-input"></td>
                                <td><input id="actaTelefonoEquipoCodigo" class="acta-input" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark">TELÉFONO IP</th></tr>
                            <tr>
                                <th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th>
                                <th colspan="3">INSTALADO</th>
                            </tr>
                            <tr><th>FECHA <button type="button" class="acta-borrar-estados" data-borrar-fechas="actaTelefonoActividadFecha" title="Borra todas las fechas de esta tabla">Borrar todo</button></th><th>ESTADO <button type="button" class="acta-borrar-estados" data-borrar-estados="actaTelefonoActividadEstado" title="Borra todos los estados de esta tabla">Borrar todo</button></th><th>OBSERVACIÓN</th></tr>
                            ${ACTA_TELEFONO_ACTIVITY_ROWS.map((activity, index) => `
                                <tr>
                                    <td class="acta-table__activity">${activity}</td>
                                    <td><input id="actaTelefonoActividadFecha${index}" class="acta-input" type="date"></td>
                                    <td><input id="actaTelefonoActividadEstado${index}" class="acta-input"></td>
                                    <td><textarea id="actaTelefonoActividadObservacion${index}" class="acta-input acta-input--textarea" rows="2"></textarea></td>
                                </tr>
                            `).join("")}
                        </table>
                        <p class="acta-certification">${ACTA_PC_CERTIFICATION_TEXT}</p>
                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                            <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                            <tr>
                                <td><label class="acta-signature-field">Nombre:<select id="actaTelefonoEntregaNombre" class="acta-input" required><option value="">-- Seleccionar --</option><option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option><option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option><option value="OTRO">OTRO</option></select><input id="actaTelefonoEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;"></label></td>
                                <td><label class="acta-signature-field">Nombre:<input id="actaTelefonoRecibeNombre" class="acta-input" required></label></td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td><label class="acta-signature-field">Firma:<input id="actaTelefonoEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica"></label></td>
                                <td><label class="acta-signature-field">Firma:<input id="actaTelefonoRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica"></label></td>
                            </tr>
                            <tr>
                                <td><label class="acta-signature-field">Fecha:<input id="actaTelefonoEntregaFecha" class="acta-input" type="date" required></label></td>
                                <td><label class="acta-signature-field">Fecha:<input id="actaTelefonoRecibeFecha" class="acta-input" type="date" required></label></td>
                            </tr>
                        </table>
                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
                <form id="actaProyectorForm" class="acta-pc-form hidden">
                    <input type="hidden" id="actaProyectorSelector">
                    <div class="acta-sheet">
                        <div class="acta-sheet__header">
                            <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                            <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                        </div>
                        <div class="acta-sheet__titles">
                            <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                            <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                            <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                        </div>
                        <table class="acta-table">
                            <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                            <tr>
                                <td class="acta-table__label">NOMBRE</td>
                                <td colspan="2"><input id="actaProyectorFuncionarioNombre" name="proyectorFuncionarioNombre" class="acta-input" required></td>
                                <td class="acta-table__label">CARGO</td>
                                <td><input id="actaProyectorFuncionarioCargo" name="proyectorFuncionarioCargo" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaProyectorFuncionarioExtension" name="proyectorFuncionarioExtension" class="acta-input" placeholder="Nº EXT."></td>
                            </tr>
                            <tr>
                                <td class="acta-table__label">CORREO</td>
                                <td colspan="2"><input id="actaProyectorFuncionarioCorreo" name="proyectorFuncionarioCorreo" class="acta-input" type="email" required></td>
                                <td class="acta-table__label">ÁREA</td>
                                <td><input id="actaProyectorFuncionarioArea" name="proyectorFuncionarioArea" class="acta-input" required></td>
                                <td class="acta-table__label-value"><input id="actaProyectorFuncionarioEdificio" name="proyectorFuncionarioEdificio" class="acta-input" placeholder="EDIFICIO" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--equipos">
                            <tr><th colspan="5">EQUIPOS</th></tr>
                            <tr>
                                <th>TIPO</th>
                                <th>MARCA</th>
                                <th>MODELO</th>
                                <th>SERIAL</th>
                                <th>CÓDIGO</th>
                            </tr>
                            <tr>
                                <td><input id="actaProyectorEquipoTipo" class="acta-input" value="PROYECTOR"></td>
                                <td><input id="actaProyectorEquipoMarca" class="acta-input"></td>
                                <td><input id="actaProyectorEquipoModelo" class="acta-input"></td>
                                <td><input id="actaProyectorEquipoSerial" class="acta-input"></td>
                                <td><input id="actaProyectorEquipoCodigo" class="acta-input" required></td>
                            </tr>
                        </table>
                        <table class="acta-table acta-table--actividades">
                            <tr><th colspan="4" class="acta-table__title-dark">PROYECTOR</th></tr>
                            <tr>
                                <th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th>
                                <th colspan="3">INSTALADO</th>
                            </tr>
                            <tr>
                                <th>FECHA <button type="button" class="acta-borrar-estados" data-borrar-fechas="actaProyectorActividadFecha" title="Borra todas las fechas de esta tabla">Borrar todo</button></th>
                                <th>ESTADO <button type="button" class="acta-borrar-estados" data-borrar-estados="actaProyectorActividadEstado" title="Borra todos los estados de esta tabla">Borrar todo</button></th>
                                <th>OBSERVACIÓN</th>
                            </tr>
                            ${ACTA_PROYECTOR_ACTIVITY_ROWS.map((activity, index) => `
                                <tr>
                                    <td class="acta-table__activity">${activity}</td>
                                    <td><input id="actaProyectorActividadFecha${index}" class="acta-input" type="date"></td>
                                    <td><input id="actaProyectorActividadEstado${index}" class="acta-input"></td>
                                    <td><textarea id="actaProyectorActividadObservacion${index}" class="acta-input acta-input--textarea" rows="2"></textarea></td>
                                </tr>
                            `).join("")}
                        </table>
                        <p class="acta-certification">${ACTA_PC_CERTIFICATION_TEXT}</p>
                        <table class="acta-table acta-table--firma">
                            <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                            <tr>
                                <th>ENTREGA</th>
                                <th>RECIBE</th>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <select id="actaProyectorEntregaNombre" class="acta-input" required>
                                            <option value="">-- Seleccionar --</option>
                                            <option value="EMERSON R. CERACAPA SOLIS">EMERSON R. CERACAPA SOLIS</option>
                                            <option value="PAUL FERNANDO OROZCO VINUEZA">PAUL FERNANDO OROZCO VINUEZA</option>
                                            <option value="OTRO">OTRO</option>
                                        </select>
                                        <input id="actaProyectorEntregaNombreOtro" class="acta-input hidden" placeholder="Escriba el nombre completo" style="margin-top: 8px;">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Nombre:
                                        <input id="actaProyectorRecibeNombre" class="acta-input" required>
                                    </label>
                                </td>
                            </tr>
                            <tr class="acta-table__row--firma">
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaProyectorEntregaFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Firma:
                                        <input id="actaProyectorRecibeFirma" class="acta-input acta-input--readonly" readonly tabindex="-1" placeholder="Firma electrónica">
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaProyectorEntregaFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                                <td>
                                    <label class="acta-signature-field">Fecha:
                                        <input id="actaProyectorRecibeFecha" class="acta-input" type="date" required>
                                    </label>
                                </td>
                            </tr>
                        </table>
                        <div class="acta-sheet__footer">
                            <div class="acta-sheet__footer-text">
                                <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                                <span>Código postal: 170518 / Quito — Ecuador</span>
                                <span>Teléfono: +539-2 394 0000</span>
                                <span>www.derechosintelectuales.gob.ec</span>
                            </div>
                            <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                        </div>
                    </div>
                </form>
                <div id="actaFormContainer" class="acta-toolbar hidden" style="margin-top:24px;">
                    <div class="acta-toolbar__actions" id="actaPcToolbarActions">
                        <button type="button" class="btn btn-primary" id="actaPcPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-secondary" id="actaPcExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaPcExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaPcResetButton">Limpiar</button>
                    </div>
                    <div class="acta-toolbar__actions hidden" id="actaImpresoraToolbarActions">
                        <button type="button" class="btn btn-primary" id="actaImpresoraPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-secondary" id="actaImpresoraExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaImpresoraExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaImpresoraResetButton">Limpiar</button>
                    </div>
                    <div class="acta-toolbar__actions hidden" id="actaEscanerToolbarActions">
                        <button type="button" class="btn btn-primary" id="actaEscanerPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-secondary" id="actaEscanerExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaEscanerExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaEscanerResetButton">Limpiar</button>
                    </div>
                    <div class="acta-toolbar__actions hidden" id="actaTelefonoToolbarActions">
                        <button type="button" class="btn btn-primary" id="actaTelefonoPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-secondary" id="actaTelefonoExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaTelefonoExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaTelefonoResetButton">Limpiar</button>
                    </div>
                    <div class="acta-toolbar__actions hidden" id="actaProyectorToolbarActions">
                        <button type="button" class="btn btn-primary" id="actaProyectorPreviewButton">Previsualizar</button>
                        <button type="button" class="btn btn-secondary" id="actaProyectorExportDocxButton">Exportar DOCX</button>
                        <button type="button" class="btn btn-secondary" id="actaProyectorExportPdfButton">Exportar PDF</button>
                        <button type="button" class="btn btn-secondary" id="actaProyectorResetButton">Limpiar</button>
                    </div>
                </div>
            </section>
        `;
    }
// Filtra los ítems del inventario para obtener solo aquellos que son de tipo PC o Laptop, dependiendo de la selección actual en el formulario del acta.
    function getActaPcItems() {
        return state.inventory.filter((item) => String(item.tipo || "").toLowerCase() === "pc");
    }
// Genera la etiqueta para las opciones de búsqueda del acta de equipos
    function actaEquipoOptionLabel(item) {
        return [
            item.codigoSbai || item.codigoMegan || `ID ${item.id}`,
            displayInventoryType(item),
            item.marca,
            item.modelo,
            item.custodio
        ].filter(Boolean).join(" · ");
    }

    function syncActaTypeTab(type) {
        document.querySelectorAll("[data-acta-type]").forEach((button) => {
            button.classList.toggle("is-active", button.dataset.actaType === type);
        });
    }
// Renderiza los resultados de búsqueda para el acta de equipos
    function renderActaEquipoResults(items, totalCount = items.length) {
        const container = document.getElementById("actaEquipoResultados");
        if (!container) {
            return;
        }
        const formContainer = document.getElementById("actaFormContainer");
        const selectedType = getActaSelectedType();
        const formMap = { impresora: "actaImpresoraForm", proyector: "actaProyectorForm", escaner: "actaEscanerForm", telefono: "actaTelefonoForm" };
        const activeFormId = formMap[selectedType] || "actaPcForm";
        const allFormIds = ["actaPcForm", "actaImpresoraForm", "actaProyectorForm", "actaEscanerForm", "actaTelefonoForm"];
        if (items.length > 0) {
            if (container) container.classList.remove("hidden");
        }
        allFormIds.forEach((id) => document.getElementById(id)?.classList.add("hidden"));
        if (formContainer) formContainer.classList.add("hidden");
        if (!items.length) {
            container.classList.add("hidden");
            container.innerHTML = '<div class="empty-state">Ingrese un criterio de búsqueda o cambie el tipo de equipo.</div>';
            return;
        }
        container.classList.remove("hidden");
        container.innerHTML = `
            <div class="acta-search-results__meta">Mostrando ${items.length} de ${totalCount} coincidencia(s). Seleccione una para autocompletar el acta.</div>
            <div class="acta-search-results__list">
                ${items.map((item) => `
                    <button type="button" class="acta-search-card" data-acta-select="${item.id}">
                        <strong>${escapeHtml(item.codigoSbai || item.codigoMegan || `ID ${item.id}`)} · ${escapeHtml(displayInventoryType(item))}</strong>
                        <span>${escapeHtml([item.marca, item.modelo, item.numeroSerie].filter(Boolean).join(" / ") || "Sin marca/modelo registrado")}</span>
                        <span>Custodio: ${escapeHtml(item.custodio || "-")} · Edificio: ${escapeHtml(item.ubicacionEdificio || "-")}</span>
                    </button>
                `).join("")}
            </div>
            ${items.length < totalCount ? `
                <div style="text-align: center; margin-top: 16px;">
                    <button type="button" class="btn btn-secondary" id="actaEquipoLoadMoreBtn">Cargar más</button>
                </div>
            ` : ''}
        `;
        container.querySelectorAll("[data-acta-select]").forEach((button) => {
            button.addEventListener("click", () => selectActaEquipo(button.dataset.actaSelect));
        });
        const loadMoreBtn = document.getElementById("actaEquipoLoadMoreBtn");
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener("click", () => {
                state.actaEquipoLimit += 50;
                renderActaEquipoResults(state.actaEquipoLastFiltered.slice(0, state.actaEquipoLimit), state.actaEquipoLastFiltered.length);
            });
        }
    }

    function autofillActaPcForm(item) {
        const selectedType = String(item.tipo || "pc").toLowerCase();
        setInputValue("actaFuncionarioNombre", item.custodio || "");
        setInputValue("actaFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaDesktopTipo", selectedType === "laptop" ? "LAPTOP" : "PC");
        setInputValue("actaDesktopMarca", item.marca || "");
        setInputValue("actaDesktopModelo", item.modelo || "");
        setInputValue("actaDesktopSerial", item.numeroSerie || "");
        setInputValue("actaDesktopCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaLaptopMarca", selectedType === "laptop" ? item.marca || "" : "");
        setInputValue("actaLaptopModelo", selectedType === "laptop" ? item.modelo || "" : "");
        setInputValue("actaLaptopSerial", selectedType === "laptop" ? item.numeroSerie || "" : "");
        setInputValue("actaLaptopCodigo", selectedType === "laptop" ? item.codigoSbai || item.codigoMegan || "" : "");
        setInputValue("actaRecibeNombre", item.custodio || "");
    }

    function resetActaPcForm() {
        document.getElementById("actaPcForm")?.reset();
        clearSelectedActaEquipo();
        setInputValue("actaDesktopTipo", getActaSelectedType() === "laptop" ? "LAPTOP" : "PC");
        setInputValue("actaLaptopTipo", "LAPTOP");
        loadActaPcInitialData();
    }

    function clearSelectedActaEquipo() {
        ["actaPcSelector", "actaImpresoraSelector", "actaProyectorSelector", "actaEscanerSelector", "actaTelefonoSelector"].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.value = "";
        });
        document.querySelectorAll(".acta-search-card").forEach((card) => card.classList.remove("is-selected"));
    }

    function setInputValue(id, value) {
        const input = document.getElementById(id);
        if (input) {
            input.value = value || "";
        }
    }

    function openActaPcPreview() {
        const form = document.getElementById("actaPcForm");
        const selector = document.getElementById("actaPcSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) {
            return;
        }
        const payload = buildActaPcPayload();
        openModal(
            "Previsualización del Acta de Equipo",
            `<div class="acta-preview">${renderActaPcPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaPc("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaPc("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    function buildActaPcPayload() {
        const selectedType = getActaSelectedType();
        return {
            subapartado: selectedType,
            equipoSeleccionado: document.getElementById("actaPcSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaFuncionarioEdificio")?.value.trim() || ""
            },
            desktop: {
                tipo: document.getElementById("actaDesktopTipo")?.value.trim() || (selectedType === "laptop" ? "LAPTOP" : "PC"),
                marca: document.getElementById("actaDesktopMarca")?.value.trim() || "",
                modelo: document.getElementById("actaDesktopModelo")?.value.trim() || "",
                serial: document.getElementById("actaDesktopSerial")?.value.trim() || "",
                codigo: document.getElementById("actaDesktopCodigo")?.value.trim() || ""
            },
            laptop: {
                tipo: document.getElementById("actaLaptopTipo")?.value.trim() || "LAPTOP",
                marca: document.getElementById("actaLaptopMarca")?.value.trim() || "",
                modelo: document.getElementById("actaLaptopModelo")?.value.trim() || "",
                serial: document.getElementById("actaLaptopSerial")?.value.trim() || "",
                codigo: document.getElementById("actaLaptopCodigo")?.value.trim() || ""
            },
            actividades: ACTA_PC_ACTIVITY_ROWS.map((actividad, index) => ({
                actividad,
                fecha: document.getElementById(`actaActividadFecha${index}`)?.value || "",
                estado: document.getElementById(`actaActividadEstado${index}`)?.value.trim() || "",
                observacion: document.getElementById(`actaActividadObservacion${index}`)?.value.trim() || ""
            }
            )),
            certificacion: ACTA_PC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaEntregaNombre", "actaEntregaNombreOtro"),
                firma: document.getElementById("actaEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaRecibeFecha")?.value || ""
            }
            
        };
    }

    function renderActaPcPreview(payload) {
        const funcionario = payload.funcionario || {};
        const desktop = payload.desktop || {};
        const laptop = payload.laptop || {};
        const actividades = Array.isArray(payload.actividades) ? payload.actividades : [];

        return `
            <div class="acta-sheet acta-sheet--preview" style="width:794px; min-height:1123px; max-width:100%; margin:0 auto; box-sizing:border-box; display:flex; flex-direction:column;">
                <div class="acta-sheet__header">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td>
                        <td>${escapeHtml(funcionario.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td>
                        <td>${escapeHtml(funcionario.cargo || "")}</td>
                        <td class="acta-table__label">N&#176; EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td>
                        <td>${escapeHtml(funcionario.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td>
                        <td>${escapeHtml(funcionario.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                    <tr><td>${escapeHtml(desktop.tipo || "")}</td><td>${escapeHtml(desktop.marca || "")}</td><td>${escapeHtml(desktop.modelo || "")}</td><td>${escapeHtml(desktop.serial || "")}</td><td>${escapeHtml(desktop.codigo || "")}</td></tr>
                    <tr><td>${escapeHtml(laptop.tipo || "")}</td><td>${escapeHtml(laptop.marca || "")}</td><td>${escapeHtml(laptop.modelo || "")}</td><td>${escapeHtml(laptop.serial || "")}</td><td>${escapeHtml(laptop.codigo || "")}</td></tr>
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="4" class="acta-table__title-dark">COMPUTADORA</th></tr>
                    <tr><th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS</th><th colspan="3">INSTALADO</th></tr>
                    <tr><th>FECHA</th><th>ESTADO</th><th>OBSERVACIÓN</th></tr>
                    ${ACTA_PC_ACTIVITY_ROWS.map((activity, index) => {
                        const row = actividades[index] || {};
                        return `
                            <tr class="${index === 3 ? "acta-table__row--tall" : ""}">
                                <td class="acta-table__activity">${activity}</td>
                                <td>${escapeHtml(row.fecha || "")}</td>
                                <td>${escapeHtml(row.estado || "")}</td>
                                <td class="acta-preview__observation">${escapeHtml(row.observacion || "")}</td>
                            </tr>
                        `;
                    }).join("")}
                </table>
                <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_PC_CERTIFICATION_TEXT)}</p>
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer" style="margin-top:auto;">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +539-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>
                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                </div>
            </div>
        `;
    }

    async function exportActaPc(format) {
        try {
            const form = document.getElementById("actaPcForm");
            const selector = document.getElementById("actaPcSelector");
            if (!form || !selector?.value) {
                showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
                return;
            }
            if (!form.reportValidity()) {
                return;
            }
            const payload = buildActaPcPayload();
            const response = await apiFetch(`/actas/equipos/pc/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore parsing fallback
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_mantenimiento_pc.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function autofillActaImpresoraForm(item) {
        setInputValue("actaImpresoraFuncionarioNombre", item.custodio || "");
        setInputValue("actaImpresoraFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaImpresoraFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaImpresoraEquipoTipo", "IMPRESORA");
        setInputValue("actaImpresoraEquipoMarca", item.marca || "");
        setInputValue("actaImpresoraEquipoModelo", item.modelo || "");
        setInputValue("actaImpresoraEquipoSerial", item.numeroSerie || "");
        setInputValue("actaImpresoraEquipoCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaImpresoraRecibeNombre", item.custodio || "");
    }

    function resetActaImpresoraForm() {
        document.getElementById("actaImpresoraForm")?.reset();
        const selector = document.getElementById("actaImpresoraSelector");
        if (selector) selector.value = "";
        document.querySelectorAll(".acta-search-card").forEach((card) => card.classList.remove("is-selected"));
        setInputValue("actaImpresoraEquipoTipo", "IMPRESORA");
        loadActaImpresoraInitialData();
    }

    function loadActaImpresoraInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaImpresoraEntregaFecha", "actaImpresoraRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) input.value = today;
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaImpresoraEntregaNombre");
            if (entrega && !entrega.value) entrega.value = state.session.displayName;
        }
    }

    function buildActaImpresoraPayload() {
        return {
            subapartado: "impresora",
            equipoSeleccionado: document.getElementById("actaImpresoraSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaImpresoraFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaImpresoraFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaImpresoraFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaImpresoraFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaImpresoraFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaImpresoraFuncionarioEdificio")?.value.trim() || ""
            },
            desktop: {
                tipo: document.getElementById("actaImpresoraEquipoTipo")?.value.trim() || "IMPRESORA",
                marca: document.getElementById("actaImpresoraEquipoMarca")?.value.trim() || "",
                modelo: document.getElementById("actaImpresoraEquipoModelo")?.value.trim() || "",
                serial: document.getElementById("actaImpresoraEquipoSerial")?.value.trim() || "",
                codigo: document.getElementById("actaImpresoraEquipoCodigo")?.value.trim() || ""
            },
            actividades: ACTA_IMPRESORA_ACTIVITY_ROWS.map((actividad, index) => ({
                actividad,
                fecha: document.getElementById(`actaImpresoraActividadFecha${index}`)?.value || "",
                estado: document.getElementById(`actaImpresoraActividadEstado${index}`)?.value.trim() || "",
                observacion: document.getElementById(`actaImpresoraActividadObservacion${index}`)?.value.trim() || ""
            })),
            certificacion: ACTA_PC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaImpresoraEntregaNombre", "actaImpresoraEntregaNombreOtro"),
                firma: document.getElementById("actaImpresoraEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaImpresoraEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaImpresoraRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaImpresoraRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaImpresoraRecibeFecha")?.value || ""
            }
        };
    }

    function renderActaImpresoraPreview(payload) {
        const funcionario = payload.funcionario || {};
        const equipo = payload.desktop || {};
        const actividades = Array.isArray(payload.actividades) ? payload.actividades : [];
        return `
            <div class="acta-sheet acta-sheet--preview">
                <div class="acta-sheet__header">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td>
                        <td>${escapeHtml(funcionario.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td>
                        <td>${escapeHtml(funcionario.cargo || "")}</td>
                        <td class="acta-table__label">N&#176; EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td>
                        <td>${escapeHtml(funcionario.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td>
                        <td>${escapeHtml(funcionario.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                    <tr>
                        <td>${escapeHtml(equipo.tipo || "IMPRESORA")}</td>
                        <td>${escapeHtml(equipo.marca || "")}</td>
                        <td>${escapeHtml(equipo.modelo || "")}</td>
                        <td>${escapeHtml(equipo.serial || "")}</td>
                        <td>${escapeHtml(equipo.codigo || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="4" class="acta-table__title-dark">IMPRESORA</th></tr>
                    <tr><th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th><th colspan="3">INSTALADO</th></tr>
                    <tr><th>FECHA</th><th>ESTADO</th><th>OBSERVACIÓN</th></tr>
                    ${ACTA_IMPRESORA_ACTIVITY_ROWS.map((activity, index) => {
                        const row = actividades[index] || {};
                        return `
                            <tr>
                                <td class="acta-table__activity">${activity}</td>
                                <td>${escapeHtml(row.fecha || "")}</td>
                                <td>${escapeHtml(row.estado || "")}</td>
                                <td class="acta-preview__observation">${escapeHtml(row.observacion || "")}</td>
                            </tr>
                        `;
                    }).join("")}
                </table>
                <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_PC_CERTIFICATION_TEXT)}</p>
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +539-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>
                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                </div>
            </div>
        `;
    }

    function openActaImpresoraPreview() {
        const form = document.getElementById("actaImpresoraForm");
        const selector = document.getElementById("actaImpresoraSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaImpresoraPayload();
        openModal(
            "Previsualización del Acta de Impresora",
            `<div class="acta-preview">${renderActaImpresoraPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaImpresora("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaImpresora("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaImpresora(format) {
        try {
            const form = document.getElementById("actaImpresoraForm");
            const selector = document.getElementById("actaImpresoraSelector");
            if (!form || !selector?.value) {
                showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
                return;
            }
            if (!form.reportValidity()) return;
            const payload = buildActaImpresoraPayload();
            const response = await apiFetch(`/actas/equipos/impresora/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore parsing fallback
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_mantenimiento_impresora.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function autofillActaEscanerForm(item) {
        setInputValue("actaEscanerFuncionarioNombre", item.custodio || "");
        setInputValue("actaEscanerFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaEscanerFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaEscanerEquipoTipo", "ESCÁNER");
        setInputValue("actaEscanerEquipoMarca", item.marca || "");
        setInputValue("actaEscanerEquipoModelo", item.modelo || "");
        setInputValue("actaEscanerEquipoSerial", item.numeroSerie || "");
        setInputValue("actaEscanerEquipoCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaEscanerRecibeNombre", item.custodio || "");
    }

    function resetActaEscanerForm() {
        document.getElementById("actaEscanerForm")?.reset();
        const selector = document.getElementById("actaEscanerSelector");
        if (selector) selector.value = "";
        document.querySelectorAll(".acta-search-card").forEach((card) => card.classList.remove("is-selected"));
        setInputValue("actaEscanerEquipoTipo", "ESCÁNER");
        loadActaEscanerInitialData();
    }

    function loadActaEscanerInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaEscanerEntregaFecha", "actaEscanerRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) input.value = today;
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaEscanerEntregaNombre");
            if (entrega && !entrega.value) entrega.value = state.session.displayName;
        }
    }

    function buildActaEscanerPayload() {
        return {
            subapartado: "escaner",
            equipoSeleccionado: document.getElementById("actaEscanerSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaEscanerFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaEscanerFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaEscanerFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaEscanerFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaEscanerFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaEscanerFuncionarioEdificio")?.value.trim() || ""
            },
            desktop: {
                tipo: document.getElementById("actaEscanerEquipoTipo")?.value.trim() || "ESCÁNER",
                marca: document.getElementById("actaEscanerEquipoMarca")?.value.trim() || "",
                modelo: document.getElementById("actaEscanerEquipoModelo")?.value.trim() || "",
                serial: document.getElementById("actaEscanerEquipoSerial")?.value.trim() || "",
                codigo: document.getElementById("actaEscanerEquipoCodigo")?.value.trim() || ""
            },
            actividades: ACTA_ESCANER_ACTIVITY_ROWS.map((actividad, index) => ({
                actividad,
                fecha: document.getElementById(`actaEscanerActividadFecha${index}`)?.value || "",
                estado: document.getElementById(`actaEscanerActividadEstado${index}`)?.value.trim() || "",
                observacion: document.getElementById(`actaEscanerActividadObservacion${index}`)?.value.trim() || ""
            })),
            certificacion: ACTA_PC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaEscanerEntregaNombre", "actaEscanerEntregaNombreOtro"),
                firma: document.getElementById("actaEscanerEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaEscanerEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaEscanerRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaEscanerRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaEscanerRecibeFecha")?.value || ""
            }
        };
    }

    function renderActaEscanerPreview(payload) {
        const funcionario = payload.funcionario || {};
        const equipo = payload.desktop || {};
        const actividades = Array.isArray(payload.actividades) ? payload.actividades : [];
        return `
            <div class="acta-sheet acta-sheet--preview">
                <div class="acta-sheet__header">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td><td>${escapeHtml(funcionario.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td><td>${escapeHtml(funcionario.cargo || "")}</td>
                        <td class="acta-table__label">N&#176; EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td><td>${escapeHtml(funcionario.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td><td>${escapeHtml(funcionario.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                    <tr>
                        <td>${escapeHtml(equipo.tipo || "ESCÁNER")}</td><td>${escapeHtml(equipo.marca || "")}</td>
                        <td>${escapeHtml(equipo.modelo || "")}</td><td>${escapeHtml(equipo.serial || "")}</td>
                        <td>${escapeHtml(equipo.codigo || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="4" class="acta-table__title-dark">ESCÁNER</th></tr>
                    <tr><th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th><th colspan="3">INSTALADO</th></tr>
                    <tr><th>FECHA</th><th>ESTADO</th><th>OBSERVACIÓN</th></tr>
                    ${ACTA_ESCANER_ACTIVITY_ROWS.map((activity, index) => {
                        const row = actividades[index] || {};
                        return `<tr>
                            <td class="acta-table__activity">${activity}</td>
                            <td>${escapeHtml(row.fecha || "")}</td>
                            <td>${escapeHtml(row.estado || "")}</td>
                            <td class="acta-preview__observation">${escapeHtml(row.observacion || "")}</td>
                        </tr>`;
                    }).join("")}
                </table>
                <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_PC_CERTIFICATION_TEXT)}</p>
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +539-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>
                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                </div>
            </div>
        `;
    }

    function openActaEscanerPreview() {
        const form = document.getElementById("actaEscanerForm");
        const selector = document.getElementById("actaEscanerSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaEscanerPayload();
        openModal(
            "Previsualización del Acta de Escáner",
            `<div class="acta-preview">${renderActaEscanerPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaEscaner("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaEscaner("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaEscaner(format) {
        try {
            const form = document.getElementById("actaEscanerForm");
            const selector = document.getElementById("actaEscanerSelector");
            if (!form || !selector?.value) {
                showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
                return;
            }
            if (!form.reportValidity()) return;
            const payload = buildActaEscanerPayload();
            const response = await apiFetch(`/actas/equipos/escaner/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore parsing fallback
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_mantenimiento_escaner.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function autofillActaTelefonoForm(item) {
        setInputValue("actaTelefonoFuncionarioNombre", item.custodio || "");
        setInputValue("actaTelefonoFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaTelefonoFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaTelefonoEquipoTipo", "TELÉFONO IP");
        setInputValue("actaTelefonoEquipoMarca", item.marca || "");
        setInputValue("actaTelefonoEquipoModelo", item.modelo || "");
        setInputValue("actaTelefonoEquipoSerial", item.numeroSerie || "");
        setInputValue("actaTelefonoEquipoCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaTelefonoRecibeNombre", item.custodio || "");
    }

    function resetActaTelefonoForm() {
        document.getElementById("actaTelefonoForm")?.reset();
        const selector = document.getElementById("actaTelefonoSelector");
        if (selector) selector.value = "";
        document.querySelectorAll(".acta-search-card").forEach((card) => card.classList.remove("is-selected"));
        setInputValue("actaTelefonoEquipoTipo", "TELÉFONO IP");
        loadActaTelefonoInitialData();
    }

    function loadActaTelefonoInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaTelefonoEntregaFecha", "actaTelefonoRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) input.value = today;
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaTelefonoEntregaNombre");
            if (entrega && !entrega.value) entrega.value = state.session.displayName;
        }
    }

    function buildActaTelefonoPayload() {
        return {
            subapartado: "telefono_ip",
            equipoSeleccionado: document.getElementById("actaTelefonoSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaTelefonoFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaTelefonoFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaTelefonoFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaTelefonoFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaTelefonoFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaTelefonoFuncionarioEdificio")?.value.trim() || ""
            },
            desktop: {
                tipo: document.getElementById("actaTelefonoEquipoTipo")?.value.trim() || "TELÉFONO IP",
                marca: document.getElementById("actaTelefonoEquipoMarca")?.value.trim() || "",
                modelo: document.getElementById("actaTelefonoEquipoModelo")?.value.trim() || "",
                serial: document.getElementById("actaTelefonoEquipoSerial")?.value.trim() || "",
                codigo: document.getElementById("actaTelefonoEquipoCodigo")?.value.trim() || ""
            },
            actividades: ACTA_TELEFONO_ACTIVITY_ROWS.map((actividad, index) => ({
                actividad,
                fecha: document.getElementById(`actaTelefonoActividadFecha${index}`)?.value || "",
                estado: document.getElementById(`actaTelefonoActividadEstado${index}`)?.value.trim() || "",
                observacion: document.getElementById(`actaTelefonoActividadObservacion${index}`)?.value.trim() || ""
            })),
            certificacion: ACTA_PC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaTelefonoEntregaNombre", "actaTelefonoEntregaNombreOtro"),
                firma: document.getElementById("actaTelefonoEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaTelefonoEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaTelefonoRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaTelefonoRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaTelefonoRecibeFecha")?.value || ""
            }
        };
    }

    function renderActaTelefonoPreview(payload) {
        const { funcionario = {}, desktop = {}, actividades = [], certificacion = "", entrega = {}, recibe = {} } = payload;
        const entregaNombre = entrega.nombre || "", entregaFirma = entrega.firma || "", entregaFecha = entrega.fecha || "";
        const recibeNombre = recibe.nombre || "", recibeFirma = recibe.firma || "", recibeFecha = recibe.fecha || "";
        return `
        <div class="acta-sheet">
            <div class="acta-sheet__header">
                <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="SENADI" class="acta-sheet__logo acta-sheet__logo--senadi">
            </div>
            <div class="acta-sheet__titles">
                <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
            </div>
            <table class="acta-table">
                <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                <tr>
                    <td class="acta-table__label">NOMBRE</td><td>${escapeHtml(funcionario.nombre || "")}</td>
                    <td class="acta-table__label">CARGO</td><td>${escapeHtml(funcionario.cargo || "")}</td>
                    <td class="acta-table__label">N° EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                </tr>
                <tr>
                    <td class="acta-table__label">CORREO</td><td>${escapeHtml(funcionario.correo || "")}</td>
                    <td class="acta-table__label">ÁREA</td><td>${escapeHtml(funcionario.area || "")}</td>
                    <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                </tr>
            </table>
            <table class="acta-table acta-table--equipos">
                <tr><th colspan="5">EQUIPOS</th></tr>
                <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                <tr>
                    <td>${escapeHtml(desktop.tipo || "TELÉFONO IP")}</td><td>${escapeHtml(desktop.marca || "")}</td>
                    <td>${escapeHtml(desktop.modelo || "")}</td><td>${escapeHtml(desktop.serial || "")}</td><td>${escapeHtml(desktop.codigo || "")}</td>
                </tr>
            </table>
            <table class="acta-table acta-table--actividades">
                <tr><th colspan="4" class="acta-table__title-dark">TELÉFONO IP</th></tr>
                <tr><th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th><th colspan="3">INSTALADO</th></tr>
                <tr><th>FECHA</th><th>ESTADO</th><th>OBSERVACIÓN</th></tr>
                ${ACTA_TELEFONO_ACTIVITY_ROWS.map((activity, index) => {
                    const row = actividades[index] || {};
                    return `<tr>
                        <td class="acta-table__activity">${escapeHtml(activity)}</td>
                        <td>${escapeHtml(row.fecha || "")}</td>
                        <td>${escapeHtml(row.estado || "")}</td>
                        <td>${escapeHtml(row.observacion || "")}</td>
                    </tr>`;
                }).join("")}
            </table>
            <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_PC_CERTIFICATION_TEXT)}</p>
            <table class="acta-table acta-table--firma">
                <tr><th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th></tr>
                <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                <tr>
                    <td><strong>Nombre:</strong> ${escapeHtml(entregaNombre)}</td>
                    <td><strong>Nombre:</strong> ${escapeHtml(recibeNombre)}</td>
                </tr>
                <tr class="acta-table__row--firma">
                    <td><strong>Firma:</strong> ${escapeHtml(entregaFirma)}</td>
                    <td><strong>Firma:</strong> ${escapeHtml(recibeFirma)}</td>
                </tr>
                <tr>
                    <td><strong>Fecha:</strong> ${escapeHtml(entregaFecha)}</td>
                    <td><strong>Fecha:</strong> ${escapeHtml(recibeFecha)}</td>
                </tr>
            </table>
            <div class="acta-sheet__footer">
                <div class="acta-sheet__footer-text">
                    <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                    <span>Código postal: 170518 / Quito — Ecuador</span>
                    <span>Teléfono: +539-2 394 0000</span>
                    <span>www.derechosintelectuales.gob.ec</span>
                </div>
                <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
            </div>
        </div>`;
    }

    function openActaTelefonoPreview() {
        const form = document.getElementById("actaTelefonoForm");
        const selector = document.getElementById("actaTelefonoSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaTelefonoPayload();
        openModal(
            "Vista Previa — Acta Teléfono IP",
            `<div class="acta-preview">${renderActaTelefonoPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaTelefono("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaTelefono("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaTelefono(format) {
        try {
            const form = document.getElementById("actaTelefonoForm");
            const selector = document.getElementById("actaTelefonoSelector");
            if (!form || !selector?.value) {
                showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
                return;
            }
            if (!form.reportValidity()) return;
            const payload = buildActaTelefonoPayload();
            const response = await apiFetch(`/actas/equipos/telefono/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore parsing fallback
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_mantenimiento_telefono.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function autofillActaProyectorForm(item) {
        setInputValue("actaProyectorFuncionarioNombre", item.custodio || "");
        setInputValue("actaProyectorFuncionarioEdificio", item.ubicacionEdificio || "");
        setInputValue("actaProyectorFuncionarioArea", item.ubicacionDireccion || item.ubicacion || "");
        setInputValue("actaProyectorEquipoTipo", "PROYECTOR");
        setInputValue("actaProyectorEquipoMarca", item.marca || "");
        setInputValue("actaProyectorEquipoModelo", item.modelo || "");
        setInputValue("actaProyectorEquipoSerial", item.numeroSerie || "");
        setInputValue("actaProyectorEquipoCodigo", item.codigoSbai || item.codigoMegan || "");
        setInputValue("actaProyectorRecibeNombre", item.custodio || "");
    }

    function resetActaProyectorForm() {
        document.getElementById("actaProyectorForm")?.reset();
        const selector = document.getElementById("actaProyectorSelector");
        if (selector) selector.value = "";
        document.querySelectorAll(".acta-search-card").forEach((card) => card.classList.remove("is-selected"));
        setInputValue("actaProyectorEquipoTipo", "PROYECTOR");
        loadActaProyectorInitialData();
    }

    function loadActaProyectorInitialData() {
        const today = new Date().toISOString().slice(0, 10);
        ["actaProyectorEntregaFecha", "actaProyectorRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) input.value = today;
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaProyectorEntregaNombre");
            if (entrega && !entrega.value) entrega.value = state.session.displayName;
        }
    }

    function buildActaProyectorPayload() {
        return {
            subapartado: "proyector",
            equipoSeleccionado: document.getElementById("actaProyectorSelector")?.value || "",
            funcionario: {
                nombre: document.getElementById("actaProyectorFuncionarioNombre")?.value.trim() || "",
                cargo: document.getElementById("actaProyectorFuncionarioCargo")?.value.trim() || "",
                extension: document.getElementById("actaProyectorFuncionarioExtension")?.value.trim() || "",
                correo: document.getElementById("actaProyectorFuncionarioCorreo")?.value.trim() || "",
                area: document.getElementById("actaProyectorFuncionarioArea")?.value.trim() || "",
                edificio: document.getElementById("actaProyectorFuncionarioEdificio")?.value.trim() || ""
            },
            desktop: {
                tipo: document.getElementById("actaProyectorEquipoTipo")?.value.trim() || "PROYECTOR",
                marca: document.getElementById("actaProyectorEquipoMarca")?.value.trim() || "",
                modelo: document.getElementById("actaProyectorEquipoModelo")?.value.trim() || "",
                serial: document.getElementById("actaProyectorEquipoSerial")?.value.trim() || "",
                codigo: document.getElementById("actaProyectorEquipoCodigo")?.value.trim() || ""
            },
            actividades: ACTA_PROYECTOR_ACTIVITY_ROWS.map((actividad, index) => ({
                actividad,
                fecha: document.getElementById(`actaProyectorActividadFecha${index}`)?.value || "",
                estado: document.getElementById(`actaProyectorActividadEstado${index}`)?.value.trim() || "",
                observacion: document.getElementById(`actaProyectorActividadObservacion${index}`)?.value.trim() || ""
            })),
            certificacion: ACTA_PC_CERTIFICATION_TEXT,
            entrega: {
                nombre: getEntregaNombreValue("actaProyectorEntregaNombre", "actaProyectorEntregaNombreOtro"),
                firma: document.getElementById("actaProyectorEntregaFirma")?.value.trim() || "",
                fecha: document.getElementById("actaProyectorEntregaFecha")?.value || ""
            },
            recibe: {
                nombre: document.getElementById("actaProyectorRecibeNombre")?.value.trim() || "",
                firma: document.getElementById("actaProyectorRecibeFirma")?.value.trim() || "",
                fecha: document.getElementById("actaProyectorRecibeFecha")?.value || ""
            }
        };
    }

    function renderActaProyectorPreview(payload) {
        const funcionario = payload.funcionario || {};
        const equipo = payload.desktop || {};
        const actividades = Array.isArray(payload.actividades) ? payload.actividades : [];
        return `
            <div class="acta-sheet acta-sheet--preview">
                <div class="acta-sheet__header">
                    <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                    <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
                </div>
                <div class="acta-sheet__titles">
                    <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                    <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                    <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
                </div>
                <table class="acta-table">
                    <tr><th colspan="6">DATOS DEL FUNCIONARIO SENADI</th></tr>
                    <tr>
                        <td class="acta-table__label">NOMBRE</td>
                        <td>${escapeHtml(funcionario.nombre || "")}</td>
                        <td class="acta-table__label">CARGO</td>
                        <td>${escapeHtml(funcionario.cargo || "")}</td>
                        <td class="acta-table__label">N&#176; EXT.</td><td>${escapeHtml(funcionario.extension || "")}</td>
                    </tr>
                    <tr>
                        <td class="acta-table__label">CORREO</td>
                        <td>${escapeHtml(funcionario.correo || "")}</td>
                        <td class="acta-table__label">ÁREA</td>
                        <td>${escapeHtml(funcionario.area || "")}</td>
                        <td class="acta-table__label">EDIFICIO</td><td>${escapeHtml(funcionario.edificio || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--equipos">
                    <tr><th colspan="5">EQUIPOS</th></tr>
                    <tr><th>TIPO</th><th>MARCA</th><th>MODELO</th><th>SERIAL</th><th>CÓDIGO</th></tr>
                    <tr>
                        <td>${escapeHtml(equipo.tipo || "PROYECTOR")}</td>
                        <td>${escapeHtml(equipo.marca || "")}</td>
                        <td>${escapeHtml(equipo.modelo || "")}</td>
                        <td>${escapeHtml(equipo.serial || "")}</td>
                        <td>${escapeHtml(equipo.codigo || "")}</td>
                    </tr>
                </table>
                <table class="acta-table acta-table--actividades">
                    <tr><th colspan="4" class="acta-table__title-dark">PROYECTOR</th></tr>
                    <tr><th rowspan="2" class="acta-table__label-large">ACTIVIDADES DE MANTENIMIENTOS A REALIZAR</th><th colspan="3">INSTALADO</th></tr>
                    <tr><th>FECHA</th><th>ESTADO</th><th>OBSERVACIÓN</th></tr>
                    ${ACTA_PROYECTOR_ACTIVITY_ROWS.map((activity, index) => {
                        const row = actividades[index] || {};
                        return `
                            <tr>
                                <td class="acta-table__activity">${activity}</td>
                                <td>${escapeHtml(row.fecha || "")}</td>
                                <td>${escapeHtml(row.estado || "")}</td>
                                <td class="acta-preview__observation">${escapeHtml(row.observacion || "")}</td>
                            </tr>
                        `;
                    }).join("")}
                </table>
                <p class="acta-certification">${escapeHtml(payload.certificacion || ACTA_PC_CERTIFICATION_TEXT)}</p>
                <table class="acta-table acta-table--firma">
                    <tr><th colspan="2">ENTREGA RECEPCION DE EQUIPO</th></tr>
                    <tr><th>ENTREGA</th><th>RECIBE</th></tr>
                    <tr><td><strong>Nombre:</strong> ${escapeHtml(payload.entrega?.nombre || "")}</td><td><strong>Nombre:</strong> ${escapeHtml(payload.recibe?.nombre || "")}</td></tr>
                    <tr class="acta-table__row--firma"><td><strong>Firma:</strong> ${escapeHtml(payload.entrega?.firma || "")}</td><td><strong>Firma:</strong> ${escapeHtml(payload.recibe?.firma || "")}</td></tr>
                    <tr><td><strong>Fecha:</strong> ${escapeHtml(payload.entrega?.fecha || "")}</td><td><strong>Fecha:</strong> ${escapeHtml(payload.recibe?.fecha || "")}</td></tr>
                </table>
                <div class="acta-sheet__footer">
                    <div class="acta-sheet__footer-text">
                        <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                        <span>Código postal: 170518 / Quito — Ecuador</span>
                        <span>Teléfono: +539-2 394 0000</span>
                        <span>www.derechosintelectuales.gob.ec</span>
                    </div>
                    <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="El Nuevo Ecuador" class="acta-sheet__footer-logo">
                </div>
            </div>
        `;
    }

    function openActaProyectorPreview() {
        const form = document.getElementById("actaProyectorForm");
        const selector = document.getElementById("actaProyectorSelector");
        if (!form || !selector?.value) {
            showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
            return;
        }
        if (!form.reportValidity()) return;
        const payload = buildActaProyectorPayload();
        openModal(
            "Previsualización del Acta de Proyector",
            `<div class="acta-preview">${renderActaProyectorPreview(payload)}</div>`,
            [
                { label: "Exportar DOCX", className: "btn btn-primary", onClick: () => exportActaProyector("docx") },
                { label: "Exportar PDF", className: "btn btn-secondary", onClick: () => exportActaProyector("pdf") },
                { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
            ],
            "modal--wide"
        );
    }

    async function exportActaProyector(format) {
        try {
            const form = document.getElementById("actaProyectorForm");
            const selector = document.getElementById("actaProyectorSelector");
            if (!form || !selector?.value) {
                showToast("Equipo requerido", "Seleccione un equipo desde el buscador inicial para autocompletar el acta.", "warning");
                return;
            }
            if (!form.reportValidity()) return;
            const payload = buildActaProyectorPayload();
            const response = await apiFetch(`/actas/equipos/proyector/export/${format}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                let message = "No se pudo exportar el acta.";
                try {
                    const errorPayload = await response.json();
                    message = errorPayload.message || errorPayload.error || message;
                } catch (error) {
                    // ignore parsing fallback
                }
                throw new Error(message);
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_mantenimiento_proyector.${format}`;
            downloadBlob(blob, filename);
            showToast("Exportación lista", `El documento ${format.toUpperCase()} fue generado correctamente.`, "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo exportar el acta.", "danger");
        }
    }

    function renderDashboard() {
        if (window.SIDashboardView?.renderDashboard) {
            return window.SIDashboardView.renderDashboard({ role });
        }
        return `
            <div class="section-heading">
                <div>
                    <h2>Navegación rápida</h2>
                    <p>Accesos principales del sistema.</p>
                </div>
            </div>
            <section class="quick-grid">${dashboardCards().join("")}</section>
            <section class="hero hero--dashboard">
                <div class="hero__grid">
                    <div class="hero-copy">
                        <h2>Inventario institucional conectado a la base real.</h2>
                        <p>Consulta equipos tecnológicos, busca por varios campos escribiendo manualmente y aprovecha sugerencias basadas en registros ya guardados en la base.</p>
                    </div>
                    <div class="hero-panel">
                        <div class="hero-panel__label">Vista rápida</div>
                        <div class="stats-grid stats-grid--compact">
                            <article class="stat-card"><span>Total cargado</span><strong id="statTotal">--</strong></article>
                            <article class="stat-card"><span>Operativos</span><strong id="statActive">--</strong></article>
                            <article class="stat-card"><span>Ubicaciones</span><strong id="statLocations">--</strong></article>
                            <article class="stat-card"><span>Rol activo</span><strong>${role === "admin" ? "Administrador" : role === "custodio" ? "Custodio" : "Técnico"}</strong></article>
                        </div>
                    </div>
                </div>
            </section>
        `;
    }

    function dashboardCards() {
        const cards = role === "admin"
            ? [
                ["Inventario", "Filtra, revisa y exporta el inventario visible.", `${basePrefix}/pages/inventario.html`, "inventory"],
                // COMENTADO: redundante con filtros del mòdulo de inventario
                // ["BÁºsqueda", "Aplica mÁºltiples criterios sobre la data cargada.", `${basePrefix}/pages/busqueda.html`, "search"],
                ["Nuevo Equipo", "Completa el formulario guiado por categorí­a.", `${basePrefix}/pages/nuevo-equipo.html`, "plusBox"]
            ]
            : [
                ["Inventario", "Filtra, revisa y exporta el inventario visible.", `${basePrefix}/pages/usuario/inventario.html`, "inventory"],
                // COMENTADO: redundante con filtros del mÁ³dulo de inventario
                // ["BÁºsqueda", "Aplica mÁºltiples criterios sobre la data cargada.", `${basePrefix}/pages/usuario/busqueda.html`, "search"]
            ];
        return cards
            .filter(([title]) => title !== "Búsqueda")
            .map(([title, text, href, icon]) => `
            <a class="mini-card mini-card--action" href="${href}">
                <span class="mini-card__icon" aria-hidden="true">${iconMarkup(icon)}</span>
                <strong>${title}</strong>
                <span>${text}</span>
                <small>Abrir mòdulo</small>
            </a>
        `);
    }

    function renderAutocompleteField(id, label, placeholder) {
        const listId = `${id}List`;
        return `
            <div class="field-group">
                <label for="${id}">${label}</label>
                <input id="${id}" type="text" list="${listId}" autocomplete="off" placeholder="${placeholder}">
                <datalist id="${listId}"></datalist>
            </div>
        `;
    }

    async function loadInitialData() {
        if (page === "dashboard" || page === "inventario" || page === "busqueda" || page === "acta-equipos" || page === "acta-software" || page === "acta-licencias") {
            await loadInventory();
            updateDashboardStats();
        }
        if (page === "actas") {
            await loadHistoricActas();
        }
        if (page === "inventario") {
            state.inventorySearchPerformed = false;
            state.filteredInventory = [];
            state.inventoryPage = 1;
            renderInventory();
            refreshInventoryAutocompletes();
            document.querySelectorAll(".filters-grid--inventory .helper-text").forEach((item) => item.remove());
            const topbarDescription = document.querySelector(".topbar__title p");
            if (topbarDescription) {
                topbarDescription.textContent = "";
            }
        }
        if (page === "busqueda") {
            renderSearchTags();
            renderSearchResults([]);
        }
        if (page === "nuevo-equipo") {
            await loadEquipmentFieldCatalog();
            populateEquipmentCategoryOptions();
            renderDynamicFields(document.getElementById("equipmentCategory").value);
        }
        if (page === "acta-equipos") {
            loadActaPcInitialData();
        }
        if (page === "acta-software") {
            loadActaSwInitialData();
        }
    }

    async function loadEquipmentFieldCatalog() {
        try {
            const response = await apiFetch("/inventario/campos");
            if (!response.ok) {
                throw new Error("catalogo");
            }
            const payload = await response.json();
            state.equipmentFieldCatalog = payload.data || {};
        } catch (error) {
            state.equipmentFieldCatalog = {};
            showToast("Campos no disponibles", "No se pudo cargar el catalogo de campos desde la base de datos.", "danger");
        }
    }

    function populateEquipmentCategoryOptions() {
        const select = document.getElementById("equipmentCategory");
        if (!select) {
            return;
        }
        const tipos = Object.keys(state.equipmentFieldCatalog);
        select.innerHTML = tipos.length
            ? tipos.map((tipo) => `<option value="${tipo}">${escapeHtml(state.equipmentFieldCatalog[tipo].label || typeLabel(tipo))}</option>`).join("")
            : '<option value="">Sin tipos disponibles</option>';
    }

    function bindNewEquipmentEvents() {
        document.getElementById("equipmentCategory")?.addEventListener("change", (event) => renderDynamicFields(event.target.value));
        document.getElementById("newEquipmentForm")?.addEventListener("submit", (event) => {
            event.preventDefault();
        });
        document.getElementById("newEquipmentForm")?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
            }
        });
        document.getElementById("saveNewEquipmentButton")?.addEventListener("click", async () => {
            const form = document.getElementById("newEquipmentForm");
            if (!form) {
                return;
            }
            const category = document.getElementById("equipmentCategory").value;
            const button = document.getElementById("saveNewEquipmentButton");
            if (!validateRequiredEquipmentFields(form)) {
                showToast("Campos obligatorios", "Complete todos los campos antes de guardar.", "warning");
                return;
            }
            button.disabled = true;
            try {
                const payload = collectEquipmentFormPayload(form);
                const response = await apiFetch(`/inventario/${category}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const result = await response.json();
                if (!response.ok || !result.success) {
                    throw new Error(result.message || "No se pudo guardar el equipo.");
                }
                await loadInventory();
                button.disabled = false;
                form.reset();
                document.getElementById("equipmentCategory").value = category;
                renderDynamicFields(category);
                showToast("Equipo guardado", "El equipo se registro correctamente en la base de datos.", "success");
            } catch (error) {
                button.disabled = false;
                showToast("Error al guardar", error.message || "No se pudo registrar el equipo.", "danger");
            }
        });
        document.getElementById("newEquipmentForm")?.addEventListener("reset", () => {
            const category = document.getElementById("equipmentCategory")?.value;
            setTimeout(() => renderDynamicFields(category), 0);
        });
    }

    function bindActasEvents() {
        if (page === "acta-equipos") {
            bindActaPcEvents();
            return;
        }
        if (page === "acta-software") {
            bindActaSwEvents();
            return;
        }
        if (page === "acta-rc") {
            bindActaRcEvents();
            return;
        }
        if (page === "acta-licencias") {
            bindActaLicenciasEvents();
            return;
        }
        document.querySelectorAll("[data-acta-form]").forEach((form) => {
            form.addEventListener("submit", (event) => {
                event.preventDefault();
                const formKey = event.currentTarget.dataset.actaForm;
                const config = actaConfig(formKey);
                showToast("Acta lista", `${config?.title || "Formulario"} preparada para revision interna.`, "success");
            });
        });
        
    }

    function collectEquipmentFormPayload(form) {
        const payload = {};
        Array.from(form.elements).forEach((element) => {
            if (!element.name) {
                return;
            }
            const value = (element.value || "").trim();
            payload[element.name] = value;
        });
        return payload;
    }

    function validateRequiredEquipmentFields(form) {
        let valid = true;
        Array.from(form.elements).forEach((element) => {
            // data-optional: campos opcionales, como "Asignar equipo" de las licencias
            if (!element.name || element.disabled || element.dataset.optional) {
                return;
            }
            const empty = !String(element.value || "").trim();
            element.classList.toggle("field-invalid", empty);
            if (empty) {
                valid = false;
            }
        });
        form.querySelector(".field-invalid")?.focus();
        return valid;
    }

    function renderDynamicDbField(field) {
        const name = field.name;
        const label = displayDbFieldLabel(field);
        const required = "required";
        const autocompleteCatalog = autocompleteCatalogForField(name);
        if (typeof field.custom === "function") {
            return field.custom({ criterio: "", valor: "" });
        }
        if (field.inputType === "textarea") {
            return `<label class="field-group field-group--wide"><span>${escapeHtml(label)}</span><textarea name="${escapeHtml(name)}" ${required}></textarea></label>`;
        }
        if (name === "estado" || field.inputType === "select") {
            return `<label class="field-group"><span>${escapeHtml(label)}</span><select name="${escapeHtml(name)}" ${required}>${VALID_STATES.map((item) => `<option value="${item}">${item}</option>`).join("")}</select></label>`;
        }
        const htmlType = field.inputType === "number" ? "number" : (field.inputType === "date" ? "date" : "text");
        if (autocompleteCatalog) {
            const listId = `new_${name}_list`;
            return `<label class="field-group"><span>${escapeHtml(label)}</span><input type="${htmlType}" name="${escapeHtml(name)}" list="${listId}" autocomplete="off" data-catalog="${autocompleteCatalog}" ${required}><datalist id="${listId}"></datalist></label>`;
        }
        return `<label class="field-group"><span>${escapeHtml(label)}</span><input type="${htmlType}" name="${escapeHtml(name)}" ${required}></label>`;
    }

    function displayDbFieldLabel(field) {
        const name = String(field?.name || "").trim();
        const label = String(field?.label || name).trim();
        const normalizedName = name.toLowerCase();
        const normalizedLabel = label.toLowerCase();
        if (normalizedName === "descripcion") {
            return "Descripción";
        }
        if (normalizedName === "ubicacion_direccion") {
            return "Dirección / Área";
        }
        if (normalizedName === "observacion") {
            return "Observaciones";
        }
        if (normalizedName === "ram" || normalizedLabel === "ram") {
            return "RAM";
        }
        if (normalizedName === "so" || normalizedLabel === "so") {
            return "SO";
        }
        if (normalizedName === "ip" || normalizedLabel === "ip") {
            return "IP";
        }
        return label || name;
    }

    function renderDynamicDbFieldGroups(fields) {
        const groups = groupDynamicDbFields(fields);
        return groups.map((group) => `
            <section class="equipment-field-section">
                <div class="equipment-field-section__header">
                    <h3>${escapeHtml(group.title)}</h3>
                </div>
                <div class="equipment-field-section__grid">
                    ${group.fields.map((field) => renderDynamicDbField(field)).join("")}
                </div>
            </section>
        `).join("");
    }

    function groupDynamicDbFields(fields) {
        const groupDefinitions = [
            { title: "Identificación", names: ["codigo_megan", "codigo_sbye", "codigo_anterior", "descripcion", "marca", "modelo", "sn", "serie", "numero_serie", "estado", "costo", "computador_asignacion"] },
            { title: "Custodio y ubicación", names: ["custodio_nombre", "id_custodio_actual", "anterior_custodio", "id_ubicacion", "ubicacion_edificio", "ubicacion_piso", "ubicacion_direccion"] },
            { title: "Características técnicas", names: ["procesador", "ram", "disco_duro", "so", "ip", "mac", "tipo_periferico", "tipo_impresora", "resolucion", "conexion", "tecnologia", "compatibilidad", "lumenes", "subtipo", "megas"] },
            { title: "Fechas", names: ["fecha_ingreso", "ultima_actualizacion", "ultimo_mantenimiento"] },
            { title: "Contrato y servicio", names: ["numero_contrato", "numero_servicio", "plan_comercial", "estado_servicio"] },
            { title: "Observaciones", names: ["caracteristicas", "anotaciones", "observacion", "observaciones"] }
        ];
        const groups = groupDefinitions.map((definition) => ({ title: definition.title, names: definition.names, fields: [] }));
        const extraGroup = { title: "Datos adicionales", names: [], fields: [] };

        fields.forEach((field) => {
            const name = String(field.name || "").toLowerCase();
            const group = groups.find((item) => item.names.includes(name)) || extraGroup;
            group.fields.push(field);
        });

        return groups.concat(extraGroup).filter((group) => group.fields.length);
    }

    function autocompleteCatalogForField(name) {
        if (name === "marca" || name === "modelo") {
            return name;
        }
        if (name === "custodio_nombre") {
            return "custodios";
        }
        if (name === "ubicacion_edificio" || name === "ubicacion_piso" || name === "ubicacion_direccion") {
            return "ubicaciones";
        }
        return "";
    }

    function setupNewEquipmentAutocompletes() {
        document.querySelectorAll("#dynamicEquipmentFields [data-catalog]").forEach((input) => {
            const load = debounce(() => loadNewEquipmentSuggestions(input), 220);
            input.addEventListener("input", load);
            loadNewEquipmentSuggestions(input);
        });
    }

    function debounce(callback, delay) {
        let timerId;
        return (...args) => {
            window.clearTimeout(timerId);
            timerId = window.setTimeout(() => callback(...args), delay);
        };
    }

    async function loadNewEquipmentSuggestions(input) {
        const catalog = input.dataset.catalog;
        const term = input.value.trim();
        const params = new URLSearchParams();
        if (term) {
            params.set("q", term);
        }
        params.set("limit", "80");

        try {
            let values = [];
            if (catalog === "marca" || catalog === "modelo") {
                values = await fetchCatalogStrings(`/inventario/catalogos/${catalog}?${params.toString()}`);
            } else if (catalog === "custodios") {
                const rows = await fetchCatalogObjects(`/inventario/custodios?${params.toString()}`);
                values = rows.map((item) => item.nombre).filter(Boolean);
            } else if (catalog === "ubicaciones") {
                const rows = await fetchCatalogObjects(`/inventario/ubicaciones?${params.toString()}`);
                const key = input.name.replace("ubicacion_", "");
                values = rows.map((item) => item[key]).filter(Boolean);
            }
            fillInputDatalist(input, values);
        } catch (error) {
            fillInputDatalist(input, []);
        }
    }

    async function fetchCatalogStrings(path) {
        const response = await apiFetch(path);
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "CatÁ¡logo no disponible.");
        }
        return Array.isArray(payload.data) ? payload.data : [];
    }

    async function fetchCatalogObjects(path) {
        const response = await apiFetch(path);
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "CatÁ¡logo no disponible.");
        }
        return Array.isArray(payload.data) ? payload.data : [];
    }

    function fillInputDatalist(input, values) {
        const listId = input.getAttribute("list");
        const list = listId ? document.getElementById(listId) : null;
        if (!list) {
            return;
        }
        const unique = Array.from(new Set(values.map((value) => String(value || "").trim()).filter(Boolean))).slice(0, 80);
        list.innerHTML = unique.map((value) => `<option value="${escapeHtml(value)}"></option>`).join("");
    }

    function formatFieldValueForInput(field, value) {
        if (value === null || value === undefined) {
            return "";
        }
        if (field?.inputType === "date") {
            const text = String(value).trim();
            return text.length >= 10 ? text.slice(0, 10) : text;
        }
        return String(value);
    }

    function renderDynamicDbEditField(field, value) {
        const name = field.name;
        const label = displayDbFieldLabel(field);
        const required = field.required ? "required" : "";
        const autocompleteCatalog = autocompleteCatalogForField(name);
        const safeValue = escapeHtml(formatFieldValueForInput(field, value));

        if (field.inputType === "textarea") {
            return `<label class="field-group field-group--wide"><span>${escapeHtml(label)}</span><textarea name="${escapeHtml(name)}" ${required}>${safeValue}</textarea></label>`;
        }
        if (typeof field.custom === "function") {
            return field.custom(value);
        }
        if (name === "estado" || field.inputType === "select") {
            const selected = normalizeState(value || "");
            // align-self:start evita que se estire si la celda vecina ("Asignar equipo") crece
            return `<label class="field-group" style="align-self:start;"><span>${escapeHtml(label)}</span><select name="${escapeHtml(name)}" ${required}>${VALID_STATES.map((item) => `<option value="${item}" ${selected === item ? "selected" : ""}>${item}</option>`).join("")}</select></label>`;
        }

        const htmlType = field.inputType === "number" ? "number" : (field.inputType === "date" ? "date" : "text");
        if (autocompleteCatalog) {
            const listId = `edit_${name}_list`;
            return `<label class="field-group"><span>${escapeHtml(label)}</span><input type="${htmlType}" name="${escapeHtml(name)}" value="${safeValue}" list="${listId}" autocomplete="off" data-catalog="${autocompleteCatalog}" ${required}><datalist id="${listId}"></datalist></label>`;
        }
        return `<label class="field-group"><span>${escapeHtml(label)}</span><input type="${htmlType}" name="${escapeHtml(name)}" value="${safeValue}" ${required}></label>`;
    }

    function renderDynamicDbEditFieldGroups(fields, values) {
        const groups = groupDynamicDbFields(fields);
        return groups.map((group) => `
            <section class="equipment-field-section">
                <div class="equipment-field-section__header">
                    <h3>${escapeHtml(group.title)}</h3>
                </div>
                <div class="equipment-field-section__grid">
                    ${group.fields.map((field) => renderDynamicDbEditField(field, values?.[field.name])).join("")}
                </div>
            </section>
        `).join("");
    }

    function setupEditEquipmentAutocompletes() {
        document.querySelectorAll("#editEquipmentForm [data-catalog]").forEach((input) => {
            const load = debounce(() => loadNewEquipmentSuggestions(input), 220);
            input.addEventListener("input", load);
            loadNewEquipmentSuggestions(input);
        });
    }

    const INVENTORY_EXPORT_COLUMNS = [
        { key: "codigoSbai", label: "Código SBYE", weight: 1.1 },
        { key: "codigoMegan", label: "Código Megan", weight: 1.1 },
        { key: "descripcion", label: "Descripción", weight: 3.2 },
        { key: "tipo", label: "Tipo", weight: 1.2 },
        { key: "marca", label: "Marca", weight: 1.2 },
        { key: "modelo", label: "Modelo", weight: 1.6 },
        { key: "numeroSerie", label: "Número de serie", weight: 1.6 },
        { key: "custodio", label: "Custodio", weight: 2.0 },
        { key: "ubicacion", label: "Ubicación", weight: 2.2 },
        { key: "detalle", label: "Detalle", weight: 2.4 },
        { key: "estado", label: "Estado", weight: 1.4 },
        { key: "ip", label: "IP", weight: 1.0 },
        { key: "observacion", label: "Observaciones", weight: 2.0 }
    ];

    function openExportDialog(format) {
        const rows = state.filteredInventory;
        if (!rows.length) {
            showToast("Sin datos", "No hay resultados filtrados para exportar.", "info");
            return;
        }

        const selected = new Set((state.exportSelection?.keys || INVENTORY_EXPORT_COLUMNS.map((col) => col.key)));
        const isPdf = format === "pdf";
        const formatIcon = isPdf ? "📄" : "📊";
        const title = isPdf ? `${formatIcon} Exportar PDF` : `${formatIcon} Exportar Excel`;
//Modal de selecciòn de columnas a exportar
        openModal(
            title,
            `
                <div class="export-dialog">
                    <div class="export-columns-section">
                        <div class="export-section-header">
                            <span class="export-section-title">Campos a exportar</span>
                            <div class="export-quick-actions">
                                <button class="btn btn-secondary btn-sm" type="button" id="exportSelectAll">Todos</button>
                                <button class="btn btn-secondary btn-sm" type="button" id="exportSelectNone">Ninguno</button>
                            </div>
                        </div>
                        <div class="export-field-list">
                            ${INVENTORY_EXPORT_COLUMNS.map((col) => `
                                <label class="export-field-item">
                                    <input type="checkbox" class="export-field-checkbox" data-export-col="${escapeHtml(col.key)}" ${selected.has(col.key) ? "checked" : ""}>
                                    <span class="export-field-label">${escapeHtml(col.label)}</span>
                                </label>
                            `).join("")}
                        </div>
                        <small class="helper-text">Se exportarán los <strong>${rows.length}</strong> registros filtrados actualmente.</small>
                    </div>
                    <div class="export-preview-section">
                        <div class="export-section-header">
                            <span class="export-section-title">Previsualizaciòn <span class="export-preview-badge">(primeros 5 registros)</span></span>
                        </div>
                        <div class="export-preview-wrapper" id="exportPreviewWrapper"></div>
                    </div>
                </div>
            `,
            [
                { label: "Cancelar", className: "btn btn-secondary", onClick: closeModal },
                ...(isPdf ? [
                    {
                        label: "Imprimir",
                        className: "btn btn-secondary",
                        onClick: () => {
                            const keys = readSelectedExportKeys();
                            if (!keys.length) { showToast("Seleccione campos", "Debe seleccionar al menos un campo.", "info"); return; }
                            closeModal();
                            exportInventoryToPrint(keys);
                        }
                    },
                    {
                        label: "Descargar PDF",
                        className: "btn btn-primary",
                        onClick: () => {
                            const keys = readSelectedExportKeys();
                            state.exportSelection = { keys };
                            if (!keys.length) { showToast("Seleccione campos", "Debe seleccionar al menos un campo.", "info"); return; }
                            closeModal();
                            exportInventoryToPdf(keys);
                        }
                    }
                ] : [
                    {
                        label: "Exportar Excel",
                        className: "btn btn-primary",
                        onClick: () => {
                            const keys = readSelectedExportKeys();
                            state.exportSelection = { keys };
                            if (!keys.length) { showToast("Seleccione campos", "Debe seleccionar al menos un campo.", "info"); return; }
                            closeModal();
                            exportInventoryToExcel(keys);
                        }
                    }
                ])
            ],
            "modal--wide"
        );

        function renderExportPreview() {
            const keys = readSelectedExportKeys();
            const wrapper = document.getElementById("exportPreviewWrapper");
            if (!wrapper) return;
            if (!keys.length) {
                wrapper.innerHTML = '<p class="export-preview-empty">Seleccione al menos un campo para previsualizar.</p>';
                return;
            }
            const cols = INVENTORY_EXPORT_COLUMNS.filter((col) => keys.includes(col.key));
            const previewRows = rows.slice(0, 5);
            wrapper.innerHTML = `
                <div class="export-preview-scroll">
                    <table class="export-preview-table">
                        <thead>
                            <tr>${cols.map((col) => `<th>${escapeHtml(col.label)}</th>`).join("")}</tr>
                        </thead>
                        <tbody>
                            ${previewRows.map((item) => `
                                <tr>${cols.map((col) => `<td>${escapeHtml(inventoryExportValue(item, col.key) || "-")}</td>`).join("")}</tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        }

        document.getElementById("exportSelectAll")?.addEventListener("click", () => {
            setAllExportCheckboxes(true);
            renderExportPreview();
        });
        document.getElementById("exportSelectNone")?.addEventListener("click", () => {
            setAllExportCheckboxes(false);
            renderExportPreview();
        });
        document.querySelectorAll("[data-export-col]").forEach((input) => {
            input.addEventListener("change", renderExportPreview);
        });
        renderExportPreview();
    }

    function setAllExportCheckboxes(checked) {
        document.querySelectorAll("[data-export-col]").forEach((input) => {
            input.checked = Boolean(checked);
        });
    }

    function readSelectedExportKeys() {
        return Array.from(document.querySelectorAll("[data-export-col]"))
            .filter((input) => input.checked)
            .map((input) => input.dataset.exportCol)
            .filter(Boolean);
    }

    function inventoryExportValue(item, key) {
        if (key === "tipo") {
            return displayInventoryType(item);
        }
        if (key === "detalle") {
            return item.procesador || item.caracteristicas || "";
        }
        if (key === "ip") {
            return item.raw?.ip || item.ip || "";
        }
        if (key === "observacion") {
            return item.raw?.observacion || item.observacion || "";
        }
        return item[key] || "";
    }

    function exportInventoryToExcel(keys) {
        const rows = state.filteredInventory;
        if (!rows.length) {
            showToast("Sin datos", "No hay resultados filtrados para exportar.", "info");
            return;
        }

        const cols = INVENTORY_EXPORT_COLUMNS.filter((col) => keys.includes(col.key));
        const header = cols.map((col) => col.label);
        const bodyRows = rows.map((item) => cols.map((col) => inventoryExportValue(item, col.key)));
        const generatedAt = new Date().toLocaleString("es-EC");
        const documentHtml = `
            <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Calibri, Arial, sans-serif; color: #16324f; margin: 32px; background: #fff; }
                        .sheet-header { margin-bottom: 24px; padding-bottom: 16px; border-bottom: 2px solid #1565c0; }
                        .sheet-header h1 { margin: 0 0 6px; font-size: 20px; color: #0d47a1; font-weight: 700; }
                        .sheet-header .subtitle { margin: 0 0 10px; font-size: 13px; color: #4d6480; font-weight: 600; }
                        .sheet-meta { display: flex; gap: 24px; flex-wrap: wrap; margin-top: 8px; }
                        .sheet-meta span { font-size: 11px; color: #6b7a8d; background: #f0f6ff; border-radius: 4px; padding: 3px 8px; }
                        table { width: 100%; border-collapse: collapse; font-size: 12px; }
                        thead tr { background: #1565c0; }
                        th { background: #1565c0; color: #fff; font-weight: 700; border: 1px solid #0d47a1; padding: 10px 9px; text-align: left; white-space: nowrap; }
                        td { border: 1px solid #d0dcea; padding: 8px 9px; vertical-align: top; color: #2c3e50; }
                        tbody tr:nth-child(even) { background: #f3f8ff; }
                        tbody tr:hover { background: #e8f0fb; }
                        .sheet-footer { margin-top: 18px; font-size: 11px; color: #9aabb8; text-align: right; }
                    </style>
                </head>
                <body>
                    <div class="sheet-header">
                        <h1>Reporte de Inventario Tecnológico</h1>
                        <div class="subtitle">Dirección de Tecnologías de Información y Comunicación - SENADI</div>
                        <div class="sheet-meta">
                            <span>📅 Fecha: ${escapeHtml(generatedAt)}</span>
                            <span>📊 Registros exportados: ${rows.length}</span>
                            <span>📋 Campos seleccionados: ${cols.length}</span>
                        </div>
                    </div>
                    <table>
                        <thead><tr>${header.map((cell) => `<th>${escapeHtml(cell)}</th>`).join("")}</tr></thead>
                        <tbody>${bodyRows.map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell || "")}</td>`).join("")}</tr>`).join("")}</tbody>
                    </table>
                    <div class="sheet-footer">Generado por Sistema de Inventario DTIC · ${escapeHtml(generatedAt)}</div>
                </body>
            </html>`;
        downloadBlob(new Blob([`\ufeff${documentHtml}`], { type: "application/vnd.ms-excel" }), `reporte_inventario_${timestampForFile()}.xls`);
    }

    // Construye el HTML para la previsualización e impresión del reporte PDF, con estilos embebidos y formato adecuado para impresión en A4 horizontal.

    function buildReportHtml(cols, rows, now, dateStr) {
        const headerRow = cols.map((c) => "<th>" + c.label + "</th>").join("");
        const bodyRows = rows.map((item, i) => {
            const cells = cols.map((c) => "<td>" + (inventoryExportValue(item, c.key) || "-") + "</td>").join("");
            return "<tr class=\"" + (i % 2 === 0 ? "even" : "odd") + "\">" + cells + "</tr>";
        }).join("");
        return (
            "<div class=\"rpt-header\"><div>" +
            "<div class=\"rpt-org\">SENADI Dirección de Tecnologías de Información y Comunicación</div>" +
            "<div class=\"rpt-sub\">Reporte de Inventario Tecnológico</div>" +
            "</div><div class=\"rpt-meta\">" +
            "<div>Fecha: " + dateStr + "</div>" +
            "<div>Emitido: " + now + "</div>" +
            "<div>Total: <strong>" + rows.length + "</strong> registro(s)</div>" +
            "</div></div>" +
            "<table><thead><tr>" + headerRow + "</tr></thead><tbody>" + bodyRows + "</tbody></table>" +
            "<div class=\"rpt-footer\">" +
            "<span>SENADI" + now + "</span>" +
            "<span>" + rows.length + " registro(s) " + cols.length + " campo(s)</span>" +
            "</div>"
        );
    }

    const REPORT_CSS = [
        "* { box-sizing: border-box; margin: 0; padding: 0; }",
        "body, div { font-family: Arial, sans-serif; font-size: 9pt; color: #111; }",
        ".rpt-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 10px 0 8px; border-bottom: 2px solid #1565c0; margin-bottom: 10px; }",
        ".rpt-org { font-size: 12pt; font-weight: bold; color: #1565c0; }",
        ".rpt-sub { font-size: 9pt; color: #444; margin-top: 3px; }",
        ".rpt-meta { text-align: right; font-size: 8pt; color: #555; line-height: 1.7; }",
        "table { width: 100%; border-collapse: collapse; table-layout: auto; }",
        "thead tr { background: #1565c0; color: #fff; }",
        "thead th { padding: 5px 6px; font-size: 8pt; font-weight: bold; text-align: left; border: 1px solid #0d47a1; }",
        "tbody tr:nth-child(even) { background: #f0f5fb; }",
        "tbody tr:nth-child(odd)  { background: #ffffff; }",
        "tbody td { padding: 4px 6px; font-size: 8pt; border: 1px solid #cfd8e8; vertical-align: top; word-break: break-word; }",
        ".rpt-footer { margin-top: 12px; border-top: 1px solid #bbb; padding-top: 5px; font-size: 7.5pt; color: #666; display: flex; justify-content: space-between; }"
    ].join(" ");

    function exportInventoryToPdf(keys) {
        const rows = state.filteredInventory;
        if (!rows.length) {
            showToast("Sin datos", "No hay resultados filtrados para exportar.", "info");
            return;
        }
        const blob = buildInventoryPdfBlob(rows, keys);
        downloadBlob(blob, "reporte_inventario_" + timestampForFile() + ".pdf");
    }

    function exportInventoryToPrint(keys) {
        const rows = state.filteredInventory;
        if (!rows.length) {
            showToast("Sin datos", "No hay resultados filtrados para exportar.", "info");
            return;
        }
        const cols = INVENTORY_EXPORT_COLUMNS.filter((col) => keys.includes(col.key));
        const now = new Date().toLocaleString("es-EC");
        const dateStr = new Date().toLocaleDateString("es-EC", { year: "numeric", month: "long", day: "numeric" });
        const body = buildReportHtml(cols, rows, now, dateStr);
        const printCss = REPORT_CSS +
            " @media print { @page { margin:1.2cm; size:A4 landscape; } thead { display:table-header-group; } tr { page-break-inside:avoid; } }";
        const html = "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
            "<title>Reporte Inventario SENADI</title>" +
            "<style>" + printCss + "</style></head><body style='padding:16px'>" +
            body + "<script>window.onload=function(){window.focus();window.print();}<\/script></body></html>";
        const win = window.open("", "_blank", "width=1100,height=800,scrollbars=yes");
        if (!win) { showToast("Bloqueado", "Permite ventanas emergentes para imprimir.", "warning"); return; }
        win.document.open();
        win.document.write(html);
        win.document.close();
    }

    function buildInventoryPdfBlob(rows, keys) {
        const W = 842, H = 595, M = 20;
        const cols = INVENTORY_EXPORT_COLUMNS.filter((c) => keys.includes(c.key));
        const totalWeight = cols.reduce((s, c) => s + (c.weight || 1), 0) || 1;
        const tableW = W - M * 2;

        // Calcular anchos de columna
        const colWidths = cols.map((c) => Math.max(30, Math.floor(tableW * (c.weight || 1) / totalWeight)));
        const usedW = colWidths.reduce((s, w) => s + w, 0);
        colWidths[colWidths.length - 1] += tableW - usedW;
        const colX = [];
        let cx = M;
        colWidths.forEach((w) => { colX.push(cx); cx += w; });

        const HEADER_H = 14;
        const ROW_H = 12;
        const FS = 7;
        const FS_H = 7.5;
        const PAGE_TOP = H - M - 38;   // y donde empieza la cabecera de tabla
        const PAGE_BOT = M + 10;
        const rowsPerPage = Math.max(1, Math.floor((PAGE_TOP - PAGE_BOT - HEADER_H) / ROW_H));

        function pdfEsc(v) {
            return String(v || "-")
                .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
                .replace(/\\/g, "\\\\").replace(/\(/g, "\\(").replace(/\)/g, "\\)")
                .replace(/[\r\n\t]/g, " ").trim();
        }
        function trunc(v, w) {
            const s = pdfEsc(v);
            const max = Math.max(2, Math.floor(w / (FS * 0.52)) - 1);
            return s.length <= max ? s : s.slice(0, max - 1) + ".";
        }
        function truncH(v, w) {
            const s = pdfEsc(v);
            const max = Math.max(2, Math.floor(w / (FS_H * 0.52)) - 1);
            return s.length <= max ? s : s.slice(0, max - 1) + ".";
        }

        const objects = [];
        function addObj(content) { objects.push(content); return objects.length; }

        const fontId = addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        const fontBoldId = addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        const pageRefs = [];

        const now = pdfEsc(new Date().toLocaleString("es-EC"));
        const totalPages = Math.max(1, Math.ceil(rows.length / rowsPerPage));

        for (let p = 0; p < totalPages; p++) {
            const pageRows = rows.slice(p * rowsPerPage, (p + 1) * rowsPerPage);
            const ops = [];

            // --- Encabezado de página ---
            ops.push("0 g");
            ops.push("BT /F2 13 Tf " + M + " " + (H - M - 13) + " Td (Reporte de Inventario - SENADI DTIC) Tj ET");
            ops.push("BT /F1 8 Tf " + M + " " + (H - M - 25) + " Td (Emitido: " + now + "   Total: " + rows.length + " registros   Pagina " + (p + 1) + "/" + totalPages + ") Tj ET");

            // --- Cabecera de tabla (fondo azul) ---
            const thY = PAGE_TOP - HEADER_H;
            ops.push("0.18 0.38 0.75 rg");
            ops.push(M + " " + thY + " " + tableW + " " + HEADER_H + " re f");
            ops.push("1 g");
            cols.forEach((col, i) => {
                ops.push("BT /F2 " + FS_H + " Tf " + (colX[i] + 2) + " " + (thY + 4) + " Td (" + truncH(col.label.toUpperCase(), colWidths[i] - 4) + ") Tj ET");
            });

            // --- Filas de datos ---
            ops.push("0 g");
            pageRows.forEach((item, ri) => {
                const ry = thY - (ri + 1) * ROW_H;
                if (ri % 2 === 0) {
                    ops.push("0.93 0.96 0.99 rg");
                    ops.push(M + " " + ry + " " + tableW + " " + ROW_H + " re f");
                    ops.push("0 g");
                }
                cols.forEach((col, ci) => {
                    const val = inventoryExportValue(item, col.key);
                    ops.push("BT /F1 " + FS + " Tf " + (colX[ci] + 2) + " " + (ry + 3) + " Td (" + trunc(val, colWidths[ci] - 4) + ") Tj ET");
                });
            });

            // --- Grilla (lineas horizontales y verticales) ---
            ops.push("0.15 w");
            ops.push("0.4 0.4 0.4 RG");
            // Borde exterior
            const gridH = HEADER_H + pageRows.length * ROW_H;
            const gridY = thY - pageRows.length * ROW_H;
            ops.push(M + " " + gridY + " " + tableW + " " + gridH + " re S");
            // Lineas horizontales entre filas
            for (let r = 0; r <= pageRows.length; r++) {
                const ly = thY - r * ROW_H;
                ops.push(M + " " + ly + " m " + (M + tableW) + " " + ly + " l S");
            }
            // Lineas verticales entre columnas
            for (let ci = 1; ci < cols.length; ci++) {
                ops.push(colX[ci] + " " + gridY + " m " + colX[ci] + " " + PAGE_TOP + " l S");
            }

            // --- Pie de página ---
            ops.push("0 g");
            ops.push("BT /F1 7 Tf " + M + " " + (M + 2) + " Td (SENADI - DTIC | " + now + ") Tj ET");

            const stream = ops.join("\n");
            const streamId = addObj("<< /Length " + stream.length + " >>\nstream\n" + stream + "\nendstream");
            const pageId = addObj("<< /Type /Page /Parent PAGES_REF 0 R /MediaBox [0 0 " + W + " " + H + "] /Contents " + streamId + " 0 R /Resources << /Font << /F1 " + fontId + " 0 R /F2 " + fontBoldId + " 0 R >> >> >>");
            pageRefs.push(pageId);
        }

        const pagesId = addObj("<< /Type /Pages /Count " + pageRefs.length + " /Kids [" + pageRefs.map((id) => id + " 0 R").join(" ") + "] >>");
        const catalogId = addObj("<< /Type /Catalog /Pages " + pagesId + " 0 R >>");

        const normalized = objects.map((obj, idx) =>
            (idx + 1) + " 0 obj\n" + obj.replace(/PAGES_REF 0 R/g, pagesId + " 0 R") + "\nendobj\n"
        );

        let pdf = "%PDF-1.4\n";
        const offsets = [];
        normalized.forEach((obj) => { offsets.push(pdf.length); pdf += obj; });
        const xrefStart = pdf.length;
        pdf += "xref\n0 " + (normalized.length + 1) + "\n0000000000 65535 f \n";
        offsets.forEach((o) => { pdf += String(o).padStart(10, "0") + " 00000 n \n"; });
        pdf += "trailer\n<< /Size " + (normalized.length + 1) + " /Root " + catalogId + " 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF";
        return new Blob([pdf], { type: "application/pdf" });
    }

    function buildInventoryPdfBlob_UNUSED_OLD(rows, keys) {
        const pageWidth = 842;
        const pageHeight = 595;
        const margin = 28;
        const headerHeight = 64;
        const footerHeight = 22;
        const tableTop = pageHeight - headerHeight;
        const tableBottom = margin + footerHeight;
        const availableWidth = pageWidth - margin * 2;

        const cols = INVENTORY_EXPORT_COLUMNS.filter((col) => keys.includes(col.key));
        const totalWeight = cols.reduce((sum, col) => sum + (col.weight || 1), 0) || 1;

        const columns = cols.map((col, idx) => {
            const width = Math.max(48, Math.floor(availableWidth * ((col.weight || 1) / totalWeight)));
            const x = margin + cols.slice(0, idx).reduce((sum, prev) => sum + Math.max(48, Math.floor(availableWidth * ((prev.weight || 1) / totalWeight))), 0);
            return { key: col.key, title: col.label.toUpperCase(), x, width };
        });

        // Ajuste final de la ultima columna para cubrir el total.
        if (columns.length) {
            const used = columns[columns.length - 1].x - margin + columns[columns.length - 1].width;
            const diff = availableWidth - used;
            columns[columns.length - 1].width += diff;
        }

        const objects = [];
        const pageRefs = [];
        const fontId = addObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        const fontBoldId = addObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");

        function addObject(content) {
            objects.push(content);
            return objects.length;
        }

        function lineCapacity(width, fontSize) {
            // Aproximacion conservadora: Helvetica ~0.5*fontSize por caracter.
            return Math.max(6, Math.floor(width / (fontSize * 0.52)));
        }

        function wrapLines(value, maxChars) {
            const clean = String(value || "-").replace(/\s+/g, " ").trim();
            if (!clean) {
                return ["-"];
            }
            if (clean.length <= maxChars) {
                return [clean];
            }
            const words = clean.split(" ");
            const lines = [];
            let current = "";
            for (const word of words) {
                if (!current) {
                    current = word;
                    continue;
                }
                if ((current + " " + word).length <= maxChars) {
                    current += " " + word;
                    continue;
                }
                lines.push(current);
                current = word;
            }
            if (current) {
                lines.push(current);
            }
            // Hard-wrap para palabras extremadamente largas
            const finalLines = [];
            for (const line of lines) {
                if (line.length <= maxChars) {
                    finalLines.push(line);
                    continue;
                }
                for (let i = 0; i < line.length; i += maxChars) {
                    finalLines.push(line.slice(i, i + maxChars));
                }
            }
            return finalLines;
        }

        function drawText(content, fontRef, fontSize, x, y, text) {
            content.push(`BT /${fontRef} ${fontSize} Tf ${x} ${y} Td (${escapePdfText(text)}) Tj ET`);
        }

        function renderPage(pageRows, pageIndex, totalPages) {
            const content = [];
            // Header
            content.push("0 g");
            drawText(content, "F2", 16, margin, pageHeight - 34, "Reporte de Inventario Filtrado");
            drawText(content, "F1", 9, margin, pageHeight - 50, `Emitido: ${escapePdfText(new Date().toLocaleString("es-EC"))}`);
            drawText(content, "F1", 9, margin + 240, pageHeight - 50, `Total: ${rows.length}`);
            drawText(content, "F1", 9, pageWidth - margin - 110, pageHeight - 50, `Pagina ${pageIndex}/${totalPages}`);

            // Table header background
            const headerY = tableTop - 18;
            content.push("0.90 0.94 0.98 rg");
            content.push(`${margin} ${headerY - 6} ${pageWidth - margin * 2} 18 re f`);
            content.push("0 g");
            columns.forEach((col) => {
                drawText(content, "F2", 7.8, col.x + 2, headerY, col.title);
            });

            // Row rendering with wrapping (no truncation)
            let cursorY = headerY - 16;
            const fontSize = 7.2;
            const lineHeight = 9;
            const cellPadding = 2;

            for (let i = 0; i < pageRows.length; i += 1) {
                const item = pageRows[i];
                const wrapped = columns.map((col) => {
                    const raw = inventoryExportValue(item, col.key);
                    const cap = lineCapacity(col.width - cellPadding * 2, fontSize);
                    return wrapLines(raw || "-", cap);
                });
                const maxLines = Math.max(1, ...wrapped.map((lines) => lines.length));
                const rowHeight = Math.max(14, (maxLines * lineHeight) + 4);

                if (cursorY - rowHeight < tableBottom) {
                    break;
                }

                if (i % 2 === 0) {
                    content.push("0.97 0.98 1 rg");
                    content.push(`${margin} ${cursorY - rowHeight + 3} ${pageWidth - margin * 2} ${rowHeight} re f`);
                    content.push("0 g");
                }

                columns.forEach((col, colIndex) => {
                    const lines = wrapped[colIndex];
                    for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
                        drawText(
                            content,
                            "F1",
                            fontSize,
                            col.x + cellPadding,
                            cursorY - (lineIndex * lineHeight),
                            lines[lineIndex]
                        );
                    }
                });

                // Grid lines
                content.push("0.15 w");
                content.push(`${margin} ${cursorY - rowHeight + 3} ${pageWidth - margin * 2} ${rowHeight} re S`);
                columns.forEach((col) => {
                    content.push(`${col.x} ${cursorY - rowHeight + 3} 0 ${rowHeight} re S`);
                });
                cursorY -= rowHeight;
            }

            return content.join("\n");
        }

        // Paginacion real con wrapping (sin truncar)
        const fontSize = 7.2;
        const lineHeight = 9;
        const cellPadding = 2;
        const headerY = tableTop - 18;
        const startY = headerY - 16;

        const preparedRows = rows.map((item) => {
            const wrapped = columns.map((col) => {
                const raw = inventoryExportValue(item, col.key);
                const cap = lineCapacity(col.width - cellPadding * 2, fontSize);
                return wrapLines(raw || "-", cap);
            });
            const maxLines = Math.max(1, ...wrapped.map((lines) => lines.length));
            const rowHeight = Math.max(14, (maxLines * lineHeight) + 4);
            return { item, wrapped, rowHeight };
        });

        const realizedPages2 = [];
        let cursor = 0;
        while (cursor < preparedRows.length) {
            let y = startY;
            const page = [];
            while (cursor < preparedRows.length) {
                const next = preparedRows[cursor];
                if (y - next.rowHeight < tableBottom) {
                    break;
                }
                page.push(next);
                y -= next.rowHeight;
                cursor += 1;
            }
            if (!page.length) {
                page.push(preparedRows[cursor]);
                cursor += 1;
            }
            realizedPages2.push(page);
        }

        // Render final: recalcula cortando por espacio disponible real
        if (false) {
        const realizedPages = [];
        let index = 0;
        while (index < rows.length) {
            let end = Math.min(rows.length, index + 20);
            while (end > index + 1) {
                const attempt = rows.slice(index, end);
                const streamText = renderPage(attempt, 1, 1);
                // Si el stream no incluye cortes, se asume OK (el corte real lo hace por bottom).
                // Ajuste: renderPage se detiene por bottom sin señal, asi que medimos con un truco:
                // contamos cuantas filas entraron buscando "re S" de cada fila (aprox).
                const renderedRows = (streamText.match(/ re S/g) || []).length;
                // Header agrega 1 rect; cada fila agrega 1 rect, entonces filas ~= renderedRows-1.
                const rowsFit = Math.max(0, renderedRows - 1);
                if (rowsFit >= attempt.length) {
                    break;
                }
                end = index + rowsFit;
            }
            if (end <= index) {
                end = index + 1;
            }
            realizedPages.push(rows.slice(index, end));
            index = end;
        }

        }
        const totalPages = realizedPages2.length || 1;
        realizedPages2.forEach((pageRows, pageIdx) => {
            const content = [];
            content.push("0 g");
            drawText(content, "F2", 16, margin, pageHeight - 34, "Reporte de Inventario Filtrado");
            drawText(content, "F1", 9, margin, pageHeight - 50, `Emitido: ${escapePdfText(new Date().toLocaleString("es-EC"))}`);
            drawText(content, "F1", 9, margin + 240, pageHeight - 50, `Total: ${rows.length}`);
            drawText(content, "F1", 9, pageWidth - margin - 110, pageHeight - 50, `Pagina ${pageIdx + 1}/${totalPages}`);

            content.push("0.90 0.94 0.98 rg");
            content.push(`${margin} ${headerY - 6} ${pageWidth - margin * 2} 18 re f`);
            content.push("0 g");
            columns.forEach((col) => {
                drawText(content, "F2", 7.8, col.x + 2, headerY, col.title);
            });

            let cursorY = startY;
            pageRows.forEach((row, stripeIndex) => {
                if (stripeIndex % 2 === 0) {
                    content.push("0.97 0.98 1 rg");
                    content.push(`${margin} ${cursorY - row.rowHeight + 3} ${pageWidth - margin * 2} ${row.rowHeight} re f`);
                    content.push("0 g");
                }

                columns.forEach((col, colIndex) => {
                    const lines = row.wrapped[colIndex];
                    for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
                        drawText(content, "F1", fontSize, col.x + cellPadding, cursorY - (lineIndex * lineHeight), lines[lineIndex]);
                    }
                });

                content.push("0.15 w");
                content.push(`${margin} ${cursorY - row.rowHeight + 3} ${pageWidth - margin * 2} ${row.rowHeight} re S`);
                columns.forEach((col) => {
                    content.push(`${col.x} ${cursorY - row.rowHeight + 3} 0 ${row.rowHeight} re S`);
                });
                cursorY -= row.rowHeight;
            });

            const stream = content.join("\n");
            const streamId = addObject(`<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`);
            const pageId = addObject(`<< /Type /Page /Parent PAGES_REF 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}] /Contents ${streamId} 0 R /Resources << /Font << /F1 ${fontId} 0 R /F2 ${fontBoldId} 0 R >> >> >>`);
            pageRefs.push(pageId);
        });

        const pagesId = addObject(`<< /Type /Pages /Count ${pageRefs.length} /Kids [${pageRefs.map((id) => `${id} 0 R`).join(" ")}] >>`);
        const catalogId = addObject(`<< /Type /Catalog /Pages ${pagesId} 0 R >>`);

        const normalizedObjects = objects.map((object, idx) => {
            const objectId = idx + 1;
            return `${objectId} 0 obj\n${object.replace(/PAGES_REF 0 R/g, `${pagesId} 0 R`)}\nendobj\n`;
        });

        let pdf = "%PDF-1.4\n";
        const offsets = [0];
        normalizedObjects.forEach((object) => {
            offsets.push(pdf.length);
            pdf += object;
        });
        const xrefStart = pdf.length;
        pdf += `xref\n0 ${normalizedObjects.length + 1}\n`;
        pdf += "0000000000 65535 f \n";
        for (let i = 1; i < offsets.length; i += 1) {
            pdf += `${String(offsets[i]).padStart(10, "0")} 00000 n \n`;
        }
        pdf += `trailer\n<< /Size ${normalizedObjects.length + 1} /Root ${catalogId} 0 R >>\nstartxref\n${xrefStart}\n%%EOF`;
        return new Blob([pdf], { type: "application/pdf" });
    }

    function escapePdfText(value) {
        return String(value || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/\\/g, "\\\\")
            .replace(/\(/g, "\\(")
            .replace(/\)/g, "\\)")
            .replace(/\r|\n/g, " ");
    }

    function fitPdfText(value, maxLength) {
        const clean = String(value || "").replace(/\s+/g, " ").trim();
        if (clean.length <= maxLength) {
            return clean;
        }
        return `${clean.slice(0, Math.max(0, maxLength - 3))}...`;
    }

    function labelForColumn(key) {
        const labels = {
            codigoSbai: "Código SBYE",
            codigoMegan: "Código Megan",
            descripcion: "Descripción",
            tipo: "Tipo",
            marca: "Marca",
            modelo: "Modelo",
            numeroSerie: "Número de serie",
            custodio: "Custodio",
            ubicacion: "ubicación",
            ubicacionEdificio: "Edificio",
            ubicacionPiso: "Piso",
            ubicacionDireccion: "Dirección",
            estado: "Estado",
            procesador: "Detalle",
            caracteristicas: "Características",
            observacion: "Observaciones",
            sistemaOperativo: "SO",
            ram: "RAM",
            discoDuro: "Disco duro",
            linea: "Línea",
            imei: "IMEI",
            resolucion: "Resolución",
            conexion: "Conexión",
            tecnologia: "Tecnología",
            compatibilidad: "Compatibilidad",
            lumenes: "Lúmenes",
            codigo_megan: "Código Megan",
            codigo_sbye: "Código SBYE",
            sn: "Número de serie",
            fecha_ingreso: "Fecha de ingreso",
            costo: "Costo",
            ultima_actualizacion: "Última actualización",
            ultimaActualizacion: "Última actualización",
            ultimo_mantenimiento: "Último mantenimiento",
            ultimoMantenimiento: "Último mantenimiento",
            id_custodio_actual: "Custodio actual (ID)",
            id_ubicacion: "ubicación (ID)",
            tipo_periferico: "Tipo de periférico",
            tipo_impresora: "Tipo de impresora",
            codigo_anterior: "Código anterior",
            numero_contrato: "Número de contrato",
            numero_servicio: "Número de servicio",
            plan_comercial: "Plan comercial",
            estado_servicio: "Estado del servicio",
            anterior_custodio: "Custodio anterior",
            subtipo: "Subtipo",
            megas: "Megas",
            acreditacion: "Acreditación",
            anotaciones: "Anotaciones",
            acta_ugdt: "Acta UGDT",
            acta_ugad: "Acta UGAD"
        };
        return labels[key] || key;
    }

    // ---------- Overrides de roles y acciones ----------

    function defaultPermissionsByRole(roleName) {
        if (roleName === "admin") {
            return {
                puedeEditarTodos: true,
                puedeActualizarEstado: true,
                puedeVer: true,
                puedeCrearEquipo: true,
                puedeEditarCustodio: true,
                puedeExportarInventario: true,
                puedeVerHistorial: true
            };
        }
        if (roleName === "custodio") {
            return {
                puedeEditarTodos: false,
                puedeActualizarEstado: false,
                puedeVer: true,
                puedeCrearEquipo: false,
                puedeEditarCustodio: false,
                puedeExportarInventario: false,
                puedeVerHistorial: true
            };
        }
        return {
            puedeEditarTodos: false,
            puedeActualizarEstado: false,
            puedeVer: true,
            puedeCrearEquipo: true,
            puedeEditarCustodio: true,
            puedeExportarInventario: true,
            puedeVerHistorial: true
        };
    }

    function mapSession(data) {
        const rolRaw = String(data?.rol || "").trim().toUpperCase();
        const role = rolRaw.includes("ADMIN")
            ? "admin"
            : (rolRaw.includes("CUSTODIO") ? "custodio" : "tecnico");

        const roleLabel = role === "admin"
            ? "Administrador"
            : (role === "custodio" ? "Custodio" : "Técnicos");

        const fallbackName = data?.nombreCompleto || data?.usuario || data?.username || "Usuario";
        const permissions = Object.assign({}, defaultPermissionsByRole(role), data?.permisos || {});

        return {
            username: data?.usuario || data?.username || "usuario",
            displayName: fallbackName,
            role,
            roleLabel,
            accessRole: role === "admin" ? "ADMINISTRADOR" : (role === "custodio" ? "CUSTODIO" : "TECNICO"),
            idCustodio: data?.idCustodio ?? null,
            permissions,
            permisos: permissions,
            rolesDisponibles: normalizeAvailableRoles(data?.rolesDisponibles)
        };
    }
    //Inicio de sesiòn
    function buildDemoSession(username, password) {
        if (username === "admin" && password === "admin123") {
            return {
                username: "admin",
                displayName: "Administrador Demo",
                role: "admin",
                roleLabel: "Administrador",
                permissions: defaultPermissionsByRole("admin"),
                permisos: defaultPermissionsByRole("admin"),
                rolesDisponibles: ["ADMINISTRADOR", "CUSTODIO"]
            };
        }
        if ((username === "tecnico" || username === "usuario") && password === "tecnico123") {
            return {
                username: "tecnico",
                displayName: "Tecnico Demo",
                role: "tecnico",
                roleLabel: "Técnicos",
                permissions: defaultPermissionsByRole("tecnico"),
                permisos: defaultPermissionsByRole("tecnico"),
                rolesDisponibles: ["TECNICO", "CUSTODIO"]
            };
        }
        if (username === "custodio" && password === "custodio123") {
            return {
                username: "custodio",
                displayName: "Custodio Demo",
                role: "custodio",
                roleLabel: "Custodio",
                permissions: defaultPermissionsByRole("custodio"),
                permisos: defaultPermissionsByRole("custodio"),
                rolesDisponibles: ["CUSTODIO"]
            };
        }
        return null;
    }

    function initLogin() {
        startClock();
        document.getElementById("loginForm")?.addEventListener("submit", handleLogin);
        const demo = getDemoSession();
        if (demo) {
            redirectForRole(demo);
        }
    }

    async function resolveSession() {
        try {
            const response = await apiFetch("/login/actual");
            if (!response.ok) {
                throw new Error("Sin sesiòn");
            }
            const payload = await response.json();
            if (payload.success && payload.data) {
                const session = mapSession(payload.data);
                setDemoSession(session);
                return session;
            }
        } catch (error) {
            clearDemoSession();
            return null;
        }
        clearDemoSession();
        return null;
    }

    function redirectForRole(session) {
        const roleName = session?.role || "tecnico";
        let target;
        if (roleName === "admin") {
            target = `${basePrefix}/pages/dashboard.html`;
        } else if (roleName === "custodio") {
            target = `${basePrefix}/pages/custodio/dashboard.html`;
        } else {
            target = `${basePrefix}/pages/usuario/dashboard.html`;
        }
        window.location.href = target.replace("/pages/pages/", "/pages/");
    }

    function isRoleAllowedForPage(pageRole, sessionRole) {
        if (pageRole === "admin") {
            return sessionRole === "admin";
        }
        if (pageRole === "tecnico") {
            return sessionRole === "tecnico";
        }
        if (pageRole === "custodio") {
            return sessionRole === "custodio";
        }
        if (pageRole === "usuario") {
            return sessionRole === "tecnico" || sessionRole === "custodio";
        }
        return true;
    }

    async function initShell() {
        state.session = await resolveSession();
        if (!state.session) {
            window.location.href = `${basePrefix}/index.html`.replace("/pages/index.html", "/index.html");
            return;
        }
        if (state.session.role === "custodio" && ["acta-equipos", "acta-software", "acta-rc"].includes(page)) {
            window.location.href = `${actasBasePath()}/actas.html`;
            return;
        }
        if (!isRoleAllowedForPage(role, state.session.role)) {
            redirectForRole(state.session);
            return;
        }
        if (page === "busqueda") {
            const target = state.session?.role === "admin"
                ? `${basePrefix}/pages/inventario.html`
                : (state.session?.role === "custodio"
                    ? `${basePrefix}/pages/custodio/inventario.html`
                    : `${basePrefix}/pages/usuario/inventario.html`);
            window.location.href = target;
            return;
        }
        if (page === "nuevo-equipo" && !canCreateEquipment()) {
            redirectForRole(state.session);
            return;
        }
        renderShell();
        bindShellEvents();
        await loadInitialData();
    }

    function renderActaInventorySearchPanel() {
        if (window.SIActasView?.renderEquipmentSearch) {
            return window.SIActasView.renderEquipmentSearch({
                buildTypeOptions: buildActaTypeOptions,
                renderAutocompleteField,
                validStates: VALID_STATES
            });
        }

        return "";
    }

    function renderActaLicenciasSearchPanel() {
        if (window.SIActasView?.renderEquipmentSearch) {
            return window.SIActasView.renderEquipmentSearch({
                buildTypeOptions: () => ACTA_LICENCIAS_COMPUTADOR_TYPES
                    .map((key) => `<option value="${key}">${TYPE_CONFIG[key].label}</option>`)
                    .join(""),
                renderAutocompleteField,
                validStates: [
                    "OPERATIVO",
                    "NO OPERATIVO"
                ]
            });
        }

        return "";
    }

    function prepareActaEquiposLayout() {
        const headerText = document.querySelector(".acta-form-panel .inventory-header p");

        if (headerText) {
            headerText.remove();
        }

        document.querySelector(".acta-subtabs")?.remove();

        const oldSearch = document.querySelector(".acta-search-panel");

        if (oldSearch) {
            oldSearch.outerHTML = renderActaInventorySearchPanel();
        }

        const exportButton = document.getElementById("actaPcExportDocxButton");

        if (exportButton) {
            exportButton.textContent = "Exportar";
        }

        const resetButton = document.getElementById("actaPcResetButton");

        if (resetButton) {
            resetButton.textContent = "Eliminar";
        }
    }

    function actaFilterIds() {
        return [
            "actaFilterCodigoSbai",
            "actaFilterCodigoMegan",
            "actaFilterDescripcion",
            "actaFilterMarca",
            "actaFilterModelo",
            "actaFilterSerie",
            "actaFilterCustodio",
            "actaFilterEdificio",
            "actaFilterPiso",
            "actaFilterDireccion"
        ];
    }

    function getActaSelectedType() {
        return document.getElementById("actaFilterTipo")?.value || "";
    }

    function getActaEquipoItems(type = getActaSelectedType()) {
        const normalizedType = String(type || "").toLowerCase();
        return state.inventory.filter((item) => {
            const itemTipo = String(item.tipo || "").toLowerCase();
            if (normalizedType) {
                return itemTipo === normalizedType;
            }
            return ACTA_EQUIPO_TYPES.includes(itemTipo);
        });
    }

    function bindEntregaNombreOtroEvent(selectId, inputId) {
        document.getElementById(selectId)?.addEventListener("change", (e) => {
            const otroInput = document.getElementById(inputId);
            if (e.target.value === "OTRO") {
                otroInput?.classList.remove("hidden");
                otroInput?.setAttribute("required", "required");
                otroInput?.focus();
            } else {
                otroInput?.classList.add("hidden");
                otroInput?.removeAttribute("required");
                if (otroInput) otroInput.value = "";
            }
        });
    }

    function getEntregaNombreValue(selectId, inputId) {
        const selVal = document.getElementById(selectId)?.value || "";
        if (selVal === "OTRO") {
            return document.getElementById(inputId)?.value.trim() || "";
        }
        return selVal;
    }

    // Validación común de todas las actas (incluye licencias):
    // la fecha de RECIBE no puede ser anterior a la fecha de ENTREGA.
    // Aplica a cada par de campos "...EntregaFecha" / "...RecibeFecha".
    function syncActaFechasRecepcion() {
        document.querySelectorAll('input[type="date"][id$="RecibeFecha"]').forEach((recibe) => {
            const entrega = document.getElementById(recibe.id.replace(/RecibeFecha$/, "EntregaFecha"));
            if (!entrega) {
                return;
            }
            // El mínimo del calendario solo se actualiza si cambió y el usuario
            // no está escribiendo en ese campo: reasignarlo mientras se teclea
            // la fecha hace que el navegador borre lo escrito.
            const minimo = entrega.value || "";
            if (recibe.min !== minimo && document.activeElement !== recibe) {
                recibe.min = minimo;
            }
            const invalida = Boolean(entrega.value && recibe.value && recibe.value < entrega.value);
            recibe.setCustomValidity(invalida
                ? "La fecha de recepción debe ser igual o posterior a la fecha de entrega."
                : "");
        });
    }

    // Se revalida al cambiar cualquier fecha y justo antes de cada clic
    // (Previsualizar / Exportar llaman a form.reportValidity()).
    ["input", "change", "click"].forEach((tipoEvento) => {
        document.addEventListener(tipoEvento, syncActaFechasRecepcion, true);
    });

    // ── Fechas de actividades de mantenimiento (PC, impresora, escáner,
    //    teléfono, proyector) ──────────────────────────────────────────────
    // Usan el calendario flatpickr con el diseño del nativo y los botones
    // "Borrar" y "Hoy". Al elegir una fecha se copia a las demás fechas vacías
    // de la misma tabla, porque casi siempre es la misma; el botón "Borrar
    // todo" del encabezado FECHA las vacía para volver a elegir.
    const FECHA_ACTIVIDAD_REGEX = /^(acta\w*ActividadFecha)\d+$/;
    const FLATPICKR_BASE = "https://cdnjs.cloudflare.com/ajax/libs/flatpickr/4.6.13";

    function camposFechaActividad(prefijo) {
        const patron = new RegExp(`^${prefijo}\\d+$`);
        return [...document.querySelectorAll("input[id]")].filter((campo) => patron.test(campo.id));
    }

    function ponerFechaActividad(campo, valor) {
        if (campo._flatpickr) {
            campo._flatpickr.setDate(valor || null, false);
        } else {
            campo.value = valor || "";
        }
    }

    function autocompletarFechasActividades(event) {
        const campo = event.target;
        const coincidencia = FECHA_ACTIVIDAD_REGEX.exec(campo?.id || "");
        if (!coincidencia || !campo.value || Number(campo.value.slice(0, 4)) < 1900) {
            return;
        }
        camposFechaActividad(coincidencia[1]).forEach((otro) => {
            if (otro !== campo && !otro.value) {
                ponerFechaActividad(otro, campo.value);
            }
        });
    }

    function borrarTodasFechasActividad(prefijo) {
        camposFechaActividad(prefijo).forEach((campo) => ponerFechaActividad(campo, ""));
    }

    document.addEventListener("change", autocompletarFechasActividades);

    // Al limpiar un formulario (botón Eliminar) también se limpia el calendario.
    document.addEventListener("reset", (event) => {
        setTimeout(() => {
            event.target.querySelectorAll?.("input[id]").forEach((campo) => {
                if (campo._flatpickr && FECHA_ACTIVIDAD_REGEX.test(campo.id)) {
                    campo._flatpickr.clear(false);
                }
            });
        }, 0);
    });

    // ── Validación de campos de texto en todas las actas y licencias ────────
    // Cada regla indica qué caracteres no se pueden escribir, el máximo de
    // caracteres y si el texto pasa a mayúsculas al terminar de escribir.
    // El correo no tiene regla (se deja como lo escriban).
    const LETRAS_PUNTO_NO_PERMITIDOS = /[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ. ]/g;
    const LETRAS_NUMEROS_NO_PERMITIDOS = /[^A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ ]/g;
    const REGLAS_CAMPOS_ACTA = [
        // Nombre (funcionario, recibe, "OTRO" de entrega) y cargo: letras, espacios y punto
        { patron: /^acta\w*(FuncionarioNombre|RecibeNombre|EntregaNombreOtro|FuncionarioCargo)$/, noPermitidos: LETRAS_PUNTO_NO_PERMITIDOS, mayusculas: true },
        // Nº de extensión: solo números, máximo 4
        { patron: /^acta\w*FuncionarioExtension$/, noPermitidos: /\D/g, maximo: 4 },
        // Área y edificio: letras, números y espacios, sin símbolos
        { patron: /^acta\w*Funcionario(Area|Edificio)$/, noPermitidos: LETRAS_NUMEROS_NO_PERMITIDOS, mayusculas: true },
        // Estado y observación de actividades: solo mayúsculas
        { patron: /^acta\w*Actividad(Estado|Observacion)\d+$/, mayusculas: true }
    ];

    function reglaCampoActa(campo) {
        if (!campo || !/^(INPUT|TEXTAREA)$/.test(campo.tagName) || campo.readOnly) {
            return null;
        }
        const regla = REGLAS_CAMPOS_ACTA.find((r) => r.patron.test(campo.id || ""));
        if (regla) {
            return regla;
        }
        // Tabla EQUIPOS (tipo, marca, modelo, serial, código): solo mayúsculas
        if (campo.type === "text" || campo.tagName === "TEXTAREA" || !campo.getAttribute("type")) {
            return campo.closest?.("table.acta-table--equipos") ? { mayusculas: true } : null;
        }
        return null;
    }

    document.addEventListener("input", (event) => {
        const campo = event.target;
        const regla = reglaCampoActa(campo);
        if (!regla || (!regla.noPermitidos && !regla.maximo)) {
            return;
        }
        let limpio = regla.noPermitidos ? campo.value.replace(regla.noPermitidos, "") : campo.value;
        if (regla.maximo) {
            limpio = limpio.slice(0, regla.maximo);
        }
        if (limpio !== campo.value) {
            const posicion = Math.max(0, (campo.selectionStart || 0) - (campo.value.length - limpio.length));
            campo.value = limpio;
            campo.setSelectionRange(posicion, posicion);
        }
    });

    // Estado de actividades: al terminar de escribir se copia (completo) a los
    // demás estados vacíos de la misma tabla; si luego se corrige uno, los
    // demás ya no cambian porque ya no están vacíos.
    const ESTADO_ACTIVIDAD_REGEX = /^(acta\w*ActividadEstado)\d+$/;

    function camposEstadoActividad(prefijo) {
        const patron = new RegExp(`^${prefijo}\\d+$`);
        return [...document.querySelectorAll("input[id]")].filter((campo) => patron.test(campo.id));
    }

    document.addEventListener("change", (event) => {
        const campo = event.target;
        const regla = reglaCampoActa(campo);
        if (regla?.mayusculas) {
            campo.value = campo.tagName === "TEXTAREA"
                ? campo.value.toUpperCase()
                : campo.value.replace(/\s+/g, " ").trim().toUpperCase();
        }
        const estado = ESTADO_ACTIVIDAD_REGEX.exec(campo?.id || "");
        if (estado && campo.value) {
            camposEstadoActividad(estado[1]).forEach((otro) => {
                if (otro !== campo && !otro.value) {
                    otro.value = campo.value;
                }
            });
        }
    });

    // Botones "Borrar todo" de los encabezados FECHA y ESTADO (solo existen en
    // el formulario; no salen en la previsualización ni en el Word/PDF).
    document.addEventListener("click", (event) => {
        const botonEstados = event.target.closest?.("[data-borrar-estados]");
        if (botonEstados) {
            camposEstadoActividad(botonEstados.dataset.borrarEstados).forEach((campo) => {
                campo.value = "";
            });
        }
        const botonFechas = event.target.closest?.("[data-borrar-fechas]");
        if (botonFechas) {
            borrarTodasFechasActividad(botonFechas.dataset.borrarFechas);
        }
    });

    async function initCalendarioFechasActividades() {
        const campos = [...document.querySelectorAll('input[type="date"]')]
            .filter((campo) => FECHA_ACTIVIDAD_REGEX.test(campo.id));
        if (!campos.length) {
            return;
        }
        try {
            if (!document.querySelector(`link[href="${FLATPICKR_BASE}/flatpickr.min.css"]`)) {
                const estilo = document.createElement("link");
                estilo.rel = "stylesheet";
                estilo.href = `${FLATPICKR_BASE}/flatpickr.min.css`;
                document.head.appendChild(estilo);
            }
            await loadScriptOnce(`${FLATPICKR_BASE}/flatpickr.min.js`);
            await loadScriptOnce(`${FLATPICKR_BASE}/l10n/es.min.js`);
        } catch (error) {
            // Sin conexión al CDN se mantiene el calendario nativo.
            return;
        }
        campos.forEach((campo) => {
            if (campo._flatpickr) {
                return;
            }
            const prefijo = FECHA_ACTIVIDAD_REGEX.exec(campo.id)[1];
            // Diseño igual al calendario nativo del navegador: días con una
            // letra (L M X J V S D) y título "octubre de 2026" con flechas ↑ ↓.
            const localeEs = window.flatpickr.l10ns?.es || window.flatpickr.l10ns.default;
            const localeActa = Object.assign({}, localeEs, {
                firstDayOfWeek: 1,
                weekdays: {
                    shorthand: ["D", "L", "M", "X", "J", "V", "S"],
                    longhand: localeEs.weekdays.longhand
                }
            });
            const actualizarTitulo = (fp) => {
                const titulo = fp.calendarContainer.querySelector(".acta-fp-titulo");
                if (titulo) {
                    titulo.textContent = `${localeActa.months.longhand[fp.currentMonth].toLowerCase()} de ${fp.currentYear} ▾`;
                }
            };
            window.flatpickr(campo, {
                locale: localeActa,
                monthSelectorType: "static",
                onMonthChange: (_, __, fp) => actualizarTitulo(fp),
                onYearChange: (_, __, fp) => actualizarTitulo(fp),
                onOpen: (_, __, fp) => actualizarTitulo(fp),
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "d/m/Y",
                altInputClass: "acta-input acta-fecha-actividad",
                allowInput: true,
                disableMobile: true,
                onReady: (_, __, fp) => {
                    fp.altInput.placeholder = "dd/mm/aaaa";
                    // Fecha escrita a mano (dd/mm/aaaa): se aplica al salir del
                    // campo o con Enter, igual que si se eligiera en el calendario.
                    const aplicarFechaEscrita = () => {
                        const texto = fp.altInput.value.trim();
                        // Si el texto es la misma fecha ya elegida no se hace nada:
                        // volver a aplicarla redibuja el calendario justo cuando el
                        // usuario hace clic en un día y ese primer clic se perdía.
                        const actual = fp.selectedDates[0] ? fp.formatDate(fp.selectedDates[0], "d/m/Y") : "";
                        if (texto === actual) {
                            return;
                        }
                        if (!texto) {
                            if (fp.input.value) fp.clear();
                            return;
                        }
                        const fecha = fp.parseDate(texto, "d/m/Y");
                        if (fecha && !Number.isNaN(fecha.getTime()) && fecha.getFullYear() >= 1900) {
                            fp.setDate(fecha, true);
                        }
                    };
                    fp.altInput.addEventListener("blur", aplicarFechaEscrita);
                    fp.altInput.addEventListener("keydown", (event) => {
                        if (event.key === "Enter") {
                            event.preventDefault();
                            aplicarFechaEscrita();
                            fp.close();
                        }
                    });
                    const pie = document.createElement("div");
                    pie.className = "acta-fp-pie";
                    const boton = (texto, accion) => {
                        const b = document.createElement("button");
                        b.type = "button";
                        b.textContent = texto;
                        b.addEventListener("click", () => {
                            accion();
                            fp.close();
                        });
                        pie.appendChild(b);
                    };
                    boton("Borrar", () => fp.clear());
                    boton("Hoy", () => fp.setDate(new Date(), true));
                    fp.calendarContainer.appendChild(pie);

                    // Encabezado como el nativo: "mes de año ▾" y flechas ↑ ↓.
                    fp.calendarContainer.classList.add("acta-fp");
                    const cabecera = document.createElement("div");
                    cabecera.className = "acta-fp-cabecera";
                    const titulo = document.createElement("span");
                    titulo.className = "acta-fp-titulo";
                    const flecha = (texto, delta, etiqueta) => {
                        const b = document.createElement("button");
                        b.type = "button";
                        b.className = "acta-fp-flecha";
                        b.textContent = texto;
                        b.setAttribute("aria-label", etiqueta);
                        b.addEventListener("click", () => fp.changeMonth(delta));
                        return b;
                    };
                    cabecera.append(titulo, flecha("↑", -1, "Mes anterior"), flecha("↓", 1, "Mes siguiente"));
                    fp.calendarContainer.insertBefore(cabecera, fp.calendarContainer.firstChild);
                    actualizarTitulo(fp);
                }
            });
        });
    }

    function loadActaPcInitialData() {
        const select = document.getElementById("actaPcSelector");
        if (!select) {
            return;
        }
        const items = getActaEquipoItems();
        select.innerHTML = '<option value="">Seleccione un equipo desde el buscador</option>' + items.map((item) => `
            <option value="${item.id}">${escapeHtml(actaEquipoOptionLabel(item))}</option>
        `).join("");
        renderActaEquipoResults([]);

        const today = new Date().toISOString().slice(0, 10);
        ["actaEntregaFecha", "actaRecibeFecha"].forEach((id) => {
            const input = document.getElementById(id);
            if (input && !input.value) {
                input.value = today;
            }
        });
        if (state.session?.displayName) {
            const entrega = document.getElementById("actaEntregaNombre");
            if (entrega && !entrega.value) {
                entrega.value = state.session.displayName;
            }
        }
    }



function bindActaLicenciasEvents() {
    actaFilterIds().forEach((id) => {
        document.getElementById(id)?.addEventListener("input", () => {
            updateAutocompleteForInput(id);
        });
    });

    document.getElementById("actaEquipoBuscarButton")
        ?.addEventListener("click", runActaLicenciasSearch);

    document.querySelector(".acta-search-panel")
        ?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                runActaLicenciasSearch();
            }
        });

    document.getElementById("actaLicPreviewButton")
        ?.addEventListener("click", openActaLicenciasPreview);

    document.getElementById("actaLicExportButton")
        ?.addEventListener("click", () => exportActaLicencias("docx"));

    document.getElementById("actaLicExportPdfButton")
        ?.addEventListener("click", () => exportActaLicencias("pdf"));

    document.getElementById("actaLicDeleteButton")
        ?.addEventListener("click", resetActaLicenciasForm);

    // Permitir escribir otro nombre en ENTREGA
    bindEntregaNombreOtroEvent(
        "actaLicEntregaNombre",
        "actaLicEntregaNombreOtro"
    );

    // Colocar la fecha actual en ENTREGA y RECIBE
    const today = new Date().toISOString().slice(0, 10);

    ["actaLicEntregaFecha", "actaLicRecibeFecha"].forEach((id) => {
        const input = document.getElementById(id);
        if (input && !input.value) {
            input.value = today;
        }
    });
}


// Los filtros aceptan datos de la licencia o de su PC/laptop:
// - si coinciden con una licencia, se muestra esa licencia;
// - si coinciden con una PC/laptop, se muestran todas las licencias asignadas a ese equipo.
// El filtro Tipo limita al tipo de equipo al que está asignada la licencia.
function runActaLicenciasSearch() {
    const { tipo, ...criteria } = collectActaSearchCriteria();
    const tipoComputador = String(tipo || "").toLowerCase();

    const computadoresPorId = new Map(state.inventory
        .filter((item) => ACTA_LICENCIAS_COMPUTADOR_TYPES.includes(String(item.tipo || "").toLowerCase()))
        .map((item) => [String(item.id), item]));
    const licencias = state.inventory.filter((item) => String(item.tipo || "").toLowerCase() === "licencia");

    const cumpleTipo = (licencia) => {
        if (!tipoComputador) {
            return true;
        }
        const computador = computadoresPorId.get(String(licencia.idComputador));
        return String(computador?.tipo || "").toLowerCase() === tipoComputador;
    };

    const idsComputadores = new Set(Array.from(computadoresPorId.values())
        .filter((computador) => !tipoComputador || String(computador.tipo || "").toLowerCase() === tipoComputador)
        .filter((computador) => hasActaCriteria(criteria) && matchesActaCriteria(computador, criteria))
        .map((computador) => String(computador.id)));

    const filtered = licencias.filter((licencia) => cumpleTipo(licencia) && (
        matchesActaCriteria(licencia, criteria) || idsComputadores.has(String(licencia.idComputador))
    ));

    renderActaLicenciasResults(filtered, computadoresPorId);
}

function hasActaCriteria(criteria) {
    return Object.values(criteria).some((value) => String(value || "").trim());
}


function renderActaLicenciasResults(items, computadoresPorId = new Map()) {
    const container = document.getElementById("actaEquipoResultados");

    if (!container) {
        return;
    }

    if (!items.length) {
        container.classList.remove("hidden");
        container.innerHTML = `
            <div class="empty-state">
                No se encontraron licencias con los criterios ingresados.
            </div>
        `;
        return;
    }

    document.getElementById("actaLicenciasForm")?.classList.add("hidden");

    container.classList.remove("hidden");

    container.innerHTML = `
        <div class="acta-search-results__meta">
            ${items.length} licencia(s) encontrada(s). Seleccione una para cargar el acta.
        </div>

        <div class="acta-search-results__list">
            ${items.map((item) => `
                <button
                    type="button"
                    class="acta-search-card"
                    data-acta-licencia-select="${item.id}"
                >
                    <strong>
                        ${escapeHtml(
                            item.codigoSbai ||
                            item.codigoMegan ||
                            `ID ${item.id}`
                        )}
                        · Licencia
                    </strong>

                    <span>
                        ${escapeHtml(
                            [item.marca, item.modelo, item.numeroSerie]
                                .filter(Boolean)
                                .join(" / ") ||
                            "Sin marca/modelo registrado"
                        )}
                    </span>

                    <span>
                        Custodio:
                        ${escapeHtml(item.custodio || "-")}
                        · Edificio:
                        ${escapeHtml(item.ubicacionEdificio || "-")}
                    </span>

                    <span>
                        Estado:
                        ${escapeHtml(item.estado || "-")}
                    </span>

                    <span>
                        Equipo asignado:
                        ${escapeHtml(describirComputadorLicencia(computadoresPorId.get(String(item.idComputador))))}
                    </span>
                </button>
            `).join("")}
        </div>
    `;

    container
        .querySelectorAll("[data-acta-licencia-select]")
        .forEach((button) => {
            button.addEventListener("click", () => {
                selectActaLicencia(button.dataset.actaLicenciaSelect);
            });
        });
}

// ===== Tabla EQUIPOS del acta de licencias: una fila por cada licencia del equipo =====

// Si la licencia está asignada a una PC/laptop, el acta incluye todas las licencias de ese equipo
// (la seleccionada primero); si no, solo la seleccionada.
function obtenerLicenciasDelMismoEquipo(licencia) {
    if (licencia.idComputador == null) {
        return [licencia];
    }
    const otras = state.inventory.filter((row) =>
        String(row.tipo || "").toLowerCase() === "licencia"
        && String(row.idComputador) === String(licencia.idComputador)
        && String(row.id) !== String(licencia.id));
    return [licencia, ...otras];
}

function licenciaParaActa(item) {
    return {
        tipo: "LICENCIA",
        marca: item.marca || "",
        modelo: item.modelo || "",
        serial: item.numeroSerie || "",
        codigo: item.codigoSbai || item.codigoMegan || ""
    };
}

function renderActaLicEquipoRow(licencia = {}) {
    const valor = (campo) => escapeHtml(licencia[campo] || "");
    return `
        <tr data-acta-lic-row>
            <td><input class="acta-input" data-lic-field="tipo" type="text" value="${valor("tipo") || "LICENCIA"}" readonly></td>
            <td><input class="acta-input" data-lic-field="marca" type="text" value="${valor("marca")}"></td>
            <td><input class="acta-input" data-lic-field="modelo" type="text" value="${valor("modelo")}"></td>
            <td><input class="acta-input" data-lic-field="serial" type="text" value="${valor("serial")}"></td>
            <td><input class="acta-input" data-lic-field="codigo" type="text" value="${valor("codigo")}" required></td>
        </tr>`;
}

function renderActaLicEquiposRows(licencias) {
    const body = document.getElementById("actaLicEquiposBody");
    if (body) {
        body.innerHTML = (licencias.length ? licencias : [{}]).map(renderActaLicEquipoRow).join("");
    }
}

function collectActaLicEquiposRows() {
    return Array.from(document.querySelectorAll("#actaLicEquiposBody [data-acta-lic-row]")).map((row) => {
        const leer = (campo) => row.querySelector(`[data-lic-field="${campo}"]`)?.value.trim() || "";
        return {
            tipo: leer("tipo") || "LICENCIA",
            marca: leer("marca"),
            modelo: leer("modelo"),
            serial: leer("serial"),
            codigo: leer("codigo")
        };
    });
}

function describirComputadorLicencia(computador) {
    if (!computador) {
        return "Sin asignar";
    }
    const codigo = computador.codigoSbai || computador.codigoMegan || computador.ip || `ID ${computador.id}`;
    return `${TYPE_CONFIG[String(computador.tipo || "").toLowerCase()]?.label || computador.tipo} ${codigo}`;
}

function autofillActaLicenciasForm(item) {
    setInputValue(
        "actaLicFuncionarioNombre",
        item.custodio || ""
    );

    setInputValue(
        "actaLicFuncionarioArea",
        item.ubicacionDireccion || item.ubicacion || ""
    );

    setInputValue(
        "actaLicFuncionarioEdificio",
        item.ubicacionEdificio || ""
    );

    renderActaLicEquiposRows(obtenerLicenciasDelMismoEquipo(item).map(licenciaParaActa));

    setInputValue(
        "actaLicRecibeNombre",
        item.custodio || ""
    );
}

function selectActaLicencia(id) {
    const item = state.inventory.find(
        (row) => String(row.id) === String(id)
    );

    if (!item) {
        return;
    }

    autofillActaLicenciasForm(item);
    const totalLicencias = obtenerLicenciasDelMismoEquipo(item).length;

    const resultsContainer =
        document.getElementById("actaEquipoResultados");

    if (resultsContainer) {
        resultsContainer.classList.add("hidden");
        resultsContainer.innerHTML = "";
    }

    const form =
        document.getElementById("actaLicenciasForm");

    if (form) {
        form.dataset.licenciaId = item.id;
        form.classList.remove("hidden");
    }

    showToast(
        "Licencia seleccionada",
        totalLicencias > 1
            ? `El acta se autocompletó con las ${totalLicencias} licencias asignadas al equipo.`
            : "El acta se autocompletó con los datos de la licencia.",
        "success"
    );
}


function buildActaLicenciasPayload() {
    const form =
        document.getElementById("actaLicenciasForm");
    const licencias = collectActaLicEquiposRows();

    return {
        licenciaSeleccionada:
            form?.dataset.licenciaId || "",

        funcionario: {
            nombre:
                document.getElementById("actaLicFuncionarioNombre")?.value.trim() || "",

            cargo:
                document.getElementById("actaLicFuncionarioCargo")?.value.trim() || "",

            extension:
                document.getElementById("actaLicFuncionarioExtension")?.value.trim() || "",

            correo:
                document.getElementById("actaLicFuncionarioCorreo")?.value.trim() || "",

            area:
                document.getElementById("actaLicFuncionarioArea")?.value.trim() || "",

            edificio:
                document.getElementById("actaLicFuncionarioEdificio")?.value.trim() || ""
        },

        // Todas las filas de la tabla EQUIPOS; "licencia" (la primera) se usa para el nombre del archivo
        licencias: licencias,

        licencia: licencias[0] || {},

        certificacion:
            ACTA_LICENCIAS_CERTIFICATION_TEXT,

        entrega: {
            nombre:
                getEntregaNombreValue(
                    "actaLicEntregaNombre",
                    "actaLicEntregaNombreOtro"
                ),

            firma:
                document.getElementById("actaLicEntregaFirma")?.value.trim() || "",

            fecha:
                document.getElementById("actaLicEntregaFecha")?.value || ""
        },

        recibe: {
            nombre:
                document.getElementById("actaLicRecibeNombre")?.value.trim() || "",

            firma:
                document.getElementById("actaLicRecibeFirma")?.value.trim() || "",

            fecha:
                document.getElementById("actaLicRecibeFecha")?.value || ""
        }
    };
}


function renderActaLicenciasPreview(payload) {
    const funcionario = payload.funcionario || {};
    const licencias = payload.licencias?.length ? payload.licencias : [payload.licencia || {}];
    const entrega = payload.entrega || {};
    const recibe = payload.recibe || {};

    return `
        <div class="acta-sheet acta-sheet--preview">

            <div class="acta-sheet__header">
                <img src="${actaAssetPath("logo_ecuador_nuevo.png")}" alt="República del Ecuador" class="acta-sheet__logo acta-sheet__logo--ecuador">
                <img src="${actaAssetPath("logo_senadi_nuevo.png")}" alt="Servicio Nacional de Derechos Intelectuales" class="acta-sheet__logo acta-sheet__logo--senadi">
            </div>

            <div class="acta-sheet__titles">
                <h3>SERVICIO NACIONAL DE DERECHOS INTELECTUALES</h3>
                <h4>DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN</h4>
                <h2>FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS</h2>
            </div>

            <table class="acta-table">
                <tr>
                    <th colspan="6">DATOS DEL FUNCIONARIO SENADI</th>
                </tr>

                <tr>
                    <td class="acta-table__label">NOMBRE</td>
                    <td>${escapeHtml(funcionario.nombre || "")}</td>

                    <td class="acta-table__label">CARGO</td>
                    <td>${escapeHtml(funcionario.cargo || "")}</td>

                    <td class="acta-table__label">Nº EXT.</td>
                    <td>${escapeHtml(funcionario.extension || "")}</td>
                </tr>

                <tr>
                    <td class="acta-table__label">CORREO</td>
                    <td>${escapeHtml(funcionario.correo || "")}</td>

                    <td class="acta-table__label">ÁREA</td>
                    <td>${escapeHtml(funcionario.area || "")}</td>

                    <td class="acta-table__label">EDIFICIO</td>
                    <td>${escapeHtml(funcionario.edificio || "")}</td>
                </tr>
            </table>

            <table class="acta-table acta-table--equipos">
                <tr>
                    <th colspan="5">EQUIPOS</th>
                </tr>

                <tr>
                    <th>TIPO</th>
                    <th>MARCA</th>
                    <th>MODELO</th>
                    <th>SERIAL</th>
                    <th>CÓDIGO</th>
                </tr>

                ${licencias.map((licencia) => `
                <tr>
                    <td>${escapeHtml(licencia.tipo || "LICENCIA")}</td>
                    <td>${escapeHtml(licencia.marca || "")}</td>
                    <td>${escapeHtml(licencia.modelo || "")}</td>
                    <td>${escapeHtml(licencia.serial || "")}</td>
                    <td>${escapeHtml(licencia.codigo || "")}</td>
                </tr>`).join("")}
            </table>

            ${renderCertificationHtml(payload.certificacion || ACTA_LICENCIAS_CERTIFICATION_TEXT)}

            <table class="acta-table acta-table--firma">
                <tr>
                    <th colspan="2">ENTREGA RECEPCIÓN DE EQUIPO</th>
                </tr>

                <tr>
                    <th>ENTREGA</th>
                    <th>RECIBE</th>
                </tr>

                <tr>
                    <td>
                        <strong>Nombre:</strong>
                        ${escapeHtml(entrega.nombre || "")}
                    </td>
                    <td>
                        <strong>Nombre:</strong>
                        ${escapeHtml(recibe.nombre || "")}
                    </td>
                </tr>

                <tr class="acta-table__row--firma">
                    <td>
                        <strong>Firma:</strong>
                        ${escapeHtml(entrega.firma || "")}
                    </td>
                    <td>
                        <strong>Firma:</strong>
                        ${escapeHtml(recibe.firma || "")}
                    </td>
                </tr>

                <tr>
                    <td>
                        <strong>Fecha:</strong>
                        ${escapeHtml(entrega.fecha || "")}
                    </td>
                    <td>
                        <strong>Fecha:</strong>
                        ${escapeHtml(recibe.fecha || "")}
                    </td>
                </tr>
            </table>

            <div class="acta-sheet__footer">
                <div class="acta-sheet__footer-text">
                    <span>Dirección: Av. República E7-197 y Diego de Almagro — Edificio FORUM 300</span>
                    <span>Código postal: 170518 / Quito — Ecuador</span>
                    <span>Teléfono: +593-2 394 0000</span>
                    <span>www.derechosintelectuales.gob.ec</span>
                </div>

                <img src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}" alt="Gobierno del Ecuador" class="acta-sheet__footer-logo">
            </div>

        </div>
    `;
}


function openActaLicenciasPreview() {
    const form =
        document.getElementById("actaLicenciasForm");

    if (!form || !form.dataset.licenciaId) {
        showToast(
            "Licencia requerida",
            "Seleccione una licencia desde el buscador para previsualizar el acta.",
            "warning"
        );

        return;
    }

    // Misma validación que las demás actas antes de previsualizar.
    if (!form.reportValidity()) {
        return;
    }

    const payload =
        buildActaLicenciasPayload();

    openModal(
        "Previsualización del Acta de Licencias",
        `<div class="acta-preview">${renderActaLicenciasPreview(payload)}</div>`,
        [
            // Mismos botones que la previsualización de las demás actas
            {
                label: "Exportar DOCX",
                className: "btn btn-primary",
                onClick: () => exportActaLicencias("docx")
            },
            {
                label: "Exportar PDF",
                className: "btn btn-secondary",
                onClick: () => exportActaLicencias("pdf")
            },
            {
                label: "Cerrar",
                className: "btn btn-secondary",
                onClick: closeModal
            }
        ],
        "modal--wide"
    );
}


function resetActaLicenciasForm() {
    const form =
        document.getElementById("actaLicenciasForm");

    if (!form) {
        return;
    }

    form.reset();

    delete form.dataset.licenciaId;

    renderActaLicEquiposRows([]);

    form.classList.add("hidden");

    const resultsContainer =
        document.getElementById("actaEquipoResultados");

    if (resultsContainer) {
        resultsContainer.classList.add("hidden");
        resultsContainer.innerHTML = "";
    }

    showToast(
        "Acta eliminada",
        "Se eliminó el acta en edición. La licencia permanece en el inventario.",
        "success"
    );
}

async function exportActaLicencias(format) {
    const form = document.getElementById("actaLicenciasForm");

    if (!form || !form.dataset.licenciaId) {
        showToast(
            "Licencia requerida",
            "Seleccione una licencia desde el buscador antes de exportar el acta.",
            "warning"
        );
        return;
    }

    if (!form.reportValidity()) {
        return;
    }

    const payload = buildActaLicenciasPayload();

    if (format === "pdf") {
        await exportActaLicenciasPdfAuto(payload);
        return;
    }

    if (format === "docx") {
        await exportActaLicenciasDocxAuto(payload);
        return;
    }

    showToast(
        "Formato no compatible",
        "El formato seleccionado no está disponible.",
        "warning"
    );
}


async function exportActaLicenciasDocxAuto(payload) {
    try {
        showToast(
            "Generando DOCX",
            "Preparando el documento Word...",
            "info"
        );

        await loadScriptOnce(
            "https://cdn.jsdelivr.net/npm/docx@9.6.1/dist/index.iife.js"
        );

        if (!window.docx?.Document || !window.docx?.Packer) {
            throw new Error(
                "No se pudo cargar la biblioteca para crear el Word."
            );
        }

        const {
            Document,
            Packer,
            Paragraph,
            TextRun,
            ImageRun,
            Table,
            TableRow,
            TableCell,
            WidthType,
            AlignmentType,
            BorderStyle,
            HeightRule,
            VerticalAlign,
            TableLayoutType,
            HorizontalPositionRelativeFrom,
            HorizontalPositionAlign,
            VerticalPositionRelativeFrom,
            VerticalPositionAlign,
            TextWrappingType,
            Header
        } = window.docx;

        const funcionario = payload.funcionario || {};
        const licencia = payload.licencia || {};
        const licencias = payload.licencias?.length ? payload.licencias : [licencia];
        const entrega = payload.entrega || {};
        const recibe = payload.recibe || {};

        const parrafo = (valor, opciones = {}) => new Paragraph({
            alignment: opciones.alineacion || AlignmentType.LEFT,
            spacing: {
                before: opciones.antes || 0,
                after: opciones.despues ?? 0
            },
            children: [
                new TextRun({
                    text: String(valor ?? ""),
                    bold: Boolean(opciones.negrita),
                    underline: opciones.subrayado ? {} : undefined,
                    size: opciones.tamano || 19,
                    font: "Arial",
                    color: opciones.color || "000000"
                })
            ]
        });

        const celda = (contenido, opciones = {}) => new TableCell({
            columnSpan: opciones.columnas || 1,

            width: opciones.ancho
                ? {
                    size: opciones.ancho,
                    type: WidthType.DXA
                }
                : undefined,

            verticalAlign: opciones.arriba
                ? VerticalAlign.TOP
                : VerticalAlign.CENTER,

            shading: opciones.fondo
                ? { fill: opciones.fondo }
                : undefined,

            margins: {
                top: 65,
                bottom: 65,
                left: 90,
                right: 90
            },

            children: Array.isArray(contenido)
                ? contenido
                : [
                    parrafo(contenido, {
                        negrita: opciones.negrita,
                        alineacion: opciones.centrado
                            ? AlignmentType.CENTER
                            : AlignmentType.LEFT,
                        tamano: opciones.tamano || 18
                    })
                ]
        });

        const tituloSeccion = (texto, columnas) => celda(texto, {
            columnas,
            fondo: "D9D9D9",
            negrita: true,
            centrado: true,
            tamano: 18
        });

        const tituloColumna = (texto, ancho) => celda(texto, {
            ancho,
            fondo: "D9D9D9",
            negrita: true,
            centrado: true,
            tamano: 18
        });

        const bordes = {
            top: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            },
            bottom: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            },
            left: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            },
            right: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            },
            insideHorizontal: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            },
            insideVertical: {
                style: BorderStyle.SINGLE,
                size: 5,
                color: "555555"
            }
        };

        const sinBordes = {
            top: { style: BorderStyle.NONE },
            bottom: { style: BorderStyle.NONE },
            left: { style: BorderStyle.NONE },
            right: { style: BorderStyle.NONE },
            insideHorizontal: { style: BorderStyle.NONE },
            insideVertical: { style: BorderStyle.NONE }
        };

        const tabla = (
            filas,
            anchos,
            mostrarBordes = true
        ) => new Table({
            width: {
                size: anchos.reduce(
                    (total, ancho) => total + ancho,
                    0
                ),
                type: WidthType.DXA
            },

            columnWidths: anchos,
            layout: TableLayoutType.FIXED,
            borders: mostrarBordes ? bordes : sinBordes,
            rows: filas
        });

        const fila = (columnas, alto) => new TableRow({
            height: alto
                ? {
                    value: alto,
                    rule: HeightRule.ATLEAST
                }
                : undefined,
            children: columnas
        });

        const cargarImagen = async (archivo) => {
            try {
                const respuesta = await fetch(
                    actaAssetPath(archivo)
                );

                if (!respuesta.ok) {
                    return null;
                }

                return new Uint8Array(
                    await respuesta.arrayBuffer()
                );
            } catch (error) {
                return null;
            }
        };

        // Plantilla institucional: una sola imagen con encabezado y pie.
        const fondoPagina = await cargarImagen("Fondo_senadi.png");

        const imagen = (
            datos,
            ancho,
            alto,
            texto,
            alineacion
        ) => new Paragraph({
            alignment: alineacion,
            children: datos
                ? [
                    new ImageRun({
                        type: "png",
                        data: datos,
                        transformation: {
                            width: ancho,
                            height: alto
                        }
                    })
                ]
                : [
                    new TextRun({
                        text: texto,
                        bold: true,
                        size: 18
                    })
                ]
        });

        const tablaFuncionario = tabla([
            fila([
                tituloSeccion(
                    "DATOS DEL FUNCIONARIO SENADI",
                    6
                )
            ]),

            fila([
                celda("NOMBRE"),
                celda(funcionario.nombre),
                celda("CARGO"),
                celda(funcionario.cargo),
                celda("Nº EXT."),
                celda(funcionario.extension)
            ]),

            fila([
                celda("CORREO"),
                celda(funcionario.correo),
                celda("ÁREA"),
                celda(funcionario.area),
                celda("EDIFICIO"),
                celda(funcionario.edificio)
            ])
        ], [
            1050, 2950, 1050,
            2500, 1000, 1850
        ]);

        const tablaEquipo = tabla([
            fila([
                tituloSeccion("EQUIPOS", 5)
            ]),

            fila([
                tituloColumna("TIPO"),
                tituloColumna("MARCA"),
                tituloColumna("MODELO"),
                tituloColumna("SERIAL"),
                tituloColumna("CÓDIGO")
            ]),

            ...licencias.map((licencia) => fila([
                celda(licencia.tipo || "LICENCIA"),
                celda(licencia.marca),
                celda(licencia.modelo),
                celda(licencia.serial),
                celda(licencia.codigo)
            ], 360))
        ], [
            2400, 1950, 2100,
            2100, 1850
        ]);

        const tablaFirmas = tabla([
            fila([
                tituloSeccion(
                    "ENTREGA RECEPCIÓN DE EQUIPO",
                    2
                )
            ]),

            fila([
                tituloColumna("ENTREGA", 5200),
                tituloColumna("RECIBE", 5200)
            ]),

            fila([
                celda([
                    parrafo("Nombre:", {
                        negrita: true,
                        tamano: 18
                    }),
                    parrafo(entrega.nombre, {
                        tamano: 19
                    })
                ], { ancho: 5200 }),

                celda([
                    parrafo("Nombre:", {
                        negrita: true,
                        tamano: 18
                    }),
                    parrafo(recibe.nombre, {
                        tamano: 19
                    })
                ], { ancho: 5200 })
            ], 550),

            fila([
                celda([
                    parrafo(
                        `Firma: ${entrega.firma || ""}`,
                        {
                            negrita: true,
                            tamano: 18
                        }
                    )
                ], {
                    ancho: 5200,
                    arriba: true
                }),

                celda([
                    parrafo(
                        `Firma: ${recibe.firma || ""}`,
                        {
                            negrita: true,
                            tamano: 18
                        }
                    )
                ], {
                    ancho: 5200,
                    arriba: true
                })
            ], 1450),

            fila([
                celda([
                    parrafo("Fecha:", {
                        negrita: true,
                        tamano: 18
                    }),
                    parrafo(entrega.fecha, {
                        tamano: 19
                    })
                ], { ancho: 5200 }),

                celda([
                    parrafo("Fecha:", {
                        negrita: true,
                        tamano: 18
                    }),
                    parrafo(recibe.fecha, {
                        tamano: 19
                    })
                ], { ancho: 5200 })
            ], 520)
        ], [5200, 5200]);

        // Fondo de página: la plantilla ocupa toda la hoja A4, detrás del
        // texto y con sus colores originales; se repite en cada página.
        const encabezadoFondo = new Header({
            children: [
                new Paragraph({
                    children: fondoPagina
                        ? [
                            new ImageRun({
                                type: "png",
                                data: fondoPagina,
                                transformation: {
                                    width: 794,
                                    height: 1123
                                },
                                floating: {
                                    horizontalPosition: {
                                        relative: HorizontalPositionRelativeFrom.PAGE,
                                        offset: 0
                                    },
                                    verticalPosition: {
                                        relative: VerticalPositionRelativeFrom.PAGE,
                                        offset: 0
                                    },
                                    wrap: {
                                        type: TextWrappingType.NONE
                                    },
                                    behindDocument: true
                                }
                            })
                        ]
                        : []
                })
            ]
        });

        const documento = new Document({
            sections: [
                {
                    properties: {
                        page: {
                            size: {
                                width: 11906,
                                height: 16838
                            },

                            // Arriba y abajo se deja el espacio del
                            // encabezado y del pie que trae el fondo.
                            margin: {
                                top: 1700,
                                right: 720,
                                bottom: 2000,
                                left: 720,
                                header: 0
                            }
                        }
                    },

                    headers: {
                        default: encabezadoFondo
                    },

                    children: [
                        parrafo(
                            "SERVICIO NACIONAL DE DERECHOS INTELECTUALES",
                            {
                                alineacion: AlignmentType.CENTER,
                                negrita: true,
                                tamano: 25,
                                despues: 45
                            }
                        ),

                        parrafo(
                            "DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN",
                            {
                                alineacion: AlignmentType.CENTER,
                                negrita: true,
                                tamano: 23,
                                despues: 45
                            }
                        ),

                        parrafo(
                            "FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS",
                            {
                                alineacion: AlignmentType.CENTER,
                                negrita: true,
                                subrayado: true,
                                tamano: 23,
                                despues: 150
                            }
                        ),

                        tablaFuncionario,

                        parrafo("", {
                            despues: 25
                        }),

                        tablaEquipo,

                        // Un párrafo de Word por cada párrafo de la certificación
                        ...certificationParagraphs(payload.certificacion || ACTA_LICENCIAS_CERTIFICATION_TEXT)
                            .map((parte, indice, partes) => parrafo(
                                parte,
                                {
                                    alineacion: AlignmentType.JUSTIFIED,
                                    tamano: 19,
                                    antes: indice === 0 ? 120 : 0,
                                    despues: indice === partes.length - 1 ? 140 : 80
                                }
                            )),

                        tablaFirmas
                    ]
                }
            ]
        });

        const blob = await Packer.toBlob(documento);

        const codigo = String(
            licencia.codigo || "sin_codigo"
        ).replace(
            /[^a-zA-Z0-9_-]/g,
            "_"
        );

        downloadBlob(
            blob,
            `acta_licencia_${codigo}.docx`
        );

        showToast(
            "Exportación lista",
            "El DOCX fue generado correctamente.",
            "success"
        );

    } catch (error) {
        console.error(
            "[exportActaLicenciasDocxAuto]",
            error
        );

        showToast(
            "Error",
            error.message || "No se pudo generar el Word.",
            "danger"
        );
    }
}

function renderActaLicenciasPdfLayout(payload) {
    const funcionario = payload.funcionario || {};
    const licencia = payload.licencia || {};
    const entrega = payload.entrega || {};

    const cell = "border:1px solid #444;padding:5px 6px;vertical-align:middle;box-sizing:border-box;overflow-wrap:anywhere;word-break:break-word;";
    const label = `${cell}font-weight:700;background:#f5f5f5;`;
    const sectionTitle = "border:1px solid #444;background:#d9d9d9;color:#496585;padding:5px 6px;text-align:center;font-weight:700;font-size:11px;";
    const columnTitle = "border:1px solid #444;background:#eeeeee;color:#496585;padding:5px 6px;text-align:left;font-weight:700;font-size:11px;";

    return `
        <div style="
            width:760px;
            min-height:1075px;
            margin:0 auto;
            box-sizing:border-box;
            padding:20px 20px 18px;
            background:#ffffff;
            color:#111111;
            font-family:Arial, Helvetica, sans-serif;
            font-size:10px;
            display:flex;
            flex-direction:column;
            overflow:hidden;
        ">

            <div>

                <div style="
                    width:100%;
                    height:70px;
                    display:flex;
                    justify-content:space-between;
                    align-items:center;
                    box-sizing:border-box;
                    margin-bottom:8px;
                ">
                    <img
                        src="${actaAssetPath("logo_ecuador_nuevo.png")}"
                        alt="República del Ecuador"
                        style="
                            width:145px;
                            height:62px;
                            display:block;
                            object-fit:contain;
                            object-position:left center;
                        "
                    >

                    <img
                        src="${actaAssetPath("logo_senadi_nuevo.png")}"
                        alt="Servicio Nacional de Derechos Intelectuales"
                        style="
                            width:175px;
                            height:62px;
                            display:block;
                            object-fit:contain;
                            object-position:right center;
                        "
                    >
                </div>

                <div style="
                    width:100%;
                    text-align:center;
                    line-height:1.25;
                    margin-bottom:20px;
                ">
                    <div style="
                        font-size:17px;
                        font-weight:700;
                        margin-bottom:3px;
                    ">
                        SERVICIO NACIONAL DE DERECHOS INTELECTUALES
                    </div>

                    <div style="
                        font-size:13px;
                        font-weight:700;
                        margin-bottom:6px;
                    ">
                        DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN
                    </div>

                    <div style="
                        font-size:14px;
                        font-weight:700;
                        text-decoration:underline;
                    ">
                        FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS
                    </div>
                </div>

                <table style="
                    width:100%;
                    max-width:100%;
                    border-collapse:collapse;
                    table-layout:fixed;
                    margin:0 0 12px 0;
                    box-sizing:border-box;
                ">
                    <colgroup>
                        <col style="width:12%;">
                        <col style="width:28%;">
                        <col style="width:11%;">
                        <col style="width:25%;">
                        <col style="width:11%;">
                        <col style="width:13%;">
                    </colgroup>

                    <tr>
                        <th colspan="6" style="${sectionTitle}">
                            DATOS DEL FUNCIONARIO SENADI
                        </th>
                    </tr>

                    <tr>
                        <td style="${label}">NOMBRE</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.nombre || "")}
                        </td>

                        <td style="${label}">CARGO</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.cargo || "")}
                        </td>

                        <td style="${label}">N° EXT.</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.extension || "")}
                        </td>
                    </tr>

                    <tr>
                        <td style="${label}">CORREO</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.correo || "")}
                        </td>

                        <td style="${label}">ÁREA</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.area || "")}
                        </td>

                        <td style="${label}">EDIFICIO</td>
                        <td style="${cell}">
                            ${escapeHtml(funcionario.edificio || "")}
                        </td>
                    </tr>
                </table>

                <table style="
                    width:100%;
                    max-width:100%;
                    border-collapse:collapse;
                    table-layout:fixed;
                    margin:0 0 12px 0;
                    box-sizing:border-box;
                ">
                    <colgroup>
                        <col style="width:16%;">
                        <col style="width:18%;">
                        <col style="width:25%;">
                        <col style="width:21%;">
                        <col style="width:20%;">
                    </colgroup>

                    <tr>
                        <th colspan="5" style="${sectionTitle}">
                            EQUIPOS
                        </th>
                    </tr>

                    <tr>
                        <th style="${columnTitle}">TIPO</th>
                        <th style="${columnTitle}">MARCA</th>
                        <th style="${columnTitle}">MODELO</th>
                        <th style="${columnTitle}">SERIAL</th>
                        <th style="${columnTitle}">CÓDIGO</th>
                    </tr>

                    <tr>
                        <td style="${cell}">
                            ${escapeHtml(licencia.tipo || "LICENCIA")}
                        </td>

                        <td style="${cell}">
                            ${escapeHtml(licencia.marca || "")}
                        </td>

                        <td style="${cell}">
                            ${escapeHtml(licencia.modelo || "")}
                        </td>

                        <td style="${cell}">
                            ${escapeHtml(licencia.serial || "")}
                        </td>

                        <td style="${cell}">
                            ${escapeHtml(licencia.codigo || "")}
                        </td>
                    </tr>
                </table>

                <div style="
                    width:100%;
                    margin:0 0 14px 0;
                    font-size:10px;
                    line-height:1.45;
                    color:#222222;
                    text-align:justify;
                    box-sizing:border-box;
                ">
                    ${escapeHtml(payload.certificacion || ACTA_LICENCIAS_CERTIFICATION_TEXT)}
                </div>

                <table style="
                    width:100%;
                    max-width:100%;
                    border-collapse:collapse;
                    table-layout:fixed;
                    margin:0;
                    box-sizing:border-box;
                ">
                    <tr>
                        <th style="${sectionTitle}">
                            ENTREGA DE EQUIPO
                        </th>
                    </tr>

                    <tr>
                        <td style="
                            border:1px solid #444;
                            background:#eeeeee;
                            color:#496585;
                            padding:5px 6px;
                            font-size:11px;
                        ">
                            ENTREGA
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            border:1px solid #444;
                            padding:6px 7px;
                            box-sizing:border-box;
                            overflow-wrap:anywhere;
                        ">
                            <strong>Nombre:</strong>
                            ${escapeHtml(entrega.nombre || "")}
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            border:1px solid #444;
                            padding:7px;
                            box-sizing:border-box;
                            height:105px;
                            vertical-align:top;
                            overflow-wrap:anywhere;
                        ">
                            <strong>Firma:</strong>

                            <div style="
                                min-height:75px;
                                margin-top:8px;
                            ">
                                ${escapeHtml(entrega.firma || "")}
                            </div>
                        </td>
                    </tr>

                    <tr>
                        <td style="
                            border:1px solid #444;
                            padding:6px 7px;
                            box-sizing:border-box;
                        ">
                            <strong>Fecha:</strong>
                            ${escapeHtml(entrega.fecha || "")}
                        </td>
                    </tr>
                </table>

            </div>

            <div style="
                width:100%;
                margin-top:auto;
                box-sizing:border-box;
                padding-top:12px;
            ">
                <div style="
                    width:100%;
                    border-top:1px solid #b7b7b7;
                    margin-bottom:8px;
                "></div>

                <div style="
                    width:100%;
                    display:flex;
                    justify-content:space-between;
                    align-items:flex-end;
                    box-sizing:border-box;
                ">
                    <div style="
                        width:68%;
                        color:#666666;
                        font-size:9px;
                        line-height:1.4;
                    ">
                        <div>
                            Dirección: Av. República E7-197 y Diego de Almagro - Edificio FORUM 300
                        </div>

                        <div>
                            Código postal: 170518 / Quito - Ecuador
                        </div>

                        <div>
                            Teléfono: +593-2 394 0000
                        </div>

                        <div>
                            www.derechosintelectuales.gob.ec
                        </div>
                    </div>

                    <div style="
                        width:30%;
                        text-align:right;
                    ">
                        <img
                            src="${actaAssetPath("logo_nuevo_ecuador_nuevo.png")}"
                            alt="El Nuevo Ecuador"
                            style="
                                width:135px;
                                height:58px;
                                display:inline-block;
                                object-fit:contain;
                                object-position:right bottom;
                            "
                        >
                    </div>
                </div>
            </div>

        </div>
    `;
}

async function exportActaLicenciasPdfAuto(payload) {
    // El PDF se genera en el servidor (texto real), igual que las demás actas,
    // para que tenga la misma calidad. La licencia viaja como "desktop".
    try {
        showToast(
            "Generando PDF",
            "Preparando el documento...",
            "info"
        );

        const solicitud = {
            funcionario: payload.funcionario,
            desktop: payload.licencia,
            licencias: payload.licencias,
            certificacion: payload.certificacion,
            entrega: payload.entrega,
            recibe: payload.recibe
        };

        const response = await apiFetch("/actas/licencias/export/pdf", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(solicitud)
        });

        if (!response.ok) {
            let message = "No se pudo generar el PDF.";
            try {
                const errorPayload = await response.json();
                message = errorPayload.message || errorPayload.error || message;
            } catch (error) {
                // ignore parsing fallback
            }
            throw new Error(message);
        }

        const blob = await response.blob();

        const codigoArchivo =
            String(payload.licencia?.codigo || "sin_codigo").replace(
                /[^a-zA-Z0-9_-]/g,
                "_"
            );

        downloadBlob(
            blob,
            `acta_licencia_${codigoArchivo}.pdf`
        );

        showToast(
            "Exportación lista",
            "El PDF fue generado correctamente.",
            "success"
        );

    } catch (error) {
        console.error(
            "[exportActaLicenciasPdfAuto]",
            error
        );

        showToast(
            "Error",
            error.message ||
            "No se pudo generar el PDF.",
            "danger"
        );
    }
}

// Función para configurar los eventos relacionados con el acta de entrega/recepción
    function bindActaPcEvents() {
        prepareActaEquiposLayout();
        document.getElementById("actaPcSelector")?.addEventListener("change", (event) => {
            const item = state.inventory.find((row) => String(row.id) === String(event.target.value));
            if (item) {
                selectActaEquipo(item.id);
            }
        });
        actaFilterIds().forEach((id) => {
            document.getElementById(id)?.addEventListener("input", () => updateAutocompleteForInput(id));
        });
        document.getElementById("actaEquipoBuscarButton")?.addEventListener("click", runActaEquipoSearch);
        document.querySelector(".acta-search-panel")?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                runActaEquipoSearch();
            }
        });
        document.getElementById("actaFilterTipo")?.addEventListener("change", () => {
            clearSelectedActaEquipo();
            loadActaPcInitialData();
            ["actaPcForm", "actaImpresoraForm", "actaProyectorForm", "actaEscanerForm", "actaTelefonoForm", "actaFormContainer"].forEach((id) => {
                document.getElementById(id)?.classList.add("hidden");
            });
        });
        document.getElementById("actaPcPreviewButton")?.addEventListener("click", openActaPcPreview);
        document.getElementById("actaPcExportDocxButton")?.addEventListener("click", () => exportActaPc("docx"));
        document.getElementById("actaPcExportPdfButton")?.addEventListener("click", () => exportActaPc("pdf"));
        document.getElementById("actaPcResetButton")?.addEventListener("click", resetActaPcForm);
        document.getElementById("actaImpresoraPreviewButton")?.addEventListener("click", openActaImpresoraPreview);
        document.getElementById("actaImpresoraExportDocxButton")?.addEventListener("click", () => exportActaImpresora("docx"));
        document.getElementById("actaImpresoraExportPdfButton")?.addEventListener("click", () => exportActaImpresora("pdf"));
        document.getElementById("actaImpresoraResetButton")?.addEventListener("click", resetActaImpresoraForm);
        document.getElementById("actaEscanerPreviewButton")?.addEventListener("click", openActaEscanerPreview);
        document.getElementById("actaEscanerExportDocxButton")?.addEventListener("click", () => exportActaEscaner("docx"));
        document.getElementById("actaEscanerExportPdfButton")?.addEventListener("click", () => exportActaEscaner("pdf"));
        document.getElementById("actaEscanerResetButton")?.addEventListener("click", resetActaEscanerForm);
        document.getElementById("actaProyectorPreviewButton")?.addEventListener("click", openActaProyectorPreview);
        document.getElementById("actaProyectorExportDocxButton")?.addEventListener("click", () => exportActaProyector("docx"));
        document.getElementById("actaProyectorExportPdfButton")?.addEventListener("click", () => exportActaProyector("pdf"));
        document.getElementById("actaProyectorResetButton")?.addEventListener("click", resetActaProyectorForm);
        document.getElementById("actaTelefonoPreviewButton")?.addEventListener("click", openActaTelefonoPreview);
        document.getElementById("actaTelefonoExportDocxButton")?.addEventListener("click", () => exportActaTelefono("docx"));
        document.getElementById("actaTelefonoExportPdfButton")?.addEventListener("click", () => exportActaTelefono("pdf"));
        document.getElementById("actaTelefonoResetButton")?.addEventListener("click", resetActaTelefonoForm);

        bindEntregaNombreOtroEvent("actaEntregaNombre", "actaEntregaNombreOtro");
        bindEntregaNombreOtroEvent("actaImpresoraEntregaNombre", "actaImpresoraEntregaNombreOtro");
        bindEntregaNombreOtroEvent("actaEscanerEntregaNombre", "actaEscanerEntregaNombreOtro");
        bindEntregaNombreOtroEvent("actaTelefonoEntregaNombre", "actaTelefonoEntregaNombreOtro");
        bindEntregaNombreOtroEvent("actaProyectorEntregaNombre", "actaProyectorEntregaNombreOtro");
        initCalendarioFechasActividades();
    }

    function bindActaRcEvents() {
        loadActaRcInitialData();
        document.getElementById("actaRcIniciarButton")?.addEventListener("click", () => {
            const form = document.getElementById("actaRcForm");
            const preview = document.getElementById("actaRcPreviewActions");
            const selector = document.getElementById("actaRcSelector");
            if (selector) selector.value = "MANUAL";
            if (form) form.classList.remove("hidden");
            if (preview) preview.classList.remove("hidden");
            document.getElementById("actaRcEquipmentHint")?.classList.add("hidden");
            document.getElementById("actaRcIniciarButton")?.classList.add("hidden");
        });
        document.getElementById("actaRcPreviewButton")?.addEventListener("click", openActaRcPreview);
        document.getElementById("actaRcExportDocxButton")?.addEventListener("click", () => exportActaRc("docx"));
        document.getElementById("actaRcExportPdfButton")?.addEventListener("click", () => exportActaRc("pdf"));
        document.getElementById("actaRcResetButton")?.addEventListener("click", resetActaRcForm);
        bindEntregaNombreOtroEvent("actaRcEntregaNombre", "actaRcEntregaNombreOtro");
        document.getElementById("actaRcAgregarTipoBtn")?.addEventListener("click", () => {
            const bodyHtml = `
                <div style="padding: 8px 0;">
                    <label for="actaRcInputNuevoTipoName" style="font-weight: bold; margin-bottom: 8px; display: block;">Nombre del Tipo de Equipo:</label>
                    <input id="actaRcInputNuevoTipoName" class="acta-input" placeholder="Ej. SWITCHES, SERVIDORES, CENTRAL TELEFONICA" style="width: 100%; border: 1px solid #ccc; border-radius: 8px; padding: 8px 12px; box-sizing: border-box; font-size: 14px;">
                </div>
            `;
            openModal("Agregar Nuevo Tipo de Equipo", bodyHtml, [
                {
                    label: "Agregar",
                    className: "btn btn-primary",
                    onClick: () => {
                        const input = document.getElementById("actaRcInputNuevoTipoName");
                        const val = input ? input.value.trim() : "";
                        if (!val) {
                            showToast("Nombre inválido", "El nombre no puede estar vacío.", "danger");
                            return;
                        }
                        addActaRcEquipmentType(val);
                        closeModal();
                    }
                },
                {
                    label: "Cancelar",
                    className: "btn btn-secondary",
                    onClick: closeModal
                }
            ]);
            setTimeout(() => {
                const input = document.getElementById("actaRcInputNuevoTipoName");
                if (input) {
                    input.focus();
                    input.addEventListener("keydown", (e) => {
                        if (e.key === "Enter") {
                            e.preventDefault();
                            const val = input.value.trim();
                            if (!val) {
                                showToast("Nombre inválido", "El nombre no puede estar vacío.", "danger");
                                return;
                            }
                            addActaRcEquipmentType(val);
                            closeModal();
                        }
                    });
                }
            }, 100);
        });
    }

    function bindActaSwEvents() {
        document.getElementById("actaSwEquipoBuscarButton")?.addEventListener("click", runActaSwEquipoSearch);
        document.getElementById("actaSwEquipoLimpiarButton")?.addEventListener("click", clearActaSwFilters);
        ["actaSwFilterCustodio", "actaSwFilterArea", "actaSwFilterEdificio", "actaSwFilterCodigoSbai"].forEach((id) => {
            document.getElementById(id)?.addEventListener("keydown", (event) => {
                if (event.key === "Enter") { event.preventDefault(); runActaSwEquipoSearch(); }
            });
        });
        document.getElementById("actaSwAgregarAdicionalButton")?.addEventListener("click", addActaSwAdicionalRow);
        document.getElementById("actaSwPreviewButton")?.addEventListener("click", openActaSwPreview);
        document.getElementById("actaSwExportDocxButton")?.addEventListener("click", () => exportActaSoftware("docx"));
        document.getElementById("actaSwExportPdfButton")?.addEventListener("click", () => exportActaSoftware("pdf"));
        document.getElementById("actaSwResetButton")?.addEventListener("click", resetActaSwForm);
        bindEntregaNombreOtroEvent("actaSwEntregaNombre", "actaSwEntregaNombreOtro");
        loadActaSwInitialData();
    }
// Función para recolectar los criterios de búsqueda del acta desde los campos del formulario
    function collectActaSearchCriteria() {
        return {
            tipo: document.getElementById("actaFilterTipo")?.value || "",
            codigoSbai: document.getElementById("actaFilterCodigoSbai")?.value || "",
            codigoMegan: document.getElementById("actaFilterCodigoMegan")?.value || "",
            descripcion: document.getElementById("actaFilterDescripcion")?.value || "",
            marca: document.getElementById("actaFilterMarca")?.value || "",
            modelo: document.getElementById("actaFilterModelo")?.value || "",
            numeroSerie: document.getElementById("actaFilterSerie")?.value || "",
            custodio: document.getElementById("actaFilterCustodio")?.value || "",
            ubicacionEdificio: document.getElementById("actaFilterEdificio")?.value || "",
            ubicacionPiso: document.getElementById("actaFilterPiso")?.value || "",
            ubicacionDireccion: document.getElementById("actaFilterDireccion")?.value || "",
            estado: document.getElementById("actaFilterEstado")?.value || ""
        };
    }
// Función para verificar si un equipo cumple con los criterios de búsqueda del acta
    function matchesActaCriteria(item, criteria) {
        return Object.keys(criteria).every((key) => {
            const expected = String(criteria[key] || "").trim().toLowerCase();
            if (!expected) {
                return true;
            }
            return String(item[key] || "").toLowerCase().includes(expected);
        });
    }
// Función para ejecutar la búsqueda de equipos en el acta
    function runActaEquipoSearch() {
        const criteria = collectActaSearchCriteria();
        const filtered = getActaEquipoItems(criteria.tipo).filter((item) => matchesActaCriteria(item, criteria));
        state.actaEquipoLastFiltered = filtered;
        state.actaEquipoLimit = 50;
        const items = filtered.slice(0, state.actaEquipoLimit);
        renderActaEquipoResults(items, filtered.length);
        if (!items.length) {
            showToast("Sin resultados", "No se encontraron equipos con ese criterio.", "info");
        }
    }

    function selectActaEquipo(id) {
        const item = state.inventory.find((row) => String(row.id) === String(id));
        if (!item) {
            return;
        }
        const filterType = document.getElementById("actaFilterTipo");
        if (filterType) {
            filterType.value = item.tipo || "";
        }
        const tipo = String(item.tipo || "").toLowerCase();
        const typeConfig = {
            impresora: { selectorId: "actaImpresoraSelector", formId: "actaImpresoraForm", toolbarId: "actaImpresoraToolbarActions", autofill: autofillActaImpresoraForm, init: loadActaImpresoraInitialData },
            proyector: { selectorId: "actaProyectorSelector", formId: "actaProyectorForm", toolbarId: "actaProyectorToolbarActions", autofill: autofillActaProyectorForm, init: loadActaProyectorInitialData },
            escaner: { selectorId: "actaEscanerSelector", formId: "actaEscanerForm", toolbarId: "actaEscanerToolbarActions", autofill: autofillActaEscanerForm, init: loadActaEscanerInitialData },
            telefono: { selectorId: "actaTelefonoSelector", formId: "actaTelefonoForm", toolbarId: "actaTelefonoToolbarActions", autofill: autofillActaTelefonoForm, init: loadActaTelefonoInitialData }
        };
        const cfg = typeConfig[tipo];
        ["actaPcForm", "actaImpresoraForm", "actaProyectorForm", "actaEscanerForm", "actaTelefonoForm"].forEach((id) => document.getElementById(id)?.classList.add("hidden"));
        ["actaPcToolbarActions", "actaImpresoraToolbarActions", "actaProyectorToolbarActions", "actaEscanerToolbarActions", "actaTelefonoToolbarActions"].forEach((id) => document.getElementById(id)?.classList.add("hidden"));
        if (cfg) {
            const selector = document.getElementById(cfg.selectorId);
            if (selector) selector.value = item.id;
            cfg.autofill(item);
            cfg.init();
            document.getElementById(cfg.formId)?.classList.remove("hidden");
            document.getElementById(cfg.toolbarId)?.classList.remove("hidden");
        } else {
            const selector = document.getElementById("actaPcSelector");
            if (selector) selector.value = item.id;
            autofillActaPcForm(item);
            document.getElementById("actaPcForm")?.classList.remove("hidden");
            document.getElementById("actaPcToolbarActions")?.classList.remove("hidden");
        }
        document.querySelectorAll(".acta-search-card").forEach((card) => {
            card.classList.toggle("is-selected", String(card.dataset.actaSelect) === String(item.id));
        });
        const formContainer = document.getElementById("actaFormContainer");
        if (formContainer) {
            formContainer.classList.remove("hidden");
        }
        const resultsContainer = document.getElementById("actaEquipoResultados");
        if (resultsContainer) {
            resultsContainer.classList.add("hidden");
            resultsContainer.innerHTML = "";
        }
        showToast("Equipo seleccionado", "El acta se autocompleto con los datos del inventario.", "success");
    }

    function clearActaSearch() {
        const type = document.getElementById("actaFilterTipo");
        const estado = document.getElementById("actaFilterEstado");
        if (type) {
            type.value = "";
        }
        if (estado) {
            estado.value = "";
        }
        actaFilterIds().forEach((id) => {
            const input = document.getElementById(id);
            if (input) {
                input.value = "";
            }
        });
        renderActaEquipoResults([]);
    }

    function buildNav() {
        const roleName = state.session?.role || role;
        let nav;
        if (roleName === "admin") {
            nav = [
                ["dashboard", "Dashboard", `${basePrefix}/pages/dashboard.html`],
                ["inventario", "Inventario", `${basePrefix}/pages/inventario.html`],
                // COMENTADO: redundante con filtros del mÁ³dulo de inventario
                // ["busqueda", "Busqueda", `${basePrefix}/pages/busqueda.html`],
                ["nuevo-equipo", "Nuevo Equipo", `${basePrefix}/pages/nuevo-equipo.html`],
                ["actas", "ACTAS", `${basePrefix}/pages/actas.html`]
            ];
        } else if (roleName === "custodio") {
            nav = [
                ["dashboard", "Dashboard", `${basePrefix}/pages/custodio/dashboard.html`],
                ["inventario", "Inventario", `${basePrefix}/pages/custodio/inventario.html`],
                // COMENTADO: redundante con filtros del mÁ³dulo de inventario
                // ["busqueda", "Busqueda", `${basePrefix}/pages/custodio/busqueda.html`]
                ["actas", "ACTAS", `${basePrefix}/pages/custodio/actas.html`]
            ];
        } else {
            nav = [
                ["dashboard", "Dashboard", `${basePrefix}/pages/usuario/dashboard.html`],
                ["inventario", "Inventario", `${basePrefix}/pages/usuario/inventario.html`],
                // COMENTADO: redundante con filtros del mÁ³dulo de inventario
                // ["busqueda", "Busqueda", `${basePrefix}/pages/usuario/busqueda.html`]
                ["actas", "ACTAS", `${basePrefix}/pages/usuario/actas.html`]
            ];
        }

        return nav
            .filter(([key]) => key !== "busqueda")
            .map(([key, label, href]) => `
                <a href="${href}" class="${(key === "actas" && isActasPage(page)) || key === page ? "is-active" : ""}">
                    <span class="nav-icon" aria-hidden="true">${NAV_ICONS[key] || iconMarkup("circle")}</span>
                    <span>${label}</span>
                </a>
            `)
            .join("");
    }

    function renderShell() {
        const appShell = document.getElementById("appShell");
        const roleName = state.session?.role || role;
        const panelLabel = roleName === "admin"
            ? "Panel administrativo"
            : (roleName === "custodio" ? "Panel custodio" : "Panel tecnico");

        appShell.innerHTML = `
            <div class="app-shell role-${roleName}">
                <aside class="sidebar" id="sidebar">
                    <div class="sidebar__brand">
                        <div class="brand-mark">SI</div>
                        <div>
                            <strong>Sistema Inventario</strong>
                            <span>${panelLabel}</span>
                        </div>
                    </div>
                    <div class="sidebar__user">
                        <div class="avatar">${state.session.displayName.charAt(0).toUpperCase()}</div>
                        <div>
                            <strong>${escapeHtml(state.session.displayName)}</strong>
                            <span>${state.session.roleLabel}</span>
                        </div>
                    </div>
                    <nav class="sidebar__nav">${buildNav()}</nav>
                    <button class="sidebar__logout" id="logoutButton">Cerrar sesión</button>
                </aside>
                <div class="app-main">
                    <header class="topbar">
                        <div class="topbar__actions">
                            <button class="topbar__toggle" id="sidebarToggle">☰</button>
                            <div class="topbar__title">
                                <h1>${pageTitle(page)}</h1>
                                <p>${pageDescription(page)}</p>
                            </div>
                        </div>
                        <div class="user-chip">${escapeHtml(state.session.displayName)} · ${state.session.roleLabel}</div>
                    </header>
                    <main class="content" id="pageContent"></main>
                </div>
            </div>
            <div class="modal-root" id="modalRoot"></div>
            <div class="toast-stack" id="toastStack"></div>
        `;
        document.getElementById("pageContent").innerHTML = renderPage();
    }

    function pageDescription(pageName) {
        const descriptions = {
            dashboard: "Resumen visual del sistema y accesos directos por rol.",
            dashboard: "",
            inventario: "Consulta y gestión del inventario con control por rol.",
            busqueda: "Busqueda multi-criterio en cliente sobre el inventario cargado.",
            "nuevo-equipo": "",
            actas: "",
            "acta-equipos": "Formulario para registrar mantenimiento preventivo de equipos.",
            "acta-software": "Formulario para registrar programas y aplicaciones instaladas.",
            "acta-rc": "Formulario institucional de redes y comunicaciones."
        };
        return descriptions[pageName] || "";
    }

    function canEditCustodio() {
        return Boolean(state.session?.permissions?.puedeEditarCustodio);
    }

    function canEditAll() {
        return Boolean(state.session?.permissions?.puedeEditarTodos);
    }

    function canChangeState() {
        return Boolean(state.session?.permissions?.puedeActualizarEstado);
    }

    function canExportInventory() {
        return Boolean(state.session?.permissions?.puedeExportarInventario);
    }

    function canCreateEquipment() {
        return Boolean(state.session?.permissions?.puedeCrearEquipo);
    }

    function canViewHistory() {
        if (!state.session?.permissions) {
            return true;
        }
        return state.session.permissions.puedeVerHistorial !== false;
    }
//Funcion que renderiza la pagina de inventario, con filtros y tabla de resultados, adaptada segun los permisos del usuario
    function renderInventoryPage() {
        return window.SIInventoryView.renderPage({
            canExportInventory,
            canEditAll,
            buildTypeOptions,
            renderAutocompleteField,
            validStates: VALID_STATES,
            labelForColumn
        });
    }

    function buildInventoryActionButtons(item, options) {
        return window.SIInventoryView.buildActionButtons(item, options, {
            canEditAll,
            canEditCustodio,
            canChangeState,
            canViewHistory,
            iconMarkup
        });
    }

    function renderInventoryRow(item) {
        return window.SIInventoryView.renderRow(item, {
            escapeHtml,
            displayInventoryType,
            stateBadge,
            canEditAll,
            buildInventoryActionButtons
        });
    }

    function renderMobileInventoryCard(item) {
        return window.SIInventoryView.renderMobileCard(item, {
            escapeHtml,
            displayInventoryType,
            stateBadge,
            stripHtml,
            buildInventoryActionButtons
        });
    }

    function renderInventory() {
        window.SIInventoryView.renderResults({
            state,
            pageSize: PAGE_SIZE,
            updateInventoryStats,
            setText,
            escapeHtml,
            renderInventoryRow,
            renderMobileInventoryCard,
            refreshInventoryAutocompletes
        });
    }

    function bindInventoryEvents() {
        inventoryFilterIds().forEach((id) => {
            document.getElementById(id)?.addEventListener("input", () => {
                updateAutocompleteForInput(id);
            });
        });
        document.getElementById("applyInventoryFilters")?.addEventListener("click", applyInventoryFilters);
        document.getElementById("clearInventoryFilters")?.addEventListener("click", clearInventoryFilters);
        
        const btnExcel = document.getElementById("exportInventoryExcel");
        const btnPdf = document.getElementById("exportInventoryPdf");
        
        if (canExportInventory()) {
            btnExcel?.addEventListener("click", () => openExportDialog("excel"));
            btnPdf?.addEventListener("click", () => openExportDialog("pdf"));
        } else {
            if (btnExcel) btnExcel.style.display = "none";
            if (btnPdf) btnPdf.style.display = "none";
        }
        document.querySelectorAll("[data-sort]").forEach((button) => button.addEventListener("click", () => sortInventory(button.dataset.sort)));
        document.getElementById("prevPage")?.addEventListener("click", () => changePage(-1));
        document.getElementById("nextPage")?.addEventListener("click", () => changePage(1));
        document.getElementById("inventoryBody")?.addEventListener("click", handleInventoryActionClick);
        document.getElementById("inventoryMobile")?.addEventListener("click", handleInventoryActionClick);
    }

    async function handleInventoryActionClick(event) {
        const button = event.target.closest("[data-inv-action]");
        if (!button) {
            return;
        }
        const action = button.dataset.invAction;
        const id = Number(button.dataset.id || 0);
        const item = state.inventory.find((entry) => Number(entry.id) === id);
        if (!item) {
            return;
        }
        if (action === "editar") {
            if (canEditAll()) {
                await abrirModalEditar(item.id);
            } else {
                await openCustodioEditor(item);
            }
            return;
        }
        if (action === "estado") {
            await openStateEditor(item);
            return;
        }
        if (action === "historial") {
            await openHistoryViewer(item);
            return;
        }
        if (action === "eliminar") {
            await confirmarYEliminarEquipo(item);
        }
    }

    async function confirmarYEliminarEquipo(item) {
        const codigo = item.codigoSbai || item.codigoMegan || `ID ${item.id}`;
        const contentHtml = `
            <div style="padding: 8px 0;">
                <p style="color: #d32f2f; font-weight: bold; margin-bottom: 12px;">¡ATENCIÓN! Esta acción es irreversible y eliminará el equipo del inventario de forma permanente.</p>
                <p style="margin-bottom: 16px;">Para confirmar la eliminación del equipo <strong>${escapeHtml(codigo)}</strong> (${escapeHtml(item.descripcion || "")}), escribe la palabra <strong style="color: #d32f2f;">ELIMINAR</strong> a continuación:</p>
                <input type="text" id="confirmarEliminarInput" class="acta-input" placeholder="Escribe ELIMINAR" style="width: 100%; border: 1px solid #ccc; border-radius: 8px; padding: 8px 12px; box-sizing: border-box; font-size: 14px;">
            </div>
        `;

        openModal("Eliminar Equipo", contentHtml, [
            {
                label: "Eliminar",
                className: "btn btn-danger",
                id: "confirmarEliminarBtn",
                disabled: true,
                onClick: async () => {
                    const input = document.getElementById("confirmarEliminarInput");
                    if (input?.value.trim().toUpperCase() !== "ELIMINAR") {
                        showToast("Validación fallida", "Debe ingresar la palabra ELIMINAR exactamente.", "warning");
                        return;
                    }
                    try {
                        const response = await apiFetch(`/inventario/${item.id}`, { method: "DELETE" });
                        if (!response.ok) {
                            const payload = await response.json();
                            throw new Error(payload.message || "Error al eliminar el equipo.");
                        }
                        
                        // Remover del estado
                        state.inventory = state.inventory.filter((entry) => entry.id !== item.id);
                        state.filteredInventory = state.filteredInventory.filter((entry) => entry.id !== item.id);
                        
                        renderInventory();
                        closeModal();
                        showToast("Equipo eliminado", "El equipo ha sido eliminado del inventario.", "success");
                    } catch (error) {
                        showToast("Error", error.message || "No se pudo eliminar el equipo.", "danger");
                    }
                }
            },
            {
                label: "Cancelar",
                className: "btn btn-secondary",
                onClick: closeModal
            }
        ]);

        // Configurar validación en tiempo real para activar el botón
        setTimeout(() => {
            const input = document.getElementById("confirmarEliminarInput");
            const btn = document.getElementById("confirmarEliminarBtn");
            if (input && btn) {
                btn.disabled = true; // Asegurar que empieza deshabilitado
                input.focus();
                input.addEventListener("input", (e) => {
                    btn.disabled = e.target.value.trim().toUpperCase() !== "ELIMINAR";
                });
            }
        }, 100);
    }

    async function loadCustodios(query, limit) {
        const params = new URLSearchParams();
        const term = typeof query === "string" ? query.trim() : "";
        if (term) {
            params.set("q", term);
        }
        const max = Number.isFinite(limit) && limit > 0 ? Math.trunc(limit) : 0;
        if (max > 0) {
            params.set("limit", String(max));
        }
        const path = params.toString() ? `/inventario/custodios?${params.toString()}` : "/inventario/custodios";
        const response = await apiFetch(path);
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "No se pudo cargar custodios.");
        }
        return Array.isArray(payload.data) ? payload.data : [];
    }

    async function openCustodioEditor(item) {
        try {
            const custodios = await loadCustodios(null, 500);
            const opcionesCustodio = custodios.map((c) => {
                const nombre = String(c?.nombre || "");
                return `<option value="${c.id}">${escapeHtml(nombre)}</option>`;
            }).join("");
            const datalistOpts = custodios.map((c) =>
                `<option value="${escapeHtml(String(c?.nombre || ""))}"></option>`
            ).join("");

            const tipoLabel = {
                pc: "PC", laptop: "Laptop", periferico: "Periférico", impresora: "Impresora",
                escaner: "Escáner", telefono: "Teléfono", proyector: "Proyector",
                infraestructura: "Infraestructura", licencia: "Licencia",
                bien_control_admin: "Bien Control Adm.", modem: "Módem"
            };
            const hoy = new Date().toISOString().split("T")[0];
            const roSt = "opacity:.55;cursor:not-allowed;background:#f4f6fb;border-color:#e0e9f6;";

            openModal(
                `Editar custodio ${escapeHtml(item.codigoSbai || String(item.id))}`,
                `<div>
                    <div style="background:#fff8e1;border:1px solid #ffe082;border-radius:8px;padding:9px 14px;margin-bottom:14px;font-size:13px;color:#7a5f00;display:flex;align-items:center;gap:8px;">
                        <span style="font-size:16px;">AVISO:</span>
                        <span>Modo solo lectura: únicamente puede modificar el custodio.</span>
                    </div>
                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;" id="editFormGrid2">
                        <label class="edit-field"><span>Còdigo SBYE</span><input value="${escapeHtml(item.codigoSbai || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Còdigo Megan</span><input value="${escapeHtml(item.codigoMegan || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field" style="grid-column:span 2;"><span>Descripción</span><input value="${escapeHtml(item.descripcion || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Tipo</span><input value="${escapeHtml(tipoLabel[item.tipo] || item.tipo || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Marca</span><input value="${escapeHtml(item.marca || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Modelo</span><input value="${escapeHtml(item.modelo || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Nùmero de serie</span><input value="${escapeHtml(item.numeroSerie || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field" style="grid-column:span 2;">
                            <span>Custodio <em style="color:var(--primary);font-weight:600;font-style:normal;">(editable)</em></span>
                            <select name="id_custodio_actual" id="custodioSelectEdit" style="border-color:var(--primary);">
                                <option value="">-- Sin custodio --</option>${opcionesCustodio}
                            </select>
                        </label>
                        <label class="edit-field"><span>Edificio</span><input value="${escapeHtml(item.ubicacionEdificio || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field"><span>Piso</span><input value="${escapeHtml(item.ubicacionPiso || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field" style="grid-column:span 2;"><span>Dirección / Área</span><input value="${escapeHtml(item.ubicacionDireccion || "")}" disabled style="${roSt}"></label>
                        <label class="edit-field" style="grid-column:span 2;"><span>Detalle</span><textarea disabled rows="2" style="${roSt}">${escapeHtml(item.observacion || item.caracteristicas || "")}</textarea></label>
                        <hr style="grid-column:span 2;border:none;border-top:1px solid #dce7f3;margin:2px 0 4px;">
                        <label class="edit-field" style="grid-column:span 2;">
                            <span>Registrado por <em style="color:var(--primary);font-weight:600;font-style:normal;">(automático)</em></span>
                            <input id="registradoPorInput" value="${escapeHtml(state.session.displayName)}" disabled style="${roSt}">
                        </label>
                        <label class="edit-field">
                            <span>Fecha y Hora del cambio</span>
                            <input type="text" id="fechaCambioInput" value="${new Date().toLocaleString("es-EC")}" disabled style="${roSt}">
                        </label>
                    </div>
                </div>`,
                [
                    { label: "Cancelar", className: "btn btn-secondary", onClick: closeModal },
                    {
                        label: "Guardar cambio",
                        className: "btn btn-primary",
                        onClick: async () => {
                            const sel = document.getElementById("custodioSelectEdit");
                            const regPor = state.session.displayName;
                            if (!sel?.value) { showToast("Aviso", "Seleccione un custodio.", "warning"); return; }
                            try {
                                const r = await apiFetch(`/inventario/${item.id}/custodio`, {
                                    method: "PUT",
                                    headers: { "Content-Type": "application/json" },
                                    body: JSON.stringify({
                                        idCustodio: parseInt(sel.value),
                                        registradoPor: regPor
                                    })
                                });
                                const p = await r.json();
                                if (!r.ok || !p.success) throw new Error(p.message || "Error al guardar");
                                closeModal();
                                showUpdatedInventoryRow(p.data);
                                showToast("Custodio actualizado", "El cambio se guardó correctamente en el historial.", "success");
                            } catch (err) {
                                showToast("Error", err.message, "danger");
                            }
                        }
                    }
                ]
            );

            const selCustodio = document.getElementById("custodioSelectEdit");
            if (selCustodio && item.custodio) {
                const match = custodios.find((c) => String(c.nombre).trim().toLowerCase() === String(item.custodio).trim().toLowerCase());
                if (match) selCustodio.value = String(match.id);
            }
        } catch (error) {
            showToast("Error", error.message || "No se pudo abrir el editor.", "danger");
        }
    }

    async function saveCustodioChange(item, idCustodio) {
        try {
            const response = await apiFetch(`/inventario/${item.id}/custodio`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ idCustodio })
            });
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "No se pudo actualizar el custodio.");
            }
            closeModal();
            showUpdatedInventoryRow(payload.data);
            showToast("Custodio actualizado", "El cambio se guardo correctamente.", "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo guardar el cambio de custodio.", "danger");
        }
    }

    async function openStateEditor(item) {
        openModal(
            `Cambiar estado (${escapeHtml(item.codigoSbai || item.id)})`,
            `
                <div class="field-group">
                    <label for="nuevoEstadoSelect">Estado</label>
                    <select id="nuevoEstadoSelect">
                        ${VALID_STATES.map((estado) => `<option value="${estado}" ${normalizeState(item.estado) === estado ? "selected" : ""}>${estado}</option>`).join("")}
                    </select>
                </div>
            `,
            [
                { label: "Cancelar", className: "btn btn-secondary", onClick: closeModal },
                {
                    label: "Guardar",
                    className: "btn btn-primary",
                    onClick: async () => {
                        const estado = document.getElementById("nuevoEstadoSelect")?.value || "";
                        await saveStateChange(item, estado);
                    }
                }
            ]
        );
    }

    function showUpdatedInventoryRow(updated) {
        if (!updated) {
            return;
        }
        const normalized = normalizeItems([updated])[0];
        const index = state.inventory.findIndex((row) => row.id === normalized.id);
        if (index >= 0) {
            state.inventory[index] = normalized;
        } else {
            state.inventory.push(normalized);
        }
        const filteredIndex = state.filteredInventory.findIndex((row) => row.id === normalized.id);
        if (filteredIndex >= 0) {
            state.filteredInventory[filteredIndex] = normalized;
        }
        refreshVisibleInventoryRow(normalized);
        if (typeof refreshInventoryAutocompletes === "function") {
            refreshInventoryAutocompletes();
        }
    }

    function refreshVisibleInventoryRow(item) {
        const totalPages = Math.max(1, Math.ceil(state.filteredInventory.length / PAGE_SIZE));
        state.inventoryPage = Math.min(state.inventoryPage, totalPages);
        const start = (state.inventoryPage - 1) * PAGE_SIZE;
        const visibleIds = state.filteredInventory.slice(start, start + PAGE_SIZE).map((row) => Number(row.id));
        if (!visibleIds.includes(Number(item.id))) {
            return;
        }

        const tableRow = document.querySelector(`#inventoryBody tr[data-inventory-id="${item.id}"]`);
        if (tableRow) {
            const wrapper = document.createElement("tbody");
            wrapper.innerHTML = renderInventoryRow(item).trim();
            const updatedRow = wrapper.firstElementChild;
            updatedRow.classList.add("inventory-row--updated");
            tableRow.replaceWith(updatedRow);
        }

        const mobileCard = document.querySelector(`#inventoryMobile [data-inventory-id="${item.id}"]`);
        if (mobileCard) {
            const wrapper = document.createElement("div");
            wrapper.innerHTML = renderMobileInventoryCard(item).trim();
            const updatedCard = wrapper.firstElementChild;
            updatedCard.classList.add("inventory-row--updated");
            mobileCard.replaceWith(updatedCard);
        }

        updateInventoryStats();
    }

    async function saveStateChange(item, estado) {
        try {
            const response = await apiFetch(`/inventario/${item.id}/estado`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ estado })
            });
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "No se pudo actualizar el estado.");
            }
            closeModal();
            showUpdatedInventoryRow(payload.data);
            showToast("Estado actualizado", "El estado del equipo fue actualizado.", "success");
        } catch (error) {
            showToast("Error", error.message || "No se pudo guardar el estado.", "danger");
        }
    }

    function renderHistoryVersionCardsLegacy(historial) {
        const usableRows = historial.filter((h) => {
            const accion = String(h.accion || "").toLowerCase();
            return !accion.includes("completa del equipo") && !String(h.valorNuevo || "").toLowerCase().includes("campos actualizados:");
        });
        const rows = usableRows.length ? usableRows : historial;
        return `
            <div style="display:grid;gap:12px;">
                ${rows.map((h, index) => {
                    const version = rows.length - index;
                    const ant = h.valorAnterior && h.valorAnterior !== "?" ? escapeHtml(h.valorAnterior) : "Sin dato anterior";
                    const nvo = h.valorNuevo && h.valorNuevo !== "?" ? escapeHtml(h.valorNuevo) : "Sin dato actualizado";
                    const rol = escapeHtml(h.rol || "ADMINISTRADOR");
                    const usuario = escapeHtml(h.usuario || "-");
                    const fecha = h.fecha ? new Date(h.fecha).toLocaleString("es-EC") : "-";
                    return `<article style="border:1px solid #dce7f3;border-radius:14px;background:#fff;padding:14px;box-shadow:0 8px 20px rgba(15,31,56,.05);">
                        <div style="display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;margin-bottom:10px;">
                            <strong style="color:var(--primary);">Versión ${version} · ${escapeHtml(h.accion || "Cambio")}</strong>
                            <span style="color:#61708a;font-size:12px;">${fecha}</span>
                        </div>
                        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
                            <div style="background:#fff8e1;border:1px solid #ffe2a8;border-radius:10px;padding:10px;"><strong>Antes</strong><div style="margin-top:6px;">${ant}</div></div>
                            <div style="background:#eaf7ee;border:1px solid #ccebd5;border-radius:10px;padding:10px;"><strong>Después</strong><div style="margin-top:6px;color:#155724;font-weight:600;">${nvo}</div></div>
                        </div>
                        <div style="margin-top:10px;color:#61708a;font-size:12px;border-top:1px solid #eef2f9;padding-top:8px;">
                            Cambio realizado por: <strong>${usuario}</strong> -  Rol: <strong>${rol}</strong>
                        </div>
                    </article>`;
                }).join("")}
            </div>
        `;
    }

    function renderHistoryVersionCards(historial) {
        const usableRows = historial.filter((h) => {
            const accion = String(h.accion || "").toLowerCase();
            return !accion.includes("completa del equipo") && !String(h.valorNuevo || "").toLowerCase().includes("campos actualizados:");
        });
        const rows = usableRows.length ? usableRows : historial;
        return `
            <div class="history-timeline">
                ${rows.map((h, index) => {
                    const version = rows.length - index;
                    const ant = h.valorAnterior && h.valorAnterior !== "?" ? escapeHtml(h.valorAnterior) : "Sin dato anterior";
                    const nvo = h.valorNuevo && h.valorNuevo !== "?" ? escapeHtml(h.valorNuevo) : "Sin dato actualizado";
                    const rol = escapeHtml(h.rol || "ADMINISTRADOR");
                    const usuario = escapeHtml(h.usuario || "-");
                    const fecha = h.fecha ? new Date(h.fecha).toLocaleString("es-EC") : "-";
                    const campo = h.campoModificado || h.campo || h.campo_modificado || extractHistoryFieldName(h.accion);
                    const fieldsHtml = renderHistoryComparisonRows(h, campo);
                    return `<article class="history-card">
                        <div class="history-card__header">
                            <div>
                                <strong>Versión ${version} - ${escapeHtml(h.accion || "Cambio")}</strong>
                                <span class="history-changed-label">Campo cambiado: ${escapeHtml(campo || "No especificado")}</span>
                            </div>
                            <time>${fecha}</time>
                        </div>
                        <div class="history-diff">
                            <div class="history-diff__old"><span>Antes</span><strong>${ant}</strong></div>
                            <div class="history-diff__new"><span>Después</span><strong>${nvo}</strong></div>
                        </div>
                        ${fieldsHtml}
                        <div class="history-card__footer">
                            <span>Usuario: <strong>${usuario}</strong></span>
                            <span>Rol: <strong>${rol}</strong></span>
                            <span>Fecha y hora: <strong>${fecha}</strong></span>
                        </div>
                    </article>`;
                }).join("")}
            </div>
        `;
    }

    function renderHistoryComparisonRows(historyItem, campo) {
        const before = Array.isArray(historyItem.camposAntes) ? historyItem.camposAntes : [];
        const after = Array.isArray(historyItem.camposDespues) ? historyItem.camposDespues : [];
        const fallback = Array.isArray(historyItem.camposRegistro) ? historyItem.camposRegistro : [];
        const beforeMap = mapHistoryFields(before.length ? before : fallback);
        const afterMap = mapHistoryFields(after.length ? after : fallback);
        const normalizedChanged = normalizeHistoryFieldLabel(campo);
        const keys = Array.from(new Set(Object.keys(beforeMap).concat(Object.keys(afterMap))));

        if (!keys.length) {
            return "";
        }

        return `<div class="history-comparison">
            <div class="history-comparison__head">Campo</div>
            <div class="history-comparison__head">Antes</div>
            <div class="history-comparison__head">Después</div>
            ${keys.map((key) => {
                const beforeField = beforeMap[key] || afterMap[key] || {};
                const afterField = afterMap[key] || beforeMap[key] || {};
                const label = afterField.label || beforeField.label || key;
                const changed = normalizeHistoryFieldLabel(label) === normalizedChanged;
                const beforeValue = beforeField.valor ?? beforeField.value ?? "";
                const afterValue = afterField.valor ?? afterField.value ?? "";
                return `<div class="history-comparison__field ${changed ? "history-comparison__field--changed" : ""}">
                    <span>${escapeHtml(label)}</span>
                    ${changed ? '<strong>Campo cambiado</strong>' : ""}
                </div>
                <div class="history-comparison__value ${changed ? "history-comparison__value--old" : ""}">${escapeHtml(beforeValue === "" || beforeValue == null ? "-" : beforeValue)}</div>
                <div class="history-comparison__value ${changed ? "history-comparison__value--new" : ""}">${escapeHtml(afterValue === "" || afterValue == null ? "-" : afterValue)}</div>`;
            }).join("")}
        </div>`;
    }

    function mapHistoryFields(fields) {
        return fields.reduce((acc, field) => {
            const label = field.label || field.campo || field.name || "";
            const key = normalizeHistoryFieldLabel(label);
            if (key) {
                acc[key] = field;
            }
            return acc;
        }, {});
    }

    function extractHistoryFieldName(action) {
        const text = String(action || "").trim();
        const lower = text.toLowerCase();
        const separator = text.indexOf(":");
        if (lower.startsWith("edici") && separator >= 0) {
            const field = text.slice(separator + 1).trim();
            return normalizeHistoryFieldLabel(field).includes("id custodio") ? "Custodio actual" : field;
        }
        if (text.toLowerCase().includes("custodio")) {
            return "Custodio actual";
        }
        if (text.toLowerCase().includes("estado")) {
            return "Estado";
        }
        return "Equipo";
    }

    function normalizeHistoryFieldLabel(value) {
        return String(value || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, " ")
            .trim();
    }

    async function openHistoryViewer(item) {
        try {
            const response = await apiFetch(`/inventario/${item.id}/historial`);
            const payload = await response.json();
            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "No se pudo consultar el historial.");
            }
            const historial = Array.isArray(payload.data) ? payload.data : [];
            const bodyHtml = historial.length
                ? `<div style="display:flex;flex-direction:column;gap:10px;">
                    ${historial.map((h) => {
                        const ant = escapeHtml(h.valorAnterior || "-");
                        const nvo = escapeHtml(h.valorNuevo || "-");
                        const fecha = h.fecha ? new Date(h.fecha).toLocaleString("es-EC") : "-";
                        const rolLabel = h.rol ? escapeHtml(h.rol) : "TÉCNICO";
                        const usuLabel = h.usuario ? escapeHtml(h.usuario) : "-";
                        return `<div style="background:#f8fafd;border:1px solid #dce7f3;border-radius:10px;padding:12px 14px;">
                            <div style="font-weight:600;color:var(--primary);margin-bottom:6px;font-size:14px;">${escapeHtml(h.accion || "Cambio")}</div>
                            <div style="display:grid;grid-template-columns:1fr 1fr;gap:4px 14px;font-size:13px;">
                                <div style="color:#61708a;">Antes:</div>
                                <div style="color:#61708a;">Ahora:</div>
                                <div style="background:#fff3cd;border-radius:5px;padding:3px 7px;font-weight:500;">${ant}</div>
                                <div style="background:#d4edda;border-radius:5px;padding:3px 7px;font-weight:600;color:#155724;">${nvo}</div>
                            </div>
                            <div style="margin-top:8px;font-size:12px;color:#61708a;border-top:1px solid #eef2f9;padding-top:6px;display:flex;justify-content:space-between;flex-wrap:wrap;gap:4px;">
                                <span>Registrado por: <strong>${usuLabel}</strong> - Rol: <strong>${rolLabel}</strong></span>
                                <span> - ${fecha}</span>
                            </div>
                        </div>`;
                    }).join("")}
                  </div>`
                : '<p style="color:#61708a;text-align:center;padding:24px;">No hay historial registrado para este equipo.</p>';

            openModal(
                `Historial - ${escapeHtml(item.codigoSbai || item.id)}`,
                historial.length ? renderHistoryVersionCards(historial) : bodyHtml,
                [{ label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }]
            );
        } catch (error) {
            showToast("Error", error.message || "No se pudo abrir el historial.", "danger");
        }
    }
// ===== PÁGINA DE NUEVO EQUIPO =====
    function renderNewEquipmentPage() {
        return `
            <section class="panel panel--narrow">
                <div class="inventory-header inventory-header--form">
                    <div>
                        <div class="eyebrow">Nuevo equipo</div>
                        <h2>Registro por tipo de activo</h2>
                    </div>
                </div>
                <div class="field-group">
                    <label for="equipmentCategory">Categoría</label>
                    <select id="equipmentCategory">
                        <option value="">Cargando tipos...</option>
                    </select>
                </div>
                <div class="helper-banner" id="equipmentCategoryHint"></div>
                <form id="newEquipmentForm">
                    <div class="form-grid" id="dynamicEquipmentFields"></div>
                    <div class="form-actions" style="margin-top:18px;">
                        <button class="btn btn-primary" type="button" id="saveNewEquipmentButton">Guardar equipo</button>
                        <button class="btn btn-secondary" type="reset">Limpiar</button>
                    </div>
                </form>
                <div class="report-history" id="equipmentFieldSummary" style="margin-top:18px;"></div>
            </section>
        `;
    }

    function renderDynamicFields(category) {
        const container = document.getElementById("dynamicEquipmentFields");
        const hint = document.getElementById("equipmentCategoryHint");
        const summary = document.getElementById("equipmentFieldSummary");
        if (!container || !hint || !summary) {
            return;
        }
        const categoryConfig = state.equipmentFieldCatalog[category];
        if (!categoryConfig) {
            container.innerHTML = "";
            hint.textContent = "";
            summary.innerHTML = "";
            return;
        }
        const baseFields = Array.isArray(categoryConfig.fields) ? categoryConfig.fields : [];
        const esLicencia = category === "licencia";
        const fields = esLicencia ? baseFields.concat(CAMPO_COMPUTADOR_LICENCIA) : baseFields;
        container.innerHTML = renderDynamicDbFieldGroups(fields);
        setupNewEquipmentAutocompletes();
        if (esLicencia) {
            bindComputadorLicencia({}, "#newEquipmentForm");
        }
        hint.textContent = "";
        summary.innerHTML = "";
    }

    // ===== MODALES DE ACCION (accesibles desde onclick en la tabla) =====

    // Campos editables por tipo de equipo
    const CAMPOS_TIPO = {
        pc:              ["procesador", "ram", "disco_duro", "so", "ip"],
        laptop:          ["procesador", "ram", "disco_duro", "so", "ip"],
        impresora:       ["tipo_impresora", "ip", "caracteristicas"],
        periferico:      ["tipo_periferico", "caracteristicas"],
        escaner:         ["caracteristicas"],
        telefono:        ["caracteristicas"],
        proyector:       ["caracteristicas", "acta_ugdt", "acta_ugad", "anotaciones"],
        infraestructura: ["subtipo", "caracteristicas", "acta_ugdt", "acta_ugad", "anotaciones"],
        licencia:        ["caracteristicas", "acta_ugdt", "acta_ugad", "anotaciones"],
        bien_control_admin: ["codigo_anterior"],
        modem:           ["numero_contrato", "numero_servicio", "plan_comercial", "estado_servicio", "megas", "acreditacion", "caracteristicas", "anotaciones"]
    };
    const LABEL_CAMPO = {
        procesador: "Procesador", ram: "RAM", disco_duro: "Disco Duro", so: "Sistema Operativo", ip: "IP",
        tipo_impresora: "Tipo Impresora", tipo_periferico: "Tipo Periférico", subtipo: "Subtipo",
        caracteristicas: "Características", anotaciones: "Anotaciones", codigo_anterior: "Código Anterior",
        acta_ugdt: "Acta UGDT", acta_ugad: "Acta UGAD",
        numero_contrato: "Número Contrato", numero_servicio: "Número Servicio",
        plan_comercial: "Plan Comercial", estado_servicio: "Estado Servicio", megas: "Megas",
        acreditacion: "Acreditación"
    };

    // Asignación de la licencia a su PC/laptop: el usuario elige con qué dato identifica el equipo
    const CRITERIOS_COMPUTADOR_LICENCIA = [
        { value: "codigo_sbye", label: "Código SBYE", ejemplo: "25208289" },
        { value: "codigo_megan", label: "Código Megan", ejemplo: "141.01.07.1778.251" },
        { value: "ip", label: "IP", ejemplo: "10.0.85.117" }
    ];
    const CAMPO_COMPUTADOR_LICENCIA = { name: "computador_asignacion", custom: renderComputadorLicenciaField };

    // Siempre inicia en "Seleccione una opción"; al elegir una, se carga el dato del equipo ya asignado
    function prepararComputadorLicencia(values) {
        values.computador_asignacion = { criterio: "", valor: "" };
    }

    // Desplegable "Asignar equipo" y, debajo, el campo que aparece al elegir una opción
    function renderComputadorLicenciaField(asignacion) {
        const criterio = asignacion?.criterio || "";
        const actual = CRITERIOS_COMPUTADOR_LICENCIA.find((item) => item.value === criterio);
        return `
            <div style="display:grid; gap:14px; align-content:start;">
                <label class="field-group">
                    <span>Asignar equipo</span>
                    <select name="computador_criterio" data-optional="true">
                        <option value="">Seleccione una opción</option>
                        ${CRITERIOS_COMPUTADOR_LICENCIA.map((item) => `<option value="${item.value}" ${item.value === criterio ? "selected" : ""}>${escapeHtml(item.label)}</option>`).join("")}
                    </select>
                </label>
                <label class="field-group ${actual ? "" : "hidden"}" id="computadorValorGroup">
                    <span id="computadorValorLabel">${actual ? `Asignar equipo por ${escapeHtml(actual.label)}` : ""}</span>
                    <input type="text" name="computador_valor" data-optional="true" value="${escapeHtml(asignacion?.valor || "")}" placeholder="${actual ? `Ej: ${escapeHtml(actual.ejemplo)}` : ""}" autocomplete="off">
                </label>
            </div>`;
    }

    // formSelector: "#editEquipmentForm" (Editar) o "#newEquipmentForm" (Nuevo Equipo)
    function bindComputadorLicencia(values, formSelector = "#editEquipmentForm") {
        const select = document.querySelector(`${formSelector} [name="computador_criterio"]`);
        const group = document.querySelector(`${formSelector} #computadorValorGroup`);
        const label = document.querySelector(`${formSelector} #computadorValorLabel`);
        const input = document.querySelector(`${formSelector} [name="computador_valor"]`);
        select?.addEventListener("change", () => {
            const actual = CRITERIOS_COMPUTADOR_LICENCIA.find((item) => item.value === select.value);
            group?.classList.toggle("hidden", !actual);
            if (!actual || !input) {
                return;
            }
            label.textContent = `Asignar equipo por ${actual.label}`;
            input.placeholder = `Ej: ${actual.ejemplo}`;
            input.value = values[`computador_${actual.value}`] || "";
            input.focus();
        });
    }

    async function abrirModalEditar(idEquipo) {
        const item = state.inventory.find((i) => i.id === idEquipo);
        if (!item) { showToast("Error", "Equipo no encontrado en el inventario cargado.", "danger"); return; }

        try {
            if (!state.equipmentFieldCatalog || !Object.keys(state.equipmentFieldCatalog).length) {
                await loadEquipmentFieldCatalog();
            }

            const categoryConfig = state.equipmentFieldCatalog[item.tipo];
            if (!categoryConfig) {
                throw new Error("No se pudo cargar la definicion del formulario para este tipo de equipo.");
            }

            const detailResponse = await apiFetch(`/inventario/${idEquipo}/edicion`);
            const detailPayload = await detailResponse.json();
            if (!detailResponse.ok || !detailPayload.success) {
                throw new Error(detailPayload.message || "No se pudo cargar la informacion completa del equipo.");
            }

            const editValues = detailPayload.data || {};
            const baseFields = Array.isArray(categoryConfig.fields) ? categoryConfig.fields : [];
            const esLicencia = item.tipo === "licencia";
            if (esLicencia) {
                prepararComputadorLicencia(editValues);
            }
            const fields = esLicencia ? baseFields.concat(CAMPO_COMPUTADOR_LICENCIA) : baseFields;
            const bodyHtml = `
                <div class="helper-banner" style="margin-bottom:16px;">Rol administrativo con edicion completa habilitada para todos los campos del formulario.</div>
                <form id="editEquipmentForm">
                    <div class="form-grid" id="editFormGrid">${renderDynamicDbEditFieldGroups(fields, editValues)}</div>
                </form>`;

            openModal(`Editar equipo ${escapeHtml(item.codigoSbai || String(idEquipo))} - ${(categoryConfig.label || item.tipo || "").toUpperCase()}`, bodyHtml, [
                {
                    label: "Guardar cambios", className: "btn btn-primary", onClick: async () => {
                        const form = document.getElementById("editEquipmentForm");
                        if (!form) return;
                        const payload = collectEquipmentFormPayload(form);
                        try {
                            const r = await apiFetch(`/inventario/${item.tipo}/${idEquipo}`, {
                                method: "PUT",
                                headers: { "Content-Type": "application/json" },
                                body: JSON.stringify(payload)
                            });
                            const p = await r.json();
                            if (!r.ok || !p.success) throw new Error(p.message || "Error al guardar");
                            showUpdatedInventoryRow(p.data);
                            showToast("Guardado", "Equipo actualizado correctamente.", "success");
                            closeModal();
                        } catch (err) {
                            showToast("Error", err.message, "danger");
                        }
                    }
                },
                { label: "Cancelar", className: "btn btn-secondary", onClick: closeModal }
            ]);

            setupEditEquipmentAutocompletes();
            if (esLicencia) {
                bindComputadorLicencia(editValues);
            }
        } catch (error) {
            showToast("Error", error.message || "No se pudo abrir el editor completo.", "danger");
        }
    }

    async function abrirModalCustodio(idEquipo) {
        const item = state.inventory.find((i) => i.id === idEquipo);
        if (!item) {
            showToast("Error", "Equipo no encontrado en el inventario cargado.", "danger");
            return;
        }
        return openCustodioEditor(item);
    }

    async function abrirModalEstado(idEquipo, estadoActual) {
        const opcionesEstado = VALID_STATES.map((s) => `<option value="${s}" ${s === estadoActual ? "selected" : ""}>${s}</option>`).join("");
        openModal("Cambiar Estado", `
            <div class="field-group">
                <label for="estadoSelect">Nuevo estado</label>
                <select id="estadoSelect" style="width:100%;padding:10px;border-radius:10px;border:1px solid #dce7f3;">${opcionesEstado}</select>
            </div>
        `, [
            {
                label: "Guardar", className: "btn btn-primary", onClick: async () => {
                    const nuevoEstado = document.getElementById("estadoSelect").value;
                    try {
                        const r = await apiFetch(`/inventario/${idEquipo}/estado`, {
                            method: "PUT",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ estado: nuevoEstado })
                        });
                        const p = await r.json();
                        if (!r.ok || !p.success) throw new Error(p.message || "Error al actualizar");
                        const idx = state.inventory.findIndex((i) => i.id === idEquipo);
                        if (idx !== -1) {
                            state.inventory[idx].estado = normalizeState(nuevoEstado);
                            renderInventory();
                            refreshInventoryAutocompletes();
                        }
                        showToast("Listo", "Estado actualizado correctamente.", "success");
                        closeModal();
                    } catch (err) {
                        showToast("Error", err.message, "danger");
                    }
                }
            },
            { label: "Cancelar", className: "btn btn-secondary", onClick: closeModal }
        ]);
    }

    async function abrirModalHistorial(idEquipo) {
        openModal("Historial del Equipo", `<div id="historialContent" style="min-height:80px;"><p style="color:#61708a;padding:16px;">Cargando historial...</p></div>`, [
            { label: "Cerrar", className: "btn btn-secondary", onClick: closeModal }
        ]);
        try {
            const res = await apiFetch(`/inventario/${idEquipo}/historial`);
            const payload = await res.json();
            const historial = Array.isArray(payload.data) ? payload.data : [];
            const container = document.getElementById("historialContent");
            if (!historial.length) {
                container.innerHTML = `<p style="color:#61708a;text-align:center;padding:24px;">Sin registros de historial para este equipo.</p>`;
                return;
            }
            container.innerHTML = renderHistoryVersionCards(historial);
        } catch (e) {
            document.getElementById("historialContent").innerHTML = `<p style="color:red;padding:12px;">Error: ${escapeHtml(e.message)}</p>`;
        }
    }

    async function loadHistoricActas() {
        const container = document.getElementById("actasListaContainer");
        if (!container) return;
        try {
            const response = await apiFetch("/actas/listar");
            if (!response.ok) {
                throw new Error("No se pudo cargar la lista de actas.");
            }
            const payload = await response.json();
            const actas = Array.isArray(payload.data) ? payload.data : [];
            if (actas.length === 0) {
                container.innerHTML = `<p style="color: #61708a; padding: 16px; text-align: center;">No hay actas registradas en el sistema.</p>`;
                return;
            }

            container.innerHTML = `
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>ID Acta</th>
                                <th>Tipo de Acta</th>
                                <th>Equipo / Código</th>
                                <th>Custodio</th>
                                <th>Creado Por</th>
                                <th>Fecha de Creación</th>
                                <th>Descargar</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${actas.map(acta => {
                                const tipoLabel = {
                                    rc: "Redes y Comunicaciones",
                                    software: "Software",
                                    pc: "PC",
                                    impresora: "Impresora",
                                    telefono: "Teléfono",
                                    proyector: "Proyector",
                                    escaner: "Escáner"
                                }[acta.tipoActa] || acta.tipoActa;

                                const dateStr = acta.fechaCreacion ? new Date(acta.fechaCreacion).toLocaleString("es-EC") : "-";

                                return `
                                    <tr>
                                        <td><strong>#${acta.idActa}</strong></td>
                                        <td>${escapeHtml(tipoLabel)}</td>
                                        <td>${escapeHtml(acta.codigoEquipo || "Varios / Manual")}</td>
                                        <td>${escapeHtml(acta.nombreCustodio)}</td>
                                        <td>${escapeHtml(acta.creadoPor)}</td>
                                        <td>${dateStr}</td>
                                        <td>
                                            <div style="display: flex; gap: 8px;">
                                                <button type="button" class="btn btn-secondary btn-sm" onclick="descargarActaArchivo(${acta.idActa}, 'pdf')" style="padding: 4px 8px; font-size: 11px; border-radius: 6px;">PDF</button>
                                                <button type="button" class="btn btn-secondary btn-sm" onclick="descargarActaArchivo(${acta.idActa}, 'docx')" style="padding: 4px 8px; font-size: 11px; border-radius: 6px;">DOCX</button>
                                            </div>
                                        </td>
                                    </tr>
                                `;
                            }).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        } catch (error) {
            container.innerHTML = `<p style="color: red; padding: 16px; text-align: center;">Error al cargar actas: ${escapeHtml(error.message)}</p>`;
        }
    }

    async function descargarActaArchivo(id, format) {
        try {
            showToast("Generando archivo", "Descargando el documento en formato " + format.toUpperCase() + "...", "info");
            const response = await apiFetch(`/actas/descargar/${id}/${format}`);
            if (!response.ok) {
                throw new Error("No se pudo descargar el acta.");
            }
            const blob = await response.blob();
            const disposition = response.headers.get("Content-Disposition") || "";
            const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
            const filename = match?.[1] || `acta_${id}.${format}`;
            downloadBlob(blob, filename);
            showToast("Descarga lista", "Archivo descargado con éxito.", "success");
        } catch (error) {
            showToast("Error", error.message || "Error al descargar el archivo.", "danger");
        }
    }

    if (typeof window !== "undefined") {
        window.openModal = openModal;
        window.closeModal = closeModal;
        window.showToast = showToast;
        window.formatDate = formatDate;
        window.abrirModalEditar = abrirModalEditar;
        window.abrirModalCustodio = abrirModalCustodio;
        window.abrirModalEstado = abrirModalEstado;
        window.abrirModalHistorial = abrirModalHistorial;
        window.descargarActaArchivo = descargarActaArchivo;
    }
})();
