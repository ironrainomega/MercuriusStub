package net.minecraftforge.mercurius.Helpers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DataHelper
{
    public static String GetSHA1Hash(String data) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] dataBytes = data.getBytes();

        dataBytes = sha1.digest(dataBytes);
        return DataHelper.bytesToHex(dataBytes);
    }

    public static String GetSHA1Hash(File file) throws NoSuchAlgorithmException, FileNotFoundException {
        byte[] buffer = new byte[8192];
        int count;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        try {
            while ((count = bis.read(buffer)) > 0) {
                sha1.update(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] dataBytes = sha1.digest();
        return DataHelper.bytesToHex(dataBytes);
    }


    public static String GetHTTPResponse(URL url)
    {
        try {
            HttpURLConnection conn = (HttpURLConnection)((url).openConnection());
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoOutput(true);

            BufferedReader in_ = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer ret = new StringBuffer();
            String line;

            while ((line = in_.readLine()) != null)
            {
                ret.append(line);
                ret.append('\r');
            }

            in_.close();
            return ret.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getJar() throws Exception
    {
        URL url = DataHelper.class.getProtectionDomain().getCodeSource().getLocation();
        String extURL = url.toExternalForm();

        if (!extURL.endsWith(".jar"))
        {
            String suffix = "/" + (DataHelper.class.getName()).replace(".", "/") + ".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.length() - 1);
        }

        try {
            return new File(new URL(extURL).toURI());
        } catch(URISyntaxException ex) {
            return new File(new URL(extURL).getPath());
        }
    }

    final static protected char[] hexArray = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}