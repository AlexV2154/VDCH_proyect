package com.example;

import com.vdch.abastecimiento.model.Abastecimiento;
import com.vdch.abastecimiento.model.DetalleAbastecimiento;
import com.vdch.abastecimiento.service.AbastecimientoService;
import com.vdch.categoria.model.Categoria;
import com.vdch.categoria.service.CategoriaService;
import com.vdch.config.DatabaseConfig;
import com.vdch.cliente.dto.ClienteResumen;
import com.vdch.cliente.model.Cliente;
import com.vdch.cliente.service.ClienteService;
import com.vdch.fiado.dto.FiadoResumen;
import com.vdch.fiado.model.PagoCredito;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.print.PrinterJob;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main extends Application {
    private BorderPane app;
    private VBox sidebar;
    private Stage mainStage;
    private String current = "Inicio";
    private Debt selectedDebt;

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final ClienteService clienteService = new ClienteService();
    private final ProductoService productoService = new ProductoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final VentaService ventaService = new VentaService();
    private final FiadoService fiadoService = new FiadoService();
    private final AbastecimientoService abastecimientoService = new AbastecimientoService();
    private final ReporteService reporteService = new ReporteService();

    private final String[] menu = {"Inicio", "Ventas", "Inventario", "Categorias", "Clientes", "Fiados", "Reportes", "Agenda", "Abastecimiento"};
    private final List<Debt> debts = new ArrayList<>();
    private final Object cacheLock = new Object();
    private boolean backendLoaded;
    private boolean backendLoading;
    private int lastStockAlertCount = -1;
    private int lastExpirationAlertCount = -1;
    private String dashboardPeriod = "DIA";
    private String supplyModePreset = "PROVEEDOR";
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
        Label loginMessage = label("Ingresa tus credenciales para continuar.", "login-help");
        loginMessage.setWrapText(true);

        Button forgotPassword = button("Olvide mi contrasena", "link-button");
        forgotPassword.setMaxWidth(Double.MAX_VALUE);
        forgotPassword.setOnAction(e -> showForgotPasswordDialog());

        Button enter = button(icon("login") + "  Entrar al sistema", "primary big-button");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setDefaultButton(true);
        enter.setOnAction(e -> authenticateAndOpen(stage, user, pass, loginMessage));
        pass.setOnAction(e -> authenticateAndOpen(stage, user, pass, loginMessage));
        user.setOnAction(e -> pass.requestFocus());

        form.getChildren().addAll(
                title("Iniciar sesion", 24),
                field("Usuario", user),
                field("Contrasena", pass),
                loginMessage,
                enter,
                forgotPassword
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
    private void authenticateAndOpen(Stage stage, TextField user, PasswordField pass, Label loginMessage) {
        String username = user.getText() == null ? "" : user.getText().trim();
        String password = pass.getText() == null ? "" : pass.getText();

        if (username.isBlank() || password.isBlank()) {
            showLoginError(loginMessage, "Completa usuario y contrasena para ingresar.");
            return;
        }
        if (!ADMIN_USER.equals(username) || !ADMIN_PASSWORD.equals(password)) {
            pass.clear();
            showLoginError(loginMessage, "Credenciales incorrectas. Usa el usuario autorizado.");
            return;
        }
        if (!DatabaseConfig.testConnection()) {
            showLoginError(loginMessage, "Credenciales correctas, pero no hay conexion con la base de datos.");
            return;
        }

        loginMessage.getStyleClass().setAll("login-help");
        loginMessage.setText("Conexion correcta. Abriendo sistema...");
        openApp(stage);
    }

    private void showLoginError(Label loginMessage, String message) {
        loginMessage.getStyleClass().setAll("login-error");
        loginMessage.setText(message);
    }

    private void showForgotPasswordDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Recuperar contrasena");
        alert.setHeaderText("Credenciales de administrador");
        alert.setContentText("Usuario: admin\nContrasena: admin123\n\nSi necesitas cambiarla, actualiza las credenciales del sistema.");
        alert.showAndWait();
    }
    private void openApp(Stage stage) {
        app = new BorderPane();
        app.getStyleClass().add("app-root");
        sidebar = new VBox(9);
        sidebar.getStyleClass().add("sidebar");
        app.setLeft(sidebar);
        ensureAgendaTables();
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
            case "Agenda" -> agenda();
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
        ResumenReporte resumen = dashboardResumen(dashboardPeriod);
        List<ProductoVendidoReporte> vendidos = dashboardMasVendidos(dashboardPeriod);
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        List<Producto> vencimientos = expirationAlerts();
        ComboBox<String> filter = select(dashboardPeriod, "DIA", "SEMANA", "MES");
        filter.setOnAction(e -> {
            dashboardPeriod = filter.getValue();
            go("Inicio");
        });
        HBox filterBar = new HBox(10,
                label("Filtrar dashboard por", "field-label"),
                filter,
                label(dashboardRangeText(dashboardPeriod), "notice-small")
        );
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("dashboard-filter");

        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.setVgap(14);
        stats.add(stat(icon("Ventas") + " Ventas", money(resumen.getVentas()), dashboardPeriodLabel(dashboardPeriod)), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", money(resumen.getGanancia()), dashboardPeriodLabel(dashboardPeriod)), 1, 0);
        stats.add(stat(icon("Fiados") + " Fiados", money(resumen.getFiados()), dashboardPeriodLabel(dashboardPeriod)), 2, 0);
        stats.add(stat(icon("Inventario") + " Stock bajo", String.valueOf(stockBajo.size()), "Productos por reponer"), 3, 0);

        HBox quick = new HBox(12,
                shortcut("Nueva venta", "Ventas"),
                shortcut("Productos", "Inventario"),
                shortcut("Clientes", "Clientes"),
                shortcut("Cobrar fiado", "Fiados")
        );
        VBox low = panel("Avisos importantes", stockRows(stockBajo));
        VBox expirations = vencimientos.isEmpty()
                ? panel("Alertas de vencimiento", expirationRows(vencimientos))
                : expirationAlert(vencimientos);
        return page("Buen dia", null, filterBar, stats, quick, chart(vendidos, "Ventas por producto - " + dashboardPeriodLabel(dashboardPeriod)), low, expirations);
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

        VBox cartItems = new VBox(8, emptyCartLabel());
        cartItems.getStyleClass().add("cart-items");
        Label total = title("Total: S/ 0.00", 20);
        total.getStyleClass().add("cart-total-amount");

        FlowPane products = new FlowPane(12, 12);
        products.setAlignment(Pos.TOP_LEFT);
        products.setPrefWrapLength(620);
        products.setMaxWidth(Double.MAX_VALUE);
        for (Producto producto : safeProductos(null)) {
            products.getChildren().add(product(producto, cartItems, total, cartLines));
        }
        if (products.getChildren().isEmpty()) {
            products.getChildren().add(muted("No hay productos registrados en la base de datos."));
        }

        ComboBox<String> saleType = select("CONTADO", "CONTADO", "CREDITO");
        Label saleTypeInfo = label("Venta al contado: cobra y descuenta stock al confirmar.", "notice-small");
        Button saleAction = button(icon("money") + "  Cobrar ahora", "primary big-button");
        saleAction.setMaxWidth(Double.MAX_VALUE);
        saleAction.setOnAction(e -> registrarVentaDesdeCarrito(customer.getValue(), cartLines, "CREDITO".equals(saleType.getValue())));
        Button receipt = button(icon("Reportes") + "  Imprimir boleta", "soft big-button");
        receipt.setMaxWidth(Double.MAX_VALUE);
        receipt.setOnAction(e -> receiptModal(customer.getValue(), cartLines, "CREDITO".equals(saleType.getValue())));
        saleType.setOnAction(e -> {
            boolean credit = "CREDITO".equals(saleType.getValue());
            saleTypeInfo.setText(credit
                    ? "Credito: selecciona un cliente. Vence en 7 dias y genera 2% semanal si no paga."
                    : "Venta al contado: cobra y descuenta stock al confirmar.");
            saleAction.setText(credit ? icon("Fiados") + "  Guardar credito" : icon("money") + "  Cobrar ahora");
        });

        HBox cartHeader = new HBox(8, label(icon("Ventas"), "cart-header-icon"), title("Carrito", 18));
        cartHeader.setAlignment(Pos.CENTER_LEFT);
        HBox totalBox = new HBox(10, label("Total a pagar", "cart-total-title"), grow(), total);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.getStyleClass().add("cart-total-box");

        VBox cart = new VBox(12,
                cartHeader,
                label("Agrega productos y ajusta cantidades con + o -.", "notice-small"),
                cartItems,
                totalBox,
                field("Tipo de venta", saleType),
                saleTypeInfo,
                receipt,
                saleAction
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
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        VBox list = new VBox(10);
        for (Producto producto : safeProductos(null)) {
            list.getChildren().add(item(producto));
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(muted("No hay productos registrados en la base de datos."));
        }
        if (stockBajo.isEmpty()) {
            return page("Inventario", add, search("Buscar producto..."), list);
        }
        return page("Inventario", add, stockAlert(stockBajo), search("Buscar producto..."), list);
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
        BigDecimal pendingAmount = debt.amountValue;
        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> go("Fiados"));
        Button pay = button(icon("ok") + " Confirmar pago", "primary big-button");
        pay.setOnAction(e -> {
            try {
                BigDecimal paid = parseMoney(amount.getText());
                if (paid.compareTo(BigDecimal.ZERO) <= 0) {
                    notify("Monto invalido", "El pago debe ser mayor a S/ 0.00.");
                    return;
                }
                if (paid.compareTo(pendingAmount) > 0) {
                    notify("Pago excedido", "No puedes pagar mas que el saldo: " + money(pendingAmount) + ".");
                    return;
                }
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
        VBox history = paymentHistoryBox(debt.payments);
        VBox card = new VBox(18,
                label(icon("money"), "pay-icon"),
                title("Pago de " + debt.name, 26),
                label("Total a pagar: " + debt.amount, "pay-balance"),
                debt.interest.compareTo(BigDecimal.ZERO) > 0
                        ? label("Calculo: " + money(debt.baseAmount) + " + (" + money(debt.baseAmount) + " x 2% x " + debt.interestWeeks + " semana(s)) = " + debt.amount, "notice-small")
                        : label("Sin interes. El credito vence a los 7 dias.", "notice-small"),
                debt.interest.compareTo(BigDecimal.ZERO) > 0
                        ? label("Interes agregado: " + money(debt.interest) + " por " + debt.overdueDays + " dia(s) vencido(s).", "notice-small")
                        : label("Saldo base: " + money(debt.baseAmount), "muted"),
                label("No se permite pagar mas que el saldo pendiente.", "notice-small"),
                history,
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
        List<ProductoVendidoReporte> masVendidos = safeMasVendidos();
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        List<Producto> vencimientos = expirationAlerts();
        Button download = button(icon("Reportes") + " Descargar PDF", "primary big-button");
        download.setOnAction(e -> downloadReportsPdf());
        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.add(stat(icon("Ventas") + " Ventas", money(resumen.getVentas()), "Hoy"), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", money(resumen.getGanancia()), "Hoy"), 1, 0);
        stats.add(stat(icon("Fiados") + " Fiados", money(resumen.getFiados()), "Pendiente"), 2, 0);
        stats.add(stat(icon("ok") + " Pagado", money(resumen.getPagado()), "Cobrado"), 3, 0);
        VBox sold = panel("Productos mas vendidos", soldRows(masVendidos));
        VBox sales = panel("Resumen de ventas", salesReportRows(resumen, masVendidos));
        VBox stock = panel("Alertas de inventario", stockReportRows(stockBajo));
        VBox expirations = panel("Alertas de vencimiento", expirationRows(vencimientos));
        HBox reports = new HBox(16, sales, stock);
        HBox.setHgrow(sales, Priority.ALWAYS);
        HBox.setHgrow(stock, Priority.ALWAYS);
        return page("Reportes", download, stats, chart(), reports, expirations, sold);
    }

    private VBox agenda() {
        ensureAgendaTables();
        TextField supplierName = input("Ej: Pepsico");
        TextField supplierPhone = input("Ej: 999 888 777");
        TextField supplierDetail = input("Ej: gaseosas, galletas, snacks");
        Button saveSupplier = button(icon("add") + " Guardar proveedor", "primary big-button");
        saveSupplier.setMaxWidth(Double.MAX_VALUE);
        saveSupplier.setOnAction(e -> {
            if (supplierName.getText().trim().isBlank()) {
                notify("Falta nombre", "Escribe el nombre del proveedor.");
                return;
            }
            try {
                saveSupplierAgenda(supplierName.getText(), supplierPhone.getText(), supplierDetail.getText());
                notify("Proveedor guardado", "Ya aparece para abastecimiento.");
                go("Agenda");
            } catch (RuntimeException ex) {
                notify("Error", shortError(ex));
            }
        });
        VBox supplierForm = new VBox(12,
                title("Agregar proveedor", 18),
                field("Nombre del proveedor", supplierName),
                field("Telefono o contacto", supplierPhone),
                field("Que trae o nota", supplierDetail),
                saveSupplier
        );
        supplierForm.getStyleClass().add("supply-form");

        TextField marketName = input("Ej: Puesto 12 - Don Luis");
        TextField marketSells = input("Ej: verduras, frutas, carnes");
        TextField marketNote = input("Ej: cobra al contado, atiende temprano");
        Button saveMarket = button(icon("add") + " Guardar puesto", "primary big-button");
        saveMarket.setMaxWidth(Double.MAX_VALUE);
        saveMarket.setOnAction(e -> {
            if (marketName.getText().trim().isBlank()) {
                notify("Falta puesto", "Escribe el puesto o nombre del puesto.");
                return;
            }
            try {
                saveMarketAgenda(marketName.getText(), marketSells.getText(), marketNote.getText());
                notify("Puesto guardado", "Ya aparece para compras de mercado.");
                go("Agenda");
            } catch (RuntimeException ex) {
                notify("Error", shortError(ex));
            }
        });
        VBox marketForm = new VBox(12,
                title("Agregar puesto de mercado", 18),
                field("Puesto o nombre", marketName),
                field("Que vende", marketSells),
                field("Nota", marketNote),
                saveMarket
        );
        marketForm.getStyleClass().add("supply-form");

        HBox forms = new HBox(16, supplierForm, marketForm);
        HBox.setHgrow(supplierForm, Priority.ALWAYS);
        HBox.setHgrow(marketForm, Priority.ALWAYS);

        VBox suppliers = new VBox(10, title("Proveedores guardados", 18));
        for (AgendaEntry entry : loadAgendaEntries("PROVEEDOR")) {
            suppliers.getChildren().add(agendaRow(entry, "PROVEEDOR"));
        }
        if (suppliers.getChildren().size() == 1) {
            suppliers.getChildren().add(muted("Aun no hay proveedores guardados."));
        }
        suppliers.getStyleClass().add("panel");

        VBox markets = new VBox(10, title("Puestos de mercado", 18));
        for (AgendaEntry entry : loadAgendaEntries("MERCADO")) {
            markets.getChildren().add(agendaRow(entry, "MERCADO"));
        }
        if (markets.getChildren().size() == 1) {
            markets.getChildren().add(muted("Aun no hay puestos guardados."));
        }
        markets.getStyleClass().add("panel");

        HBox lists = new HBox(16, suppliers, markets);
        HBox.setHgrow(suppliers, Priority.ALWAYS);
        HBox.setHgrow(markets, Priority.ALWAYS);
        return page("Agenda", null, forms, lists);
    }

    private HBox agendaRow(AgendaEntry entry, String type) {
        String detail = "PROVEEDOR".equals(type)
                ? emptyToDefault(entry.phone, "Sin telefono") + " | " + emptyToDefault(entry.detail, "Sin nota")
                : emptyToDefault(entry.detail, "Sin detalle");
        Button delete = button("Eliminar", "gray");
        delete.setOnAction(e -> confirmDelete("Eliminar", "Se quitara de la agenda: " + entry.name, () -> {
            try {
                deleteAgendaEntry(entry.id, type);
                notify("Eliminado", "Ya no aparecera en abastecimiento.");
                go("Agenda");
            } catch (RuntimeException ex) {
                notify("Error", shortError(ex));
            }
        }));
        HBox actions = new HBox(8, delete);
        VBox text = new VBox(4, title(entry.name, 16), muted(detail));
        HBox row = new HBox(12, text, grow(), actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("list-row");
        return row;
    }
    private VBox abastecimiento() {
        long[] currentSupplyId = new long[]{0L};
        boolean startAsMarket = isMarketSupply(supplyModePreset);

        TextField supplierName = input("");
        ComboBox<String> supplyType = select(supplyModePreset, "PROVEEDOR", "MERCADO");
        ComboBox<AgendaEntry> supplySource = new ComboBox<>();
        supplySource.getStyleClass().add("input");
        ComboBox<AgendaEntry> marketSource = new ComboBox<>();
        marketSource.getStyleClass().add("input");
        TextField contact = input("Telefono o contacto");
        ComboBox<String> receipt = select("SIN COMPROBANTE", "SIN COMPROBANTE", "BOLETA", "FACTURA", "TICKET");
        TextField observation = input("Que trae o nota");

        VBox supplierSourceField = field("Proveedor", supplySource);
        VBox marketStallField = field("Puesto de mercado", marketSource);
        VBox contactField = field("Telefono o contacto", contact);
        VBox receiptField = field("Comprobante", receipt);
        VBox observationField = field("Observaciones", observation);

        configureSupplySources(supplySource);
        configureMarketSources(marketSource);
        Runnable syncSupplySource = () -> {
            boolean market = isMarketSupply(supplyType.getValue());
            supplierSourceField.setVisible(!market);
            supplierSourceField.setManaged(!market);
            marketStallField.setVisible(market);
            marketStallField.setManaged(market);
            contactField.setVisible(!market);
            contactField.setManaged(!market);
            receiptField.setVisible(!market);
            receiptField.setManaged(!market);
            observationField.setVisible(!market);
            AgendaEntry selected = market ? marketSource.getValue() : supplySource.getValue();
            supplierName.setText(selected == null ? "" : selected.name);
            if (!market && selected != null) {
                contact.setText(textOrEmpty(selected.phone));
                observation.setText(textOrEmpty(selected.detail));
            }
        };
        supplySource.setOnAction(e -> syncSupplySource.run());
        marketSource.setOnAction(e -> syncSupplySource.run());
        syncSupplySource.run();

        VBox supplier = new VBox(12,
                title("Datos del abastecimiento", 18),
                field("Tipo", supplyType),
                supplierSourceField,
                marketStallField,
                contactField,
                receiptField,
                observationField
        );
        supplier.getStyleClass().add("supply-form");

        ComboBox<CategoriaChoice> category = supplyCategorySelect();
        ComboBox<ProductChoice> productSelect = new ComboBox<>();
        populateSupplyProducts(productSelect, selectedCategoryId(category), supplyType.getValue());
        productSelect.getStyleClass().add("input");

        ComboBox<String> unit = new ComboBox<>();
        unit.getStyleClass().add("input");
        configureSupplyUnits(unit, productSelect.getValue() == null ? null : productSelect.getValue().producto);

        category.setOnAction(e -> {
            populateSupplyProducts(productSelect, selectedCategoryId(category), supplyType.getValue());
            configureSupplyUnits(unit, productSelect.getValue() == null ? null : productSelect.getValue().producto);
        });
        productSelect.setOnAction(e -> configureSupplyUnits(unit, productSelect.getValue() == null ? null : productSelect.getValue().producto));
        supplyType.setOnAction(e -> {
            supplyModePreset = supplyType.getValue();
            configureSupplySources(supplySource);
            configureMarketSources(marketSource);
            syncSupplySource.run();
            populateSupplyProducts(productSelect, selectedCategoryId(category), supplyType.getValue());
            configureSupplyUnits(unit, productSelect.getValue() == null ? null : productSelect.getValue().producto);
        });

        TextField quantity = input("Ej: 25, 500 o 12");
        TextField cost = input("Ej: 85.00");
        TextField salePrice = input("Ej: 4.20");

        VBox productForm = new VBox(12,
                title("Producto", 18),
                field("Categoria", category),
                field("Producto", productSelect),
                field("Cantidad recibida", quantity),
                field("Unidad de compra", unit),
                field("Costo de compra", cost),
                field("Precio de venta", salePrice)
        );
        productForm.getStyleClass().add("supply-form");

        VBox purchaseList = new VBox(10, supplyEmptyLabel());
        purchaseList.getStyleClass().add("supply-list");
        VBox purchasePanel = new VBox(12, title("Lista", 18), purchaseList);
        purchasePanel.getStyleClass().add("supply-list-panel");

        int[] summaryProducts = new int[]{0};
        BigDecimal[] summaryCost = new BigDecimal[]{BigDecimal.ZERO};
        BigDecimal[] summarySaleValue = new BigDecimal[]{BigDecimal.ZERO};
        Label summaryProductsValue = new Label("0");
        Label summaryCostValue = new Label(money(BigDecimal.ZERO));
        Label summarySaleValueLabel = new Label(money(BigDecimal.ZERO));
        Label summaryProfitValue = new Label(money(BigDecimal.ZERO));

        Button addLine = button(icon("add") + " Agregar", "primary big-button");
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
            try {
                DetalleAbastecimiento detail = new DetalleAbastecimiento();
                detail.setIdProducto(selected.producto.getIdProducto());
                QuantityParts parts = parseQuantity(quantity.getText(), unit.getValue());
                detail.setCantidad(parts.amount);
                detail.setUnidadCompra(parts.unit);
                detail.setCostoUnitario(parseMoney(cost.getText()));
                detail.setPrecioVenta(parseMoney(salePrice.getText()));

                BigDecimal normalized = parts.normalizedFor(selected.producto);
                BigDecimal lineCost = normalized.multiply(detail.getCostoUnitario());
                BigDecimal lineSaleValue = normalized.multiply(detail.getPrecioVenta());
                abastecimientoService.agregarProducto(currentSupplyId[0], detail);

                if (summaryProducts[0] == 0) {
                    purchaseList.getChildren().clear();
                }
                purchaseList.getChildren().add(supplyPurchaseCard(
                        supplyOrigin(supplierName.getText(), supplyType.getValue()),
                        selected.producto.getNombre(),
                        parts.amount.toPlainString() + " " + parts.unit,
                        detail.getCostoUnitario(),
                        detail.getPrecioVenta(),
                        lineCost,
                        lineSaleValue
                ));

                summaryProducts[0]++;
                summaryCost[0] = summaryCost[0].add(lineCost);
                summarySaleValue[0] = summarySaleValue[0].add(lineSaleValue);
                summaryProductsValue.setText(String.valueOf(summaryProducts[0]));
                summaryCostValue.setText(money(summaryCost[0]));
                summarySaleValueLabel.setText(money(summarySaleValue[0]));
                summaryProfitValue.setText(money(summarySaleValue[0].subtract(summaryCost[0])));

                notify("Stock actualizado", selected.producto.getNombre() + " agregado al inventario.");
                quantity.clear();
                refreshBackendData();
            } catch (RuntimeException ex) {
                notify("Error de abastecimiento", shortError(ex));
            }
        });

        Button clean = button("Limpiar", "gray big-button");
        clean.setOnAction(e -> {
            quantity.clear();
            cost.clear();
            salePrice.clear();
        });

        VBox total = new VBox(12,
                title("Resumen", 18),
                summaryRow("Productos", summaryProductsValue),
                summaryRow("Costo", summaryCostValue),
                summaryRow("Valor venta", summarySaleValueLabel),
                summaryRow("Ganancia", summaryProfitValue),
                addLine,
                clean
        );
        total.getStyleClass().add("cart");

        HBox forms = new HBox(16, supplier, productForm);
        HBox.setHgrow(supplier, Priority.ALWAYS);
        HBox.setHgrow(productForm, Priority.ALWAYS);
        HBox body = new HBox(18, purchasePanel, total);
        HBox.setHgrow(purchasePanel, Priority.ALWAYS);
        return page(startAsMarket ? "Compra de mercado" : "Proveedor en tienda", null, forms, body);
    }
    private void productModal() {
        productFormModal(null);
    }

    private void productEditModal(Producto existing) {
        productFormModal(existing);
    }

    private void productFormModal(Producto existing) {
        boolean editing = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Ej: Arroz Costeno");
        TextField description = input("Descripcion del producto");
        ComboBox<CategoriaChoice> category = categorySelect(editing ? existing.getIdCategoria() : null);
        ComboBox<SellModeChoice> sellMode = sellModeSelect(editing ? existing.getUnidadBase() : null);
        TextField salePrice = input("Ej: 4.20");
        TextField cost = input("Ej: 3.50");
        TextField stock = input("Cantidad disponible");
        TextField minStock = input("Avisar cuando llegue a...");
        DatePicker expiration = datePicker(null);

        if (editing) {
            name.setText(textOrEmpty(existing.getNombre()));
            description.setText(textOrEmpty(existing.getDescripcion()));
            salePrice.setText(valueOrZero(existing.getPrecioVenta()).toPlainString());
            cost.setText(valueOrZero(existing.getPrecioCompra()).toPlainString());
            stock.setText(valueOrZero(existing.getStockActual()).toPlainString());
            minStock.setText(valueOrZero(existing.getStockMinimo()).toPlainString());
            expiration.setValue(existing.getFechaVencimiento());
        }

        VBox box = new VBox(12,
                label(icon(editing ? "Inventario" : "add"), "modal-icon"),
                title(editing ? "Editar producto" : "Nuevo producto", 20),
                label("Completa solo lo necesario. Los menus evitan errores al guardar.", "notice-small"),
                field("Nombre", name),
                field("Descripcion", description),
                field("Categoria", category),
                field("Se vende por", sellMode),
                label("Precio de venta: lo que cobra la tienda al cliente.", "product-help"),
                field("Precio de venta", salePrice),
                label("Costo de compra: lo que le costo a la tienda.", "product-help"),
                field("Costo de compra", cost),
                field("Cantidad en inventario", stock),
                field("Avisar stock bajo en", minStock),
                field("Fecha de vencimiento", expiration)
        );
        box.getStyleClass().add("modal");

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            try {
                Producto product = new Producto();
                if (editing) {
                    product.setIdProducto(existing.getIdProducto());
                    product.setEstado(existing.isEstado());
                }
                product.setNombre(name.getText());
                product.setDescripcion(description.getText());
                product.setIdCategoria(selectedCategoryId(category));
                SellModeChoice mode = selectedSellMode(sellMode);
                product.setTipoVenta(mode.tipoVenta);
                product.setUnidadBase(mode.unidadBase);
                product.setEquivalencia(BigDecimal.ONE);
                product.setPrecioVenta(parseMoney(salePrice.getText()));
                product.setPrecioCompra(parseMoney(cost.getText()));
                product.setStockActual(parseMoney(stock.getText()));
                product.setStockMinimo(parseMoney(minStock.getText()));
                product.setFechaVencimiento(expiration.getValue());
                Long id = productoService.guardarProducto(product);
                product.setIdProducto(id);
                product.setEstado(true);
                upsertCachedProduct(product);
                dialog.close();
                notify(editing ? "Producto actualizado" : "Producto guardado", "Producto #" + id + " listo.");
                refreshBackendData();
                go("Inventario");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        showModal(dialog, editing ? "Editar producto" : "Nuevo producto", box, 470, 760);
    }

    private void productInfoModal(Producto product) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        VBox box = new VBox(12,
                label(icon("Inventario"), "modal-icon"),
                title(product.getNombre(), 22),
                label(productSummary(product), "notice-small"),
                productInfoGrid(product)
        );
        box.getStyleClass().add("modal");

        Button close = button("Cerrar", "gray big-button");
        close.setOnAction(e -> dialog.close());
        box.getChildren().add(buttons(close, button("Listo", "primary big-button")));
        Button ready = findButtonInBox(box, "Listo");
        if (ready != null) {
            ready.setOnAction(e -> dialog.close());
        }

        showModal(dialog, "Detalle de producto", box, 500, 540);
    }

    private void clienteModal() {
        clienteFormModal(null);
    }

    private void clienteEditModal(ClienteResumen existing) {
        clienteFormModal(existing);
    }

    private void clienteFormModal(ClienteResumen existing) {
        boolean editing = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Nombre completo");
        TextField phone = input("Telefono");
        TextField address = input("Direccion");
        TextField notes = input("Notas");

        if (editing) {
            name.setText(textOrEmpty(existing.getNombres()));
            phone.setText(textOrEmpty(existing.getTelefono()));
            address.setText(textOrEmpty(existing.getDireccion()));
            notes.setText(textOrEmpty(existing.getNotas()));
        }

        VBox box = new VBox(12,
                label(icon("Clientes"), "modal-icon"),
                title(editing ? "Editar cliente" : "Nuevo cliente", 20),
                field("Nombre", name),
                field("Telefono", phone),
                field("Direccion", address),
                field("Notas", notes)
        );
        box.getStyleClass().add("modal");

        Button cancel = button("Cancelar", "gray big-button");
        cancel.setOnAction(e -> dialog.close());
        Button save = button(icon("ok") + " Guardar", "primary big-button");
        save.setOnAction(e -> {
            try {
                Cliente cliente = new Cliente();
                if (editing) {
                    cliente.setIdCliente(existing.getIdCliente());
                }
                cliente.setNombres(name.getText());
                cliente.setTelefono(phone.getText());
                cliente.setDireccion(address.getText());
                cliente.setNotas(notes.getText());
                Long id = clienteService.guardarCliente(cliente);
                dialog.close();
                notify(editing ? "Cliente actualizado" : "Cliente guardado", "Cliente #" + id + " listo.");
                refreshBackendData();
                go("Clientes");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        showModal(dialog, editing ? "Editar cliente" : "Nuevo cliente", box, 430, 500);
    }

    private void clienteInfoModal(ClienteResumen cliente) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("product-info-grid");
        addInfo(grid, 0, "Codigo", cliente.getIdCliente() == null ? "Sin codigo" : "#" + cliente.getIdCliente());
        addInfo(grid, 1, "Nombre", textOrEmpty(cliente.getNombres()));
        addInfo(grid, 2, "Telefono", textOrEmpty(cliente.getTelefono()));
        addInfo(grid, 3, "Direccion", textOrEmpty(cliente.getDireccion()).isBlank() ? "No registrada" : cliente.getDireccion());
        addInfo(grid, 4, "Notas", textOrEmpty(cliente.getNotas()).isBlank() ? "Sin notas" : cliente.getNotas());
        addInfo(grid, 5, "Saldo fiado", money(cliente.getSaldoFiado()));

        VBox box = new VBox(12,
                label(icon("Clientes"), "modal-icon"),
                title(textOrEmpty(cliente.getNombres()), 22),
                label("Ficha del cliente", "notice-small"),
                grid
        );
        box.getStyleClass().add("modal");

        Button close = button("Cerrar", "primary big-button");
        close.setOnAction(e -> dialog.close());
        box.getChildren().add(buttons(button("Volver", "gray big-button"), close));
        Button back = findButtonInBox(box, "Volver");
        if (back != null) {
            back.setOnAction(e -> dialog.close());
        }

        showModal(dialog, "Detalle de cliente", box, 470, 470);
    }

    private void categoriaModal() {
        categoriaFormModal(null);
    }

    private void categoriaEditModal(Categoria existing) {
        categoriaFormModal(existing);
    }

    private void categoriaFormModal(Categoria existing) {
        boolean editing = existing != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        TextField name = input("Nombre de categoria");
        TextField description = input("Descripcion");

        if (editing) {
            name.setText(textOrEmpty(existing.getNombre()));
            description.setText(textOrEmpty(existing.getDescripcion()));
        }

        VBox box = new VBox(12,
                label(icon("Categorias"), "modal-icon"),
                title(editing ? "Editar categoria" : "Nueva categoria", 20),
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
                if (editing) {
                    categoria.setIdCategoria(existing.getIdCategoria());
                }
                categoria.setNombre(name.getText());
                categoria.setDescripcion(description.getText());
                Long id = categoriaService.guardarCategoria(categoria);
                dialog.close();
                notify(editing ? "Categoria actualizada" : "Categoria guardada", "Categoria #" + id + " lista.");
                refreshBackendData();
                go("Categorias");
            } catch (RuntimeException ex) {
                notify("Error al guardar", shortError(ex));
            }
        });
        box.getChildren().add(buttons(cancel, save));
        showModal(dialog, editing ? "Editar categoria" : "Nueva categoria", box, 410, 360);
    }

    private void categoriaInfoModal(Categoria categoria) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("product-info-grid");
        addInfo(grid, 0, "Codigo", categoria.getIdCategoria() == null ? "Sin codigo" : "#" + categoria.getIdCategoria());
        addInfo(grid, 1, "Nombre", textOrEmpty(categoria.getNombre()));
        addInfo(grid, 2, "Descripcion", textOrEmpty(categoria.getDescripcion()).isBlank() ? "Sin descripcion" : categoria.getDescripcion());
        addInfo(grid, 3, "Estado", categoria.isEstado() ? "Activa" : "Inactiva");

        VBox box = new VBox(12,
                label(icon("Categorias"), "modal-icon"),
                title(textOrEmpty(categoria.getNombre()), 22),
                label("Ficha de categoria", "notice-small"),
                grid
        );
        box.getStyleClass().add("modal");

        Button close = button("Cerrar", "primary big-button");
        close.setOnAction(e -> dialog.close());
        box.getChildren().add(buttons(button("Volver", "gray big-button"), close));
        Button back = findButtonInBox(box, "Volver");
        if (back != null) {
            back.setOnAction(e -> dialog.close());
        }

        showModal(dialog, "Detalle de categoria", box, 430, 400);
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
        showModal(dialog, modalTitle, box, modalTitle.equals("Nuevo producto") ? 450 : 390, modalTitle.equals("Nuevo producto") ? 610 : 360);
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
                showStockAlertIfNeeded();
                showExpirationAlertIfNeeded();
                if (app != null) {
                    go(current);
                }
            });
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void showStockAlertIfNeeded() {
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        int count = stockBajo.size();
        if (count == 0) {
            lastStockAlertCount = 0;
            return;
        }
        if (count != lastStockAlertCount) {
            ProductoStockBajo first = stockBajo.get(0);
            notify("Alerta de stock", count + " producto(s) en minimo. Revisar: " + first.getNombre());
            lastStockAlertCount = count;
        }
    }

    private void showExpirationAlertIfNeeded() {
        List<Producto> vencimientos = expirationAlerts();
        int count = vencimientos.size();
        if (count == 0) {
            lastExpirationAlertCount = 0;
            return;
        }
        if (count != lastExpirationAlertCount) {
            Producto first = vencimientos.get(0);
            notify("Alerta de vencimiento", count + " producto(s) vencidos o por vencer. Revisar: " + first.getNombre());
            lastExpirationAlertCount = count;
        }
    }

    private ResumenReporte safeResumenHoy() {
        synchronized (cacheLock) {
            return cachedResumen;
        }
    }

    private ResumenReporte dashboardResumen(String period) {
        LocalDate[] range = dashboardRange(period);
        try {
            return reporteService.obtenerResumen(range[0], range[1]);
        } catch (RuntimeException ex) {
            return safeResumenHoy();
        }
    }

    private List<ProductoVendidoReporte> dashboardMasVendidos(String period) {
        LocalDate[] range = dashboardRange(period);
        try {
            return reporteService.listarProductosMasVendidos(range[0], range[1], 5);
        } catch (RuntimeException ex) {
            return safeMasVendidos();
        }
    }

    private LocalDate[] dashboardRange(String period) {
        LocalDate today = LocalDate.now();
        return switch (emptyToDefault(period, "DIA")) {
            case "SEMANA" -> new LocalDate[]{today.minusDays(6), today.plusDays(1)};
            case "MES" -> new LocalDate[]{today.withDayOfMonth(1), today.plusDays(1)};
            default -> new LocalDate[]{today, today.plusDays(1)};
        };
    }

    private String dashboardPeriodLabel(String period) {
        return switch (emptyToDefault(period, "DIA")) {
            case "SEMANA" -> "Esta semana";
            case "MES" -> "Este mes";
            default -> "Hoy";
        };
    }

    private String dashboardRangeText(String period) {
        LocalDate[] range = dashboardRange(period);
        return "Desde " + range[0] + " hasta " + range[1].minusDays(1);
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

    private void upsertCachedProduct(Producto product) {
        synchronized (cacheLock) {
            for (int i = 0; i < cachedProductos.size(); i++) {
                Producto currentProduct = cachedProductos.get(i);
                if (currentProduct.getIdProducto() != null && currentProduct.getIdProducto().equals(product.getIdProducto())) {
                    cachedProductos.set(i, product);
                    return;
                }
            }
            cachedProductos.add(product);
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

    private VBox stockAlert(List<ProductoStockBajo> products) {
        int sinStock = 0;
        for (ProductoStockBajo product : products) {
            if (valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0) {
                sinStock++;
            }
        }
        String message = "ALERTA: " + products.size() + " producto(s) en stock minimo";
        if (sinStock > 0) {
            message += " y " + sinStock + " sin stock";
        }
        VBox box = new VBox(8, label(message, "stock-alert-title"));
        int limit = Math.min(products.size(), 4);
        for (int i = 0; i < limit; i++) {
            ProductoStockBajo product = products.get(i);
            String status = valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0 ? "SIN STOCK" : "MINIMO";
            box.getChildren().add(row(product.getNombre(), status + " - " + units(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase())));
        }
        if (products.size() > limit) {
            box.getChildren().add(muted("Hay mas productos con alerta. Revisa Reportes > Alertas de inventario."));
        }
        box.getStyleClass().add("stock-alert");
        return box;
    }

    private List<Producto> expirationAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(30);
        List<Producto> alerts = new ArrayList<>();
        for (Producto product : safeProductos(null)) {
            LocalDate expiration = product.getFechaVencimiento();
            if (expiration != null && !expiration.isAfter(limit)) {
                alerts.add(product);
            }
        }
        alerts.sort((left, right) -> left.getFechaVencimiento().compareTo(right.getFechaVencimiento()));
        return alerts;
    }

    private VBox expirationAlert(List<Producto> products) {
        int expired = 0;
        for (Producto product : products) {
            if (product.getFechaVencimiento().isBefore(LocalDate.now())) {
                expired++;
            }
        }
        String message = "ALERTA: " + products.size() + " producto(s) vencidos o por vencer";
        if (expired > 0) {
            message += " y " + expired + " ya vencido(s)";
        }
        VBox box = new VBox(8, label(message, "expiry-alert-title"));
        int limit = Math.min(products.size(), 5);
        for (int i = 0; i < limit; i++) {
            Producto product = products.get(i);
            box.getChildren().add(row(product.getNombre(), expirationStatus(product.getFechaVencimiento())));
        }
        if (products.size() > limit) {
            box.getChildren().add(muted("Hay mas productos por vencer. Revisa Reportes > Alertas de vencimiento."));
        }
        box.getStyleClass().add("expiry-alert");
        return box;
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

    private Node[] salesReportRows(ResumenReporte resumen, List<ProductoVendidoReporte> products) {
        BigDecimal unidades = BigDecimal.ZERO;
        BigDecimal totalVendido = BigDecimal.ZERO;
        ProductoVendidoReporte top = null;
        for (ProductoVendidoReporte product : products) {
            unidades = unidades.add(valueOrZero(product.getCantidadVendida()));
            totalVendido = totalVendido.add(valueOrZero(product.getTotalVendido()));
            if (top == null || valueOrZero(product.getTotalVendido()).compareTo(valueOrZero(top.getTotalVendido())) > 0) {
                top = product;
            }
        }
        String topProduct = top == null ? "Sin ventas" : top.getNombre() + " (" + money(top.getTotalVendido()) + ")";
        return new Node[]{
                row("Ingreso del dia", money(resumen.getVentas())),
                row("Ganancia del dia", money(resumen.getGanancia())),
                row("Total vendido en ranking", money(totalVendido)),
                row("Cantidad vendida", units(unidades)),
                row("Producto lider", topProduct)
        };
    }

    private Node[] stockReportRows(List<ProductoStockBajo> products) {
        if (products.isEmpty()) {
            return new Node[]{row("Inventario", "Sin alertas de stock")};
        }
        int sinStock = 0;
        for (ProductoStockBajo product : products) {
            if (valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0) {
                sinStock++;
            }
        }
        List<Node> rows = new ArrayList<>();
        rows.add(row("Productos en minimo", String.valueOf(products.size())));
        rows.add(row("Productos sin stock", String.valueOf(sinStock)));
        for (ProductoStockBajo product : products) {
            String status = valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0 ? "SIN STOCK" : "Stock minimo";
            rows.add(row(product.getNombre(), status + " - " + units(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase())));
        }
        return rows.toArray(Node[]::new);
    }

    private Node[] expirationRows(List<Producto> products) {
        if (products.isEmpty()) {
            return new Node[]{row("Vencimientos", "Sin productos vencidos o proximos a vencer")};
        }
        List<Node> rows = new ArrayList<>();
        rows.add(row("Rango de alerta", "Vencidos y proximos 30 dias"));
        rows.add(row("Productos con alerta", String.valueOf(products.size())));
        for (Producto product : products) {
            String detail = expirationStatus(product.getFechaVencimiento());
            if (product.getStockActual() != null) {
                detail += " | Stock " + units(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase());
            }
            rows.add(row(product.getNombre(), detail));
        }
        return rows.toArray(Node[]::new);
    }

    private String expirationStatus(LocalDate date) {
        if (date == null) {
            return "Sin fecha";
        }
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, date);
        if (days < 0) {
            return "VENCIDO hace " + Math.abs(days) + " dia(s) - " + date;
        }
        if (days == 0) {
            return "VENCE HOY - " + date;
        }
        return "Vence en " + days + " dia(s) - " + date;
    }

    private void downloadReportsPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte PDF");
        chooser.setInitialFileName("reporte-virgencita-" + LocalDate.now() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) {
            chooser.setInitialDirectory(downloads);
        }
        File selected = chooser.showSaveDialog(mainStage);
        if (selected == null) {
            return;
        }
        Path path = selected.toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            path = Path.of(path.toString() + ".pdf");
        }
        try {
            writeDetailedReportPdf(path);
            notify("PDF descargado", "Reporte guardado correctamente.");
        } catch (IOException ex) {
            notify("Error PDF", shortError(ex));
        }
    }

    private void writeDetailedReportPdf(Path path) throws IOException {
        ResumenReporte resumen = safeResumenHoy();
        List<ProductoVendidoReporte> masVendidos = safeMasVendidos();
        List<ProductoStockBajo> stockBajo = safeStockBajo();
        List<Producto> vencimientos = expirationAlerts();
        PdfReport pdf = new PdfReport();

        pdf.section("Resumen del dia");
        pdf.cardRow(new String[][]{
                {"Ventas", money(resumen.getVentas()), "Total registrado hoy"},
                {"Ganancia", money(resumen.getGanancia()), "Margen del dia"},
                {"Fiados", money(resumen.getFiados()), "Pendiente por cobrar"},
                {"Pagado", money(resumen.getPagado()), "Cobrado"}
        });

        pdf.note("Estado rapido", "Stock bajo: " + stockBajo.size()
                + " producto(s) | Vencimientos: " + vencimientos.size()
                + " producto(s) | Ranking: " + masVendidos.size() + " producto(s).");

        pdf.barChart("Grafica de productos mas vendidos", masVendidos);

        List<String[]> stockRows = new ArrayList<>();
        if (stockBajo.isEmpty()) {
            stockRows.add(new String[]{"Inventario", "OK", "Sin alertas", ""});
        } else {
            for (ProductoStockBajo product : stockBajo) {
                String status = valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0 ? "SIN STOCK" : "STOCK MINIMO";
                stockRows.add(new String[]{
                        product.getNombre(),
                        status,
                        units(product.getStockActual()),
                        textOrEmpty(product.getUnidadBase())
                });
            }
        }
        pdf.table(
                "Alertas de inventario",
                new String[]{"Producto", "Estado", "Stock", "Unidad"},
                new double[]{230, 105, 80, 75},
                stockRows
        );

        List<String[]> expirationRows = new ArrayList<>();
        if (vencimientos.isEmpty()) {
            expirationRows.add(new String[]{"Vencimientos", "OK", "Sin alertas", ""});
        } else {
            for (Producto product : vencimientos) {
                expirationRows.add(new String[]{
                        product.getNombre(),
                        product.getFechaVencimiento() == null ? "Sin fecha" : product.getFechaVencimiento().toString(),
                        expirationLabel(product.getFechaVencimiento()),
                        units(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase())
                });
            }
        }
        pdf.table(
                "Alertas de vencimiento",
                new String[]{"Producto", "Fecha", "Estado", "Stock"},
                new double[]{215, 90, 125, 80},
                expirationRows
        );

        List<String[]> soldRows = new ArrayList<>();
        if (masVendidos.isEmpty()) {
            soldRows.add(new String[]{"Sin ventas registradas", "0", "S/ 0.00"});
        } else {
            for (ProductoVendidoReporte product : masVendidos) {
                soldRows.add(new String[]{
                        product.getNombre(),
                        units(product.getCantidadVendida()),
                        money(product.getTotalVendido())
                });
            }
        }
        pdf.table(
                "Detalle de productos mas vendidos",
                new String[]{"Producto", "Cantidad", "Total"},
                new double[]{300, 95, 110},
                soldRows
        );

        writePdf(path, pdf.finish());
    }

    private String expirationLabel(LocalDate date) {
        if (date == null) {
            return "Sin fecha";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days < 0) {
            return "VENCIDO";
        }
        if (days == 0) {
            return "VENCE HOY";
        }
        return "Vence en " + days + " dia(s)";
    }

    private void writePdf(Path path, List<String> pages) throws IOException {
        List<String> objects = new ArrayList<>();
        objects.add("");
        objects.add("");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        List<Integer> pageIds = new ArrayList<>();
        for (String content : pages) {
            int contentId = objects.size() + 1;
            int length = content.getBytes(StandardCharsets.ISO_8859_1).length;
            objects.add("<< /Length " + length + " >>\nstream\n" + content + "\nendstream");
            int pageId = objects.size() + 1;
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentId + " 0 R >>");
            pageIds.add(pageId);
        }
        StringBuilder kids = new StringBuilder();
        for (Integer pageId : pageIds) {
            kids.append(pageId).append(" 0 R ");
        }
        objects.set(0, "<< /Type /Catalog /Pages 2 0 R >>");
        objects.set(1, "<< /Type /Pages /Kids [" + kids + "] /Count " + pageIds.size() + " >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i < offsets.size(); i++) {
            out.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
        Files.write(path, out.toByteArray());
    }

    private String fitPdfText(String text, int maxLength) {
        String value = textOrEmpty(text);
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String pdfNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private class PdfReport {
        private final List<String> pages = new ArrayList<>();
        private StringBuilder content;
        private int pageNumber = 1;
        private double y;

        private PdfReport() {
            startPage();
        }

        private void startPage() {
            content = new StringBuilder();
            fill(0.96, 0.98, 1.00);
            rect(0, 0, 595, 842, true);
            fill(0.03, 0.32, 0.58);
            rect(0, 760, 595, 82, true);
            fill(0.10, 0.55, 0.90);
            rect(0, 760, 595, 8, true);
            text("Virgencita de Chapi", 50, 806, 22, "F2", 1, 1, 1);
            text("Reporte de ventas, inventario y vencimientos", 50, 784, 11, "F1", 0.86, 0.94, 1);
            text("Fecha: " + LocalDate.now(), 438, 806, 10, "F2", 1, 1, 1);
            y = 720;
        }

        private List<String> finish() {
            footer();
            pages.add(content.toString());
            return pages;
        }

        private void ensure(double height) {
            if (y - height >= 62) {
                return;
            }
            footer();
            pages.add(content.toString());
            pageNumber++;
            startPage();
        }

        private void section(String title) {
            ensure(42);
            text(title, 50, y, 16, "F2", 0.02, 0.13, 0.27);
            fill(0.10, 0.55, 0.90);
            rect(50, y - 10, 70, 3, true);
            y -= 30;
        }

        private void cardRow(String[][] cards) {
            ensure(88);
            double x = 50;
            double width = 116;
            for (String[] card : cards) {
                fill(1, 1, 1);
                rect(x, y - 70, width, 70, true);
                stroke(0.80, 0.88, 0.94);
                rect(x, y - 70, width, 70, false);
                fill(0.10, 0.55, 0.90);
                rect(x, y - 3, width, 3, true);
                text(card[0], x + 10, y - 20, 9, "F2", 0.28, 0.38, 0.52);
                text(card[1], x + 10, y - 43, 15, "F2", 0.02, 0.13, 0.27);
                text(fitPdfText(card[2], 23), x + 10, y - 59, 8, "F1", 0.35, 0.45, 0.58);
                x += width + 13;
            }
            y -= 90;
        }

        private void note(String title, String detail) {
            ensure(56);
            fill(1.00, 0.97, 0.88);
            rect(50, y - 42, 495, 42, true);
            stroke(0.92, 0.75, 0.37);
            rect(50, y - 42, 495, 42, false);
            text(title, 64, y - 17, 10, "F2", 0.50, 0.27, 0.02);
            text(fitPdfText(detail, 82), 64, y - 32, 9, "F1", 0.50, 0.27, 0.02);
            y -= 60;
        }

        private void barChart(String title, List<ProductoVendidoReporte> products) {
            section(title);
            if (products.isEmpty()) {
                note("Grafica", "Todavia no hay ventas registradas para dibujar la grafica.");
                return;
            }
            int limit = Math.min(5, products.size());
            ensure(38 + limit * 30);
            BigDecimal max = BigDecimal.ZERO;
            for (int i = 0; i < limit; i++) {
                BigDecimal total = valueOrZero(products.get(i).getTotalVendido());
                if (total.compareTo(max) > 0) {
                    max = total;
                }
            }
            fill(1, 1, 1);
            rect(50, y - (limit * 30 + 18), 495, limit * 30 + 18, true);
            stroke(0.82, 0.89, 0.95);
            rect(50, y - (limit * 30 + 18), 495, limit * 30 + 18, false);
            y -= 22;
            for (int i = 0; i < limit; i++) {
                ProductoVendidoReporte product = products.get(i);
                double ratio = max.compareTo(BigDecimal.ZERO) == 0
                        ? 0
                        : valueOrZero(product.getTotalVendido()).divide(max, 4, RoundingMode.HALF_UP).doubleValue();
                text(fitPdfText(product.getNombre(), 24), 64, y, 9, "F2", 0.02, 0.13, 0.27);
                fill(0.90, 0.94, 0.98);
                rect(220, y - 8, 210, 10, true);
                fill(0.10, 0.55, 0.90);
                rect(220, y - 8, Math.max(4, 210 * ratio), 10, true);
                text(money(product.getTotalVendido()), 442, y, 9, "F2", 0.02, 0.13, 0.27);
                y -= 30;
            }
            y -= 14;
        }

        private void table(String title, String[] headers, double[] widths, List<String[]> rows) {
            section(title);
            drawTableHeader(headers, widths);
            boolean alternate = false;
            for (String[] row : rows) {
                if (y - 24 < 62) {
                    footer();
                    pages.add(content.toString());
                    pageNumber++;
                    startPage();
                    section(title + " (continuacion)");
                    drawTableHeader(headers, widths);
                    alternate = false;
                }
                fill(alternate ? 0.97 : 1.00, alternate ? 0.99 : 1.00, 1.00);
                rect(50, y - 20, 495, 24, true);
                stroke(0.88, 0.92, 0.96);
                line(50, y - 20, 545, y - 20);
                double x = 58;
                for (int i = 0; i < headers.length; i++) {
                    text(fitPdfText(i < row.length ? row[i] : "", cellLength(widths[i])), x, y - 11, 8, "F1", 0.09, 0.17, 0.30);
                    x += widths[i];
                }
                y -= 24;
                alternate = !alternate;
            }
            y -= 18;
        }

        private void drawTableHeader(String[] headers, double[] widths) {
            ensure(34);
            fill(0.03, 0.32, 0.58);
            rect(50, y - 22, 495, 24, true);
            double x = 58;
            for (int i = 0; i < headers.length; i++) {
                text(headers[i], x, y - 13, 8, "F2", 1, 1, 1);
                x += widths[i];
            }
            y -= 26;
        }

        private int cellLength(double width) {
            return Math.max(8, (int) (width / 5.2));
        }

        private void footer() {
            stroke(0.76, 0.84, 0.91);
            line(50, 44, 545, 44);
            text("Sistema Virgencita de Chapi", 50, 28, 8, "F1", 0.35, 0.45, 0.58);
            text("Pagina " + pageNumber, 500, 28, 8, "F1", 0.35, 0.45, 0.58);
        }

        private void fill(double r, double g, double b) {
            content.append(pdfNumber(r)).append(' ').append(pdfNumber(g)).append(' ').append(pdfNumber(b)).append(" rg\n");
        }

        private void stroke(double r, double g, double b) {
            content.append(pdfNumber(r)).append(' ').append(pdfNumber(g)).append(' ').append(pdfNumber(b)).append(" RG\n");
        }

        private void rect(double x, double y, double w, double h, boolean fill) {
            content.append(pdfNumber(x)).append(' ').append(pdfNumber(y)).append(' ')
                    .append(pdfNumber(w)).append(' ').append(pdfNumber(h)).append(fill ? " re f\n" : " re S\n");
        }

        private void line(double x1, double y1, double x2, double y2) {
            content.append(pdfNumber(x1)).append(' ').append(pdfNumber(y1)).append(" m ")
                    .append(pdfNumber(x2)).append(' ').append(pdfNumber(y2)).append(" l S\n");
        }

        private void text(String text, double x, double y, int size, String font, double r, double g, double b) {
            content.append("BT /").append(font).append(' ').append(size).append(" Tf ")
                    .append(pdfNumber(r)).append(' ').append(pdfNumber(g)).append(' ').append(pdfNumber(b)).append(" rg ")
                    .append(pdfNumber(x)).append(' ').append(pdfNumber(y)).append(" Td (")
                    .append(pdfEscape(text)).append(") Tj ET\n");
        }
    }

    private String pdfEscape(String text) {
        String clean = text == null ? "" : text
                .replace("\u00e1", "a")
                .replace("\u00e9", "e")
                .replace("\u00ed", "i")
                .replace("\u00f3", "o")
                .replace("\u00fa", "u")
                .replace("\u00c1", "A")
                .replace("\u00c9", "E")
                .replace("\u00cd", "I")
                .replace("\u00d3", "O")
                .replace("\u00da", "U")
                .replace("\u00f1", "n")
                .replace("\u00d1", "N");
        return clean.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
    private VBox product(Producto product, VBox cartItems, Label total, List<CartLine> cartLines) {
        Button add = button(icon("add") + " Agregar", "soft");
        add.setMinWidth(132);
        boolean outOfStock = valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0;
        add.setDisable(outOfStock);
        add.setOnAction(e -> askQuantity(product, cartItems, total, cartLines));
        Label name = title(product.getNombre(), 16);
        name.setWrapText(true);
        Label detail = muted(product.getTipoVenta() + " | " + stockLabel(product));
        detail.setWrapText(true);
        Label price = title(money(product.getPrecioVenta()), 15);
        VBox b = new VBox(
                9,
                name,
                detail,
                price,
                add
        );
        b.getStyleClass().addAll("card", "product-card");
        if (outOfStock) {
            b.getStyleClass().add("stock-empty-card");
        } else if (isStockLow(product)) {
            b.getStyleClass().add("stock-low-card");
        }
        b.setMinSize(205, 160);
        b.setPrefSize(205, 160);
        b.setMaxSize(205, 160);
        return b;
    }

    private boolean canAddToCart(Producto product, BigDecimal requested, List<CartLine> cartLines) {
        if (requested.compareTo(BigDecimal.ZERO) <= 0) {
            notify("Cantidad invalida", "La cantidad debe ser mayor a cero.");
            return false;
        }
        BigDecimal available = valueOrZero(product.getStockActual());
        BigDecimal reserved = reservedStock(product, cartLines);
        BigDecimal remaining = available.subtract(reserved);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            notify("Sin stock", product.getNombre() + " ya no tiene stock disponible.");
            return false;
        }
        if (requested.compareTo(remaining) > 0) {
            notify("Stock insuficiente", "Disponible: " + units(remaining) + " " + textOrEmpty(product.getUnidadBase()) + ".");
            return false;
        }
        return true;
    }

    private String stockValidationMessage(List<CartLine> cartLines) {
        for (CartLine line : cartLines) {
            BigDecimal available = valueOrZero(line.product.getStockActual());
            BigDecimal requested = requestedStock(line.product, cartLines);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                return line.product.getNombre() + " esta sin stock.";
            }
            if (requested.compareTo(available) > 0) {
                return line.product.getNombre() + " solo tiene " + units(available) + " " + textOrEmpty(line.product.getUnidadBase()) + ".";
            }
        }
        return null;
    }

    private BigDecimal remainingStockAfter(Producto product, List<CartLine> cartLines) {
        return valueOrZero(product.getStockActual()).subtract(requestedStock(product, cartLines));
    }

    private BigDecimal reservedStock(Producto product, List<CartLine> cartLines) {
        return requestedStock(product, cartLines);
    }

    private BigDecimal requestedStock(Producto product, List<CartLine> cartLines) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cartLines) {
            if (line.product.getIdProducto() != null && line.product.getIdProducto().equals(product.getIdProducto())) {
                total = total.add(new QuantityParts(line.quantity, line.unit).normalizedFor(product));
            }
        }
        return total;
    }

    private void notifyStockStatus(Producto product, BigDecimal remaining) {
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            notify("Producto agregado", product.getNombre() + " agregado. El stock quedaria en cero.");
            return;
        }
        if (remaining.compareTo(valueOrZero(product.getStockMinimo())) <= 0) {
            notify("Stock minimo", product.getNombre() + " quedaria en " + units(remaining) + " " + textOrEmpty(product.getUnidadBase()) + ".");
            return;
        }
        notify("Producto agregado", product.getNombre() + " agregado al carrito.");
    }

    private void notifyLowStockAfterSale(List<CartLine> cartLines) {
        for (CartLine line : cartLines) {
            BigDecimal remaining = valueOrZero(line.product.getStockActual()).subtract(requestedStock(line.product, cartLines));
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                notify("Stock agotado", line.product.getNombre() + " se quedo sin stock.");
                return;
            }
            if (remaining.compareTo(valueOrZero(line.product.getStockMinimo())) <= 0) {
                notify("Stock minimo", line.product.getNombre() + " llego a su stock minimo.");
                return;
            }
        }
    }

    private String stockLabel(Producto product) {
        if (valueOrZero(product.getStockActual()).compareTo(BigDecimal.ZERO) <= 0) {
            return "SIN STOCK";
        }
        String stock = "Stock " + units(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase());
        if (isStockLow(product)) {
            return stock + " (minimo)";
        }
        return stock;
    }

    private boolean isStockLow(Producto product) {
        BigDecimal stock = valueOrZero(product.getStockActual());
        return stock.compareTo(BigDecimal.ZERO) > 0 && stock.compareTo(valueOrZero(product.getStockMinimo())) <= 0;
    }

    private void askQuantity(Producto product, VBox cartItems, Label total, List<CartLine> cartLines) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(mainStage);
        dialog.setTitle("Agregar producto");
        dialog.setHeaderText(product.getNombre() + " - " + money(product.getPrecioVenta()));
        dialog.setContentText("Cantidad:");
        dialog.getEditor().setPromptText(productPrompt(product));
        dialog.showAndWait().ifPresent(quantity -> {
            try {
                QuantityParts parts = parseQuantity(quantity, product.getUnidadBase());
                BigDecimal normalized = parts.normalizedFor(product);
                if (!validateQuantityForProduct(product, normalized)) {
                    return;
                }
                if (!canAddToCart(product, normalized, cartLines)) {
                    return;
                }
                CartLine line = findCartLine(product, cartLines);
                if (line == null) {
                    line = new CartLine(product, normalized, cartUnit(product), null, valueOrZero(product.getPrecioVenta()));
                    cartLines.add(line);
                } else {
                    line.quantity = line.quantity.add(normalized);
                    updateCartLine(line);
                }
                refreshCart(cartItems, total, cartLines);
                notifyStockStatus(product, remainingStockAfter(product, cartLines));
            } catch (RuntimeException ex) {
                notify("Cantidad invalida", "Revisa la cantidad ingresada.");
            }
        });
    }

    private void refreshCart(VBox cartItems, Label total, List<CartLine> cartLines) {
        cartItems.getChildren().clear();
        if (cartLines.isEmpty()) {
            cartItems.getChildren().add(emptyCartLabel());
        } else {
            for (CartLine line : cartLines) {
                cartItems.getChildren().add(cartRow(line, cartItems, total, cartLines));
            }
        }
        total.setText("Total: " + money(cartTotal(cartLines)));
    }

    private VBox cartRow(CartLine line, VBox cartItems, Label total, List<CartLine> cartLines) {
        Label name = label(line.product.getNombre(), "cart-product-name");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        Label quantity = cartDetail("Cantidad", line.quantityText);
        Label unit = cartDetail("Precio unitario", money(line.unitPrice));
        Label subtotal = label("Subtotal: " + money(line.subtotal), "cart-line-subtotal");
        subtotal.setWrapText(true);
        VBox details = new VBox(5, quantity, unit, subtotal);
        details.setMaxWidth(Double.MAX_VALUE);

        Button minus = button("-", "cart-step gray");
        minus.setOnAction(e -> decreaseCartLine(line, cartItems, total, cartLines));
        Button plus = button("+", "cart-step soft");
        plus.setOnAction(e -> increaseCartLine(line, cartItems, total, cartLines));
        Button remove = button("Quitar", "cart-remove gray");
        remove.setOnAction(e -> {
            cartLines.remove(line);
            refreshCart(cartItems, total, cartLines);
        });
        HBox actions = new HBox(6, minus, plus, remove);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox row = new VBox(10, name, details, actions);
        row.getStyleClass().add("cart-row");
        return row;
    }

    private Label cartDetail(String label, String value) {
        Label detail = label(label + ": " + value, "cart-detail-line");
        detail.setWrapText(true);
        detail.setMaxWidth(Double.MAX_VALUE);
        return detail;
    }

    private void receiptModal(CustomerChoice customer, List<CartLine> cartLines, boolean credit) {
        if (cartLines.isEmpty()) {
            notify("Carrito vacio", "Agrega productos antes de imprimir la boleta.");
            return;
        }
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(mainStage);

        VBox receipt = receiptView(customer, cartLines, credit);
        Button close = button("Cerrar", "gray big-button");
        close.setOnAction(e -> dialog.close());
        Button print = button(icon("Reportes") + " Imprimir", "primary big-button");
        print.setOnAction(e -> printReceipt(receipt));

        VBox box = new VBox(12, receipt, buttons(close, print));
        box.getStyleClass().add("modal");
        showModal(dialog, "Boleta de venta", box, 470, 720);
    }

    private VBox receiptView(CustomerChoice customer, List<CartLine> cartLines, boolean credit) {
        String customerName = customer == null || customer.idCliente == null ? "Venta rapida" : customer.name;
        String phone = customer == null || customer.telefono == null || customer.telefono.isBlank() ? "Sin telefono" : customer.telefono;
        VBox items = new VBox(8);
        for (CartLine line : cartLines) {
            items.getChildren().add(receiptItem(line));
        }
        VBox receipt = new VBox(10,
                label("Virgencita de Chapi", "receipt-title"),
                label("Boleta de venta", "receipt-subtitle"),
                receiptLine("Fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                receiptLine("Cliente", customerName),
                receiptLine("Telefono", phone),
                receiptLine("Tipo de venta", credit ? "CREDITO" : "CONTADO"),
                new Separator(),
                label("Detalle de productos", "receipt-section"),
                items,
                new Separator(),
                receiptLine("Total de items", String.valueOf(cartLines.size())),
                receiptTotal("TOTAL", money(cartTotal(cartLines))),
                label(credit ? "Credito registrado por 7 dias. Si vence, sube 2% por cada semana sin pagar." : "Pago contado.", "receipt-note")
        );
        receipt.getStyleClass().add("receipt");
        receipt.setMaxWidth(Double.MAX_VALUE);
        return receipt;
    }

    private VBox receiptItem(CartLine line) {
        VBox item = new VBox(5,
                label(line.product.getNombre(), "receipt-item-name"),
                receiptLine("Cantidad", line.quantityText),
                receiptLine("Precio unitario", money(line.unitPrice)),
                receiptLine("Subtotal", money(line.subtotal))
        );
        item.getStyleClass().add("receipt-item");
        return item;
    }

    private HBox receiptLine(String left, String right) {
        Label l = label(left, "receipt-line-label");
        l.setWrapText(true);
        Label r = label(right, "receipt-line-value");
        r.setWrapText(true);
        HBox line = new HBox(8, l, grow(), r);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    private HBox receiptTotal(String left, String right) {
        Label l = label(left, "receipt-total-label");
        Label r = label(right, "receipt-total-value");
        HBox line = new HBox(8, l, grow(), r);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("receipt-total-box");
        return line;
    }

    private void printReceipt(Node receipt) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            notify("Impresora", "No se encontro una impresora disponible.");
            return;
        }
        if (!job.showPrintDialog(mainStage)) {
            return;
        }
        boolean printed = job.printPage(receipt);
        if (printed) {
            job.endJob();
            notify("Boleta", "Boleta enviada a imprimir.");
        } else {
            notify("Boleta", "No se pudo imprimir la boleta.");
        }
    }

    private Label emptyCartLabel() {
        Label empty = label("Carrito vacio. Agrega un producto para empezar.", "cart-empty");
        empty.setWrapText(true);
        return empty;
    }

    private void increaseCartLine(CartLine line, VBox cartItems, Label total, List<CartLine> cartLines) {
        BigDecimal step = cartStep(line);
        QuantityParts parts = new QuantityParts(step, line.unit);
        BigDecimal normalized = parts.normalizedFor(line.product);
        if (!canIncreaseCartLine(line, normalized, cartLines)) {
            return;
        }
        line.quantity = line.quantity.add(step);
        updateCartLine(line);
        refreshCart(cartItems, total, cartLines);
        notifyStockStatus(line.product, remainingStockAfter(line.product, cartLines));
    }

    private void decreaseCartLine(CartLine line, VBox cartItems, Label total, List<CartLine> cartLines) {
        BigDecimal step = cartStep(line);
        if (line.quantity.subtract(step).compareTo(BigDecimal.ZERO) <= 0) {
            cartLines.remove(line);
        } else {
            line.quantity = line.quantity.subtract(step);
            updateCartLine(line);
        }
        refreshCart(cartItems, total, cartLines);
    }

    private boolean canIncreaseCartLine(CartLine line, BigDecimal normalizedStep, List<CartLine> cartLines) {
        BigDecimal available = valueOrZero(line.product.getStockActual());
        BigDecimal requested = requestedStock(line.product, cartLines);
        BigDecimal remaining = available.subtract(requested);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            notify("Sin stock", line.product.getNombre() + " ya no tiene mas stock disponible.");
            return false;
        }
        if (normalizedStep.compareTo(remaining) > 0) {
            notify("Stock insuficiente", "Solo queda " + units(remaining) + " " + textOrEmpty(line.product.getUnidadBase()) + " disponible.");
            return false;
        }
        return true;
    }

    private BigDecimal cartStep(CartLine line) {
        String unit = normalizeUnit(line.unit);
        if ("KG".equals(unit)) {
            return new BigDecimal("0.100");
        }
        if ("G".equals(unit)) {
            return new BigDecimal("100");
        }
        return BigDecimal.ONE;
    }

    private String cartUnit(Producto product) {
        return normalizeUnit(product == null ? "UNIDAD" : product.getUnidadBase());
    }

    private CartLine findCartLine(Producto product, List<CartLine> cartLines) {
        for (CartLine line : cartLines) {
            if (line.product.getIdProducto() != null && line.product.getIdProducto().equals(product.getIdProducto())) {
                return line;
            }
        }
        return null;
    }

    private void updateCartLine(CartLine line) {
        line.quantityText = units(line.quantity) + " " + line.unit;
        line.subtotal = line.quantity.multiply(valueOrZero(line.product.getPrecioVenta()));
    }

    private void registrarVentaDesdeCarrito(CustomerChoice customer, List<CartLine> cartLines, boolean asDebt) {
        if (cartLines.isEmpty()) {
            notify("Carrito vacio", "Agrega productos antes de confirmar.");
            return;
        }
        String stockError = stockValidationMessage(cartLines);
        if (stockError != null) {
            notify("Stock insuficiente", stockError);
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
                notifyLowStockAfterSale(cartLines);
                go("Fiados");
            } else {
                Long id = ventaService.registrarVentaConDetalles(sale, details);
                notify("Venta registrada", "Venta #" + id + " guardada correctamente.");
                refreshBackendData();
                notifyLowStockAfterSale(cartLines);
                go("Ventas");
            }
        } catch (RuntimeException ex) {
            notify("Error de venta", shortError(ex));
        }
    }

    private Long guardarAbastecimiento(TextField supplierName, ComboBox<String> supplyType, TextField contact, ComboBox<String> receipt, TextField observation) {
        try {
            Abastecimiento supply = new Abastecimiento();
            supply.setProveedor(supplierName.getText());
            supply.setLugarCompra(supplierName.getText());
            supply.setTipoAbastecimiento(emptyToDefault(supplyType.getValue(), "PROVEEDOR"));
            supply.setContacto(contact.getText());
            supply.setComprobante(emptyToDefault(receipt.getValue(), "SIN COMPROBANTE"));
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
                BigDecimal interest = valueOrZero(debt.getInteresGenerado());
                BigDecimal totalWithInterest = valueOrZero(debt.getSaldoPendiente());
                BigDecimal baseAmount = totalWithInterest.subtract(interest);
                int overdueDays = debt.getDiasVencidos();
                int interestWeeks = interestWeeks(overdueDays);
                String detail = interest.compareTo(BigDecimal.ZERO) > 0
                        ? money(baseAmount) + " x 2% x " + interestWeeks + " semana(s) = " + money(interest)
                        : "Sin interes. Vence: " + (debt.getFechaLimite() == null ? "Sin fecha" : debt.getFechaLimite());
                List<PagoCredito> payments = debt.getIdCredito() == null
                        ? new ArrayList<>()
                        : fiadoService.listarPagosPorCredito(debt.getIdCredito());
                debts.add(new Debt(
                        debt.getIdCredito(),
                        debt.getCliente(),
                        debt.getFechaCredito() == null ? "Pendiente" : debt.getFechaCredito().toLocalDate().toString(),
                        money(totalWithInterest),
                        totalWithInterest,
                        baseAmount,
                        interest,
                        overdueDays,
                        interestWeeks,
                        detail,
                        true,
                        payments
                ));
            }
        }
    }

    private HBox item(Producto product) {
        String detail = product.getTipoVenta() + " | Stock " + valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase());
        HBox row = item(product.getNombre(), detail, money(product.getPrecioVenta()));
        Button edit = findButton(row, "Editar");
        if (edit != null) {
            edit.setOnAction(e -> productEditModal(product));
        }
        Button details = findButton(row, "Detalles");
        if (details != null) {
            details.setOnAction(e -> productInfoModal(product));
        }
        Button delete = findButton(row, "Eliminar");
        if (delete != null && product.getIdProducto() != null) {
            delete.setOnAction(e -> confirmDelete(
                    "Eliminar producto",
                    "Deseas eliminar el producto \"" + product.getNombre() + "\"?",
                    () -> {
                try {
                    productoService.cambiarEstadoProducto(product.getIdProducto(), false);
                    notify("Producto eliminado", product.getNombre() + " fue desactivado.");
                    refreshBackendData();
                    go("Inventario");
                } catch (RuntimeException ex) {
                    notify("Error al eliminar", shortError(ex));
                }
            }));
        }
        return row;
    }

    private GridPane productInfoGrid(Producto product) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("product-info-grid");
        addInfo(grid, 0, "Codigo", product.getIdProducto() == null ? "Sin codigo" : "#" + product.getIdProducto());
        addInfo(grid, 1, "Forma de venta", textOrEmpty(product.getTipoVenta()));
        addInfo(grid, 2, "Unidad principal", textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 3, "Precio venta", money(product.getPrecioVenta()));
        addInfo(grid, 4, "Costo compra", money(product.getPrecioCompra()));
        addInfo(grid, 5, "Stock actual", valueOrZero(product.getStockActual()) + " " + textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 6, "Stock minimo", valueOrZero(product.getStockMinimo()) + " " + textOrEmpty(product.getUnidadBase()));
        addInfo(grid, 7, "Vencimiento", product.getFechaVencimiento() == null ? "No registrado" : product.getFechaVencimiento().toString());
        addInfo(grid, 8, "Estado", product.isEstado() ? "Activo" : "Inactivo");
        addInfo(grid, 9, "Descripcion", textOrEmpty(product.getDescripcion()).isBlank() ? "Sin descripcion" : product.getDescripcion());
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
        Button edit = button("Editar", "soft");
        edit.setOnAction(e -> categoriaEditModal(category));
        Button details = button("Detalles", "soft");
        details.setOnAction(e -> categoriaInfoModal(category));
        Button delete = button("Eliminar", "gray");
        delete.setOnAction(e -> confirmDelete(
                "Eliminar categoria",
                "Deseas eliminar la categoria \"" + category.getNombre() + "\"?",
                () -> {
                    try {
                        categoriaService.cambiarEstadoCategoria(category.getIdCategoria(), false);
                        notify("Categoria eliminada", category.getNombre() + " fue desactivada.");
                        refreshBackendData();
                        go("Categorias");
                    } catch (RuntimeException ex) {
                        notify("Error al eliminar", shortError(ex));
                    }
                }));

        HBox actions = new HBox(8, edit, details, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox b = new VBox(10,
                title(icon("Categorias") + "  " + category.getNombre(), 16),
                muted(textOrEmpty(category.getDescripcion()).isBlank() ? "Sin descripcion" : category.getDescripcion()),
                actions
        );
        b.getStyleClass().add("card");
        b.setPrefSize(320, 140);
        return b;
    }

    private VBox client(ClienteResumen cliente) {
        Button edit = button("Editar", "soft");
        edit.setOnAction(e -> clienteEditModal(cliente));
        Button details = button("Detalles", "soft");
        details.setOnAction(e -> clienteInfoModal(cliente));
        Button delete = button("Eliminar", "gray");
        delete.setOnAction(e -> confirmDelete(
                "Eliminar cliente",
                "Deseas eliminar al cliente \"" + cliente.getNombres() + "\"?",
                () -> {
                    try {
                        clienteService.cambiarEstadoCliente(cliente.getIdCliente(), false);
                        notify("Cliente eliminado", cliente.getNombres() + " fue desactivado.");
                        refreshBackendData();
                        go("Clientes");
                    } catch (RuntimeException ex) {
                        notify("Error al eliminar", shortError(ex));
                    }
                }));

        HBox actions = new HBox(8, edit, details, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox b = new VBox(9,
                title(icon("Clientes") + "  " + cliente.getNombres(), 16),
                muted(textOrEmpty(cliente.getTelefono()).isBlank() ? "Sin telefono" : cliente.getTelefono()),
                muted("Saldo " + money(cliente.getSaldoFiado())),
                actions
        );
        b.getStyleClass().add("card");
        b.setPrefSize(340, 165);
        return b;
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
        if (!isBulkProduct(product)) return "Ej: 1, 2 o 3";
        if ("KG".equals(unit) || "KILO".equals(unit)) return "Ej: 500 g o 2 kg";
        return "Ej: 1 unidad";
    }

    private boolean validateQuantityForProduct(Producto product, BigDecimal normalized) {
        if (isBulkProduct(product)) {
            return true;
        }
        if (normalized.stripTrailingZeros().scale() > 0) {
            notify("Cantidad invalida", "Este producto solo permite numeros enteros: 1, 2, 3...");
            return false;
        }
        return true;
    }

    private boolean isBulkProduct(Producto product) {
        return product != null && "GRANEL".equalsIgnoreCase(textOrEmpty(product.getTipoVenta()));
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

    private int interestWeeks(int overdueDays) {
        if (overdueDays <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(overdueDays / 7.0));
    }

    private String units(BigDecimal value) {
        return valueOrZero(value).stripTrailingZeros().toPlainString();
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
        Button details = button("Detalles", "soft");
        details.setOnAction(e -> notify("Detalles", "Selecciona un producto guardado para ver su ficha."));
        Button delete = button("Eliminar", "gray");
        delete.setOnAction(e -> notify("Aviso", "Selecciona un producto guardado para desactivarlo."));
        HBox actions = new HBox(8, edit, details, delete);
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
        Label interest = label(debt.detail, debt.interest.compareTo(BigDecimal.ZERO) > 0 ? "notice-small" : "muted");
        interest.setWrapText(true);
        Label total = debt.interest.compareTo(BigDecimal.ZERO) > 0
                ? label("Total actualizado: " + debt.amount, "notice-small")
                : muted("Total: " + debt.amount);
        total.setWrapText(true);
        VBox b = new VBox(10, title(icon("Fiados") + "  " + debt.name, 17), muted(debt.date), title(debt.amount, 18), interest, total, paymentHistoryBox(debt.payments), action);
        b.getStyleClass().add(debt.pending ? "debt-card" : "paid-card");
        b.setPrefSize(300, debt.payments.isEmpty() ? 235 : 315);
        return b;
    }

    private VBox paymentHistoryBox(List<PagoCredito> payments) {
        VBox history = new VBox(7, title("Historial de abonos", 15));
        history.getStyleClass().add("payment-history");
        if (payments == null || payments.isEmpty()) {
            Label empty = muted("Todavia no hay abonos registrados.");
            empty.setWrapText(true);
            history.getChildren().add(empty);
            return history;
        }
        for (PagoCredito payment : payments) {
            history.getChildren().add(paymentHistoryRow(payment));
        }
        return history;
    }

    private HBox paymentHistoryRow(PagoCredito payment) {
        Label date = label(formatPaymentDate(payment.getFechaPago()), "payment-date");
        Label amount = label(money(payment.getMontoPagado()), "payment-amount");
        HBox row = new HBox(8, date, grow(), amount);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("payment-row");
        return row;
    }

    private String formatPaymentDate(LocalDateTime date) {
        return date == null ? "Sin fecha" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    private HBox row(String left, String right) {
        HBox r = new HBox(10, new Label(left), grow(), new Label(right));
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("list-row");
        return r;
    }

    private HBox summaryRow(String left, Label value) {
        value.setWrapText(true);
        HBox r = new HBox(10, new Label(left), grow(), value);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getStyleClass().add("list-row");
        return r;
    }

    private Label supplyEmptyLabel() {
        Label empty = label("Todavia no hay productos en esta compra. Agrega productos y se sumaran al inventario.", "supply-empty");
        empty.setWrapText(true);
        return empty;
    }

    private VBox supplyPurchaseCard(String origin, String product, String quantity, BigDecimal unitCost, BigDecimal salePrice, BigDecimal lineCost, BigDecimal lineSaleValue) {
        Label name = label(product, "supply-item-name");
        name.setWrapText(true);
        VBox details = new VBox(6,
                supplyDetail("Origen", origin),
                supplyDetail("Cantidad recibida", quantity),
                supplyDetail("Costo unitario", money(unitCost)),
                supplyDetail("Precio de venta", money(salePrice)),
                supplyDetail("Costo total", money(lineCost)),
                supplyDetail("Valor para venta", money(lineSaleValue))
        );
        VBox card = new VBox(10, name, details);
        card.getStyleClass().add("supply-purchase-card");
        return card;
    }

    private Label supplyDetail(String left, String right) {
        Label detail = label(left + ": " + right, "supply-detail-line");
        detail.setWrapText(true);
        detail.setMaxWidth(Double.MAX_VALUE);
        return detail;
    }



    private void configureSupplySources(ComboBox<AgendaEntry> sourceSelect) {
        sourceSelect.getItems().clear();
        sourceSelect.getItems().addAll(loadAgendaEntries("PROVEEDOR"));
        if (!sourceSelect.getItems().isEmpty()) {
            sourceSelect.setValue(sourceSelect.getItems().get(0));
        }
        sourceSelect.setMaxWidth(Double.MAX_VALUE);
    }

    private void configureMarketSources(ComboBox<AgendaEntry> sourceSelect) {
        sourceSelect.getItems().clear();
        sourceSelect.getItems().addAll(loadAgendaEntries("MERCADO"));
        if (!sourceSelect.getItems().isEmpty()) {
            sourceSelect.setValue(sourceSelect.getItems().get(0));
        }
        sourceSelect.setMaxWidth(Double.MAX_VALUE);
    }
    private boolean isMarketSupply(String type) {
        return "MERCADO".equalsIgnoreCase(emptyToDefault(type, "PROVEEDOR"));
    }


    private boolean isMarketProduct(Producto product) {
        if (product == null) return false;
        String type = textOrEmpty(product.getTipoVenta()).toUpperCase();
        String unit = normalizeUnit(product.getUnidadBase());
        return "GRANEL".equals(type) || "KG".equals(unit) || "G".equals(unit);
    }

    private String supplyOrigin(String supplier, String type) {
        String name = textOrEmpty(supplier).isBlank() ? "Sin proveedor" : supplier.trim();
        return name + " / " + emptyToDefault(type, "PROVEEDOR");
    }

    private void ensureAgendaTables() {
        try {
            DatabaseConfig.jdbcTemplate().execute("""
                    CREATE TABLE IF NOT EXISTS agenda_proveedores (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        nombre VARCHAR(150) NOT NULL,
                        telefono VARCHAR(80),
                        detalle VARCHAR(250),
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            DatabaseConfig.jdbcTemplate().execute("""
                    CREATE TABLE IF NOT EXISTS agenda_mercado (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        puesto VARCHAR(150) NOT NULL,
                        vende VARCHAR(180),
                        nota VARCHAR(250),
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            seedSupplierAgenda("Pepsico", "", "Gaseosas, galletas y snacks");
            seedSupplierAgenda("Coca Cola", "", "Gaseosas y bebidas");
            seedSupplierAgenda("Backus", "", "Bebidas");
            seedSupplierAgenda("Gloria", "", "Lacteos");
            seedSupplierAgenda("Alicorp", "", "Abarrotes");
        } catch (RuntimeException ex) {
            notify("Agenda", shortError(ex));
        }
    }

    private void seedSupplierAgenda(String name, String phone, String detail) {
        Integer exists = DatabaseConfig.jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM agenda_proveedores WHERE LOWER(nombre) = LOWER(?)",
                Integer.class,
                name
        );
        if (exists == null || exists == 0) {
            saveSupplierAgenda(name, phone, detail);
        }
    }

    private void saveSupplierAgenda(String name, String phone, String detail) {
        DatabaseConfig.jdbcTemplate().update(
                "INSERT INTO agenda_proveedores(nombre, telefono, detalle, activo) VALUES (?, ?, ?, TRUE)",
                name.trim(), textOrEmpty(phone).trim(), textOrEmpty(detail).trim()
        );
    }

    private void saveMarketAgenda(String puesto, String vende, String nota) {
        DatabaseConfig.jdbcTemplate().update(
                "INSERT INTO agenda_mercado(puesto, vende, nota, activo) VALUES (?, ?, ?, TRUE)",
                puesto.trim(), textOrEmpty(vende).trim(), textOrEmpty(nota).trim()
        );
    }

    private void deleteAgendaEntry(long id, String type) {
        if ("MERCADO".equals(type)) {
            DatabaseConfig.jdbcTemplate().update("UPDATE agenda_mercado SET activo = FALSE WHERE id = ?", id);
        } else {
            DatabaseConfig.jdbcTemplate().update("UPDATE agenda_proveedores SET activo = FALSE WHERE id = ?", id);
        }
    }

    private List<AgendaEntry> loadAgendaEntries(String type) {
        ensureAgendaTablesQuietly();
        if ("MERCADO".equals(type)) {
            return DatabaseConfig.jdbcTemplate().query(
                    "SELECT id, puesto, COALESCE(vende, ''), COALESCE(nota, '') FROM agenda_mercado WHERE activo = TRUE ORDER BY puesto",
                    (rs, row) -> new AgendaEntry(rs.getLong(1), rs.getString(2), "", marketDetail(rs.getString(3), rs.getString(4)))
            );
        }
        return DatabaseConfig.jdbcTemplate().query(
                "SELECT id, nombre, COALESCE(telefono, ''), COALESCE(detalle, '') FROM agenda_proveedores WHERE activo = TRUE ORDER BY nombre",
                (rs, row) -> new AgendaEntry(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4))
        );
    }

    private void ensureAgendaTablesQuietly() {
        try {
            DatabaseConfig.jdbcTemplate().execute("""
                    CREATE TABLE IF NOT EXISTS agenda_proveedores (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        nombre VARCHAR(150) NOT NULL,
                        telefono VARCHAR(80),
                        detalle VARCHAR(250),
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            DatabaseConfig.jdbcTemplate().execute("""
                    CREATE TABLE IF NOT EXISTS agenda_mercado (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        puesto VARCHAR(150) NOT NULL,
                        vende VARCHAR(180),
                        nota VARCHAR(250),
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        } catch (RuntimeException ignored) {
        }
    }

    private String marketDetail(String sells, String note) {
        String left = textOrEmpty(sells).isBlank() ? "Sin detalle" : sells.trim();
        String right = textOrEmpty(note).isBlank() ? "" : " | " + note.trim();
        return left + right;
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

    private BarChart<String, Number> chart() {
        return chart(safeMasVendidos(), "Ventas por producto");
    }

    private BarChart<String, Number> chart(List<ProductoVendidoReporte> products, String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Producto");
        xAxis.setTickLabelRotation(-18);
        yAxis.setLabel("Total vendido (S/)");

        BarChart<String, Number> c = new BarChart<>(xAxis, yAxis);
        c.setTitle(title);
        c.setLegendVisible(false);
        c.setAnimated(false);
        c.setPrefHeight(300);
        c.getStyleClass().add("chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ProductoVendidoReporte product : products) {
            series.getData().add(new XYChart.Data<>(
                    textOrEmpty(product.getNombre()),
                    valueOrZero(product.getTotalVendido())
            ));
        }
        c.getData().add(series);
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

    private void confirmDelete(String title, String message, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ButtonType yes = new ButtonType("Si", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yes, no);
        alert.showAndWait().ifPresent(choice -> {
            if (choice == yes) {
                onConfirm.run();
            }
        });
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

    private DatePicker datePicker(LocalDate value) {
        DatePicker picker = new DatePicker(value);
        picker.setPromptText("Seleccionar fecha");
        picker.getStyleClass().add("input");
        picker.setMaxWidth(Double.MAX_VALUE);
        return picker;
    }

    private ComboBox<String> select(String selected, String... values) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("input");
        combo.getItems().addAll(values);
        combo.setMaxWidth(Double.MAX_VALUE);
        if (!combo.getItems().isEmpty()) {
            combo.setValue(selected == null ? combo.getItems().get(0) : selected);
        }
        return combo;
    }

    private ComboBox<CategoriaChoice> supplyCategorySelect() {
        ComboBox<CategoriaChoice> combo = new ComboBox<>();
        combo.getStyleClass().add("input");
        combo.getItems().add(new CategoriaChoice(null, "Todas las categorias"));
        for (Categoria categoria : safeCategorias()) {
            combo.getItems().add(new CategoriaChoice(categoria.getIdCategoria(), categoria.getNombre()));
        }
        combo.setValue(combo.getItems().get(0));
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private void populateSupplyProducts(ComboBox<ProductChoice> productSelect, Long categoryId, String supplyType) {
        productSelect.getItems().clear();
        boolean market = isMarketSupply(supplyType);
        for (Producto producto : safeProductos(null)) {
            if (categoryId == null || categoryId.equals(producto.getIdCategoria())) {
                if (!market || isMarketProduct(producto)) {
                    productSelect.getItems().add(new ProductChoice(producto));
                }
            }
        }
        if (!productSelect.getItems().isEmpty()) {
            productSelect.setValue(productSelect.getItems().get(0));
        } else {
            productSelect.setValue(null);
        }
        productSelect.setMaxWidth(Double.MAX_VALUE);
    }

    private void configureSupplyUnits(ComboBox<String> unitSelect, Producto product) {
        unitSelect.getItems().clear();
        String base = product == null ? "UNIDAD" : normalizeUnit(product.getUnidadBase());
        if ("KG".equals(base)) {
            unitSelect.getItems().addAll("KG", "G");
        } else if ("G".equals(base)) {
            unitSelect.getItems().addAll("G", "KG");
        } else {
            unitSelect.getItems().add(base);
        }
        unitSelect.setValue(unitSelect.getItems().get(0));
        unitSelect.setMaxWidth(Double.MAX_VALUE);
    }

    private ComboBox<CategoriaChoice> categorySelect(Long selectedId) {
        ComboBox<CategoriaChoice> combo = new ComboBox<>();
        combo.getStyleClass().add("input");
        combo.getItems().add(new CategoriaChoice(null, "Sin categoria"));
        for (Categoria categoria : safeCategorias()) {
            combo.getItems().add(new CategoriaChoice(categoria.getIdCategoria(), categoria.getNombre()));
        }
        combo.setValue(combo.getItems().get(0));
        if (selectedId != null) {
            for (CategoriaChoice choice : combo.getItems()) {
                if (selectedId.equals(choice.idCategoria)) {
                    combo.setValue(choice);
                    break;
                }
            }
        }
        return combo;
    }

    private ComboBox<SellModeChoice> sellModeSelect(String selectedUnit) {
        ComboBox<SellModeChoice> combo = new ComboBox<>();
        combo.getStyleClass().add("input");
        combo.getItems().addAll(
                new SellModeChoice("Unidad (botella, lata, bolsa)", "UNIDAD", "UNIDAD"),
                new SellModeChoice("Kilo (kg)", "GRANEL", "KG"),
                new SellModeChoice("Gramo (g)", "GRANEL", "G"),
                new SellModeChoice("Paquete", "PAQUETE", "PAQUETE"),
                new SellModeChoice("Caja", "PAQUETE", "CAJA")
        );
        combo.setValue(combo.getItems().get(0));
        String unit = emptyToDefault(selectedUnit, "UNIDAD").toUpperCase();
        for (SellModeChoice choice : combo.getItems()) {
            if (choice.unidadBase.equals(unit)) {
                combo.setValue(choice);
                break;
            }
        }
        return combo;
    }

    private Long selectedCategoryId(ComboBox<CategoriaChoice> combo) {
        CategoriaChoice selected = combo.getValue();
        return selected == null ? null : selected.idCategoria;
    }

    private SellModeChoice selectedSellMode(ComboBox<SellModeChoice> combo) {
        SellModeChoice selected = combo.getValue();
        return selected == null ? combo.getItems().get(0) : selected;
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

    private void showModal(Stage dialog, String title, VBox content, double preferredWidth, double preferredHeight) {
        Rectangle2D bounds = ownerScreenBounds();
        double width = Math.max(360, Math.min(preferredWidth, bounds.getWidth() - 80));
        double height = Math.max(300, Math.min(preferredHeight, bounds.getHeight() - 80));

        content.setMaxWidth(Double.MAX_VALUE);
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("modal-scroll");
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        dialog.setTitle(title);
        dialog.setMinWidth(Math.min(360, width));
        dialog.setMinHeight(Math.min(300, height));
        dialog.setResizable(true);
        dialog.setScene(scene(scroll, width, height));
        dialog.setOnShown(e -> keepInsideScreen(dialog, bounds));
        dialog.showAndWait();
    }

    private Rectangle2D ownerScreenBounds() {
        if (mainStage != null) {
            List<Screen> screens = Screen.getScreensForRectangle(mainStage.getX(), mainStage.getY(), mainStage.getWidth(), mainStage.getHeight());
            if (!screens.isEmpty()) {
                return screens.get(0).getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private void keepInsideScreen(Stage dialog, Rectangle2D bounds) {
        if (dialog.getX() < bounds.getMinX()) {
            dialog.setX(bounds.getMinX() + 20);
        }
        if (dialog.getY() < bounds.getMinY()) {
            dialog.setY(bounds.getMinY() + 20);
        }
        if (dialog.getX() + dialog.getWidth() > bounds.getMaxX()) {
            dialog.setX(bounds.getMaxX() - dialog.getWidth() - 20);
        }
        if (dialog.getY() + dialog.getHeight() > bounds.getMaxY()) {
            dialog.setY(bounds.getMaxY() - dialog.getHeight() - 20);
        }
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
            case "Agenda" -> "\u260E";
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


    private static class AgendaEntry {
        private final long id;
        private final String name;
        private final String phone;
        private final String detail;

        private AgendaEntry(long id, String name, String phone, String detail) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.phone = phone == null ? "" : phone;
            this.detail = detail == null ? "" : detail;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    private static class Debt {
        private final Long idCredito;
        private final String name;
        private String date;
        private final String amount;
        private final BigDecimal amountValue;
        private final BigDecimal baseAmount;
        private final BigDecimal interest;
        private final int overdueDays;
        private final int interestWeeks;
        private final String detail;
        private final List<PagoCredito> payments;
        private boolean pending;

        private Debt(String name, String date, String amount, boolean pending) {
            this(null, name, date, amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, "", pending, new ArrayList<>());
        }

        private Debt(Long idCredito, String name, String date, String amount, BigDecimal amountValue, BigDecimal baseAmount, BigDecimal interest, int overdueDays, int interestWeeks, String detail, boolean pending, List<PagoCredito> payments) {
            this.idCredito = idCredito;
            this.name = name;
            this.date = date;
            this.amount = amount;
            this.amountValue = amountValue;
            this.baseAmount = baseAmount == null ? BigDecimal.ZERO : baseAmount;
            this.interest = interest == null ? BigDecimal.ZERO : interest;
            this.overdueDays = overdueDays;
            this.interestWeeks = interestWeeks;
            this.detail = detail == null ? "" : detail;
            this.payments = payments == null ? new ArrayList<>() : payments;
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

    private static class CategoriaChoice {
        private final Long idCategoria;
        private final String nombre;

        private CategoriaChoice(Long idCategoria, String nombre) {
            this.idCategoria = idCategoria;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static class SellModeChoice {
        private final String label;
        private final String tipoVenta;
        private final String unidadBase;

        private SellModeChoice(String label, String tipoVenta, String unidadBase) {
            this.label = label;
            this.tipoVenta = tipoVenta;
            this.unidadBase = unidadBase;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class CartLine {
        private final Producto product;
        private BigDecimal quantity;
        private String unit;
        private String quantityText;
        private final BigDecimal unitPrice;
        private BigDecimal subtotal;

        private CartLine(Producto product, BigDecimal quantity, String unit, String quantityText, BigDecimal unitPrice) {
            this.product = product;
            this.quantity = quantity;
            this.unit = unit;
            this.quantityText = quantityText == null || quantityText.isBlank() ? quantity.stripTrailingZeros().toPlainString() + " " + unit : quantityText;
            this.unitPrice = unitPrice;
            this.subtotal = new QuantityParts(quantity, unit).normalizedFor(product).multiply(unitPrice);
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


