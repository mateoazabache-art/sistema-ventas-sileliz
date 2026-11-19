# Guía rápida para escribir un DAO

Molde: `SistemaVentas/src/java/pe/tecsup/sileliz/dao/UsuarioDao.java`.

## Las 5 reglas

1. **Conexión:** siempre `Conexion.getConnection()` dentro de `try (...)`. Nunca URL, usuario ni clave en el código.
2. **SQL seguro:** siempre `PreparedStatement` con `?`. Nunca concatenar lo que escribe el usuario.
3. **Columnas:** se listan las columnas que se necesitan. Nunca `SELECT *`.
4. **Errores:** `catch (SQLException e) { throw ErrorSql.traducir(e); }`. Nunca `printStackTrace()` ni devolver `null`/`false` para esconder el error.
5. **Dos tablas = una transacción:** `setAutoCommit(false)`, `commit()` si todo sale bien, `rollback()` si algo falla.

## Esqueleto de un método

```java
public Optional<Categoria> buscarPorId(int id) {
    String sql = "SELECT id_categoria, nombre, activo FROM categoria WHERE id_categoria = ?";
    try (Connection cn = Conexion.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    } catch (SQLException e) {
        throw ErrorSql.traducir(e);
    }
}
```

## Lista paginada

```java
int t = Paginado.normalizarTam(tam);
// ... "ORDER BY nombre LIMIT ? OFFSET ?"
ps.setInt(n, t);
ps.setInt(n + 1, Paginado.offset(pagina, t));
// ... luego un SELECT count(*) con el MISMO WHERE
return Paginado.de(lista, pagina, t, total);
```

## Persona + cliente (o persona + usuario)

1. `INSERT INTO persona (...) VALUES (...) RETURNING id_persona` → se lee el id.
2. `INSERT INTO cliente (id_persona, observaciones) VALUES (?, ?)` con ese id.
3. Los dos en la misma conexión con `setAutoCommit(false)` y `commit()`. Ver `UsuarioDao.insertar`.

## Desactivar, no borrar (RN-05)

`UPDATE ... SET activo = false WHERE id_... = ?` y devolver `executeUpdate() == 1`.

## Datos para probar (bd/02_datos_prueba.sql)

- Usuarios: `admin / Admin2026` y `vendedor / Venta2026`
- Clientes: DNI `70000001` a `70000010`
- Categorías 1 a 5, productos `P-001` a `P-020`, SKU como `P-001-XL-BLA`
