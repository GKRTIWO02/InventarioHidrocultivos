package hidro.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/** Muestra el stock como JProgressBar; el valor de la celda es un porcentaje 0-100. */
public class StockProgressRenderer implements TableCellRenderer {
    private static final Color VERDE = new Color(0, 100, 0);
    private static final Color AMARILLO = new Color(255, 215, 0);
    private static final Color ROJO = new Color(200, 0, 0);

    private final JProgressBar barra = new JProgressBar(0, 100);

    public StockProgressRenderer() {
        barra.setStringPainted(true);
        barra.setBorderPainted(false);
    }

    /** 100% verde oscuro, 50% amarillo, 0% rojo, interpolando linealmente. */
    public static Color colorPara(int pct) {
        int p = Math.max(0, Math.min(100, pct));
        return p >= 50 ? mezclar(AMARILLO, VERDE, (p - 50) / 50.0) : mezclar(ROJO, AMARILLO, p / 50.0);
    }

    private static Color mezclar(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                         (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                         (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        int pct = value instanceof Number ? (int) Math.round(((Number) value).doubleValue()) : 0;
        pct = Math.max(0, Math.min(100, pct));
        barra.setValue(pct);
        barra.setString(pct + "%");
        barra.setForeground(colorPara(pct));
        return barra;
    }
}
