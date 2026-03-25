package com.project.mvcgithublogin.repository;

import com.project.mvcgithublogin.domain.JobPosting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookmarkRepository {

    int insertBookmark(@Param("userId") Long userId, @Param("postingId") Long postingId);

    int deleteBookmark(@Param("userId") Long userId, @Param("postingId") Long postingId);

    int existsBookmark(@Param("userId") Long userId, @Param("postingId") Long postingId);

    List<Long> findBookmarkedPostingIds(@Param("userId") Long userId);

    List<JobPosting> findBookmarkedJobs(@Param("userId") Long userId);
}