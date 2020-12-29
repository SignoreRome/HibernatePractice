import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class MainApp {
    public static void main(String[] args) {
        initData();
        CountDownLatch countDownLatch = new CountDownLatch(8);
        long start = System.currentTimeMillis();
        try (SessionFactory sessionFactory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory()) {
            Random random = new Random();
            Thread[] threads = new Thread[8];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    System.out.println(Thread.currentThread().getName() + " is started");
                    for (int j = 0; j < 20000; j++) {
                        optimisticBlock(sessionFactory, random); //115 секунд
//                        pessimisticBlock(sessionFactory, random); //112 секунд
                    }
                    System.out.println(Thread.currentThread().getName() + " is ended");
                    countDownLatch.countDown();
                });
                threads[i].start();
            }
            try {
                countDownLatch.await();
                System.out.println("#End");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try (Session session = sessionFactory.getCurrentSession()) {
                session.beginTransaction();
                BigInteger sum = (BigInteger) session.createNativeQuery("SELECT SUM (val) FROM items ").getSingleResult();
                session.getTransaction().commit();
                System.out.println(sum);
            }
        }
        System.out.println(System.currentTimeMillis()-start);
    }

    private static void pessimisticBlock(SessionFactory sessionFactory, Random random) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            Item item = (Item) session.createQuery("from Item i where i.id = :id")
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setParameter("id", random.nextInt(40) + 1)
                    .getSingleResult();
            item.setVal(item.getVal() + 1);
            sleepThreadUncheck(5);
            session.getTransaction().commit();
        }
    }

    private static void optimisticBlock(SessionFactory sessionFactory, Random random) {
        boolean updatedRow = false;
        while (!updatedRow) {
            try (Session session = sessionFactory.getCurrentSession()) {
                session.beginTransaction();
                Item item = session.get(Item.class, random.nextInt(40) + 1);
                item.setVal(item.getVal() + 1);
                sleepThreadUncheck(5);
                try {
                    session.getTransaction().commit();
                    updatedRow = true;
                } catch (OptimisticLockException optEx) {
                    session.getTransaction().rollback();
                }
            }
        }
    }

    public static void initData() {
        try (SessionFactory sessionFactory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory()) {
            try (Session session = sessionFactory.getCurrentSession()) {
                String sql = Files.lines(Paths.get("drop-and-create.sql")).collect(Collectors.joining(" "));
                session.beginTransaction();
                session.createNativeQuery(sql).executeUpdate();
                session.getTransaction().commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sleepThreadUncheck(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
