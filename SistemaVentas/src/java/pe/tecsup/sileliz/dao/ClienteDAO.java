package pe.tecsup.sileliz.dao;

import pe.tecsup.sileliz.model.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    // Configura los parámetros de conexión a tu base de datos local
    private String url = "jdbc:postgresql://localhost:5432/sistema_ventas"; 
    private String usuario = "postgres";
    private String password = "tu_password";

    private Connection conectar() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    // 1. Listar todos los clientes activos
    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE estado = 1";
        
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Cliente c = new Cliente(
                    rs.getInt("id"),
                    rs.getString("dni"),
                    rs.getString("nombres"),
                    rs.getString("telefono"),
                    rs.getString("correo"),
                    rs.getInt("estado")
                );
                lista.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // 2. Buscar por DNI (Vital para el módulo de ventas)[cite: 5]
    public Cliente buscarPorDni(String dniBuscado) {
        Cliente c = null;
        String sql = "SELECT * FROM clientes WHERE dni = ? AND estado = 1";
        
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, dniBuscado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c = new Cliente(
                        rs.getInt("id"),
                        rs.getString("dni"),
                        rs.getString("nombres"),
                        rs.getString("telefono"),
                        rs.getString("correo"),
                        rs.getInt("estado")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return c;
    }

    // 3. Insertar Cliente
    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO clientes (dni, nombres, telefono, correo, estado) VALUES (?, ?, ?, ?, 1)";
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, cliente.getDni());
            ps.setString(2, cliente.getNombres());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getCorreo());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}