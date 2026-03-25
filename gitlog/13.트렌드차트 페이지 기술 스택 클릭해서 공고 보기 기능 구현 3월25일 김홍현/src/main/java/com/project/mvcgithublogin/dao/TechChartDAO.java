package com.project.mvcgithublogin.dao;

import com.project.mvcgithublogin.dto.ChartCache;
import com.project.mvcgithublogin.dto.JobPostingbyTech;
import com.project.mvcgithublogin.dto.TechChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TechChartDAO {

    @Autowired
    private DataSource dataSource;

    // 캐시 선언
    private Map<Integer, ChartCache> cache = new HashMap<>();

    // 2주 (밀리초)
    private final long EXPIRE_TIME = 1000L * 60 * 60 * 24 * 14;

    public List<TechChart> getTechStats(int categoryId) {

        // 캐시 확인
        if (cache.containsKey(categoryId)) {
            ChartCache cached = cache.get(categoryId);

            long now = System.currentTimeMillis();

            // 유효 시간 체크
            if (now - cached.getTime() < EXPIRE_TIME) {
                System.out.println("Cache : 1");
                return cached.getData();
            } else {
                System.out.println("Cache : 0");
            }
        }

        // DB 조회
        System.out.println("DB : 1");

        List<TechChart> list = new ArrayList<>();

        String sql = "SELECT stack_name, cnt, percent " +
                "FROM tech_cal " +
                "WHERE category_id = ? " +
                "ORDER BY percent DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement psmt = conn.prepareStatement(sql)) {

            psmt.setInt(1, categoryId);
            ResultSet rs = psmt.executeQuery();

            while (rs.next()) {
                TechChart chart = new TechChart();
                chart.setStackname(rs.getString("stack_name"));
                chart.setCnt(rs.getInt("cnt"));
                chart.setPercent(rs.getDouble("percent"));
                list.add(chart);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 캐시에 저장
        cache.put(categoryId, new ChartCache(list));

        return list;
    }

    public List<JobPostingbyTech> getJobsByTech(String stackname) {

        List<JobPostingbyTech> list = new ArrayList<>();

        String sql = "SELECT *\n" +
                "FROM job_posting jp\n" +
                "CROSS JOIN (\n" +
                "    SELECT LEVEL AS lvl FROM dual CONNECT BY LEVEL <= 10\n" +
                ") l\n" +
                "WHERE TRIM(REGEXP_SUBSTR(jp.tech_stack, '[^,]+', 1, l.lvl)) = ?\n" +
                "AND jp.posted_date >= ADD_MONTHS(SYSDATE, -4)"; // SYSDATE, 뒷 부분이 최근 (4)달 이내 데이터 조회 조건

        try (Connection conn = dataSource.getConnection();
             PreparedStatement psmt = conn.prepareStatement(sql)) {

            psmt.setString(1, stackname);
            ResultSet rs = psmt.executeQuery();

            while (rs.next()) {
                JobPostingbyTech job = new JobPostingbyTech();
                job.setCompanyName(rs.getString("company_name"));
                job.setJobPosition(rs.getString("job_position"));
                job.setTechStack(rs.getString("tech_stack"));
                job.setPostingURL(rs.getString("posting_url"));

                job.setSalary(rs.getString("salary"));
                job.setEducation(rs.getString("education"));
                job.setRegion(rs.getString("region"));
                job.setEmploymentType(rs.getString("employment_type"));
                job.setPostedDate(rs.getString("posted_date"));
                job.setDeadline(rs.getString("deadline"));
                list.add(job);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
