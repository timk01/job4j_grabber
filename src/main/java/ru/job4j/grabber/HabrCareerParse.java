package ru.job4j.grabber;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.models.Post;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    /**
     * "https://career.habr.com/vacancies?page=1&q=Java%20developer&type=all"
     * - полный путь (на самом деле мы собираем строчку, но номер страницы может меняться, остальное статично)
     * <p> </p>
     * "https://career.habr.com/vacancies?page=2&q=Java%20%&type=all"
     */

    private final DateTimeParser dateTimeParser;
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final int START_PAGE = 1;
    private static final int END_PAGE = 5;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    /**
     * "https://career.habr.com/vacancies/1000138805"
     * т.е. адрес сайта "https://career.habr.com/ (SOURCE_LINK)" +
     * некий префикс для деталей +
     * номер;
     * Или, SOURCE_LINK + /vacancies/1000139737 - что нам УЖЕ возвращает строка link.attr("href")
     * <p></p>
     * т.е. использование метода это retrieveDescription(link.attr("href"));
     * <p></p>
     * внутри - та же логика, т.е. извлекаем из документа .first().text()
     * ((первый элемент, т.к. порядок сдвигается если его нет + текст))
     * <p></p>
     * позднее разбить на это (сейчас сплошная стена текста)
     * <pre>
     * String[] lines = vacancyDescription.split("\\.\\s+");
     * for (String line : lines) {
     *     System.out.println(line);
     * }
     * </pre>
     *
     * @param link of type String
     * @return vacancy description of type String
     */

    private String retrieveDescription(String link) throws IOException {
        String fullPage = String.format("%s%s", SOURCE_LINK, link);
        Document document = Jsoup.connect(fullPage).get();
        return document.select(".vacancy-description__text").first().text();
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
     * <p>
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
     * @throws IOException
     */

    private Post postParsing(Element row) throws IOException {

        /*
                Post post = new Post();
        post.setTitle(e.select(".vacancy-card__title").first().text());
        post.setDescription(retrieveDescription(String.format("%s%s", SOURCE_LINK, e.select(".vacancy-card__title").first().child(0).attr("href"))));
        post.setLink(String.format("%s%s", SOURCE_LINK, e.select(".vacancy-card__title").first().child(0).attr("href")));
        post.setCreated(dateTimeParser.parse(e.select(".vacancy-card__date").first().child(0).attr("datetime")));
        return post;
         */

        Post post = new Post();
        Element firstVacancyTitle = row.select(".vacancy-card__title")
                .first()
                .child(0);
        post.setTitle(firstVacancyTitle.text());
        post.setLink(String.format("%s%s", SOURCE_LINK, firstVacancyTitle.attr("href")));
        String titleReference = firstVacancyTitle.attr("href");
        post.setDescription(retrieveDescription(titleReference));
        Element firstVacancyDate = row.select(".vacancy-card__date")
                .first()
                .child(0);
        post.setCreated(dateTimeParser.parse(
                firstVacancyDate.attr("datetime"))
        );
        return post;
    }

    /**
     * Здесь мы сначала мастерим полную ссылку (номер будет страницы будет меняться)
     * Потом получаем по коннекту и get() документ. Документ - целиком.
     * Из документа по всей вакансии (общее поле) получаем ее элементы (на странице - много элементов)
     * По каждому из элементов проходимся и заполняем модель с полями, что раньше выводили на экран.
     * И добавляем каждую модель в лист.
     * @throws IOException
     */
    @Override
    public List<Post> list(String link) {
        List<Post> postList = new ArrayList<>();
        for (int pageNumb = START_PAGE; pageNumb <= END_PAGE; pageNumb++) {
            String fullPage = String.format("%s?page=%s", link, pageNumb);
            Document document;
            try {
                document = Jsoup.connect(fullPage).get();
                Elements vacancyCard = document.select(".vacancy-card__inner");
                for (Element element : vacancyCard) {
                    postList.add(postParsing(element));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return postList;
    }
}