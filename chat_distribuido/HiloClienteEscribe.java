package chat_distribuido;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class HiloClienteEscribe extends Thread{
	private Scanner scanner; 
	private DataOutputStream out;
	Cliente cliente;
	String nombreSala;
	
	public HiloClienteEscribe(Scanner scanner, DataOutputStream out, Cliente cliente, String nombreSala) {
		this.scanner = scanner;
		this.out = out;
		this.cliente = cliente;
		this.nombreSala = nombreSala;
	}

	/*
	 * Pre: ---
	 * Post: Abre un bucle infinito en el cuál 
	 * está siempre esperando que el cliente escriba una línea.
	 * Cada línea escrita por el cliente, se envía al servidor.
	 * si el cliente escribe el mensaje LEAVE_ROOM, rompe el bucle y finaliza el hilo.
	 */
	@Override
	public void run() {
		while(true) {
			String mensaje = scanner.nextLine().strip();
			try {
				if(mensaje.length() <= 140) {
					/*NOTA: Este mensaje hay que encriptarlo, y añadirle el comando adecuado, 
					 * el nombre del remitente y el nombre de la sala.*/
					out.writeUTF(cliente.encriptar(mensaje));
					//Se muestra por consola el mensaje escrito por el propio cliente, como en un chat
				}else System.out.println("Error: max message length exceeded");
				if(mensaje.equals("LEAVE_ROOM")) {
					System.out.println("Leaving room " + nombreSala);
					break;
				}else System.out.println("Me: " + mensaje);
			} catch (IOException e) {
				System.out.println("Failed to send message to room " + nombreSala);
				e.printStackTrace();
			}
		}
		
	}
	
}
