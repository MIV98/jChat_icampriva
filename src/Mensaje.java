public class Mensaje {
    
    private ClienteThreadIn emisor;
    private ClienteThreadIn receptor;
    private String mensaje;
    
    public Mensaje(ClienteThreadIn emisor, ClienteThreadIn receptor, String mensaje) {
        this.emisor = emisor;
        this.receptor = receptor;
        this.mensaje = mensaje;
    }

    public ClienteThreadIn getEmisor() {
        return emisor;
    }

    public void setEmisor(ClienteThreadIn emisor) {
        this.emisor = emisor;
    }

    public ClienteThreadIn getReceptor() {
        return receptor;
    }

    public void setReceptor(ClienteThreadIn receptor) {
        this.receptor = receptor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    

}
