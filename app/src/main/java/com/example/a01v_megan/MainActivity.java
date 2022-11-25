package com.example.a01v_megan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;

// Actividad principal, aquí se hace uso de la herencia de AppCompatActivity y hacemos uso de la
// clase abstracta TextToSpeech.OnInitListener (API de voz)
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    /**     Variables      **/
    // Código solicitado para el reconocedor de voz (cualquier numero)
    private static final int RECONOCEDOR_VOZ = 7;
    // Código solicitado para el intent de llamada
    private static final int PERMISSION_CODE = 100;
    // TextView para la entrada de datos
    private TextView escuchando;
    // TextView para la salida de datos
    private TextView respuesta;
    // ImageView para la foto
    private ImageView foto;
    // ArrayList donde se encontrara el diccionario de datos local
    private ArrayList<Respuesta> diccionarioDatos;
    // Instancia de la clase TextToSpeech, dira la respuesta al usuario
    private TextToSpeech leerRespuesta;
    // Bitmap para guardar la imagen
    Bitmap bitmap;

    /** Método principal que se ejecutara al abrir la aplicación **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Llamada al método inicializar
        inicializar();

        // Para confirmar el permiso al usuario sobre hacer llamadas en su dispositivo
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_CODE);
        }
        // Para confirmar el permiso de escribir en la memoria
        if(ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        }
        // Para confirmar el permiso al usuario sobre hacer usar la cámara en su dispositivo
        if(ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 102);
        }
    }

    /** Método donde se reconocera a los intents ejecutados por la aplicación **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Para saber si se ejecuto el intent de escuchar la voz
        if(resultCode == RESULT_OK && requestCode == RECONOCEDOR_VOZ){
            // Creamos un objeto ArrayList donde guardaremos todas las respuestas preestablecidas en forma de token
            ArrayList<String> reconocido = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // Asignamos el token reconocido
            String escuchado = reconocido.get(0);
            // Escribimos lo escuchado en el TextView
            escuchando.setText(escuchado);
            // Método para procesar la consulta
            prepararRespuesta(escuchado);
        }
        // Si se ejecuto el intent de abrir la camara
        if(requestCode == 101) {
            if(resultCode == Activity.RESULT_OK && data != null) {
                // Guardamos en un bipmap los datos que se extraeran de la foto capturada
                bitmap = (Bitmap) data.getExtras().get("data");
                // Dibujamos la imagen en un ImageView
                foto.setImageBitmap(bitmap);
                // Llamada al método para guardar la imagen en el dispositivo
                guardarFoto();
            }
        }
    }

    /** Método para abrir la cámara del celular **/
    private void abrirCamara() {
        // Creamos el intent en el que asignaremos la acción de tomar una captura
        Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Nos aseguramoes de que exista una actividad que maneje el intent
        if (intentCamara.resolveActivity(getPackageManager()) != null) {
            // Se inicia la actividad
            startActivityForResult(intentCamara, 101);
        }
    }

    /** Método para guardar la imagen **/
    private void guardarFoto() {
        // Creamos un OutputStream para el flujo de datos
        OutputStream opS = null;
        // Creamos un file para la salida de datos (la imagen)
        File file = null;

        // Si es verdadero se usara el método para las APIS más recientes
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Caracteristicas del archivo
            ContentValues values = new ContentValues();
            // Para procesar los datos
            ContentResolver resolver = getContentResolver();

            // Nombre del archivo obteniendo la hora actual en la que se creo el archivo
            String fileName = System.currentTimeMillis() + "_img";

            // Guardamos las propiedades del archivo
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName); //Nombre
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg"); //Tipo MIME
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp"); //Ruta
            values.put(MediaStore.Images.Media.IS_PENDING, 1); //Si se ha terminado (1->no)

            // Coleccion para la API
            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            // Procesar los valores
            Uri imageUri = resolver.insert(collection, values);

            try {
                // Guardar en el flujo de datos la colección (URI)
                opS = resolver.openOutputStream(imageUri);
            } catch (FileNotFoundException e) {
                // Si hay error
                e.printStackTrace();
            }

            // Limpiamos valores
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0); //0->la imagen se termino de procesar
            // Actualizar los datos nuevos
            resolver.update(imageUri, values, null, null);
        } else { // Si es falso entonces se usara el método para lás APIS mas antiguas
            // Directorio de la imagen
            String imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            // Nombre del archivo
            String fileName = System.currentTimeMillis() + ".jpeg";
            // Creamos un nuevo archivo
            file = new File(imageDir, fileName);

            try {
                // Guardamos en el flujo de datos
                opS = new FileOutputStream(file);
            } catch (FileNotFoundException err) {
                // Si hay error
                err.printStackTrace();
            }
        }
        // Comprimimos la imagen para darle calidad
        boolean saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, opS);

        // Si es verdadero quiere decir que la imagen se guardo correctamente
        if(saved)
            // Mensaje
            Toast.makeText(this, "Imagen guardada", Toast.LENGTH_SHORT).show();
        // Si es diferente de null entonces se han guardado datos en el flujo de datos
        if(opS != null) {
            try {
                // Limpiamos y cerramos el buffer
                opS.flush();
                opS.close();
            } catch (IOException e) {
                // Si hay error
                e.printStackTrace();
            }
            // Para las APIs 29<
            // No genere un error ya que solo se usa en las APIs nuevas
            if(file != null)
                MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null, null);
        }
    }

    /** Método para abrir los contactos del celular **/
    private void abrirContactos() {
        // Creamos el intent para la acción de abrir contactos
        Intent intentContactos  = new Intent();
        // Establecemos el componente Contactos accediendo desde el sistema de nuestro dispositivivo
        intentContactos.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.DialtactsContactsEntryActivity"));
        // Se usa para "abrir/mostrar" la aplicación de contactos
        intentContactos.setAction("android.intent.action.MAIN");
        intentContactos.addCategory("android.intent.category.LAUNCHER");
        intentContactos.addCategory("android.intent.category.DEFAULT");
        // Se inicia el intent
        startActivity(intentContactos);
    }

    /** Método para crear una alarma **/
    public void crearAlarma() {
        // Creamos el intent para la acción de establecer una alarma
        Intent intentAlarma = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Alarma");

        // Nos aseguramoes de que exista una actividad que maneje el intent
        if (intentAlarma.resolveActivity(getPackageManager()) != null) {
            startActivity(intentAlarma);
        }
    }

    /** Método para crear un temporizador **/
    public void crearTemporizador(int seg) {
        // Creamos el intent para la acción de establecer un temporizador
        Intent intentTemp = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Temporizador")
                .putExtra(AlarmClock.EXTRA_LENGTH, seg)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false);

        // Nos aseguramoes de que exista una actividad que maneje el intent
        if (intentTemp.resolveActivity(getPackageManager()) != null) {
            // Iniciamos la actividad (intent)
            startActivity(intentTemp);
        }
    }

    /** Método para marcar a un telefono celular **/
    public void marcarTelefono(String numeroTel) {
        // Creamos el intent para la acción de llamada
        Intent intentLlamada = new Intent(Intent.ACTION_CALL);
        // Pasamos la información necesario al intent
        intentLlamada.setData(Uri.parse("tel:" + numeroTel));
        // Empezamos la actividad (intent)
        startActivity(intentLlamada);
    }

    /** Método para agregar un evento **/
    public void agregarEvento() {
        // Creamos el intent para la creación del evento en el calendario
        Intent intentEvento = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "");

        // Nos aseguramoes de que exista una actividad que maneje el intent
        if (intentEvento.resolveActivity(getPackageManager()) != null) {
            // Iniciamos el intent
            startActivity(intentEvento);
        }
    }

    /** Método para buscar en internet **/
    public void buscarEnInternet(String busqueda) {
        // Creamos el intent para realizar la busqueda en la web
        Intent intentBusqueda = new Intent(Intent.ACTION_WEB_SEARCH);
        // Agregamos al intent lo que queremos buscar
        intentBusqueda.putExtra(SearchManager.QUERY, busqueda);
        // Inicializamos el intent
        startActivity(intentBusqueda);
    }

    /** Método donde se prepara lo escuchado por la aplicación para trabajar con el diccionario
       de datos local y regresar una respuesta al usuario **/
    private void prepararRespuesta(String escuchado) {
        // La cadena recibida se normaliza (transforma el texto Unicode en una representacion descompuesta)
        String normalizar = Normalizer.normalize(escuchado, Normalizer.Form.NFD);
        // A la cadena normalizada se le quitan los acentos
        String sintilde = normalizar.replaceAll("[^\\p{ASCII}]", "");

        // Creamos una variable int que nos servira para comparar dos Strings
        int resultado;
        // Guardamos la primera respuesta, que obtenemos del diccionario de datos, en un String
        String respuesta = diccionarioDatos.get(0).getRespuestas();

        // Recorremos t0do el diccionario de datos
        for (int i = 0; i < diccionarioDatos.size(); i++){
            // Guardamos la posición de la cuestion
            resultado = sintilde.toLowerCase().indexOf(diccionarioDatos.get(i).getCuestion());
            // Si <resultado> es diferente de -1 quiere decir que encontro una coincidencia entre la
            // cadena recibida y un elemento del diccionario de datos
            if(resultado != -1){
                // Se guarda la respuesta que coincidio con la cadena de entrada (cuestion)
                respuesta = diccionarioDatos.get(i).getRespuestas();
            }
        }
        // Se llama al método que determinara que función externa debera realizar la aplicación
        // ejemplo: llamar a un numero.
        acciones(sintilde.toLowerCase());

        // Llamada al método que se encargara de darle una respuesta al usuario
        responder(respuesta);
    }

    /** Este método se encarga de llamar a la función de voz, el parametro que reciba será lo que la
       aplicación dira **/
    private void responder(String respuestita) {
        // Escribimos la respuesta en el TextView
        respuesta.setText(respuestita);
        // Haciendo uso del objeto de la clase TextToSpeech utilizamos la funcion speak para hacer que hable
        leerRespuesta.speak(respuestita, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /** Método donde inicializamos algunos TextView, el diccionario de datos y el objeto TextToSpeech **/
    public void inicializar(){
        // Enlazamos <escuchando> y <respuesta> a los TextView correspondientes
        escuchando = findViewById(R.id.jTextEscucho);
        respuesta = findViewById(R.id.jTextDigo);
        foto = findViewById(R.id.imageView4);

        // Guardamos en nuestra variable todas las posibles respuestas de la aplicación
        diccionarioDatos = proveerDatos();

        // Creamos el objeto TextToSpeech
        leerRespuesta = new TextToSpeech(this,this);
    }

    /** Método para utilizar la voz del celular **/
    public void hablar(View view) {
        // Creamos el intent para la acción de hablar
        Intent intentHablar = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Establecemos el idioma a español México
        intentHablar.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "es-MX");
        // Iniciamos la actividad
        startActivityForResult(intentHablar, RECONOCEDOR_VOZ);
    }

    /** Método del botón ayuda, rediccionara a la página web**/
    public void ayuda(View view) {
        // Creamos el intent para redireccionar a la página web
        Intent intentAyuda = new Intent(Intent.ACTION_VIEW);
        // Asignamos la url al intent
        intentAyuda.setData(Uri.parse("https://softgem.netlify.app/ayuda.html"));
        // Iniciamos la actividad
        startActivity(intentAyuda);
    }

    /** Pequeño diccionario de datos local
        Contiene todas las respuestas que puede proveer la aplicación **/
    public ArrayList<Respuesta> proveerDatos()
    {
        ArrayList<Respuesta> respuestas = new ArrayList<>();
        respuestas.add(new Respuesta("defecto", "Lo siento, no te entiendo."));
        respuestas.add(new Respuesta("hola megan", "¡Hola! ¿Qué tal? Si necesitas algo solo dimelo."));
        respuestas.add(new Respuesta("hola", "¡Hola! ¿Qué tal? Si necesitas algo solo dimelo."));
        respuestas.add(new Respuesta("megan puedes contarme un chiste", "¿Cómo se dice disparo en árabe? Ahí-va-la-bala."));
        respuestas.add(new Respuesta("cuentame un chiste", "El otro día vendí mi aspiradora. Lo único que hacía era acumular polvo."));
        respuestas.add(new Respuesta("podrias contarme otro chiste", "¿Cuál es el colmo de Aladdín? Tener mal genio."));
        respuestas.add(new Respuesta("quien te desarrollo", "Mis desarrolladores son la empresa Softgem."));
        respuestas.add(new Respuesta("quien te creo", "La empresa Softgem son mis creadores."));
        respuestas.add(new Respuesta("adios megan", "Adiós, cuidate."));
        respuestas.add(new Respuesta("megan me puedes ayudar con unos problemas", "Claro, será un placer ayudarte"));
        respuestas.add(new Respuesta("megan me ayudas con unos problemas", "Claro, será un placer ayudarte"));
        respuestas.add(new Respuesta("megan me puedes ayudas con un problema", "Claro, será un placer ayudarte"));
        respuestas.add(new Respuesta("megan quien es el mejor presidente de mexico", "El mejor presidente de México es amlo."));
        respuestas.add(new Respuesta("megan me puedes decir si va a ganar el santos", "Lo siento, pero el Santos no será campeón."));
        respuestas.add(new Respuesta("megan mexico va a ganar el mundial", "Lo siento, pero México no ganará el mundial."));
        respuestas.add(new Respuesta("como estas", "Muy bien, ¿y tú?"));
        respuestas.add(new Respuesta("camara", "Un momento."));
        respuestas.add(new Respuesta("megan abre la camara", "Un momento."));
        respuestas.add(new Respuesta("abrir camara", "Un momento."));
        respuestas.add(new Respuesta("abre la camara", "Un momento."));
        respuestas.add(new Respuesta("contactos", "Un momento."));
        respuestas.add(new Respuesta("megan abre los contactos", "Un momento."));
        respuestas.add(new Respuesta("megan muestrame mis contactos", "Un momento."));
        respuestas.add(new Respuesta("abrir contactos", "Un momento."));
        respuestas.add(new Respuesta("abre los contactos", "Un momento."));
        respuestas.add(new Respuesta("megan crea una alarma", "Un momento."));
        respuestas.add(new Respuesta("crea una alarma", "Un momento."));
        respuestas.add(new Respuesta("crear una alarma", "Un momento."));
        respuestas.add(new Respuesta("alarma", "Un momento."));
        respuestas.add(new Respuesta("megan crea un temporizador", "Un momento."));
        respuestas.add(new Respuesta("crea un temporizador", "Un momento."));
        respuestas.add(new Respuesta("temporizador", "Un momento."));
        respuestas.add(new Respuesta("llama", "Un momento."));
        respuestas.add(new Respuesta("megan llama", "Un momento."));
        respuestas.add(new Respuesta("megan llamar", "Un momento."));
        respuestas.add(new Respuesta("abre el calendario", "Un momento."));
        respuestas.add(new Respuesta("crea un evento", "Un momento."));
        respuestas.add(new Respuesta("crear un evento", "Un momento."));
        respuestas.add(new Respuesta("megan busca", "Un momento."));

        return respuestas;
    }

    @Override
    public void onInit(int status) {
    }

    /** Método donde dependiendo de la petición recibida, se ejecutara o no una acción **/
    public void acciones(@NonNull String cadena) {
        // Array de tipo String donde guardaremos los resultados de hacer un split a la cadena que
        // recibimos
        String[] cadenaSeparada = cadena.split(" ");

        // Ciclo for para recorrer t0do el arreglo String
        for(int i=0; i<cadenaSeparada.length; i++) {
            // Utilizamos un switch para ejecutar la acción que el usuario este requiriendo
            // esto lo hacemos por medio del String[] y comparaciones de palabras
            switch (cadenaSeparada[i]) {
                // Caso para abrir la cámara
                case "camara": { abrirCamara(); break; }

                // Caso para abrir la lista de contactos del usuario
                case "contactos": { abrirContactos(); break; }

                // Caso para crear una alarma
                case "alarma": { crearAlarma(); break; }

                // Caso para crear un temporizador
                case "temporizador": {
                    // Bloque try-catch para evitar el cierre de la aplicación en caso de error al
                    // no cumplir con los parametros necesarios
                    try {
                        // Creamos otro Array de tipo String donde dividiremos la cadena que recibimos
                        // por cada espacio en blanco
                        String[] tiempo = cadena.split("\\s");
                        // la cadena de entrada al terminar en "n segundos" pasaremos siempre como
                        // valor el penúltimo miembro del arreglo
                        int seg = Integer.parseInt(tiempo[tiempo.length - 2]);
                        // Llamada al método para crear un temporizador
                        crearTemporizador(seg);
                    } catch (Exception err) {
                        // Mensaje al usuario sobre error
                        Toast.makeText(this, "Se requiere los segundos", Toast.LENGTH_LONG).show();
                    }
                    break;
                }

                // Caso para hacer una llamada
                case "llama": {
                    // Bloque try-catch para evitar el cierre de la aplicación en caso de error al
                    // no cumplir con los parametros necesarios
                    try {
                        // Creamos otro Array de tipo String donde dividiremos la cadena que recibimos
                        // por cada espacio en blanco
                        String[] tel = cadena.split(" ");
                        // Por la forma en que el API detecta un número teléfonico tendremos que pasar como
                        // valor los tres ultimos elementos del arreglo
                        String numero = tel[tel.length - 3] + tel[tel.length - 2] + tel[tel.length - 1];
                        // Llamada al método para hacer una llamada
                        marcarTelefono(numero);
                    } catch (Exception err) {
                        // Mensaje al usuario sobre error
                        Toast.makeText(this, "Se requiere un teléfono válido", Toast.LENGTH_LONG).show();
                    }
                    break;
                }

                // Caso para la creación de evento
                case "evento":
                case "calendario": { agregarEvento(); break; }

                // Caso para buscar en internet
                case "busca": {
                    // Bloque try-catch para evitar el cierre de la aplicación en caso de error al
                    // no cumplir con los parametros necesarios
                    try {
                        // Creamos un substring de la cadena de entrada, dado que la estructura siempre sera
                        // "megan busca ..." el indice del que se tomara el substring sera a partir del 12
                        String subCadena = cadena.substring(12);
                        // Llamada al método para buscar en internet
                        buscarEnInternet(subCadena);
                    } catch (Exception err) {
                        // Mensaje al usuario sobre error
                        Toast.makeText(this, "Por favor ingrese la busqueda", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        }
    }
}