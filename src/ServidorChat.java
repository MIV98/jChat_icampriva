// Iván Campelo

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;

public class ServidorChat {

    public static final String IP_SERVER = "192.168.0.27";
    public static final int PORT = 5;

    public static HashMap<String, Socket> usuariosConectados;
    private static ServerSocket server;

    public enum Comando {
        AYUDA, LISTAR, CHARLAR, SALIR;
    }

    public static void main(String[] args) {
        
        try {
            usuariosConectados = new HashMap<>();
            
            server = new ServerSocket(PORT);
            System.out.println("Server activo en " + server.getLocalSocketAddress());

            // It's always gonna be running sooo...
            while (true) {
                Socket cliente = server.accept();
                
                DataInputStream cliIn = new DataInputStream(cliente.getInputStream());
                DataOutputStream cliOut = new DataOutputStream(cliente.getOutputStream());

                String nick = cliIn.readUTF();
                // TODO handle users trying to connect with a username that already exists

                synchronized (ServidorChat.usuariosConectados) {
                    if (ServidorChat.usuariosConectados.containsKey(nick)) {
                        cliOut.writeUTF("[ERROR] El usuario ya está conectado!");
                        cliOut.writeUTF(Comando.SALIR.toString());
                        
                        // Show a different prompt in the server to differentiate
                        // rejections from disconnections
                        System.out.println(nick + "\t"
                            + cliente.getInetAddress() + "\tRECHAZADO");
                        
                        cliente.close();
                    } else {
                        ServidorChat.usuariosConectados.put(nick, cliente);
                        // Tell the client conection was successful
                        System.out.println(nick + "\t" + cliente.getInetAddress() + "\tCONECTADO");
                        cliOut.writeUTF("Estás conectado con el nick " + nick 
                            + ".\nUse el comando #AYUDA para ver todos los comandos.");
                    }
                }

                // if connection wasn't rejected start a new thread
                if (!cliente.isClosed()) {
                    Thread hiloSocket = new Thread(() -> manejarComandosSocket(nick, cliente));

                    hiloSocket.start();
                }

            }

        } catch (IOException e) {
            System.err.println("ERROR iniciando server!");;
        }

    }

    // TODO clean up and refactor all of this in general ugh
    public static void manejarComandosSocket(String nick, Socket cliente) {

        try (DataInputStream cliIn = new DataInputStream(cliente.getInputStream())) {
            while (usuariosConectados.containsKey(nick)) {                
                String comando = cliIn.readUTF(); // TODO make commands case insensitive
                DataOutputStream cliOut = new DataOutputStream(cliente.getOutputStream());

                if (!comando.startsWith("#") && !comando.startsWith("!")) {
                    cliOut.writeUTF("[ERROR] "
                            + comando + " no se reconoce como comando. Si quieres iniciar una " +
                            "conversación o responder a un usuario utiliza el comando" +
                            " #charlar <nic>");
                } else if (comando.startsWith("!")) {
                    synchronized (ServidorChat.usuariosConectados) {
                        String[] split = comando.split(" ");

                        // split[0] contains recipient username, beginIndex:1 skips the "!"
                        String nickReceptor = split[0].substring(1);

                        if (usuariosConectados.containsKey(nickReceptor)) {
                            Socket receptor = usuariosConectados.get(nickReceptor);
                            // split[1] contains the actual message
                            String mensaje = "";

                            for (int i = 1; i < split.length; i++) {
                                mensaje += " " + split[i];
                            }

                            String mensajeSend = ">" + nick + mensaje;
                            DataOutputStream receptorOut = new DataOutputStream(receptor.getOutputStream());
                            receptorOut.writeUTF(mensajeSend);
                        } else {
                            // The recipient is disconnected so the client should leave the conversation
                            // The content of the command is irrelevant, only the
                            // "!" is important
                            cliOut.writeUTF("!DISCONNECTED_USER");
                        }

                    }
                } else {
                    comando = comando.substring(1); // skip the "#"

                    // TODO refactor to a SWITCH
                    if (comando.toUpperCase().contains(Comando.CHARLAR.toString())) {

                        // -T-O-D-O- how do clients get out of a conversation?
                        // DONE -> This is managed on Client-Side

                        synchronized (ServidorChat.usuariosConectados) {
                            // TODO server shouldn't support whitespace in the username
                            String[] split = comando.split(" ");
                            if (!ServidorChat.usuariosConectados.containsKey(split[1])) {
                                cliOut.writeUTF("[ERROR] " +
                                        "El usuario " + split[1] + " no se encuentra conectado." +
                                        " Utiliza el comando #list para ver los usuarios " +
                                        "conectados");
                            } else if (nick.equals(split[1])) { // If sender is the same as the receiver
                                // Don't allow users to try to talk to themselves
                                cliOut.writeUTF("[ERROR] No puedes iniciar una conversación contigo mismo.");
                            } else {
                                cliOut.writeUTF("!" + split[1]);
                            }
                        }
                    } else if (comando.toUpperCase().contains(Comando.AYUDA.toString())) {
                        cliOut.writeUTF("#listar: lista" +
                                " todos los usuarios conectados.\n" +
                                "#charlar <usuario>: comienza la comunicación con el usuario <usuario>\n" +
                                "#salir: se desconecta del chat");
                    } else if (comando.toUpperCase().contains(Comando.LISTAR.toString())) {
                        synchronized (ServidorChat.usuariosConectados) {
                            String salida = "Actualmente están conectados "
                                    + ServidorChat.usuariosConectados.size() + " usuarios:\n";

                            for (Entry<String, Socket> e : ServidorChat.usuariosConectados.entrySet()) {
                                salida += e.getKey() + "\n"; // the key is the username
                            }
                            cliOut.writeUTF(salida);
                        }
                    } else if (comando.toUpperCase().contains(Comando.SALIR.toString())) {
                        cliOut.writeUTF(Comando.SALIR.toString());
                        synchronized (ServidorChat.usuariosConectados) {
                            if (ServidorChat.usuariosConectados.containsKey(nick)) {
                                System.out.println(nick + "\t"
                                        + cliente.getInetAddress() + "\tDESCONECTADO");

                                cliente.close();

                                usuariosConectados.remove(nick);
                            }
                        }
                    } else {
                        // TODO this is the same code as above soooo... refactor a bit
                        cliOut.writeUTF("[ERROR] " + comando + " no se reconoce como comando. " +
                                "Si quieres iniciar una conversación o responder a un usuario" +
                                "utilza el comando #charlar <nic>");
                    }
                }
            }
        } catch (IOException e) {
            // connection is lost so user should be removed and their nickname freed
            if (usuariosConectados.containsKey(nick)) {
                usuariosConectados.remove(nick);
            }

            System.out.println(nick + "\t"
                    + cliente.getInetAddress() + "\tDESCONECTADO");
        }
    }
}
