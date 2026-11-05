/**
 * Ejemplo de integración LDAP en el frontend (JavaScript)
 * 
 * Requiere que el servidor REST esté en funcionamiento
 * Base URL: http://localhost:8080/api/login
 */

// URL del endpoint REST
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Realiza login contra LDAP/BD
 * @param {string} usuario - Usuario sin dominio (ej: "jdoe")
 * @param {string} password - Contraseña
 * @param {string} rolElegido - (Opcional) Rol a seleccionar si hay múltiples
 */
async function realizarLogin(usuario, password, rolElegido = null) {
    try {
        const response = await fetch(`${API_BASE_URL}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include', // Incluir cookies
            body: JSON.stringify({
                username: usuario.trim(),
                password: password,
                rolElegido: rolElegido
            })
        });

        const data = await response.json();

        if (data.success) {
            console.log(`✓ Autenticación exitosa vía: ${data.message}`);
            
            // Si requiere selección de perfil y no lo hizo
            if (data.requiereSeleccionPerfil) {
                mostrarSelectorRoles(data.data.rolesDisponibles, usuario, password);
                return;
            }

            // Login exitoso
            guardarSesion(data.data);
            redirigirAlDashboard(data.data);
        } else {
            console.error('✗ Error de autenticación:', data.message);
            mostrarError(data.message);
        }
    } catch (error) {
        console.error('Error al conectar con servidor:', error);
        mostrarError('Error de conexión con el servidor');
    }
}

/**
 * Muestra selector de roles si el usuario tiene múltiples
 */
function mostrarSelectorRoles(roles, usuario, password) {
    const modal = document.getElementById('selectorRolesModal');
    const selectRol = document.getElementById('selectRol');
    
    // Limpiar opciones previas
    selectRol.innerHTML = '';
    
    // Agregar opciones de rol
    roles.forEach(rol => {
        const option = document.createElement('option');
        option.value = rol;
        option.textContent = formatearRol(rol);
        selectRol.appendChild(option);
    });

    // Mostrar modal
    modal.style.display = 'block';

    // Evento al confirmar selección
    document.getElementById('confirmarRolBtn').onclick = () => {
        const rolSeleccionado = selectRol.value;
        modal.style.display = 'none';
        realizarLogin(usuario, password, rolSeleccionado);
    };
}

/**
 * Formatea el nombre del rol para mostrar
 */
function formatearRol(rol) {
    const map = {
        'ADMINISTRADOR': 'Administrador',
        'TECNICO': 'Técnico',
        'CUSTODIO': 'Custodio'
    };
    return map[rol] || rol;
}

/**
 * Guarda la información del usuario en sesión local
 */
function guardarSesion(usuarioData) {
    // Guardar en sessionStorage (se limpia al cerrar navegador)
    sessionStorage.setItem('usuario', JSON.stringify(usuarioData));
    
    // Guardar en localStorage (persiste entre sesiones - opcional)
    // localStorage.setItem('usuarioInfo', JSON.stringify(usuarioData));
}

/**
 * Obtiene el usuario actual de la sesión
 */
function obtenerUsuarioActual() {
    const usuario = sessionStorage.getItem('usuario');
    return usuario ? JSON.parse(usuario) : null;
}

/**
 * Redirige al dashboard después de login exitoso
 */
function redirigirAlDashboard(usuarioData) {
    console.log(`Bienvenido ${usuarioData.nombreCompleto} (${usuarioData.rol})`);
    
    // Redirigir según el rol
    switch (usuarioData.rol) {
        case 'ADMINISTRADOR':
            window.location.href = '/dashboard.html?rol=admin';
            break;
        case 'TECNICO':
            window.location.href = '/dashboard.html?rol=tecnico';
            break;
        case 'CUSTODIO':
            window.location.href = '/dashboard.html?rol=custodio';
            break;
        default:
            window.location.href = '/dashboard.html';
    }
}

/**
 * Muestra mensaje de error
 */
function mostrarError(mensaje) {
    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-danger';
    alertDiv.textContent = mensaje;
    
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.insertBefore(alertDiv, loginForm.firstChild);
        
        // Auto-remover después de 5 segundos
        setTimeout(() => alertDiv.remove(), 5000);
    }
}

/**
 * Cierra la sesión
 */
async function realizarLogout() {
    try {
        const response = await fetch(`${API_BASE_URL}/login/logout`, {
            method: 'POST',
            credentials: 'include'
        });

        const data = await response.json();
        
        if (data.success) {
            // Limpiar datos locales
            sessionStorage.removeItem('usuario');
            localStorage.removeItem('usuarioInfo');
            
            // Redirigir a login
            window.location.href = '/index.html';
        }
    } catch (error) {
        console.error('Error al cerrar sesión:', error);
    }
}

/**
 * Obtiene la información del usuario actual desde servidor
 */
async function obtenerUsuarioActualDelServidor() {
    try {
        const response = await fetch(`${API_BASE_URL}/login/actual`, {
            method: 'GET',
            credentials: 'include'
        });

        const data = await response.json();
        
        if (data.success) {
            guardarSesion(data.data);
            return data.data;
        } else {
            // Usuario no autenticado
            window.location.href = '/index.html';
            return null;
        }
    } catch (error) {
        console.error('Error obteniendo usuario actual:', error);
        return null;
    }
}

/**
 * Verifica si el usuario tiene un permiso específico
 */
function tienePermiso(nombrePermiso) {
    const usuario = obtenerUsuarioActual();
    if (!usuario || !usuario.permisos) {
        return false;
    }
    return usuario.permisos[nombrePermiso] === true;
}

/**
 * Verifica si el usuario tiene un rol específico
 */
function tieneRol(rol) {
    const usuario = obtenerUsuarioActual();
    if (!usuario || !usuario.rolesDisponibles) {
        return false;
    }
    return usuario.rolesDisponibles.includes(rol);
}

// Ejemplo de uso en HTML:
/*

<form id="loginForm" onsubmit="handleLoginSubmit(event)">
    <div class="form-group">
        <label for="usuario">Usuario:</label>
        <input type="text" id="usuario" name="usuario" required>
    </div>
    <div class="form-group">
        <label for="password">Contraseña:</label>
        <input type="password" id="password" name="password" required>
    </div>
    <button type="submit" class="btn btn-primary">Ingresar</button>
</form>

<script>
function handleLoginSubmit(event) {
    event.preventDefault();
    const usuario = document.getElementById('usuario').value;
    const password = document.getElementById('password').value;
    realizarLogin(usuario, password);
}

// Proteger página: verificar autenticación al cargar
window.addEventListener('load', () => {
    const usuario = obtenerUsuarioActual();
    if (!usuario) {
        obtenerUsuarioActualDelServidor();
    }
});
</script>

<!-- Modal selector de roles -->
<div id="selectorRolesModal" class="modal" style="display:none;">
    <div class="modal-content">
        <h2>Seleccione su rol</h2>
        <select id="selectRol"></select>
        <button id="confirmarRolBtn" class="btn btn-primary">Confirmar</button>
    </div>
</div>

*/

// Ejemplo de uso en componente Vue.js:
/*

<template>
    <div class="login-container">
        <form @submit.prevent="handleLogin">
            <input v-model="usuario" type="text" placeholder="Usuario">
            <input v-model="password" type="password" placeholder="Contraseña">
            <button type="submit">Ingresar</button>
        </form>
        <div v-if="error" class="error">{{ error }}</div>
    </div>
</template>

<script>
export default {
    data() {
        return {
            usuario: '',
            password: '',
            error: ''
        }
    },
    methods: {
        async handleLogin() {
            this.error = '';
            await realizarLogin(this.usuario, this.password);
        }
    }
}
</script>

*/

