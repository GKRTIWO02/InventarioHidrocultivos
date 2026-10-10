package hidro.ui;

import hidro.db.Conexion;
import hidro.db.PrediccionInventario;
import hidro.db.ProduccionDAO;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class VentanaPrincipal extends JFrame {
    private static final int PRODUCTO_TE_COMPOST = 1;
    private static final int SEMANAS = 4;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] {"ID", "Insumo", "Unidad", "Stock actual", "Stock"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public VentanaPrincipal() {
        super("Inventario de Hidrocultivos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new StockProgressRenderer());

        JButton producir = new JButton("Producir");
        JButton proyectar = new JButton("Proyectar agotamiento");
        producir.addActionListener(e -> producir());
        proyectar.addActionListener(e -> proyectar());
        JPanel botones = new JPanel();
        botones.add(producir);
        botones.add(proyectar);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        setSize(700, 350);
        setLocationRelativeTo(null);
        cargar();
    }

    private void cargar() {
        modelo.setRowCount(0);
        try (Connection con = Conexion.obtener(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT id, nombre, unidad, stock, stock_maximo FROM insumo ORDER BY id")) {
            while (rs.next()) {
                double max = rs.getDouble("stock_maximo");
                double pct = max > 0 ? rs.getDouble("stock") * 100.0 / max : 0;
                modelo.addRow(new Object[] {rs.getInt("id"), rs.getString("nombre"),
                    rs.getString("unidad"), rs.getBigDecimal("stock"), pct});
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void producir() {
        String s = JOptionPane.showInputDialog(this, "Cantidad de Te de Compost (L) a producir:", "10");
        if (s == null) return;
        try {
            new ProduccionDAO().registrarProduccion(PRODUCTO_TE_COMPOST, new BigDecimal(s.trim()));
            JOptionPane.showMessageDialog(this, "Produccion registrada");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad invalida");
        } catch (SQLException ex) {
            error(ex);
        }
        cargar();
    }

    private void proyectar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un insumo");
            return;
        }
        int id = (Integer) modelo.getValueAt(fila, 0);
        try {
            JOptionPane.showMessageDialog(this, modelo.getValueAt(fila, 1) + ": "
                    + new PrediccionInventario().proyectar(id, SEMANAS));
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void error(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
