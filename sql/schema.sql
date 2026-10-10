CREATE DATABASE IF NOT EXISTS hidrocultivos CHARACTER SET utf8mb4;
USE hidrocultivos;

DROP PROCEDURE IF EXISTS sp_registrar_produccion;
DROP TABLE IF EXISTS movimiento;
DROP TABLE IF EXISTS produccion;
DROP TABLE IF EXISTS receta;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS insumo;

CREATE TABLE insumo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  unidad VARCHAR(20) NOT NULL,
  stock DECIMAL(12,3) NOT NULL DEFAULT 0 CHECK (stock >= 0),
  stock_maximo DECIMAL(12,3) NOT NULL DEFAULT 100
) ENGINE=InnoDB;

CREATE TABLE producto (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  unidad VARCHAR(20) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE receta (
  producto_id INT NOT NULL,
  insumo_id INT NOT NULL,
  cantidad DECIMAL(12,3) NOT NULL,   -- insumo por unidad de producto
  PRIMARY KEY (producto_id, insumo_id),
  FOREIGN KEY (producto_id) REFERENCES producto(id),
  FOREIGN KEY (insumo_id) REFERENCES insumo(id)
) ENGINE=InnoDB;

CREATE TABLE produccion (
  id INT AUTO_INCREMENT PRIMARY KEY,
  producto_id INT NOT NULL,
  cantidad DECIMAL(12,3) NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB;

CREATE TABLE movimiento (
  id INT AUTO_INCREMENT PRIMARY KEY,
  insumo_id INT NOT NULL,
  tipo ENUM('ENTRADA','SALIDA') NOT NULL,
  cantidad DECIMAL(12,3) NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  produccion_id INT NULL,
  FOREIGN KEY (insumo_id) REFERENCES insumo(id),
  FOREIGN KEY (produccion_id) REFERENCES produccion(id)
) ENGINE=InnoDB;

INSERT INTO insumo (nombre, unidad, stock, stock_maximo) VALUES
 ('Agua', 'L', 500, 500),
 ('Humus', 'kg', 60, 100),
 ('Melaza', 'L', 20, 50),
 ('Solucion nutritiva A', 'L', 8, 40);

INSERT INTO producto (nombre, unidad) VALUES ('Te de Compost', 'L');

-- Por cada litro de Te de Compost: 0.9 L agua, 0.1 kg humus, 0.02 L melaza
INSERT INTO receta (producto_id, insumo_id, cantidad) VALUES
 (1, 1, 0.900), (1, 2, 0.100), (1, 3, 0.020);

-- Historial de consumo de ejemplo (ultimas semanas)
INSERT INTO movimiento (insumo_id, tipo, cantidad, fecha)
SELECT i.id, 'SALIDA', c.qty, DATE_SUB(NOW(), INTERVAL c.dias DAY)
FROM insumo i
JOIN (SELECT 2 AS id, 3.0 AS qty, 3 AS dias UNION ALL SELECT 2, 2.5, 10
      UNION ALL SELECT 2, 3.5, 17 UNION ALL SELECT 2, 3.0, 24
      UNION ALL SELECT 3, 1.0, 5 UNION ALL SELECT 3, 1.5, 12
      UNION ALL SELECT 3, 1.0, 20
      UNION ALL SELECT 4, 2.0, 2 UNION ALL SELECT 4, 2.0, 9
      UNION ALL SELECT 4, 2.5, 16 UNION ALL SELECT 4, 1.5, 23
      UNION ALL SELECT 1, 40, 4 UNION ALL SELECT 1, 35, 18) c ON c.id = i.id;

DELIMITER //
CREATE PROCEDURE sp_registrar_produccion(IN p_producto INT, IN p_cantidad DECIMAL(12,3))
BEGIN
  DECLARE v_faltantes INT DEFAULT 0;
  DECLARE v_prod_id INT;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cantidad invalida';
  END IF;

  START TRANSACTION;

  IF NOT EXISTS (SELECT 1 FROM producto WHERE id = p_producto) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Producto inexistente';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM receta WHERE producto_id = p_producto) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Producto sin receta';
  END IF;

  -- Bloquea todas las filas de insumo involucradas
  SELECT COUNT(*) INTO v_faltantes FROM (
    SELECT i.id FROM receta r JOIN insumo i ON i.id = r.insumo_id
    WHERE r.producto_id = p_producto
    ORDER BY i.id FOR UPDATE) bloqueados;

  SELECT COUNT(*) INTO v_faltantes
  FROM receta r JOIN insumo i ON i.id = r.insumo_id
  WHERE r.producto_id = p_producto AND i.stock < r.cantidad * p_cantidad;

  IF v_faltantes > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente de materias primas';
  END IF;

  INSERT INTO produccion (producto_id, cantidad) VALUES (p_producto, p_cantidad);
  SET v_prod_id = LAST_INSERT_ID();

  INSERT INTO movimiento (insumo_id, tipo, cantidad, produccion_id)
  SELECT r.insumo_id, 'SALIDA', r.cantidad * p_cantidad, v_prod_id
  FROM receta r WHERE r.producto_id = p_producto;

  UPDATE insumo i JOIN receta r ON r.insumo_id = i.id
  SET i.stock = i.stock - r.cantidad * p_cantidad
  WHERE r.producto_id = p_producto;

  COMMIT;
END//
DELIMITER ;
