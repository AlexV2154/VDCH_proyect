package com.example;

import javafx.application.Application;
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

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private BorderPane app;
    private VBox sidebar;
    private Stage mainStage;
    private String current = "Inicio";
    private Debt selectedDebt;

    private final String[] menu = {"Inicio", "Ventas", "Inventario", "Categorias", "Clientes", "Fiados", "Reportes", "Abastecimiento"};
    private final List<Debt> debts = new ArrayList<>(List.of(
            new Debt("Mario Holgado", "Hoy", "S/ 9.00", true),
            new Debt("Juan Perez", "Ayer", "S/ 5.00", true),
            new Debt("Maria Valeria", "Pagado", "S/ 12.00", false)
    ));

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
        ImageView logo = new ImageView(new Image("file:C:/Users/sozha/OneDrive/Documentos/demo/_docx_reference/unzipped/word/media/image14.png", true));
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
                muted("Pantalla clara y sencilla para atender la tienda sin complicaciones."),
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
        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.setVgap(14);
        stats.add(stat(icon("Ventas") + " Ventas hoy", "S/ 35.50", "5 ventas registradas"), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", "S/ 11.75", "Margen del dia"), 1, 0);
        stats.add(stat(icon("Fiados") + " Por cobrar", "S/ 14.00", "2 clientes pendientes"), 2, 0);
        stats.add(stat(icon("Inventario") + " Stock bajo", "2", "Productos por reponer"), 3, 0);

        HBox quick = new HBox(12,
                shortcut("Nueva venta", "Ventas"),
                shortcut("Productos", "Inventario"),
                shortcut("Clientes", "Clientes"),
                shortcut("Cobrar fiado", "Fiados")
        );
        VBox low = panel("Avisos importantes", row("Maracuya", "Queda 1"), row("Coca Cola", "Queda 1"), row("Fiados pendientes", "S/ 14.00"));
        return page("Buen dia", null, stats, quick, chart(), low);
    }

    private VBox ventas() {
        ComboBox<String> customer = new ComboBox<>();
        customer.getItems().addAll("Venta rapida", "Mario Holgado", "Juan Perez", "Maria Valeria", "Cliente nuevo");
        customer.setValue("Venta rapida");
        customer.getStyleClass().add("input");
        Label customerInfo = muted("Sin cliente registrado. Ideal para compras al paso.");
        customer.setOnAction(e -> {
            String value = customer.getValue();
            if ("Venta rapida".equals(value)) {
                customerInfo.setText("Sin cliente registrado. Ideal para compras al paso.");
            } else if ("Cliente nuevo".equals(value)) {
                customerInfo.setText("Cliente nuevo seleccionado. Puedes guardarlo desde Clientes.");
                notify("Cliente nuevo", "Luego puedes registrarlo en el modulo Clientes.");
            } else if ("Mario Holgado".equals(value)) {
                customerInfo.setText("Telefono: 999 999 999 | Saldo fiado: S/ 9.00");
            } else if ("Juan Perez".equals(value)) {
                customerInfo.setText("Telefono: 987 654 321 | Saldo fiado: S/ 5.00");
            } else {
                customerInfo.setText("Telefono: 912 456 789 | Sin deuda pendiente");
            }
        });
        Button newCustomer = button(icon("add") + " Nuevo cliente", "soft big-button");
        newCustomer.setOnAction(e -> modal("Nuevo cliente", List.of("Nombre", "Telefono", "Notas")));
        VBox customerBox = new VBox(10,
                title("Cliente que compra", 18),
                field("Seleccionar cliente", customer),
                customerInfo,
                newCustomer
        );
        customerBox.getStyleClass().add("customer-box");

        VBox cartItems = new VBox(8,
                row("Coca Cola", "1 unidad  S/ 3.00"),
                row("Arroz Costeno", "500 g  S/ 2.10")
        );
        Label total = title("Total: S/ 5.10", 20);

        HBox products = new HBox(12,
                product("Arroz Costeno", "S/ 4.20 x kg", "Granel: kilo o gramos", "Ej: 500 g o 2 kg", cartItems, total),
                product("Coca Cola", "S/ 3.00", "Unidad: botella 500 ml", "Ej: 1 unidad o 6 unidades", cartItems, total),
                product("Maracuya", "S/ 7.50 x kg", "Granel: kilo o gramos", "Ej: 250 g o 1 kg", cartItems, total),
                product("Aceite Primor", "S/ 8.00", "Unidad: botella", "Ej: 1 unidad", cartItems, total)
        );

        Button confirm = button(icon("ok") + "  Confirmar venta", "primary big-button");
        confirm.setOnAction(e -> notify("Venta registrada", "Se guardo la venta por S/ 5.50."));
        Button charge = button(icon("money") + "  Cobrar ahora", "dark big-button");
        charge.setOnAction(e -> notify("Cobro listo", "Puedes entregar el comprobante al cliente."));
        Button debt = button(icon("Fiados") + "  Guardar como fiado", "soft big-button");
        debt.setOnAction(e -> {
            debts.add(new Debt("Cliente nuevo", "Hoy", "S/ 5.50", true));
            notify("Fiado agregado", "La venta paso a la lista de Fiados.");
            go("Fiados");
        });

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
        VBox list = new VBox(10,
                item("Arroz Costeno", "Categoria: granel | Venta por kilo, gramos o paquete", "S/ 4.20 x kg"),
                item("Coca Cola", "Bebida 500 ml", "S/ 3.00"),
                item("Maracuya", "Categoria: granel | Venta por kilo o gramos", "S/ 7.50 x kg")
        );
        return page("Inventario", add, search("Buscar producto..."), list);
    }

    private VBox categorias() {
        Button add = button(icon("add") + " Nueva categoria", "dark big-button");
        add.setOnAction(e -> modal("Nueva categoria", List.of("Nombre")));
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.add(category("Abarrotes", "3 productos"), 0, 0);
        grid.add(category("Bebidas", "2 productos"), 1, 0);
        grid.add(category("Higiene personal", "2 productos"), 2, 0);
        grid.add(category("Snack y Golosinas", "1 producto"), 0, 1);
        return page("Categorias", add, search("Buscar categoria..."), grid);
    }

    private VBox clientes() {
        Button add = button(icon("add") + " Nuevo cliente", "dark big-button");
        add.setOnAction(e -> modal("Nuevo cliente", List.of("Nombre", "Telefono", "Notas")));
        HBox cards = new HBox(12,
                client("Mario Holgado", "999 999 999", "S/ 9.00"),
                client("Juan Perez", "987 654 321", "S/ 5.00"),
                client("Maria Valeria", "912 456 789", "S/ 0.00")
        );
        return page("Clientes", add, search("Buscar cliente..."), cards);
    }

    private VBox fiados() {
        Label hint = label("Toca Registrar pago para cobrar directamente desde aqui.", "notice");
        HBox cards = new HBox(14);
        for (Debt debt : debts) cards.getChildren().add(debtCard(debt));
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
            selectedDebt.pending = false;
            selectedDebt.date = "Pagado hoy";
            notify("Pago confirmado", selectedDebt.name + " ya figura como pagado.");
            go("Fiados");
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
        GridPane stats = new GridPane();
        stats.setHgap(14);
        stats.add(stat(icon("Ventas") + " Ventas", "S/ 35.50", "Hoy"), 0, 0);
        stats.add(stat(icon("profit") + " Ganancia", "S/ 11.75", "Hoy"), 1, 0);
        stats.add(stat(icon("Fiados") + " Fiados", "S/ 14.00", "Pendiente"), 2, 0);
        stats.add(stat(icon("ok") + " Pagado", "S/ 12.00", "Cobrado"), 3, 0);
        VBox sold = panel("Productos mas vendidos", row("Coca Cola", "S/ 50.00"), row("Maracuya", "S/ 10.00"), row("Arroz Costeno", "S/ 7.50"));
        return page("Reportes", null, stats, chart(), sold);
    }

    private VBox abastecimiento() {
        Button supplierMode = button(icon("Clientes") + " Proveedor guardado", "primary big-button");
        supplierMode.setOnAction(e -> notify("Modo proveedor", "Registra los datos del proveedor que llega a la tienda."));
        Button marketMode = button(icon("store") + " Compra de mercado", "soft big-button");
        marketMode.setOnAction(e -> marketPurchaseModal());
        Button saveSupplier = button(icon("ok") + " Guardar proveedor", "dark big-button");
        saveSupplier.setOnAction(e -> notify("Proveedor guardado", "El proveedor queda listo para futuros abastecimientos."));

        VBox supplier = new VBox(12,
                title("Datos del ingreso", 18),
                new HBox(10, supplierMode, marketMode),
                field("Proveedor o lugar de compra", input("Ej: Don Luis / Mercado Central")),
                field("Tipo de abastecimiento", input("Proveedor, mercado, distribuidora")),
                field("Telefono o contacto", input("Opcional")),
                field("Comprobante", input("Boleta, factura o sin comprobante")),
                field("Fecha de llegada", input("Hoy")),
                saveSupplier
        );
        supplier.getStyleClass().add("supply-form");

        VBox productForm = new VBox(12,
                title("Producto que deja", 18),
                field("Producto", input("Ej: Arroz Costeno")),
                field("Categoria", input("Granel, bebidas, limpieza...")),
                field("Cantidad recibida", input("Ej: 25 kg, 12 unidades, 6 paquetes")),
                field("Unidad de compra", input("Kilo, gramo, paquete, caja, unidad")),
                field("Costo de compra", input("Ej: S/ 85.00")),
                field("Precio de venta", input("Ej: S/ 4.20 x kg")),
                field("Observaciones", input("Ej: pago pendiente, entrega incompleta"))
        );
        productForm.getStyleClass().add("supply-form");

        Button addLine = button(icon("add") + " Agregar producto recibido", "soft big-button");
        addLine.setOnAction(e -> notify("Producto agregado", "El abastecimiento quedo en la lista."));

        TableView<String[]> table = new TableView<>();
        String[] cols = {"Proveedor", "Producto", "Cantidad", "Unidad", "Costo", "Venta"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(v -> new SimpleStringProperty(v.getValue()[idx]));
            c.setPrefWidth(125);
            table.getColumns().add(c);
        }
        table.getItems().addAll(List.of(
                new String[]{"Don Luis", "Arroz Costeno", "25", "kg", "S/ 85.00", "S/ 4.20 x kg"},
                new String[]{"Mercado Central", "Maracuya", "10", "kg", "S/ 45.00", "S/ 7.50 x kg"}
        ));
        Button save = button(icon("ok") + " Guardar ingreso", "primary big-button");
        save.setOnAction(e -> notify("Abastecimiento guardado", "El inventario fue actualizado."));
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
        modal("Nuevo producto", List.of("Nombre", "Categoria", "Tipo de venta", "Unidad base", "Equivalencia", "Precio por unidad/kg", "Costo", "Stock actual", "Stock minimo"));
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

    private VBox product(String name, String price, String detail, String prompt, VBox cartItems, Label total) {
        Button add = button(icon("add") + " Agregar", "soft");
        add.setOnAction(e -> askQuantity(name, price, prompt, cartItems, total));
        VBox b = new VBox(9, title(name, 16), muted(detail), muted("Stock disponible"), title(price, 15), add);
        b.getStyleClass().add("card");
        b.setPrefSize(170, 135);
        return b;
    }

    private void askQuantity(String name, String price, String prompt, VBox cartItems, Label total) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(mainStage);
        dialog.setTitle("Agregar producto");
        dialog.setHeaderText(name + " - " + price);
        dialog.setContentText("Cantidad:");
        dialog.getEditor().setPromptText(prompt);
        dialog.showAndWait().ifPresent(quantity -> {
            String clean = quantity.trim().isEmpty() ? prompt.replace("Ej: ", "") : quantity.trim();
            cartItems.getChildren().add(row(name, clean + "  " + price));
            total.setText("Total actualizado");
            notify("Producto agregado", name + " agregado: " + clean + ".");
        });
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
        delete.setOnAction(e -> notify("Aviso", name + " no se elimina porque es demo frontend."));
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
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.getData().add(new XYChart.Data<>("Lun", 4));
        s.getData().add(new XYChart.Data<>("Mar", 9));
        s.getData().add(new XYChart.Data<>("Mie", 14));
        s.getData().add(new XYChart.Data<>("Jue", 18));
        s.getData().add(new XYChart.Data<>("Vie", 25));
        c.getData().add(s);
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
        private final String name;
        private String date;
        private final String amount;
        private boolean pending;

        private Debt(String name, String date, String amount, boolean pending) {
            this.name = name;
            this.date = date;
            this.amount = amount;
            this.pending = pending;
        }
    }
}
