async function iniciarSesion() {
  try {
    const correo = document.getElementById("usuario").value.trim();
    const contrasena = document.getElementById("contrasena").value.trim();

    if (!correo || !contrasena) {
      alert("Por favor completa usuario y contraseña.");
      return;
    }

    // URL correcta del endpoint de AuthController
    const url = "/api/auth/login";

    const resp = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "same-origin", // IMPORTANTE: enviar cookies de sesión al servidor
      body: JSON.stringify({ email: correo, password: contrasena })
    });

    // Intentamos parsear JSON; si falla, intentamos leer texto para mostrar mensaje
    let data = null;
    try {
      data = await resp.json();
    } catch (err) {
      // no JSON
      const txt = await resp.text().catch(() => null);
      data = txt ? { mensaje: txt } : null;
    }

    // Si el servidor respondió 2xx y el objeto indica success
    if (resp.ok && data && data.success) {
      // Guardar información del usuario en localStorage usando datos del rol correcto
      const usuarioObj = {
        correo: correo,
        rol: data.rol || '',
        // Preferir datos de estudiante si existen, si no usar datos de profesor
        apellidosNombres: (data.estudiante ? data.estudiante.apellidosNombres : (data.profesor ? data.profesor.apellidosNombres : null)),
        numeroMatricula: data.estudiante ? data.estudiante.numeroMatricula : null,
        idUsuario: (data.estudiante ? data.estudiante.idUsuario : (data.profesor ? data.profesor.idUsuario : null)),
        cui: data.estudiante ? data.estudiante.cui : null
      };
      localStorage.setItem('usuarioDatos', JSON.stringify(usuarioObj));

      if (Array.isArray(data.horarios)) {
        localStorage.setItem('horarios', JSON.stringify(data.horarios));
      } else {
        localStorage.removeItem('horarios');
      }

      // Redirecciones (usar rutas que existen en templates/)
      const rol = (data.rol || "").toLowerCase();
      let destino = "/est/estudiante_dashboard.html"; // página por defecto

      if (rol.includes("docente") || rol === "docente")
        destino = "/doc/nuevo_profesor.html"; // ajusta si necesitas otro archivo
      else if (rol.includes("admin") || rol === "administrador")
        destino = "/admin/admin.html";
      else if (rol.includes("secretaria"))
        destino = "/secr/secretaria.html";
      else if (rol.includes("estudiante"))
        destino = "/est/estudiante_dashboard.html";

      window.location.href = destino;
      return;
    }

    // Si llegamos aquí: respuesta no OK o success=false
    // Intentar mostrar mensaje que venga del servidor
    const mensaje = (data && data.mensaje) ? data.mensaje :
                    (resp.status === 401 ? "Credenciales incorrectas." : `Error: ${resp.status} ${resp.statusText}`);
    alert("⚠️ " + mensaje);

  } catch (err) {
    console.error("Error en iniciarSesion:", err);
    alert("❌ Error al contactar el servidor");
  }
}
