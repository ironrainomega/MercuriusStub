package net.minecraftforge.mercurius;

import akka.util.Reflect;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.mercurius.Helpers.DataHelper;
import net.minecraftforge.mercurius.Helpers.HttpDownloadHelper;
import net.minecraftforge.mercurius.Helpers.LogHelper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

@Mod(modid = net.minecraftforge.mercurius.StatsModStub.MODID, version = net.minecraftforge.mercurius.StatsModStub.VERSION)
public class StatsModStub
{
    public static final String MODID = "mercurius";
    public static final String MODNAME = "Mercurius";
    public static final String VERSION = "1.0.1"; //Can we get gradle to replace these things?
    public static final String GUIFACTORY = "net.minecraftforge.mercurius.StatsMod$GuiFactory";

    public static final String MercuriusJarURL = "http://files.minecraftforge.net/maven/net/minecraftforge/Mercurius/1.10.2/Mercurius-1.10.2.jar"; // HTTPS as MITM is a thing...

    private Class loadedMercurius;
    private Object loadedMercuriusInstance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        File mercuriusFile = new File(e.getModConfigurationDirectory().getParent(), "/local/Mercurius-1.10.2.jar");
        try
        {
            if (!mercuriusFile.exists()) {
                LogHelper.info("Mercurius doesn't exist locally... downloading from: " + StatsModStub.MercuriusJarURL);
                // need to download the file.
                downloadFile(e.getModConfigurationDirectory().getParent() + "/local");
            }

            // even after first download I want to keep this check... just to make sure something fishy didn't happen during download...
            String sha1Local = DataHelper.GetSHA1Hash(mercuriusFile).toLowerCase().trim();
            String sha1Remote = DataHelper.GetHTTPResponse(new URL(StatsModStub.MercuriusJarURL + ".sha1")).toLowerCase().trim();

            LogHelper.info("Local SHA1: " + sha1Local);
            LogHelper.info("Remote SHA1: " + sha1Remote);

            if (!sha1Local.equals(sha1Remote))
            {
                LogHelper.info("Hashes don't match, either a newer version is available or something is funky...");

                // let's remove the local file...
                mercuriusFile.delete();
                // need to download the file.
                downloadFile(e.getModConfigurationDirectory().getParent() + "/local");
            }
        } catch (Exception e1) {
            LogHelper.error("Something went really wrong here...");
            e1.printStackTrace();
            return;
        }

        URLClassLoader cl;
        try {
            cl = URLClassLoader.newInstance(new URL[] {mercuriusFile.toURL()}, StatsModStub.class.getClassLoader());
            loadedMercurius = cl.loadClass("net.minecraftforge.mercurius.StatsMod");
            loadedMercuriusInstance = loadedMercurius.newInstance();

            //getMethod("preInit").invoke(loadedMercuriusInstance, (Object)e);
            ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[] {"preInit"}, e.getClass()).invoke(loadedMercuriusInstance, e);

            LogHelper.info("aaa");

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent e)
    {
        if (loadedMercurius != null)
        {
            try {
                ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[] {"init"}, e.getClass()).invoke(loadedMercuriusInstance, e);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }

        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        if (loadedMercurius != null)
        {
            try {
                // In dev env, postInit actually calls into obfuscation functions...
                ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[] {"postInit"}, e.getClass()).invoke(loadedMercuriusInstance, e);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent e)
    {
        if (loadedMercurius != null)
        {
            try {
                ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[] {"serverStarting"}, e.getClass()).invoke(loadedMercuriusInstance, e);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent e)
    {
        if (loadedMercurius != null)
        {
            try {
                ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[] {"serverStopping"}, e.getClass()).invoke(loadedMercuriusInstance, e);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
    }

    private boolean downloadFile(String localDir)
    {
        try {
            File d = new File(localDir);
            if (!d.exists())
                d.mkdir();

            HttpDownloadHelper.downloadFile(StatsModStub.MercuriusJarURL, localDir);
            LogHelper.info("Downloaded Mercurius...");
            return true;
        } catch (IOException e) {
            LogHelper.error("Downloading Mercurius failed...");
            e.printStackTrace();
            return false;
        }
    }
}
