package io.hqwu.commons.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA for hq-commons
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-01-15
 * Time: 3:29 p.m.
 */
class Base62UtilTest {
    private static final Logger LOGGER = new Logger();

    @Test
    void test_encode_0() {
        LOGGER.debug(Base62Util.decode("100000"));  // 916132832
        LOGGER.debug(Base62Util.encode(Long.MAX_VALUE));  // 916132832
    }

    @Test
    void test_encode_1() {
        HashMap<Long, List<String>> map = new HashMap<>();

        for (int i = 0; ; i++) {
            String in = Base62Util.encode(i+1);
//            in = String.format("%05d", i+1);

//            int crc = 0xff & CRC8.calcCrc8(in.getBytes());
            long crc = Base62Util.crc32(in.getBytes());
            String b62_crc = Base62Util.encode(crc, 6);
            String enc = in + b62_crc;
            LOGGER.debug("原文：%s, CRC8：%s %s", in, b62_crc, enc);

            if (map.containsKey(crc)) {
//                LOGGER.debug(" === %d -> %s, %s", crc, in, map.get(crc));
                List<String> list = map.get(crc);
                list.add(in);
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(in);
                map.put(crc, list);
            }
            if (map.size() >= 256) {
                LOGGER.debug("i = " + i);
                break;
            }
        }
        map.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getKey)).forEach(entry -> {
            long crc = entry.getKey();
            List<String> list = entry.getValue();
            String hex=Long.toHexString(crc);
            String b62_crc = Base62Util.encode(crc, 8);
            LOGGER.debug("%d/%s/%s -> %s", crc, hex, b62_crc, list);
        });
    }

    @Test
    void test_encode_crc() {
        HashMap<Long, List<String>> map = new HashMap<>();

        for (int i = 0; i < 100000; i++) {
            long expect = i + 1;
            String in = Base62Util.encode(expect, true);
            LOGGER.debug(i + " = " + in);
            long num = Base62Util.decode(in, true);
            assertEquals(expect, num);
        }

    }

    @Test
    void test_decode_1() {
        String in = Base62Util.encode(100, true);
        LOGGER.debug("100 = " + in);
    }

    @Test
    void test_decode_2() {
        Base62Util.decode("1c1WWBm3", true);
    }
}