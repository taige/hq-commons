package com.umpay.commons.util;

import org.junit.jupiter.api.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * Date: 18-4-24
 * Time: 下午12:01
 * 
 */
public class RSACoderUtilTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlBV6hIzDTTfb0/fx46iXxURX72HQyc1l0C46EyTQSKbd5DIF4L8jwg5cAeifmeUMGuSYeWocE9hvGGghmRfxGUe1JoHwkFzxLmm1R5wFMDqv9cLlzrxFTriz69bx2zzCd4AxyYUwhGPTyNyNQfIysbgToOaxrDKVB9i0HVQTb8ku0xmi2ZGR7DXv4HFAE67H3RF+W7Jugixl5B0TAWSJ/i4VqTCJjFiUKTjlxZ4wBJ+bKebfyEwlvpjKlhr5b7mANMayrnOT5UWN5L48foxdt6Bv0bMlbx2El8wnQCOrFN8iMZC71Vik5uSPhqAOg53yRSELEzcLL4Gs1LT6Db3KPwIDAQAB";

    private String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCUFXqEjMNNN9vT9/HjqJfFRFfvYdDJzWXQLjoTJNBIpt3kMgXgvyPCDlwB6J+Z5Qwa5Jh5ahwT2G8YaCGZF/EZR7UmgfCQXPEuabVHnAUwOq/1wuXOvEVOuLPr1vHbPMJ3gDHJhTCEY9PI3I1B8jKxuBOg5rGsMpUH2LQdVBNvyS7TGaLZkZHsNe/gcUATrsfdEX5bsm6CLGXkHRMBZIn+LhWpMImMWJQpOOXFnjAEn5sp5t/ITCW+mMqWGvlvuYA0xrKuc5PlRY3kvjx+jF23oG/RsyVvHYSXzCdAI6sU3yIxkLvVWKTm5I+GoA6DnfJFIQsTNwsvgazUtPoNvco/AgMBAAECggEASl5TDlhnCNOhud1OhIe52N8OznCwW3ikxh1BGzYkyCfNTfn1S76SZbWybV73MGGAZ80f6fHpMepDON2q6ejFM234wuME/ms+0JFT8iefz23ZyrHbmayhnfxSl4F2KyVjgIJH/q2+BO7hgIGLC3BoFyqb6PWSiylIAQ3KvuPncv6xy8qiN+y7ZCmwCxLoHcuypbnQL9EuWE9wBsuT6HUsg4gyZPe7RWQEw2kjLToQqFc0jMOKTp+fuqJ/8MS8i8E6uuOGy4D4QFhDhgSDcj1596jc/hFf979jkopOsbSNYsEHjM2hp55mKqI2D2I3qQIbSVKHyGhhxVZA1vJR7wkVYQKBgQDEaXCNwTc1EbShTIUKQj7dvH67nzUgEd3tmksB6IJVMYPGeIDwtNM2xgUXkpzESvnb+x6J9dgIsqzbbDVXpz/DE5dlOfGaFiJFOxDGwJaRpG0vYoZ+HS2dL7C8iZipk4qK63cp5DDcDiUc1G2Fslrulhe56MEbpjr2G0kXQPq8DwKBgQDBApb7lCQ7inPVbaOtmlazzUQ15MbutcQ4OezdVXNuWBD5CfM5wP5OE7AUPu1umZRZHHjlqyy/L7rpi7l1MgClVecR6eV3mASxlEOkr0aWJAaHynp9E4veFs3CN2SM4tNRG+r9ynt8dTdxYICGYZIBnIam+WE0qYggRXTuynee0QKBgFk1Ip/fr/l0jzvri0l7iR4g7Na+mtx0AP5X49V8CyYylQ1h0f6BwqTQD6QDPRySiSV2ywoSFQruTEBkx12GYxlxHoQlayx2/R3AlAARnNGNguHQuBEzo3IPJRCc1i+/CV/LTpZCREbOTeDoQs5EkbRni2Mu6fZq68C359yd8MPVAoGBALS7cBX0ClmWtBDOsc4GD9oeBBlTONSecDcWRq6bLZPN81a/8nnKNld2KBNNOZevSDKJbsvhFe2RDD1VETykreYkIlOr6aurNfnzE8RKPJaq9VKCT2B/xSEZyWJr7EPgwm/Y7Jrp1+ga+ue3raIoC0hnAc+pBVUCyu0tHOndvVWBAoGAXa6m7ynGxIXhskRMu+r16LcLMXKDkcYNahIiOcWEOYZsLZAHfwjnjzLkDeaSHqmYdery/RxsuBXLiZHxmfkeNmcgq8krtUCgUjLKbUFRkWTSdTqCKJh3CuWX9Z0pbbMaC/7FmKvRzHVc8YaMCoz/lS+VpUnEUnpc5rHl/0F/h6Y=";

    private String charset = "UTF-8";

    @Test
    public void testRsaSign() throws Exception {
        String plainText = "{\"encrypted\":false,\"biz_response\":\"{\"success\":false,\"error_code\":\"ZMOP.unknow_error\",\"error_message\":\"未知错误\"}\"}";
        String sign = RSACoderUtil.sign(plainText.getBytes(charset), privateKey);

        boolean b = RSACoderUtil.verify(plainText.getBytes(charset), publicKey, sign);

        LOGGER.info(b);
    }

    @Test
    public void testRsaEncry() throws Exception {
        String plainText = "7020114194";
        String encrypt = RSACoderUtil.encrypt(plainText, charset, publicKey);
        LOGGER.info(encrypt);
        String b = RSACoderUtil.decrypt(encrypt, privateKey, charset);

        LOGGER.info(b);
    }

    @Test
    public void testRsaDecrypt() throws Exception {
        String plainText = "gK/E1WwS5bSELkbsZlei5itL8Fe4Yd3IHRY85js+MzWmGgZFMUpvbzNwQllQ6/3lcgpgYi74VAaYw8GOqvRR4V8dvag7UGVid3GydlT61lL9w6/il/8S8lrwuMqa3nVjnI9nBi0fVbM4Pi2aVIEUDl5X50GroL6okinQ9lxw81Cwv2F0bzO0WzeeY94u5/VXV9SQCHhWrk8V3+zrvzeQVV1z3zRKWo5wNGMuk658PvC3BPdouVWsT906VjVL0NY16Hz4sFrdgc9CEpc16EaZpRqakq/TDVz8lVqJVOadvWujSec6IeUwL0Z4pq+kqeCEI50QCIEC+hxNegDSy/6yPw==";
        String encrypt = RSACoderUtil.encrypt(plainText, charset, publicKey);

        String b = RSACoderUtil.decrypt(encrypt, privateKey, charset);

        LOGGER.info(b);
    }

}
