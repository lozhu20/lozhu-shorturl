package day.happy365.shorturlservice.config;

public class DynamicRoutingDataSourceContext {

    public static final String MASTER = "master";

    public static final String SLAVE1 = "slave1";

    public static final String SLAVE2 = "slave2";

    private static final ThreadLocal<String> THREAD_LOCAL_DATA_SOURCE = new ThreadLocal<>();

    public static void setRoutingDataSource(String dataSource) {
        if (dataSource == null) {
            throw new NullPointerException();
        }
        THREAD_LOCAL_DATA_SOURCE.set(dataSource);
    }

    public static String getRoutingDataSource() {
        String dataSourceType = THREAD_LOCAL_DATA_SOURCE.get();
        if (dataSourceType == null) {
            THREAD_LOCAL_DATA_SOURCE.set(DynamicRoutingDataSourceContext.MASTER);
            return getRoutingDataSource();
        }
        return dataSourceType;
    }

    public static void removeRoutingDataSource() {
        THREAD_LOCAL_DATA_SOURCE.remove();
    }
}
