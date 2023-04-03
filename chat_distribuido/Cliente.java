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
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;

/*
 * Pre: ---
 * Post: Clase que representa a cada cliente que se quiere conectar al servidor,
 * gestiona la comunicación con el servidor y las opciones que se muestran a 
 * cada cliente como interfaz de usuario.
 */
public class Cliente  extends Conexion{
    private String nombre;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private PublicKey serverPublicKey;
    private String salaID;
    DataInputStream in;
    DataOutputStream out;
    
	public Cliente() throws IOException {
		super("cliente");
	}
	
	/*
	 * Pre: ---
	 * Post: método que se ejecuta al iniciar el cliente, crea las claves
	 * pública y privada, crea los canales de entrada y salida para comunicarse
	 * con el servidor.
	 * Recibe e imprime el mensaje de confirmaciónd de conexión del servidor
	 * Obliga al usuario a establecer un nickName con el método selectNickname()
	 * Comienza un bucle while que le muestra el menú al usuario y le pide 
	 * escoger una opción.
	 * Llama al método adecuado según la opción escogida por el usuario, hasta que 
	 * este escoge cerrar sesión.
	 */
	public void startClient() {//Método para iniciar el cliente
		try {
            // Enviamos la clave pública del cliente al servidor y recibimos la del servidor.
            establecerCanalesYClaves();
            
            Scanner scanner = new Scanner(System.in);
            selectNickname(in, out, scanner);
            boolean cerrarSesion = false;
            while (!cerrarSesion) {
                System.out.println("BIENVENIDO AL CHAT !\n SELECCIONA EL NÚMERO DE LA OPCION :"
                		+ "\n\t1. Unirse a sala"
                		+ "\n\t2. Crear nueva sala"
                		+ "\n\t3. Listar salas"
                		+ "\n\t4. cambiar nombre"
                		+ "\n\t5. Desconectar");
                String opcion = scanner.nextLine().strip().toLowerCase();
            	switch (opcion) {
					case "1":
						seleccionarSala(scanner, out, in);
						break;
					case "2":
						crearNuevaSala(scanner, out, in);
						break;
					case "3":
						listarSalas(in, out);
						break;
					case "4":
						selectNickname(in, out, scanner);
						break;
					case "5":
						cerrarSesion(cs, scanner, out);
						cerrarSesion = true;
						break;
					default:
						System.out.println("OPCION NO VALIDA");
						break;
				}
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Pre: Se ha conectado un cliente al servidor
	 * Post: Establece los canales de entrada y salida, crea las 
	 * claves pública y privada, le envía la clave pública al servidor
	 * y recibe la clave pública del servidor.
	 */
	private void establecerCanalesYClaves() {
		try {
			// Generamos un par de claves RSA
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            //System.out.println("Clave public cliente: " + publicKey);
			in = new DataInputStream(cs.getInputStream());
			out = new DataOutputStream(cs.getOutputStream());
	        System.out.println(in.readUTF() + "\n\tSession INICIADA");
	        
	        //Convertimos la clave pública a byte[]
	        byte[] bytePublicKey = publicKey.getEncoded();
	        //System.out.println("\nBYTE KEY: " + bytePublicKey);
	        
	        //Clave en byte[] a String 
	        String strKey = Base64.getEncoder().encodeToString(bytePublicKey);
	        //Enviamos clave pública en string al server.
	        out.writeUTF(strKey);
	        
	        // Ahora recibimos la clave pública del server:
	        String serverPublicKeyString = in.readUTF();
	        // Convertir la clave pública del cliente de String a PublicKey
	        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(serverPublicKeyString);
	        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(serverPublicKeyBytes);
	        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	        this.serverPublicKey = keyFactory.generatePublic(publicKeySpec);
	        
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
	 * Post: Se le pide al cliente que diga un nombre de sala existente, 
	 * se comprueba que no tiene espacios, se envía este nombre al metodo unirseASala.
	 */
	private void seleccionarSala(Scanner scanner, DataOutputStream out, DataInputStream in) {
		String nombreSala = "";
		while(true) {
			System.out.println("\nInserte el nombre de la sala o  CANCELAR para volver al menú:");
			nombreSala = scanner.nextLine().strip();
			if(nombreSala.contains(" ")) {
				System.out.println("el nombre de la sala no puede contener espacios");
				continue;
			}else if(nombreSala.length()<1) {
				System.out.println("NOMBRE DE LA SALA NO ES VALIDO");
				continue;
			}else if(nombreSala.equals("CANCELAR")) return;
			break;
		}
		unirseASala(scanner, in, out, nombreSala);
	}

	/*
	 * Pre: El nombre de la sala ha sido introducido por el cliente
	 * Post: Se envía el nombre de la sala al servidor, junto con el comando JOIN_ROOM. 
	 * El servidor comprueba que la sala existe y que se tiene acceso 
	 * (a implementar comprobación de password). Si todo está bien
	 * El servidor envía un mensaje afirmativo y se actualiza la sala del cliente.
	 * A continuación se crean los hilos de escucua y escritura para el cliente, y se inician.
	 * Se espera hasta que los hilos finalicen, esto significa que el cliente se ha desconectado
	 * de la sala. Por último se elimina la sala anotada en el cliente.
	 *  
	 */
	public void unirseASala(Scanner scanner, DataInputStream in, DataOutputStream out, String nombreSala){
		try {
			out.writeUTF("UNIRSE_A_SALA " + nombreSala);
			String respuestaServidor = in.readUTF();
			// Se realiza la verificación de si la sala es privada, y en caso afirmativo, pide la contraseña
			String password = "";
			// Variable que usamos para saber si imprimirle al usuario el mensaje de contraseña incorrecta
			boolean primeraRonda = true;
			while (respuestaServidor.equals("INSERTAR_CONTRASEÑA")) {
				if (!primeraRonda) System.out.println("\nCONTRASEÑA INCORRECTA,  VUELVE A INTENTAR");
				while (true) {
					System.out.println("\nCONTRASEÑA PARA LA SALA o CANCELAR PARA SALIR:");
					password = scanner.nextLine().strip();
					if (password.contains(" ") || password.length() < 1) {
						System.out.println("CONTRASEÑA NO VALIDA, VUELVE A INTENTAR");
						primeraRonda = false;
						continue;
					}
					if (password.equals("CANCELAR")) {
						out.writeUTF("CANCELAR");
						System.out.println("Abortando unirse a la sala\n");
						return;
					} else {
						// Enviamos la contraseña hasheada al server
						out.writeUTF("" + password.hashCode());
					}
					respuestaServidor = in.readUTF();
					break;
				}
				primeraRonda = false;
				
			}
			if(respuestaServidor.equals("SALA_UNIDA")) {
				this.salaID = nombreSala;
				HiloClienteEscucha hiloClienteEscucha = new HiloClienteEscucha(in, this, nombreSala);  
				HiloClienteEscribe hiloClienteEscribe = new HiloClienteEscribe(scanner, out,this, nombreSala);
				hiloClienteEscucha.start();
				hiloClienteEscribe.start();
				hiloClienteEscribe.join();
				hiloClienteEscucha.join();
			}else {
				System.out.println("\nSALA NO ENCONTRADA\n");
			}
		} catch (IOException e1) {
			System.out.println("\nError al comunicarse con el servidor al intentar unirse a la sala");
			
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.salaID = null;
	}

	/*
	 * Pre: ---
	 * Post: Solicita al usuario un nombre de sala, 
	 * Pregunta al usuario si la sala es privada, si lo es, le solicita también 
	 * el password. Realiza una validación básica para el password.
	 * Una vez establecidos los parámetros, se le envían todos en un mensaje al servidor,
	 * junto con el comando NUEVA_SALA
	 *  
	 */
	public void crearNuevaSala(Scanner scanner, DataOutputStream out, DataInputStream in){
		System.out.println("\n NOMBRE DE LA SALA:");
		String nombreSala = "";
		//Se pide el nombre de la sala al cliente
		while(true) {
			nombreSala = scanner.nextLine().strip();
			if(nombreSala.contains(" ")) {
				System.out.println("EL nombre no debe contener espacios, "
						+ "elige otro nombre o corriga el anterior");
				continue;
			}else break;
		}
		//Se pregunta al cliente si la sala es privada, y si lo es, se le pide una contraseña
		String password = "";
		System.out.println("Hacer la sala privada? (Y/N)");
		String privada = scanner.nextLine().strip();
		boolean esPrivada = privada.equalsIgnoreCase("Y") || privada.equalsIgnoreCase("Yes");
		if(!esPrivada) {
			System.out.println("Configurando clase publica");
		}
		else {
			System.out.println("Configurando clase PRIVADA.INSERTE contrseña de cla clase:");
			while(true) {
				password = scanner.nextLine().strip();
				if(password.contains(" ")) {
					System.out.println("la contraseña no puede contener especios, "
							+ "elige otra contrseña o cirrige la anterior");
					continue;
				}else if(password.length() < 1) {
					System.out.println("contraseña demasiado corta, elige otra contraña");
					continue;
				}else break;
			}
		}
		try {
			out.writeUTF("NUEVA_SALA " + nombreSala + " " + esPrivada + " " + password.hashCode());
			String respuestaServidor = in.readUTF();
			if(respuestaServidor.equals("ERROR AL CREAR LA SALA")) 
				System.out.println("Error creando sala");
			else if(respuestaServidor.equals("LA SALA YA EXISTE")){
				System.out.println("nombre de la sala ya existe.");
			}else {
				System.out.println("Sala creada: " + nombreSala);
			}
		} catch (IOException e) {
			System.out.println("Error creando sala");
			e.printStackTrace();
		}
		listarSalas(in, out);
	}

	/*
	 * Pre: ---
	 * Envía al servidor el comando para listar las salas disponibles
	 * Recibe del servidor e imprime por pantalla, las id de las salas
	 * disponibles combinadas en un solo String.
	 */
	public void listarSalas(DataInputStream in, DataOutputStream out) {
		try {
			out.writeUTF("LISTAR_SALAS");
			System.out.println("\nSALAS disponibles: \n--");
			System.out.println(in.readUTF());
		} catch (IOException e) {
			System.out.println("Error listando salas");
			e.printStackTrace();
		}
		
	}

	/*
	 * Pre: El cliente quiere enviar un mensaje al servidor
	 * Post: Se encripta el mensaje y se devuelve para enviarlo al server. 
	 */
	public String encriptar(String mensaje) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
			byte[] bytesEncriptados = cipher.doFinal(mensaje.getBytes());
			String mensajeEncriptado = Base64.getEncoder().encodeToString(bytesEncriptados);
			return mensajeEncriptado;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Pre: El servidor ha enviado un mensaje encriptado
	 * Post: Se desencripta el mensaje y se devuelve desencriptado
	 */
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
	
	/*
	 * Pre: Los canales de entrada y salida de mensajes con el servidor han sido creados  
	 * Post: Este método pregunta al usuario por un nuevo nickname, se lo envía al servidor, 
	 * el servidor verifica en su lista de nombres activos, si el nombre está disponible,
	 * devuelve un mensaje afirmativo, sino, devuelve un mensaje negativo.
	 * El usuario deberá introducir nicknames de nuevo hasta que seleccione uno disponible.
	 */
	private void selectNickname(DataInputStream in, DataOutputStream out, Scanner scanner) {
		while(true) {
			System.out.println("\ninserte su nombre: ");
			String nickname = scanner.nextLine().strip();
			if(nickname.contains(" ")) {
				System.out.println("el nombre no puede contener espacios");
				continue;
			}
			String mensaje = "";
			//Comprobamos si el cliente ya tiene nombre para que pueda escoger el mismo si lo desea
			if(this.nombre == null) {
				mensaje = "ELEGIR_NOMBRE " + nickname;
			}
			else mensaje = "ELEGIR_NOMBRE " + nickname + " " + this.nombre;
			try {
				out.writeUTF(mensaje);
				String respuesta = in.readUTF();
				if(respuesta.equals("NOMBRE_ACTUALIZADO")) {
					this.nombre = nickname;
					System.out.println("NOMBRE ACTUALIZADO: " + nickname + "\n");
					break;
				} else if(respuesta.equals("ERROR")){
					System.out.println("NOMBRE NO VALIDO\n");
				}else System.out.println("EL NOMBRE YA ESTÁ EN USO\n");
			} catch (IOException e) {
				System.out.println("NOMBRE NO VALIDO, REINTENTA\n");
				e.printStackTrace();
			}
		}
	}

	/*
	 * Pre: El usuario ha seleccionado la opción de finalizar sesion
	 * Post: Se envía un mensaje al servidor que le indica que cierre la conexión
	 * con el cliente, se cierra la conexión con el servidor y el escáner.
	 */
	private void cerrarSesion(Socket cs, Scanner scanner, DataOutputStream out) {
		System.out.println("SESION FINALIZADO");
		
		try {
			out.writeUTF("FIN_SESION" );
			cs.close();
			scanner.close();
			System.out.println("DESCONECTADO");
		} catch (IOException e) {
			System.out.println("No se pudo finalizar la sesión, inténtalo de nuevo");
			e.printStackTrace();
		}
		
	}
	
	//Getters, Setters

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public String getSalaID() {
		return salaID;
	}

	public void setSalaID(String salaID) {
		this.salaID = salaID;
	}
	
}
