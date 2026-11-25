package fr.simon.marquis.secretcodes;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrawlerTest {

    @Test
    public void secretCodeSchemes() {
        assertTrue(Crawler.SECRET_CODE_SCHEME.matcher("android_secret_code").matches());
        assertTrue(Crawler.SECRET_CODE_SCHEME.matcher("android_vivo_sec_code").matches());
        assertTrue(Crawler.SECRET_CODE_SCHEME.matcher("iqoo_secret_code").matches());
        assertFalse(Crawler.SECRET_CODE_SCHEME.matcher("http").matches());
        assertFalse(Crawler.SECRET_CODE_SCHEME.matcher("mqqverifycode").matches());
        assertFalse(Crawler.SECRET_CODE_SCHEME.matcher("secret_code").matches());
    }
}
