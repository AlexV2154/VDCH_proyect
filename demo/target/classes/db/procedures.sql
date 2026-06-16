CREATE OR REPLACE FUNCTION fn_normalizar_cantidad(
    p_id_producto BIGINT,
    p_cantidad NUMERIC,
    p_unidad VARCHAR
) RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
    v_unidad_base VARCHAR(30);
    v_unidad VARCHAR(30);
BEGIN
    SELECT unidad_base INTO v_unidad_base
    FROM productos
    WHERE id_producto = p_id_producto;

    v_unidad_base := UPPER(COALESCE(v_unidad_base, 'UNIDAD'));
    v_unidad := UPPER(COALESCE(p_unidad, v_unidad_base));

    IF v_unidad_base IN ('KG', 'KILO') AND v_unidad IN ('G', 'GRAMO', 'GRAMOS') THEN
        RETURN p_cantidad / 1000;
    END IF;

    IF v_unidad_base IN ('G', 'GRAMO', 'GRAMOS') AND v_unidad IN ('KG', 'KILO') THEN
        RETURN p_cantidad * 1000;
    END IF;

    RETURN p_cantidad;
END;
$$;

CREATE OR REPLACE FUNCTION sp_guardar_cliente(
    p_id_cliente BIGINT,
    p_nombres VARCHAR,
    p_telefono VARCHAR,
    p_direccion VARCHAR,
    p_notas TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_cliente BIGINT;
BEGIN
    IF p_id_cliente IS NULL THEN
        INSERT INTO clientes (nombres, telefono, direccion, notas)
        VALUES (p_nombres, p_telefono, p_direccion, p_notas)
        RETURNING id_cliente INTO v_id_cliente;
    ELSE
        UPDATE clientes
        SET nombres = p_nombres,
            telefono = p_telefono,
            direccion = p_direccion,
            notas = p_notas
        WHERE id_cliente = p_id_cliente
        RETURNING id_cliente INTO v_id_cliente;
    END IF;

    RETURN v_id_cliente;
END;
$$;

CREATE OR REPLACE FUNCTION sp_guardar_categoria(
    p_id_categoria BIGINT,
    p_nombre VARCHAR,
    p_descripcion TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_categoria BIGINT;
BEGIN
    IF p_id_categoria IS NULL THEN
        INSERT INTO categorias (nombre, descripcion)
        VALUES (p_nombre, p_descripcion)
        RETURNING id_categoria INTO v_id_categoria;
    ELSE
        UPDATE categorias
        SET nombre = p_nombre,
            descripcion = p_descripcion
        WHERE id_categoria = p_id_categoria
        RETURNING id_categoria INTO v_id_categoria;
    END IF;

    RETURN v_id_categoria;
END;
$$;

CREATE OR REPLACE FUNCTION sp_guardar_producto(
    p_id_producto BIGINT,
    p_id_categoria BIGINT,
    p_nombre VARCHAR,
    p_descripcion TEXT,
    p_tipo_venta VARCHAR,
    p_unidad_base VARCHAR,
    p_equivalencia NUMERIC,
    p_precio_compra NUMERIC,
    p_precio_venta NUMERIC,
    p_stock_actual NUMERIC,
    p_stock_minimo NUMERIC,
    p_fecha_vencimiento DATE
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_producto BIGINT;
BEGIN
    IF p_id_producto IS NULL THEN
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
            fecha_vencimiento
        )
        VALUES (
            p_id_categoria,
            p_nombre,
            p_descripcion,
            UPPER(COALESCE(p_tipo_venta, 'UNIDAD')),
            UPPER(COALESCE(p_unidad_base, 'UNIDAD')),
            COALESCE(p_equivalencia, 1),
            COALESCE(p_precio_compra, 0),
            COALESCE(p_precio_venta, 0),
            COALESCE(p_stock_actual, 0),
            COALESCE(p_stock_minimo, 0),
            p_fecha_vencimiento
        )
        RETURNING id_producto INTO v_id_producto;
    ELSE
        UPDATE productos
        SET id_categoria = p_id_categoria,
            nombre = p_nombre,
            descripcion = p_descripcion,
            tipo_venta = UPPER(COALESCE(p_tipo_venta, 'UNIDAD')),
            unidad_base = UPPER(COALESCE(p_unidad_base, 'UNIDAD')),
            equivalencia = COALESCE(p_equivalencia, 1),
            precio_compra = COALESCE(p_precio_compra, 0),
            precio_venta = COALESCE(p_precio_venta, 0),
            stock_actual = COALESCE(p_stock_actual, 0),
            stock_minimo = COALESCE(p_stock_minimo, 0),
            fecha_vencimiento = p_fecha_vencimiento
        WHERE id_producto = p_id_producto
        RETURNING id_producto INTO v_id_producto;
    END IF;

    RETURN v_id_producto;
END;
$$;

CREATE OR REPLACE FUNCTION sp_registrar_venta(
    p_id_cliente BIGINT,
    p_id_usuario BIGINT,
    p_canal_venta VARCHAR,
    p_tipo_pago VARCHAR,
    p_total NUMERIC,
    p_monto_pagado NUMERIC
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_venta BIGINT;
    v_total NUMERIC := COALESCE(p_total, 0);
    v_pagado NUMERIC := COALESCE(p_monto_pagado, 0);
BEGIN
    INSERT INTO ventas (
        id_cliente,
        id_usuario,
        canal_venta,
        tipo_pago,
        total,
        monto_pagado,
        saldo_pendiente,
        estado
    )
    VALUES (
        p_id_cliente,
        p_id_usuario,
        UPPER(COALESCE(p_canal_venta, 'TIENDA')),
        UPPER(COALESCE(p_tipo_pago, 'CONTADO')),
        v_total,
        v_pagado,
        GREATEST(v_total - v_pagado, 0),
        CASE WHEN v_total - v_pagado > 0 THEN 'PENDIENTE' ELSE 'PAGADA' END
    )
    RETURNING id_venta INTO v_id_venta;

    RETURN v_id_venta;
END;
$$;

CREATE OR REPLACE FUNCTION sp_agregar_detalle_venta(
    p_id_venta BIGINT,
    p_id_producto BIGINT,
    p_cantidad NUMERIC,
    p_unidad_venta VARCHAR,
    p_cantidad_texto VARCHAR,
    p_precio_unitario NUMERIC
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_detalle BIGINT;
    v_cantidad_stock NUMERIC;
    v_subtotal NUMERIC;
    v_total NUMERIC;
    v_pagado NUMERIC;
BEGIN
    v_cantidad_stock := fn_normalizar_cantidad(p_id_producto, p_cantidad, p_unidad_venta);
    v_subtotal := v_cantidad_stock * p_precio_unitario;

    INSERT INTO detalle_ventas (
        id_venta,
        id_producto,
        cantidad,
        unidad_venta,
        cantidad_texto,
        precio_unitario,
        subtotal
    )
    VALUES (
        p_id_venta,
        p_id_producto,
        p_cantidad,
        UPPER(COALESCE(p_unidad_venta, 'UNIDAD')),
        p_cantidad_texto,
        p_precio_unitario,
        v_subtotal
    )
    RETURNING id_detalle_venta INTO v_id_detalle;

    UPDATE productos
    SET stock_actual = stock_actual - v_cantidad_stock
    WHERE id_producto = p_id_producto;

    SELECT COALESCE(SUM(subtotal), 0) INTO v_total
    FROM detalle_ventas
    WHERE id_venta = p_id_venta;

    SELECT monto_pagado INTO v_pagado
    FROM ventas
    WHERE id_venta = p_id_venta;

    UPDATE ventas
    SET total = v_total,
        saldo_pendiente = GREATEST(v_total - COALESCE(v_pagado, 0), 0),
        estado = CASE WHEN v_total - COALESCE(v_pagado, 0) > 0 THEN 'PENDIENTE' ELSE 'PAGADA' END
    WHERE id_venta = p_id_venta;

    RETURN v_id_detalle;
END;
$$;

CREATE OR REPLACE FUNCTION sp_crear_credito_desde_venta(
    p_id_venta BIGINT,
    p_fecha_limite DATE,
    p_observacion TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_credito BIGINT;
    v_id_cliente BIGINT;
    v_total NUMERIC;
    v_saldo NUMERIC;
BEGIN
    SELECT id_cliente, total, saldo_pendiente
    INTO v_id_cliente, v_total, v_saldo
    FROM ventas
    WHERE id_venta = p_id_venta;

    IF v_id_cliente IS NULL THEN
        RAISE EXCEPTION 'La venta % no tiene cliente para registrar fiado', p_id_venta;
    END IF;

    IF COALESCE(v_saldo, 0) <= 0 THEN
        RAISE EXCEPTION 'La venta % no tiene saldo pendiente', p_id_venta;
    END IF;

    INSERT INTO creditos (
        id_venta,
        id_cliente,
        monto_total,
        saldo_pendiente,
        estado,
        fecha_limite,
        observacion
    )
    VALUES (
        p_id_venta,
        v_id_cliente,
        v_total,
        v_saldo,
        'PENDIENTE',
        p_fecha_limite,
        p_observacion
    )
    RETURNING id_credito INTO v_id_credito;

    UPDATE ventas
    SET tipo_pago = 'FIADO',
        estado = 'PENDIENTE'
    WHERE id_venta = p_id_venta;

    RETURN v_id_credito;
END;
$$;

CREATE OR REPLACE FUNCTION sp_registrar_pago_credito(
    p_id_credito BIGINT,
    p_monto_pagado NUMERIC,
    p_metodo_pago VARCHAR,
    p_observacion TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_pago BIGINT;
    v_id_venta BIGINT;
    v_saldo NUMERIC;
BEGIN
    INSERT INTO pagos_credito (
        id_credito,
        monto_pagado,
        metodo_pago,
        observacion
    )
    VALUES (
        p_id_credito,
        p_monto_pagado,
        UPPER(COALESCE(p_metodo_pago, 'EFECTIVO')),
        p_observacion
    )
    RETURNING id_pago_credito INTO v_id_pago;

    UPDATE creditos
    SET saldo_pendiente = GREATEST(saldo_pendiente - p_monto_pagado, 0),
        estado = CASE WHEN saldo_pendiente - p_monto_pagado <= 0 THEN 'PAGADO' ELSE 'PENDIENTE' END
    WHERE id_credito = p_id_credito
    RETURNING id_venta, saldo_pendiente INTO v_id_venta, v_saldo;

    UPDATE ventas
    SET monto_pagado = LEAST(total, monto_pagado + p_monto_pagado),
        saldo_pendiente = v_saldo,
        estado = CASE WHEN v_saldo <= 0 THEN 'PAGADA' ELSE 'PENDIENTE' END
    WHERE id_venta = v_id_venta;

    RETURN v_id_pago;
END;
$$;

CREATE OR REPLACE FUNCTION sp_guardar_abastecimiento(
    p_id_abastecimiento BIGINT,
    p_id_usuario BIGINT,
    p_proveedor VARCHAR,
    p_lugar_compra VARCHAR,
    p_tipo_abastecimiento VARCHAR,
    p_contacto VARCHAR,
    p_comprobante VARCHAR,
    p_observacion TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_abastecimiento BIGINT;
BEGIN
    IF p_id_abastecimiento IS NULL THEN
        INSERT INTO abastecimientos (
            id_usuario,
            proveedor,
            lugar_compra,
            tipo_abastecimiento,
            contacto,
            comprobante,
            observacion
        )
        VALUES (
            p_id_usuario,
            p_proveedor,
            p_lugar_compra,
            UPPER(COALESCE(p_tipo_abastecimiento, 'PROVEEDOR')),
            p_contacto,
            p_comprobante,
            p_observacion
        )
        RETURNING id_abastecimiento INTO v_id_abastecimiento;
    ELSE
        UPDATE abastecimientos
        SET id_usuario = p_id_usuario,
            proveedor = p_proveedor,
            lugar_compra = p_lugar_compra,
            tipo_abastecimiento = UPPER(COALESCE(p_tipo_abastecimiento, 'PROVEEDOR')),
            contacto = p_contacto,
            comprobante = p_comprobante,
            observacion = p_observacion
        WHERE id_abastecimiento = p_id_abastecimiento
        RETURNING id_abastecimiento INTO v_id_abastecimiento;
    END IF;

    RETURN v_id_abastecimiento;
END;
$$;

CREATE OR REPLACE FUNCTION sp_agregar_detalle_abastecimiento(
    p_id_abastecimiento BIGINT,
    p_id_producto BIGINT,
    p_cantidad NUMERIC,
    p_unidad_compra VARCHAR,
    p_costo_unitario NUMERIC,
    p_precio_venta NUMERIC
) RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_detalle BIGINT;
    v_cantidad_stock NUMERIC;
    v_subtotal NUMERIC;
BEGIN
    v_cantidad_stock := fn_normalizar_cantidad(p_id_producto, p_cantidad, p_unidad_compra);
    v_subtotal := v_cantidad_stock * p_costo_unitario;

    INSERT INTO detalle_abastecimientos (
        id_abastecimiento,
        id_producto,
        cantidad,
        unidad_compra,
        costo_unitario,
        precio_venta,
        subtotal
    )
    VALUES (
        p_id_abastecimiento,
        p_id_producto,
        p_cantidad,
        UPPER(COALESCE(p_unidad_compra, 'UNIDAD')),
        p_costo_unitario,
        COALESCE(p_precio_venta, 0),
        v_subtotal
    )
    RETURNING id_detalle_abastecimiento INTO v_id_detalle;

    UPDATE productos
    SET stock_actual = stock_actual + v_cantidad_stock,
        precio_compra = p_costo_unitario,
        precio_venta = COALESCE(NULLIF(p_precio_venta, 0), precio_venta)
    WHERE id_producto = p_id_producto;

    UPDATE abastecimientos
    SET total = (
        SELECT COALESCE(SUM(subtotal), 0)
        FROM detalle_abastecimientos
        WHERE id_abastecimiento = p_id_abastecimiento
    )
    WHERE id_abastecimiento = p_id_abastecimiento;

    RETURN v_id_detalle;
END;
$$;

CREATE OR REPLACE FUNCTION sp_anular_venta(
    p_id_venta BIGINT
) RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    item RECORD;
BEGIN
    FOR item IN
        SELECT id_producto, cantidad, unidad_venta
        FROM detalle_ventas
        WHERE id_venta = p_id_venta
    LOOP
        UPDATE productos
        SET stock_actual = stock_actual + fn_normalizar_cantidad(item.id_producto, item.cantidad, item.unidad_venta)
        WHERE id_producto = item.id_producto;
    END LOOP;

    UPDATE creditos
    SET estado = 'ANULADO'
    WHERE id_venta = p_id_venta;

    UPDATE ventas
    SET estado = 'ANULADA',
        saldo_pendiente = 0
    WHERE id_venta = p_id_venta;
END;
$$;

CREATE OR REPLACE FUNCTION fn_buscar_productos(
    p_texto VARCHAR
) RETURNS TABLE (
    id_producto BIGINT,
    id_categoria BIGINT,
    nombre VARCHAR,
    descripcion TEXT,
    tipo_venta VARCHAR,
    unidad_base VARCHAR,
    precio_venta NUMERIC,
    stock_actual NUMERIC,
    stock_minimo NUMERIC
)
LANGUAGE sql
AS $$
    SELECT
        p.id_producto,
        p.id_categoria,
        p.nombre,
        p.descripcion,
        p.tipo_venta,
        p.unidad_base,
        p.precio_venta,
        p.stock_actual,
        p.stock_minimo
    FROM productos p
    WHERE p.estado = TRUE
      AND (
          p_texto IS NULL
          OR p.nombre ILIKE '%' || p_texto || '%'
          OR p.descripcion ILIKE '%' || p_texto || '%'
      )
    ORDER BY p.nombre;
$$;

CREATE OR REPLACE FUNCTION fn_listar_categorias()
RETURNS TABLE (
    id_categoria BIGINT,
    nombre VARCHAR,
    descripcion TEXT,
    estado BOOLEAN
)
LANGUAGE sql
AS $$
    SELECT
        id_categoria,
        nombre,
        descripcion,
        estado
    FROM categorias
    WHERE estado = TRUE
    ORDER BY nombre;
$$;

CREATE OR REPLACE FUNCTION fn_buscar_clientes(
    p_texto VARCHAR
) RETURNS TABLE (
    id_cliente BIGINT,
    nombres VARCHAR,
    telefono VARCHAR,
    direccion VARCHAR,
    notas TEXT,
    saldo_fiado NUMERIC
)
LANGUAGE sql
AS $$
    SELECT
        c.id_cliente,
        c.nombres,
        c.telefono,
        c.direccion,
        c.notas,
        COALESCE(SUM(cr.saldo_pendiente) FILTER (WHERE cr.estado = 'PENDIENTE'), 0) AS saldo_fiado
    FROM clientes c
    LEFT JOIN creditos cr ON cr.id_cliente = c.id_cliente
    WHERE c.estado = TRUE
      AND (
          p_texto IS NULL
          OR c.nombres ILIKE '%' || p_texto || '%'
          OR c.telefono ILIKE '%' || p_texto || '%'
      )
    GROUP BY c.id_cliente
    ORDER BY c.nombres;
$$;

CREATE OR REPLACE FUNCTION sp_cambiar_estado_cliente(
    p_id_cliente BIGINT,
    p_estado BOOLEAN
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE clientes
    SET estado = p_estado
    WHERE id_cliente = p_id_cliente;
END;
$$;

CREATE OR REPLACE FUNCTION sp_cambiar_estado_categoria(
    p_id_categoria BIGINT,
    p_estado BOOLEAN
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE categorias
    SET estado = p_estado
    WHERE id_categoria = p_id_categoria;
END;
$$;

CREATE OR REPLACE FUNCTION sp_cambiar_estado_producto(
    p_id_producto BIGINT,
    p_estado BOOLEAN
) RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE productos
    SET estado = p_estado
    WHERE id_producto = p_id_producto;
END;
$$;

CREATE OR REPLACE FUNCTION fn_listar_fiados()
RETURNS TABLE (
    id_credito BIGINT,
    id_cliente BIGINT,
    cliente VARCHAR,
    telefono VARCHAR,
    monto_total NUMERIC,
    saldo_pendiente NUMERIC,
    estado VARCHAR,
    fecha_credito TIMESTAMP,
    fecha_limite DATE
)
LANGUAGE sql
AS $$
    SELECT
        cr.id_credito,
        c.id_cliente,
        c.nombres AS cliente,
        c.telefono,
        cr.monto_total,
        cr.saldo_pendiente,
        cr.estado,
        cr.fecha_credito,
        cr.fecha_limite
    FROM creditos cr
    JOIN clientes c ON c.id_cliente = cr.id_cliente
    WHERE cr.estado IN ('PENDIENTE', 'VENCIDO')
    ORDER BY cr.fecha_credito DESC;
$$;

CREATE OR REPLACE FUNCTION fn_resumen_hoy()
RETURNS TABLE (
    ventas NUMERIC,
    ganancia NUMERIC,
    fiados NUMERIC,
    pagado NUMERIC
)
LANGUAGE sql
AS $$
    WITH ventas_hoy AS (
        SELECT *
        FROM ventas
        WHERE estado <> 'ANULADA'
          AND fecha_venta::date = CURRENT_DATE
    ),
    totales AS (
        SELECT
            COALESCE(SUM(total), 0) AS ventas,
            COALESCE(SUM(saldo_pendiente), 0) AS fiados,
            COALESCE(SUM(monto_pagado), 0) AS pagado
        FROM ventas_hoy
    ),
    ganancias AS (
        SELECT COALESCE(SUM((dv.precio_unitario - p.precio_compra) * fn_normalizar_cantidad(dv.id_producto, dv.cantidad, dv.unidad_venta)), 0) AS ganancia
        FROM detalle_ventas dv
        JOIN ventas_hoy v ON v.id_venta = dv.id_venta
        JOIN productos p ON p.id_producto = dv.id_producto
    )
    SELECT
        t.ventas,
        g.ganancia,
        t.fiados,
        t.pagado
    FROM totales t
    CROSS JOIN ganancias g;
$$;

CREATE OR REPLACE FUNCTION fn_productos_mas_vendidos(
    p_limite INTEGER DEFAULT 5
) RETURNS TABLE (
    id_producto BIGINT,
    nombre VARCHAR,
    cantidad_vendida NUMERIC,
    total_vendido NUMERIC
)
LANGUAGE sql
AS $$
    SELECT
        p.id_producto,
        p.nombre,
        COALESCE(SUM(dv.cantidad), 0) AS cantidad_vendida,
        COALESCE(SUM(dv.subtotal), 0) AS total_vendido
    FROM detalle_ventas dv
    JOIN ventas v ON v.id_venta = dv.id_venta
    JOIN productos p ON p.id_producto = dv.id_producto
    WHERE v.estado <> 'ANULADA'
    GROUP BY p.id_producto, p.nombre
    ORDER BY cantidad_vendida DESC, total_vendido DESC
    LIMIT p_limite;
$$;

CREATE OR REPLACE FUNCTION fn_productos_stock_bajo()
RETURNS TABLE (
    id_producto BIGINT,
    nombre VARCHAR,
    stock_actual NUMERIC,
    stock_minimo NUMERIC,
    unidad_base VARCHAR
)
LANGUAGE sql
AS $$
    SELECT
        id_producto,
        nombre,
        stock_actual,
        stock_minimo,
        unidad_base
    FROM productos
    WHERE estado = TRUE
      AND stock_actual <= stock_minimo
    ORDER BY stock_actual ASC, nombre ASC;
$$;
