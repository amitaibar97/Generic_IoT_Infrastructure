

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientTest implements Runnable {

    public static void main(String[] args) throws IOException {
//        for (int i = 0; i < 5; ++i) {
        new Thread(new ClientTest()).start();
//        }
    }

    public void run() {
        try {
            // Create a SocketChannel object.
            SocketChannel socketChannel = SocketChannel.open();


            // Connect the SocketChannel to the server.
            InetSocketAddress serverAddress = new InetSocketAddress(
                    "localhost", 8080);
            socketChannel.connect(serverAddress);
            System.out.println(socketChannel);

            for (int i = 0; i < 3; ++i) {

                // Create a ByteBuffer object to store the data you want to send.
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                String request = "{\"operation\": \"RegisterProduct\", " +
                        "\"company_name\": \"dummy\"," +
                        "\"product_name\": \"locker\"," +
                        "\"serial_number\": \"123456\"," +
                        "\"username\": \"john_doe\"," +
                        "\"user_email\": \"john.doe@example.com\"}";
                // Write the data to the ByteBuffer object.
//                byteBuffer.put(request.getBytes(), 0, request.getBytes().length);
                byteBuffer.put(request.getBytes());
                byteBuffer.flip();
                // Write the ByteBuffer object to the SocketChannel.
                socketChannel.write(byteBuffer);



                ByteBuffer response = ByteBuffer.allocate(500);
                socketChannel.read(response);
                response.flip();
                String responseStr = new String(response.array(), 0, response.remaining());

                System.out.println(responseStr);


            }
            socketChannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
