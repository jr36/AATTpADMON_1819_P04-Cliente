import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;
import java.util.Date;
import javax.smartcardio.CardChannel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.xml.bind.DatatypeConverter;
import java.util.LinkedHashMap;
import com.sun.javafx.collections.MappingChange.Map;


import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.jse.smartcardio.SmartcardIoConnection;



/**
 * Aplicaciones TelemÃ¡ticas para la AdministraciÃ³n
 * 
 * Este programa debe ller el nombre y NIF de un usuario del DNIe, formar el identificador de usuario y autenticarse con un servidor remoto a travÃ©s de HTTP 
 * @author Juan Carlos Cuevas MartÃ­nez
 *  @author Raquel Viciana Abad
 */




public class Main{
    /**
     * @param args the command line arguments
     */
	private static  String url = "localhost:8080"; 
	public static RSAPublicKey rsakey = null;
    public static String alias = "CertFirmaDigital";
    public static Usuario user = new Usuario();
    private static KeyStore dniKS = null;
    private static X509Certificate authCert = null;
    static String Password;
    static char[] PASSWORD;
    static String Usu; //Definimos nombre de usuario (1º letra nombre, 1º Apellido y 1º letra segundo apellido)
	static SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static String fecha = date.format(new Date()); //Obtenemos fecha y pasamos a string
	static String Datosenclaro;
	static String ClavePublica; //Pasamos Clave obtenida a String para transmitir
	static byte[] data; //Datos en claro en byte para realizar firma
	static String nif;
	static String ClaveServicio; //Clave de servicio que se introduce en firma y se comprueba en servidor
	static String firma=null;
	
	//Arrays con resultados del servidor y mensajes
	public final static String[] resultados = {"OK","Error","ErrorUser"};
    public final static String[] mensajes = {"Autenticación Correcta.","Error en la autenticación, usuario inválido.",
                                            "Error de conexión.", "Error en la firma."};
  
      //Teneis que poner vuestra contraseña (Se debe solicitar mediante una interfaz de usuario)
    
    public static void main(String[] args) throws Exception{
    	
    	   JPasswordField password = new JPasswordField();
           password.setEchoChar('*');
           Object[] obj = {"Introduzca contraseña del DNI:\n\n", password};
           Object stringArray[] = {"OK", "Cancelar"};
           if (JOptionPane.showOptionDialog(null, obj, "Clave DNIe",
           JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, stringArray, obj) == JOptionPane.YES_OPTION) {
           Password = new String(password.getPassword());
           }
    	PASSWORD = Password .toCharArray();//Proceso para pedir por pantalla clave privada del dni e iniciar servicio
    	
    	 ClaveServicio = (String) JOptionPane.showInputDialog(null,"Introduzca la clave:","Clave de servicio",JOptionPane.PLAIN_MESSAGE,null,null,null); 
        //Clave de servicio que debe coincidir con la existente en la BBDD
    	 
    	 
    	 System.setProperty("es.gob.jmulticard.fastmode", "true");
    	
        
       
        
        final Provider p = new DnieProvider(new SmartcardIoConnection());
		Security.addProvider(p);
		dniKS = KeyStore.getInstance("DNI"); //Iniciar acceso a los certificados de la tarjeta
		dniKS.load(null, PASSWORD);
		final Enumeration<String> aliases = dniKS.aliases();
		while (aliases.hasMoreElements()) {
			System.out.println(aliases.nextElement());
		}
		
		
		
        //Se obtiene el certificado
       authCert = (X509Certificate) dniKS.getCertificate("CertAutenticacion");
       System.out.println("Formato del certificado: "+authCert.toString());
        
        //En base a los datos del certificado se crea el usuario
        user = new Usuario(authCert);
        String[] aux; //Para Split de apellidos
        String ape=new String(user.apellidos); //Asi obtenemos String de la variable apellidos
        aux=ape.split(" ");//Separamos a partir del espacio
        user.setApellido1(aux[0]); 
        user.setApellido2(aux[1]);
	    Usu = user.getNombre().substring(0,1)+ user.getApellido1()+user.getApellido2().substring(0,1); //Obtenemos nombre de usuario
	    
	    
	    Usu=Usu.toLowerCase();//Pasamos nombre de usuario a minusculas
	    
	    nif=user.getNif().toLowerCase(); //Pasamos dni a minuscula
        
	    
	    if(user!=null) JOptionPane.showMessageDialog(null, "Hola " + user.toString()+ " continue con el proceso de verificación");
		//Creamos Datos en claro (usuario+dni+fecha)
        Datosenclaro=Usu+nif+fecha+ClaveServicio; //Datos para firmar desde el cliente
       
    	
        //Acceso a la clave pública de uno de los certificados
		rsakey = (RSAPublicKey) dniKS.getCertificate(alias).getPublicKey();
		
		Main.SavePublicKey(rsakey); //llamamos al metodo de guardar clave publica
		Main.testSign(); //Llamamos al metodo para realizar la firma
		
		String enviar="Usu="+Usu +"&nif="+nif+"&fecha="+fecha+"&key="+ClavePublica+"&firma="+firma; //Cadena de datos que se envian al servidor
		System.out.println("La fecha de la peticion: "+fecha);
		String respuesta = peticion(enviar); //Llamada al metodo de petición al servidor
        System.out.println(respuesta);	//Respuesta proveniente del servidor
       
       
    }

        //Testeo de la firma
    static void testSign() throws Exception {
    	
    	data=Datosenclaro.getBytes(); 
    	
    	Signature signature = Signature.getInstance("SHA1withRSA"); //$NON-NLS-1$
    	signature.initSign((PrivateKey) dniKS.getKey(alias, PASSWORD)); //$NON-NLS-1$
    	//rsakey=(RSAPublicKey) dniKS.getCertificate(alias).getPublicKey();
    	signature.update(data); //$NON-NLS-1$
    	final byte[] signatureBytes = signature.sign(); //Se completa el proceso
  
    	
    	firma=DatatypeConverter.printBase64Binary(signatureBytes); //Pasamos firma a base64
    	firma = firma.replace("+", "%2B"); //Sustituimos caracteres para correcta transmision
    	
    	ClavePublica = DatatypeConverter.printBase64Binary(rsakey.getEncoded()); //Pasamos ClavePublica a base64
        ClavePublica = ClavePublica.replace("+", "%2B"); //Sustituimos caracteres para correcta transmision
    	

    }
 
    static void SavePublicKey(RSAPublicKey publickey) {
   
        //Mostrar y salvar la clave pública
    	
        	try {


            String rsakey = publickey.getFormat() + " " + publickey.getAlgorithm() + publickey.toString();
            System.out.println("Clave publica: "+rsakey);
            
            FileOutputStream keyfos = new FileOutputStream("public.key");
            byte encodedKey[] = publickey.getEncoded();
            keyfos.write(encodedKey);
            keyfos.close();
            
            
           
            
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        	
 
    	 
    }
public static String peticion(String aenviar) throws MalformedURLException, ProtocolException, IOException{
        
        //Variable que indica la línea que se lee del canal
        String inputline= "";
        //Variable que contendrá el resultado del servidor separado por =
        String [] salida = null;
        
        
        byte[] datos = aenviar.getBytes( StandardCharsets.UTF_8 );
        int longitud = datos.length;
        
        //Cadena con la URL
        String direccion = "http://"+url+"/myapp/autenticar";
        System.out.println(direccion);
        
        try{
            //Monto la URL
            URL url = new URL(direccion);
            
            try{
                //Establezco conexión y parámetros
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();         
                
                conn.setDoOutput(true);
                conn.setConnectTimeout(2000);//Tiempo de intento de conexión al servidor
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod("POST");//Método POST
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(longitud));
                conn.setUseCaches(false);
                
                //Escribe en el canal de escritura
                try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                    wr.write(datos);
                }
                
                //Canal de lectura
                Reader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                BufferedReader in = new BufferedReader(br);
                
                //Mientras lea líneas
                while ((inputline = in.readLine()) != null) {
                    //Si la línea empieza por Resultado=
                    if(inputline.startsWith("Resultado=")){
                        //Separo las palabras por el =
                        salida = inputline.split("=");
                    }
                }
                //Devuelvo la cadena que había detrás del =
                return salida[1];
            
            //Si no puedo leer o escribir
            }catch(IOException e){
                //Error de conexión
                return mensajes[2];
            }
        
        //Si la URL no está bien montada
        }catch(MalformedURLException u){
            //Error de URL
            return mensajes[3];
        }
        
    }
}