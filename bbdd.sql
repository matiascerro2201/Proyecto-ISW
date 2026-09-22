-- ============================================================
-- Esquema MySQL: Sistema de gestion de practicas pedagogicas
-- Version final del MER (incorpora todas las revisiones:
-- especializacion de roles, seccion_asignatura, dimension/criterio
-- normalizados, pauta_documento, detalle por criterio, etc.)
-- ============================================================

CREATE DATABASE IF NOT EXISTS practicas_pedagogicas
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE practicas_pedagogicas;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. USUARIOS Y ROLES (especializacion 1:1)
-- ============================================================

CREATE TABLE usuario (
    id_usuario     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    rol            ENUM('ESTUDIANTE','COLABORADOR','TUTOR','PROFESOR_ASIGNATURA','COORDINADOR') NOT NULL,
    creado_en      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE centro_practica (
    id_centro   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    comuna      VARCHAR(100),
    tipo        ENUM('MUNICIPAL','SUBVENCIONADO','PARTICULAR','EDUCACION_ADULTOS') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE estudiante (
    id_usuario   BIGINT PRIMARY KEY,
    carrera      VARCHAR(150) NOT NULL,
    CONSTRAINT fk_estudiante_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE profesor_colaborador (
    id_usuario   BIGINT PRIMARY KEY,
    id_centro    BIGINT NOT NULL,
    CONSTRAINT fk_colaborador_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_colaborador_centro
        FOREIGN KEY (id_centro) REFERENCES centro_practica(id_centro)
) ENGINE=InnoDB;

CREATE TABLE profesor_tutor (
    id_usuario   BIGINT PRIMARY KEY,
    CONSTRAINT fk_tutor_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE profesor_asignatura (
    id_usuario   BIGINT PRIMARY KEY,
    CONSTRAINT fk_profasig_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 2. ASIGNATURAS Y SECCIONES
-- ============================================================

CREATE TABLE asignatura (
    id_asignatura     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(150) NOT NULL,
    ciclo             ENUM('INICIAL','INTERMEDIO','FINAL') NOT NULL,
    numero_lineas     TINYINT NOT NULL DEFAULT 1   -- 0 = sin practica real, 1 = unica, 2 = especialidad+orientacion
) ENGINE=InnoDB;

-- Una asignatura, dictada por un profesor de asignatura, en un anio/semestre concreto.
-- Existe para no repetir el profesor en cada inscripcion (todas las inscripciones
-- de la misma seccion comparten el mismo profesor de asignatura).
CREATE TABLE seccion_asignatura (
    id_seccion      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_asignatura   BIGINT NOT NULL,
    id_profesor     BIGINT NOT NULL,
    anio            SMALLINT NOT NULL,
    semestre        TINYINT NOT NULL,
    CONSTRAINT fk_seccion_asignatura
        FOREIGN KEY (id_asignatura) REFERENCES asignatura(id_asignatura),
    CONSTRAINT fk_seccion_profesor
        FOREIGN KEY (id_profesor) REFERENCES profesor_asignatura(id_usuario),
    UNIQUE KEY uq_seccion (id_asignatura, anio, semestre)
) ENGINE=InnoDB;

-- ============================================================
-- 3. INSCRIPCION Y LINEAS DE PRACTICA
-- ============================================================

CREATE TABLE inscripcion_asignatura (
    id_inscripcion  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante   BIGINT NOT NULL,
    id_seccion      BIGINT NOT NULL,
    id_centro       BIGINT NOT NULL,
    estado          ENUM('EN_CURSO','APROBADO','REPROBADO','RETIRADO') NOT NULL DEFAULT 'EN_CURSO',
    CONSTRAINT fk_inscripcion_estudiante
        FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_usuario),
    CONSTRAINT fk_inscripcion_seccion
        FOREIGN KEY (id_seccion) REFERENCES seccion_asignatura(id_seccion),
    CONSTRAINT fk_inscripcion_centro
        FOREIGN KEY (id_centro) REFERENCES centro_practica(id_centro),
    UNIQUE KEY uq_inscripcion (id_estudiante, id_seccion)
) ENGINE=InnoDB;

-- 1 o 2 lineas por inscripcion (unica / especialidad / orientacion).
-- id_tutor es nulo salvo en Practica Profesional.
CREATE TABLE linea_practica (
    id_linea         BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion   BIGINT NOT NULL,
    id_colaborador   BIGINT NOT NULL,
    id_tutor         BIGINT NULL,
    tipo             ENUM('UNICA','ESPECIALIDAD','ORIENTACION') NOT NULL,
    CONSTRAINT fk_linea_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion_asignatura(id_inscripcion) ON DELETE CASCADE,
    CONSTRAINT fk_linea_colaborador
        FOREIGN KEY (id_colaborador) REFERENCES profesor_colaborador(id_usuario),
    CONSTRAINT fk_linea_tutor
        FOREIGN KEY (id_tutor) REFERENCES profesor_tutor(id_usuario),
    UNIQUE KEY uq_linea (id_inscripcion, tipo)
) ENGINE=InnoDB;

-- ============================================================
-- 4. PAUTAS DE EVALUACION DE DESEMPEÑO (linea) Y SU DETALLE
-- ============================================================

-- Plantilla: define QUE se evalua (no a quien). Varia por asignatura/linea/aplicacion.
CREATE TABLE pauta_evaluacion (
    id_pauta_evaluacion  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_asignatura        BIGINT NOT NULL,
    tipo_linea           ENUM('UNICA','ESPECIALIDAD','ORIENTACION') NOT NULL,
    numero_aplicacion     TINYINT NOT NULL DEFAULT 1,   -- 1ra, 2da, etc. (cada una con criterios propios)
    ponderacion          DECIMAL(5,2) NOT NULL,         -- % sobre la nota final de la asignatura
    CONSTRAINT fk_pauta_eval_asignatura
        FOREIGN KEY (id_asignatura) REFERENCES asignatura(id_asignatura)
) ENGINE=InnoDB;

CREATE TABLE dimension (
    id_dimension         BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pauta_evaluacion  BIGINT NOT NULL,
    nombre               VARCHAR(200) NOT NULL,   -- ej. "Conocimiento y preparacion de la enseñanza"
    orden                TINYINT NOT NULL,
    CONSTRAINT fk_dimension_pauta
        FOREIGN KEY (id_pauta_evaluacion) REFERENCES pauta_evaluacion(id_pauta_evaluacion) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE criterio (
    id_criterio    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_dimension   BIGINT NOT NULL,
    descripcion    VARCHAR(500) NOT NULL,
    puntaje_max    DECIMAL(5,2) NOT NULL,
    orden          TINYINT NOT NULL,
    CONSTRAINT fk_criterio_dimension
        FOREIGN KEY (id_dimension) REFERENCES dimension(id_dimension) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Instancia real: colaborador o tutor completa una pauta sobre una linea.
CREATE TABLE evaluacion_aplicada (
    id_evaluacion        BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_linea             BIGINT NOT NULL,
    id_pauta_evaluacion  BIGINT NOT NULL,
    tipo_evaluador       ENUM('COLABORADOR','TUTOR') NOT NULL,
    fecha                DATETIME NOT NULL,
    puntaje_total        DECIMAL(6,2),   -- cache = SUM(detalle_criterio.puntaje_obtenido); recalcular al guardar detalle
    CONSTRAINT fk_eval_linea
        FOREIGN KEY (id_linea) REFERENCES linea_practica(id_linea) ON DELETE CASCADE,
    CONSTRAINT fk_eval_pauta
        FOREIGN KEY (id_pauta_evaluacion) REFERENCES pauta_evaluacion(id_pauta_evaluacion)
) ENGINE=InnoDB;

-- Detalle por criterio: puntaje + transcripcion de la nota de voz (nunca el audio).
CREATE TABLE detalle_criterio (
    id_detalle               BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_evaluacion            BIGINT NOT NULL,
    id_criterio              BIGINT NOT NULL,
    puntaje_obtenido         DECIMAL(5,2) NOT NULL,
    transcripcion_comentario TEXT,
    CONSTRAINT fk_detalle_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion_aplicada(id_evaluacion) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_criterio
        FOREIGN KEY (id_criterio) REFERENCES criterio(id_criterio),
    UNIQUE KEY uq_detalle (id_evaluacion, id_criterio)
) ENGINE=InnoDB;

-- ============================================================
-- 5. DOCUMENTOS DEL ESTUDIANTE Y SU PAUTA
-- ============================================================

-- Plantilla de evaluacion para un documento (Informe/Producto/Proyecto de Intervencion).
CREATE TABLE pauta_documento (
    id_pauta_documento  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_asignatura       BIGINT NOT NULL,
    ponderacion         DECIMAL(5,2) NOT NULL,
    tipo                ENUM('INFORME','PRODUCTO','PROYECTO_INTERVENCION') NOT NULL,
    numero              TINYINT NOT NULL,
    CONSTRAINT fk_pauta_doc_asignatura
        FOREIGN KEY (id_asignatura) REFERENCES asignatura(id_asignatura),
    UNIQUE KEY uq_pauta_documento (id_asignatura, tipo, numero)
) ENGINE=InnoDB;

-- El documento subido por el estudiante, evaluado directamente aqui
-- (relacion 1:1 evento-evaluacion, a diferencia de linea_practica que admite N evaluaciones).
CREATE TABLE documento_estudiante (
    id_documento          BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion        BIGINT NOT NULL,
    id_pauta_documento    BIGINT NOT NULL,
    contenido_url         VARCHAR(500) NOT NULL,
    fecha_subida          DATETIME NOT NULL,
    puntaje_obtenido      DECIMAL(6,2),
    comentarios_evaluador TEXT,
    CONSTRAINT fk_documento_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion_asignatura(id_inscripcion) ON DELETE CASCADE,
    CONSTRAINT fk_documento_pauta
        FOREIGN KEY (id_pauta_documento) REFERENCES pauta_documento(id_pauta_documento)
) ENGINE=InnoDB;

-- ============================================================
-- 6. INFORME FINAL (1:1 con la inscripcion)
-- ============================================================

CREATE TABLE informe_final (
    id_informe             BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_inscripcion         BIGINT NOT NULL UNIQUE,
    id_corrector           BIGINT NOT NULL,   -- profesor_asignatura que efectivamente valido este informe
    calificacion_sugerida  DECIMAL(3,1),
    calificacion_final     DECIMAL(3,1),
    fecha_generacion       DATETIME NULL,
    fecha_validacion       DATETIME NULL,
    fecha_publicacion      DATETIME NULL,
    CONSTRAINT fk_informe_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion_asignatura(id_inscripcion) ON DELETE CASCADE,
    CONSTRAINT fk_informe_corrector
        FOREIGN KEY (id_corrector) REFERENCES profesor_asignatura(id_usuario)
) ENGINE=InnoDB;

-- ============================================================
-- 7. PLANIFICACION DE CLASES Y CHATBOT
-- ============================================================

CREATE TABLE planificacion (
    id_planificacion   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_linea           BIGINT NOT NULL,
    contenido          TEXT NOT NULL,
    estado             ENUM('ENVIADA','APROBADA','RECHAZADA') NOT NULL DEFAULT 'ENVIADA',
    retroalimentacion  TEXT,
    fecha_envio        DATETIME NOT NULL,
    fecha_retro        DATETIME NULL,
    CONSTRAINT fk_planificacion_linea
        FOREIGN KEY (id_linea) REFERENCES linea_practica(id_linea) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE interaccion_chatbot (
    id_interaccion      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_planificacion    BIGINT NOT NULL,
    mensaje_estudiante  TEXT NOT NULL,
    respuesta_bot       TEXT NOT NULL,
    fecha               DATETIME NOT NULL,
    CONSTRAINT fk_interaccion_planificacion
        FOREIGN KEY (id_planificacion) REFERENCES planificacion(id_planificacion) ON DELETE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
