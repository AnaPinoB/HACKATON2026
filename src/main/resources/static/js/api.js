/* Cliente de la API del mismo origen. Solo la cookie HttpOnly mantiene la sesion. */
(() => {
    let csrf = null;
    async function token() {
        const response = await fetch('/api/csrf', { credentials: 'same-origin', cache: 'no-store', headers: { 'X-Requested-With': 'XMLHttpRequest' } });
        if (!response.ok) throw new Error('No fue posible iniciar una sesión segura.');
        csrf = await response.json();
        return csrf;
    }
    async function request(path, options = {}) {
        const { body, method = 'GET', form = false, quiet = false } = options;
        const headers = { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
        if (!['GET', 'HEAD'].includes(method)) {
            const current = csrf || await token();
            headers[current.headerName] = current.token;
        }
        if (body !== undefined) headers['Content-Type'] = form ? 'application/x-www-form-urlencoded' : 'application/json';
        let response;
        try {
            response = await fetch(path, { method, headers, credentials: 'same-origin', cache: 'no-store',
                body: body === undefined ? undefined : form ? new URLSearchParams(body).toString() : JSON.stringify(body) });
        } catch {
            throw new Error('No hay conexión con el servidor. Conservamos los datos del formulario; reintenta.');
        }
        const payload = response.status === 204 ? null : await response.json().catch(() => null);
        if (!response.ok) {
            if (response.status === 401 && !quiet) window.dispatchEvent(new Event('cacao:session-expired'));
            if (response.status === 403) csrf = null;
            const error = new Error(payload?.detail || (response.status === 403 ? 'No tienes permiso o la sesión cambió. Reintenta.' : 'No fue posible completar la solicitud.'));
            error.status = response.status;
            throw error;
        }
        return payload;
    }
    window.CacaoApi = {
        request,
        async login(correo, password) {
            await token();
            await request('/api/login', { method: 'POST', form: true, quiet: true, body: { correo, password } });
            csrf = null;
            await token(); // Spring renueva el token tras autenticar.
            return request('/api/cuenta');
        },
        async logout() {
            await request('/api/logout', { method: 'POST' });
            csrf = null;
        }
    };
})();
