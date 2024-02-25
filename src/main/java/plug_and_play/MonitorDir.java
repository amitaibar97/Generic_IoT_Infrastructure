package plug_and_play;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class MonitorDir {

    DynamicJarLoader jarLoader;

    public MonitorDir(String interfaceName) {
        this.jarLoader = new DynamicJarLoader(interfaceName);
    }

    public void watchDirectoryPath(String pathName) {
        Path path = Paths.get(pathName);



        FileSystem fs = path.getFileSystem();

        try {
            WatchService watchService = fs.newWatchService();
            path.register(watchService, ENTRY_CREATE);

            WatchKey key = null;
            do {
                key = watchService.take();

                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    Path newFile = (Path) watchEvent.context();
                    Path absoultPath = path.resolve(newFile);

                    File file = absoultPath.toFile();
                    while (!file.canRead());
                    ArrayList<Class<?>> classes = this.jarLoader.load(file.getAbsolutePath());
                    //TODO to something for all the loaded classes
                }

            } while (key.reset()); // ?

        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
