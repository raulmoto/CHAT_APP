package chat_distribuido;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Sala {
	private String idid;
    private String nombre;
    private boolean privada;
    private String password;
    private List<String> nombresClientes;
    private ArrayList<ThreadServidor> hilosServidor;

	public Sala(String nombre, boolean privada, String password) {
		this.idid = generateRandomID();
        this.nombre = nombre;
        this.privada = privada;
        if(privada) this.password = password;
        this.nombresClientes = new ArrayList<>();
        hilosServidor = new ArrayList<>();
    }

    public boolean verificarPassword(String password) {
        return this.password.equals(password);
    }
    
    public void agregarNombreCliente(String nombreCliente) {
    	nombresClientes.add(nombreCliente);
    }

    public void quitarNombreCliente(String nombreCliente) {
    	nombresClientes.remove(nombreCliente);
    }
    
    public static String generateRandomID() {
	    int leftLimit = 97; // letter 'a'
	    int rightLimit = 122; // letter 'z'
	    int targetStringLength = 5;
	    Random random = new Random();

	    String generatedString = random.ints(leftLimit, rightLimit + 1)
	      .limit(targetStringLength)
	      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
	      .toString();

	    return generatedString;
	}


    public void enviarMensaje(String remitente, String mensaje) {
	    for(ThreadServidor hiloServidor : hilosServidor) {
			if(!hiloServidor.getNombreCliente().equals(remitente)) {
				try {
					hiloServidor.getOut().writeUTF(hiloServidor.encriptar(remitente + ": " + mensaje));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }
    
	public String getIdid() {
		return idid;
	}

	public void setIdid(String idid) {
		this.idid = idid;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public boolean isPrivada() {
		return privada;
	}

	public void setPrivada(boolean privada) {
		this.privada = privada;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public List<String> getNombresClientes() {
		return nombresClientes;
	}

	public void setNombresClientes(List<String> nombresClientes) {
		this.nombresClientes = nombresClientes;
	}

	public ArrayList<ThreadServidor> getHilosServidor() {
		return hilosServidor;
	}

	public void setHilosServidor(ArrayList<ThreadServidor> hilosServidor) {
		this.hilosServidor = hilosServidor;
	}

	@Override
	public String toString() {
		return "Sala [name: " + nombre + ", is privada: " + privada + ", clients online: " + hilosServidor.size() + "]";
	}
	
	
    
}
