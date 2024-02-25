package db;

import com.sun.media.sound.InvalidDataException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.sql.*;

public class SqlCRUD {

    private final CRUD companyCrud;

    private final CRUD productCrud;

    public SqlCRUD(String url, String username, String password, String dbName) throws SQLException {
        url += dbName;
        this.companyCrud = new CompanyCRUDImp(url, username, password);
        this.productCrud = new ProductCRUDImp(url, username, password);
        createTables(url, username, password);
    }

    public SqlCRUD(String username, String password) throws SQLException {
        this("jdbc:mysql://localhost:3306/", username, password, "AdminDB");
    }

    public SqlCRUD() throws SQLException {
        this("root", "12345");
    }

    private static JsonObject enrich(@NotNull JsonObject source, String key, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.forEach(builder::add);
        return builder.build();
    }


    private void createTables(String url, String username, String password) throws SQLException {
        String createCompanies = String.format(
                "CREATE TABLE IF NOT EXISTS %s " +
                        "(%s varchar(256) PRIMARY KEY, " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s BIGINT UNSIGNED);",
                Keys.COMPANIES_TABLE_NAME.key,
                Keys.COMPANY_NAME.key,
                Keys.COMPANY_ADDRESS.key,
                Keys.CONTACT_NAME.key,
                Keys.CONTACT_PHONE.key,
                Keys.CONTACT_EMAIL.key,
                Keys.SERVICE_FEE.key);

        String createCreditCards = String.format(
                "CREATE TABLE IF NOT EXISTS %s " +
                        "(%s varchar(256) PRIMARY KEY, " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s varchar(256));",
                Keys.CARD_DETAILS_TABLE_NAME.key,
                Keys.CARD_NUMBER.key,
                Keys.COMPANY_NAME.key,
                Keys.CARD_HOLDER_NAME.key,
                Keys.EX_DATE.key,
                Keys.CVV.key);

        String createProducts = String.format(
                "CREATE TABLE IF NOT EXISTS %s " +
                        "(%s BIGINT UNSIGNED PRIMARY KEY, " +
                        "%s varchar(256), " +
                        "%s varchar(256), " +
                        "%s varchar(256));",
                Keys.PRODUCTS_TABLE_NAME.key,
                Keys.PRODUCT_ID.key,
                Keys.COMPANY_NAME.key,
                Keys.PRODUCT_NAME.key,
                Keys.PRODUCT_DESCRIPTION.key);

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {

            statement.execute(createCompanies);
            statement.execute(createCreditCards);
            statement.execute(createProducts);
        } catch (SQLException e) {
            throw new SQLException("create tables failed " + e.getMessage());
        }

    }

    public JsonObject registerCompany(JsonObject data) throws SQLException, InvalidDataException {
        return companyCrud.createCRUD(data);
    }

    public JsonObject registerProduct(JsonObject data) throws SQLException, InvalidDataException {
        return productCrud.createCRUD(data);
    }


    enum Keys {
        COMPANIES_TABLE_NAME("Companies"),
        CARD_DETAILS_TABLE_NAME("CreditCardDetails"),
        PRODUCTS_TABLE_NAME("Products"),
        COMPANY_NAME("company_name"),
        COMPANY_ADDRESS("company_address"),
        CONTACT_NAME("contact_name"),
        CONTACT_PHONE("contact_phone"),
        CONTACT_EMAIL("contact_email"),
        SERVICE_FEE("service_fee"),
        CARD_NUMBER("card_number"),
        CARD_HOLDER_NAME("card_holder_name"),
        EX_DATE("ex_date"),
        CVV("CVV"),
        PRODUCT_ID("product_id"),
        PRODUCT_NAME("product_name"),
        PRODUCT_DESCRIPTION("product_description");

        final String key;

        Keys(String key) {
            this.key = key;
        }
    }

    private static class CompanyCRUDImp implements CRUD {
        private static final String CREATE_COMPANY_CMD = String.format(
                "INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES" +
                        " (?, ?, ?, ?, ?, ?)",
                Keys.COMPANIES_TABLE_NAME.key,
                Keys.COMPANY_NAME.key,
                Keys.COMPANY_ADDRESS.key,
                Keys.CONTACT_NAME.key,
                Keys.CONTACT_PHONE.key,
                Keys.CONTACT_EMAIL.key,
                Keys.SERVICE_FEE.key);
        private static final String CREATE_CREDIT_CARD_CMD = String.format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES" +
                        " (?, ?, ?, ?, ?)",
                Keys.CARD_DETAILS_TABLE_NAME.key,
                Keys.CARD_NUMBER.key,
                Keys.COMPANY_NAME.key,
                Keys.CARD_HOLDER_NAME.key,
                Keys.EX_DATE.key,
                Keys.CVV.key);
        private final String jdbcUrl;
        private final String user;
        private final String password;

        public CompanyCRUDImp(String url, String username, String password) {
            this.jdbcUrl = url;
            this.user = username;
            this.password = password;
        }

        @Override
        public JsonObject createCRUD(JsonObject data) {
            try (Connection con = DriverManager.getConnection(jdbcUrl, user, password)) {
                try (PreparedStatement stmt = con.prepareStatement(CREATE_COMPANY_CMD)) {
                    stmt.setString(1, data.getString(Keys.COMPANY_NAME.key));
                    stmt.setString(2, data.getString(Keys.COMPANY_ADDRESS.key));
                    stmt.setString(3, data.getString(Keys.CONTACT_NAME.key));
                    stmt.setString(4, data.getString(Keys.CONTACT_PHONE.key));
                    stmt.setString(5, data.getString(Keys.CONTACT_EMAIL.key));
                    stmt.setInt(6, data.getInt(Keys.SERVICE_FEE.key));

                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = con.prepareStatement(CREATE_CREDIT_CARD_CMD)) {
                    stmt.setString(1, data.getString(Keys.CARD_NUMBER.key));
                    stmt.setString(2, data.getString(Keys.COMPANY_NAME.key));
                    stmt.setString(3, data.getString(Keys.CARD_HOLDER_NAME.key));
                    stmt.setString(4, data.getString(Keys.EX_DATE.key));
                    stmt.setString(5, data.getString(Keys.CVV.key));

                    stmt.executeUpdate();
                }

                enrich(data, "status", "CompanyRegistered");
            } catch (SQLException e) {
                enrich(data, "status", e.getMessage());
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

    private static class ProductCRUDImp implements CRUD {
        private static final String CREATE_PRODUCT_CMD = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) " +
                        "VALUES (?, ?, ?, ?)",
                Keys.PRODUCTS_TABLE_NAME.key,
                Keys.PRODUCT_ID.key,
                Keys.COMPANY_NAME.key,
                Keys.PRODUCT_NAME.key,
                Keys.PRODUCT_DESCRIPTION.key);
        private final String jdbcUrl;
        private final String user;
        private final String password;

        public ProductCRUDImp(String url, String username, String password) {
            this.jdbcUrl = url;
            this.user = username;
            this.password = password;
        }

        @Override
        public JsonObject createCRUD(JsonObject data) {
            try (Connection con = DriverManager.getConnection(jdbcUrl, user, password)) {
                try (PreparedStatement stmt = con.prepareStatement(CREATE_PRODUCT_CMD)) {
                    stmt.setInt(1, data.getInt(Keys.PRODUCT_ID.key));
                    stmt.setString(2, data.getString(Keys.COMPANY_NAME.key));
                    stmt.setString(3, data.getString(Keys.PRODUCT_NAME.key));
                    stmt.setString(4, data.getString(Keys.PRODUCT_DESCRIPTION.key));

                    stmt.executeUpdate();
                }

                enrich(data, "status", "ProductRegistered");
            } catch (SQLException e) {
                enrich(data, "status", e.getMessage());
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


}
