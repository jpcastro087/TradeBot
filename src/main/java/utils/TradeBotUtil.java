package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.CollectionUtil;

public class TradeBotUtil {
    public static List<JSONObject> resultSetToListJSON(ResultSet rs) {
        List<JSONObject> jsonObjects = new ArrayList<>();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                int numColumns = rsmd.getColumnCount();
                JSONObjectCustom obj = new JSONObjectCustom();
                for (int i = 1; i <= numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    Object object = parsearTipoDato(rs.getObject(column_name));
                    obj.put(column_name, object);
                }
                jsonObjects.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObjects;
    }

    public static String listToJsonString(List<JSONObject> listJSONObject) {
        JSONArray json = new JSONArray();
        if (!CollectionUtil.isNullOrEmpty(listJSONObject))
            for (JSONObject jsonObject : listJSONObject)
                json.put(jsonObject);
        return json.toString();
    }

    public static JSONObject resultSetToJSON(ResultSet rs) {
        JSONObjectCustom jSONObject = null;
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int count = 0;
            while (rs.next()) {
                jSONObject = new JSONObjectCustom();
                count++;
                if (count > 1) {
                    throw new IncorrectResultSetSizeException(
                            "Esperaba devolver uno y devuelve mutilice el mLaSegundaUtil.resultSetToJsonList(rs)");
                }
                int numColumns = rsmd.getColumnCount();
                for (int i = 1; i <= numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    Object object = parsearTipoDato(rs.getObject(column_name));

                    jSONObject.put(column_name, object);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jSONObject;
    }

    private static Object parsearTipoDato(Object object) {
        Object objectResult = object;
        if (object instanceof Number) {
            Number num = (Number)object;
            try {
                objectResult = Integer.valueOf(Integer.parseInt(num.toString()));
                return objectResult;
            } catch (NumberFormatException numberFormatException) {
                try {
                    objectResult = Long.valueOf(Long.parseLong(num.toString()));
                    return objectResult;
                } catch (NumberFormatException numberFormatException1) {
                    try {
                        objectResult = Float.valueOf(Float.parseFloat(num.toString()));
                        return objectResult;
                    } catch (NumberFormatException numberFormatException2) {
                        try {
                            objectResult = Double.valueOf(Double.parseDouble(num.toString()));
                            return objectResult;
                        } catch (NumberFormatException numberFormatException3) {}
                    }
                }
            }
        }
        return objectResult;
    }

    public static class IncorrectResultSetSizeException extends Exception {
        private static final long serialVersionUID = 1L;

        public IncorrectResultSetSizeException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class JSONObjectCustom extends JSONObject {
        public JSONObject put(String key, Object value) throws JSONException {
            return super.put(key, (value == null) ? JSONObject.NULL : value);
        }
    }
}
