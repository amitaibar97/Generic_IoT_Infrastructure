

import javax.json.JsonObject;
import java.sql.SQLException;

public interface CRUD {
    JsonObject createCRUD(JsonObject jsonObject) throws SQLException;

    JsonObject readCRUD(JsonObject jsonObject) throws SQLException;

    JsonObject updateCRUD(JsonObject jsonObject) throws SQLException;

    JsonObject deleteCRUD(JsonObject jsonObject) throws SQLException;

}

