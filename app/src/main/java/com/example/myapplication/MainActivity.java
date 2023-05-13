package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String server = "10.0.2.2";
    protected static int port = 7070;
    protected static List<Integer>  numerosCliente = new ArrayList<>();
    protected static Map<Integer, KeyPair> keyList = new HashMap<>();
    protected static Integer[] messageBody;

    static {
        messageBody = new Integer[4];
    }

    protected static byte[]  firma;
    protected static String respuesta = "";
    protected static Integer numClienteActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Se especifican los numeros de cliente valdios
        poblarNumerosCliente();

        // Creamos las keys para varios clientes cada uno con un número de cliente asociado (los que están declarado en el atributo numerosCliente)
        try {
            createKeys();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Capturamos el boton de Enviar
        View button = findViewById(R.id.button_send);

        // Llama al listener del boton Enviar
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

    }

    // Se añaden los numeros de cliente validos
    private void poblarNumerosCliente() {
        numerosCliente.add(7);
        numerosCliente.add(123);
        numerosCliente.add(256);
    }

    // Creación de las claves
    private void createKeys() throws NoSuchAlgorithmException {
        for(Integer i: numerosCliente) {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(2048);
            KeyPair keys = keygen.generateKeyPair();
            keyList.put(i, keys);
        }
    }

    // Creación de un cuadro de dialogo para confirmar pedido
    private void showDialog() throws Resources.NotFoundException {
        boolean enviar = true;

        EditText campoCamas = (EditText) findViewById(R.id.camas);
        String camas = String.valueOf(campoCamas.getText());
        Boolean noCamas = camas.equals("");

        EditText campoMesas = (EditText) findViewById(R.id.mesas);
        String mesas = String.valueOf(campoMesas.getText());
        Boolean noMesas = mesas.equals("");

        EditText campoSillas = (EditText) findViewById(R.id.sillas);
        String sillas = String.valueOf(campoSillas.getText());
        Boolean noSillas = sillas.equals("");

        EditText camposSillones = (EditText) findViewById(R.id.sillones);
        String sillones = String.valueOf(camposSillones.getText());
        Boolean noSillones = sillones.equals("");

        EditText campoNumCliente = (EditText) findViewById(R.id.editTextNumber3);
        String numCLienteValue = String.valueOf(campoNumCliente.getText());

        if (noCamas && noMesas && noSillas && noSillones) {
            // Mostramos un mensaje emergente;
            Toast.makeText(getApplicationContext(), "Debe especificar al menos un elemento", Toast.LENGTH_SHORT).show();
        } else if (numCLienteValue.length()==0) {
            // Mostramos un mensaje emergente;
            Toast.makeText(getApplicationContext(), "El número de cliente es obligatorio", Toast.LENGTH_SHORT).show();
        } else {

            if(!noCamas) {
                int numCamas = Integer.parseInt(camas);
                if(numCamas<0 || numCamas>300) {
                    Toast.makeText(getApplicationContext(), "Las cantidades deben estar comprendidas entre 0 y 300", Toast.LENGTH_SHORT).show();
                    enviar = false;
                    messageBody[0] = 0;
                } else {
                    messageBody[0] = numCamas;
                }
            } else {
                messageBody[0] = 0;
            }

            if(!noMesas) {
                int numMesas = Integer.parseInt(mesas);
                if(numMesas<0 || numMesas>300) {
                    Toast.makeText(getApplicationContext(), "Las cantidades deben estar comprendidas entre 0 y 300", Toast.LENGTH_SHORT).show();
                    enviar = false;
                    messageBody[1] = 0;
                } else {
                    messageBody[1] = numMesas;
                }
            } else {
                messageBody[1] = 0;
            }

            if(!noSillas) {
                int numSillas = Integer.parseInt(sillas);
                if(numSillas<0 || numSillas>300) {
                    Toast.makeText(getApplicationContext(), "Las cantidades deben estar comprendidas entre 0 y 300", Toast.LENGTH_SHORT).show();
                    enviar = false;
                    messageBody[2] = 0;
                } else {
                    messageBody[2] = numSillas;
                }
            } else {
                messageBody[2] = 0;
            }

            if(!noSillones) {
                int numSillones = Integer.parseInt(sillones);
                if(numSillones<0 || numSillones>300) {
                    Toast.makeText(getApplicationContext(), "Las cantidades deben estar comprendidas entre 0 y 300", Toast.LENGTH_SHORT).show();
                    enviar = false;
                    messageBody[3] = 0;
                } else {
                    messageBody[3] = numSillones;
                }
            } else {
                messageBody[3] = 0;
            }

            final int numCliente = Integer.parseInt(numCLienteValue);
            if (numCliente<0 || numCliente>300 || !numerosCliente.contains(numCliente)) {
                // Mostramos un mensaje emergente;
                Toast.makeText(getApplicationContext(), "Número de cliente incorrecto", Toast.LENGTH_SHORT).show();
                enviar = false;
            } else {
                numClienteActual = numCliente;
            }

            if(enviar) {
                new AlertDialog.Builder(this)
                        .setTitle("Enviar")
                        .setMessage("Se va a proceder al envio")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    // Catch ok button and send information
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        // 1. Extraer los datos de la vista

                                        // Ya los tenemos en messageBody

                                        // 2. Firmar los datos

                                        // Con el número de cliente sacamos el par de claves
                                        KeyPair kp = keyList.get(numCliente);
                                        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

                                        try {
                                            Signature sg = Signature.getInstance("SHA256withRSA");
                                            sg.initSign(privateKey);
                                            sg.update(Arrays.toString(messageBody).getBytes());
                                            firma = sg.sign();
                                        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                                            e.printStackTrace();
                                        }

                                        // 3. Enviar los datos
                                        respuesta = "";
                                        try {
                                            new Connection().execute().get();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        Toast.makeText(MainActivity.this, "Petición enviada correctamente", Toast.LENGTH_SHORT).show();
                                        Toast.makeText(MainActivity.this, "Respuesta del servidor: " + respuesta, Toast.LENGTH_SHORT).show();
                                        respuesta = "";
                                    }
                                }

                        ).setNegativeButton(android.R.string.no, null).show();
            }
        }
    }

    // Metodo para pasar la firma a HEX
    public static String bytesToHex(byte[] bytes) {
        BigInteger bigInteger = new BigInteger(1, bytes);
        String hexadecimal = bigInteger.toString(16);
        // Asegurarse de que la cadena tenga la longitud adecuada
        int longitudDeseada = (bytes.length * 2);
        while (hexadecimal.length() < longitudDeseada) {
            hexadecimal = "0" + hexadecimal;
        }
        return hexadecimal;
    }

    class Connection extends AsyncTask<String , String , String> {

        @Override
        protected String doInBackground(String... strings) {
            try {

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                InputStream i = getResources().openRawResource(R.raw.keystore);
                keyStore.load(i, "complexpassword".toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, "complexpassword".toCharArray());
                TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tm.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(keyManagerFactory.getKeyManagers(), tm.getTrustManagers(), null);
                SSLSocketFactory socketFactory =sslContext.getSocketFactory();
                SSLSocket socket = (SSLSocket) socketFactory.createSocket();

                socket.connect(new InetSocketAddress("10.0.2.2" ,7070), 2000);
                socket.startHandshake();

                // Si el servidor acepta la conexion
                if(socket.isConnected()) {

                    // Crea un PrintWriter para enviar datos al servidor
                    PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                    // Crea un objeto BufferedReader para leer la respuesta del servidor
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Usuario transmite el mensaje
                    output.println(Arrays.toString(messageBody));
                    output.flush();

                    // Usuario transmite la firma del mensaje
                    output.println(bytesToHex(firma));
                    output.flush();
                    firma = new byte[]{};

                    // Usuario transmite su clave pública
                    RSAPublicKey pk = (RSAPublicKey) keyList.get(numClienteActual).getPublic();
                    numClienteActual = null;
                    output.println(bytesToHex(pk.getEncoded()));
                    output.flush();

                    // Lee la respuesta del servidor
                    respuesta = input.readLine();

                    // Se cierra la conexion
                    output.close();
                    input.close();
                } else {
                    respuesta = "Peticion rechazada por sobrecarga";
                }
                socket.close();

            } catch (Exception Exception) {
                Exception.printStackTrace();
                respuesta = "Error en la trasnmision";
            }
            return null;
        }

    }

}

