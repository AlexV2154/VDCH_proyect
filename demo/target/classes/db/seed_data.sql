INSERT INTO usuarios (nombre, usuario, clave, rol, estado)
VALUES
    ('Administrador', 'admin', 'admin123', 'ADMIN', TRUE)
ON CONFLICT (usuario) DO UPDATE
SET nombre = EXCLUDED.nombre,
    clave = EXCLUDED.clave,
    rol = EXCLUDED.rol,
    estado = EXCLUDED.estado;

INSERT INTO categorias (nombre, descripcion, estado)
VALUES
    ('Abarrotes', 'Productos de primera necesidad', TRUE),
    ('Bebidas', 'Gaseosas, aguas y jugos', TRUE),
    ('Frutas', 'Productos vendidos por peso', TRUE),
    ('Limpieza', 'Productos para limpieza del hogar', TRUE)
ON CONFLICT (nombre) DO UPDATE
SET descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado;

INSERT INTO productos (
    id_categoria,
    nombre,
    descripcion,
    tipo_venta,
    unidad_base,
    equivalencia,
    precio_compra,
    precio_venta,
    stock_actual,
    stock_minimo,
    estado
)
SELECT
    c.id_categoria,
    datos.nombre,
    datos.descripcion,
    datos.tipo_venta,
    datos.unidad_base,
    datos.equivalencia,
    datos.precio_compra,
    datos.precio_venta,
    datos.stock_actual,
    datos.stock_minimo,
    TRUE
FROM (
    VALUES
        ('Abarrotes', 'Arroz Costeno', 'Venta por kilo o gramos', 'GRANEL', 'KG', 1::NUMERIC, 3.50::NUMERIC, 4.20::NUMERIC, 25::NUMERIC, 5::NUMERIC),
        ('Bebidas', 'Coca Cola 500 ml', 'Botella personal', 'UNIDAD', 'UNIDAD', 1::NUMERIC, 2.40::NUMERIC, 3.00::NUMERIC, 24::NUMERIC, 6::NUMERIC),
        ('Frutas', 'Maracuya', 'Venta por kilo o gramos', 'GRANEL', 'KG', 1::NUMERIC, 4.50::NUMERIC, 7.50::NUMERIC, 10::NUMERIC, 2::NUMERIC),
        ('Abarrotes', 'Aceite Primor', 'Botella de aceite', 'UNIDAD', 'UNIDAD', 1::NUMERIC, 6.50::NUMERIC, 8.00::NUMERIC, 12::NUMERIC, 3::NUMERIC)
) AS datos(categoria, nombre, descripcion, tipo_venta, unidad_base, equivalencia, precio_compra, precio_venta, stock_actual, stock_minimo)
JOIN categorias c ON c.nombre = datos.categoria
WHERE NOT EXISTS (
    SELECT 1
    FROM productos p
    WHERE p.nombre = datos.nombre
);

INSERT INTO clientes (nombres, telefono, direccion, notas, estado)
SELECT
    datos.nombres,
    datos.telefono,
    datos.direccion,
    datos.notas,
    TRUE
FROM (
    VALUES
        ('Mario Holgado', '999999999', 'Sin direccion', 'Cliente frecuente'),
        ('Juan Perez', '987654321', 'Sin direccion', 'Compra al fiado'),
        ('Maria Valeria', '912456789', 'Sin direccion', 'Cliente sin deuda')
) AS datos(nombres, telefono, direccion, notas)
WHERE NOT EXISTS (
    SELECT 1
    FROM clientes c
    WHERE c.nombres = datos.nombres
);

WITH venta_contado AS (
    INSERT INTO ventas (
        id_cliente,
        id_usuario,
        canal_venta,
        tipo_pago,
        total,
        monto_pagado,
        saldo_pendiente,
        estado,
        fecha_venta
    )
    SELECT
        c.id_cliente,
        u.id_usuario,
        'TIENDA',
        'CONTADO',
        6.00,
        6.00,
        0,
        'PAGADA',
        CURRENT_TIMESTAMP
    FROM clientes c
    CROSS JOIN usuarios u
    WHERE c.nombres = 'Maria Valeria'
      AND u.usuario = 'admin'
      AND NOT EXISTS (
          SELECT 1
          FROM ventas v
          WHERE v.id_cliente = c.id_cliente
            AND v.total = 6.00
            AND v.fecha_venta::date = CURRENT_DATE
      )
    RETURNING id_venta
)
INSERT INTO detalle_ventas (id_venta, id_producto, cantidad, unidad_venta, cantidad_texto, precio_unitario, subtotal)
SELECT
    vc.id_venta,
    p.id_producto,
    2,
    'UNIDAD',
    '2 unidades',
    3.00,
    6.00
FROM venta_contado vc
JOIN productos p ON p.nombre = 'Coca Cola 500 ml';

WITH venta_fiado AS (
    INSERT INTO ventas (
        id_cliente,
        id_usuario,
        canal_venta,
        tipo_pago,
        total,
        monto_pagado,
        saldo_pendiente,
        estado,
        fecha_venta
    )
    SELECT
        c.id_cliente,
        u.id_usuario,
        'TIENDA',
        'FIADO',
        9.00,
        0,
        9.00,
        'PENDIENTE',
        CURRENT_TIMESTAMP
    FROM clientes c
    CROSS JOIN usuarios u
    WHERE c.nombres = 'Mario Holgado'
      AND u.usuario = 'admin'
      AND NOT EXISTS (
          SELECT 1
          FROM creditos cr
          JOIN clientes cx ON cx.id_cliente = cr.id_cliente
          WHERE cx.nombres = 'Mario Holgado'
            AND cr.estado = 'PENDIENTE'
      )
    RETURNING id_venta, id_cliente
),
detalle AS (
    INSERT INTO detalle_ventas (id_venta, id_producto, cantidad, unidad_venta, cantidad_texto, precio_unitario, subtotal)
    SELECT
        vf.id_venta,
        p.id_producto,
        1.2,
        'KG',
        '1.2 kg',
        7.50,
        9.00
    FROM venta_fiado vf
    JOIN productos p ON p.nombre = 'Maracuya'
    RETURNING id_venta
)
INSERT INTO creditos (id_venta, id_cliente, monto_total, saldo_pendiente, estado, fecha_limite, observacion)
SELECT
    vf.id_venta,
    vf.id_cliente,
    9.00,
    9.00,
    'PENDIENTE',
    CURRENT_DATE + 7,
    'Credito inicial de prueba'
FROM venta_fiado vf;

WITH abastecimiento AS (
    INSERT INTO abastecimientos (
        id_usuario,
        proveedor,
        lugar_compra,
        tipo_abastecimiento,
        contacto,
        comprobante,
        total,
        fecha_abastecimiento,
        observacion
    )
    SELECT
        u.id_usuario,
        'Don Luis',
        'Mercado Central',
        'PROVEEDOR',
        '999111222',
        'Boleta',
        85.00,
        CURRENT_TIMESTAMP,
        'Ingreso inicial de inventario'
    FROM usuarios u
    WHERE u.usuario = 'admin'
      AND NOT EXISTS (
          SELECT 1
          FROM abastecimientos a
          WHERE a.proveedor = 'Don Luis'
            AND a.fecha_abastecimiento::date = CURRENT_DATE
      )
    RETURNING id_abastecimiento
)
INSERT INTO detalle_abastecimientos (
    id_abastecimiento,
    id_producto,
    cantidad,
    unidad_compra,
    costo_unitario,
    precio_venta,
    subtotal
)
SELECT
    a.id_abastecimiento,
    p.id_producto,
    25,
    'KG',
    3.40,
    4.20,
    85.00
FROM abastecimiento a
JOIN productos p ON p.nombre = 'Arroz Costeno';

INSERT INTO egresos (id_usuario, tipo_egreso, descripcion, monto, fecha_egreso)
SELECT
    u.id_usuario,
    'SERVICIO',
    'Pago de luz',
    35.00,
    CURRENT_TIMESTAMP
FROM usuarios u
WHERE u.usuario = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM egresos e
      WHERE e.tipo_egreso = 'SERVICIO'
        AND e.descripcion = 'Pago de luz'
        AND e.fecha_egreso::date = CURRENT_DATE
  );
