
        let perfilData = {};

        // Abrir pestañas
        function openTab(tabName) {
            document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
            document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
            document.getElementById(tabName).classList.add('active');
            // marcar boton activo (event puede no estar definido si se invoca programáticamente)
            if (event && event.currentTarget) event.currentTarget.classList.add('active');
        }

        // Cargar al iniciar
        document.addEventListener('DOMContentLoaded', function() {
            // Leer el objeto usuarioDatos desde localStorage (según lo indicaste)
            // Ejemplo del localStorage: 
            // {correo: "202501005@instituto.edu.pe", rol: "ESTUDIANTE", apellidosNombres: "Rojas, Martín Andrés", cui: "CUI2025005", idUsuario: 1005, numeroMatricula: 2025005, rol: "ESTUDIANTE"}
            const usuarioLocal = JSON.parse(localStorage.getItem('usuarioDatos') || localStorage.getItem('estudianteActual') || '{}');

            // Si no hay usuario en localStorage, muestra valores por defecto
            const idUsuario = usuarioLocal.idUsuario || usuarioLocal.id || null;

            // Mostrar nombre simple en sidebar si lo tienes en localStorage
            if (usuarioLocal.apellidosNombres) {
                document.getElementById('userName').textContent = usuarioLocal.apellidosNombres;
                document.getElementById('userAvatar').textContent = obtenerIniciales(usuarioLocal.apellidosNombres);
            }

            if (!idUsuario) {
                // no hay id, carga ejemplo
                cargarPerfilEjemplo();
                return;
            }

            // Llamar API para obtener perfil
            fetch(`/api/estudiante/${idUsuario}`)
                .then(resp => {
                    if (!resp.ok) throw new Error('No se pudo obtener el perfil');
                    return resp.json();
                })
                .then(data => {
                    perfilData = data;
                    actualizarInterfazPerfil();
                })
                .catch(err => {
                    console.error('Error al cargar perfil:', err);
                    // fallback a ejemplo
                    cargarPerfilEjemplo();
                });
        });

        function obtenerIniciales(nombreCompleto) {
            if (!nombreCompleto) return 'U';
            const partes = nombreCompleto.trim().split(/\s+/);
            if (partes.length === 1) return partes[0].charAt(0).toUpperCase();
            return (partes[0].charAt(0) + partes[partes.length-1].charAt(0)).toUpperCase();
        }

        function actualizarInterfazPerfil() {
            // defensivo: si perfilData es un objeto plano devuelto por el backend
            document.getElementById('profileName').textContent = perfilData.nombreCompleto || 'Sin nombre';
            document.getElementById('profileCode').innerHTML = `<strong>Código:</strong> ${perfilData.codigo || '-'}`;
            document.getElementById('profileCareer').innerHTML = `<strong>Carrera:</strong> ${perfilData.carrera || 'Ciencia de la Computación'}`;
            document.getElementById('profileSemester').innerHTML = `<strong>Semestre:</strong> ${perfilData.semestre || '-'}`;
            document.getElementById('profileEmail').innerHTML = `<strong>Email:</strong> ${perfilData.email || '-'}`;
            document.getElementById('profileAvatar').textContent = perfilData.iniciales || obtenerIniciales(perfilData.nombreCompleto);

            // Estadísticas
            document.getElementById('statCourses').textContent = perfilData.cursosActivos != null ? perfilData.cursosActivos : 0;
            document.getElementById('statAverage').textContent = perfilData.promedioGeneral != null ? perfilData.promedioGeneral : '-';
            document.getElementById('statAttendance').textContent = perfilData.asistencia != null ? perfilData.asistencia : '-';

            // Formularios
            // separar nombres/apellidos si lo deseas (aquí intento separar por primer espacio)
            const nombreCompleto = perfilData.nombreCompleto || '';
            const partes = nombreCompleto.split(' ');
            const nombres = partes.slice(0, Math.max(1, partes.length-1)).join(' ');
            const apellidos = partes.length > 1 ? partes.slice(-1).join(' ') : '';
            document.getElementById('firstName').value = nombres;
            document.getElementById('lastName').value = apellidos;
            document.getElementById('email').value = perfilData.email || '';
            document.getElementById('studentCode').value = perfilData.codigo || '';
            document.getElementById('career').value = perfilData.carrera || 'Ciencia de la Computación';
            document.getElementById('faculty').value = perfilData.facultad || 'Ingeniería de Producción y Servicios';
            document.getElementById('semester').value = perfilData.semestre || '';
        }

        // ejemplo fallback
        function cargarPerfilEjemplo() {
            perfilData = {
                nombreCompleto: 'Rojas, Martín Andrés',
                codigo: 'CUI2025005',
                carrera: 'Ciencia de la Computación',
                semestre: '5to Semestre',
                email: '202501005@instituto.edu.pe',
                iniciales: 'RM',
                nombres: 'Martín Andrés',
                apellidos: 'Rojas',
                estadisticas: { cursosActivos: 6, promedioGeneral: '15.2', asistencia: '92%' }
            };
            actualizarInterfazPerfil();
        }

        // seguridad: cambiar contraseña (llamar a endpoint backend si tienes uno)
        async function cambiarContrasena() {
            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (!currentPassword || !newPassword || !confirmPassword) {
                mostrarMensaje('Por favor, completa todos los campos', 'error');
                return;
            }
            if (newPassword !== confirmPassword) {
                mostrarMensaje('Las contraseñas nuevas no coinciden', 'error');
                return;
            }
            if (newPassword.length < 8) {
                mostrarMensaje('La contraseña debe tener al menos 8 caracteres', 'error');
                return;
            }

            // Ejemplo: POST a /api/estudiante/{id}/cambiar-contrasena
            const usuarioLocal = JSON.parse(localStorage.getItem('usuarioDatos') || '{}');
            const idUsuario = usuarioLocal.idUsuario || usuarioLocal.id;
            if (!idUsuario) {
                mostrarMensaje('No se encontró usuario en localStorage', 'error');
                return;
            }

            try {
                const res = await fetch(`/api/estudiante/${idUsuario}/cambiar-contrasena`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ currentPassword, newPassword })
                });
                if (!res.ok) throw new Error('Error al cambiar la contraseña');
                mostrarMensaje('Contraseña cambiada exitosamente', 'success');
                document.getElementById('currentPassword').value = '';
                document.getElementById('newPassword').value = '';
                document.getElementById('confirmPassword').value = '';
            } catch (err) {
                console.error(err);
                mostrarMensaje('Error al cambiar la contraseña', 'error');
            }
        }

        function mostrarMensaje(mensaje, tipo) {
            const messageDiv = document.getElementById('securityMessage');
            messageDiv.textContent = mensaje;
            messageDiv.className = `message ${tipo}`;
            messageDiv.style.display = 'block';
            setTimeout(() => { messageDiv.style.display = 'none'; }, 5000);
        }

        // Fortaleza password (igual a la original)
        function checkPasswordStrength() {
            const password = document.getElementById('newPassword').value;
            const strengthBar = document.getElementById('passwordStrength');
            let strength = 0;
            if (password.length >= 8) strength++;
            if (password.match(/[a-z]/) && password.match(/[A-Z]/)) strength++;
            if (password.match(/\d/)) strength++;
            if (password.match(/[^a-zA-Z\d]/)) strength++;
            strengthBar.className = 'strength-fill';
            if (strength === 0) { strengthBar.style.width = '0%'; }
            else if (strength === 1) { strengthBar.className += ' strength-weak'; }
            else if (strength <= 3) { strengthBar.className += ' strength-medium'; }
            else { strengthBar.className += ' strength-strong'; }
        }

        // Logout
        document.querySelector('.logout-btn').addEventListener('click', function() {
            if (confirm('¿Está seguro de que desea cerrar sesión?')) {
                localStorage.removeItem('usuarioDatos');
                window.location.href = '/Inicio_Sesion.html';
            }
        });
