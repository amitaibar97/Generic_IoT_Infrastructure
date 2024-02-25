import com.sun.media.sound.InvalidDataException;

import org.jetbrains.annotations.NotNull;
import thread_pool.ThreadPool;
import db.IoTCRUD;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;


public class GatewayServer {
    private final MultiProtocolServer multiProtocolServer;
    private final RequestHandler requestHandler;
    private final PlugAndPlay plugAndPlay;

    public GatewayServer() throws SQLException {
        this.multiProtocolServer = new MultiProtocolServer();
        this.requestHandler = new RequestHandler();

        try {
            this.plugAndPlay = new PlugAndPlay(Callable.class.getName(), "call", "/home/amitaib97/Desktop/mockGateway");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            this.multiProtocolServer.addTCPConnection(8083);
            this.multiProtocolServer.addUDPConnection(8081);
            this.multiProtocolServer.start();
            new Thread(this.plugAndPlay).start();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void stop() {
        this.multiProtocolServer.stop();
        this.plugAndPlay.stop();
    }

    private void handle(ByteBuffer buffer, Communicator communicator) {
        this.requestHandler.handle(buffer, communicator);
    }

    private void addToFactory(String key, Function<JsonObject, Command> commandRecipe) {
        requestHandler.addToFactory(key, commandRecipe);
    }
    private interface CrudOp {
        JsonObject operate(IoTCRUD manager, JsonObject data) throws SQLException, InvalidDataException;
    }

    public enum Operation {
        REG_COMPANY("RegisterCompany",
                new String[]{IoTCRUD.COMPANY_NAME_KEY},
                IoTCRUD::registerCompanyCRUD),
        REG_PRODUCT("RegisterProduct",
                new String[]{IoTCRUD.COMPANY_NAME_KEY,
                        IoTCRUD.PRODUCT_NAME_KEY},
                IoTCRUD::registerProductCRUD),
        REG_IOT("RegisterIot", new String[]{IoTCRUD.COMPANY_NAME_KEY,
                IoTCRUD.PRODUCT_NAME_KEY, IoTCRUD.SERIAL_NUMBER_KEY,
                IoTCRUD.USERNAME_KEY, IoTCRUD.USER_EMAIL_KEY},
                IoTCRUD::registerIotCRUD),
        REG_UPDATE("update", new String[]{IoTCRUD.COMPANY_NAME_KEY,
                IoTCRUD.PRODUCT_NAME_KEY, IoTCRUD.IOT_ID_KEY},
                IoTCRUD::updateCRUD);

        private final String op;
        private final String[] labels;

        private final CrudOp operate;

        Operation(String op, String[] labels, CrudOp operate) {
            this.op = op;
            this.labels = Arrays.copyOf(labels, labels.length);
            this.operate = operate;
        }

        public String getOp() {
            return op;
        }

        private String[] getLabels() {
            return Arrays.copyOf(labels, labels.length);
        }

        public JsonObject operate(IoTCRUD manager, JsonObject data) throws SQLException, InvalidDataException {
            return operate.operate(manager, data);
        }
}


    private interface Communicator {
        ByteBuffer receive();

        void send(ByteBuffer buffer);

    }

    private interface Command {
        void exec() throws InvalidDataException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, SQLException;
    }

    private static class RequestHandler {

        private ThreadPool threadPool;
        private final Creator<String, JsonObject, Command> factory;

        private final IoTCRUD managerCRUD = new IoTCRUD();

        private RequestHandler() {
            threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            factory = new Creator<>();
            initFactory();
        }

        private void initFactory() {
            basicOperationInit(Operation.REG_COMPANY);
            basicOperationInit(Operation.REG_PRODUCT);
            basicOperationInit(Operation.REG_IOT);
            basicOperationInit(Operation.REG_UPDATE);
        }

        private void basicOperationInit(Operation operation) {
            factory.add(operation.getOp(), data -> () -> {
                System.out.println(operation.getOp());
                if (!validateJson(data, operation.getLabels())) {
                    throw new InvalidDataException("InValid Request fields");
                }
                operation.operate(managerCRUD, data);
            });
        }

        private boolean validateJson(JsonObject data, String[] labels) {
            for (String label : labels) {
                if (!data.containsKey(label)) {
                    return false;
                }
            }
            return true;
        }

        private JsonObject strToJson(String data) {
            StringReader stringReader = new StringReader(data);
            try (JsonReader jsonReader = Json.createReader(stringReader)) {
                return jsonReader.readObject();
            }
        }

        private void handle(ByteBuffer buffer, Communicator communicator) {
            if (null == buffer) {
                return;
            }
            this.threadPool.submit(createRunnable(buffer, communicator), ThreadPool.Priority.DEFAULT);
        }

        private Entry<String, JsonObject> parse(String request) throws InvalidDataException {
            JsonObject data = strToJson(request);
            String operation = data.getString("operation");
            if (null == operation) {
                return null;
            }
            return new ParseEntry(operation, data);

        }

        public void addToFactory(String key, Function<JsonObject, Command> commandRecipe) {
            factory.add(key, commandRecipe);
        }

        public class Creator<K, D, V> {
            HashMap<K, Function<D, ? extends V>> typeCreators = new HashMap<>();

            public V create(K key, D data) {
                Function<D, ? extends V> recipe = typeCreators.get(key);
                if (null == recipe) {
                    return null;
                }
                return recipe.apply(data);
            }

            public V create(K key) {
                return this.create(key, null);
            }

            public void add(K key, Function<D, ? extends V> creator) {
                typeCreators.put(key, creator);
            }
        }


        private @NotNull Runnable createRunnable(ByteBuffer buffer, Communicator communicator) {
            ByteBuffer bufferDup = buffer.duplicate();

            return () -> {
                try {
                    String request = new String(bufferDup.array(), 0, bufferDup.remaining());
                    System.out.println("request before paring: " +request);
                    Entry<String, JsonObject> parseEntry = parse(request);
                    if (null == parseEntry) {
                        throw new InvalidDataException("Bad Request");
                    }

                    Command cmd = factory.create(parseEntry.getKey(), parseEntry.getValue());
                    if (null == cmd) {
                        throw new InvalidDataException(parseEntry.getKey() + " Bad Command");
                    }

                    cmd.exec();
                    communicator.send(ByteBuffer.wrap((parseEntry.getKey() + " Successes").getBytes()));
                } catch (InvalidDataException | SQLException e) {
                    communicator.send(ByteBuffer.wrap(e.getMessage().getBytes()));
                } catch (InvocationTargetException | NoSuchMethodException | InstantiationException| IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            };

        }
        private static class ParseEntry implements Entry<String, JsonObject> {
            private final String KEY;
            private JsonObject value;

            public ParseEntry(String key, JsonObject value) {
                this.KEY = key;
                this.value = value;
            }

            @Override
            public String getKey() {
                return KEY;
            }

            @Override
            public JsonObject getValue() {
                return value;
            }

            @Override
            public JsonObject setValue(JsonObject newValue) {
                JsonObject oldValue = this.value;
                this.value = newValue;
                return oldValue;
            }
        }

    }

    private class MultiProtocolServer {
        private final CommunicationManager communicationManger;
        private final MessageManager messageManager;


        public MultiProtocolServer() {
            this.communicationManger = new CommunicationManager();
            this.messageManager = new MessageManager();
        }

        public void addTCPConnection(int clientPort) throws IOException {
            this.communicationManger.addTCPConnection(clientPort);
        }

        public void addUDPConnection(int clientPort) throws IOException {
            this.communicationManger.addUDPConnection(clientPort);
        }

        public void stop() {
            this.communicationManger.stop();
        }


        public void start() throws IOException, ClassNotFoundException {
            this.communicationManger.start();
        }

        /*=================================================================================================*/
        /*===================================== Massage Handlers =====================================*/
        /*=================================================================================================*/

        private class MessageManager {
            public void handle(@NotNull Communicator communicator) throws IOException, ClassNotFoundException {
                ByteBuffer byteBuffer = communicator.receive();
                GatewayServer.this.handle(byteBuffer, communicator);
            }
        }

        /*=================================================================================================*/
        /*===================================== Communication Manager =====================================*/
        /*=================================================================================================*/

        private class CommunicationManager {

            private final Selector selector;
            private boolean isRunning;
            private final SelectorRunner selectorRunner;


            public CommunicationManager() {
                try {
                    this.selector = Selector.open();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.isRunning = true;
                this.selectorRunner = new SelectorRunner();
            }

            public void addTCPConnection(int TCPClientPort) throws IOException {
                ServerSocketChannel tcpServerSocket = ServerSocketChannel.open();
                tcpServerSocket.configureBlocking(false);
                tcpServerSocket.bind(new InetSocketAddress("localhost", TCPClientPort));
                tcpServerSocket.register(selector, SelectionKey.OP_ACCEPT);

            }

            public void addUDPConnection(int UDPClientPort) throws IOException {
                DatagramChannel udpServerSocket = DatagramChannel.open();
                udpServerSocket.configureBlocking(false);
                udpServerSocket.bind(new InetSocketAddress("localhost", UDPClientPort));
                udpServerSocket.register(selector, SelectionKey.OP_READ);
            }

            public void start() {
                new Thread(this.selectorRunner).start();
            }

            public void stop() {
                this.isRunning = false;
            }

            /*================================ Selector Runner ==============================================*/

            private class SelectorRunner implements Runnable {
                private final TCPRegister tcpRegister;

                public SelectorRunner() {
                    this.tcpRegister = new TCPRegister();
                }

                @Override
                public void run() {
                    Set<SelectionKey> selectedKeys = null;
                    while (isRunning) {
                        try {
                            selector.select();
                            selectedKeys = selector.selectedKeys();
                            Iterator<SelectionKey> iter = selectedKeys.iterator();
                            Communicator communicator;

                            while (iter.hasNext()) {
                                SelectionKey key = iter.next();

                                if (key.isAcceptable()) {
                                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                                    this.tcpRegister.TCPAccept(serverSocketChannel);

                                } else if (key.isReadable()) {
                                    SelectableChannel channel = key.channel();
                                    if (channel instanceof SocketChannel) { // TCP
                                        communicator = (TCPCommunicator) key.attachment();
                                        MultiProtocolServer.this.messageManager.handle(communicator);
                                    } else { // UDP
                                        DatagramChannel datagramChannel = (DatagramChannel) channel;
                                        datagramChannel.disconnect();
                                        communicator = new UDPCommunicator(datagramChannel);
                                        MultiProtocolServer.this.messageManager.handle(communicator);

                                    }
                                }
                                iter.remove();
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    assert selectedKeys != null;
                    selectedKeys.clear();
                }
            }


            /*======================================== TCP register =================================================*/

            private class TCPRegister {
                public TCPRegister() {
                }

                public TCPCommunicator TCPAccept(ServerSocketChannel serverSocketChannel) {
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                        TCPCommunicator tcpCommunicator = new TCPCommunicator(socketChannel);
                        key.attach(tcpCommunicator);
                        return tcpCommunicator;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            /*========================================== Communicators =================================================*/

            private class TCPCommunicator implements Communicator {
                private final SocketChannel clientSocketChannel;
                private final String HTTP_RESPONSE_TEMPLATE = "HTTP/1.1 %d\n" + "Content-Type: application/json\n" + "\n" + "{\"status\": \"%s\"}";
                private final String TCP_RESPONSE_TEMPLATE = "{\"status\": \"%s\"}";
                private boolean isHTTP = false;



                public TCPCommunicator(SocketChannel clientSocketChannel) {
                    this.clientSocketChannel = clientSocketChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = clientSocketChannel.read(buffer);
                        if (bytesRead == -1) {
                            clientSocketChannel.close();
                            return null;
                        }

                        buffer.flip();
                        return parseHTTP(buffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                private @NotNull ByteBuffer parseHTTP(@NotNull ByteBuffer buffer) {
                    ByteBuffer bufferDup = buffer.duplicate();
                    String request = new String(bufferDup.array(), 0, bufferDup.remaining());

                    String[] splitRequest = request.split("\\{", 2);
                    if (0 != splitRequest[0].length()) {
                        isHTTP = true;
                    }

                    String body = "{" + splitRequest[1];
                    return ByteBuffer.wrap(body.getBytes());
                }


                @Override
                public void send(ByteBuffer buffer) {
                    try {
                        if (!clientSocketChannel.isOpen() || !clientSocketChannel.isConnected()) {
                            System.out.println("SocketChannel is not open or connected!");
                            return;
                        }
                        ByteBuffer bufferDup = buffer.duplicate();
                        String responseMsg = new String(bufferDup.array(), 0, bufferDup.remaining());

                        String response;
                        if (isHTTP) {
                            String responseBody = new String(buffer.array(), StandardCharsets.UTF_8).trim();
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json\r\n" +
                                    "Content-Length: " + responseMsg.getBytes().length + "\r\n" +
                                    "\r\n" +
                                    responseBody;
                        } else {
                            response = String.format(TCP_RESPONSE_TEMPLATE, responseMsg);
                        }

                        buffer = ByteBuffer.wrap(response.getBytes());
//                        buffer.limit(buffer.array().length);

                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(buffer);
                        }
                        if (isHTTP) {
                            clientSocketChannel.shutdownOutput();
                        }
//                        clientSocketChannel.finishConnect();


                        System.out.println("Sending response to " + clientSocketChannel);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            private class UDPCommunicator implements Communicator {

                private final DatagramChannel clientDatagramChannel;
                private SocketAddress clientAddress;

                public UDPCommunicator(DatagramChannel clientDatagramChannel) {
                    this.clientDatagramChannel = clientDatagramChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        this.clientAddress = clientDatagramChannel.receive(buffer);
                        buffer.flip();
                        return buffer;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void send(ByteBuffer buffer) {
                    try {
//                        buffer.limit(buffer.array().length);
                        this.clientDatagramChannel.send(buffer, clientAddress);
                        buffer.flip();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
}

    private class PlugAndPlay implements Runnable {
        private MonitorDir monitorDir;
        private DynamicJarLoader dynamicJarLoader;
        private String methodName;

        private boolean isRunning = true;

        public PlugAndPlay(String interfaceName, String methodName, String pathToDir) throws IOException {
            this.monitorDir = new MonitorDir(pathToDir);
            this.dynamicJarLoader = new DynamicJarLoader(interfaceName);
            this.methodName = methodName;
        }

        @Override
        public void run() {
            while (this.isRunning) {
                try {
                    File file = this.monitorDir.watchDirectoryPath();
//                    if (file == null) {
//                        throw new RuntimeException("no such file");
//                    }
                    ArrayList<Class<?>> classes = this.dynamicJarLoader.load(file.getAbsolutePath());
                    for (Class<?> cls : classes) {
                        addToFactory(cls);
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

        }

        public void stop(){
            this.isRunning = false;
        }
        private void addToFactory(Class<?> cls) {
            Function<JsonObject, Command> function = s -> () -> {
                Method method = cls.getMethod(this.methodName);
                method.invoke(cls.newInstance());
            };

            requestHandler.factory.add(cls.getName(), function);
            System.out.println("Created new API method : " + cls.getName());
        }

        private class MonitorDir {
            private final String pathName;

            public MonitorDir(String pathName) {
                this.pathName = pathName;
            }


            public File watchDirectoryPath() {
                Path path = Paths.get(this.pathName);

                FileSystem fs = path.getFileSystem();

                try {
                    WatchService watchService = fs.newWatchService();
                    path.register(watchService, ENTRY_CREATE);

                    WatchKey key;
                    do {
                        key = watchService.take();

                        for (WatchEvent<?> watchEvent : key.pollEvents()) {
                            Path newFile = (Path) watchEvent.context();
                            Path absoultPath = path.resolve(newFile);
                            File file = absoultPath.toFile();
                            while (!file.canRead()) ;
                            return file;
                        }
                    } while (key.reset());

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

        }

        private class DynamicJarLoader {
            private final String interfaceName;


            public DynamicJarLoader(String interfaceName) {
                this.interfaceName = interfaceName;
            }


            public ArrayList<Class<?>> load(String pathToJar) {
                JarFile jarFile = null;
                Enumeration<JarEntry> enumeration;
                ArrayList<Class<?>> availableClasses;
                URL[] urls;
                try {
                    jarFile = new JarFile(pathToJar);
                    enumeration = jarFile.entries();
                    availableClasses = new ArrayList<>();
                    urls = new URL[]{new URL("jar:file:" + jarFile.getName() + "!/")};
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                URLClassLoader cl = URLClassLoader.newInstance(urls);

                while (enumeration.hasMoreElements()) {
                    JarEntry je = enumeration.nextElement();
                    String className = je.getName();
                    if ((className.endsWith(".class"))) {
                        try {
                            className = className.substring(0, className.length() - 6);
                            className = className.replace('/', '.');
                            Class<?> myClass = cl.loadClass(className);
                            Class<?>[] interfaces = myClass.getInterfaces();

                            for (Class<?> cls : interfaces) {
                                if (this.interfaceName.equals(cls.getName())) {
                                    availableClasses.add(myClass);
                                }
                            }

                        } catch (ClassNotFoundException e) {
                            System.out.println("Class " + className + " was not found!" + e);
                        }
                    }
                }
                return availableClasses;
            }
        }

    }
    private static JsonObject parseStringToJson(String input) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        // Split the input string by '@'
        String[] keyValuePairs = input.split("//");

        for (String pair : keyValuePairs) {
            // Split each pair by ':'
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                // Add the key-value pair to the JsonObjectBuilder
                jsonBuilder.add(keyValue[0], keyValue[1]);
            } else {
                // Handle invalid format (e.g., missing value)
                System.err.println("Invalid format for pair: " + pair);
            }
        }

        return jsonBuilder.build();
    }

    private static boolean isValid(JsonObject data, String @NotNull [] keys){
        for (String key : keys){
            if(!data.containsKey(key)) {
                return false;
            }
        }

        return true;
    }


    public static void main(String[] args) throws SQLException {
        GatewayServer gatewayServer = new GatewayServer();
        gatewayServer.start();
    }
}

