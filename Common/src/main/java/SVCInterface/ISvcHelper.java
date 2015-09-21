package SVCInterface;
import EntityInterface.IEntityRandomAccess;
import EntityInterface.IEntitySchema;
import java.sql.Connection;

public interface ISvcHelper {
    public IEntitySchema getSchema(String schemaId);
    public IEntityRandomAccess rawData2CSON(String rawData,String schemaId);
    public Connection getConn();
    public void releaseConn(Connection conn);
}
