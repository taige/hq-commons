package com.umpay.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: zhangyao
 * Date: 18-4-26
 * Time: 上午9:44
 * To change this template use File | Settings | File Templates.
 */
public class Base64UtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Test
    public void test() throws Exception {
        LOGGER.info(Base64Util.encryptBASE64("unpay:test".getBytes()));
    }

    @Test
    public void test_2() {
        String enc = "ew0KICAiSW5mb2tpbmQiOiAyLA0KICAiSW5mb0NsaWVudE5hbWUiOiAi5a6i5oi35ZCN56ewIiwNCiAgIkluZm9DbGllbnRUYXhDb2RlIjogIiIsDQogICJJbmZvQ2xpZW50QmFua0FjY291bnQiOiAiIiwNCiAgIkluZm9DbGllbnRBZGRyZXNzUGhvbmUiOiAiIiwNCiAgIkluZm9TZWxsZXJCYW5rQWNjb3VudCI6ICIiLA0KICAiSW5mb1NlbGxlckFkZHJlc3NQaG9uZSI6ICIiLA0KICAiSW5mb1RheFJhdGUiOiAwLA0KICAiSW5mb05vdGVzIjogIiIsDQogICJJbmZvSW52b2ljZXIiOiAi5byA56Wo5Lq6IiwNCiAgIkluZm9DaGVja2VyIjogIuWkjeaguOS6uiIsDQogICJJbmZvQ2FzaGllciI6ICLmlLbmrL7kuroiLA0KICAiSW5mb0xpc3ROYW1lIjogIiIsDQogICJSZXF1ZXN0X0ludm9pY2VfSXRlbXMiOiBbDQogICAgew0KICAgICAgIkxpc3RHb29kc05hbWUiOiAi5ZWG5ZOB5ZCN56ewIiwNCiAgICAgICJMaXN0VGF4SXRlbSI6ICIiLA0KICAgICAgIkxpc3RTdGFuZGFyZCI6ICLop4TmoLzlnovlj7ciLA0KICAgICAgIkxpc3RVbml0IjogIuWNleS9jSIsDQogICAgICAiTGlzdE51bWJlciI6IDEuMCwNCiAgICAgICJMaXN0UHJpY2VLaW5kIjogMCwNCiAgICAgICJMaXN0UHJpY2UiOiAxLjAsDQogICAgICAiTGlzdEFtb3VudCI6IDEuMCwNCiAgICAgICJMaXN0VGF4QW1vdW50IjogMC4xMywNCiAgICAgICJMaXN0VGF4UmF0ZSI6IDEzLA0KICAgICAgIkdvb2RzTm9WZXIiOiAiMzIuMCIsDQogICAgICAiR29vZHNUYXhObyI6ICIxMDMwMjAxOTkwMDAwMDAwMDAwIiwNCiAgICAgICJUYXhQcmUiOiAiMCIsDQogICAgICAiVGF4UHJlQ29uIjogIiIsDQogICAgICAiWmVyb1RheCI6ICIiLA0KICAgICAgIkNyb3BHb29kc05vIjogIiIsDQogICAgICAiVGF4RGVkdWN0aW9uIjogIiINCiAgICB9DQogIF0NCn0=";
        byte[] bs = Base64Util.base64ToByteArray(enc);
        String dec = new String(bs);
        LOGGER.info(dec);
        String enc2 = Base64Util.byteArrayToBase64(dec.getBytes());
        assertEquals(enc, enc2);
    }
}
