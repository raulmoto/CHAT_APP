package chat_distribuido;

import java.io.IOException;

/*
 * Clase principal que inicializa el cliente.
 */
public class MainClienteChat {
  public static void main(String[] args) throws IOException {

	  Cliente cli;
		try {
			cli = new Cliente();
			System.out.println("Starting session\n");
		    cli.startClient(); //Se inicia el cliente
		} catch (IOException e) {
			e.printStackTrace();
		} //Se crea el cliente
	  
  }
}