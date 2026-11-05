(function () {
    window.SIDashboardView = {
        renderDashboard({ role }) {
            return `
                <section class="hero hero--dashboard">
                    <div class="hero__grid">
                        <div class="hero-copy">
                            <h2>Inventario institucional.</h2>
                            <p>Consulta equipos tecnológicos mediante búsquedas avanzadas por múltiples campos, 
                            ingresando información y aprovechando sugerencias basadas en registros previamente almacenados.</p>
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
    };
})();
