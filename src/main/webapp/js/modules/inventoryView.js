(function () {
    function renderPage(ctx) {
        const canExport = ctx.canExportInventory();
        return `
            <section class="panel inventory-panel">
                <div class="inventory-header">
                    <div>
                        <div class="eyebrow">Módulo de inventario</div>
                        <h2>Inventario institucional</h2>
                        <p id="inventoryIntroText">Utiliza los filtros para localizar equipos por código, custodio, ubicación, marca, modelo y estado.</p>
                    </div>
                </div>
                <div class="filters-grid filters-grid--inventory">
                    <div class="field-group">
                        <label for="filterTipo">Tipo</label>
                        <select id="filterTipo">
                            <option value="">Todos</option>
                            ${ctx.buildTypeOptions()}
                        </select>
                    </div>
                    ${ctx.renderAutocompleteField("filterCodigoSbai", "Código SBYE", "Filtrar por código")}
                    ${ctx.renderAutocompleteField("filterCodigoMegan", "Código Megan", "Filtrar por código")}
                    ${ctx.renderAutocompleteField("filterDescripcion", "Descripción", "Filtrar por descripción")}
                    ${ctx.renderAutocompleteField("filterMarca", "Marca", "Filtrar por marca")}
                    ${ctx.renderAutocompleteField("filterModelo", "Modelo", "Filtrar por modelo")}
                    ${ctx.renderAutocompleteField("filterSerie", "Número de serie", "Filtrar por número")}
                    ${ctx.renderAutocompleteField("filterCustodio", "Custodio", "Filtrar por custodio")}
                    ${ctx.renderAutocompleteField("filterEdificio", "Edificio", "Filtrar por edificio")}
                    ${ctx.renderAutocompleteField("filterPiso", "Piso", "Filtrar por piso")}
                    ${ctx.renderAutocompleteField("filterDireccion", "Dirección", "Filtrar por dirección")}
                    <div class="field-group">
                        <label for="filterEstado">Estado</label>
                        <select id="filterEstado">
                            ${ctx.validStates.map((item) => `<option value="${item}">${item}</option>`).join("")}
                        </select>
                    </div>
                </div>
                <div class="toolbar" style="margin-top:16px;">
                    <button class="btn btn-success hidden" id="exportInventoryExcel">Exportar a Excel</button>
                    <button class="btn btn-secondary hidden" id="exportInventoryPdf">Exportar a PDF</button>
                    <button class="btn btn-primary" id="applyInventoryFilters">Buscar</button>
                    <button class="btn btn-secondary" id="clearInventoryFilters">Limpiar Filtros</button>
                </div>
                <div id="inventoryMeta" class="search-results-meta"></div>
                <div id="inventoryCriteriaSummary" class="criteria-summary"></div>
                <div id="inventoryResultsSection" class="inventory-results-section hidden">
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    ${inventoryColumns(ctx).map((key) => `<th><button class="table-sort" data-sort="${key}">${ctx.labelForColumn(key)}</button></th>`).join("")}
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody id="inventoryBody"></tbody>
                        </table>
                    </div>
                    <div class="mobile-cards" id="inventoryMobile"></div>
                    <div class="pagination">
                        <span id="inventoryPaginationMeta"></span>
                        <div class="toolbar">
                            <button class="btn btn-secondary" id="prevPage">Anterior</button>
                            <button class="btn btn-secondary" id="nextPage">Siguiente</button>
                        </div>
                    </div>
                </div>
            </section>
        `;
    }

    function inventoryColumns(ctx) {
        const cols = [
            "codigoSbai",
            "codigoMegan",
            "descripcion",
            "tipo",
            "marca",
            "modelo",
            "numeroSerie",
            "custodio",
            "ubicacionEdificio",
            "ubicacionPiso",
            "ubicacionDireccion",
            "procesador",
            "estado"
        ];
        cols.push("ultimaActualizacion", "ultimoMantenimiento");
        return cols;
    }

    function buildActionButtons(item, options, ctx) {
        const buttons = [];
        const wrap = options?.wrap !== false;
        if (ctx.canEditAll()) {
            buttons.push(`<button class="icon-btn" type="button" data-inv-action="editar" data-id="${item.id}" aria-label="Editar equipo" title="Editar equipo">${ctx.iconMarkup("edit")}</button>`);
            buttons.push(`<button class="icon-btn icon-btn--danger" type="button" data-inv-action="eliminar" data-id="${item.id}" aria-label="Eliminar equipo" title="Eliminar equipo">${ctx.iconMarkup("delete")}</button>`);
        } else if (ctx.canEditCustodio()) {
            buttons.push(`<button class="icon-btn" type="button" data-inv-action="editar" data-id="${item.id}" aria-label="Editar custodio" title="Editar custodio">${ctx.iconMarkup("edit")}</button>`);
        }
        if (ctx.canChangeState()) {
            buttons.push(`<button class="icon-btn" type="button" data-inv-action="estado" data-id="${item.id}" aria-label="Cambiar estado" title="Cambiar estado">${ctx.iconMarkup("repeat")}</button>`);
        }
        if (ctx.canViewHistory()) {
            buttons.push(`<button class="icon-btn" type="button" data-inv-action="historial" data-id="${item.id}" aria-label="Ver historial" title="Ver historial">${ctx.iconMarkup("history")}</button>`);
        }
        if (!buttons.length) {
            return '<span class="muted">Sin acciones</span>';
        }
        const body = buttons.join("");
        return wrap ? `<div class="action-row">${body}</div>` : body;
    }

    function renderRow(item, ctx) {
        return `
            <tr data-inventory-id="${item.id}">
                <td>${ctx.escapeHtml(item.codigoSbai || "-")}</td>
                <td>${ctx.escapeHtml(item.codigoMegan || "-")}</td>
                <td>${ctx.escapeHtml(item.descripcion || "-")}</td>
                <td>${ctx.escapeHtml(ctx.displayInventoryType(item))}</td>
                <td>${ctx.escapeHtml(item.marca || "-")}</td>
                <td>${ctx.escapeHtml(item.modelo || "-")}</td>
                <td>${ctx.escapeHtml(item.numeroSerie || "-")}</td>
                <td>${ctx.escapeHtml(item.custodio || "-")}</td>
                <td>${ctx.escapeHtml(item.ubicacionEdificio || "-")}</td>
                <td>${ctx.escapeHtml(item.ubicacionPiso || "-")}</td>
                <td>${ctx.escapeHtml(item.ubicacionDireccion || "-")}</td>
                <td>${ctx.escapeHtml(item.procesador || item.caracteristicas || "-")}</td>
                <td>${ctx.stateBadge(item.estado)}</td>
                <td>${ctx.escapeHtml(item.ultimaActualizacion || "-")}</td>
                <td>${ctx.escapeHtml(item.ultimoMantenimiento || "-")}</td>
                <td>${ctx.buildInventoryActionButtons(item)}</td>
            </tr>
        `;
    }

    function renderMobileCard(item, ctx) {
        return `
            <article class="mobile-card" data-inventory-id="${item.id}">
                <strong>${ctx.escapeHtml(item.codigoSbai || "-")} · ${ctx.escapeHtml(ctx.displayInventoryType(item))}</strong>
                <span>Megan: ${ctx.escapeHtml(item.codigoMegan || "-")}</span>
                <span>Descripción: ${ctx.escapeHtml(item.descripcion || "-")}</span>
                <span>Marca / Modelo: ${ctx.escapeHtml(item.marca || "-")} ${ctx.escapeHtml(item.modelo || "")}</span>
                <span>Custodio: ${ctx.escapeHtml(item.custodio || "-")}</span>
                <span>Ubicación: ${ctx.escapeHtml(item.ubicacion || "-")}</span>
                <span>Detalle: ${ctx.escapeHtml(item.procesador || item.caracteristicas || "-")}</span>
                <span>Estado: ${ctx.stripHtml(ctx.stateBadge(item.estado))}</span>
                <div class="action-row" style="margin-top:10px;">${ctx.buildInventoryActionButtons(item, { wrap: false })}</div>
            </article>
        `;
    }

    function renderResults(ctx) {
        const tbody = document.getElementById("inventoryBody");
        const mobile = document.getElementById("inventoryMobile");
        if (!tbody || !mobile) {
            return;
        }
        const total = ctx.state.filteredInventory.length;
        const totalPages = Math.max(1, Math.ceil(total / ctx.pageSize));
        ctx.state.inventoryPage = Math.min(ctx.state.inventoryPage, totalPages);
        const start = (ctx.state.inventoryPage - 1) * ctx.pageSize;
        const pageItems = ctx.state.filteredInventory.slice(start, start + ctx.pageSize);
        const resultsSection = document.getElementById("inventoryResultsSection");
        const criteriaSummary = document.getElementById("inventoryCriteriaSummary");

        ctx.updateInventoryStats();
        if (resultsSection) {
            resultsSection.classList.toggle("hidden", !ctx.state.inventorySearchPerformed);
        }
        if (criteriaSummary) {
            criteriaSummary.innerHTML = ctx.state.inventorySearchPerformed
                ? `<strong>Búsqueda enviada:</strong> ${ctx.state.inventoryLastCriteria.length ? ctx.state.inventoryLastCriteria.map(ctx.escapeHtml).join(" · ") : "sin filtros, mostrando todo el inventario."}`
                : "";
        }
        ctx.setText("inventoryMeta", ctx.state.inventorySearchPerformed ? `${total} resultados filtrados · página ${ctx.state.inventoryPage} de ${totalPages}` : "Ingrese uno o varios criterios y presione Buscar.");
        ctx.setText("inventoryPaginationMeta", ctx.state.inventorySearchPerformed && pageItems.length ? `Mostrando ${start + 1}-${start + pageItems.length}` : "");
        // Mostrar botones de exportación solo si hay resultados de búsqueda
        const exportExcelBtn = document.getElementById("exportInventoryExcel");
        const exportPdfBtn = document.getElementById("exportInventoryPdf");
        if (exportExcelBtn) {
            exportExcelBtn.classList.toggle("hidden", !ctx.state.inventorySearchPerformed || !total);
        }
        if (exportPdfBtn) {
            exportPdfBtn.classList.toggle("hidden", !ctx.state.inventorySearchPerformed || !total);
        }
        tbody.innerHTML = pageItems.length
            ? pageItems.map(ctx.renderInventoryRow).join("")
            : `<tr><td colspan="12">${ctx.state.inventorySearchPerformed ? (total ? "No hay equipos para los filtros aplicados." : "Equipo no registrado.") : "La tabla se mostrará cuando realice una búsqueda."}</td></tr>`;
        mobile.innerHTML = pageItems.map(ctx.renderMobileInventoryCard).join("");
        ctx.refreshInventoryAutocompletes();
    }

    window.SIInventoryView = {
        renderPage,
        buildActionButtons,
        renderRow,
        renderMobileCard,
        renderResults
    };
})();
