package db;

import com.sun.media.sound.InvalidDataException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.json.Json;
import javax.json.JsonObject;
import java.sql.SQLException;

import static org.bson.assertions.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IoTCRUDTest {

    private final IoTCRUD crud = new IoTCRUD();
    private final String COMPANY_NAME_VALUE = "TestCompany";
    private final String PRODUCT_NAME_VALUE = "TestProduct2";
    private final String SERIAL_NUMBER_VALUE = "TestSerialNumber";
    private final String USERNAME_VALUE = "TestUsername";
    private final String USER_EMAIL_VALUE = "Test@test.com";

    private static ObjectId iotID;

    @Test
    @Order(1)
    void registerCompanyCRUD() throws SQLException, InvalidDataException {
        System.out.println("1 start");
        JsonObject request = Json.createObjectBuilder()
                .add(IoTCRUD.COMPANY_NAME_KEY, COMPANY_NAME_VALUE)
                .build();

        JsonObject response = crud.registerCompanyCRUD(request);
        assertNotNull(response);
        assertEquals(COMPANY_NAME_VALUE, response.getString(IoTCRUD.COMPANY_NAME_KEY));
        System.out.println("1 end");
    }

    @Test
    @Order(2)
    void registerProductCRUD() throws SQLException, InvalidDataException {
        System.out.println("2 start");
        JsonObject request = Json.createObjectBuilder()
                .add(IoTCRUD.COMPANY_NAME_KEY, COMPANY_NAME_VALUE)
                .add(IoTCRUD.PRODUCT_NAME_KEY, PRODUCT_NAME_VALUE)
                .build();

        JsonObject response = crud.registerProductCRUD(request);
        assertNotNull(response);
        assertEquals(COMPANY_NAME_VALUE, response.getString(IoTCRUD.COMPANY_NAME_KEY));
        assertEquals(PRODUCT_NAME_VALUE, response.getString(IoTCRUD.PRODUCT_NAME_KEY));
        System.out.println("2 end");
    }

    @Test
    @Order(3)
    void registerIotCRUD() throws SQLException, InvalidDataException {
        System.out.println("3 start");
        JsonObject request = Json.createObjectBuilder()
                .add(IoTCRUD.COMPANY_NAME_KEY, COMPANY_NAME_VALUE)
                .add(IoTCRUD.PRODUCT_NAME_KEY, PRODUCT_NAME_VALUE)
                .add(IoTCRUD.SERIAL_NUMBER_KEY, SERIAL_NUMBER_VALUE)
                .add(IoTCRUD.USERNAME_KEY, USERNAME_VALUE)
                .add(IoTCRUD.USER_EMAIL_KEY, USER_EMAIL_VALUE)
                .build();

        JsonObject response = crud.registerIotCRUD(request);

        assertNotNull(response);
        assertNotNull(response.getString(IoTCRUD.IOT_ID_KEY));
        assertEquals(COMPANY_NAME_VALUE, response.getString(IoTCRUD.COMPANY_NAME_KEY));
        assertEquals(PRODUCT_NAME_VALUE, response.getString(IoTCRUD.PRODUCT_NAME_KEY));
        assertEquals(SERIAL_NUMBER_VALUE, response.getString(IoTCRUD.SERIAL_NUMBER_KEY));
        assertEquals(USERNAME_VALUE, response.getString(IoTCRUD.USERNAME_KEY));
        assertEquals(USER_EMAIL_VALUE, response.getString(IoTCRUD.USER_EMAIL_KEY));

        iotID = new ObjectId(response.getString(IoTCRUD.IOT_ID_KEY));
        System.out.println(iotID);
        System.out.println("3 end");
    }

    @Test
    @Order(4)
    void updateCRUD() throws SQLException, InvalidDataException {
        System.out.println("4 start");
        System.out.println(iotID);
        JsonObject request = Json.createObjectBuilder()
                .add(IoTCRUD.COMPANY_NAME_KEY, COMPANY_NAME_VALUE)
                .add(IoTCRUD.PRODUCT_NAME_KEY, PRODUCT_NAME_VALUE)
                .add(IoTCRUD.IOT_ID_KEY, iotID.toString())
                .add("sensor 1", 42)
                .add("sensor 2", "ok")
                .add("sensor 3", true)
                .build();

        JsonObject response = crud.updateCRUD(request);

        assertNotNull(response);
        assertNotNull(response.getString(IoTCRUD.IOT_ID_KEY));
        assertEquals(COMPANY_NAME_VALUE, response.getString(IoTCRUD.COMPANY_NAME_KEY));
        assertEquals(PRODUCT_NAME_VALUE, response.getString(IoTCRUD.PRODUCT_NAME_KEY));

        System.out.println(response);

        System.out.println("4 end");

    }
}

