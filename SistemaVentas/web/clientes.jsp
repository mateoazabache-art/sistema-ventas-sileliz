<!DOCTYPE html>
<html lang="es">
<head>
    <link rel="stylesheet" href="css/estilos.css">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestión de Ventas Sileliz</title>
</head>
<body>
    <header>
        <h1>Sistema de Ventas - Sileliz</h1>
        <nav>
            <ul>
                <li><a href="#">Inicio</a></li>
                <li><a href="#">Ventas</a></li>
                <li><a href="#">Productos</a></li>
            </ul>
        </nav>
    </header>

    <main id="contenido-principal">
    <h2>Gestión de Clientes</h2>
    <p>Módulo para el registro, consulta por DNI y control de clientes del sistema.</p>

    <!-- Formulario de Búsqueda y Registro -->
    <div class="card-acciones" style="margin-bottom: 20px;">
        <form action="clientes.jsp" method="get" style="display: inline-block; margin-right: 20px;">
            <input type="text" name="dniBusqueda" placeholder="Buscar por DNI..." required style="padding: 5px; width: 200px;">
            <button type="submit" style="padding: 6px 12px;">Buscar</button>
        </form>
    </div>

    <!-- Tabla de Listado de Clientes -->
    <table border="1" cellpadding="10" cellspacing="0" style="width: 100%; border-collapse: collapse; text-align: left;">
        <thead>
            <tr style="background-color: #f2f2f2;">
                <th>ID</th>
                <th>DNI</th>
                <th>Nombres y Apellidos</th>
                <th>Teléfono</th>
                <th>Correo</th>
                <th>Estado</th>
            </tr>
        </thead>
        <tbody>
            <!-- Aquí conectaremos dinámicamente con tu ClienteDAO -->
            <tr>
                <td>1</td>
                <td>72819203</td>
                <td>Juan Pérez Gómez</td>
                <td>987654321</td>
                <td>juan.perez@email.com</td>
                <td>Activo</td>
            </tr>
            <tr>
                <td>2</td>
                <td>45678912</td>
                <td>María López Silva</td>
                <td>912345678</td>
                <td>maria.lopez@email.com</td>
                <td>Activo</td>
            </tr>
        </tbody>
    </table>
    </main>

    <footer>
        <p>&copy; 2026 Sistema Ventas - Sileliz</p>
    </footer>

    <!-- Scripts del proyecto -->
    <script src="js/api.js"></script>
    <script src="js/layout.js"></script>
</body>
</html> 