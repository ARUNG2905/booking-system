package com.undoschool.bookingsystem.repository;

import com.undoschool.bookingsystem.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("SELECT c FROM Course c WHERE c.teacher.id = :teacherId ORDER BY c.createdAt DESC")
    List<Course> findByTeacherId(@Param("teacherId") UUID teacherId);
}
