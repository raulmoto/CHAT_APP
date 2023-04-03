package chat_distribuido;

import java.io.DataInputStream;
import java.io.IOException;

public class HiloClienteEscucha extends Thread{
	
	private DataInputStream in;
	Cliente cliente;
	String nombreSala;
	
	public HiloClienteEscucha(DataInputStream in, Cliente cliente, String nombreSala) {
		this.in = in;
		this.cliente = cliente;
		this.nombreSala = nombreSala;
	}

	/*
	 * Pre: ---
	 * Post: Saluda al cliente indicándole el comando para salir de la sala.
	 * (El mensaje se leería desde el hiloClienteEscribe)
	 * Abre un bucle infinito, en el cuál, está siempre a la escucha de lo que envíe el servidor.
	 * Cada mensaje del servidor, se le imprime por pantalla al cliente 
	 * (Estos mensajes serían los mensajes escritos por los otros clientes en la sala.)
	 * Si recibe el mensaje LEAVE_ROOM, rompe el bucle y finaliza el método y el hilo.
	 */
	@Override
	public void run() {
		System.out.println("Welcome to room " + nombreSala
				+ "\ntype \"LEAVE_ROOM\" to exit"
				+ "\nMaximum message length: 140 characters");
		while(true) {
			
			try {
				String mensajeRecibido = cliente.desencriptar(in.readUTF());
				if(mensajeRecibido.equals("LEAVE_ROOM")) break; //Rompemos el hilo y dejamos la sala.
				System.out.println(mensajeRecibido);
			} catch (IOException e) {
				System.out.println("Error at receiving message from chat room " + nombreSala);
				e.printStackTrace();
			}
		}
	}
	
}
