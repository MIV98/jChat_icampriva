// Iván Campelo

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteThread  extends Thread {
    private String nick;
    private Socket socket;

    private boolean conversando;
    private String nickReceptor;

    private Thread receiver;
    private Thread sender;

    public ClienteThread(Socket socket, String nick) {
        this.socket = socket;
        this.nick = nick;
        this.conversando = false;
        this.nickReceptor = "";
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());) {
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            this.receiver = new Thread(() -> {
                String mensaje = "";
                while (!mensaje.equalsIgnoreCase(ServidorChat.Comando.SALIR.toString())) {
                    try {
                        mensaje = in.readUTF();

                        System.out.println(mensaje);
                    } catch (IOException e) {
                        System.err.println("[ERROR] Leyendo respuesta del servidor");
                    }
                }

                this.sender.interrupt();
            });

            this.receiver.start();

            this.sender = new Thread(() -> {
                String mensaje = "";
                Scanner sc = new Scanner(System.in);
                while (true) {
                    mensaje = sc.nextLine();
                    try {
                        out.writeUTF(mensaje);
                    } catch (IOException e) {
                        System.err.println("[ERROR] Server desconectado.");
                    }
                }
            });

            this.sender.start();

            this.sender.join();
            this.receiver.join();

        } catch (IOException ex) {
            System.err.println("Error obteniendo Socket Data Streams");
        } catch (InterruptedException intEx) {
            System.err.println("Adios...");
        }
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Thread getReceiver() {
        return receiver;
    }

    public void setReceiver(Thread receiver) {
        this.receiver = receiver;
    }

    public Thread getSender() {
        return sender;
    }

    public void setSender(Thread sender) {
        this.sender = sender;
    }

    public boolean isConversando() {
        return conversando;
    }

    public void setConversando(boolean conversando) {
        this.conversando = conversando;
    }

    public String getNickReceptor() {
        return nickReceptor;
    }

    public void setNickReceptor(String nickReceptor) {
        this.nickReceptor = nickReceptor;
    }

    public void iniciarConversacion(String nickReceptor) {
        this.conversando = true;
        this.nickReceptor = nickReceptor;
    }

    
    
}
