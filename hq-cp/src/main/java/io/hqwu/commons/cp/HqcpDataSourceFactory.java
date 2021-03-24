package io.hqwu.commons.cp;

import com.ibatis.sqlmap.engine.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

public class HqcpDataSourceFactory extends HqcpDataSource implements DataSourceFactory {

    public DataSource getDataSource() {
        return this;
    }

    public void initialize(Map map) {

        Properties prop = new Properties();
        prop.putAll(map);

        this.setProperties(prop);
    }

}
