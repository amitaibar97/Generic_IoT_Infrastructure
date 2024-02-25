package plug_and_play;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class DynamicJarLoader {
    private final String interfaceName;

    public DynamicJarLoader(String interfaceName) {
        this.interfaceName = interfaceName;

    }

    public ArrayList<Class<?>> load(String jarFilePath) throws IOException, ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<>();
        URL[] urls = {new URL("jar:file:" + jarFilePath + "!/")};
        URLClassLoader classLoader = URLClassLoader.newInstance(urls);

        try (JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);

                    className = className.replace('/', '.');

                    Class myClass = classLoader.loadClass(className);
                    Class<?>[] interfaces = myClass.getInterfaces();
                    for (Class<?> inter : interfaces) {
                        if (inter.getSimpleName().equals(interfaceName)) {
                            classes.add(myClass);
                        }
                    }
                }
            }
        }

        return classes;
    }
}
