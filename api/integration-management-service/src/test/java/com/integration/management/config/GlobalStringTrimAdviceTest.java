package com.integration.management.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalStringTrimAdvice")
class GlobalStringTrimAdviceTest {

    static class Payload {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    @DisplayName("initBinder should trim strings and convert blanks to null")
    void initBinder_trimsToNull() {
        GlobalStringTrimAdvice advice = new GlobalStringTrimAdvice();
        Payload target = new Payload();
        WebDataBinder binder = new WebDataBinder(target);
        advice.initBinder(binder);

        MutablePropertyValues pvs = new MutablePropertyValues();
        pvs.add("value", "   ");
        binder.bind(pvs);

        assertThat(target.getValue()).isNull();
    }
}
