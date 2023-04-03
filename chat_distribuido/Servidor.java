package chat_distribuido;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
 * Pre: ---
 * Post: Clase que funciona como servidor, establece el socket y 
 * espera por conexiones de clientes, lanza un hilo por cada cliente 
 * conectado.
 */
public class Servidor extends Conexion {
	/*map es similar al arraylist, con lal diferencia en que, en el arraylist los datos se almacenan de manera secuencial
	 * y para acceder a ellos basta con decir "dame el elemento en la posicion tal...", mientras que con el map tienes que 
	 * pasarle la clave (nombre del elemento).
	 * 
	 * en el arrylist recorres la tabla comparando cada elemento y en map no!!, directamente coges el nombre ya que está indexado*/
    private Map<String, Sala> salas;
    private List<String> nombresClientes;
    private ArrayList<ThreadServidor> hilosServidor;

    public Servidor() throws IOException {
    	super("servidor");
        salas = new HashMap<>();
        nombresClientes = new ArrayList<>();
        hilosServidor = new ArrayList<ThreadServidor>();
    }

    /*
     * Pre: ---
     * Post: Espera por conexiones de clientes, lanza un hilo por cada cliente conectado
     * Notifica a la consola del servidor cada vez que se conecta un cliente
     */
    public void iniciar() {
        try {
        	while(true) {
        		System.out.println("Servidor Salesianos chat..."); //Esperando conexión
        		ThreadServidor hilo = new ThreadServidor(ss.accept(), hilosServidor, salas, nombresClientes);
        		hilo.start();
                System.out.println("Client concectado");
                // se envía el arrayList para que el hilo pueda borrarse de este al finalizar:  
                hilosServidor.add(hilo); 
                System.out.println("Clients online: " + hilosServidor.size());
        	}
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor");
            e.printStackTrace();
        }
    }

}
