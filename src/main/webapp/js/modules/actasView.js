(function () {
    window.SIActasView = {
        renderHub({ basePath, iconMarkup, esCustodio }) {
            let formsHtml = "";
            if (!esCustodio) {
                const cards = [
                    ["Equipos", "Acta de mantenimiento preventivo de equipos.", `${basePath}/acta-equipos.html`],
                    ["Software", "Acta de programas y aplicaciones instaladas.", `${basePath}/acta-software.html`],
                    ["Redes y Comunicaciones", "Acta de mantenimiento preventivo de redes y comunicaciones.", `${basePath}/acta-rc.html`],
                    ["Licencias", "Acta de reunion y de entrega de recepción de licencias de software.", `${basePath}/acta-licencias.html`]
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
        },
//Buscador de equipos para autocompletar actas
        renderEquipmentSearch({ buildTypeOptions, renderAutocompleteField, validStates }) {
            return `
                <div class="acta-search-panel">
                    <div>
                        <div class="eyebrow">Búsqueda de equipo</div>
                        <h3>Localice el equipo para generar el acta</h3>
                        <p>Use los mismos criterios del módulo Inventario. Al seleccionar una coincidencia, el acta se autocompleta automáticamente.</p>
                    </div>
                    <div class="filters-grid filters-grid--inventory">
                        <div class="field-group">
                            <label for="actaFilterTipo">Tipo</label>
                            <select id="actaFilterTipo">
                                <option value="">Todos</option>
                                ${buildTypeOptions()}
                            </select>
                        </div>
                        ${renderAutocompleteField("actaFilterCodigoSbai", "Código SBYE", "Filtrar por código")}
                        ${renderAutocompleteField("actaFilterCodigoMegan", "Código Megan", "Filtrar por código")}
                        ${renderAutocompleteField("actaFilterDescripcion", "Descripción", "Filtrar por descripción")}
                        ${renderAutocompleteField("actaFilterMarca", "Marca", "Filtrar por marca")}
                        ${renderAutocompleteField("actaFilterModelo", "Modelo", "Filtrar por modelo")}
                        ${renderAutocompleteField("actaFilterSerie", "Número de serie", "Filtrar por número")}
                        ${renderAutocompleteField("actaFilterCustodio", "Custodio", "Filtrar por custodio")}
                        ${renderAutocompleteField("actaFilterEdificio", "Edificio", "Filtrar por edificio")}
                        ${renderAutocompleteField("actaFilterPiso", "Piso", "Filtrar por piso")}
                        ${renderAutocompleteField("actaFilterDireccion", "Dirección", "Filtrar por dirección")}
                        <div class="field-group">
                            <label for="actaFilterEstado">Estado</label>
                            <select id="actaFilterEstado">
                                <option value="">Todos</option>
                                ${validStates.map((item) => `<option value="${item}">${item}</option>`).join("")}
                            </select>
                        </div>
                    </div>
                    <div class="toolbar" style="margin-top:16px;">
                        <button type="button" class="btn btn-primary" id="actaEquipoBuscarButton">Buscar</button>
                    </div>
                    <div id="actaEquipoResultados" class="acta-search-results"></div>
                </div>
            `;
        }
    };
})();
