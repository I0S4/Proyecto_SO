import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import dto.EstadoSistemaDTO;
import java.util.Collections;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 7070;
    private static final SimuladorSO simulador = new SimuladorSO();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                });
            });
        });

        // REST Endpoints
        app.post("/api/configurar", ctx -> {
            JsonNode body = objectMapper.readTree(ctx.body());
            String algoritmo = body.get("algoritmo").asText();
            int quantum = body.get("quantum").asInt();
            int tamanoPagina = body.get("tamanoPagina").asInt();
            int tamanoRam = body.get("tamanoRam").asInt();
            
            synchronized (simulador) {
                simulador.configurar(algoritmo, quantum, tamanoPagina, tamanoRam);
            }
            EstadoSistemaDTO estado = obtenerEstado();
            broadcast(estado);
            ctx.json(estado);
        });

        app.post("/api/admitir", ctx -> {
            JsonNode body = objectMapper.readTree(ctx.body());
            String id = body.get("id").asText();
            int totalInstrucciones = body.get("totalInstrucciones").asInt();
            int bytesStack = body.get("bytesStack").asInt();
            int bytesHeap = body.get("bytesHeap").asInt();
            
            synchronized (simulador) {
                simulador.admitirProceso(id, totalInstrucciones, bytesStack, bytesHeap);
            }
            EstadoSistemaDTO estado = obtenerEstado();
            broadcast(estado);
            ctx.json(estado);
        });

        app.post("/api/io", ctx -> {
            JsonNode body = objectMapper.readTree(ctx.body());
            String idProceso = body.get("idProceso").asText();
            String nombreDispositivo = body.get("nombreDispositivo").asText();
            
            synchronized (simulador) {
                simulador.solicitarES(idProceso, nombreDispositivo);
            }
            EstadoSistemaDTO estado = obtenerEstado();
            broadcast(estado);
            ctx.json(estado);
        });

        app.post("/api/tick", ctx -> {
            EstadoSistemaDTO estado;
            synchronized (simulador) {
                estado = simulador.ejecutarTick();
            }
            broadcast(estado);
            ctx.json(estado);
        });

        app.get("/api/estado", ctx -> {
            ctx.json(obtenerEstado());
        });

        // WebSocket Endpoint
        app.ws("/simulador", ws -> {
            ws.onConnect(ctx -> {
                sessions.add(ctx);
                System.out.println("[WS] Cliente conectado desde: " + ctx.session.getRemoteAddress());
                // Send current state on connect
                ctx.send(obtenerEstado());
            });

            ws.onClose(ctx -> {
                sessions.remove(ctx);
                System.out.println("[WS] Cliente desconectado. Conexiones activas: " + sessions.size());
            });

            ws.onError(ctx -> {
                sessions.remove(ctx);
                System.out.println("[WS] Error en la conexion: " + (ctx.error() != null ? ctx.error().getMessage() : "Desconocido") + ". Conexiones activas: " + sessions.size());
            });

            ws.onMessage(ctx -> {
                String message = ctx.message();
                JsonNode body = objectMapper.readTree(message);
                String action = body.has("action") ? body.get("action").asText() : "";
                EstadoSistemaDTO nuevoEstado = null;

                synchronized (simulador) {
                    switch (action.toLowerCase()) {
                        case "configurar":
                        case "configure":
                            String algoritmo = body.get("algoritmo").asText();
                            int quantum = body.get("quantum").asInt();
                            int tamanoPagina = body.get("tamanoPagina").asInt();
                            int tamanoRam = body.get("tamanoRam").asInt();
                            simulador.configurar(algoritmo, quantum, tamanoPagina, tamanoRam);
                            break;

                        case "admitir":
                        case "admit":
                            String id = body.get("id").asText();
                            int totalInstrucciones = body.get("totalInstrucciones").asInt();
                            int bytesStack = body.get("bytesStack").asInt();
                            int bytesHeap = body.get("bytesHeap").asInt();
                            simulador.admitirProceso(id, totalInstrucciones, bytesStack, bytesHeap);
                            break;

                        case "io":
                        case "solicitares":
                            String idProceso = body.get("idProceso").asText();
                            String nombreDispositivo = body.get("nombreDispositivo").asText();
                            simulador.solicitarES(idProceso, nombreDispositivo);
                            break;

                        case "tick":
                            simulador.ejecutarTick();
                            break;

                        case "estado":
                        case "status":
                            // Just request status refresh
                            break;

                        default:
                            ctx.send(MapError("Acción desconocida: " + action));
                            return;
                    }
                    nuevoEstado = simulador.generarEstadoCompleto();
                }

                if (nuevoEstado != null) {
                    broadcast(nuevoEstado);
                }
            });
        });

        app.start(PORT);
        System.out.println("Servidor iniciado en http://localhost:" + PORT);
    }

    private static EstadoSistemaDTO obtenerEstado() {
        synchronized (simulador) {
            return simulador.generarEstadoCompleto();
        }
    }

    private static void broadcast(EstadoSistemaDTO estado) {
        String msg;
        try {
            msg = objectMapper.writeValueAsString(estado);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        for (WsContext session : sessions) {
            if (session.session.isOpen()) {
                try {
                    session.send(msg);
                } catch (Exception e) {
                    sessions.remove(session);
                }
            }
        }
    }

    private static String MapError(String errorMsg) {
        return "{\"error\": \"" + errorMsg + "\"}";
    }
}
