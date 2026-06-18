package com.example;

import com.vdch.abastecimiento.model.Abastecimiento;
import com.vdch.abastecimiento.model.DetalleAbastecimiento;
import com.vdch.abastecimiento.service.AbastecimientoService;
import com.vdch.categoria.model.Categoria;
import com.vdch.categoria.service.CategoriaService;
import com.vdch.cliente.dto.ClienteResumen;
import com.vdch.cliente.model.Cliente;
import com.vdch.cliente.service.ClienteService;
import com.vdch.fiado.dto.FiadoResumen;
import com.vdch.fiado.service.FiadoService;
import com.vdch.inventario.dto.ProductoStockBajo;
import com.vdch.inventario.model.Producto;
import com.vdch.inventario.service.ProductoService;
import com.vdch.reportes.dto.ProductoVendidoReporte;
import com.vdch.reportes.dto.ResumenReporte;
import com.vdch.reportes.service.ReporteService;
import com.vdch.venta.model.DetalleVenta;
import com.vdch.venta.model.Venta;
import com.vdch.venta.service.VentaService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private BorderPane app;
    private VBox sidebar;
    private Stage mainStage;
    private String current = "Inicio";
    private Debt selectedDebt;

    private final ClienteService clienteService = new ClienteService();
    private final ProductoService productoService = new ProductoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final VentaService ventaService = new VentaService();
    private final FiadoService fiadoService = new FiadoService();
    private final AbastecimientoService abastecimientoService = new AbastecimientoService();
    private final ReporteService reporteService = new ReporteService();

    private final String[] menu = {"Inicio", "Ventas", "Inventario", "Categorias", "Clientes", "Fiados", "Reportes", "Abastecimiento"};
    private final List<Debt> debts = new ArrayList<>();
    private final Object cacheLock = new Object();
    private boolean backendLoaded;
    private boolean backendLoading;
    private ResumenReporte cachedResumen = new ResumenReporte();
    private List<ProductoStockBajo> cachedStockBajo = new ArrayList<>();
    private List<ProductoVendidoReporte> cachedMasVendidos = new ArrayList<>();
    private List<ClienteResumen> cachedClientes = new ArrayList<>();
    private List<Categoria> cachedCategorias = new ArrayList<>();
    private List<Producto> cachedProductos = new ArrayList<>();
    private List<FiadoResumen> cachedFiados = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        stage.setTitle("Virgencita de Chapi");
        stage.setMinWidth(1120);
        stage.setMinHeight(735);
        stage.setScene(login(stage));
        stage.show();
    }

    private Scene login(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("login-root");

        VBox brand = new VBox(14);
        brand.setAlignment(Pos.CENTER);
        brand.getStyleClass().add("brand-panel");
        ImageView logo = new ImageView(new Image("file:C:/Users/sozha/OneDrive/Documentos/demo/_docx_reference/unzipped/word/media/image14.png", true)); //Cambiar ruta de
        logo.setFitWidth(138);
        logo.setFitHeight(138);
        logo.setPreserveRatio(true);
        brand.getChildren().addAll(logo, label("Virgencita de Chapi", "brand-title"), label("Tienda familiar", "brand-subtitle"));

        VBox form = new VBox(14);
        form.getStyleClass().add("login-form");
        TextField user = input("Escribe tu usuario");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Escribe tu clave");
        pass.getStyleClass().add("input");
        Button enter = button(icon("login") + "  Entrar al sistema", "primary big-button");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setOnAction(e -> openApp(stage));
        form.getChildren().addAll(
                title("Iniciar sesion", 24),
                field("Usuario", user),
                field("Contrasena", pass),
                enter
        );

        HBox center = new HBox(36, brand, form);
        center.setAlignment(Pos.CENTER);
        root.setCenter(center);

        Label footer = muted("2026 Virgencita de Chapi. Sistema de ventas e inventario.");
        BorderPane.setAlignment(footer, Pos.CENTER);
        BorderPane.setMargin(footer, new Insets(0, 0, 24, 0));
        root.setBottom(footer);
        return scene(root, 980, 720);
    }

    private void openApp(Stage stage) {
        app = new BorderPane();
        app.getStyleClass().add("app-root");
        sidebar = new VBox(9);
        sidebar.getStyleClass().add("sidebar");
        app.setLeft(sidebar);
        refreshBackendData();
        go("Inicio");
        stage.setScene(scene(app, 1160, 735));
        notify("Bienvenida", "Lista para vender, revisar fiados y registrar pagos.");
    }

    private void go(String page) {
        current = page;
        drawSidebar();
        Node content = switch (page) {
            case "Ventas" -> ventas();
            case "Inventario" -> inventario();
            case "Categorias" -> categorias();
            case "Clientes" -> clientes();
            case "Fiados" -> fiados();
            case "Reportes" -> reportes();
            case "Abastecimiento" -> abastecimiento();
            default -> inicio();
        };
        app.setCenter(scrollable(content));
    }

    private void drawSidebar() {
        sidebar.getChildren().clear();
        VBox logo = new VBox(5, label(icon("store"), "logo-icon"), label("Virgencita de\nChapi", "logo-text"), label("Gestion de tienda", "logo-sub"));
        logo.setAlignment(Pos.CENTER);
        logo.getStyleClass().add("logo-box");
        sidebar.getChildren().add(logo);

        for (String item : menu) {
            Button b = new Button(icon(item) + "  " + item);
            b.getStyleClass().add("nav-button");
            if (item.equals(current)) b.getStyleClass().add("nav-active");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> go(item));
            sidebar.getChildren().add(b);
        }
    }

    private VBox inicio() {
        ResumenReporte resumen = safeResumenHoy();
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.setVgap(14);
        stats.add(stat(icon("Ventas") + " Ventas hoy", money(resumen.getVentas()), "Total registrado"), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", money(resumen.getGanancia()), "Margen del dia"), 1, 0);
        stats.add(stat(icon("Fiados") + " Por cobrar", money(resumen.getFiados()), "Saldo pendiente"), 2, 0);
        stats.add(stat(icon("Inventario") + " Stock bajo", String.valueOf(stockBajo.size()), "Productos por reponer"), 3, 0);

        HBox quick = new HBox(12,
                shortcut("Nueva venta", "Ventas"),
                shortcut("Productos", "Inventario"),
                shortcut("Clientes", "Clientes"),
                shortcut("Cobrar fiado", "Fiados")
        );
        VBox low = panel("Avisos importantes", stockRows(stockBajo));
        return page("Buen dia", null, stats, quick, chart(), low);
    }

    private VBox ventas() {
        List<CartLine> cartLines = new ArrayList<>();
        List<ClienteResumen> clientes = safeClientes(null);
        ComboBox<CustomerChoice> customer = new ComboBox<>();
        customer.getItems().add(new CustomerChoice(null, "Venta rapida", null, BigDecimal.ZERO));
        for (ClienteResumen cliente : clientes) {
            customer.getItems().add(new CustomerChoice(
                    cliente.getIdCliente(),
                    cliente.getNombres(),
                    cliente.getTelefono(),
                    valueOrZero(cliente.getSaldoFiado())
            ));
        }
        customer.setValue(customer.getItems().get(0));
        customer.getStyleClass().add("input");
        Label customerInfo = muted("Sin cliente registrado. Ideal para compras al paso.");
        customer.setOnAction(e -> {
            CustomerChoice value = customer.getValue();
            if (value == null || value.idCliente == null) {
                customerInfo.setText("Sin cliente registrado. Ideal para compras al paso.");
            } else {
                customerInfo.setText("Telefono: " + textOrEmpty(value.telefono) + " | Saldo fiado: " + money(value.saldoFiado));
            }
        });
        Button newCustomer = button(icon("add") + " Nuevo cliente", "soft big-button");
        newCustomer.setOnAction(e -> clienteModal());
        VBox customerBox = new VBox(10,
                title("Cliente que compra", 18),
                field("Seleccionar cliente", customer),
                customerInfo,
                newCustomer
        );
        customerBox.getStyleClass().add("customer-box");

        VBox cartItems = new VBox(8, muted("Carrito vacio"));
        Label total = title("Total: S/ 0.00", 20);

        HBox products = new HBox(12);
        for (Producto producto : safeProductos(null)) {
            products.getChildren().add(product(producto, cartItems, total, cartLines));
        }
        if (products.getChildren().isEmpty()) {
            products.getChildren().add(muted("No hay productos registrados en la base de datos."));
        }

        Button confirm = button(icon("ok") + "  Confirmar venta", "primary big-button");
        confirm.setOnAction(e -> registrarVentaDesdeCarrito(customer.getValue(), cartLines, false));
        Button charge = button(icon("money") + "  Cobrar ahora", "dark big-button");
        charge.setOnAction(e -> registrarVentaDesdeCarrito(customer.getValue(), cartLines, false));
        Button debt = button(icon("Fiados") + "  Guardar como fiado", "soft big-button");
        debt.setOnAction(e -> registrarVentaDesdeCarrito(customer.getValue(), cartLines, true));

        VBox cart = new VBox(12,
                title("Carrito", 18),
                label("Selecciona un producto y escribe cantidad o peso.", "notice-small"),
                cartItems,
                total,
                confirm,
                charge,
                debt
        );
        cart.getStyleClass().add("cart");
        VBox side = new VBox(14, customerBox, cart);
        HBox body = new HBox(18, products, side);
        HBox.setHgrow(products, Priority.ALWAYS);
        return page("Nueva venta", null, search("Buscar producto..."), body);
    }

    private VBox inventario() {
        Button add = button(icon("add") + " Nuevo producto", "dark big-button");
        add.setOnAction(e -> productModal());
        VBox list = new VBox(10);
        for (Producto producto : safeProductos(null)) {
            list.getChildren().add(item(producto));
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(muted("No hay productos registrados en la base de datos."));
        }
        return page("Inventario", add, search("Buscar producto..."), list);
    }

    private VBox categorias() {
        Button add = button(icon("add") + " Nueva categoria", "dark big-button");
        add.setOnAction(e -> categoriaModal());
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        List<Categoria> categorias = safeCategorias();
        for (int i = 0; i < categorias.size(); i++) {
            Categoria categoria = categorias.get(i);
            grid.add(category(categoria), i % 3, i / 3);
        }
        Node content = categorias.isEmpty() ? muted("No hay categorias registradas en la base de datos.") : grid;
        return page("Categorias", add, search("Buscar categoria..."), content);
    }

    private VBox clientes() {
        Button add = button(icon("add") + " Nuevo cliente", "dark big-button");
        add.setOnAction(e -> clienteModal());
        HBox cards = new HBox(12);
        for (ClienteResumen cliente : safeClientes(null)) {
            cards.getChildren().add(client(cliente));
        }
        if (cards.getChildren().isEmpty()) {
            cards.getChildren().add(muted("No hay clientes registrados en la base de datos."));
        }
        return page("Clientes", add, search("Buscar cliente..."), cards);
    }

    private VBox fiados() {
        Label hint = label("Toca Registrar pago para cobrar directamente desde aqui.", "notice");
        HBox cards = new HBox(14);
        loadDebtsFromBackend();
        for (Debt debt : debts) cards.getChildren().add(debtCard(debt));
        if (cards.getChildren().isEmpty()) {
            cards.getChildren().add(muted("No hay fiados pendientes en la base de datos."));
        }
        ScrollPane scroll = new ScrollPane(cards);
        scroll.getStyleClass().add("clean-scroll");
        scroll.setFitToHeight(true);
        return page("Fiados", null, hint, scroll);
    }

    private VBox pagar(Debt debt) {
        selectedDebt = debt;
        TextField amount = input(debt.amount.replace("S/ ", ""));
        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> go("Fiados"));
        Button pay = button(icon("ok") + " Confirmar pago", "primary big-button");
        pay.setOnAction(e -> {
            try {
                BigDecimal paid = parseMoney(amount.getText());
                if (selectedDebt.idCredito != null) {
                    fiadoService.registrarPago(selectedDebt.idCredito, paid, "EFECTIVO", "Pago desde frontend");
                }
                selectedDebt.pending = false;
                selectedDebt.date = "Pagado hoy";
                notify("Pago confirmado", selectedDebt.name + " ya figura como pagado.");
                refreshBackendData();
                go("Fiados");
            } catch (RuntimeException ex) {
                notify("Error de pago", shortError(ex));
            }
        });
        VBox card = new VBox(18,
                label(icon("money"), "pay-icon"),
                title("Pago de " + debt.name, 26),
                label("Saldo pendiente: " + debt.amount, "pay-balance"),
                field("Monto recibido", amount),
                buttons(cancel, pay)
        );
        card.getStyleClass().add("pay-card");
        VBox wrap = new VBox(card);
        wrap.setAlignment(Pos.CENTER);
        wrap.getStyleClass().add("pay-page");
        return wrap;
    }

    private VBox reportes() {
        ResumenReporte resumen = safeResumenHoy();
        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.add(stat(icon("Ventas") + " Ventas", money(resumen.getVentas()), "Hoy"), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", money(resumen.getGanancia()), "Hoy"), 1, 0);
        stats.add(stat(icon("Fiados") + " Fiados", money(resumen.getFiados()), "Pendiente"), 2, 0);
        stats.add(stat(icon("ok") + " Pagado", money(resumen.getPagado()), "Cobrado"), 3, 0);
        VBox sold = panel("Productos mas vendidos", soldRows(safeMasVendidos()));
        return page("Reportes", null, stats, chart(), sold);
    }

    private VBox abastecimiento() {
        long[] currentSupplyId = new long[]{0L};
        List<Producto> productos = safeProductos(null);

        Button supplierMode = button(icon("Clientes") + " Proveedor guardado", "primary big-button");
        supplierMode.setOnAction(e -> notify("Modo proveedor", "Registra los datos del proveedor que llega a la tienda."));
        Button marketMode = button(icon("store") + " Compra de mercado", "soft big-button");
        marketMode.setOnAction(e -> marketPurchaseModal());
        Button saveSupplier = button(icon("ok") + " Guardar proveedor", "dark big-button");

        TextField supplierName = input("Ej: Don Luis / Mercado Central");
        TextField supplyType = input("Proveedor, mercado, distribuidora");
        TextField contact = input("Opcional");
        TextField receipt = input("Boleta, factura o sin comprobante");
        TextField arrivalDate = input("Hoy");

        VBox supplier = new VBox(12,
                title("Datos del ingreso", 18),
                new HBox(10, supplierMode, marketMode),
                field("Proveedor o lugar de compra", supplierName),
                field("Tipo de abastecimiento", supplyType),
                field("Telefono o contacto", contact),
                field("Comprobante", receipt),
                field("Fecha de llegada", arrivalDate),
                saveSupplier
        );
        supplier.getStyleClass().add("supply-form");

        ComboBox<ProductChoice> productSelect = new ComboBox<>();
        for (Producto producto : productos) {
            productSelect.getItems().add(new ProductChoice(producto));
        }
        if (!productSelect.getItems().isEmpty()) {
            productSelect.setValue(productSelect.getItems().get(0));
        }
        productSelect.getStyleClass().add("input");
        TextField category = input("Granel, bebidas, limpieza...");
        TextField quantity = input("Ej: 25 kg, 12 unidades, 6 paquetes");
        TextField unit = input("Kilo, gramo, paquete, caja, unidad");
        TextField cost = input("Ej: 85.00");
        TextField salePrice = input("Ej: 4.20");
        TextField observation = input("Ej: pago pendiente, entrega incompleta");

        VBox productForm = new VBox(12,
                title("Producto que deja", 18),
                field("Producto", productSelect),
                field("Categoria", category),
                field("Cantidad recibida", quantity),
                field("Unidad de compra", unit),
                field("Costo de compra", cost),
                field("Precio de venta", salePrice),
                field("Observaciones", observation)
        );
        productForm.getStyleClass().add("supply-form");

        Button addLine = button(icon("add") + " Agregar producto recibido", "soft big-button");

        TableView<String[]> table = new TableView<>();
        String[] cols = {"Proveedor", "Producto", "Cantidad", "Unidad", "Costo", "Venta"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(v -> new SimpleStringProperty(v.getValue()[idx]));
            c.setPrefWidth(125);
            table.getColumns().add(c);
        }
        Button save = button(icon("ok") + " Guardar ingreso", "primary big-button");
        save.setOnAction(e -> {
            Long id = guardarAbastecimiento(supplierName, supplyType, contact, receipt, observation);
            if (id != null) {
                currentSupplyId[0] = id;
                notify("Abastecimiento guardado", "Ingreso #" + id + " listo para agregar productos.");
            }
        });
        saveSupplier.setOnAction(save.getOnAction());
        addLine.setOnAction(e -> {
            if (currentSupplyId[0] == 0L) {
                Long id = guardarAbastecimiento(supplierName, supplyType, contact, receipt, observation);
                if (id == null) return;
                currentSupplyId[0] = id;
            }
            ProductChoice selected = productSelect.getValue();
            if (selected == null) {
                notify("Producto requerido", "Selecciona un producto del inventario.");
                return;
            }
            DetalleAbastecimiento detail = new DetalleAbastecimiento();
            detail.setIdProducto(selected.producto.getIdProducto());
            QuantityParts parts = parseQuantity(quantity.getText(), unit.getText());
            detail.setCantidad(parts.amount);
            detail.setUnidadCompra(parts.unit);
            detail.setCostoUnitario(parseMoney(cost.getText()));
            detail.setPrecioVenta(parseMoney(salePrice.getText()));
            try {
                abastecimientoService.agregarProducto(currentSupplyId[0], detail);
                table.getItems().add(new String[]{
                        textOrEmpty(supplierName.getText()),
                        selected.producto.getNombre(),
                        parts.amount.toPlainString(),
                        parts.unit,
                        money(detail.getCostoUnitario()),
                        money(detail.getPrecioVenta())
                });
                notify("Producto agregado", "El inventario fue actualizado.");
                refreshBackendData();
            } catch (RuntimeException ex) {
                notify("Error de abastecimiento", shortError(ex));
            }
        });
        Button clean = button("Limpiar formulario", "gray big-button");
        clean.setOnAction(e -> notify("Formulario limpio", "Puedes ingresar nuevos productos."));
        VBox total = new VBox(12,
                title("Resumen", 18),
                row("Productos", "2"),
                row("Costo total", "S/ 130.00"),
                row("Valor estimado", "S/ 180.00"),
                row("Origen", "Proveedor / mercado"),
                save,
                clean
        );
        total.getStyleClass().add("cart");
        HBox forms = new HBox(16, supplier, productForm);
        HBox.setHgrow(supplier, Priority.ALWAYS);
        HBox.setHgrow(productForm, Priority.ALWAYS);
        HBox body = new HBox(18, table, total);
        HBox.setHgrow(table, Priority.ALWAYS);
        return page("Nuevo abastecimiento", null, forms, addLine, body);
    }

    private void productModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Ej: Arroz Costeno");
        TextField category = input("Id categoria, opcional");
        TextField type = input("UNIDAD, GRANEL o PAQUETE");
        TextField unit = input("UNIDAD, KG, G, PAQUETE");
        TextField equivalence = input("1");
        TextField salePrice = input("4.20");
        TextField cost = input("3.50");
        TextField stock = input("10");
        TextField minStock = input("2");

        VBox box = new VBox(12,
                label(icon("add"), "modal-icon"),
                title("Nuevo producto", 20),
                field("Nombre", name),
                field("Categoria", category),
                field("Tipo de venta", type),
                field("Unidad base", unit),
                field("Equivalencia", equivalence),
                field("Precio por unidad/kg", salePrice),
                field("Costo", cost),
                field("Stock actual", stock),
                field("Stock minimo", minStock)
        );
        box.getStyleClass().add("modal");

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            try {
                Producto product = new Producto();
                product.setNombre(name.getText());
                product.setIdCategoria(parseNullableLong(category.getText()));
                product.setTipoVenta(emptyToDefault(type.getText(), "UNIDAD"));
                product.setUnidadBase(emptyToDefault(unit.getText(), "UNIDAD"));
                product.setEquivalencia(parseMoney(emptyToDefault(equivalence.getText(), "1")));
                product.setPrecioVenta(parseMoney(salePrice.getText()));
                product.setPrecioCompra(parseMoney(cost.getText()));
                product.setStockActual(parseMoney(stock.getText()));
                product.setStockMinimo(parseMoney(minStock.getText()));
                Long id = productoService.guardarProducto(product);
                dialog.close();
                notify("Producto guardado", "Producto #" + id + " registrado.");
                refreshBackendData();
                go("Inventario");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        dialog.setTitle("Nuevo producto");
        dialog.setScene(scene(box, 450, 650));
        dialog.showAndWait();
    }

    private void productInfoModal(Producto product) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        Label sizeInfo = label(sizeGuide(product), "product-size-info");
        sizeInfo.setWrapText(true);
        sizeInfo.setVisible(false);
        sizeInfo.setManaged(false);

        Button sizeButton = button("Ver talle", "soft big-button");
        sizeButton.setOnAction(e -> {
            boolean show = !sizeInfo.isVisible();
            sizeInfo.setVisible(show);
            sizeInfo.setManaged(show);
            sizeButton.setText(show ? "Ocultar talle" : "Ver talle");
        });

        VBox box = new VBox(12,
                label(icon("Inventario"), "modal-icon"),
                title(product.getNombre(), 22),
                label(productSummary(product), "notice-small"),
                productInfoGrid(product),
                sizeButton,
                sizeInfo
        );
        box.getStyleClass().add("modal");

        Button close = button("Cerrar", "gray big-button");
        close.setOnAction(e -> dialog.close());
        box.getChildren().add(buttons(close, button("Listo", "primary big-button")));
        Button ready = findButtonInBox(box, "Listo");
        if (ready != null) {
            ready.setOnAction(e -> dialog.close());
        }

        dialog.setTitle("Detalle de producto");
        dialog.setScene(scene(box, 500, 620));
        dialog.showAndWait();
    }

    private void clienteModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Nombre completo");
        TextField phone = input("Telefono");
        TextField notes = input("Notas");
        VBox box = new VBox(12,
                label(icon("Clientes"), "modal-icon"),
                title("Nuevo cliente", 20),
                field("Nombre", name),
                field("Telefono", phone),
                field("Notas", notes)
        );
        box.getStyleClass().add("modal");

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            try {
                Cliente cliente = new Cliente();
                cliente.setNombres(name.getText());
                cliente.setTelefono(phone.getText());
                cliente.setNotas(notes.getText());
                Long id = clienteService.guardarCliente(cliente);
                dialog.close();
                notify("Cliente guardado", "Cliente #" + id + " registrado.");
                refreshBackendData();
                go("Clientes");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        dialog.setTitle("Nuevo cliente");
        dialog.setScene(scene(box, 390, 380));
        dialog.showAndWait();
    }

    private void categoriaModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Nombre de categoria");
        TextField description = input("Descripcion");
        VBox box = new VBox(12,
                label(icon("Categorias"), "modal-icon"),
                title("Nueva categoria", 20),
                field("Nombre", name),
                field("Descripcion", description)
        );
        box.getStyleClass().add("modal");

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            try {
                Categoria categoria = new Categoria();
                categoria.setNombre(name.getText());
                categoria.setDescripcion(description.getText());
                Long id = categoriaService.guardarCategoria(categoria);
                dialog.close();
                notify("Categoria guardada", "Categoria #" + id + " registrada.");
                refreshBackendData();
                go("Categorias");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        dialog.setTitle("Nueva categoria");
        dialog.setScene(scene(box, 390, 330));
        dialog.showAndWait();
    }

    private void marketPurchaseModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        VBox box = new VBox(12);
        box.getStyleClass().add("modal");
        box.getChildren().addAll(
                label(icon("store"), "modal-icon"),
                title("Compra de mercado", 22),
                label("Para compras normales sin proveedor fijo.", "notice-small"),
                field("Mercado o lugar", input("Ej: Mercado Central")),
                field("Puesto o vendedor", input("Ej: Puesto 12 / senora Rosa")),
                field("Producto comprado", input("Ej: Tomate, arroz, maracuya")),
                field("Cantidad comprada", input("Ej: 5 kg, 2 cajas, 12 unidades")),
                field("Unidad", input("Kilo, gramo, caja, paquete, unidad")),
                field("Costo total", input("Ej: S/ 45.00")),
                field("Precio de venta", input("Ej: S/ 7.50 x kg")),
                field("Observaciones", input("Ej: contado, sin boleta, producto fresco"))
        );

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar compra", "primary big-button");
        save.setOnAction(e -> {
            dialog.close();
            notify("Compra de mercado guardada", "La compra se agrego al abastecimiento.");
        });
        box.getChildren().add(buttons(cancel, save));

        dialog.setTitle("Compra de mercado");
        dialog.setScene(scene(box, 470, 690));
        dialog.showAndWait();
    }

    private void modal(String modalTitle, List<String> fields) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);
        VBox box = new VBox(12);
        box.getStyleClass().add("modal");
        box.getChildren().addAll(label(icon("add"), "modal-icon"), title(modalTitle, 20));
        for (String f : fields) box.getChildren().add(field(f, input(f.equals("Categoria") ? "Sin categoria" : "")));
        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            dialog.close();
            notify("Guardado", modalTitle + " registrado correctamente.");
        });
        box.getChildren().add(buttons(cancel, save));
        dialog.setTitle(modalTitle);
        dialog.setScene(scene(box, modalTitle.equals("Nuevo producto") ? 450 : 390, modalTitle.equals("Nuevo producto") ? 610 : 360));
        dialog.showAndWait();
    }

    private void refreshBackendData() {
        synchronized (cacheLock) {
            if (backendLoading) {
                return;
            }
            backendLoading = true;
        }

        Thread loader = new Thread(() -> {
            ResumenReporte resumen = new ResumenReporte();
            List<ProductoStockBajo> stockBajo = List.of();
            List<ProductoVendidoReporte> masVendidos = List.of();
            List<ClienteResumen> clientes = List.of();
            List<Categoria> categorias = List.of();
            List<Producto> productos = List.of();
            List<FiadoResumen> fiados = List.of();
            List<String> errors = new ArrayList<>();

            try { resumen = reporteService.obtenerResumenHoy(); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { stockBajo = productoService.listarProductosStockBajo(); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { masVendidos = reporteService.listarProductosMasVendidos(5); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { clientes = clienteService.buscarClientes(null); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { categorias = categoriaService.listarCategoriasActivas(); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { productos = productoService.buscarProductos(null); } catch (Throwable ex) { errors.add(shortError(ex)); }
            try { fiados = fiadoService.listarFiadosPendientes(); } catch (Throwable ex) { errors.add(shortError(ex)); }

            synchronized (cacheLock) {
                cachedResumen = resumen;
                cachedStockBajo = new ArrayList<>(stockBajo);
                cachedMasVendidos = new ArrayList<>(masVendidos);
                cachedClientes = new ArrayList<>(clientes);
                cachedCategorias = new ArrayList<>(categorias);
                cachedProductos = new ArrayList<>(productos);
                cachedFiados = new ArrayList<>(fiados);
                backendLoaded = true;
                backendLoading = false;
            }

            Platform.runLater(() -> {
                if (!errors.isEmpty()) {
                    notify("Base de datos", errors.get(0));
                }
                if (app != null) {
                    go(current);
                }
            });
        });
        loader.setDaemon(true);
        loader.start();
    }

    private ResumenReporte safeResumenHoy() {
        synchronized (cacheLock) {
            return cachedResumen;
        }
    }

    private List<ProductoStockBajo> safeStockBajo() {
        synchronized (cacheLock) {
            return new ArrayList<>(cachedStockBajo);
        }
    }

    private List<ProductoVendidoReporte> safeMasVendidos() {
        synchronized (cacheLock) {
            return new ArrayList<>(cachedMasVendidos);
        }
    }

    private List<ClienteResumen> safeClientes(String text) {
        synchronized (cacheLock) {
            if (text == null || text.isBlank()) {
                return new ArrayList<>(cachedClientes);
            }
            List<ClienteResumen> filtered = new ArrayList<>();
            for (ClienteResumen cliente : cachedClientes) {
                if (contains(cliente.getNombres(), text) || contains(cliente.getTelefono(), text)) {
                    filtered.add(cliente);
                }
            }
            return filtered;
        }
    }

    private List<Categoria> safeCategorias() {
        synchronized (cacheLock) {
            return new ArrayList<>(cachedCategorias);
        }
    }

    private List<Producto> safeProductos(String text) {
        synchronized (cacheLock) {
            if (text == null || text.isBlank()) {
                return new ArrayList<>(cachedProductos);
            }
            List<Producto> filtered = new ArrayList<>();
            for (Producto producto : cachedProductos) {
                if (contains(producto.getNombre(), text) || contains(producto.getDescripcion(), text)) {
                    filtered.add(producto);
                }
            }
            return filtered;
        }
    }

    private Node[] stockRows(List<ProductoStockBajo> products) {
        if (products.isEmpty()) {
            return new Node[]{row("Stock bajo", "Sin alertas")};
        }
        List<Node> rows = new ArrayList<>();
        for (ProductoStockBajo product : products) {
            rows.add(row(product.getNombre(), product.getStockActual() + " " + product.getUnidadBase()));
        }
        return rows.toArray(Node[]::new);
    }

    private Node[] soldRows(List<ProductoVendidoReporte> products) {
        if (products.isEmpty()) {
            return new Node[]{row("Sin ventas", "S/ 0.00")};
        }
        List<Node> rows = new ArrayList<>();
        for (ProductoVendidoReporte product : products) {
            rows.add(row(product.getNombre(), money(product.getTotalVendido())));
        }
        return rows.toArray(Node[]::new);
    }

    private VBox product(Producto product, VBox cartItems, Label total, List<CartLine> cartLines) {
        Button add = button(icon("add") + " Agregar", "soft");
        add.setOnAction(e -> askQuantity(product, cartItems, total, cartLines));
        VBox b = new VBox(
                9,
                title(product.getNombre(), 16),
                muted(product.getTipoVenta() + " | Stock " + valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase())),
                title(money(product.getPrecioVenta()), 15),
                add
        );
        b.getStyleClass().add("card");
        b.setPrefSize(170, 145);
        return b;
    }

    private void askQuantity(Producto product, VBox cartItems, Label total, List<CartLine> cartLines) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(mainStage);
        dialog.setTitle("Agregar producto");
        dialog.setHeaderText(product.getNombre() + " - " + money(product.getPrecioVenta()));
        dialog.setContentText("Cantidad:");
        dialog.getEditor().setPromptText(productPrompt(product));
        dialog.showAndWait().ifPresent(quantity -> {
            QuantityParts parts = parseQuantity(quantity, product.getUnidadBase());
            BigDecimal lineTotal = parts.normalizedFor(product).multiply(valueOrZero(product.getPrecioVenta()));
            CartLine line = new CartLine(product, parts.amount, parts.unit, quantity.trim(), valueOrZero(product.getPrecioVenta()), lineTotal);
            cartLines.add(line);
            refreshCart(cartItems, total, cartLines);
            notify("Producto agregado", product.getNombre() + " agregado al carrito.");
        });
    }

    private void refreshCart(VBox cartItems, Label total, List<CartLine> cartLines) {
        cartItems.getChildren().clear();
        if (cartLines.isEmpty()) {
            cartItems.getChildren().add(muted("Carrito vacio"));
        } else {
            for (CartLine line : cartLines) {
                cartItems.getChildren().add(row(line.product.getNombre(), line.quantityText + "  " + money(line.subtotal)));
            }
        }
        total.setText("Total: " + money(cartTotal(cartLines)));
    }

    private void registrarVentaDesdeCarrito(CustomerChoice customer, List<CartLine> cartLines, boolean asDebt) {
        if (cartLines.isEmpty()) {
            notify("Carrito vacio", "Agrega productos antes de confirmar.");
            return;
        }
        if (asDebt && (customer == null || customer.idCliente == null)) {
            notify("Cliente requerido", "Selecciona un cliente para guardar fiado.");
            return;
        }
        try {
            BigDecimal total = cartTotal(cartLines);
            Venta sale = new Venta();
            sale.setIdCliente(customer == null ? null : customer.idCliente);
            sale.setCanalVenta("TIENDA");
            sale.setTipoPago(asDebt ? "FIADO" : "CONTADO");
            sale.setTotal(total);
            sale.setMontoPagado(asDebt ? BigDecimal.ZERO : total);

            List<DetalleVenta> details = new ArrayList<>();
            for (CartLine line : cartLines) {
                DetalleVenta detail = new DetalleVenta();
                detail.setIdProducto(line.product.getIdProducto());
                detail.setCantidad(line.quantity);
                detail.setUnidadVenta(line.unit);
                detail.setCantidadTexto(line.quantityText);
                detail.setPrecioUnitario(line.unitPrice);
                details.add(detail);
            }

            if (asDebt) {
                ventaService.guardarVentaComoFiado(sale, details, LocalDate.now().plusDays(7), "Fiado registrado desde frontend");
                notify("Fiado agregado", "La venta paso a la lista de Fiados.");
                refreshBackendData();
                go("Fiados");
            } else {
                Long id = ventaService.registrarVentaConDetalles(sale, details);
                notify("Venta registrada", "Venta #" + id + " guardada correctamente.");
                refreshBackendData();
                go("Ventas");
            }
        } catch (RuntimeException ex) {
            notify("Error de venta", shortError(ex));
        }
    }

    private Long guardarAbastecimiento(TextField supplierName, TextField supplyType, TextField contact, TextField receipt, TextField observation) {
        try {
            Abastecimiento supply = new Abastecimiento();
            supply.setProveedor(supplierName.getText());
            supply.setLugarCompra(supplierName.getText());
            supply.setTipoAbastecimiento(emptyToDefault(supplyType.getText(), "PROVEEDOR"));
            supply.setContacto(contact.getText());
            supply.setComprobante(receipt.getText());
            supply.setObservacion(observation.getText());
            return abastecimientoService.guardarAbastecimiento(supply);
        } catch (RuntimeException ex) {
            notify("Error de abastecimiento", shortError(ex));
            return null;
        }
    }

    private void loadDebtsFromBackend() {
        synchronized (cacheLock) {
            debts.clear();
            for (FiadoResumen debt : cachedFiados) {
                debts.add(new Debt(
                        debt.getIdCredito(),
                        debt.getCliente(),
                        debt.getFechaCredito() == null ? "Pendiente" : debt.getFechaCredito().toLocalDate().toString(),
                        money(debt.getSaldoPendiente()),
                        true
                ));
            }
        }
    }

    private HBox item(Producto product) {
        String detail = product.getTipoVenta() + " | Stock " + valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase());
        HBox row = item(product.getNombre(), detail, money(product.getPrecioVenta()));
        Button edit = findButton(row, "Editar");
        if (edit != null) {
            edit.setOnAction(e -> productInfoModal(product));
        }
        Button delete = findButton(row, "Eliminar");
        if (delete != null && product.getIdProducto() != null) {
            delete.setOnAction(e -> {
                try {
                    productoService.cambiarEstadoProducto(product.getIdProducto(), false);
                    notify("Producto eliminado", product.getNombre() + " fue desactivado.");
                    refreshBackendData();
                    go("Inventario");
                } catch (RuntimeException ex) {
                    notify("Error al eliminar", shortError(ex));
                }
            });
        }
        return row;
    }

    private GridPane productInfoGrid(Producto product) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("product-info-grid");
        addInfo(grid, 0, "Codigo", product.getIdProducto() == null ? "Sin codigo" : "#" + product.getIdProducto());
        addInfo(grid, 1, "Tipo de venta", textOrEmpty(product.getTipoVenta()));
        addInfo(grid, 2, "Unidad base", textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 3, "Equivalencia", valueOrZero(product.getEquivalencia()).toPlainString());
        addInfo(grid, 4, "Precio venta", money(product.getPrecioVenta()));
        addInfo(grid, 5, "Costo", money(product.getPrecioCompra()));
        addInfo(grid, 6, "Stock actual", valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 7, "Stock minimo", valueOrZero(product.getStockMinimo()) + " " + textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 8, "Vencimiento", product.getFechaVencimiento() == null ? "No registrado" : product.getFechaVencimiento().toString());
        addInfo(grid, 9, "Estado", product.isEstado() ? "Activo" : "Inactivo");
        addInfo(grid, 10, "Descripcion", textOrEmpty(product.getDescripcion()).isBlank() ? "Sin descripcion" : product.getDescripcion());
        return grid;
    }

    private void addInfo(GridPane grid, int row, String name, String value) {
        Label key = label(name, "product-info-key");
        Label val = label(emptyToDefault(value, "No registrado"), "product-info-value");
        val.setWrapText(true);
        grid.add(key, 0, row);
        grid.add(val, 1, row);
    }

    private String productSummary(Producto product) {
        return "Stock " + valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase())
                + " | Precio " + money(product.getPrecioVenta());
    }

    private String sizeGuide(Producto product) {
        String unit = textOrEmpty(product.getUnidadBase()).toUpperCase();
        if ("KG".equals(unit) || "G".equals(unit) || "KILO".equals(unit)) {
            return "Info de talle: este producto se vende por peso. Usa el campo de cantidad como 500 g, 1 kg o 2 kg segun lo que pida el cliente.";
        }
        if ("PAQUETE".equals(unit) || "CAJA".equals(unit)) {
            return "Info de talle: registra el tamano del paquete o caja en la descripcion del producto. Ejemplo: paquete chico, mediano, grande o caja x12.";
        }
        return "Info de talle: talles sugeridos S, M, L y XL. Agrega el talle exacto en la descripcion del producto para que aparezca en esta ficha.";
    }

    private Button findButtonInBox(VBox box, String text) {
        for (Node node : box.getChildren()) {
            if (node instanceof HBox row) {
                Button button = findButton(row, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private VBox category(Categoria category) {
        return category(category.getNombre(), textOrEmpty(category.getDescripcion()));
    }

    private VBox client(ClienteResumen cliente) {
        return client(cliente.getNombres(), textOrEmpty(cliente.getTelefono()), money(cliente.getSaldoFiado()));
    }

    private Button findButton(HBox row, String text) {
        for (Node node : row.getChildren()) {
            if (node instanceof HBox actions) {
                for (Node child : actions.getChildren()) {
                    if (child instanceof Button button && button.getText().equals(text)) {
                        return button;
                    }
                }
            }
        }
        return null;
    }

    private String productPrompt(Producto product) {
        String unit = textOrEmpty(product.getUnidadBase()).toUpperCase();
        if ("KG".equals(unit) || "KILO".equals(unit)) return "Ej: 500 g o 2 kg";
        return "Ej: 1 unidad";
    }

    private BigDecimal cartTotal(List<CartLine> cartLines) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cartLines) {
            total = total.add(line.subtotal);
        }
        return total;
    }

    private QuantityParts parseQuantity(String raw, String defaultUnit) {
        String clean = emptyToDefault(raw, "1").trim().toLowerCase();
        String[] parts = clean.split("\\s+");
        BigDecimal amount = parseMoney(parts[0]);
        String unit = parts.length > 1 ? parts[1] : emptyToDefault(defaultUnit, "UNIDAD");
        return new QuantityParts(amount, normalizeUnit(unit));
    }

    private String normalizeUnit(String unit) {
        String clean = emptyToDefault(unit, "UNIDAD").trim().toUpperCase();
        return switch (clean) {
            case "KILO", "KILOS" -> "KG";
            case "GRAMO", "GRAMOS" -> "G";
            case "UNIDADES" -> "UNIDAD";
            default -> clean;
        };
    }

    private BigDecimal parseMoney(String value) {
        String clean = emptyToDefault(value, "0")
                .replace("S/", "")
                .replace(",", ".")
                .trim();
        return new BigDecimal(clean.isBlank() ? "0" : clean);
    }

    private Long parseNullableLong(String value) {
        String clean = textOrEmpty(value).trim();
        return clean.isBlank() ? null : Long.parseLong(clean);
    }

    private String money(BigDecimal value) {
        return "S/ " + valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean contains(String source, String text) {
        return source != null && text != null && source.toLowerCase().contains(text.toLowerCase());
    }

    private String shortError(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "Revisa la conexion con la base de datos.";
        return message.length() > 90 ? message.substring(0, 90) + "..." : message;
    }

    private VBox page(String pageTitle, Button action, Node... nodes) {
        VBox page = new VBox(18);
        page.getStyleClass().add("page");
        HBox top = new HBox(12, title(pageTitle, 26), grow());
        top.setAlignment(Pos.CENTER_LEFT);
        if (action != null) top.getChildren().add(action);
        page.getChildren().add(top);
        page.getChildren().addAll(nodes);
        return page;
    }

    private VBox stat(String name, String value, String tag) {
        VBox b = new VBox(7, muted(name), title(value, 22), muted(tag));
        b.getStyleClass().add("stat");
        b.setPrefWidth(205);
        return b;
    }

    private ScrollPane scrollable(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("main-scroll");
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private HBox item(String name, String detail, String price) {
        Button edit = button("Editar", "soft");
        edit.setOnAction(e -> notify("Editar producto", "Abriendo ficha de " + name + "."));
        Button delete = button("Eliminar", "gray");
        delete.setOnAction(e -> notify("Aviso", "Selecciona un producto guardado para desactivarlo."));
        HBox actions = new HBox(8, edit, delete);
        VBox text = new VBox(4, title(name, 16), muted(detail));
        HBox row = new HBox(12, text, grow(), title(price, 16), actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }

    private VBox category(String name, String detail) {
        VBox b = new VBox(10, title(icon("Categorias") + "  " + name, 16), muted(detail), buttons(button("Editar", "soft"), button("Ver", "gray")));
        b.getStyleClass().add("card");
        b.setPrefSize(245, 118);
        return b;
    }

    private VBox client(String name, String phone, String debt) {
        Button details = button("Ver detalle", "soft");
        details.setOnAction(e -> notify("Cliente", name + " tiene saldo " + debt + "."));
        VBox b = new VBox(9, title(icon("Clientes") + "  " + name, 16), muted(phone), details, muted("Saldo " + debt));
        b.getStyleClass().add("card");
        b.setPrefSize(230, 150);
        return b;
    }

    private VBox debtCard(Debt debt) {
        Button action = button(debt.pending ? icon("money") + " Registrar pago" : icon("ok") + " Pagado", debt.pending ? "primary big-button" : "gray big-button");
        action.setDisable(!debt.pending);
        action.setOnAction(e -> app.setCenter(pagar(debt)));
        VBox b = new VBox(10, title(icon("Fiados") + "  " + debt.name, 17), muted(debt.date), title(debt.amount, 18), action);
        b.getStyleClass().add(debt.pending ? "debt-card" : "paid-card");
        b.setPrefSize(235, 165);
        return b;
    }

    private HBox row(String left, String right) {
        HBox r = new HBox(10, new Label(left), grow(), new Label(right));
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("list-row");
        return r;
    }

    private VBox panel(String panelTitle, Node... rows) {
        VBox p = new VBox(9, title(panelTitle, 17));
        p.getChildren().addAll(rows);
        p.getStyleClass().add("panel");
        return p;
    }

    private Button shortcut(String text, String target) {
        Button b = new Button(icon(target) + "\n" + text);
        b.getStyleClass().add("shortcut");
        b.setPrefSize(175, 78);
        b.setOnAction(e -> go(target));
        return b;
    }

    private HBox search(String prompt) {
        TextField s = input(prompt);
        Button find = button(icon("search") + " Buscar", "soft");
        find.setOnAction(e -> notify("Busqueda", "Filtro aplicado en pantalla."));
        HBox box = new HBox(10, s, find);
        box.getStyleClass().add("search");
        HBox.setHgrow(s, Priority.ALWAYS);
        return box;
    }

    private LineChart<String, Number> chart() {
        LineChart<String, Number> c = new LineChart<>(new CategoryAxis(), new NumberAxis());
        c.setLegendVisible(false);
        c.setAnimated(false);
        c.setPrefHeight(260);
        c.getStyleClass().add("chart");
        return c;
    }

    private void notify(String title, String text) {
        if (mainStage == null || !mainStage.isShowing()) return;
        Popup popup = new Popup();
        VBox box = new VBox(4, label(title, "toast-title"), label(text, "toast-text"));
        box.getStyleClass().add("toast");
        popup.getContent().add(box);
        popup.setAutoHide(true);
        popup.show(mainStage, mainStage.getX() + mainStage.getWidth() - 360, mainStage.getY() + 82);
        new Thread(() -> {
            try {
                Thread.sleep(2300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            javafx.application.Platform.runLater(popup::hide);
        }).start();
    }

    private VBox field(String name, Control input) {
        return new VBox(6, label(name, "field-label"), input);
    }

    private TextField input(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.getStyleClass().add("input");
        return t;
    }

    private HBox buttons(Button a, Button b) {
        HBox h = new HBox(10, grow(), a, b);
        h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    private Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Label title(String text, int size) {
        Label l = new Label(text);
        l.getStyleClass().add("title");
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        return l;
    }

    private Label muted(String text) {
        return label(text, "muted");
    }

    private Label label(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().add(style);
        return l;
    }

    private Button button(String text, String styleClasses) {
        Button b = new Button(text);
        b.getStyleClass().addAll(styleClasses.split(" "));
        return b;
    }

    private Scene scene(javafx.scene.Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        return scene;
    }

    private String icon(String name) {
        return switch (name) {
            case "Inicio" -> "\u2302";
            case "Ventas", "Nueva venta" -> "\uD83D\uDED2";
            case "Inventario", "Productos" -> "\uD83D\uDCE6";
            case "Categorias" -> "\uD83D\uDCC1";
            case "Clientes" -> "\uD83D\uDC64";
            case "Fiados", "Cobrar fiado" -> "\uD83D\uDCCB";
            case "Reportes" -> "\uD83D\uDCCA";
            case "Abastecimiento" -> "\uD83D\uDE9A";
            case "money" -> "\uD83D\uDCB5";
            case "profit" -> "\uD83D\uDCC8";
            case "add" -> "\u2795";
            case "ok" -> "\u2713";
            case "search" -> "\uD83D\uDD0D";
            case "store" -> "\uD83C\uDFEA";
            case "login" -> "\u27A4";
            default -> "\u25CF";
        };
    }

    private static class Debt {
        private final Long idCredito;
        private final String name;
        private String date;
        private final String amount;
        private boolean pending;

        private Debt(String name, String date, String amount, boolean pending) {
            this(null, name, date, amount, pending);
        }

        private Debt(Long idCredito, String name, String date, String amount, boolean pending) {
            this.idCredito = idCredito;
            this.name = name;
            this.date = date;
            this.amount = amount;
            this.pending = pending;
        }
    }

    private static class CustomerChoice {
        private final Long idCliente;
        private final String name;
        private final String telefono;
        private final BigDecimal saldoFiado;

        private CustomerChoice(Long idCliente, String name, String telefono, BigDecimal saldoFiado) {
            this.idCliente = idCliente;
            this.name = name;
            this.telefono = telefono;
            this.saldoFiado = saldoFiado;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class ProductChoice {
        private final Producto producto;

        private ProductChoice(Producto producto) {
            this.producto = producto;
        }

        @Override
        public String toString() {
            return producto.getNombre();
        }
    }

    private static class CartLine {
        private final Producto product;
        private final BigDecimal quantity;
        private final String unit;
        private final String quantityText;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        private CartLine(Producto product, BigDecimal quantity, String unit, String quantityText, BigDecimal unitPrice, BigDecimal subtotal) {
            this.product = product;
            this.quantity = quantity;
            this.unit = unit;
            this.quantityText = quantityText;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }
    }

    private static class QuantityParts {
        private final BigDecimal amount;
        private final String unit;

        private QuantityParts(BigDecimal amount, String unit) {
            this.amount = amount;
            this.unit = unit;
        }

        private BigDecimal normalizedFor(Producto product) {
            String base = product.getUnidadBase() == null ? "UNIDAD" : product.getUnidadBase().toUpperCase();
            if (("KG".equals(base) || "KILO".equals(base)) && "G".equals(unit)) {
                return amount.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
            }
            if ("G".equals(base) && "KG".equals(unit)) {
                return amount.multiply(new BigDecimal("1000"));
            }
            return amount;
        }
    }
}
