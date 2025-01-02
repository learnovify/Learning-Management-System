package com.lsm.mapper;

import com.lsm.model.DTOs.ClassEntityRequestDTO;
import com.lsm.model.DTOs.ClassEntityResponseDTO;
import com.lsm.model.DTOs.TeacherCourseResponseDTO;
import com.lsm.model.entity.Assignment;
import com.lsm.model.entity.ClassEntity;
import com.lsm.model.entity.base.AppUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ClassEntityMapper {

    public ClassEntity toEntity(ClassEntityRequestDTO dto) {
        return ClassEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    public ClassEntityResponseDTO toDTO(ClassEntity entity) {
        List<TeacherCourseResponseDTO> teacherCourses = entity.getTeacherCourses().stream()
                .filter(tc -> tc.getTeacher() != null && tc.getCourse() != null)
                .map(tc -> TeacherCourseResponseDTO.builder()
                        .teacherId(tc.getTeacher().getId())
                        .courseId(tc.getCourse().getId())
                        .courseName(tc.getCourse().getName())
                        .classIdsAndNames(tc.getClasses() != null ?
                                tc.getClasses().stream()
                                        .collect(Collectors.toMap(
                                                ClassEntity::getId,
                                                ClassEntity::getName,
                                                (existing, replacement) -> existing,
                                                HashMap::new
                                        )) :
                                new HashMap<>())
                        .build())
                .collect(Collectors.toList());

        Map<Long, String> studentMappings = entity.getStudents() != null ?
                entity.getStudents().stream()
                        .collect(Collectors.toMap(
                                AppUser::getId,
                                student -> student.getName() + " " + student.getSurname(),
                                (existing, replacement) -> existing,
                                HashMap::new
                        )) :
                new HashMap<>();

        List<Long> assignmentIds = entity.getAssignments() != null ?
                entity.getAssignments().stream()
                        .map(Assignment::getId)
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        return ClassEntityResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .teacherCourses(teacherCourses)
                .studentIdAndNames(studentMappings)
                .assignmentIds(assignmentIds)
                .build();
    }
}