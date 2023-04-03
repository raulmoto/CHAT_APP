package chat_distribuido;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

/*
 * Pre: --- 
 * Post: Gestiona la comunicación de cada cliente con el servidor, 
 * 		 recibe los comandos del cliente y llama a métodos según lo requerido. 
 */
public class ThreadServidor extends Thread{
	protected Socket cs;
	protected ArrayList<ThreadServidor> hilosServidor;
	protected Map<String, Sala> salas;
	List<String> nombresClientes;
	private String nombreCliente;
	private Sala salaActual;
	DataInputStream in;
	DataOutputStream out;
	private PublicKey publicKey;
    private PrivateKey privateKey;
    private PublicKey clientPublicKey;
	
	public ThreadServidor() {
		super();
	}
	
	public ThreadServidor(Socket cs, ArrayList<ThreadServidor> hilosServidor, 
			Map<String, Sala> salas, List<String> nombresClientes) {
		super();
		this.cs = cs;
		this.hilosServidor = hilosServidor;
		this.salas = salas;
        this.nombresClientes = nombresClientes;
	}
	
	/*
	 * Pre: ---
	 * Post: Crea los canales de entrada y salida para comunicarse con el cliente.
	 * Abre un bucle infinito en el que espera un mensaje del cliente
	 * Al recibir mensaje del cliente, evalúa el mensaje y llama al método
	 * necesario en cada caso.
	 */
	@Override
    public void run() {
		try {
			establecerCanalesYClaves();
	        boolean cerrarSesion = false;
	        while (!cerrarSesion) {
	            String comandoCliente = in.readUTF(); // el comando de (nueva sala que manda) el cliente se gurada aqui
	            String[] tablaComandoCliente = comandoCliente.split(" ");
	            switch (tablaComandoCliente[0].strip()) {
	            case "ELEGIR_NOMBRE":
	            	selectNickname(in, out, tablaComandoCliente);
					break;
				case "UNIRSE_A_SALA":
					unirASala(in, out, tablaComandoCliente);
					break;
				case "NUEVA_SALA":
					crearSala(tablaComandoCliente);
					break;
				case "LISTAR_SALAS":
					listarSalas();
					
					break;
				case "FIN_SESION":
					cerrarSesion();
					cerrarSesion = true;
				default:
					out.writeUTF("INVALID_OPTION");
					break;
				}
	        }
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Pre: Se ha conectado un cliente al servidor
	 * Post: Establece los canales de entrada y salida, crea las 
	 * claves pública y privada, le envía la clave pública al cliente
	 * y recibe la clave pública del cliente.
	 */
	public void establecerCanalesYClaves() {
        try {
        	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            //Establece los canales de entrada y salida
        	this.in = new DataInputStream(cs.getInputStream());
            this.out = new DataOutputStream(cs.getOutputStream());
			out.writeUTF("peticion recibida con exito");
			
			//Convertimos la clave pública a byte[]
	        byte[] bytePublicKey = publicKey.getEncoded();
	        
	        //Clave en byte[] a String 
	        String strKey = Base64.getEncoder().encodeToString(bytePublicKey);
	        
	        // Convertir la clave pública del cliente de String a PublicKey
	        String clientPublicKeyString = in.readUTF();
	        byte[] publicKeyBytes = Base64.getDecoder().decode(clientPublicKeyString);
	        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        this.clientPublicKey = keyFactory.generatePublic(publicKeySpec);
	        
	        // Ahora enviamos la clave publica del server al cliente:
	        out.writeUTF(strKey);
	        
	        System.out.println("claves publicas compartidas\n");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Pre: ---
	 * Post: Crea un String que contiene los id de todas las 
	 * salas operativas en el momento. Si no hay salas o si hay un 
	 * error, se lo comunica al cliente.
	 */
	private void listarSalas() {
		String mensaje = "";
		List<Sala> salasAImprimir =  obtenerSalas();
		if(salasAImprimir.size() > 0)
			for(Sala sala : salasAImprimir) {
				mensaje+=sala.toString() + "\n";
			}
		else mensaje = "\nNo hay salas disponibles\n";
		try {
			out.writeUTF(mensaje);
		} catch (IOException e) {
			System.out.println("Error enviando lista de salas al cliente");
			e.printStackTrace();
		}
	}

	/*
	 * Pre: ---
	 * Post: Extrae el nombre de la nueva sala, si es privada, y la contraseña si
	 * existe, de la tabla que se le pasa. Llama al método agregarSala para 
	 * Agregar la sala creada a la lista. Envía un mensaje de confirmación al
	 * cliente, o de error en su caso.
	 */
	private void crearSala(String[] tablaComandoCliente) {
		String password = "";
		try {
			//Comprobamos que el nombre de la sala no está siendo usado
			//para cada sala en el arraylist, haz esto.
			for(Sala sala : new ArrayList<>(salas.values())) {
				if(sala.getNombre().equals(tablaComandoCliente[1])) {
					out.writeUTF("LA SALA YA EXISTE");
					return;
				}
			}
			//Si la longitud es 4, significa que se ha enviado una contraseña
			if(tablaComandoCliente.length == 4) {
				password = tablaComandoCliente[3].strip();
			}
			agregarSala(new Sala(tablaComandoCliente[1], 
					tablaComandoCliente[2].strip().equalsIgnoreCase("true"), password));
			out.writeUTF("SALA CREADA");
		} catch (Exception e) {
			try {
				out.writeUTF("ERROR AL CREAR LA SALA");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	/*
	 * Pre: El cliente ha introducido el comando para desconectarse
	 * Post: Cierra el socket cs y por tanto la comunicación con el cliente
	 */
	private void cerrarSesion() {
		System.out.println("Cliente logging out");
		try {
			cs.close();
			hilosServidor.remove(this);
			System.out.println("Clients online: " + hilosServidor.size());
		} catch (IOException e) {
			System.out.println("Error logging out client");
			e.printStackTrace();
		}
	}

	/*
	 * Pre: los canales de entrada y salida han sido establecidos e inicializados.
	 * Post: Comprueba que el nickname recibido no existe ya en la lista nombresClientes,
	 * Si lo está, envía el mensaje respectivo al cliente, si está libre, lo establece
	 * como nickname del cliente, y se lo comunica al cliente.
	 */
	private void selectNickname(DataInputStream in, DataOutputStream out, String[] tablaComandoCliente) {
		//Con este if, nos aseguramos de borrar el nick anterior del cliente, si tenía uno
		if(tablaComandoCliente.length == 3) nombresClientes.remove(tablaComandoCliente[2].strip());
		String newNickname = tablaComandoCliente[1].strip();
		boolean occupied = false;
		for(String nombre : nombresClientes) {
			if(nombre.equals(newNickname)) {
				occupied = true;
				break;
			}
		}
		try {
			if(occupied) {
				out.writeUTF("NICKNAME_OCCUPIED");
			}
			else {
				out.writeUTF("NOMBRE_ACTUALIZADO");
				this.nombreCliente = newNickname;
				this.nombresClientes.add(newNickname);
				System.out.println("nombre cliente actualizado: " + newNickname);
			}
		} catch (IOException e) {
			try {
				out.writeUTF("ERROR");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	/*
	 * Pre: ---
	 * Post: Busca la sala con el indicador que el cliente ha introducido, 
	 * si lo encuentra, guarda esta sala como propia y añade este hilo a la sala.
	 * Envía un mensaje de confirmación al cliente y llama al método enSala para empezar a chatear
	 * Al terminar, elimina el hilo de la sala y la sala del hilo, ya que se 
	 * entiende que el cliente ha escrito el comando para salir de la sala.
	 */
	private void unirASala(DataInputStream in, DataOutputStream out, String[] tablaComandoCliente) {
		Sala sala = salas.get(tablaComandoCliente[1].strip());
		try {
			if (sala != null) {
				// Se realiza la verificación de password de entrada a la sala.
				if(!verificarPassword(sala)) return;
				this.salaActual = sala;
				this.salaActual.getHilosServidor().add(this);
				out.writeUTF("SALA_UNIDA");
				//Se inicia el chat:
				enSala(in);
			}
			else {
				out.writeUTF("SALA NO ENCONTRADA");
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(this.salaActual != null) {
			this.salaActual.getHilosServidor().remove(this);
			//Si la sala se queda sin miembros, se elimina.
			if(this.salaActual.getHilosServidor().size() == 0) quitarSala(this.salaActual);
			this.salaActual = null;
		}
	}

	/*
	 * Pre: El cliente se ha unido a la sala exitosamente
	 * Post: Muestra un mensaje de confirmación en la consola del servidor
	 * inicia un bucle while para mantenerse atento a los mensajes enviados desde el cliente
	 * dicho mensaje se redirige a la sala para hacer el broadcast.
	 */
	private void enSala(DataInputStream in) {
		System.out.println("Cliente " + nombreCliente + " concectado a sala " + salaActual.getNombre());
		salaActual.enviarMensaje(nombreCliente, " uniendo a sala");
		while(true) {
			try {
				String mensaje = desencriptar(in.readUTF());
				if(mensaje.equals("LEAVE_ROOM")) {
					System.out.println("Cliente " + nombreCliente + " dejando sala " + salaActual.getNombre());
					out.writeUTF(encriptar("LEAVE_ROOM"));
					salaActual.enviarMensaje(nombreCliente, " left the chat");
					break;
				}else {
					salaActual.enviarMensaje(nombreCliente, mensaje);
				}
			} catch (IOException e) {
				System.out.println("Error al leer mensaje del cliente " + nombreCliente 
						+ " in room " + salaActual.getNombre());
				e.printStackTrace();
			}
		}
	}

    /*
     * Pre: Se ha creado la sala correctamente
     * Post: Agrega la sala a la lista local de salas
     */
    public void agregarSala(Sala sala) {
        salas.put(sala.getNombre(), sala);
    }

    /*
     * Pre: La sala existe
     * Post: Elimina una sala de la lista local.
     */
    public void quitarSala(Sala sala) {
        salas.remove(sala.getNombre());
    }

    /*
     * Pre: ---
     * Post: Devuelve una lista con las salas existentes
     */
    public List<Sala> obtenerSalas() {
        return new ArrayList<>(salas.values());
    }
    
    /*
     * Pre: El cliente está solicitando unirse a una sala
     * Post: Comprueba si la sala es privada, en caso de serlo, solicita la 
     * contraseña al cliente y la compara. devuelve true si el cliente
     * obtiene el acceso a la sala.
     */
    public boolean verificarPassword(Sala sala) {
    	// Se realiza la verificación de password de entrada a la sala.
		try {
			while(true) {
				if(sala.isPrivada()) {
					out.writeUTF("INSERTAR_CONTRASEÑA");
					String password = in.readUTF();
					if(password.equals("CANCELAR")){
						System.out.println("Abortando unirse a la sala");
						return false;
					}else if(!sala.verificarPassword(password)) continue;
					else break; // En este else, las passwords coinciden, así que rompe el bucle
				}else break; // Rompe el bucle si la sala no es privada (no tiene password).
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        return true;
    }
    
    /*
	 * Pre: El servidor quiere enviar un mensaje al cliente
	 * Post: Se encripta el mensaje y se devuelve para enviarlo al cliente. 
	 */
	public String encriptar(String mensaje) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
			byte[] bytesEncriptados = cipher.doFinal(mensaje.getBytes());
			String mensajeEncriptado = Base64.getEncoder().encodeToString(bytesEncriptados);
			return mensajeEncriptado;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String desencriptar(String mensajeEncriptado) {
	    try {
	        Cipher cipher = Cipher.getInstance("RSA");
	        cipher.init(Cipher.DECRYPT_MODE, privateKey);
	        byte[] mensajeBytes = Base64.getDecoder().decode(mensajeEncriptado);
	        byte[] bytesDesencriptados = cipher.doFinal(mensajeBytes);
	        return new String(bytesDesencriptados);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	public Socket getCs() {
		return cs;
	}

	public void setCs(Socket cs) {
		this.cs = cs;
	}

	public ArrayList<ThreadServidor> getHilosServidor() {
		return hilosServidor;
	}

	public void setHilosServidor(ArrayList<ThreadServidor> hilosServidor) {
		this.hilosServidor = hilosServidor;
	}

	public Map<String, Sala> getSalas() {
		return salas;
	}

	public void setSalas(Map<String, Sala> salas) {
		this.salas = salas;
	}

	public List<String> getNombresClientes() {
		return nombresClientes;
	}

	public void setNombresClientes(List<String> nombresClientes) {
		this.nombresClientes = nombresClientes;
	}

	public String getNombreCliente() {
		return nombreCliente;
	}

	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}

	public Sala getSalaActual() {
		return salaActual;
	}

	public void setSalaActual(Sala salaActual) {
		this.salaActual = salaActual;
	}

	public DataOutputStream getOut() {
		return out;
	}

	public void setOut(DataOutputStream out) {
		this.out = out;
	}

}
