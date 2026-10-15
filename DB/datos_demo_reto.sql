USE retocacao_db;

INSERT INTO lote (codigo, productor, origen, fecha, cantidad, unidad, estado_lote)
VALUES ('CAC001', 'Productor A', 'Rivera', CURDATE(), 100.00, 'kg', 'PENDIENTE'), ('CAC002', 'Productor B', 'Campoalegre', CURDATE(), 150.00, 'kg', 'PENDIENTE'),
('CAC003', 'Productor C', 'Algeciras', CURDATE(), 80.00, 'kg', 'PENDIENTE');

SELECT * FROM lote;