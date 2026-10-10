package hidro.db;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class ProduccionDAO {

    /** Invoca sp_registrar_produccion; lanza SQLException si falta stock (la BD hace ROLLBACK). */
    public void registrarProduccion(int productoId, BigDecimal cantidad) throws SQLException {
        try (Connection con = Conexion.obtener();
             CallableStatement cs = con.prepareCall("{call sp_registrar_produccion(?, ?)}")) {
            cs.setInt(1, productoId);
            cs.setBigDecimal(2, cantidad);
            cs.execute();
        }
    }
}
