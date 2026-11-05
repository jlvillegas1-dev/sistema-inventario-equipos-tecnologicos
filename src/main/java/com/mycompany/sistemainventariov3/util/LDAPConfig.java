package com.mycompany.sistemainventariov3.util;

/**
 * Configuracion centralizada para LDAP/Active Directory.
 *
 * Los valores se pueden sobreescribir con variables de entorno o propiedades
 * del sistema (-DLDAP_SERVER=...).
 */
public final class LDAPConfig {

    private static final String LDAP_SERVER = getProperty("LDAP_SERVER", "192.168.1.1");
    private static final int LDAP_PORT = getIntProperty("LDAP_PORT", 389);
    private static final String LDAP_DOMAIN = normalizeDomain(getProperty("LDAP_DOMAIN", "iepi.gov.ec"));

    private static final String LDAP_USER_ADMIN = getProperty("LDAP_USER_ADMIN", "");
    private static final String LDAP_PASS_ADMIN = getProperty("LDAP_PASS_ADMIN", "");

    private static final String SEARCH_BASE = getProperty("LDAP_SEARCH_BASE", "DC=iepi,DC=gov,DC=ec");

    private static final String GROUP_ADMIN = getProperty("LDAP_GROUP_ADMIN", "SC_Inv_Admin");
    private static final String GROUP_TECNICO = getProperty("LDAP_GROUP_TECNICO", "SC_Inv_Tecnico");
    private static final String GROUP_CUSTODIO = getProperty("LDAP_GROUP_CUSTODIO", "SC_Inv_Custodio");

    private static final int TIMEOUT_CONEXION = getIntProperty("LDAP_TIMEOUT", 5000);

    private static final String ATTR_SAM = "sAMAccountName";
    private static final String ATTR_DISPLAY_NAME = "displayName";
    private static final String ATTR_MAIL = "mail";
    private static final String ATTR_PHONE = "telephoneNumber";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_DEPARTMENT = "department";
    private static final String ATTR_DN = "distinguishedName";
    private static final String ATTR_MEMBER_OF = "memberOf";

    private LDAPConfig() {
    }

    private static String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        return defaultValue;
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("LDAP config " + key + " invalido: " + value + ". Usando " + defaultValue);
            return defaultValue;
        }
    }

    private static String normalizeDomain(String value) {
        if (value == null) {
            return "";
        }
        String domain = value.trim();
        while (domain.startsWith("@")) {
            domain = domain.substring(1);
        }
        return domain;
    }

    public static String buildPrincipal(String user) {
        String cleanUser = user == null ? "" : user.trim();
        if (cleanUser.contains("@") || cleanUser.contains("\\")) {
            return cleanUser;
        }
        return cleanUser + "@" + LDAP_DOMAIN;
    }

    public static String getLdapServer() {
        return LDAP_SERVER;
    }

    public static int getLdapPort() {
        return LDAP_PORT;
    }

    public static String getLdapDomain() {
        return LDAP_DOMAIN;
    }

    public static String getLdapUserAdmin() {
        return LDAP_USER_ADMIN;
    }

    public static String getLdapPassAdmin() {
        return LDAP_PASS_ADMIN;
    }

    public static String getSearchBase() {
        return SEARCH_BASE;
    }

    public static String getGroupAdmin() {
        return GROUP_ADMIN;
    }

    public static String getGroupTecnico() {
        return GROUP_TECNICO;
    }

    public static String getGroupCustodio() {
        return GROUP_CUSTODIO;
    }

    public static int getTimeoutConexion() {
        return TIMEOUT_CONEXION;
    }

    public static String getAttrSam() {
        return ATTR_SAM;
    }

    public static String getAttrDisplayName() {
        return ATTR_DISPLAY_NAME;
    }

    public static String getAttrMail() {
        return ATTR_MAIL;
    }

    public static String getAttrPhone() {
        return ATTR_PHONE;
    }

    public static String getAttrTitle() {
        return ATTR_TITLE;
    }

    public static String getAttrDepartment() {
        return ATTR_DEPARTMENT;
    }

    public static String getAttrDn() {
        return ATTR_DN;
    }

    public static String getAttrMemberOf() {
        return ATTR_MEMBER_OF;
    }

    public static void printConfig() {
        System.out.println("==== CONFIGURACION LDAP ====");
        System.out.println("Servidor: " + LDAP_SERVER + ":" + LDAP_PORT);
        System.out.println("Dominio bind: @" + LDAP_DOMAIN);
        System.out.println("Base busqueda: " + SEARCH_BASE);
        System.out.println("Grupo admin: " + GROUP_ADMIN);
        System.out.println("Grupo tecnico: " + GROUP_TECNICO);
        System.out.println("Grupo custodio: " + GROUP_CUSTODIO);
        System.out.println("============================");
    }
}
