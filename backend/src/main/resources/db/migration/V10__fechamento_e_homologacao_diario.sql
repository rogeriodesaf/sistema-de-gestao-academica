ALTER TABLE ofertas_disciplinas
    ADD COLUMN data_encerramento TIMESTAMP,
    ADD COLUMN encerrado_por_professor_id BIGINT REFERENCES professores(id),
    ADD COLUMN data_homologacao TIMESTAMP,
    ADD COLUMN homologado_por_usuario_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN data_reabertura TIMESTAMP,
    ADD COLUMN reaberto_por_usuario_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN motivo_reabertura VARCHAR(2000);

ALTER TABLE matriculas_disciplinas
    ADD COLUMN data_consolidacao TIMESTAMP;
