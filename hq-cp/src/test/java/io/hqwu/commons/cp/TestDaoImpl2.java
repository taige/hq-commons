package io.hqwu.commons.cp;

import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.annotation.Transactional;

/**
* Created with IntelliJ IDEA for plat-arch-svn
* User: taige
* Date: 15/2/6
* Time: 上午11:48
*/
public class TestDaoImpl2 extends JdbcDaoSupport implements TestDao2 {

    @Override
    @Transactional
    public void execute(String sql) {
        getJdbcTemplate().execute(sql);
    }

}
