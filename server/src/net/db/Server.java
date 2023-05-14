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
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {
	public static void main(String[] args) throws IOException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InvalidKeySpecException {

		//El informe se ejecutará cada mes
		ScheduledExecutorService ses =  Executors.newSingleThreadScheduledExecutor();
		ses.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					registro();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 0, 30, TimeUnit.DAYS);  

		ServerSocket serverSocket = openSocket();

    while (true) {

			System.err.println("Esperando conexiones de clientes...");

			// Comprobamos si ha habido mas de 3 peticiones en las ultimas 4 horas
			Integer numPeticiones = getPeticionesUltimasCuatroHoras();

			SSLSocket socket = (SSLSocket) serverSocket.accept();

			// Abrimos un BufferedReader para leer los datos del cliente y un PrintWriter para enviar datos al cliente
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			if(numPeticiones<3) {
				System.err.println("Nueva conexion entrante");
			
				// Obtenemos mensaje, firma y clave pública del cliente
				String mensaje = input.readLine();
				String firma = input.readLine();
				String publicKey = input.readLine();
									
				// Verificamos la firma del cliente
				Signature sg = Signature.getInstance("SHA256withRSA");
				RSAPublicKey pk = (RSAPublicKey) KeyFactory.getInstance("RSA")
						.generatePublic(new X509EncodedKeySpec(hexToBytes(publicKey)));

				sg.initVerify(pk);
				sg.update(mensaje.getBytes());
				Boolean verify = sg.verify(hexToBytes(firma));
				
				// Verificamos los valores del mensaje
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
				System.err.println("Sobrecarga de peticiones");
				System.out.println("Peticion rechazada por sobrecarga");
				output.println("Peticion rechazada por sobrecarga");
				output.flush();
				input.close();
				output.close();
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

        for (int i=0; i<l.size(); i++){
            String fechaPeticion = l.get(i).split(";")[1];
            LocalDateTime fp = LocalDateTime.parse(fechaPeticion, DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
            Integer mesPeticion =fp.getMonthValue();
            Integer mp = Integer.parseInt(mes);
            if(mp==mesPeticion){
                petValidas +=1;
            }
        }
        for (int i=0; i<q.size(); i++){
            String fechaPeticion = q.get(i).split(";")[1];
            LocalDateTime fp = LocalDateTime.parse(fechaPeticion, DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
            Integer mesPeticion =fp.getMonthValue();
            Integer mp = Integer.parseInt(mes);
            if(mp==mesPeticion){
                petInvalidas +=1;
            }
        }
        return new Double[] {petInvalidas, petValidas};

    }


      public static void registro() throws IOException {

    	String mes = String.format("%02d", LocalDate.now().getMonthValue());
    	int anyo = LocalDate.now().getYear();
    	
    	Double[] resultadosMes = getNumPeticionesEnMes(mes);
    	Double peticionesOk = resultadosMes[1];
    	Double peticionesTotal = peticionesOk + resultadosMes[0];
    	Double ratioMes = peticionesOk / peticionesTotal;
    	
    	Integer mesPrevio = (Integer.valueOf(mes) - 1) % 12;
    	Double[] resultadosMesPrevio = getNumPeticionesEnMes(mesPrevio.toString());
    	Double peticionesOkMesPrevio = resultadosMesPrevio[1];
    	Double peticionesTotalMesPrevio = peticionesOkMesPrevio + resultadosMesPrevio[0];
    	Double ratioMesPrevio = peticionesOkMesPrevio / peticionesTotalMesPrevio;
    	Integer mes2Previo = (Integer.valueOf(mes) - 2) % 12;
    	Double[] resultados2MesesPrevio = getNumPeticionesEnMes(mes2Previo.toString());
    	Double peticionesOk2MesPrevio = resultados2MesesPrevio[1];
    	Double peticionesTotal2MesPrevio = peticionesOk2MesPrevio + resultados2MesesPrevio[0];
    	Double ratio2MesPrevio = peticionesOk2MesPrevio / peticionesTotal2MesPrevio;
		
    	String tendencia = "0";
    	if (!ratio2MesPrevio.isNaN() && !ratioMesPrevio.isNaN()) {
    		if (ratioMes>ratioMesPrevio && ratioMes>ratio2MesPrevio 
    				|| ratioMes>ratioMesPrevio && ratioMes.equals(ratio2MesPrevio)
    				|| ratioMes.equals(ratioMesPrevio) && ratioMes>ratio2MesPrevio) {
    			tendencia = "+";
    		} else if (ratioMes<ratioMesPrevio || ratioMes<ratio2MesPrevio) {
    			tendencia = "-";
    		}
    	}
    	try {
			FileWriter fw = new FileWriter("registro.txt", true);
			BufferedWriter out = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(out);
			pw.println("Mes: " + mes + ", Ano: " + anyo + ", Ratio mensual: " + ratioMes + ", Tendencia: " + tendencia);
			pw.close();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
