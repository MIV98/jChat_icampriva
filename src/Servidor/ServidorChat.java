// Iván Campelo
package Servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;

public class ServidorChat {

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
    /**
     * Main method for each server thread. It handles communication client-server
     * and client-server-client, enabling 2 users to communicate with each other
     * @param nick Client's username
     * @param cliente Client's socket
    */
    public static void manejarComandosSocket(String nick, Socket cliente) {

        try (DataInputStream cliIn = new DataInputStream(cliente.getInputStream())) {
            while (usuariosConectados.containsKey(nick)) {                
                String comando = cliIn.readUTF();
                DataOutputStream cliOut = new DataOutputStream(cliente.getOutputStream());

                if (!comando.startsWith("#") && !comando.startsWith("!")) {
                    cliOut.writeUTF("[ERROR] "
                            + comando + " no se reconoce como comando. Si quieres iniciar una " +
                            "conversación o responder a un usuario utiliza el comando" +
                            " #charlar <nic>");
                } else if (comando.startsWith("!")) {
                    // "!" indicates the user is sending a message to another user
                    redirectMsgToReceiver(nick, comando, cliOut);
                } else {
                    handleCommand(nick, cliente, comando, cliOut);
                }
            }
        } catch (IOException e) {
            // connection is lost so user should be removed and their nickname freed
            synchronized (usuariosConectados) {
                if (usuariosConectados.containsKey(nick)) {
                    usuariosConectados.remove(nick);
                }
            }

            System.out.println(nick + "\t"
                    + cliente.getInetAddress() + "\tDESCONECTADO");
        }
    }

    /**
     * Parses the user's command and handles it accordingly, sending the proper 
     * response to the client if necessary
     * @param nick The client's username
     * @param cliente The client's socket
     * @param comando The RAW command received from the client (including the "#")
     * @param cliOut The client's output stream
     * @throws IOException Thrown if connection to client is lost
     */
    private static void handleCommand(String nick, Socket cliente, String comando, DataOutputStream cliOut)
            throws IOException {
        comando = comando.substring(1); // skip the "#"
        
        // safety check in case user only inputs a "#" or inputs a whitespace after it
        if (comando.length() > 0 && !comando.startsWith(" ")) {
            // TODO refactor into a SWITCH
            // This check is a bit too much maybe but this way the command
            // has to actually be "#charlar" (so "#charlar*" isn't accepted)
            if (comando.toUpperCase().split(" ")[0].equals(Comando.CHARLAR.toString())) {
                synchronized (ServidorChat.usuariosConectados) {
                    String[] split = comando.split("\s+");

                    // Server doesn't support whitespace in usernames 
                    // so expected length is 2
                    if (split.length == 2) {
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
                    } else {
                        cliOut.writeUTF("[ERROR] Introduzca un nombre de usuario válido después del comando #charlar");
                    }
                }
            } else if (comando.toUpperCase().stripTrailing().equals(Comando.AYUDA.toString())) {
                cliOut.writeUTF("#listar: lista todos los usuarios conectados.\n" +
                        "#charlar <usuario>: comienza la comunicación con el usuario <usuario>\n" +
                        "#salir: se desconecta del chat");
            } else if (comando.toUpperCase().stripTrailing().equals(Comando.LISTAR.toString())) {
                synchronized (ServidorChat.usuariosConectados) {
                    String salida = "Actualmente están conectados "
                            + ServidorChat.usuariosConectados.size() + " usuarios:\n";
   
                    for (Entry<String, Socket> e : ServidorChat.usuariosConectados.entrySet()) {
                        // This adds a distinction so the user's can recognise thir own username
                        if (nick.equals(e.getKey())) salida += e.getKey() + " (tú)" + "\n";
                        else salida += e.getKey() + "\n"; // the key is the username
                    }
                    
                    cliOut.writeUTF(salida);
                }
            } else if (comando.toUpperCase().stripTrailing().equals(Comando.SALIR.toString())) {
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
                cliOut.writeUTF("[ERROR] #" + comando + " no se reconoce como comando. " +
                        "Si quieres iniciar una conversación o responder a un usuario" +
                        "utilza el comando #charlar <nic>");
            }
        } else {
            cliOut.writeUTF("[ERROR] Por favor, escriba un comando después de la almohadilla (#) sin espacios.");
        }
    }

    /**
     * Sends the message from one client ("sender") to another one 
     * in the list of connected users ("receiver")
     * @param nick The "sender" client username
     * @param comando The RAW command received from the client (with the "!" send msg instruction)
     * @param cliOut The sender's Data Output Stream
     * @throws IOException Thrown if connection with the client is lost
     */
    private static void redirectMsgToReceiver(String nick, String comando, DataOutputStream cliOut) throws IOException {
        synchronized (ServidorChat.usuariosConectados) {
            // This split is fine with just one space since it's managed by the client
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

                String mensajeSend = ">" + nick + ": " + mensaje;
                DataOutputStream receptorOut = new DataOutputStream(receptor.getOutputStream());
                receptorOut.writeUTF(mensajeSend);
            } else {
                // The recipient is disconnected so the client should leave the conversation
                // The content of the command is irrelevant, only the
                // "!" is important
                cliOut.writeUTF("!DISCONNECTED_USER");
            }

        }
    }
}
