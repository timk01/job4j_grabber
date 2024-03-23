package ru.job4j.grabber;

import ru.job4j.grabber.models.Post;
import ru.job4j.grabber.utils.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection connection;

    public PsqlStore(Properties properties) throws SQLException {
        connection = DriverManager.getConnection(
                properties.getProperty("url"),
                properties.getProperty("username"),
                properties.getProperty("password"));
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "INSERT INTO post(name, text, link, created) "
                                     + "VALUES (?, ?, ?, ?) "
                                     + "ON CONFLICT(link) "
                                     + "DO NOTHING",
                             Statement.RETURN_GENERATED_KEYS
                     )
        ) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                post.setId(generatedKeys.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("text"),
                resultSet.getString("link"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT * FROM post")
        ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(getPost(resultSet));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT * FROM post where id = ?")
        ) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    post = getPost(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    private static Properties getProperties() {
        Properties properties = new Properties();
        try (InputStream input = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("db/rabbit.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    private void clearTable() {
        try (PreparedStatement setSeqToOne =
                     connection.prepareStatement("ALTER SEQUENCE post_id_seq RESTART WITH 1");
             PreparedStatement deteleTable =
                     connection.prepareStatement("DELETE FROM post")) {
            setSeqToOne.executeUpdate();
            deteleTable.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try (PsqlStore psqlStore = new PsqlStore(getProperties())) {
            psqlStore.clearTable();
            System.out.println("table is empty before:");
            psqlStore.getAll().forEach(System.out::println);
            System.out.println();
            psqlStore.save(new Post(
                    "job1",
                    "https://job1",
                    "job1 description",
                    LocalDateTime.now()));
            psqlStore.save(new Post(
                    "job2",
                    "https://unique_link",
                    "not unique link descr",
                    LocalDateTime.now()));
            psqlStore.save(new Post(
                    "job3",
                    "https://job33",
                    "job1 description",
                    LocalDateTime.now()));
            psqlStore.save(new Post(
                    "job4",
                    "https://unique_link",
                    "not unique link descr",
                    LocalDateTime.now()));
            System.out.println("found all existed after:");
            psqlStore.getAll().forEach(System.out::println);
            System.out.println("found 1:");
            System.out.println(psqlStore.findById(1));
            System.out.println("found 2 (double link):");
            System.out.println(psqlStore.findById(2));
            System.out.println("found 3:");
            System.out.println(psqlStore.findById(3));
            System.out.println();

            HabrCareerParse habrCareerParse = new HabrCareerParse(new Parser());
            List<Post> postList = habrCareerParse
                    .list("https://career.habr.com/vacancies?page=1&q=Java%20developer&type=all");
            postList.forEach(psqlStore::save);
            System.out.println("found all existed after addition from Habr:");
            psqlStore.getAll().forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}