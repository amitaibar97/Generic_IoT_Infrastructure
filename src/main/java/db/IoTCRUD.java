package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sun.media.sound.InvalidDataException;
import org.bson.types.ObjectId;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.bson.Document;

import java.sql.SQLException;
import java.util.Map;

public class IoTCRUD {
    private final static String USERS_COLLECTION_STRING_FORMAT = "%s_users";
    private final static String UPDATES_COLLECTION_STRING_FORMAT = "%s_updates";
    public final static String COMPANY_NAME_KEY = "company_name";
    public final static String PRODUCT_NAME_KEY = "product_name";
    public final static String SERIAL_NUMBER_KEY = "serial_number";
    public final static String USERNAME_KEY = "username";
    public final static String USER_EMAIL_KEY = "user_email";
    public final static String IOT_ID_KEY = "iot_id";
    private final CRUD companyCrud;
    private final CRUD productCrud;
    private final CRUD iotCrud;
    private final CRUD updateCrud;


    public IoTCRUD(String connectionString) {
        this.companyCrud = new CompanyCRUDImp(connectionString);
        this.productCrud = new ProductCRUDImp(connectionString);
        this.iotCrud = new IotCRUDImp(connectionString);
        this.updateCrud = new UpdateCRUDImp(connectionString);
    }

    public IoTCRUD() {
        this("mongodb://localhost:27017");
    }

    public JsonObject registerCompanyCRUD(JsonObject data) throws SQLException, InvalidDataException {
       return this.companyCrud.createCRUD(data);
    }

    public JsonObject registerProductCRUD(JsonObject data) throws SQLException, InvalidDataException {
       return this.productCrud.createCRUD(data);
    }

    public JsonObject registerIotCRUD(JsonObject data) throws SQLException, InvalidDataException {
        return this.iotCrud.createCRUD(data);
    }

    public JsonObject updateCRUD(JsonObject data) throws SQLException, InvalidDataException {
        return this.updateCrud.createCRUD(data);
    }

    private static class CompanyCRUDImp implements CRUD {
        private final String connectionString;

        public CompanyCRUDImp(String connectionString) {
            this.connectionString = connectionString;
        }

        @Override
        public JsonObject createCRUD(JsonObject data) {
            String companyName = data.getString(COMPANY_NAME_KEY);
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                mongoClient.getDatabase(companyName);
            }
            return data;
        }
        //not implemented yet, not part of the current api
        @Override
        public JsonObject readCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {
            return null;
        }
    }

    private static class ProductCRUDImp implements CRUD {
        private final String connectionString;

        public ProductCRUDImp(String connectionString) {
            this.connectionString = connectionString;
        }

        @Override
        public JsonObject createCRUD(JsonObject data) {
            String companyName = data.getString(COMPANY_NAME_KEY);
            String productName = data.getString(PRODUCT_NAME_KEY);
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                MongoDatabase database = mongoClient.getDatabase(companyName);
                database.createCollection(String.format(USERS_COLLECTION_STRING_FORMAT, productName)); // create collection for users
                database.createCollection(String.format(UPDATES_COLLECTION_STRING_FORMAT, productName)); // create collection for updates
            }
            return data;
        }

        @Override
        public JsonObject readCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) {
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject data) {
            return null;
        }
    }

    private static class IotCRUDImp implements CRUD {

        private final String connectionString;

        public IotCRUDImp(String connectionString) {
            this.connectionString = connectionString;
        }

        @Override
        public JsonObject createCRUD(JsonObject data){
            String companyName = data.getString(COMPANY_NAME_KEY);
            String productName = data.getString(PRODUCT_NAME_KEY);
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                MongoDatabase database = mongoClient.getDatabase(companyName);
                MongoCollection<Document> usersCollection =
                        database.getCollection(String.format(USERS_COLLECTION_STRING_FORMAT, productName));

                Document dataDocument = Document.parse(data.toString());
                dataDocument.remove(COMPANY_NAME_KEY);
                dataDocument.remove(PRODUCT_NAME_KEY);

                usersCollection.insertOne(dataDocument);

                dataDocument.append(COMPANY_NAME_KEY, companyName);
                dataDocument.append(PRODUCT_NAME_KEY, productName);

                ObjectId objectId = dataDocument.getObjectId("_id");
                dataDocument.remove("_id");
                dataDocument.append(IOT_ID_KEY, objectId);

                return convertDocumentToJson(dataDocument);

            }
        }

        @Override
        public JsonObject readCRUD(JsonObject data) throws SQLException {
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject data) throws SQLException {
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject jsonObject) throws SQLException, InvalidDataException {
            return null;
        }
    }

    private class UpdateCRUDImp implements CRUD {

        private final String connectionString;

        public UpdateCRUDImp(String connectionString) {
            this.connectionString = connectionString;
        }

        @Override
        public JsonObject createCRUD(JsonObject data) throws SQLException, InvalidDataException {
            String companyName = data.getString(COMPANY_NAME_KEY);
            String productName = data.getString(PRODUCT_NAME_KEY);

            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                MongoDatabase database = mongoClient.getDatabase(companyName);
                MongoCollection<Document> updatesCollection = database.getCollection(String.format(UPDATES_COLLECTION_STRING_FORMAT, productName));

                Document update = Document.parse(data.toString());

                update.remove(COMPANY_NAME_KEY);
                update.remove(PRODUCT_NAME_KEY);

                updatesCollection.insertOne(update);

                update.append(COMPANY_NAME_KEY, companyName);
                update.append(PRODUCT_NAME_KEY, productName);

                return convertDocumentToJson(update);
            }

        }

        @Override
        public JsonObject readCRUD(JsonObject jsonObject){
            return null;
        }

        @Override
        public JsonObject updateCRUD(JsonObject jsonObject){
            return null;
        }

        @Override
        public JsonObject deleteCRUD(JsonObject jsonObject){
            return null;
        }
    }

    private static JsonObject convertDocumentToJson(Document document) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            addJsonEntry(jsonObjectBuilder, entry.getKey(), entry.getValue());
        }

        return jsonObjectBuilder.build();
    }

    private static void addJsonEntry(JsonObjectBuilder jsonObjectBuilder, String key, Object value) {
        if (value instanceof Document) {
            jsonObjectBuilder.add(key, convertDocumentToJson((Document) value));
        } else {
            jsonObjectBuilder.add(key, value.toString());
        }
    }

}