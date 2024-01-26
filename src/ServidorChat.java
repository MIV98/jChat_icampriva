// Iván Campelo

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServidorChat {

    public static final String IP_SERVER = "192.168.0.27";
    public static final int PORT = 5;

    private static ArrayList<ClienteChat> usuariosConectados = new ArrayList<>();
    private static ServerSocket server;

    public static void main(String[] args) {
        
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server activo en " + server.getLocalSocketAddress() 
                + ":" + server.getLocalPort());

            // It's always gonna be running sooo...
            while (true) {
                Socket cliente = server.accept();
                ClienteThreadIn cin = new ClienteThreadIn(cliente.getInputStream().readAllBytes());

            }

        } catch (IOException e) {
            System.err.println("ERROR iniciando server!");;
        }

    }
}
