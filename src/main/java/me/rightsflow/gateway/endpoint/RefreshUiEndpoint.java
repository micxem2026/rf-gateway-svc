package me.rightsflow.gateway.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.stereotype.Component;

/**
 * Простая HTML-страница с кнопкой для вызова POST /actuator/refresh.
 * <p>
 * Регистрируется как кастомный actuator-эндпоинт (id = "refresh-ui"), поэтому
 * автоматически попадает на management-порт (тот же 8091, что и остальные
 * actuator-эндпоинты) — в обход роутинга Gateway на основном порту 8090.
 * <p>
 * Используется современный @Endpoint/@WebEndpoint API — @RestControllerEndpoint
 * deprecated (forRemoval) начиная с Spring Boot 3.3.0.
 * <p>
 * Доступ ограничен только сетевой изоляцией management-порта (не публикуется
 * наружу из k8s), без отдельной аутентификации.
 */
@Component
@WebEndpoint(id = "refresh-ui")
public class RefreshUiEndpoint {

    @ReadOperation(produces = "text/html")
    public WebEndpointResponse<String> page() {
        return new WebEndpointResponse<>(HTML, 200);
    }

    private static final String HTML = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <title>RightsFlow Gateway — Actuator</title>
                <style>
                    body { font-family: system-ui, sans-serif; max-width: 480px; margin: 60px auto; padding: 0 16px; }
                    h1 { font-size: 1.2rem; }
                    button {
                        padding: 10px 20px; font-size: 1rem; cursor: pointer;
                        background: #2563eb; color: #fff; border: none; border-radius: 6px;
                    }
                    button:disabled { background: #94a3b8; cursor: not-allowed; }
                    #status { margin-top: 16px; white-space: pre-wrap; font-family: monospace; font-size: 0.9rem; }
                    .ok { color: #16a34a; }
                    .err { color: #dc2626; }
                    .links { margin-top: 32px; font-size: 0.9rem; }
                    .links a { display: block; margin-top: 6px; }
                </style>
            </head>
            <body>
                <h1>RightsFlow Gateway</h1>
                <button id="refreshBtn" onclick="doRefresh()">Обновить конфигурацию (refresh)</button>
                <div id="status"></div>

                <div class="links">
                    <a href="/actuator/health" target="_blank">/actuator/health</a>
                    <a href="/actuator/info" target="_blank">/actuator/info</a>
                </div>

                <script>
                    async function doRefresh() {
                        const btn = document.getElementById('refreshBtn');
                        const status = document.getElementById('status');
                        btn.disabled = true;
                        status.className = '';
                        status.textContent = 'Выполняется...';
                        try {
                            const res = await fetch('/actuator/refresh', { method: 'POST' });
                            const body = await res.text();
                            if (res.ok) {
                                status.className = 'ok';
                                status.textContent = 'OK (' + res.status + ')\\n' + body;
                            } else {
                                status.className = 'err';
                                status.textContent = 'Ошибка ' + res.status + '\\n' + body;
                            }
                        } catch (e) {
                            status.className = 'err';
                            status.textContent = 'Ошибка сети: ' + e.message;
                        } finally {
                            btn.disabled = false;
                        }
                    }
                </script>
            </body>
            </html>
            """;
}