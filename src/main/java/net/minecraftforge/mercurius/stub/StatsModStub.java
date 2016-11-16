package net.minecraftforge.mercurius.stub;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

@Mod(modid = net.minecraftforge.mercurius.stub.StatsModStub.MODID, version = net.minecraftforge.mercurius.stub.StatsModStub.VERSION)
public class StatsModStub
{
    public static final String MODID = "mercurius_stub";
    public static final String MODNAME = "MercuriusStub";
    public static final String VERSION = "1.0"; //Can we get gradle to replace these things?

    @SuppressWarnings("rawtypes")
    private Class loadedMercurius;
    private Object loadedMercuriusInstance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        if (Loader.isModLoaded("mercurius"))
        {
            LogHelper.info("Normal Mercurius found, disabeling stub");
            return;
        }

        File librariesDir = new File("./libraries/"); //Server is working directory/libraries/
        if (e.getSide() == Side.CLIENT)
        {
            try
            {
                File forgeFile = Utils.getJar(MinecraftForge.class);
                //Client is harder, we have to un-mavenize the location of Forge.
                //This is nasty as shit, we need a better way..
                File tmp =  Utils.findFirstParent(forgeFile, "libraries");
                if (tmp == null)
                {
                    LogHelper.fatal("Could not determine libraries folder from: " + forgeFile);
                    //return;
                }
                else
                    librariesDir = tmp;
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
                LogHelper.fatal("Could not determine Forge library file");
                return;
            }
        }

        File libFile = Utils.updateMercurius(librariesDir, ForgeVersion.mcVersion);
        if (libFile == null)
        {
            LogHelper.fatal("Mercurius Updating failed");
            return;
        }


        URLClassLoader cl;
        try
        {
            cl = URLClassLoader.newInstance(new URL[] {libFile.toURI().toURL()}, StatsModStub.class.getClassLoader());
            loadedMercurius = cl.loadClass("net.minecraftforge.mercurius.StatsMod");
            loadedMercuriusInstance = loadedMercurius.newInstance();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }

        invokeEvent("preInit", e);
    }

    @EventHandler
    public void init(FMLInitializationEvent e)
    {
        invokeEvent("init", e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
        invokeEvent("postInit", e);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent e)
    {
        invokeEvent("serverStarting", e);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent e)
    {
        invokeEvent("serverStopping", e);
    }

    @SuppressWarnings("unchecked")
    private void invokeEvent(String name, FMLEvent event)
    {
        if (loadedMercurius == null)
            return;

        try {
            ReflectionHelper.findMethod(loadedMercurius, loadedMercuriusInstance, new String[]{ name }, event.getClass())
                .invoke(loadedMercuriusInstance, event);
        } catch (UnableToFindMethodException e) {
            // No method found so its not listening for it.
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
