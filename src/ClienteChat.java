// Iván Campelo

import java.io.IOException;
import java.net.Socket;

public class ClienteChat {

    public static String nick;
    public static Socket server;

    public static void main(String[] args) {
        
        if (args.length != 3) {
            System.out.println("Uso: java ClienteChat <dirección_servidor> <nombre_nic>");
        } else {
            // Actual client functionality
            
            try {
                // args[1] is <direccion_servidor>
                Socket server = new Socket(args[1], ServidorChat.PORT);
                // args[2] is <nombre_nic>
                nick = args[2];
                server.getOutputStream().write(nick.getBytes());
                ClienteThread hilo = ServidorChat.usuariosConectados.get(server.getInputStream().readAllBytes().toString());
                System.out.println("Estás conectado com el nick " + nick);
                hilo.join();
                System.out.println("Adios...");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Hilo interrumpido");
            }

        }

    }
}
