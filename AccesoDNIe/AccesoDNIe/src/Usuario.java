import java.security.cert.X509Certificate;

/**
 * Clase para almacenar los datos de un usuario
 * @author Juan Carlos
 * @author Raquel Viciana
 */
public class Usuario {
    private String nombre;
    private String apellido1="";
    private String apellido2="";
    private String nif;
    public static final String NAME="GIVENNAME=";
    public static final String DNI="SERIALNUMBER=";
    public static final String CN="CN=\"";
    String apellidos = "";


    
    public Usuario(String n,String a1,String a2,String ni){
        nombre=n;
        apellido1=a1;
        apellido2=a2;
        nif=ni;
    }
    

    public Usuario(){};
    
    public Usuario(String data) {

        nombre = data.substring(data.indexOf(NAME) + NAME.length());
        nombre = nombre.substring(0, nombre.indexOf(","));
        nif = data.substring(data.indexOf(DNI) + DNI.length());
        nif = nif.substring(0, nif.indexOf(","));
        apellidos = data.substring(data.indexOf(CN) + CN.length());
        apellidos = apellidos.substring(0, apellidos.indexOf(","));
        
    }
    
    public Usuario (X509Certificate authCert){
        String data = authCert.toString();
        
        nombre = data.substring(data.indexOf(NAME) + NAME.length());
        nombre = nombre.substring(0, nombre.indexOf(","));
        nif = data.substring(data.indexOf(DNI) + DNI.length());
        nif = nif.substring(0, nif.indexOf(","));
        apellidos = data.substring(data.indexOf(CN) + CN.length());
        apellidos = apellidos.substring(0, apellidos.indexOf(","));
        
    }

    @Override
    public String toString(){
        return nombre+" "+apellidos+" "+nif;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }
          
}
