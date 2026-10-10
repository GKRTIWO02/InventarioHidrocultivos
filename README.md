# Inventario de Hidrocultivos (Java Swing + MySQL)

Aplicación de escritorio (NetBeans / Java Swing) para control de suministros de hidrocultivos.

## Requisitos
- JDK 8+ 
- XAMPP (MySQL/MariaDB en `localhost:3306`, usuario `root`, sin contraseña)
- MySQL Connector/J (https://dev.mysql.com/downloads/connector/j/)

## 1. XAMPP
1. Abre el Panel de Control de XAMPP y pulsa **Start** en *MySQL*.
2. Abre http://localhost/phpmyadmin.

## 2. Importar el SQL
- En phpMyAdmin: pestaña **Importar** → selecciona `sql/schema.sql` → **Continuar**.
- O por consola: `mysql -u root < sql/schema.sql`
Crea la base `hidrocultivos`, tablas, el procedimiento `sp_registrar_produccion` y datos de ejemplo.

## 3. Driver
Copia `mysql-connector-j-X.Y.Z.jar` a la carpeta `lib/`.
En NetBeans: clic derecho en *Libraries* → *Add JAR/Folder* → selecciona el jar.

## 4. Compilar y ejecutar
```
mkdir -p build
javac -cp "lib/*" -d build $(find src -name "*.java")
java -cp "build:lib/*" hidro.ui.Main        # en Windows usa ; en lugar de :
```
(En NetBeans: Run Project, clase principal `hidro.ui.Main`.)

## Módulos
- `hidro.db.ProduccionDAO`: invoca `sp_registrar_produccion` con `CallableStatement` (transacción atómica, `FOR UPDATE`, ROLLBACK ante fallo).
- `hidro.db.PrediccionInventario`: consumo diario promedio de las últimas N semanas y días restantes.
- `hidro.ui.StockProgressRenderer`: celda de stock como `JProgressBar` (verde oscuro 100% → amarillo 50% → rojo 0%).
- `hidro.ui.VentanaPrincipal`: demo con `JTable` y botones **Producir** y **Proyectar agotamiento**.
