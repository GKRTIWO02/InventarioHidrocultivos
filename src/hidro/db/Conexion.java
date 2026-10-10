package hidro.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Conexion JDBC a MySQL (XAMPP). */
public final class Conexion {
    public static final String URL =
        "jdbc:mysql://localhost:3306/hidrocultivos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String USUARIO = "root";
    public static final String CLAVE = "";

    private Conexion() { }

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
