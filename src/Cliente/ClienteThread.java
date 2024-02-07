package Cliente;
// Iván Campelo

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import Servidor.ServidorChat;
import Servidor.ServidorChat.Comando;

public class ClienteThread  extends Thread {
    private String nick;
    private Socket socket;

    private boolean conversando;
    private String nickReceptor;
    private boolean isRunning;

    private Thread receiver;
    private Thread sender;

    public ClienteThread(Socket socket, String nick) {
        this.socket = socket;
        this.nick = nick;
        this.isRunning = true;
        this.conversando = false;
        this.nickReceptor = "";
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());) {
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // This thread will receive messages from the server
            this.receiver = new Thread(() -> {
                String mensaje = "";
                do {
                    try {
                        mensaje = in.readUTF();

                        if (mensaje.equalsIgnoreCase(ServidorChat.Comando.SALIR.toString())) {
                            break;
                        }

                        /** The server will tell the client to start a conversation 
                        * with <user> by sending a !<user> message.
                        *
                        * The way I decided to implement this, if a user is IN a conversation
                        * They can no longer input #commands (except for #salir, 
                        * to leave the conversation).
                        * This is the intended behavior as of rn.
                        */
                        if (mensaje.startsWith("!")) {
                            //  finish a conversation when the recipient is disconnected
                            if (this.isConversando()) {
                                this.finalizarConversacion("El usuario " + this.nickReceptor + " está desconectado, dejando la conversación...");
                                mensaje = "Use #ayuda para ver una lista de comandos:";
                            } else {
                                // substring with beginIndex1 to skip the "!" server token
                                this.iniciarConversacion(mensaje.substring(1));
                                mensaje = "Ahora estás conectado con " + this.nickReceptor + " Escribe para hablarle";
                            }
                        }

                        System.out.println(mensaje);
                    } catch (IOException e) {
                        // ---- why does it keep throwing me this exception
                        // nvm I'm a dumbass and forgot to make the server thread's run a loop -.-"
                        System.err.println("[ERROR] Conexión con Server perdida");
                        break;
                    }
                } while (!mensaje.equalsIgnoreCase(ServidorChat.Comando.SALIR.toString()));

                System.out.println("Te has desconectado del server. Pulse Enter para salir.");

                this.isRunning = false;
            });


            this.receiver.start();

            // This thread is dedicated to sending messages to the server
            this.sender = new Thread(() -> {
                String mensaje = "";
                Scanner sc = new Scanner(System.in);
                
                while (true) {
                    mensaje = sc.nextLine();

                    // I break out of the loop manually here to avoid an IOException

                    if (!this.isRunning) {
                        break;
                    }

                    try {
                        if (this.isConversando()) {

                            /** Code to allow users to leave a conversation
                            * RN the first time they type in #SALIR will "leave
                            * the conversation and put them in regular mode again"
                            * In reality nothing happens server side since 
                            * client-to-client communication is based on tokens
                            * sent to the server and managed server-side
                            */
                            // TODO this is a bit of a funky way to check this 
                            // and it could be broken by an user ("# blabla #salir")
                            if (mensaje.startsWith("#") && 
                                    mensaje.toUpperCase().contains("#" + ServidorChat.Comando.SALIR)) {
                                this.finalizarConversacion("Has dejado la conversación con " + this.nickReceptor);
                                mensaje = sc.nextLine();

                                // I probably don't have to check if this.isRunning()
                                // since there shouldn't be a possibility for
                                // the user to disconnect via cmd
                            } else {
                                mensaje = "!" + this.nickReceptor + " " + mensaje;
                            }

                        }

                        out.writeUTF(mensaje);
                    } catch (IOException e) {
                        System.err.println("[ERROR] Server desconectado.");
                        sc.close();
                        break;
                    }
                }

                sc.close();
            });

            this.sender.start();

            // I don't necessarily need to wait for both threads to finish
            // Especially since the scanner is blocking the sender thread until
            // last user input (maybe change this but I like the exit message prompt thingy)
            this.receiver.join();
            this.sender.join();

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

    // Mensajito is shown to the user so they know the reason they left/got kicked from the conversation
    private void finalizarConversacion(String mensajito) {
        this.conversando = false;
        System.out.println(mensajito);
        this.nickReceptor = "";
    }
    
}
