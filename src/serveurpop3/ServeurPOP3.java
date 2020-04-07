/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serveurpop3;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author p1509696
 */
public class ServeurPOP3 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        try{
            boolean ssl=true;
            ServerSocket srv=new ServerSocket(110,5);
            SSLContext sslContext=SSLContext.getDefault();
            SSLServerSocketFactory ssf=sslContext.getServerSocketFactory();
            SSLServerSocket ss=(SSLServerSocket)ssf.createServerSocket(1048,5);
            String[] supportedCipher=ss.getSupportedCipherSuites();

            int i=0;
            List<String> cipherToUse=new ArrayList<String>();
            for(int j=0; j<supportedCipher.length;j++){
                String[] split=supportedCipher[j].split("_");
                if (split[2].equals("anon")) {
                    cipherToUse.add(supportedCipher[j]);
                    System.out.println(cipherToUse.get(i));
                    i++;
                }
            }
            String[] cipherToEnable=new String[cipherToUse.size()];
            for(int j=0; j<cipherToUse.size();j++){
                cipherToEnable[j]=cipherToUse.get(j);
            }
            ss.setEnabledCipherSuites(cipherToEnable);
            while(true){
                Socket so;
                if(ssl){
                    so=ss.accept();
                }
                else{
                    so=srv.accept(); //garder la possibilité d'une serversocket sans ssl pour faciliter les tests avec putty
                }
                MailPOP3 mail= new MailPOP3(so);
                new Thread(mail).start();

            }
        } catch(IOException | NoSuchAlgorithmException ex){ System.err.println(ex); }
    }

}
