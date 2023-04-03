package chat_distribuido;

import java.io.IOException;

//Clase principal que pone en marcha el servidor.
public class MainServidor {
	
	public static void main(String[] args) throws IOException {
		Servidor serv = new Servidor(); //Se crea el servidor
		
		System.out.println("SERVIDOR INICIADO\n");
		serv.iniciar(); //Se inicia el servidor
    }
	  
}