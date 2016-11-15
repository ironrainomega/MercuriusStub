package net.minecraftforge.mercurius.stub;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils
{
    private static final String FORGE_MAVEN = "http://files.minecraftforge.net/maven/"; //TODO: HTTPS
    private static final int BUFFER_SIZE = 4096;
    private static final int TIMEOUT = 24 * 60 * 60 * 1000;

    public static String downloadFile(String url, File target)
    {
        int response = -1;
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection)(new URL(url)).openConnection();
            response = conn.getResponseCode();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            LogHelper.warn("Failed to download " + url + " with exception: " + e.getMessage());
            return null;
        }

        if (response == HttpURLConnection.HTTP_OK)
        {
            FileOutputStream output = null;
            DigestInputStream input = null;

            try
            {
                input = new DigestInputStream(conn.getInputStream(), MessageDigest.getInstance("SHA1"));
                output = new FileOutputStream(target);

                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = input.read(buffer)) != -1)
                {
                    output.write(buffer, 0, bytesRead);
                }
                return bytesToHex(input.getMessageDigest().digest());
            }
            catch (IOException e)
            {
                e.printStackTrace(); // Something broke so lets close and delete the temp file
                LogHelper.warn("Failed to download " + url + " with exception: " + e.getMessage());
                closeSilently(output);
                if (target.exists())
                    target.delete();
            }
            catch (NoSuchAlgorithmException e){} // Should never happen, seriously what Java install doesn't have SHA1?
            finally
            {
                closeSilently(input);
                closeSilently(output);
            }
        }
        else
        {
            LogHelper.warn("No file to download. Server replied HTTP code: " + response);
        }
        conn.disconnect();
        return null;
    }

    public static String downloadString(String url)
    {
        try
        {
            HttpURLConnection conn = (HttpURLConnection)((new URL(url)).openConnection());
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            return toString(conn.getInputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String readFile(File file)
    {
        try
        {
            return toString(new FileInputStream(file));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeFile(File file, String data)
    {
        BufferedWriter out = null;
        try
        {
            out = new BufferedWriter(new FileWriter(file));
            out.write(data);
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace(); //Something went wrong writing to the file? How?
        }
        finally
        {
            closeSilently(out);
        }
        return false;
    }

    private static String toString(InputStream in)
    {
        try
        {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = in.read(buffer)) != -1)
            {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            closeSilently(in);
        }
        return null;
    }

    private static void closeSilently(Closeable c)
    {
        if (c != null)
        {
            try
            {
                c.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
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

    public static File getMavenFile(File root, String group, String artifact, String version)
    {
        String path = group.replace('.', '/') + '/' + artifact + '/' + version + '/' + artifact + '-' + version + ".jar";
        return new File(root, path);
    }

    public static File findFirstParent(File file, String target)
    {
        for (; file != null && !file.getName().equals(target); file = file.getParentFile());
        return file;
    }

    public static File getJar(Class<?> cls) throws Exception
    {
        URL url = cls.getProtectionDomain().getCodeSource().getLocation();
        String extURL = url.toExternalForm();

        if (!extURL.endsWith(".jar"))
        {
            String suffix = "/" + (cls.getName()).replace(".", "/") + ".class";
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


    public static String getSHA1(File file)
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        DigestInputStream input = null;
        try
        {
            input = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("SHA1"));
            while (input.read(buffer) > 0); //Read everything!
            return Utils.bytesToHex(input.getMessageDigest().digest());
        }
        catch (NoSuchAlgorithmException e){} // Should never happen, seriously what Java install doesn't have SHA1?
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            closeSilently(input);
        }
        return null;
    }

    private static String getRemoteChecksum(String group, String artifact, String version)
    {
        String path = group.replace('.', '/') + '/' + artifact + '/' + version + '/' + artifact + '-' + version + ".jar.sha1";
        return downloadString(FORGE_MAVEN + path).replaceAll("\r?\n", "");
    }
    private static String downloadMavenFile(File file, String group, String artifact, String version)
    {
        String path = group.replace('.', '/') + '/' + artifact + '/' + version + '/' + artifact + '-' + version + ".jar";
        return downloadFile(FORGE_MAVEN + path, file);
    }

    public static File updateMercurius(File libs, String mcversion)
    {
        File target = updateMavenFile(libs, "net.minecraftforge", "Mercurius", mcversion);
        return target;
    }
    private static File updateMavenFile(File libs, String group, String artifact, String version)
    {
        File target = getMavenFile(libs, group, artifact, version);
        File shaF = new File(target.getAbsolutePath() + ".sha");

        String checksum = "";
        boolean needsDownload = true;

        if (target.exists())
        {
            LogHelper.info("File exists, Checking hash: " + target.getAbsolutePath());
            String fileChecksum = Utils.getSHA1(target);
            if (shaF.exists())
            {
                checksum = Utils.readFile(shaF).replaceAll("\r?\n", "");
                if (checksum.equals(fileChecksum))
                {
                    if (shaF.lastModified() < System.currentTimeMillis() - TIMEOUT)
                    {
                        LogHelper.info("  Hash matches, However out of date, downloading checksum");
                        String remoteChecksum = Utils.getRemoteChecksum("net.minecraftforge", "Mercurius", version);
                        if (checksum.equals(remoteChecksum))
                        {
                            LogHelper.info("    Remote Checksum verified, not downloading: " + checksum);
                            shaF.setLastModified(System.currentTimeMillis());
                            needsDownload = false;
                        }
                        else
                        {
                            LogHelper.info("    Hash mismatch, Deleting File");
                            LogHelper.info("      Target: " + checksum);
                            LogHelper.info("      Actual: " + remoteChecksum);
                            target.delete();
                        }
                    }
                    else
                    {
                        LogHelper.info("  Hash matches, Skipping download: " + checksum);
                        needsDownload = false;
                    }
                }
                else
                {
                    LogHelper.info("  Hash mismatch, Deleting File");
                    LogHelper.info("    Target: " + checksum);
                    LogHelper.info("    Actual: " + fileChecksum);
                    target.delete();
                }
            }
            else
            {
                LogHelper.info("  Hash file does not exist, but file does. Creating hash file with current hash. with 24 hour timeout.");
                LogHelper.info("    Checksum: " + fileChecksum);
                needsDownload = !writeFile(shaF, fileChecksum); // If we cant write the file, try redownloading
            }
        }

        if (needsDownload)
        {
            if (target.exists())
                target.delete();

            File parent = target.getParentFile();
            if (!parent.exists())
                parent.mkdirs();

            String remoteChecksum = Utils.getRemoteChecksum(group, artifact, version);
            String downloadedChecksum = downloadMavenFile(target, group, artifact, version);
            if (remoteChecksum == null || downloadedChecksum == null)
            {
                LogHelper.info("Downloading failed, exiting!");
                return null;
            }

            if (!remoteChecksum.equals(downloadedChecksum))
            {
                LogHelper.error("Download failed, Invalid checksums!");
                LogHelper.error("  Target: " + remoteChecksum);
                LogHelper.error("  Actual: " + downloadedChecksum);
                target.delete();
                return null;
            }

            LogHelper.info("Download checksums verified: " + remoteChecksum);
            writeFile(shaF, remoteChecksum);
        }

        return target;
    }
}
