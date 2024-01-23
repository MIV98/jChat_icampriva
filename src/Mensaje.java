public class Mensaje {
    
    private ClienteThread emisor;
    private ClienteThread receptor;
    private String mensaje;
    
    public Mensaje(ClienteThread emisor, ClienteThread receptor, String mensaje) {
        this.emisor = emisor;
        this.receptor = receptor;
        this.mensaje = mensaje;
    }

    public ClienteThread getEmisor() {
        return emisor;
    }

    public void setEmisor(ClienteThread emisor) {
        this.emisor = emisor;
    }

    public ClienteThread getReceptor() {
        return receptor;
    }

    public void setReceptor(ClienteThread receptor) {
        this.receptor = receptor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    

}
