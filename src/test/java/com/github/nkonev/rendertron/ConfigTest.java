package com.github.nkonev.rendertron;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ConfigTest {
    @Test(expected = Exception.class)
    public void should_throw_exception_if_invalid_timeout_value_specified() throws Exception {
        //given
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("socketTimeout", "not_an_int");
        Config config = new Config(configuration);
        //when
        config.getHttpClient();
    }

    @Test
    public void should_pass_if_correct_timeout_value_specified() throws Exception {
        //given
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("socketTimeout", "1000");
        Config config = new Config(configuration);
        //when
        final CloseableHttpClient httpClient = config.getHttpClient();

        assertThat(httpClient, is(notNullValue()));
    }
}
