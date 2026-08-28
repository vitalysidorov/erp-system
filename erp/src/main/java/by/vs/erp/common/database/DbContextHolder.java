package by.vs.erp.common.database;

public class DbContextHolder {
    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();
    public static void set(DataSourceType type) { CONTEXT.set(type); }
    public static DataSourceType get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}
