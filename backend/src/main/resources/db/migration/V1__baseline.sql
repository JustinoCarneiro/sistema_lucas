-- V1__baseline.sql
-- Este script serve como baseline inicial para o Flyway.
-- A aplicação estava utilizando o `spring.jpa.hibernate.ddl-auto=update` e já pode possuir tabelas no banco de dados local.
-- O Flyway irá adotar esse banco de dados atual como a Versão 1, ignorando a criação manual.

-- NOTA: Para um deploy em banco de dados totalmente zerado, o ideal seria que este arquivo
-- contivesse todo o DDL real. Caso a aplicação precise rodar do zero em um pipeline de CI/CD,
-- você pode popular esse arquivo com o dump (schema) do banco atual gerado pelas entidades JPA.
