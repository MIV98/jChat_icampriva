// Iván Campelo

import java.net.ServerSocket;
import java.util.ArrayList;

public class ServidorChat {

    public static final String IP_SERVER = "192.168.0.27";
    public static final int PORT = 5;

    private static ArrayList<ClienteChat> usuariosConectados = new ArrayList<>();
    private static ServerSocket server;

    public static void main(String[] args) {
        
        server = new ServerSocket(PORT);

    }
}
