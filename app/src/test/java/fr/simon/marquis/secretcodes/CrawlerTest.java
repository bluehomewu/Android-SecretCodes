package fr.simon.marquis.secretcodes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

    /** A dex file reduced to what Crawler.calculatorCodes reads: the string_ids table and the string data. */
    private static byte[] dexWithStrings(String... strings) throws Exception {
        java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        int idsOff = 0x70;
        int dataOff = idsOff + 4 * strings.length;
        java.nio.ByteBuffer ids = java.nio.ByteBuffer.allocate(4 * strings.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (String s : strings) {
            ids.putInt(dataOff + data.size());
            data.write(s.length()); // uleb128 utf16 length, all test strings are < 128 chars
            data.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            data.write(0);
        }
        java.nio.ByteBuffer dex = java.nio.ByteBuffer.allocate(dataOff + data.size()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        dex.putInt(0x38, strings.length).putInt(0x3C, idsOff);
        dex.position(idsOff);
        dex.put(ids.array()).put(data.toByteArray());
        return dex.array();
    }

    @Test
    public void calculatorCodesFromDexStrings() throws Exception {
        byte[] dex = dexWithStrings(".12345+", ".98765+", "πeπe741,603eπeπ", "πEπE741,603EπEπ",
                "π", "πeπe741", ".5", "1+", "degree_mode", "π|e|\\d*\\.*\\d*e?\\d*");
        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList(".12345+=", ".98765+=", "πeπe741603eπeπ=")),
                Crawler.calculatorCodes(dex));
    }

    @Test
    public void runtimeCodesRespectManufacturer() {
        String[] any = {"*", "com.android.server.telecom", "823241"};
        String[] nothing = {"Nothing", "com.android.phone", "6833284"};
        assertTrue(Crawler.runtimeEntryApplies(any, "vivo", "com.android.server.telecom"));
        assertTrue(Crawler.runtimeEntryApplies(nothing, "Nothing", "com.android.phone"));
        assertTrue(Crawler.runtimeEntryApplies(nothing, "NOTHING", "com.android.phone"));
        assertFalse(Crawler.runtimeEntryApplies(nothing, "vivo", "com.android.phone"));
        assertFalse(Crawler.runtimeEntryApplies(nothing, null, "com.android.phone"));
        assertFalse(Crawler.runtimeEntryApplies(any, "vivo", "com.android.phone"));
    }
}
