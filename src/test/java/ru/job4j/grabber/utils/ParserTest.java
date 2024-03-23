package ru.job4j.grabber.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

    private static DateTimeParser parser;

    @BeforeAll
    public static void init() {
        parser = new Parser();
    }

    @Test
    public void parseFormattedDate1() {
        String date = "2024-02-21T18:21:56+03:00";
        assertThat(parser.parse(date))
                .isEqualTo(LocalDateTime.of(
                        2024,
                        Month.FEBRUARY,
                        21,
                        18,
                        21,
                        56)
                );
    }

    @Test
    public void parseFormattedDate2() {
        String date = "2020-02-20T17:21:56+03:00";
        assertThat(parser.parse(date))
                .isEqualTo(LocalDateTime.of(
                        2020,
                        Month.FEBRUARY,
                        20,
                        17,
                        21,
                        56)
                );
    }
}