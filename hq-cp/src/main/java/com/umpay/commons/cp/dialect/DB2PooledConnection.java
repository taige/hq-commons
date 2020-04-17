package com.umpay.commons.cp.dialect;

import com.umpay.commons.cp.PooledConnection;
import com.umpay.commons.cp.UmpayCP;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA
 * User: taige
 * Date: 14-3-25
 * Time: 下午10:18
 */
public class DB2PooledConnection extends PooledConnection {

    public DB2PooledConnection(UmpayCP pool, int connId) throws SQLException {
        super(pool, connId);
    }

    @Override
    public boolean isFetalException(SQLException sqle) {
        if (super.isFetalException(sqle)) {
            return true;
        }

        int errorCode = sqle.getErrorCode();
        switch (errorCode) {
            case -512: // STATEMENT REFERENCE TO REMOTE OBJECT IS INVALID
            case -514: // THE CURSOR IS NOT IN A PREPARED STATE
            case -516: // THE DESCRIBE STATEMENT DOES NOT SPECIFY A PREPARED STATEMENT
            case -518: // THE EXECUTE STATEMENT DOES NOT IDENTIFY A VALID PREPARED STATEMENT
            case -525: // THE SQL STATEMENT CANNOT BE EXECUTED BECAUSE IT WAS IN ERROR AT BIND TIME FOR SECTION = sectno
                // PACKAGE = pkgname CONSISTENCY TOKEN = contoken
            case -909: // THE OBJECT HAS BEEN DELETED OR ALTERED
            case -918: // THE SQL STATEMENT CANNOT BE EXECUTED BECAUSE A CONNECTION HAS BEEN LOST
            case -924: // DB2 CONNECTION INTERNAL ERROR, function-code,return-code,reason-code
                return true;
            default:
                break;
        }
        return false;
    }
}
