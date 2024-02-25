package db;

import com.sun.media.sound.InvalidDataException;
import org.json.JSONException;


import javax.json.JsonObject;
import java.sql.SQLException;

public interface CRUD {
    JsonObject createCRUD(JsonObject jsonObject) throws SQLException, InvalidDataException;

    JsonObject readCRUD(JsonObject jsonObject) throws SQLException;

    JsonObject updateCRUD(JsonObject jsonObject) throws SQLException;

    JsonObject deleteCRUD(JsonObject jsonObject) throws SQLException, InvalidDataException;

}

