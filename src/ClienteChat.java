// Iván Campelo

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClienteChat {

    public static String nick;
    public static Socket server;

    public static void main(String[] args) {
        
        if (args.length != 2) {
            System.out.println("Uso: java ClienteChat <dirección_servidor> <nombre_nic>");
        } else {
            // Actual client functionality
            
            try {
                // args[0] is <direccion_servidor>
                Socket server = new Socket(args[0], ServidorChat.PORT);
                // args[1] is <nombre_nic>
                nick = args[1];
                
                DataInputStream serverIn = new DataInputStream(server.getInputStream());
                DataOutputStream serverOut = new DataOutputStream(server.getOutputStream());

                serverOut.writeUTF(nick);
                ClienteThread hilo = new ClienteThread(server, nick);
                if (ServidorChat.registrarUsuario(nick, hilo)) {
                    System.out.println("Estás conectado com el nick " + nick);
                    hilo.start();
                    hilo.join();
                } else {
                    System.err.println("[ERROR] El usuario " + nick + " ya se encuentra conectado!");
                }

                System.out.println("Adios...");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Hilo interrumpido");
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        }
    }
}
