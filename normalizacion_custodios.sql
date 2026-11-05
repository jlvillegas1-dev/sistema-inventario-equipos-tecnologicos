-- ============================================================
-- NORMALIZACIÓN DE CUSTODIOS DUPLICADOS
-- Unifica nombres inconsistentes conservando todos los equipos
-- Ejecutar sobre la base inventario_dtic_2026 ya cargada
-- ============================================================

USE inventario_dtic_2026;
SET FOREIGN_KEY_CHECKS = 0;

-- Patrón por grupo:
-- 1. Redirige equipos del duplicado al canónico
-- 2. Redirige historial_custodio del duplicado al canónico
-- 3. Elimina el registro duplicado

-- --------------------------------------------------------
-- GRUPO 1: ADRIANA CARRILLO → ADRIANA E. CARRILLO ALMEIDA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA E. CARRILLO ALMEIDA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA CARRILLO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA E. CARRILLO ALMEIDA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA CARRILLO') t);
DELETE FROM custodio WHERE nombre = 'ADRIANA CARRILLO';

-- --------------------------------------------------------
-- GRUPO 2: ADRIANA CERVANTES CHACON → ADRIANA LEONOR CERVANTES CHACON
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA LEONOR CERVANTES CHACON' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA CERVANTES CHACON') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA LEONOR CERVANTES CHACON' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ADRIANA CERVANTES CHACON') t);
DELETE FROM custodio WHERE nombre = 'ADRIANA CERVANTES CHACON';

-- --------------------------------------------------------
-- GRUPO 3: ANDRADE SAETEROS LUIS ALFREDO → LUIS A. ANDRADE SAETEROS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'LUIS A. ANDRADE SAETEROS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANDRADE SAETEROS LUIS ALFREDO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'LUIS A. ANDRADE SAETEROS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANDRADE SAETEROS LUIS ALFREDO') t);
DELETE FROM custodio WHERE nombre = 'ANDRADE SAETEROS LUIS ALFREDO';

-- --------------------------------------------------------
-- GRUPO 4: ANGEL F. ONTANEDA JIMENEZ → ANGEL FABIAN ONTANEDA JIMENEZ
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANGEL FABIAN ONTANEDA JIMENEZ' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANGEL F. ONTANEDA JIMENEZ') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANGEL FABIAN ONTANEDA JIMENEZ' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ANGEL F. ONTANEDA JIMENEZ') t);
DELETE FROM custodio WHERE nombre = 'ANGEL F. ONTANEDA JIMENEZ';

-- --------------------------------------------------------
-- GRUPO 5: BYRON TAFUR / TAFUR BUSTOS BYRONE ISAAC → BYRONE I. TAFUR BUSTOS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'BYRONE I. TAFUR BUSTOS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('BYRON TAFUR','TAFUR BUSTOS BYRONE ISAAC')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'BYRONE I. TAFUR BUSTOS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('BYRON TAFUR','TAFUR BUSTOS BYRONE ISAAC')) t);
DELETE FROM custodio WHERE nombre IN ('BYRON TAFUR','TAFUR BUSTOS BYRONE ISAAC');

-- --------------------------------------------------------
-- GRUPO 6: CRIOLLO NAVARRETE CARLOS JAVIER → CARLOS J. CRIOLLO NAVARRETE
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CARLOS J. CRIOLLO NAVARRETE' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CRIOLLO NAVARRETE CARLOS JAVIER') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CARLOS J. CRIOLLO NAVARRETE' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CRIOLLO NAVARRETE CARLOS JAVIER') t);
DELETE FROM custodio WHERE nombre = 'CRIOLLO NAVARRETE CARLOS JAVIER';

-- --------------------------------------------------------
-- GRUPO 7: CEVALLOS JEREZ FREDDY FABIAN → FREDDY F. CEVALLOS JEREZ
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FREDDY F. CEVALLOS JEREZ' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CEVALLOS JEREZ FREDDY FABIAN') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FREDDY F. CEVALLOS JEREZ' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'CEVALLOS JEREZ FREDDY FABIAN') t);
DELETE FROM custodio WHERE nombre = 'CEVALLOS JEREZ FREDDY FABIAN';

-- --------------------------------------------------------
-- GRUPO 8: EMERSON CERACAPA SOLÍS → EMERSON R. CERACAPA SOLIS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'EMERSON R. CERACAPA SOLIS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'EMERSON CERACAPA SOLÍS') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'EMERSON R. CERACAPA SOLIS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'EMERSON CERACAPA SOLÍS') t);
DELETE FROM custodio WHERE nombre = 'EMERSON CERACAPA SOLÍS';

-- --------------------------------------------------------
-- GRUPO 9: FABIAN P. LOPEZ NUÑEZ → FABIAN PATRICIO LOPEZ NUÑEZ
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FABIAN PATRICIO LOPEZ NUÑEZ' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FABIAN P. LOPEZ NUÑEZ') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FABIAN PATRICIO LOPEZ NUÑEZ' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FABIAN P. LOPEZ NUÑEZ') t);
DELETE FROM custodio WHERE nombre = 'FABIAN P. LOPEZ NUÑEZ';

-- --------------------------------------------------------
-- GRUPO 10: FERNANDO B. NOGALES SORNOZA → FERNANDO BAYARDO NOGALES SORNOZA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FERNANDO BAYARDO NOGALES SORNOZA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FERNANDO B. NOGALES SORNOZA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FERNANDO BAYARDO NOGALES SORNOZA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FERNANDO B. NOGALES SORNOZA') t);
DELETE FROM custodio WHERE nombre = 'FERNANDO B. NOGALES SORNOZA';

-- --------------------------------------------------------
-- GRUPO 11: FREDDY P. VALAREZO ROMERO / VALAREZO ROMERO FREDDY PATRICIO → FREDDY PATRICIO VALAREZO ROMERO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FREDDY PATRICIO VALAREZO ROMERO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('FREDDY P. VALAREZO ROMERO','VALAREZO ROMERO FREDDY PATRICIO')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'FREDDY PATRICIO VALAREZO ROMERO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('FREDDY P. VALAREZO ROMERO','VALAREZO ROMERO FREDDY PATRICIO')) t);
DELETE FROM custodio WHERE nombre IN ('FREDDY P. VALAREZO ROMERO','VALAREZO ROMERO FREDDY PATRICIO');

-- --------------------------------------------------------
-- GRUPO 12: HYPATIA E. PIEDRA ILLESCAS → HYPATIA ELIZABETH PIEDRA ILLESCAS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'HYPATIA ELIZABETH PIEDRA ILLESCAS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'HYPATIA E. PIEDRA ILLESCAS') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'HYPATIA ELIZABETH PIEDRA ILLESCAS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'HYPATIA E. PIEDRA ILLESCAS') t);
DELETE FROM custodio WHERE nombre = 'HYPATIA E. PIEDRA ILLESCAS';

-- --------------------------------------------------------
-- GRUPO 13: IRMA SANGUIL VILLACIS → IRMA MONSERRATH SANGUIL VILLACIS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'IRMA MONSERRATH SANGUIL VILLACIS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'IRMA SANGUIL VILLACIS') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'IRMA MONSERRATH SANGUIL VILLACIS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'IRMA SANGUIL VILLACIS') t);
DELETE FROM custodio WHERE nombre = 'IRMA SANGUIL VILLACIS';

-- --------------------------------------------------------
-- GRUPO 14: ISMAEL DAVID RODRIGUEZ FIGEROA (typo) → ISMAEL RODRIGUEZ FIGUEROA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ISMAEL RODRIGUEZ FIGUEROA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ISMAEL DAVID RODRIGUEZ FIGEROA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ISMAEL RODRIGUEZ FIGUEROA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'ISMAEL DAVID RODRIGUEZ FIGEROA') t);
DELETE FROM custodio WHERE nombre = 'ISMAEL DAVID RODRIGUEZ FIGEROA';

-- --------------------------------------------------------
-- GRUPO 15: JESUS JESSENIA GOMEZ DELGADO → JESSENIA GOMEZ DELGADO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JESSENIA GOMEZ DELGADO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JESUS JESSENIA GOMEZ DELGADO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JESSENIA GOMEZ DELGADO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JESUS JESSENIA GOMEZ DELGADO') t);
DELETE FROM custodio WHERE nombre = 'JESUS JESSENIA GOMEZ DELGADO';

-- --------------------------------------------------------
-- GRUPO 16: JHON F. SALAS VALDES / SALAS VALDES JHON FREDDY → JHON FREDDY SALAS VALDES
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JHON FREDDY SALAS VALDES' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('JHON F. SALAS VALDES','SALAS VALDES JHON FREDDY')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JHON FREDDY SALAS VALDES' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('JHON F. SALAS VALDES','SALAS VALDES JHON FREDDY')) t);
DELETE FROM custodio WHERE nombre IN ('JHON F. SALAS VALDES','SALAS VALDES JHON FREDDY');

-- --------------------------------------------------------
-- GRUPO 17: JORGE P. PAEZ ZURITA → JORGE PAUL PAEZ ZURITA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JORGE PAUL PAEZ ZURITA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JORGE P. PAEZ ZURITA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JORGE PAUL PAEZ ZURITA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JORGE P. PAEZ ZURITA') t);
DELETE FROM custodio WHERE nombre = 'JORGE P. PAEZ ZURITA';

-- --------------------------------------------------------
-- GRUPO 18: JOSE L. CAJAMARCA CRIOLLO → JOSE LUIS CAJAMARCA CRIOLLO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JOSE LUIS CAJAMARCA CRIOLLO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JOSE L. CAJAMARCA CRIOLLO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JOSE LUIS CAJAMARCA CRIOLLO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JOSE L. CAJAMARCA CRIOLLO') t);
DELETE FROM custodio WHERE nombre = 'JOSE L. CAJAMARCA CRIOLLO';

-- --------------------------------------------------------
-- GRUPO 19: JULIO A. ROBALINO JACOME / ROBALINO JACOME JULIO ALBERTO → JULIO ALBERTO ROBALINO JÁCOME
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JULIO ALBERTO ROBALINO JÁCOME' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('JULIO A. ROBALINO JACOME','ROBALINO JACOME JULIO ALBERTO')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'JULIO ALBERTO ROBALINO JÁCOME' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('JULIO A. ROBALINO JACOME','ROBALINO JACOME JULIO ALBERTO')) t);
DELETE FROM custodio WHERE nombre IN ('JULIO A. ROBALINO JACOME','ROBALINO JACOME JULIO ALBERTO');

-- --------------------------------------------------------
-- GRUPO 20: KHATERINE ALEGRIA RENGIFO CEVALLOS / RENGIFO CEVALLOS KHATERYNE ALEGRIA → KATHERINE A. RENGIFO CEVALLOS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'KATHERINE A. RENGIFO CEVALLOS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('KHATERINE ALEGRIA  RENGIFO CEVALLOS','RENGIFO CEVALLOS KHATERYNE ALEGRIA')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'KATHERINE A. RENGIFO CEVALLOS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('KHATERINE ALEGRIA  RENGIFO CEVALLOS','RENGIFO CEVALLOS KHATERYNE ALEGRIA')) t);
DELETE FROM custodio WHERE nombre IN ('KHATERINE ALEGRIA  RENGIFO CEVALLOS','RENGIFO CEVALLOS KHATERYNE ALEGRIA');

-- --------------------------------------------------------
-- GRUPO 21: MANOSALVAS ARMIJOS GABRIELA ALEXANDRA → GABRIELA A. MANOSALVAS ARMIJOS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'GABRIELA A. MANOSALVAS ARMIJOS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MANOSALVAS ARMIJOS GABRIELA ALEXANDRA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'GABRIELA A. MANOSALVAS ARMIJOS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MANOSALVAS ARMIJOS GABRIELA ALEXANDRA') t);
DELETE FROM custodio WHERE nombre = 'MANOSALVAS ARMIJOS GABRIELA ALEXANDRA';

-- --------------------------------------------------------
-- GRUPO 22: MARIA B. CHAVEZ ARBOLEDA → MARIA BELEN CHAVEZ ARBOLEDA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARIA BELEN CHAVEZ ARBOLEDA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARIA B. CHAVEZ ARBOLEDA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARIA BELEN CHAVEZ ARBOLEDA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARIA B. CHAVEZ ARBOLEDA') t);
DELETE FROM custodio WHERE nombre = 'MARIA B. CHAVEZ ARBOLEDA';

-- --------------------------------------------------------
-- GRUPO 23: MARITZA L. ALVAREZ CORNEJO → MARITZA LORENA ALVAREZ CORNEJO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARITZA LORENA ALVAREZ CORNEJO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARITZA L. ALVAREZ CORNEJO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARITZA LORENA ALVAREZ CORNEJO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MARITZA L. ALVAREZ CORNEJO') t);
DELETE FROM custodio WHERE nombre = 'MARITZA L. ALVAREZ CORNEJO';

-- --------------------------------------------------------
-- GRUPO 24: MELISSA FRÍAS RUIZ → MELISSA FERNANDA FRIAS RUIZ
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MELISSA FERNANDA FRIAS RUIZ' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MELISSA FRÍAS RUIZ') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MELISSA FERNANDA FRIAS RUIZ' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MELISSA FRÍAS RUIZ') t);
DELETE FROM custodio WHERE nombre = 'MELISSA FRÍAS RUIZ';

-- --------------------------------------------------------
-- GRUPO 25: MÓNICA VALVINA VÁSQUEZ LEMA → MONICA V. VASQUEZ LEMA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MONICA V. VASQUEZ LEMA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MÓNICA VALVINA VÁSQUEZ LEMA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MONICA V. VASQUEZ LEMA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MÓNICA VALVINA VÁSQUEZ LEMA') t);
DELETE FROM custodio WHERE nombre = 'MÓNICA VALVINA VÁSQUEZ LEMA';

-- --------------------------------------------------------
-- GRUPO 26: PAUL OROZCO VINUEZA → PAUL FERNANDO OROZCO VINUEZA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAUL FERNANDO OROZCO VINUEZA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAUL OROZCO VINUEZA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAUL FERNANDO OROZCO VINUEZA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAUL OROZCO VINUEZA') t);
DELETE FROM custodio WHERE nombre = 'PAUL OROZCO VINUEZA';

-- --------------------------------------------------------
-- GRUPO 27: MOSQUERA HIDALGO PAULINA DEL CONSUELO → PAULINA C. MOSQUERA HIDALGO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAULINA C. MOSQUERA HIDALGO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MOSQUERA HIDALGO PAULINA DEL CONSUELO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PAULINA C. MOSQUERA HIDALGO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'MOSQUERA HIDALGO PAULINA DEL CONSUELO') t);
DELETE FROM custodio WHERE nombre = 'MOSQUERA HIDALGO PAULINA DEL CONSUELO';

-- --------------------------------------------------------
-- GRUPO 28: PEDRO A. BAQUERO GUEVARA → PEDRO ANIBAL BAQUERO GUEVARA
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PEDRO ANIBAL BAQUERO GUEVARA' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PEDRO A. BAQUERO GUEVARA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PEDRO ANIBAL BAQUERO GUEVARA' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PEDRO A. BAQUERO GUEVARA') t);
DELETE FROM custodio WHERE nombre = 'PEDRO A. BAQUERO GUEVARA';

-- --------------------------------------------------------
-- GRUPO 29: SALGADO ARIAS BLANCA MARGARITA → BLANCA M. SALGADO ARIAS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'BLANCA M. SALGADO ARIAS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'SALGADO ARIAS BLANCA MARGARITA') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'BLANCA M. SALGADO ARIAS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'SALGADO ARIAS BLANCA MARGARITA') t);
DELETE FROM custodio WHERE nombre = 'SALGADO ARIAS BLANCA MARGARITA';

-- --------------------------------------------------------
-- GRUPO 30: TERESA N. ESPINOZA ZALDUMBIDE → TERESA NATALIA ESPINOZA ZALDUMBIDE
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'TERESA NATALIA ESPINOZA ZALDUMBIDE' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'TERESA N. ESPINOZA ZALDUMBIDE') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'TERESA NATALIA ESPINOZA ZALDUMBIDE' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'TERESA N. ESPINOZA ZALDUMBIDE') t);
DELETE FROM custodio WHERE nombre = 'TERESA N. ESPINOZA ZALDUMBIDE';

-- --------------------------------------------------------
-- GRUPO 31: WALTER F. DARQUEA CHUGCHO → WALTER FABIAN DARQUEA CHUGCHO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'WALTER FABIAN DARQUEA CHUGCHO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'WALTER F. DARQUEA CHUGCHO') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'WALTER FABIAN DARQUEA CHUGCHO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'WALTER F. DARQUEA CHUGCHO') t);
DELETE FROM custodio WHERE nombre = 'WALTER F. DARQUEA CHUGCHO';

-- --------------------------------------------------------
-- GRUPO 32: Karina Vazquez (minúsculas) → KARINA VAZQUEZ
-- --------------------------------------------------------
UPDATE custodio SET nombre = 'KARINA VAZQUEZ' WHERE nombre = 'Karina Vazquez';

-- --------------------------------------------------------
-- GRUPO 33: LOAIZA MOREIRA LUCIA CRISTINA (orden invertido) → LUCIA CRISTINA LOAIZA MOREIRA
-- --------------------------------------------------------
UPDATE custodio SET nombre = 'LUCIA CRISTINA LOAIZA MOREIRA' WHERE nombre = 'LOAIZA MOREIRA LUCIA CRISTINA';

-- --------------------------------------------------------
-- GRUPO 34: PADILLA RIVAS KATYA DOMINIQUE → KATYA DOMINIQUE PADILLA RIVAS
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'KATYA DOMINIQUE PADILLA RIVAS' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PADILLA RIVAS KATYA DOMINIQUE') t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'KATYA DOMINIQUE PADILLA RIVAS' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'PADILLA RIVAS KATYA DOMINIQUE') t);
DELETE FROM custodio WHERE nombre = 'PADILLA RIVAS KATYA DOMINIQUE';

-- --------------------------------------------------------
-- GRUPO 35: '.' y 'S/N' → SIN CUSTODIO
-- --------------------------------------------------------
UPDATE equipo SET id_custodio_actual =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'SIN CUSTODIO' LIMIT 1) t)
WHERE id_custodio_actual IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('.','S/N')) t);
UPDATE historial_custodio SET id_custodio =
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre = 'SIN CUSTODIO' LIMIT 1) t)
WHERE id_custodio IN
    (SELECT id_custodio FROM (SELECT id_custodio FROM custodio WHERE nombre IN ('.','S/N')) t);
DELETE FROM custodio WHERE nombre IN ('.','S/N');

SET FOREIGN_KEY_CHECKS = 1;

-- Verificación final: debe mostrar 0 si todo se ejecutó correctamente
SELECT COUNT(*) AS duplicados_restantes FROM custodio
WHERE nombre IN (
    'ADRIANA CARRILLO','ADRIANA CERVANTES CHACON','ANDRADE SAETEROS LUIS ALFREDO',
    'ANGEL F. ONTANEDA JIMENEZ','BYRON TAFUR','TAFUR BUSTOS BYRONE ISAAC',
    'CRIOLLO NAVARRETE CARLOS JAVIER','CEVALLOS JEREZ FREDDY FABIAN',
    'EMERSON CERACAPA SOLÍS','FABIAN P. LOPEZ NUÑEZ','FERNANDO B. NOGALES SORNOZA',
    'FREDDY P. VALAREZO ROMERO','VALAREZO ROMERO FREDDY PATRICIO',
    'HYPATIA E. PIEDRA ILLESCAS','IRMA SANGUIL VILLACIS',
    'ISMAEL DAVID RODRIGUEZ FIGEROA','JESUS JESSENIA GOMEZ DELGADO',
    'JHON F. SALAS VALDES','SALAS VALDES JHON FREDDY','JORGE P. PAEZ ZURITA',
    'JOSE L. CAJAMARCA CRIOLLO','JULIO A. ROBALINO JACOME','ROBALINO JACOME JULIO ALBERTO',
    'KHATERINE ALEGRIA  RENGIFO CEVALLOS','RENGIFO CEVALLOS KHATERYNE ALEGRIA',
    'MANOSALVAS ARMIJOS GABRIELA ALEXANDRA','MARIA B. CHAVEZ ARBOLEDA',
    'MARITZA L. ALVAREZ CORNEJO','MELISSA FRÍAS RUIZ','MÓNICA VALVINA VÁSQUEZ LEMA',
    'PAUL OROZCO VINUEZA','MOSQUERA HIDALGO PAULINA DEL CONSUELO',
    'PEDRO A. BAQUERO GUEVARA','SALGADO ARIAS BLANCA MARGARITA',
    'TERESA N. ESPINOZA ZALDUMBIDE','WALTER F. DARQUEA CHUGCHO',
    'Karina Vazquez','LOAIZA MOREIRA LUCIA CRISTINA','PADILLA RIVAS KATYA DOMINIQUE',
    '.','S/N'
);
