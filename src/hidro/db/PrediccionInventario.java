package hidro.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PrediccionInventario {

    /** Consumo diario promedio (SALIDAS) de las ultimas {@code semanas} semanas. */
    public double consumoDiarioPromedio(int insumoId, int semanas) throws SQLException {
        if (semanas <= 0) throw new IllegalArgumentException("semanas debe ser > 0");
        String sql = "SELECT COALESCE(SUM(cantidad),0) FROM movimiento "
                   + "WHERE insumo_id = ? AND tipo = 'SALIDA' "
                   + "AND fecha >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, insumoId);
            ps.setInt(2, semanas * 7);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1) / (semanas * 7.0);
            }
        }
    }

    public double stockActual(int insumoId) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT stock FROM insumo WHERE id = ?")) {
            ps.setInt(1, insumoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Insumo inexistente: " + insumoId);
                return rs.getDouble(1);
            }
        }
    }

    public String proyectar(int insumoId, int semanas) throws SQLException {
        return mensaje(stockActual(insumoId), consumoDiarioPromedio(insumoId, semanas), LocalDate.now());
    }

    /** Dias restantes = stock / consumo diario promedio. */
    public static String mensaje(double stock, double consumoDiario, LocalDate hoy) {
        if (stock <= 0) return "Sin stock: reabastecer de inmediato";
        if (consumoDiario <= 0) return "Sin consumo registrado en el periodo: no se puede estimar agotamiento";
        long dias = (long) Math.floor(stock / consumoDiario);
        return "Suministro actual suficiente para " + dias + " dias (agotamiento estimado "
                + hoy.plusDays(dias) + ")";
    }
}
