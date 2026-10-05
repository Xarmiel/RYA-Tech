(function () {
  'use strict';

  const form = document.getElementById('login-form');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const submitButton = document.getElementById('login-submit');
  const message = document.getElementById('login-message');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    message.textContent = '';
    message.classList.remove('is-success');
    submitButton.disabled = true;
    submitButton.textContent = 'Verificando...';

    try {
      const usuario = await window.api.iniciarSesion({
        email: emailInput.value.trim(),
        password: passwordInput.value
      });
      sessionStorage.setItem('rya_usuario', JSON.stringify(usuario));
      message.textContent = `¡Bienvenido/a, ${usuario.nombre}!`;
      message.classList.add('is-success');
      window.location.assign('inicio.html');
    } catch (error) {
      if (error.status === 401) {
        message.textContent = 'El correo o la contraseña no son correctos.';
      } else if (!error.status && (error instanceof TypeError || error.name === 'AbortError')) {
        message.textContent = 'No se pudo conectar con el servidor. Inicia el backend en http://localhost:8080 e inténtalo de nuevo.';
      } else {
        message.textContent = error.message || 'No se pudo iniciar sesión. Inténtalo de nuevo.';
      }
    } finally {
      submitButton.disabled = false;
      submitButton.textContent = 'Iniciar sesión';
    }
  });
})();
