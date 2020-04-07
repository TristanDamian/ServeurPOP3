/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serveurpop3;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
/**
 *
 * @author p1509696
 */
public class MailPOP3 implements Runnable{
    Socket so;
    public MailPOP3(Socket socket) {
        this.so=socket;
    }

    @Override
    public void run() {
        try { 
            BufferedInputStream  IS =new BufferedInputStream(so.getInputStream());
            BufferedOutputStream  OS;
            try {
                OS = new BufferedOutputStream(so.getOutputStream());
                DataInputStream in=new DataInputStream(IS);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String hello="OK POP3 SRV READY "+timestamp.getTime()+"\r\n";
                System.out.println(timestamp.getTime());
                Hashtable UsersList= new Hashtable();       //pour associer chaque utilisateur à son mot de passe
                UsersList.put("User1", "azerty");
                UsersList.put("User1@example.com", "azerty");
                UsersList.put("User2", "123");
                UsersList.put("User2@example.com", "123");

                System.out.println(UsersList.get("User1"));
                byte[] toSend=hello.getBytes();
                for(int i=0; i<toSend.length;i++){
                    OS.write(toSend[i]);
                }
                OS.flush();
                boolean continuer=true;
                String message;
                String secret=new String();
                String userName;
                String etat="Authorisation";
                File boiteMail = null;
                List<String> listeMail = new ArrayList<String>();
                List<Boolean> listeDelete = new ArrayList<Boolean>();
                do{
                    message=in.readLine();
                    System.out.println("."+message+".");
                    
                    String[] message_parse=message.split(" ");
                    for(int i=0;i<0;i++){
                        System.out.println(message_parse[i]);
                    }
                    String commande=message_parse[0];
                    switch(commande){
                        case "APOP":  if(etat=="Authorisation" && message_parse.length>2){
                                         userName=message_parse[1];
                                         System.out.println(userName);
                                         String pwd = message_parse[2];
                                         secret="<"+timestamp.getTime()+UsersList.get(userName)+">";
                                         boiteMail = new File("MailUser/"+userName+".txt");
                                         if (boiteMail.exists()){
                                             if (CheckPwd(secret,pwd)){
                                                 System.out.println("MDP Correct");
                                                 listeMail= GetMail(boiteMail,listeDelete);
                                                 System.out.println(listeMail);
                                                 etat="Transaction";
                                                 Send("OK \r\n",OS);
                                             }
                                             else{
                                                 Send("ERR WRONG PASSWORD\r\n",OS);
                                             }
                                             }

                                         else{
                                             Send("ERR WRONG USER\r\n",OS);
                                         }
                                        }
                                        else {
                                            Send("ERR\r\n",OS);
                                        }
                                        break;

                        case "USER":    if(etat=="Authorisation" && message_parse.length>1){
                                            System.out.println("aaUSER");
                                            userName=message_parse[1];
                                            secret="<"+timestamp.getTime()+UsersList.get(userName)+">";
                                            boiteMail = new File("MailUser/"+userName+".txt");
                                            if (boiteMail.exists()) {
                                                System.out.println("EXISTE");
                                                etat="WaitPASS";
                                                Send("USER OK, PWD ?\r\n",OS);
                                            }
                                            else {
                                                Send("ERR WRONG USER\r\n", OS);
                                            }
                                        }
                                        else {
                                            Send("ERR",OS);
                                        }
                                        break;

                        case "PASS":    if(etat.equals("WaitPASS") && message_parse.length>1){
                                            System.out.println("ON est là");
                                            String pwd = message_parse[1];
                                            if (boiteMail.exists()){
                                                if (CheckPwd(secret,pwd)){
                                                    System.out.println("MDP Correct");
                                                    listeMail= GetMail(boiteMail,listeDelete);
                                                    System.out.println(listeMail);
                                                    etat="Transaction";
                                                    Send("OK \r\n",OS);
                                                }
                                                else{
                                                    Send("ERR WRONG PASSWORD\r\n",OS);
                                                }
                                            }
                                        }
                                        else {
                                            Send("ERR\r\n",OS);
                                        }
                                        break;

                        case "STAT":   if(etat=="Transaction"){
                                            Send("+OK "+(listeMail.size())+" "+boiteMail.getTotalSpace()+"\r\n",OS);
                                        }
                                        else {
                                            Send("ERR\r\n",OS);
                                        }
                                        break;

                        case "RETR":    if(etat=="Transaction" && message_parse.length>1){
                                                int numMail=Integer.parseInt(message_parse[1]);
                                                if(!(numMail>listeMail.size())&&numMail>0) {
                                                    Send("+OK\r\n" + listeMail.get(numMail - 1), OS);
                                                }
                                                else{
                                                Send("ERR WRONG MAIL NUMBER\r\n", OS);
                                            }
                                        }
                                        else {
                                            Send("ERR",OS);
                                        }
                                        break;

                        case "DELE":    if(etat=="Transaction" && message_parse.length>1){
                                            int numMail=Integer.parseInt(message_parse[1]);
                                            if(!(numMail>listeMail.size())&&numMail>0) {
                                               listeDelete.set(numMail-1,true);
                                               int num=numMail-1;
                                                Send("+OK Mail" + num +" sera supprimé\r\n", OS);
                                            }
                                            else{
                                                Send("ERR WRONG MAIL NUMBER\r\n", OS);

                                            }
                                        }
                                        else {
                                             Send("ERR",OS);
                                        }
                                        break;

                        case "QUIT":    OS.flush();
                                        System.out.println("EXISTE");
                                        if(boiteMail!=null){
                                            FileWriter writer= new FileWriter(boiteMail);
                                            BufferedWriter buffWriter= new BufferedWriter(writer);
                                            for (int i=0; i<listeMail.size();i++){
                                                if(!listeDelete.get(i)){
                                                    String toWrite=listeMail.get(i)+".\r\n";
                                                    buffWriter.write(toWrite);
                                                }
                                            }
                                            buffWriter.close();
                                            writer.close();
                                        }
                                        

                                        continuer=false;
                                        break;

                         default: Send("ERR UNKNOWN COMMAND\r\n",OS);
                                    break;
                    }



                }while(continuer);
                Send("AU REVOIR",OS);
                Thread.sleep(160);
                so.close();
            } catch (IOException ex) {
                Logger.getLogger(MailPOP3.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MailPOP3.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } catch (IOException ex) {
                Logger.getLogger(MailPOP3.class.getName()).log(Level.SEVERE, null, ex);
            }
        
    }

    boolean  CheckPwd(String secret, String pwd) throws NoSuchAlgorithmException {
        byte[] toCode=secret.getBytes();
        System.out.println(secret);
        String digest="";
        byte[] hash = MessageDigest.getInstance("MD5").digest(toCode);
        for(byte b : hash){ digest=digest+String.format("%02x", b);}
        System.out.println(digest);
        System.out.println("EXISTE");
        if(digest.equals(pwd)){
            return true;
        }
        else{
            return false;
        }
    }

    List<String> GetMail(File boiteMail, List<Boolean> listeDelete) throws IOException {
        List<String>listeMail=new ArrayList<String>();
        FileReader reader= new FileReader(boiteMail);
        BufferedReader readFile = new BufferedReader(reader);
        int countMail=0;
        String Mail = "";
        String line;
        line=readFile.readLine();
        while(line!=null){
            Mail="";
            while(!line.matches(".")){
                Mail = Mail + line + "\r\n";
                line=readFile.readLine();
            }
            if (!Mail.contentEquals("")) {
                listeMail.add(Mail);
                listeDelete.add(false);
            }
            line=readFile.readLine();
        }
        return listeMail;
    }

    void Send(String message, BufferedOutputStream OS) throws IOException{
        byte[] toSend=message.getBytes();

        for(int i=0; i<toSend.length;i++){
            OS.write(toSend[i]);
            }
        OS.flush();
    }
}
