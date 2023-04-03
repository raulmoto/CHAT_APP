package chat_distribuido;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Conexion {
    private final int PUERTO = 1234; //Puerto para la conexión
//    private final String HOST = "192.168.1.152"; //IP Raul
    private final String HOST = "localhost"; //Host para la conexión
    protected ServerSocket ss; //Socket del servidor
    protected Socket cs; //Socket del cliente
    
    /*
     * Pre: ---
     * Post: Al ss no se le envía host porque él toma la ip de la máquina donde se está creando.
     * Este código sirve para crear un cliente-servidor en la misma máquina
     */
    public Conexion(String tipo) throws IOException {//Constructor
        if(tipo.equalsIgnoreCase("servidor")) {
            ss = new ServerSocket(PUERTO);//Se crea el socket para el servidor en puerto 1234
            //cs = new Socket(); //Socket para el cliente
        } else {
            cs = new Socket(HOST, PUERTO); //Socket para el cliente en localhost en puerto 1234
        }
    }
}
