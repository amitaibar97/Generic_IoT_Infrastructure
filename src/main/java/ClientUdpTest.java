import java.io.IOException;
import java.net.*;

public class ClientUdpTest {

    public static void main(String[] args) {
        String serverHostname = "localhost";
        int serverPort = 8081;

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(serverHostname);

            // JSON message for registering a company
            String jsonMessage = "{\"operation\":\"RegisterProduct\",\"company_name\":\"ExampleCompany\", \"product_name\":\"ExampleProduct\"}";
            byte[] sendData = jsonMessage.getBytes();

            // Create a UDP packet to send to the server
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);

            // Send the packet
            clientSocket.send(sendPacket);

            System.out.println("Message sent to the server: " + jsonMessage);

            // Receive the response from the server
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            // Display the received message
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received from server: " + receivedMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
