INSERT INTO clientes (id, tipo_cliente, nit) VALUES
(1, 'VIP', NULL),
(2, 'FRECUENTE', NULL),
(3, 'MOROSO', NULL),
(4, 'REGULAR', NULL),
(5, 'REGULAR', '900123456-7');

INSERT INTO productos (id, precio) VALUES
(101, 100000.00),
(102, 250000.00),
(103, 50000.00);

INSERT INTO inventario (producto_id, stock) VALUES
(101, 10),
(102, 5),
(103, 20);

INSERT INTO facturas (cliente_id, monto, pagada) VALUES
(3, 150000.00, FALSE),
(3, 50000.00, FALSE),
(4, 0.00, TRUE);