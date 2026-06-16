package com.vdch.shared.constants;

public final class DbFunctions {
    public static final String GUARDAR_CLIENTE = "sp_guardar_cliente";
    public static final String GUARDAR_CATEGORIA = "sp_guardar_categoria";
    public static final String GUARDAR_PRODUCTO = "sp_guardar_producto";
    public static final String REGISTRAR_VENTA = "sp_registrar_venta";
    public static final String AGREGAR_DETALLE_VENTA = "sp_agregar_detalle_venta";
    public static final String CREAR_CREDITO_DESDE_VENTA = "sp_crear_credito_desde_venta";
    public static final String REGISTRAR_PAGO_CREDITO = "sp_registrar_pago_credito";
    public static final String GUARDAR_ABASTECIMIENTO = "sp_guardar_abastecimiento";
    public static final String AGREGAR_DETALLE_ABASTECIMIENTO = "sp_agregar_detalle_abastecimiento";
    public static final String ANULAR_VENTA = "sp_anular_venta";
    public static final String BUSCAR_PRODUCTOS = "fn_buscar_productos";
    public static final String LISTAR_CATEGORIAS = "fn_listar_categorias";
    public static final String BUSCAR_CLIENTES = "fn_buscar_clientes";
    public static final String LISTAR_FIADOS = "fn_listar_fiados";
    public static final String RESUMEN_HOY = "fn_resumen_hoy";
    public static final String PRODUCTOS_MAS_VENDIDOS = "fn_productos_mas_vendidos";
    public static final String PRODUCTOS_STOCK_BAJO = "fn_productos_stock_bajo";
    public static final String CAMBIAR_ESTADO_CLIENTE = "sp_cambiar_estado_cliente";
    public static final String CAMBIAR_ESTADO_CATEGORIA = "sp_cambiar_estado_categoria";
    public static final String CAMBIAR_ESTADO_PRODUCTO = "sp_cambiar_estado_producto";

    private DbFunctions() {
    }
}
