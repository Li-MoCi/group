package com.warmer.base.util;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Lazy(false)
public class Neo4jUtil implements AutoCloseable {

    private static Driver neo4jDriver;

    private static final Logger log = LoggerFactory.getLogger(Neo4jUtil.class);

    @Autowired
    @Lazy
    public void setNeo4jDriver(Driver neo4jDriver) {
        Neo4jUtil.neo4jDriver = neo4jDriver;
    }

    /**
     * 测试neo4j连接是否打开
     */
    public static boolean isNeo4jOpen() {
        try (Session session = neo4jDriver.session()) {
            log.debug("连接成功：" + session.isOpen());
            return session.isOpen();
        }
    }

    /**
     * neo4j驱动执行cypher
     *
     * @param cypherSql cypherSql
     */

    public static void runCypherSql(String cypherSql) {
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            session.run(cypherSql);
        }
    }

    public <T> List<T> readCyphers(String cypherSql, Function<Record, T> mapper) {
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            return result.list(mapper);
        }
    }

    /**
     * 返回节点集合，此方法不保留关系
     *
     * @param cypherSql cypherSql
     */
    public static List<HashMap<String, Object>> getGraphNode(String cypherSql) {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Pair<String, Value> pair : f) {
                        String typeName = pair.value().type().name();
                        if (typeName.equals("NODE")) {
                            Node noe4jNode = pair.value().asNode();
                            Map<String, Object> nodeProperties = noe4jNode.asMap();
                            HashMap<String, Object> rss = new HashMap<>(nodeProperties);

                            rss.put("id_internal", String.valueOf(noe4jNode.id()));

                            if (!rss.containsKey("uuid")) {
                                rss.put("uuid", String.valueOf(noe4jNode.id()));
                                log.warn("Node (internal id: " + noe4jNode.id()
                                        + ") is missing 'uuid' property. Falling back to internal id for 'uuid' field.");
                            }
                            ents.add(rss);
                        }
                    }

                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }

    /**
     * 获取数据库索引
     * 
     * @return
     */
    public static List<HashMap<String, Object>> getGraphIndex() {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            String cypherSql = "call db.indexes";
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        if (key.equalsIgnoreCase("labelsOrTypes")) {
                            String objects = value.asList().stream().map(n -> n.toString())
                                    .collect(Collectors.joining(","));
                            rss.put(key, objects);
                        } else {
                            rss.put(key, value);
                        }
                    }
                    ents.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }

    public static List<HashMap<String, Object>> getGraphLabels() {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            String cypherSql = "call db.labels";
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        if (key.equalsIgnoreCase("label")) {
                            String objects = value.toString().replace("\"", "");
                            rss.put(key, objects);
                        } else {
                            rss.put(key, value);
                        }
                    }
                    ents.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }

    public static Map<String, Object> getLabelsInfo() {
        Map<String, Object> ent = new HashMap<>();
        try (Session session = neo4jDriver.session()) {
            String cypherSql = "CALL apoc.meta.stats() YIELD labels RETURN labels";
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                Record record = result.single();
                Map<String, Object> mp = record.asMap();
                ent = (Map<String, Object>) mp.get("labels");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ent;
    }

    /**
     * 删除索引
     * 
     * @param label
     */
    public static void deleteIndex(String label) {
        try (Session session = neo4jDriver.session()) {
            String cypherSql = String.format("DROP INDEX ON :`%s`(name)", label);
            session.run(cypherSql);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 创建索引
     * 
     * @param label
     * @param prop
     */
    public static void createIndex(String label, String prop) {
        try (Session session = neo4jDriver.session()) {
            String cypherSql = String.format("CREATE INDEX ON :`%s`(%s)", label, prop);
            session.run(cypherSql);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static HashMap<String, Object> getSingleGraphNode(String cypherSql) {
        List<HashMap<String, Object>> ent = getGraphNode(cypherSql);
        if (ent.size() > 0) {
            return ent.get(0);
        }
        return null;
    }

    /**
     * 获取一个标准的表格，一般用于语句里使用as
     *
     * @param cypherSql
     * @return
     */
    public static List<HashMap<String, Object>> getGraphTable(String cypherSql) {
        List<HashMap<String, Object>> resultData = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    HashMap<String, Object> rss = new HashMap<String, Object>();
                    for (Pair<String, Value> pair : f) {
                        String key = pair.key();
                        Value value = pair.value();
                        rss.put(key, value);
                    }
                    resultData.add(rss);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return resultData;
    }

    /**
     * 返回关系，不保留节点内容
     *
     * @param cypherSql
     * @return
     */
    public static List<HashMap<String, Object>> getGraphRelationShip(String cypherSql) {
        List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Pair<String, Value> pair : f) {
                        HashMap<String, Object> rss = new HashMap<String, Object>();
                        String typeName = pair.value().type().name();
                        if (typeName.equals("RELATIONSHIP")) {
                            Relationship rship = pair.value().asRelationship();
                            String uuid = String.valueOf(rship.id());
                            String sourceId = String.valueOf(rship.startNodeId());
                            String targetId = String.valueOf(rship.endNodeId());
                            Map<String, Object> map = rship.asMap();
                            for (Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                rss.put(key, entry.getValue());
                            }
                            rss.put("uuid", uuid);
                            rss.put("sourceId", sourceId);
                            rss.put("targetId", targetId);
                            ents.add(rss);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ents;
    }

    /**
     * 获取值类型的结果,如count,uuid
     *
     * @return 1 2 3 等数字类型
     */
    public static long getGraphValue(String cypherSql) {
        long val = 0;
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result cypherResult = session.run(cypherSql);
            if (cypherResult.hasNext()) {
                Record record = cypherResult.next();
                for (Value value : record.values()) {
                    val = value.asLong();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return val;
    }

    /**
     * 返回节点和关系，节点node,关系relationship,路径path,集合list,map
     *
     * @param cypherSql
     * @return
     */
    public static HashMap<String, Object> getGraphNodeAndShip(String cypherSql) {
        HashMap<String, Object> mo = new HashMap<String, Object>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                List<HashMap<String, Object>> ents = new ArrayList<HashMap<String, Object>>();
                List<HashMap<String, Object>> ships = new ArrayList<HashMap<String, Object>>();
                List<String> processedInternalNodeIds = new ArrayList<String>();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> f = recordItem.fields();
                    for (Pair<String, Value> pair : f) {
                        HashMap<String, Object> rShips = new HashMap<String, Object>();
                        String typeName = pair.value().type().name();
                        if ("NULL".equals(typeName)) {
                            continue;
                        }
                        if ("NODE".equals(typeName)) {
                            Node noe4jNode = pair.value().asNode();
                            String internalId = String.valueOf(noe4jNode.id());
                            if (!processedInternalNodeIds.contains(internalId)) {
                                Map<String, Object> nodeProperties = noe4jNode.asMap();
                                HashMap<String, Object> rss = new HashMap<>(nodeProperties);
                                rss.put("id_internal", internalId);
                                if (!rss.containsKey("uuid")) {
                                    rss.put("uuid", internalId);
                                    log.warn("Node (internal id: " + internalId
                                            + ") in getGraphNodeAndShip is missing 'uuid' property. Falling back to internal id for 'uuid' field.");
                                }
                                ents.add(rss);
                                processedInternalNodeIds.add(internalId);
                            }
                        } else if ("RELATIONSHIP".equals(typeName)) {
                            Relationship rship = pair.value().asRelationship();
                            String uuid = String.valueOf(rship.id());
                            String sourceInternalId = String.valueOf(rship.startNodeId());
                            String targetInternalId = String.valueOf(rship.endNodeId());
                            Map<String, Object> map = rship.asMap();
                            for (Entry<String, Object> entry : map.entrySet()) {
                                String key = entry.getKey();
                                rShips.put(key, entry.getValue());
                            }
                            rShips.put("uuid", uuid);
                            rShips.put("sourceId", sourceInternalId);
                            rShips.put("targetId", targetInternalId);
                            ships.add(rShips);
                        } else if ("PATH".equals(typeName)) {
                            Path path = pair.value().asPath();
                            for (Node nodeItem : path.nodes()) {
                                String internalId = String.valueOf(nodeItem.id());
                                if (!processedInternalNodeIds.contains(internalId)) {
                                    Map<String, Object> nodeProperties = nodeItem.asMap();
                                    HashMap<String, Object> rssNode = new HashMap<>(nodeProperties);
                                    rssNode.put("id_internal", internalId);
                                    if (!rssNode.containsKey("uuid")) {
                                        rssNode.put("uuid", internalId);
                                        log.warn("Node (internal id: " + internalId
                                                + ") in PATH processing (getGraphNodeAndShip) is missing 'uuid' property. Falling back to internal id for 'uuid' field.");
                                    }
                                    ents.add(rssNode);
                                    processedInternalNodeIds.add(internalId);
                                }
                            }
                            for (Relationship nextRel : path.relationships()) {
                                rShips = new HashMap<String, Object>();
                                String relUuid = String.valueOf(nextRel.id());
                                String sourceInternalId = String.valueOf(nextRel.startNodeId());
                                String targetInternalId = String.valueOf(nextRel.endNodeId());
                                Map<String, Object> map = nextRel.asMap();
                                for (Entry<String, Object> entry : map.entrySet()) {
                                    String key = entry.getKey();
                                    rShips.put(key, entry.getValue());
                                }
                                rShips.put("uuid", relUuid);
                                rShips.put("sourceId", sourceInternalId);
                                rShips.put("targetId", targetInternalId);
                                ships.add(rShips);
                            }
                        } else if (typeName.contains("LIST")) {
                            Iterable<Value> val = pair.value().values();
                            for (Value listItem : val) {
                                if ("RELATIONSHIP".equals(listItem.type().name())) {
                                    Relationship rship = listItem.asRelationship();
                                    HashMap<String, Object> rShipItem = new HashMap<>();
                                    String relUuid = String.valueOf(rship.id());
                                    String sourceInternalId = String.valueOf(rship.startNodeId());
                                    String targetInternalId = String.valueOf(rship.endNodeId());
                                    Map<String, Object> map = rship.asMap();
                                    for (Entry<String, Object> entry : map.entrySet()) {
                                        rShipItem.put(entry.getKey(), entry.getValue());
                                    }
                                    rShipItem.put("uuid", relUuid);
                                    rShipItem.put("sourceId", sourceInternalId);
                                    rShipItem.put("targetId", targetInternalId);
                                    ships.add(rShipItem);
                                }
                            }
                        } else if (typeName.contains("MAP")) {
                            HashMap<String, Object> mapResult = new HashMap<>();
                            mapResult.put(pair.key(), pair.value().asMap());
                            ents.add(mapResult);
                        } else {
                            HashMap<String, Object> scalarResult = new HashMap<>();
                            scalarResult.put(pair.key(), pair.value().toString());
                            ents.add(scalarResult);
                        }
                    }
                }
                mo.put("node", ents);
                mo.put("relationship", toDistinctList(ships));
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return mo;
    }

    /**
     * 去掉json键的引号，否则neo4j会报错
     *
     * @param jsonStr
     * @return
     */
    public static String getFilterPropertiesJson(String jsonStr) {
        return jsonStr.replaceAll("\"(\\w+)\"(\\s*:\\s*)", "$1$2"); // 去掉key的引号
    }

    /**
     * 对象转json，key=value,用于 cypher set语句.
     * 此版本会跳过名为 "uuid" 的字段，以防止在SET语句中修改它。
     *
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String getKeyValCyphersql(T obj) {
        Map<String, Object> map = new HashMap<String, Object>();
        List<String> sqlList = new ArrayList<String>();
        // 得到类对象
        Class userCla = obj.getClass();
        /* 得到类中的所有属性集合 */
        Field[] fs = userCla.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            Class type = f.getType();
            String key = f.getName(); // 获取字段名

            // 跳过 "uuid" 字段，不将其包含在 SET 子句中
            if ("uuid".equals(key)) {
                continue;
            }

            f.setAccessible(true); // 设置些属性是可以访问的
            Object val = new Object();
            try {
                val = f.get(obj);
                if (val == null) {
                    // 对于SET子句，如果值为null，通常我们可能想移除该属性或不做任何事
                    // 或者根据需求设置为特定值，例如空字符串。这里我们跳过null值。
                    // 如果希望将null值对应的属性也加入SET n.key = null，则需要不同处理。
                    // 目前行为：如果val为null，则不生成该属性的SET语句部分。
                    // 如果您的NodeItem中字段默认为null且希望在数据库中也体现为null，则需要调整。
                    // 通常，如果前端没有传递某个值，我们可能不希望在SET中显式设置它为null，除非业务需要。
                    // 这里为了简单，如果值为null，我们不将其加入SET列表。
                    // 如果需要显式设置为null，则应判断 val == null 并构造 "n." + key + "=null"
                    if (val == null && !(type.isPrimitive())) { // 基本类型不能为null，所以只对对象类型检查
                        // 如果需要显式设置null，可以在这里添加: sqlList.add("n." + key + "=null");
                        continue; // 当前选择：不为null值生成SET语句
                    } else if (val == null && type.isPrimitive()) {
                        // 这是一个不太可能的情况，基本类型字段被 f.get(obj) 返回 null
                        // 但作为防御性编程，可以记录或跳过
                        log.warn("Primitive field " + key + " has null value, skipping in SET clause.");
                        continue;
                    }
                }
                String sql = "";
                // String key = f.getName(); // 已提前获取

                if (val instanceof String[]) {
                    // 如果为true则强转成String数组
                    String[] arr = (String[]) val;
                    List<String> quotedArr = new ArrayList<>();
                    for (int j = 0; j < arr.length; j++) {
                        quotedArr.add("'" + arr[j].replace("'", "\\'") + "'"); // 对字符串中的单引号进行转义
                    }
                    sql = "n." + key + "=[" + String.join(",", quotedArr) + "]";
                } else if (val instanceof List) {
                    // 如果为true则强转成List
                    List<?> listVal = (List<?>) val;
                    List<String> processedList = new ArrayList<>();
                    for (Object item : listVal) {
                        if (item instanceof String) {
                            processedList.add("'" + ((String) item).replace("'", "\\'") + "'");
                        } else {
                            processedList.add(String.valueOf(item)); // 数字等直接toString
                        }
                    }
                    sql = "n." + key + "=[" + String.join(",", processedList) + "]";
                } else {
                    // 得到此属性的值
                    map.put(key, val);// 设置键值 (map的使用在此方法中似乎不是主要目的)
                    if (type.getName().equals("int") || type.getName().equals("java.lang.Integer") ||
                            type.getName().equals("long") || type.getName().equals("java.lang.Long") ||
                            type.getName().equals("double") || type.getName().equals("java.lang.Double") ||
                            type.getName().equals("float") || type.getName().equals("java.lang.Float") ||
                            type.getName().equals("boolean") || type.getName().equals("java.lang.Boolean")) {
                        sql = "n." + key + "=" + val.toString() + ""; // 数字和布尔值不需要引号
                    } else if (val instanceof String) {
                        // 对字符串中的单引号进行转义
                        sql = "n." + key + "='" + ((String) val).replace("'", "\\'") + "'";
                    } else {
                        // 对于其他类型，作为字符串处理（可能需要更具体的转换）
                        log.warn("Unhandled type in getKeyValCyphersql for field " + key + ": " + type.getName()
                                + ". Treating as string.");
                        sql = "n." + key + "='" + val.toString().replace("'", "\\'") + "'";
                    }
                }
                if (!sql.isEmpty()) {
                    sqlList.add(sql);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Error accessing field " + key + ": " + e.getMessage(), e);
            }
        }
        return String.join(",", sqlList);
    }

    /**
     * 将haspmap集合反序列化成对象集合
     *
     * @param maps
     * @param type
     * @param <T>
     * @return
     */
    public static <T> List<T> hashMapToObject(List<HashMap<String, Object>> maps, Class<T> type) {
        try {
            List<T> list = new ArrayList<T>();
            for (HashMap<String, Object> r : maps) {
                T t = type.newInstance();
                Iterator iter = r.entrySet().iterator();// 该方法获取列名.获取一系列字段名称.例如name,age...
                while (iter.hasNext()) {
                    Entry entry = (Entry) iter.next();// 把hashmap转成Iterator再迭代到entry
                    String key = entry.getKey().toString(); // 从iterator遍历获取key
                    Object value = entry.getValue(); // 从hashmap遍历获取value
                    if ("serialVersionUID".toLowerCase().equals(key.toLowerCase())) {
                        continue;
                    }
                    Field field = type.getDeclaredField(key);// 获取field对象
                    if (field != null) {
                        // System.out.print(field.getType());
                        field.setAccessible(true);
                        // System.out.print(field.getType().getName());
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0);// 设置值
                            } else {
                                field.set(t, Integer.parseInt(value.toString()));// 设置值
                            }
                        } else if (field.getType() == long.class || field.getType() == Long.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0);// 设置值
                            } else {
                                field.set(t, Long.parseLong(value.toString()));// 设置值
                            }

                        } else if (field.getType() == Double.class) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, 0.0);// 设置值
                            } else {
                                field.set(t, Double.parseDouble(value.toString()));// 设置值
                            }

                        } else {
                            if (field.getType().equals(List.class)) {
                                if (value == null || StringUtil.isBlank(value.toString())) {
                                    field.set(t, null);
                                } else {
                                    field.set(t, value);// 设置值
                                }
                            } else {
                                field.set(t, value);// 设置值
                            }
                        }
                    }

                }
                list.add(t);
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将haspmap反序列化成对象
     *
     * @param map
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T hashMapToObjectItem(HashMap<String, Object> map, Class<T> type) {
        try {
            T t = type.newInstance();
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Entry entry = (Entry) iter.next();// 把hashmap转成Iterator再迭代到entry
                String key = entry.getKey().toString(); // 从iterator遍历获取key
                Object value = entry.getValue(); // 从hashmap遍历获取value
                if ("serialVersionUID".toLowerCase().equals(key.toLowerCase())) {
                    continue;
                }
                Field field = type.getDeclaredField(key);// 获取field对象
                if (field != null) {
                    field.setAccessible(true);
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0);// 设置值
                        } else {
                            field.set(t, Integer.parseInt(value.toString()));// 设置值
                        }
                    } else if (field.getType() == long.class || field.getType() == Long.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0);// 设置值
                        } else {
                            field.set(t, Long.parseLong(value.toString()));// 设置值
                        }

                    } else if (field.getType() == Double.class) {
                        if (value == null || StringUtil.isBlank(value.toString())) {
                            field.set(t, 0.0);// 设置值
                        } else {
                            field.set(t, Double.parseDouble(value.toString()));// 设置值
                        }

                    } else {
                        if (field.getType().equals(List.class)) {
                            if (value == null || StringUtil.isBlank(value.toString())) {
                                field.set(t, null);
                            } else {
                                field.set(t, value);// 设置值
                            }
                        } else {
                            field.set(t, value);// 设置值
                        }

                    }
                }

            }

            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回单个节点信息
     */
    public static HashMap<String, Object> getOneNode(String cypherSql) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        try (Session session = neo4jDriver.session()) {
            log.debug(cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                Record record = result.list().get(0);
                Pair<String, Value> f = record.fields().get(0);
                String typeName = f.value().type().name();
                if ("NODE".equals(typeName)) {
                    Node noe4jNode = f.value().asNode();
                    String uuid = String.valueOf(noe4jNode.id());
                    Map<String, Object> map = noe4jNode.asMap();
                    for (Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        ret.put(key, entry.getValue());
                    }
                    ret.put("uuid", uuid);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return ret;
    }

    public static boolean batchRunCypherWithTx(List<String> cyphers) {
        Session session = neo4jDriver.session();
        try (Transaction tx = session.beginTransaction()) {
            for (String cypher : cyphers) {
                tx.run(cypher);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        }
        return true;
    }

    public static List<HashMap<String, Object>> toDistinctList(List<HashMap<String, Object>> list) {
        Set<String> keysSet = new HashSet<String>();
        Iterator<HashMap<String, Object>> it = list.iterator();
        while (it.hasNext()) {
            HashMap<String, Object> map = it.next();
            String uuid = (String) map.get("uuid");
            int beforeSize = keysSet.size();
            keysSet.add(uuid);
            int afterSize = keysSet.size();
            if (afterSize != (beforeSize + 1)) {
                it.remove();
            }
        }
        return list;
    }

    @Override
    public void close() throws Exception {
        neo4jDriver.close();
    }
}
