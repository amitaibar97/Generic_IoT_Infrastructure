
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.json.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@WebServlet("/product")
public class Product extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SqlCRUD crud;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		crud = Utils.getSqlCrud();
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] fields = new String[]{"company_name", 
				"product_id", 
				"product_name", 
				"product_description"}; 
		JsonObject data = Utils.parseRequest(request, fields);

		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder(data);
		jsonObjectBuilder.add("operation", "RegisterProduct");

        // Build the final JsonObject
        data = jsonObjectBuilder.build();
    	JsonObject crudResponse = null;
		try {
			crudResponse = crud.registerProductCRUD(data);
			SocketChannel socketChannel = SocketChannel.open();


            // Connect the SocketChannel to the server.
            InetSocketAddress serverAddress = new InetSocketAddress(
                    "localhost", 8083);
            socketChannel.connect(serverAddress);
            System.out.println(socketChannel);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            String jsonString = data.toString();
            byteBuffer.put(jsonString.getBytes());
            byteBuffer.flip();
            // Write the ByteBuffer object to the SocketChannel.
            socketChannel.write(byteBuffer);
            ByteBuffer res = ByteBuffer.allocate(500);
            socketChannel.read(res);
            res.flip();
            String responseStr = new String(res.array(), 0, res.remaining());

            System.out.println(responseStr);
            socketChannel.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String crudStatus = crudResponse.getString("status").replace("'", "\\'");
    	
    	
    	File index = Utils.getStaticFile(getServletContext(), "index.html");
    	Document doc = Jsoup.parse(index, null);
        doc.getElementById("body").attr("onload", "registerProductForm('" + crudStatus + "')");
    	
    	
        response.setContentType("text/html");
        
    	if (!crudStatus.endsWith("ProductRegistered")) {
    		response.setStatus(404);
    		response.getWriter().println(doc);
    		return;
    	}
    	
    	
    	doc.getElementById("responseMsg").attr("style", "color: green");
    	response.setStatus(201);
        response.getWriter().println(doc);
    }

}

