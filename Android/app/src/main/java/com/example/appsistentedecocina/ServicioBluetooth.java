package com.example.appsistentedecocina;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServicioBluetooth extends Service {

    // variable global para ver si el servicio está iniciado
    public static boolean IS_STARTED = false;

    /* Constantes de broadcast */
    public static final int ACTION_ERROR = 100;
    public static final int ACTION_ESTADO = 101;
    public static final int ACTION_NOTIFICACION = 102;

    /* Constantes utilizadas para comunicación Bluetooth */
    public static final byte INACTIVO = -1;
    public static final byte LEER_UNICO_PROD = 1;
    public static final byte LEER_MULTI_PROD = 2;
    public static final byte ESPERAR_PRODUCTO = 3;
    public static final byte SENSAR_PESO = 4;
    public static final byte BAJAR_BRAZO = 5;
    public static final byte SENSAR_PESO_SINFIN = 6;
    public static final byte EXTRAER_PRODUCTO = 7;
    public static final byte SUBIR_BRAZO = 8;
    public static final byte ESPERAR_NO_PRODUCTO = 9;
    public static final byte CANT_NO_DISP = 10; //A
    public static final byte SETEAR_IDDISP = 90; //5A
    public static final byte ENVIAR_IDDISP = 91; //5B
    public static final byte VALIDAR_HUMEDAD = 92; //5C
    public static final byte BT_CONECTADO = 93; //5D
    public static final byte BT_DESCONECTADO = 94; //5E

    public static final byte GIRAR_SINFIN_HORARIO = 126;
    public static final byte GIRAR_SINFIN_ANTIHORARIO = 125;
    public static final byte DETENER_SINFIN = 124;
    public static final byte ENCENDER_LED = 123;

    /* Mapa de los estados con los mensajes a mostrar */
    Map<Integer, String> mapaEstados = new HashMap<Integer, String>();

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread connectedThread;
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private LocalBinder binder = new LocalBinder();

    // Texto y color del estado actual del arduino
    protected String textEstado = "Desconectado";
    protected int colorEstado = Color.RED;

    @Override
    public void onCreate() {
        Log.d("ServicioBluetooth", "servicio creado.");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        ServicioBluetooth getService() {
            return ServicioBluetooth.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IS_STARTED = true;
        Log.d("ServicioBluetooth", "servicio iniciando...");
        //obtengo el adaptador del bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Log.d("ServicioBluetooth", "servicio iniciando correctamente.");

        /* Construimos el mapa de estados */
        mapaEstados.put((int)INACTIVO, "Inactivo");
        mapaEstados.put((int)LEER_UNICO_PROD, "Leyendo producto");
        mapaEstados.put((int)LEER_MULTI_PROD, "Leyendo productos");
        mapaEstados.put((int)ESPERAR_PRODUCTO, "Esperando recipiente");
        mapaEstados.put((int)SENSAR_PESO, "Sensando peso");
        mapaEstados.put((int)BAJAR_BRAZO, "Bajando brazo");
        mapaEstados.put((int)SENSAR_PESO_SINFIN, "Sensando peso sinfin");
        mapaEstados.put((int)EXTRAER_PRODUCTO, "Extrayendo produtco");
        mapaEstados.put((int)SUBIR_BRAZO, "Subiendo brazo");
        mapaEstados.put((int)ESPERAR_NO_PRODUCTO, "Esperando");
        mapaEstados.put((int)CANT_NO_DISP, "Producto insuficiente");
        mapaEstados.put((int)SETEAR_IDDISP, "S_IDDISP");
        mapaEstados.put((int)ENVIAR_IDDISP, "E_IDDISP");
        mapaEstados.put((int)VALIDAR_HUMEDAD, "Validando humedad");
        mapaEstados.put((int)BT_CONECTADO, "Conectado");
        mapaEstados.put((int)BT_DESCONECTADO, "Desconectado");
        return START_STICKY;
    }

    public void escribir (String s) {
        if (connectedThread != null) {
            connectedThread.write(s);
            Log.d("ServicioBluetooth", "Mensaje enviado a Bluetooth: " + s);
        } else {
            enviarError("No hay conexión establecida con dispositivo Bluetooth.");
        }
    }

    public void escribirNum (Integer num) {
        if(connectedThread != null) {
            connectedThread.writeNum(num);
            Log.d("ServicioBluetooth", "Mensaje enviado a Bluetooth: " + num);
        } else {
            enviarError("No hay conexión establecida con dispositivo Bluetooth.");
        }
    }

    public void escribirByte (byte b) {
        if(connectedThread != null) {
            connectedThread.writeNum(b);
            Log.d("ServicioBluetooth", "Mensaje enviado a Bluetooth: " + b);
        } else {
            enviarError("No hay conexión establecida con dispositivo Bluetooth.");
        }
    }

    public void conectar (String btAddress) {
        if (btAdapter != null) {
            /* Se realiza la conexion del Bluetooth crea y se conectandose a a traves de un socket */
            BluetoothDevice device = btAdapter.getRemoteDevice(btAddress);

            /* Primero desconecto si existe alguna conexión */
            desconectar();

            /* Invoco a un SocketThread para la creación del socket Bluetooth,
               esto es porque tal proceso puede llegar a bloquear al thread
               principal si se intenta conectar a un dispositivo no válido. */
            new SocketThread(device).start();
        }
    }

    synchronized private void desconectar() {
        /* Cancelo el thread actual (si existe) */
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private class ConnectedThread extends Thread {
        public static final int CANT_BYTES_NUM = 4;
        public static final int TAM_BYTE = 8;
        public static final int CANT_PARTES_MENSAJE = 2;
        public static final int TAM_BUFFER = 1024;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket) {
            Log.d("ServicioBluetooth", "creando hilo secundario...");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("ServicioBluetooth", "fallo al crear hilo sencundario..." + e.getMessage());

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.d("ServicioBluetooth", "hilo secundario creado.");
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run() {
            long millis = System.currentTimeMillis();
            Log.d("ServicioBluetooth", "hilo secundario en ejecución.");
            byte[] buffer = new byte[TAM_BUFFER];
            String stringBuffer = new String();
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC06
            while (true) {
                try {
                    //se leen los datos del Bluetooth
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    stringBuffer += readMessage;
                    int index = stringBuffer.indexOf("\n");

                    if (index > 0) {
                        String cadena = stringBuffer.substring(0, index);
                        Log.d("ServicioBluetooth", "Cadena: " + cadena);
                        stringBuffer = stringBuffer.substring(index);

                        String params[] = cadena.split(":");
                        if (params.length == CANT_PARTES_MENSAJE) {
                            // el primer valor es la acción
                            int accion = Integer.valueOf(params[0]);
                            switch (accion) {
                                case ACTION_ERROR:
                                    enviarError(params[1]);
                                    break;
                                case ACTION_ESTADO:
                                    cambiarEstado(params[1]);
                                    break;
                                case ACTION_NOTIFICACION:
                                    enviarNotificacion(params[1]);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (index == 0) {
                        stringBuffer = stringBuffer.substring(1);
                    }
                } catch (IOException e) {
                    Log.d("ServicioBluetooth", "excepción en la ejecución del hilo secundario: " + e.getMessage());
                    break;
                }
            }
        }

        /* Método para escribir un mensaje al dispositivo Bluetooth */
        public void write (String input) {
            byte[] msgBuffer = input.getBytes();

            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d("ServicioBluetooth",
                        "excepción en escritura del hilo secundario: " + e.getMessage());

                enviarError("No se pudo enviar mensaje al dispositivo Bluetooth.");
            }
        }

        public void writeNum (int num) {
            byte[] byteBuffer = new byte[CANT_BYTES_NUM];

            for (int i = 0; i < CANT_BYTES_NUM; i++) {
                byteBuffer[i] = (byte) (num >> (TAM_BYTE * (CANT_BYTES_NUM - 1 - i)));
            }

            try {
                mmOutStream.write(byteBuffer);
            } catch (IOException e) {
                Log.d("ServicioBluetooth",
                        "excepción en escritura del hilo secundario: " + e.getMessage());

                enviarError("No se pudo enviar mensaje al dispositivo Bluetooth.");
            }
        }

        public void writeByte (byte b) {
            try {
                mmOutStream.write(b);
            } catch (IOException e) {
                Log.d("ServicioBluetooth",
                        "excepciónn en escritura del hilo secundario: " + e.getMessage());

                enviarError("No se pudo enviar mensaje al dispositivo Bluetooth.");
            }
        }

        public void cancel() {
            /* Intento cerrar el socket */
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e("ServicioBluetooth", "No se pudo cerrar el socket al desconectar.", e);
            }
        }
    }

    private void enviarError(String param) {
        Intent i = new Intent(String.valueOf(ACTION_ERROR));
        i.putExtra("Error", param);
        sendBroadcast(i);
    }

    private void enviarNotificacion(String param) {
        Intent i = new Intent(String.valueOf(ACTION_NOTIFICACION));
        i.putExtra("Notificacion", param);
        sendBroadcast(i);
    }

    private void cambiarEstado(String param) {
        int estado = Integer.valueOf(param);
        textEstado = mapaEstados.get(estado);
        if (estado == (int)BT_DESCONECTADO) {
            colorEstado = Color.RED;
        } else {
            colorEstado = Color.YELLOW;
        }
        enviarEstado();
    }

    public void enviarEstado () {
        Intent i = new Intent(String.valueOf(ACTION_ESTADO));
        Log.d("ServicioBluetooth", String.valueOf(ACTION_ESTADO));
        i.putExtra("Estado", textEstado);
        i.putExtra("Color", colorEstado);
        sendBroadcast(i);
    }

    class SocketThread extends Thread {
        BluetoothDevice device;

        SocketThread(BluetoothDevice device) {
            this.device = device;
        }

        public void run() {
            /* Intento crear socket */
            try {
                btSocket = device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
            } catch (IOException e) {
                enviarError("No se pudo establecer la conexión con el dispositivo Bluetooth.");

                Log.d("ServicioBluetooth", "Error creación socket: " + e.getMessage());
                return;
            }

            /* Intento establecer conexión */
            try {
                btSocket.connect();
            } catch (IOException e) {
                /* Aviso que no se pudo conectar */
                enviarError("No se pudo establecer la conexión con el dispositivo Bluetooth.");
                Log.d("ServicioBluetooth", "Error conexión socket: " + e.getMessage());

                /* Intento cerrar el socket */
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    /* Ni me gasto en avisar nada, si llegamos acá estamos hasta las manos */
                    Log.d("ServicioBluetooth", "Error al cerrar socket: " + e.getMessage());
                }

                return;
            }

            /* Si llegamos acá es porque la conexión se pudo establecer */
            connectedThread = new ConnectedThread(btSocket);
            connectedThread.start();

            /* Se envia el carácter que inicializa el embebido */
            connectedThread.writeByte(BT_CONECTADO);

            return;
        }
    }
}
