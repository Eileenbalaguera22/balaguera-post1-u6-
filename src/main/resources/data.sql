INSERT INTO clientes (id, tipo_cliente) VALUES
(1, 'VIP'),
(2, 'FRECUENTE'),
(3, 'MOROSO'),
(4, 'REGULAR');

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