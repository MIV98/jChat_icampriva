// Iván Campelo

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class ServidorChat {

    public static final String IP_SERVER = "192.168.0.27";
    public static final int PORT = 5;

    public static HashMap<String, ClienteThread> usuariosConectados = new HashMap<>();
    private static ServerSocket server;

    public enum Comando {
        AYUDA, LISTAR, CHARLAR, SALIR;
    }

    public static void main(String[] args) {
        
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server activo en " + server.getLocalSocketAddress());

            // It's always gonna be running sooo...
            while (true) {
                Socket cliente = server.accept();
                
                DataInputStream cliIn = new DataInputStream(cliente.getInputStream());
                DataOutputStream cliOut = new DataOutputStream(cliente.getOutputStream());

                String nick = cliIn.readUTF();
                // TODO handle users trying to connect with a username that already exists
                
                System.out.println(nick + "\t" + cliente.getInetAddress() + "\tCONECTADO");
                Thread hiloSocket = new Thread(() -> manejarComandosSocket(cliente, usuariosConectados.get(nick)));
                hiloSocket.start();

                // Send the client their Thread nickname so they can acces it and wait for it to complete
                cliOut.writeUTF("Estás conectado con el nick " + nick);
            }

        } catch (IOException e) {
            System.err.println("ERROR iniciando server!");;
        }

    }

    // TODO clean up and refactor all of this in general ugh
    public static void manejarComandosSocket(Socket socket, ClienteThread cliente) {
        try (DataInputStream cliIn = new DataInputStream(socket.getInputStream())){
            String comando = cliIn.readUTF().toUpperCase();
            DataOutputStream cliOut = new DataOutputStream(socket.getOutputStream());


            if (!comando.startsWith("#") && !cliente.isConversando()) {
                cliOut.writeUTF("[ERROR] "
                        + comando + " no se reconoce como comando. Si quieres iniciar una " +
                        "conversación o responder a un usuario utilza el comando" +
                        " #charlar <nic>");
            } else if(!comando.startsWith("#") && cliente.isConversando()) {
                synchronized (usuariosConectados) {
                    // TODO check if receptor is still connected and end conversation if they're not
                    ClienteThread receptor = usuariosConectados.get(cliente.getNickReceptor());
                    String mensaje = ">" + cliente.getNick() + " " + comando;
                    DataOutputStream receptorOut = new DataOutputStream(receptor.getSocket().getOutputStream());
                    receptorOut.writeUTF(mensaje);
                }
            } else {
                comando = comando.substring(1); // skip the "#"

                // TODO refactor to a SWITCH
                if (comando.contains(Comando.CHARLAR.toString())) {
                    synchronized (usuariosConectados) {
                        String[] split = comando.split(" ");
                        if (!usuariosConectados.containsKey(split[1])) {
                            cliOut.writeUTF("[ERROR] " +
                            "El usuario" + split[1] + " no se encuentra conectado." +
                            " Utiliza el comando #list para ver los usuarios " +
                            "conectados");
                        } else {
                            cliente.iniciarConversacion(split[1]);
                        }
                    }
                } else if(comando.contains(Comando.AYUDA.toString())) {
                    cliOut.writeUTF("#listar: lista" +
                        " todos los usuarios conectados.\n" + 
                        "#charlar <usuario>: comienza la comunicación con el usuario <usuario>\n" + 
                        "#salir: se desconecta del chat");
                } else if (comando.contains(Comando.LISTAR.toString())) {
                    synchronized (usuariosConectados) {
                        String salida = "Actualmente están conectados " 
                            + usuariosConectados.size() + " usuarios:\n";
    
                        for (Entry<String, ClienteThread> e : usuariosConectados.entrySet()) {
                            salida += e.getKey() + "\n"; // the key is the username
                        }
                        cliOut.writeUTF(salida);
                    }
                } else if (comando.contains(Comando.SALIR.toString())) {
                    cliOut.writeUTF(Comando.SALIR.toString());
                    desconectarCliente(cliente);
                } else {
                    // TODO this is the same code as above soooo... refactor a bit
                    cliOut.writeUTF("[ERROR] " + comando + " no se reconoce como comando. " + 
                        "Si quieres iniciar una conversación o responder a un usuario" + 
                        "utilza el comando #charlar <nic>");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("This exception should never happen since"
            + " desconectarCliente() checks if the client is conected first.");
        }
    }

    public static void desconectarCliente(ClienteThread cliente) throws Exception {
        synchronized (usuariosConectados) {
            if (usuariosConectados.containsKey(cliente.getNick())) {
                System.out.println(cliente.getNick() + "\t" 
                + cliente.getSocket().getInetAddress() + "\tDESCONECTADO");
    
                
                getUsuarioConectado(cliente.getNick()).interrupt();
                usuariosConectados.remove(cliente.getNick());
            }
        }
    }

    // TODO use this method wherever it's useful + create custom Exception
    public static ClienteThread getUsuarioConectado(String nick) throws Exception {
        synchronized (usuariosConectados) {
            if (usuariosConectados.containsKey(nick)) {
                return usuariosConectados.get(nick);
            } else {
                throw new Exception("[ERROR]" + nick + " no se encuantra conectado!");
            }
        }
    }

    public static boolean registrarUsuario(String nick, ClienteThread hilo) {
        synchronized (usuariosConectados) {
            if (usuariosConectados.containsKey(nick)) {
                return false;
            } else {
                usuariosConectados.put(nick, hilo);    
                return true;
            }
        }
    }
}
