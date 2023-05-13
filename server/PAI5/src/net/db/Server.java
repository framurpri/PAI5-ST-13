package net.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {

private static Boolean socketCerrado = true; 
	private static Boolean sobrecarga = false;

	final static String clave = "5mBwvMUjCxoRGQcCtNgLvThAWE1Zk4w+rcya9hda3Lll_hkhftCcA6qRbmwr+OOyh4jMIqN5iBvQFrBv6X01BuIOhc8jxFa4mO36bGG1DE6ucVEOUdiJ5doDuwlWm2d_W8TaB6xy43_fylXMlfAWPosbXw22RT7CWYD3wYuN5Jk=";
	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public static void main(String[] args) throws IOException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException {

    ServerSocket serverSocket = null;


		// Programamos que se ejecute el informe cada mes
		ScheduledExecutorService ses =  Executors.newSingleThreadScheduledExecutor();
		ses.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				try {
					informe();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0, 30, TimeUnit.DAYS);  

    while (true) {
			sobrecarga = getPeticionesUltimasCuatroHoras() >= 3;
			if(socketCerrado && !sobrecarga) {
				serverSocket = openSocket();
				socketCerrado = false;
			}
			// Espera las peticiones del cliente para comprobar mensaje/MAC
			try {
				System.err.println("Esperando conexiones de clientes...");
				// Miramos si ha habido mas de 3 peticiones en las ultimas 4 horas
				Integer numPeticiones = getPeticionesUltimasCuatroHoras();
				if(numPeticiones<3) {
					SSLSocket socket = (SSLSocket) serverSocket.accept();
					System.err.println("Nueva conexion entrante");
				
					// Abre un BufferedReader para leer los datos del cliente
					BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					// Abre un PrintWriter para enviar datos al cliente
					PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
	
					// Se lee mensaje, firma y public key
					String mensaje = input.readLine();
					String firma = input.readLine();
					String publicKey = input.readLine();
										
					// Verifica los datos
					Signature sg = Signature.getInstance("SHA256withRSA");
					RSAPublicKey pk = (RSAPublicKey) KeyFactory.getInstance("RSA")
							.generatePublic(new X509EncodedKeySpec(hexToBytes(publicKey)));
					sg.initVerify(pk);
					sg.update(mensaje.getBytes());
					Boolean verify = sg.verify(hexToBytes(firma));
					
					// Y mira que sean validos: mensaje en formato "[numMesas, numMesas, numSillas, numSillones]"
					String sinCorchetes = mensaje.substring(1, mensaje.length() - 1);
					String[] split = sinCorchetes.split(",");
					Boolean valido = true;
					for(String s: split) {
						Integer aux = Integer.parseInt(s.trim());
						if(aux<0 || aux>300) {
							valido = false;
							break;
						}
					}
					if (verify && valido) {
						System.out.println("Peticion OK");
						output.println("Peticion OK");
						output.flush();
						savePeticion("Validas", split);
					} else {
						System.out.println("Peticion INCORRECTA");
						output.println("Peticion INCORRECTA");
						output.flush();
						savePeticion("Invalidas", split);
					}
					
					input.close();
					output.close();
				} else {

					if(!socketCerrado) {
						serverSocket.close();	
					}
					socketCerrado = true;
					TimeUnit.SECONDS.sleep(30);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
	}

        public static ServerSocket openSocket() throws IOException {
            System.setProperty("javax.net.ssl.keyStore", "keystore.p12");
            System.setProperty("javax.net.ssl.keyStorePassword", "complexpassword");
            SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) socketFactory.createServerSocket(7070);
            serverSocket.setEnabledProtocols(new String[] { "TLSv1.3" });
            return serverSocket;
        }

				public static byte[] hexToBytes(String hexadecimal) {
					int longitud = hexadecimal.length();
					byte[] bytes = new byte[longitud / 2];
	
					for (int i = 0; i < longitud; i += 2) {
							String byteHex = hexadecimal.substring(i, i + 2);
							byte b = (byte) Integer.parseInt(byteHex, 16);
							bytes[i / 2] = b;
					}
					return bytes;
			}



    public static void savePeticion(String validacion, String[] peticion) throws IOException{
       String peticionFinal = "";

       //Accedemos a la ruta de la carpeta
       String rutaArchivo = System.getProperty("user.dir") +"\\resources\\peticiones" + 
       validacion + "\\";
       File folder = new File(rutaArchivo);
       Integer numeroPeticiones = folder.listFiles().length;
       peticionFinal += numeroPeticiones+1 + ";";
       
       String hactual =  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
       peticionFinal += hactual + ";";

       for(int i=0; i<peticion.length; i++) {
           peticionFinal+=peticion[i].trim()+","; 
    }
       peticionFinal += ";" + validacion;

       File archivo = new File(rutaArchivo + peticionFinal);
       //Guardamos el nonce en dicha carpeta.
       archivo.createNewFile();
       
       //Y escribimos en el interior de la carpeta el nombre del log.
       FileWriter escritor = new FileWriter(archivo);
			 String res = "";
			 String[] recursos = {"Camas", "Mesas", "Sillas", "Sillones"};
			 for(int i=0; i<peticion.length; i++) {
				 res+=recursos[i]+"="+peticion[i].trim()+", "; 
			 }
       escritor.write(res);
       escritor.close();
    }
    

    public static List<String> getPeticiones(String validacion) throws IOException{
        
        List<String> l = new ArrayList<String>();
        String ruta = System.getProperty("user.dir") +"\\resources\\";
        File folder = new File(ruta + "peticiones" + validacion + "\\");

        File[] files = folder.listFiles();

        for (File file : files) {
            l.add(file.getName());
        }

        return l;
    }

    public static Integer getPeticionesUltimasCuatroHoras() throws IOException{
        Integer acum = 0;
        List<String> l = getPeticiones("validas");
        List<String> q = getPeticiones("invalidas");
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

        for (int i=0; i<l.size(); i++){
            long intervalo = getMinutesBetweenDates(l.get(i).split(";")[1], LocalDateTime.now().format(formato));
            if(intervalo<14400){
                acum +=1;
            }
        }
        for (int i=0; i<q.size(); i++){
            long intervalo = getMinutesBetweenDates(q.get(i).split(";")[1], LocalDateTime.now().format(formato));
            if(intervalo<14400){
                acum +=1;
            }
        }
        return acum;
    }

    public static long getMinutesBetweenDates(String date1, String date2) {
        LocalDateTime dateTime1 = LocalDateTime.parse(date1, DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
        LocalDateTime dateTime2 = LocalDateTime.parse(date2, DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
        return ChronoUnit.SECONDS.between(dateTime1, dateTime2);
    }


    public static Double[] getNumPeticionesEnMes(String mes) throws IOException {
        Double petValidas = 0.;
        Double petInvalidas = 0.;
        List<String> l = getPeticiones("validas");
        List<String> q = getPeticiones("invalidas");
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

        for (int i=0; i<l.size(); i++){
            long intervalo = getMinutesBetweenDates(l.get(i).split(";")[1], LocalDateTime.now().format(formato));
            if(intervalo<108000){
                petValidas +=1;
            }
        }
        for (int i=0; i<q.size(); i++){
            long intervalo = getMinutesBetweenDates(q.get(i).split(";")[1], LocalDateTime.now().format(formato));
            if(intervalo<14400){
                petInvalidas +=1;
            }
        }
        return new Double[] {petInvalidas, petValidas};
    
    }


      public static void informe() throws IOException {
    	Map<String, String> traductor = new HashMap<>();
    	traductor.put("JANUARY", "01");
    	traductor.put("FEBRUARY", "02");
    	traductor.put("MARCH", "03");
    	traductor.put("APRIL", "04");
    	traductor.put("MAY", "05");
    	traductor.put("JUNE", "06");
    	traductor.put("JULY", "07");
    	traductor.put("AUGUST", "08");
    	traductor.put("SEPTEMBER", "09");
    	traductor.put("OCTOBER", "10");
    	traductor.put("NOVEMBER", "11");
    	traductor.put("DICEMBER", "12");
    	String mes = LocalDate.now().getMonth().toString();
    	String numeroMes = traductor.get(mes);
    	int anyo = LocalDate.now().getYear();
    	
    	Double[] resultadosMes = getNumPeticionesEnMes(numeroMes);
    	Double peticionesOk = resultadosMes[1];
    	Double peticionesTotal = peticionesOk + resultadosMes[0];
    	Double ratioMes = peticionesOk / peticionesTotal;
    	
    	Integer mesPrevio = (Integer.valueOf(numeroMes) - 1) % 12;
    	String mesPrevioFormatoBD = String.format("%02d", mesPrevio);
    	Double[] resultadosMesPrevio = getNumPeticionesEnMes(mesPrevioFormatoBD);
    	Double peticionesOkMesPrevio = resultadosMesPrevio[1];
    	Double peticionesTotalMesPrevio = peticionesOkMesPrevio + resultadosMesPrevio[0];
    	Double ratioMesPrevio = peticionesOkMesPrevio / peticionesTotalMesPrevio;
    	
    	Integer mes2Previo = (Integer.valueOf(numeroMes) - 2) % 12;
    	String mes2PrevioFormatoBD = String.format("%02d", mes2Previo);
    	Double[] resultados2MesesPrevio = getNumPeticionesEnMes(mes2PrevioFormatoBD);
    	Double peticionesOk2MesPrevio = resultados2MesesPrevio[1];
    	Double peticionesTotal2MesPrevio = peticionesOk2MesPrevio + resultados2MesesPrevio[0];
    	Double ratio2MesPrevio = peticionesOk2MesPrevio / peticionesTotal2MesPrevio;
    	    	
    	String tendencia = "0";
    	if (!ratio2MesPrevio.isNaN() && !ratioMesPrevio.isNaN()) {
    		if (ratioMes>ratioMesPrevio && ratioMes>ratio2MesPrevio 
    				|| ratioMes>ratioMesPrevio && ratioMes==ratio2MesPrevio
    				|| ratioMes==ratioMesPrevio && ratioMes>ratio2MesPrevio) {
    			tendencia = "+";
    		} else if (ratioMes<ratioMesPrevio || ratioMes<ratio2MesPrevio) {
    			tendencia = "-";
    		}
    	}
    	try {
			FileWriter fw = new FileWriter("registro.txt", true); // true para aï¿½adir a lo que ya haya
			BufferedWriter out = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(out);
			pw.println("Mes: " + numeroMes + ", Ano: " + anyo + ", Ratio mensual: " + ratioMes + ", Tendencia: " + tendencia);
			pw.close();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
