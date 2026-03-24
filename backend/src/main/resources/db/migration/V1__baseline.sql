-- V1__baseline.sql
-- Schema inicial para Projeto Lucas (Corrigido para nomes singulares conforme Hibernate default)

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS patient (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    cpf VARCHAR(20),
    phone VARCHAR(20),
    birth_date DATE,
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(20),
    gender VARCHAR(50),
    allergies TEXT,
    address VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS professional (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    tipo_registro VARCHAR(50),
    registro_conselho VARCHAR(50),
    specialty VARCHAR(255),
    cpf VARCHAR(20),
    phone VARCHAR(20),
    birth_date DATE,
    gender VARCHAR(50),
    address VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS appointments (
    id SERIAL PRIMARY KEY,
    professional_id BIGINT REFERENCES professional(id),
    patient_id BIGINT REFERENCES patient(id),
    date_time TIMESTAMP,
    reason TEXT,
    status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS prontuarios (
    id SERIAL PRIMARY KEY,
    appointment_id BIGINT REFERENCES appointments(id),
    patient_id BIGINT REFERENCES patient(id),
    professional_id BIGINT REFERENCES professional(id),
    notas TEXT,
    criado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documentos (
    id SERIAL PRIMARY KEY,
    tipo VARCHAR(50),
    titulo VARCHAR(255),
    conteudo_texto TEXT,
    nome_arquivo VARCHAR(255),
    arquivo_base64 TEXT,
    paciente_id BIGINT REFERENCES patient(id),
    profissional_id BIGINT REFERENCES professional(id),
    criado_em TIMESTAMP,
    disponivel BOOLEAN DEFAULT FALSE
);
