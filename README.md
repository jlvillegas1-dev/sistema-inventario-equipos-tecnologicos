# Sistema de Inventario de Bienes Tecnológicos - SENADI

## 📋 Descripción del Proyecto

Sistema web modular desarrollado en **Java 11** con arquitectura **MVC** (Modelo-Vista-Controlador) para gestionar el inventario de equipos tecnológicos de la institución pública ecuatoriana SENADI. 

El sistema incluye:
- **Autenticación** con roles (Administrador y Técnico)
- **Gestión de inventario** (PCs, Laptops y otros bienes)
- **Histórico de cambios** automático para cada bien
- **Búsqueda avanzada** de equipos
- **Exportación a Excel** de reportes
- **Interfaz moderna** con colores institucionales

---

## 🏗️ Arquitectura Técnica

### Capas de la Aplicación

#### 1. **Capa de Modelo (Model)**
- Entidades JPA: `Usuario`, `HistoricoCambio`, `Custodio`, `Ubicacion`, `PC`, `Laptop`
- Ubicación: `src/main/java/.../model/`

#### 2. **Capa de Acceso a Datos (DAO)**
- DAOs genéricos y especializados para cada entidad
- Ubicación: `src/main/java/.../dao/`
- Clases principales: `DAOGenerico`, `UsuarioDAO`, `PCDAO`, `LaptopDAO`, etc.

#### 3. **Capa de Negocio (Service)**
- Servicios para lógica de negocio
- Ubicación: `src/main/java/.../service/`
- Clases principales: `UsuarioService`, `BienService`, `HistoricoService`

#### 4. **Capa de Presentación (Resources - JAX-RS)**
- Controladores REST para APIs
- Ubicación: `src/main/java/.../resources/`
- Recursos: `LoginResource`, `InventarioResource`, `BusquedaResource`, `HistoricoResource`, `ReportesResource`

#### 5. **Capa de Interfaz Gráfica (Frontend)**
- HTML5, CSS3, JavaScript vanilla
- Ubicación: `src/main/webapp/`
- Archivos: `index.html`, `css/estilos.css`, `js/main.js`

#### 6. **Utilidades**
- `JPAUtil`: Gestión de EntityManager
- `SesionUsuario`: Contexto de sesión
- `EncriptacionUtil`: Encriptación MD5

---

## 🗄️ Base de Datos

### Tablas Principales

```sql
-- Usuarios y autenticación
usuarios

-- Histórico de cambios
historico_cambios

-- Catálogos y referencias
custodios
ubicaciones

-- Inventarios de bienes
pcs
laptops
perifericos
impresoras
proyectores
escaners
telefonos
bienes_control_administrativo
```

### Tablas NO utilizadas (según requisitos)
- infraestructura
- licencias
- modems
- formato

---

## 🚀 Instalación y Configuración

### Requisitos Previos
- **Java 11** (JDK 11)
- **Apache Maven 3.6+**
- **MySQL 8.x**
- **NetBeans 11+** o **Eclipse** (opcional)

### Pasos de Instalación

#### 1. Crear Base de Datos
```sql
-- Ejecutar el script principal
mysql -u root -p < inventario_dtic_2026_modelo_workbench_compatible.sql

-- Ejecutar script de tablas adicionales
mysql -u root -p inventario_dtic_2026 < src/main/sql/usuarios_historico.sql
```

#### 2. Configurar Conexión a BD
Editar `src/main/resources/META-INF/persistence.xml`:

```xml
<property name="javax.persistence.jdbc.url" 
    value="jdbc:mysql://localhost:3306/inventario_dtic_2026?..." />
<property name="javax.persistence.jdbc.user" value="root" />
<property name="javax.persistence.jdbc.password" value="root" />
```

#### 3. Compilar Proyecto
```bash
mvn clean compile
```

#### 4. Empaquetar Aplicación
```bash
mvn clean package
```

#### 5. Desplegar en Servidor
- Copiar `target/SistemaInventarioV3-1.0-SNAPSHOT.war` a `${CATALINA_HOME}/webapps/`
- Reiniciar Tomcat

#### 6. Acceder a Aplicación
```
http://localhost:8080/SistemaInventarioV3/
```

---

## 👤 Credenciales de Prueba

### Administrador
- **Usuario:** `admin`
- **Contraseña:** `admin123`
- **Permisos:** Crear, editar, eliminar bienes; cambiar estado; ver histórico

### Técnico
- **Usuario:** `tecnico`
- **Contraseña:** `tecnico123`
- **Permisos:** Crear bienes; modificar solo custodio; ver histórico

---

## 📡 Endpoints REST API

### Autenticación
- `POST /resources/login` - Iniciar sesión
- `GET /resources/login/actual` - Obtener usuario actual
- `POST /resources/login/logout` - Cerrar sesión

### Inventario
- `GET /resources/inventario/pcs` - Listar PCs
- `GET /resources/inventario/laptops` - Listar Laptops
- `POST /resources/inventario/pcs` - Crear PC
- `PUT /resources/inventario/pcs/{id}` - Actualizar PC
- `DELETE /resources/inventario/pcs/{id}` - Eliminar PC

### Búsqueda
- `GET /resources/busqueda?tipo=pcs&criterio=marca&valor=Dell`
- `GET /resources/busqueda/megan/{codigo}`
- `GET /resources/busqueda/sbai/{codigo}`

### Histórico
- `GET /resources/historico/bien/{tabla}/{id}`
- `GET /resources/historico/usuario/{usuario}`
- `GET /resources/historico/tabla/{tabla}`

### Reportes
- `GET /resources/reportes/pcs/excel` - Exportar PCs
- `GET /resources/reportes/laptops/excel` - Exportar Laptops
- `GET /resources/reportes/inventario/excel` - Exportar completo
- `GET /resources/reportes/estadisticas` - Estadísticas

---

## 🎨 Guía de Estilos - Interfaz Gráfica

### Colores Institucionales SENADI
- **Azul Oscuro (#002d5b):** Encabezados y botones principales
- **Azul Medio (#0052a3):** Detalles y acentos
- **Blanco:** Fondo principal
- **Gris (#f5f5f5):** Fondo secundario

### Estructura de Navegación
1. **Bienvenida:** Panel inicial con accesos rápidos
2. **Inventario:** Vista tabular de PCs y Laptops
3. **Nuevo Equipo:** Formulario para registrar bienes
4. **Búsqueda:** Búsqueda avanzada por múltiples criterios
5. **Histórico:** Visualización de cambios
6. **Reportes:** Generación y exportación de datos

---

## 📊 Funcionalidades Principales

### 1. Autenticación
- Login con usuario y contraseña
- Validación de roles
- Sesión persistente
- Logout seguro

### 2. Gestión de Inventario
- Crear nuevos equipos
- Editar información de equipos
- Cambiar estado de equipos (solo Admin)
- Eliminar equipos
- Validación de códigos únicos (Megan, SBAI)

### 3. Histórico Automático
- Registra cada cambio realizado
- Incluye: usuario, fecha, campo, valor anterior, valor nuevo
- Motivo de cambio (obligatorio para ciertos cambios)
- Queries por bien, usuario, tabla o campo específico

### 4. Búsqueda Avanzada
- Búsqueda por marca, estado, custodio
- Búsqueda por código Megan
- Búsqueda por código SBAI
- Resultados en tabla editable

### 5. Reportes
- Estadísticas del inventario
- Exportación a Excel
- Filtrado de campos
- Descarga directa

---

## 📝 Validaciones Implementadas

- **Códigos únicos:** Megan y SBAI no pueden repetirse
- **Campos obligatorios:** Tipo, Código Megan, Código SBAI, Estado
- **Control de acceso:** Validación de roles por operación
- **Motivo de cambio:** Requerido para cambios administrativos
- **Integridad referencial:** Validación de custodios y ubicaciones

---

## 🛠️ Tecnologías Utilizadas

| Componente | Versión | Propósito |
|-----------|---------|----------|
| Java | 11 | Lenguaje principal |
| JAX-RS | 2.1 | Framework REST |
| JPA/Hibernate | 5.6 | ORM |
| MySQL | 8.x | Base de datos |
| GSON | 2.10.1 | Serialización JSON |
| Apache POI | 5.2.3 | Exportación Excel |
| Maven | 3.6+ | Build tool |

---

## 🐛 Troubleshooting

### Error de Conexión a BD
- Verificar que MySQL está corriendo
- Confirmar credenciales en `persistence.xml`
- Verificar que la tabla existe y está poblada

### Error 404 en recursos
- Verificar que la WAR está correctamente desplegada
- Confirmar que los paths son correctos en `main.js`

### Sesión expirada
- Limpiar cookies del navegador
- Volver a iniciar sesión

---

## 📚 Referencias Útiles

- Documentación de [JAX-RS](https://projects.eclipse.org/projects/ee4j.jax-rs)
- Documentación de [Hibernate JPA](https://hibernate.org/orm/)
- Documentación de [Apache POI](https://poi.apache.org/)

---

##✅ Estado del Proyecto

- [x] FASE 1: Configuración Java 11 y dependencias
- [x] FASE 2: Entidades JPA y DAOs
- [x] FASE 3: Servicios de negocio
- [x] FASE 4: Controladores REST
- [x] FASE 5: Interfaz gráfica HTML/CSS/JS
- [x] FASE 6: Exportación a Excel y características finales

---

## 👥 Equipo de Desarrollo

- **Proyecto:** Sistema de Inventario SENADI
- **Versión:** 1.0
- **Fecha:** Abril 2026

---

## 📄 Licencia

Este proyecto es de código cerrado para uso exclusivo de SENADI.

