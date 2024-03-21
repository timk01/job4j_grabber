package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class HabrCareerParse implements DateTimeParser {

    /**
     * "https://career.habr.com/vacancies?page=1&q=Java%20developer&type=all"
     * - полный путь (на самом деле мы собираем строчку, но номер страницы может меняться, остальное статично)
     */

    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";

    @Override
    public LocalDateTime parse(String parse) {
        return ZonedDateTime
                .parse(parse, DateTimeFormatter.ISO_DATE_TIME)
                .toLocalDateTime();

    }

    /**
     * Jsoup.connect(fullPage).get() - получение ВСЕЙ страницы
     * document.select(".vacancy-card__inner") - получение кусочка, элмента страницы
     * (здесь это окошко, внутри коего - вакансия)
     * этот элемент, а точнее, СПИСОК элементов - лист. По нему можно проходить.
     * <P></P>
     * И у каждого элемента этого листа есть поля (внутри тега - характеристики)
     * например, ".vacancy-card__title" - тайтл вакансии, целиком - содержит сам тайтл и ссылку (хРеф)
     * row.select(".vacancy-card__title").first(); - получение первого ряда (это типа первого элемента)
     * title.child(0) - у этого первого ряда - первую строчку...
     * ink.text(); - а из нее - текст
     * <P></P>
     * System.out.printf("%s %s%n", vacancyName, vacLink);
     * - в конце сбор из измени вакансии (предварительно подготовленного - это текст) + ссылки
     * Java Developer "https://career.habr.com/vacancies/1000091983"
     * ссылка содержит хабр + хРеф от вакансии
     * <P></P>
     * классы vacancy-card__title и vacancy-card__date являются дочерними элементами класса vacancy-card__inner.
     * Таким образом, они находятся внутри элемента с классом vacancy-card__inner.
     * Когда вы используете метод document.select(".vacancy-card__inner"),
     * вы получаете все элементы на странице, которые имеют класс vacancy-card__inner.
     * Затем, в цикле forEach, вы обрабатываете каждый из этих элементов отдельно.
     * Внутри каждого элемента с классом vacancy-card__inner вы выбираете элементы
     * с классами vacancy-card__title и vacancy-card__date, чтобы получить нужные данные о вакансиях.
     * <P></P>
     * <div class="vacancy-card__title">
     *     <a href="/vacancies/1000138805" class="vacancy-card__title-link">
     *         Разработчик Java / Developer Java (Junior)</a></div>
     * .first() возвращает первый элемент из коллекции элементов, найденных внутри (!!!) элемента row,
     * которая представляет собой список элементов с классом vacancy-card__title, находящихся внутри элемента row.
     * -- т.е. мы сначала нащли 1 элемент (вакансию) в целом, потом копаемся внутри и извлекаем самое первое поле,
     * что соответствует vacancy-card__title (из коллекции заголовков)
     *
     * !!!!!! - т.е. я верно понимаю, что и в .vacancy-card__title может быть список ?
     * (по-сути это первый элемент жэтого списка) - ДА
     * <P></P>
     * Первый дочерний элемент этого заголовка" означает первый элемент,
     * который находится непосредственно внутри элемента .vacancy-card__title (это а ссылкой href)
     * <div class="vacancy-card__title">
     *     <a href="https://example.com">Ссылка</a>
     *     <span>Текст</span>
     * </div>
     * Здесь <a> и <span> являются дочерними элементами элемента с классом .vacancy-card__title.
     * Первым дочерним элементом этого заголовка будет <a>.
     * <P></P>
     * text() - это метод в библиотеке Jsoup, который используется для извлечения текстового содержимого элемента.
     * В контексте данного кода link.text() используется для извлечения текстового содержимого элемента <a>,
     * который находится внутри .vacancy-card__title.
     * т.е. String vacancyName = link.text(); даст мне Разработчик Java / Developer Java (Junior)
     * <P></P>
     * link.attr("href") - соответственно получение по ключу value, наподобие property.get("key")
     *
     * @param args
     * @throws IOException
     */

    public static void main(String[] args) throws IOException {
        int pageNumber = 1;
        String fullPage = String.format("%s%s%d%s", SOURCE_LINK, PREFIX, pageNumber, SUFFIX);
        Document document = Jsoup.connect(fullPage).get();
        Elements vacancyCard = document.select(".vacancy-card__inner");

        HabrCareerParse habrCareerParse = new HabrCareerParse();

        vacancyCard.forEach(row -> {
                    Element title = row.select(".vacancy-card__title").first();
                    Element link = title.child(0);
                    String vacancyName = link.text();
                    String vacLink = String.format("%s%s", SOURCE_LINK, link.attr("href"));
                    Element transition = row.select(".vacancy-card__date").first();
                    Element child = transition.child(0);
                    String formattedDate = String.format("%s", child.attr("datetime"));
                    System.out.printf("%s; %s; %s%n", vacancyName, vacLink, habrCareerParse.parse(formattedDate));
                }
        );
    }
}
