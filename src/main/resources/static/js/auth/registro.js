(function () {
  'use strict';

  const form = document.getElementById('register-form');
  const nombreInput = document.getElementById('nombre');
  const emailInput = document.getElementById('email');
  const telefonoInput = document.getElementById('telefono');
  const passwordInput = document.getElementById('password');
  const confirmPasswordInput = document.getElementById('confirm-password');
  const submitButton = document.getElementById('register-submit');
  const message = document.getElementById('register-message');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    message.textContent = '';
    message.classList.remove('is-success');

    const nombre = nombreInput.value.trim();
    const email = emailInput.value.trim();
    const telefono = telefonoInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    if (!nombre) {
      message.textContent = 'Por favor ingresa tu nombre completo.';
      nombreInput.focus();
      return;
    }

    if (!email) {
      message.textContent = 'Por favor ingresa un correo electrónico válido.';
      emailInput.focus();
      return;
    }

    if (password.length < 6) {
      message.textContent = 'La contraseña debe contener al menos 6 caracteres.';
      passwordInput.focus();
      return;
    }

    if (password !== confirmPassword) {
      message.textContent = 'Las contraseñas no coinciden. Por favor verifícalas.';
      confirmPasswordInput.focus();
      return;
    }

    submitButton.disabled = true;
    submitButton.textContent = 'Creando cuenta...';

    try {
      const nuevoUsuario = await window.api.crearUsuario({
        nombre: nombre,
        email: email,
        telefono: telefono || null,
        password: password,
        rol: 'CLIENT'
      });

      message.textContent = `¡Registro exitoso! Bienvenido/a, ${nuevoUsuario.nombre}. Redirigiendo...`;
      message.classList.add('is-success');

      form.reset();

      setTimeout(() => {
        window.location.assign('login.html');
      }, 1500);

    } catch (error) {
      submitButton.disabled = false;
      submitButton.textContent = 'Crear cuenta';

      if (error.status === 400) {
        message.textContent = error.message || 'Datos de registro inválidos o el correo ya está registrado.';
      } else if (!error.status && (error instanceof TypeError || error.name === 'AbortError')) {
        message.textContent = 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
      } else {
        message.textContent = error.message || 'Ocurrió un error al registrar la cuenta. Inténtalo de nuevo.';
      }
    }
  });
})();
