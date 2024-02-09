// Iván Campelo
package Cliente;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import Servidor.ServidorChat;

public class ClienteChat {

    public static String nick;
    public static Socket server;

    public static void main(String[] args) {
        
        if (args.length != 2) {
            System.out.println("Uso: java ClienteChat <dirección_servidor> <nombre_nic>");
            if (args.length > 2) {
                // I decided user nicknames shouldn't contain whitespaces
                System.out.println("El <nombre_nic> no puede contener espacios.");
            }
        } else {
            
            // Actual client functionality
            try {
                // args[0] is <direccion_servidor>
                Socket server = new Socket(args[0], ServidorChat.PORT);
                // args[1] is <nombre_nic>, since it doesn't support whitespaces
                nick = args[1];
                
                DataOutputStream serverOut = new DataOutputStream(server.getOutputStream());

                serverOut.writeUTF(nick);
                
                ClienteThread hilo = new ClienteThread(server, nick);

                hilo.start();
                hilo.join();

                server.close();
                
            } catch (IOException ex) {
                System.err.println("[ERROR] No se puede conectar con el server en " 
                    + args[0] + ":" + ServidorChat.PORT);
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Hilo interrumpido");
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

        }
    }
}
