package gsoc.google.com.physicalweblgpoireader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import gsoc.google.com.physicalweblgpoireader.model.POI;

/**
 * Created by lgwork on 27/06/16.
 */
public class LGutils {

    public static boolean copyFiletoLG(InputStream is,Activity activity) throws JSchException, IOException {

        boolean success = true;

        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String user = prefs.getString("lgUser", "lg");
        String password = prefs.getString("lgPassword", "lqgalaxy");
        String lgIp = prefs.getString("lgIP", "");
        String lgPort = prefs.getString("lgPort", "22");

        String lgKMLName = "/var/www/"+prefs.getString("lgKMLName","test1.kml");

        Session session = new JSch().getSession(user,lgIp, Integer.parseInt(lgPort));
        session.setPassword(password);
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);
        session.connect();

        boolean ptimestamp = true;

        String command="scp " + (ptimestamp ? "-p" :"") +" -t "+lgKMLName;
        Channel channel=session.openChannel("exec");
        ((ChannelExec)channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out= channel.getOutputStream();
        InputStream in= channel.getInputStream();

        channel.connect();

        if(checkAck(in)!=0){
            success = false;
            System.exit(0);
        }

        File fileToCopy = getFileFromInputStream(is,activity,lgKMLName);

        if(ptimestamp){
            command="T "+(fileToCopy.lastModified()/1000)+" 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command+=(" "+(fileToCopy.lastModified()/1000)+" 0\n");
            out.write(command.getBytes()); out.flush();
            if(checkAck(in)!=0){
                success = false;
                System.exit(0);
            }
        }

        // send "C0644 filesize filename", where filename should not include '/'
        long filesize=fileToCopy.length();
        command="C0644 "+filesize+" ";
        if(fileToCopy.getPath().lastIndexOf('/')>0){
            command+=fileToCopy.getPath().substring(fileToCopy.getPath().lastIndexOf('/')+1);
        }
        else{
            command+=fileToCopy.getPath();
        }
        command+="\n";
        out.write(command.getBytes()); out.flush();
        if(checkAck(in)!=0){
            success = false;
            System.exit(0);
        }

        // send a content of lfile
        FileInputStream  fis=new FileInputStream(fileToCopy);
        byte[] buf=new byte[1024];
        while(true){
            int len=fis.read(buf, 0, buf.length);
            if(len<=0) break;
            out.write(buf, 0, len); //out.flush();
        }
        fis.close();
        fis=null;
        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
        if(checkAck(in)!=0){
            success = false;
            System.exit(0);
        }
        out.close();

        channel.disconnect();
        session.disconnect();

        return success;
    }

    private static File getFileFromInputStream(InputStream is, Activity activity, String lgKMLName) throws IOException {
        try {
            File file = new File(activity.getCacheDir(),"tmpFileKML.kml");
            OutputStream output = new FileOutputStream(file);
            try {
                try {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = is.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace(); // handle exception, define IOException and others
            }
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            is.close();
        }
        return null;
    }

    static int checkAck(InputStream in) throws IOException{
        int b=in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if(b==0) return b;
        if(b==-1) return b;

        if(b==1 || b==2){
            StringBuffer sb=new StringBuffer();
            int c;
            do {
                c=in.read();
                sb.append((char)c);
            }
            while(c!='\n');
            if(b==1){ // error
                System.out.print(sb.toString());
            }
            if(b==2){ // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    public static boolean visitPOIS(List<POI> poisList, Activity activity) {
        return sendTourPOIs(poisList,activity);
    }


    private static boolean sendTourPOIs(List<POI> poisList, Activity activity) {
        sendFirstTourPOI(poisList.get(0),activity);
       return sendOtherTourPOIs(poisList, 10,activity);
    }

    private static void sendFirstTourPOI(POI firstPoi,Activity activity) {
        try {
            setConnectionWithLiquidGalaxy(buildCommand(firstPoi),activity);
        } catch (JSchException e) {
            e.printStackTrace();
            AndroidUtils.showMessage("Error in connection with Liquid Galaxy.",activity);
        }
    }

    private static boolean sendOtherTourPOIs(List<POI> pois, int poisDuration, Activity activity) {

        for(int i=1;i<pois.size();i++){
            sendTourPOI(poisDuration, buildCommand(pois.get(i)),activity);
        }

        return true;

    }

    private static void sendTourPOI(Integer duration, String command, Activity activity) {
        try {
            Thread.sleep((long) (duration.intValue() * 1000));
            setConnectionWithLiquidGalaxy(command,activity);
        } catch (InterruptedException e) {
            e.printStackTrace();
            AndroidUtils.showMessage("Error in duration of POIs",activity);
        } catch (JSchException e2) {
            AndroidUtils.showMessage("Error in connection with Liquid Galaxy.",activity);
            e2.printStackTrace();
        }
    }

    private static String buildCommand(POI poi) {
        return "echo 'flytoview=<LookAt><longitude>" + (poi.getPoint().getLongitude()) + "</longitude><latitude>" + (poi.getPoint().getLatitude()) + "</latitude><altitude>" + (0) + "</altitude><heading>" + (78) + "</heading><tilt>" + (61) + "</tilt><range>" + (300) + "</range><gx:altitudeMode>" + "relativeToSeaFloor" + "</gx:altitudeMode></LookAt>' > /tmp/query.txt";
    }


    private static String setConnectionWithLiquidGalaxy(String command,Activity activity) throws JSchException {
        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String user = prefs.getString("lgUser", "lg");
        String password = prefs.getString("lgPassword", "lqgalaxy");
        String lgIp = prefs.getString("lgIP", "");
        String lgPort = prefs.getString("lgPort", "22");

        Session session = new JSch().getSession(user,lgIp, Integer.parseInt(lgPort));
        session.setPassword(password);
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);
        session.connect();
        ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        channelssh.setOutputStream(baos);
        channelssh.setCommand(command);
        channelssh.connect();
        channelssh.disconnect();
        return baos.toString();
    }

}
