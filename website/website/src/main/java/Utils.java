

import java.io.File;
import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class Utils {
	
	private static SqlCRUD sqlCrud;
	private static Object obj = new Object();
	
	public static File getStaticFile(ServletContext context, String path) {
		String htmlFilePath = context.getRealPath("static/" + path);
		return new File(htmlFilePath);
	}
	
	public static SqlCRUD getSqlCrud() throws ServletException {
		if (sqlCrud == null) {
			synchronized (obj) {
				if (sqlCrud == null) {
					try {
						Class.forName("com.mysql.cj.jdbc.Driver");
						sqlCrud = new SqlCRUD();
					} catch (ClassNotFoundException | SQLException e) {
						throw new ServletException(e);
					}
				}
			}
		}
		return sqlCrud;
	}
	
	public static JsonObject parseRequest(HttpServletRequest request, String[] keys) {
		JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
		String param;
		for (String key : keys) {
			param = request.getParameter(key);
			dataBuilder.add(key, param);
		}
		
		return dataBuilder.build();
	}

}
