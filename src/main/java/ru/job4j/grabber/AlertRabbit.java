package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {

    public static Properties getProperties() {
        Properties properties = new Properties();
        try (InputStream input = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("db/rabbit.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    /**
     * Детали отработки.
     * Метод Thread.sleep(10000) приостанавливает выполнение главного потока на 10 секунд.
     * После этого вызывается метод scheduler.shutdown(), который останавливает работу планировщика.
     * Таким образом, общее время выполнения программы составляет время ожидания (10 секунд) плюс время,
     * необходимое для выполнения всех задач, которое зависит от интервала и настроек триггера.
     * Из доки (в принципе если код внутри, между стартом и винишем - он выполнится, НО):
     * you will also need to allow some time for the job to be triggered and executed before calling shutdown()
     * - for a simple example such as this, you might just want to add a Thread.sleep(60000) call
     * <p></p>
     * Метод scheduler.scheduleJob(job, trigger) в Quartz используется для связывания задания (Job)
     * с триггером (Trigger) и определения времени его выполнения.
     * <p></p>
     * Триггер определяет условия запуска задания, включая точное время начала выполнения,
     * интервалы повторения, расписание и другие параметры.
     * Таким образом, триггер можно рассматривать как "когда" задание будет выполнено.
     * <p></p>
     * Triggers are the 'mechanism' by which Jobs are scheduled.
     * Many Triggers can point to the same Job, but a single Trigger can only point to one Job.
     * <p></p>
     * Планировщик (Scheduler) Quartz ответственен за управление выполнением заданий и
     * триггеров в соответствии с их расписанием. Когда условия триггера выполнены
     * (например, достигнуто указанное время начала выполнения или прошло определенное время),
     * планировщик запускает связанное с триггером задание для выполнения.
     * <p></p>
     * Вы создаете объект JobDetail с помощью метода newJob(Rabbit.class).
     * Этот метод указывает на класс, который должен быть выполнен в качестве задачи (job).
     * В данном случае это класс Rabbit, который реализует интерфейс Job.
     * <p></p>
     * Вы добавляете данные в JobDataMap с помощью метода usingJobData(data).
     * В эту карту вы можете поместить любые данные,
     * которые необходимо передать задаче (job) для ее выполнения.
     * <p></p>
     * После того как вы настроили JobDetail и JobDataMap, вы создаете объект Trigger,
     * который определяет расписание выполнения задачи (job).
     * В данном случае, используя startNow(), вы указываете, что задача должна начать выполняться
     * сразу после запуска шедулера, а withSchedule(times) указывает, как часто и с каким интервалом
     * эта задача будет повторяться.
     * В вашем случае, она будет выполняться с интервалом, указанным в interval,
     * и будет повторяться бесконечно.
     * <p></p>
     * Наконец, вы вызываете метод scheduleJob(job, trigger),
     * чтобы добавить вашу задачу (job) в планировщик (scheduler).
     * После этого планировщик будет запускать эту задачу согласно указанному расписанию.
     * <p></p>
     * The interface to be implemented by classes which represent a 'job' to be performed.
     * А это про Джоб (Job).
     * @param args
     */

    public static void main(String[] args) {
        Properties properties = getProperties();
        String interval = properties.getProperty("rabbit.interval");
        try (Connection cn = DriverManager.getConnection(
                properties.getProperty("url"),
                properties.getProperty("username"),
                properties.getProperty("password"))) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", cn);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(interval))
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
        } catch (SchedulerException | InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");

            try (PreparedStatement statement =
                         connection.prepareStatement("INSERT INTO rabbit(created_date) VALUES (?)")) {
                statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}