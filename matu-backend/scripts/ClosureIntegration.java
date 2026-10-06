import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.course.mapper.*;
import com.peakui.course.model.dto.*;
import com.peakui.course.service.CourseService;
import com.peakui.course.service.VideoMetadataService;
import com.peakui.course.service.impl.CourseServiceImpl;
import com.peakui.course.feign.*;
import com.peakui.info.mapper.*;
import com.peakui.info.model.dto.UpdateFeedbackRequest;
import com.peakui.info.service.FeedbackService;
import com.peakui.info.service.impl.FeedbackServiceImpl;
import com.peakui.info.feign.AuthUserFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import static org.mockito.Mockito.*;

/** Real MySQL + MyBatis + Spring transaction checks; only writes to one disposable database. */
public class ClosureIntegration {
    static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    static void denied(Runnable action, String message) {
        boolean failed = false;
        try { action.run(); } catch (RuntimeException expected) { failed = true; }
        check(failed, message);
    }
    static Object transactional(Object service, DataSourceTransactionManager manager) {
        ProxyFactory proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return proxy.getProxy();
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        String host = System.getenv().getOrDefault("MYSQL_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("MYSQL_PORT", "3306");
        String user = System.getenv().getOrDefault("MYSQL_USERNAME", "root");
        String password = System.getenv().getOrDefault("MYSQL_PASSWORD", "");
        String db = "matu_verify_" + UUID.randomUUID().toString().replace("-", "");
        String options = "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&connectTimeout=8000";
        String base = "jdbc:mysql://" + host + ":" + port + "/";
        try (Connection admin = DriverManager.getConnection(base + options, user, password); Statement ddl = admin.createStatement()) {
            ddl.execute("CREATE DATABASE `" + db + "` CHARACTER SET utf8mb4");
            try {
                DriverManagerDataSource ds = new DriverManagerDataSource(base + db + options, user, password);
                try (Connection connection = ds.getConnection()) {
                    ScriptUtils.executeSqlScript(connection, new FileSystemResource(root.resolve("database/bootstrap/matu_course.sql")));
                    ScriptUtils.executeSqlScript(connection, new FileSystemResource(root.resolve("database/bootstrap/matu_info.sql")));
                }
                MybatisConfiguration configuration = new MybatisConfiguration();
                configuration.setMapUnderscoreToCamelCase(true);
                for (Class<?> mapper : new Class<?>[]{CourseMapper.class, CourseChapterMapper.class, CourseVideoMapper.class,
                        CourseProgressMapper.class, CourseNoteMapper.class, CourseReviewMapper.class, CourseCertificateMapper.class,
                        CourseArticleMapper.class, UserFeedbackMapper.class, FeedbackHistoryMapper.class}) configuration.addMapper(mapper);
                MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
                factory.setDataSource(ds); factory.setConfiguration(configuration);
                MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
                pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
                factory.setPlugins(pagination);
                SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
                DataSourceTransactionManager transactions = new DataSourceTransactionManager(ds);
                HttpServletRequest http = mock(HttpServletRequest.class);
                CourseService courses = (CourseService) transactional(new CourseServiceImpl(
                        session.getMapper(CourseMapper.class), session.getMapper(CourseChapterMapper.class),
                        session.getMapper(CourseVideoMapper.class), session.getMapper(CourseProgressMapper.class),
                        session.getMapper(CourseNoteMapper.class), session.getMapper(CourseReviewMapper.class),
                        session.getMapper(CourseCertificateMapper.class), session.getMapper(CourseArticleMapper.class),
                        mock(FileFeignClient.class), mock(UserFeignClient.class), mock(VideoMetadataService.class), http,
                        mock(CourseEntitlementClient.class)), transactions);
                FeedbackService feedback = (FeedbackService) transactional(new FeedbackServiceImpl(
                        session.getMapper(UserFeedbackMapper.class), session.getMapper(FeedbackHistoryMapper.class),
                        mock(AuthUserFeignClient.class), new ObjectMapper(), http), transactions);
                JdbcTemplate jdbc = new JdbcTemplate(ds);
                jdbc.update("INSERT INTO courses(id,instructor_id,title,status,is_free) VALUES(1,11,'Integration course',1,1)");
                jdbc.update("INSERT INTO course_chapters(id,course_id,chapter_title) VALUES(2,1,'Chapter')");
                jdbc.update("INSERT INTO course_videos(id,chapter_id,video_title,video_url,duration,status) VALUES(3,2,'Video','https://example.invalid/test.mp4',100,1)");
                jdbc.update("INSERT INTO user_feedbacks(id,user_id,title,content,ip_address) VALUES(10,22,'Test','Test feedback','127.0.0.1')");
                try (var auth = mockStatic(StpUtil.class)) {
                    auth.when(StpUtil::isLogin).thenReturn(true);
                    auth.when(StpUtil::getLoginIdAsLong).thenReturn(22L);
                    when(http.getHeader("X-User-Roles")).thenReturn("TEACHER");
                    denied(() -> courses.updateCourse(1L, new UpdateCourseRequest()), "Cross-owner edit must fail");
                    System.out.println("PASS teacher ownership enforced against persisted course");

                    UpdateProgressRequest progress = new UpdateProgressRequest(); progress.setVideoId(3L);
                    progress.setProgressPercent(BigDecimal.valueOf(40)); progress.setWatchedDuration(40);
                    courses.updateProgress(1L, progress);
                    progress.setProgressPercent(BigDecimal.TEN); progress.setWatchedDuration(10); courses.updateProgress(1L, progress);
                    check(courses.listMyProgress(1L).size() == 1, "Progress must upsert");
                    check(courses.listMyProgress(1L).get(0).getProgressPercent().compareTo(BigDecimal.valueOf(40)) == 0, "Progress must not regress");
                    denied(() -> courses.issueCertificate(1L), "Incomplete learner must not receive certificate");
                    progress.setProgressPercent(BigDecimal.valueOf(100)); progress.setWatchedDuration(100); courses.updateProgress(1L, progress);
                    String certificate = courses.issueCertificate(1L).getCertificateNo();
                    check(certificate.equals(courses.issueCertificate(1L).getCertificateNo()), "Certificate must be idempotent");
                    check(jdbc.queryForObject("SELECT COUNT(*) FROM course_certificates", Integer.class) == 1, "Duplicate certificate row");
                    System.out.println("PASS persisted monotonic progress, completion gate and idempotent certificate");

                    CreateNoteRequest note = new CreateNoteRequest();note.setCourseId(1L);note.setVideoId(3L);note.setContent("My private note");note.setIsPublic(0);
                    courses.createNote(note);
                    check(courses.listNotes(1L,3L,false,1L,10L).getTotal() == 1, "Owner must see private note");
                    auth.when(StpUtil::getLoginIdAsLong).thenReturn(33L);
                    check(courses.listNotes(1L,3L,false,1L,10L).getTotal() == 0, "Other user must not see private note");
                    check(courses.listNotes(1L,3L,true,1L,10L).getTotal() == 0, "Public list must not leak private note");
                    auth.when(StpUtil::getLoginIdAsLong).thenReturn(22L);
                    System.out.println("PASS private notes isolated across actual database queries");

                    CreateReviewRequest review = new CreateReviewRequest(); review.setCourseId(1L); review.setRating(5); review.setContent("First");
                    courses.createReview(review);
                    jdbc.update("UPDATE course_reviews SET is_verified_purchase=1 WHERE user_id=22 AND course_id=1");
                    review.setRating(3); review.setContent("Updated"); courses.createReview(review);
                    check(jdbc.queryForObject("SELECT COUNT(*) FROM course_reviews",Integer.class) == 1, "Review must update same row");
                    check(jdbc.queryForObject("SELECT is_verified_purchase FROM course_reviews WHERE user_id=22 AND course_id=1",Integer.class) == 1, "Review update must preserve verified purchase");
                    check(jdbc.queryForObject("SELECT rating FROM courses WHERE id=1",BigDecimal.class).compareTo(BigDecimal.valueOf(3)) == 0, "Aggregate rating must refresh");
                    System.out.println("PASS repeat review updates persisted row and aggregate rating");

                    UpdateFeedbackRequest update = new UpdateFeedbackRequest(); update.setStatus(2);update.setReplyContent("Resolved");
                    feedback.updateFeedback(10L, update);
                    check(feedback.getAdminFeedbackDetail(10L).getHistory().size() == 1, "History must persist");
                    feedback.updateFeedback(10L, update);
                    check(feedback.getAdminFeedbackDetail(10L).getHistory().size() == 1, "No-op must not append history");
                    update.setStatus(1); update.setReplyContent("Reopened"); feedback.updateFeedback(10L, update);
                    check(jdbc.queryForObject("SELECT resolved_at IS NULL FROM user_feedbacks WHERE id=10",Boolean.class), "Resolution timestamp must clear in SQL");
                    check(feedback.getAdminFeedbackDetail(10L).getHistory().size() == 2, "Second change must append history");
                    System.out.println("PASS feedback history, no-op suppression and persisted reopening timestamp");

                    jdbc.execute("CREATE TRIGGER reject_test_history BEFORE INSERT ON feedback_history FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Injected history failure'");
                    update.setStatus(2);update.setReplyContent("Must roll back");
                    denied(() -> feedback.updateFeedback(10L,update), "Injected history failure must propagate");
                    check(jdbc.queryForObject("SELECT status FROM user_feedbacks WHERE id=10",Integer.class) == 1, "Feedback update must roll back with failed history");
                    check("Reopened".equals(jdbc.queryForObject("SELECT reply_content FROM user_feedbacks WHERE id=10",String.class)), "Reply must roll back");
                    System.out.println("PASS Spring transaction rolls back feedback when history insert fails");
                }
                System.out.println("SUCCESS: all database integration scenarios passed");
            } finally {
                if (!db.matches("matu_verify_[0-9a-f]{32}")) throw new IllegalStateException("Unsafe temporary database name");
                ddl.execute("DROP DATABASE `" + db + "`");
                System.out.println("Temporary integration database removed");
            }
        }
    }
}
