package com.lsm.service;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.lsm.model.DTOs.*;
import com.lsm.model.entity.StudentSubmission;
import com.lsm.model.entity.AssignmentDocument;
import com.lsm.model.entity.ClassEntity;
import com.lsm.model.entity.Course;
import com.lsm.model.entity.enums.AssignmentStatus;
import com.lsm.repository.ClassEntityRepository;
import com.lsm.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import com.lsm.model.entity.Assignment;
import com.lsm.model.entity.base.AppUser;
import com.lsm.model.entity.enums.Role;
import com.lsm.repository.AppUserRepository;
import com.lsm.repository.AssignmentRepository;

import jakarta.transaction.Transactional;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AppUserRepository appUserRepository;
    private final ClassEntityRepository classEntityRepository;
    private final CourseRepository courseRepository;
    private final AssignmentDocumentService assignmentDocumentService;
    private final StudentSubmissionService studentSubmissionService;

    @Value("${assignment.max-title-length:100}")
    private int maxTitleLength;

    @Value("${assignment.min-due-date-days:1}")
    private int minDueDateDays;

    @Autowired
    public AssignmentService(AssignmentRepository assignmentRepository, AppUserRepository appUserRepository, ClassEntityRepository classEntityRepository, CourseRepository courseRepository, AssignmentDocumentService assignmentDocumentService, StudentSubmissionService studentSubmissionService) {
        this.assignmentRepository = assignmentRepository;
        this.appUserRepository = appUserRepository;
        this.classEntityRepository = classEntityRepository;
        this.courseRepository = courseRepository;
        this.assignmentDocumentService = assignmentDocumentService;
        this.studentSubmissionService = studentSubmissionService;
    }

    @Transactional
    public List<AssignmentDTO> getAllAssignments(AppUser currentUser) throws AccessDeniedException {
        if (currentUser.getRole() == Role.ROLE_STUDENT || currentUser.getRole() == Role.ROLE_TEACHER)
            throw new AccessDeniedException("Only admin and coordinator can list all assignments");
        List<Assignment> assignments = assignmentRepository.findAll();
        return assignments.stream()
                .map(assignment -> new AssignmentDTO(assignment, "Retrieved successfully"))
                .collect(Collectors.toList());
    }

    @Transactional
    public Assignment createAssignment(AssignmentRequestDTO dto, Long loggedInUserId)
            throws AccessDeniedException {
        try {
            log.info("Creating assignment with title: {}", dto.getTitle());

            validateAssignmentRequest(dto);

            AppUser teacher = appUserRepository.findById(loggedInUserId)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

            ClassEntity classEntity = classEntityRepository.findClassEntityByName(dto.getClassName())
                    .orElseThrow(() -> new EntityNotFoundException("Class not found"));

            Course course = courseRepository.findCourseByName(dto.getCourseName())
                    .orElseThrow(() -> new EntityNotFoundException("Course not found"));

            validateTeacherAccess(teacher, classEntity);
            // validateUniqueTitle(dto.getTitle(), classEntity); // No existingAssignmentId for creation

            Assignment assignment = createAssignmentEntity(dto, teacher, classEntity, course);

            log.info("Assignment created successfully with ID: {}", assignment.getId());
            return assignmentRepository.save(assignment);

        } catch (Exception e) {
            log.error("Error creating assignment: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public List<AssignmentDTO> getAssignmentsByClass(Long studentId, AppUser loggedInUser)
            throws AccessDeniedException, EntityNotFoundException {
        AppUser user = appUserRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (loggedInUser .getRole() == Role.ROLE_TEACHER) {
            boolean hasAccess = loggedInUser.getTeacherDetails().getClasses().stream()
                    .map(classEntityRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .anyMatch(classEntity -> classEntity.getStudents().contains(user));

            if (!hasAccess) {
                throw new AccessDeniedException("Teachers can only find assignments by their own students");
            }
        }
        else if (loggedInUser.getRole() == Role.ROLE_STUDENT) {
            if(!studentId.equals(loggedInUser.getId()))
                throw new AccessDeniedException("Student can only display their own assignments");
        }

        ClassEntity classEntity = classEntityRepository.findById(user.getStudentDetails().getClassEntity()).orElseThrow(
                    () -> new EntityNotFoundException("Class not found")
        );

        // For teachers, only show their own assignments
        List<Assignment> assignments;
        if (loggedInUser.getRole() == Role.ROLE_TEACHER) {
            assignments = assignmentRepository.findByClassEntityAndAssignedBy(classEntity, loggedInUser);
        } else {
            assignments = assignmentRepository.findByClassEntityOrderByDueDateDesc(classEntity);
        }

        return assignments.stream()
                .map(assignment -> new AssignmentDTO(assignment, ""))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AssignmentDTO> getAssignmentsByTeacher(Long teacherId, AssignmentFilterDTO filter)
            throws AccessDeniedException, EntityNotFoundException {
        // Find teacher
        AppUser  teacher = appUserRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        // Validate that user is actually a teacher
        if (teacher.getRole() != Role.ROLE_TEACHER) {
            throw new IllegalArgumentException("Specified user is not a teacher");
        }

        // Get all assignments created by the teacher
        List<Assignment> assignments = assignmentRepository.findByAssignedByOrderByDueDateDesc(teacher);

        // Apply filters based on AssignmentFilterDTO
        if (filter != null) {
            if (filter.getClassId() != null) {
                assignments = assignments.stream()
                        .filter(assignment -> assignment.getClassEntity().getId().equals(filter.getClassId()))
                        .collect(Collectors.toList());
            }
            if (filter.getCourseId() != null) {
                assignments = assignments.stream()
                        .filter(assignment -> assignment.getCourse().getId().equals(filter.getCourseId()))
                        .collect(Collectors.toList());
            }
            if (filter.getDueDate() != null) {
                assignments = assignments.stream()
                        .filter(assignment -> assignment.getDueDate().isEqual(filter.getDueDate()))
                        .collect(Collectors.toList());
            }
        }

        return assignments.stream()
                .map(assignment -> new AssignmentDTO(assignment, "Retrieved successfully"))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<StudentAssignmentViewDTO> getAssignmentsByStudent(Long studentId)
            throws AccessDeniedException, EntityNotFoundException {
        // Find student
        AppUser student = appUserRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        // Validate that user is actually a student
        if (student.getRole() != Role.ROLE_STUDENT) {
            throw new IllegalArgumentException("Specified user is not a student");
        }

        // Get student's class
        ClassEntity classEntity = classEntityRepository.findById(student.getStudentDetails().getClassEntity())
                .orElseThrow(() -> new EntityNotFoundException("Class not found by id: " + student.getStudentDetails().getClassEntity()));

        // Get all assignments for the student's class
        List<Assignment> assignments = assignmentRepository.findByClassEntityOrderByDueDateDesc(classEntity);

        // Convert to StudentAssignmentViewDTO
        return assignments.stream()
                .map(assignment -> new StudentAssignmentViewDTO(assignment, studentId))
                .collect(Collectors.toList());
    }

    @Transactional
    public Assignment updateAssignment(Long assignmentId, AssignmentRequestDTO dto, Long loggedInUserId)
            throws AccessDeniedException {
        AppUser user = appUserRepository.findById(loggedInUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if(user.getRole() == Role.ROLE_STUDENT) {
            throw new AccessDeniedException("Students can't update assignments");
        }
        try {
            log.info("Updating assignment ID: {} with title: {}", assignmentId, dto.getTitle());

            Assignment existingAssignment = assignmentRepository.findByIdWithSubmissions(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));

            ClassEntity classEntity = classEntityRepository.findClassEntityByName(dto.getClassName())
                    .orElseThrow(() -> new EntityNotFoundException("Class not found"));

            Course course = courseRepository.findCourseByName(dto.getCourseName())
                    .orElseThrow(() -> new EntityNotFoundException("Course not found"));

            // Validate teacher access
            validateTeacherAccess(existingAssignment.getAssignedBy(), classEntity);

            // Update assignment fields
            updateAssignmentFields(existingAssignment, dto, classEntity, course);

            log.info("Assignment updated successfully: {}", assignmentId);
            return assignmentRepository.save(existingAssignment);

        } catch (Exception e) {
            log.error("Error updating assignment {}: {}", assignmentId, e.getMessage());
            throw e;
        }
    }

    private void updateAssignmentFields(Assignment assignment, AssignmentRequestDTO dto,
                                        ClassEntity classEntity, Course course) {
        assignment.setTitle(dto.getTitle().trim());
        assignment.setDescription(dto.getDescription());
        assignment.setDueDate(dto.getDueDate());
        assignment.setClassEntity(classEntity);
        assignment.setCourse(course);
    }

    @Transactional
    public void deleteAssignment(Long assignmentId, Long loggedInUserId)
            throws AccessDeniedException {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));

        AppUser user = appUserRepository.findById(loggedInUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.getTeacherDetails().getClasses().stream()
                .map(classEntityRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(classEntity -> classEntity.getAssignments().remove(assignment));

        // Only teacher who created the assignment or admin can delete it
        if ((user.getRole() != Role.ROLE_ADMIN || user.getRole() != Role.ROLE_COORDINATOR) && !assignment.getAssignedBy().getId().equals(loggedInUserId)) {
            throw new AccessDeniedException("You can only delete your own assignments");
        }

        assignmentRepository.delete(assignment);
    }

    @Transactional
    public Assignment gradeAssignment(Long assignmentId, GradeDTO gradeDTO, AppUser currentUser, Long studentId)
            throws AccessDeniedException {
        Assignment assignment = findById(assignmentId);

        if (currentUser.getRole() == Role.ROLE_STUDENT)
            throw new AccessDeniedException("Student can't grade assignments");

        // Validate that only teachers can grade their own assignments
        if (currentUser.getRole() == Role.ROLE_TEACHER &&
                !assignment.getAssignedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the assigned teacher can grade this assignment");
        }

        // Check if the assignment is past due and if any submission is not submitted
        boolean canGrade = assignment.getStudentSubmissions().stream()
                .noneMatch(studentSubmission -> studentSubmission.getStatus() != AssignmentStatus.SUBMITTED)
        /*  && assignment.getDueDate().isBefore(LocalDate.now()) */;

        if (!canGrade)
            throw new IllegalStateException("Can only grade assignments that have been submitted");

        // Update the submission for the specific student
        assignment.getStudentSubmissions().stream()
                .filter(studentSubmission -> studentSubmission.getStudent().getId().equals(studentId))
                .forEach(studentSubmission -> {
                    studentSubmission.setGrade(gradeDTO.getGrade());
                    studentSubmission.setFeedback(gradeDTO.getFeedback());
                    studentSubmission.setStatus(AssignmentStatus.GRADED);
                });

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Assignment unsubmitAssignment(Long assignmentId, AppUser  currentUser) throws AccessDeniedException {
        Assignment assignment = findById(assignmentId);

        if (assignment.getDueDate().isBefore(LocalDate.now()))
            throw new IllegalStateException("Can only unsubmit assignments that have been due.");

        // Validate that only the student can unsubmit their assignment
        if (currentUser .getRole() != Role.ROLE_STUDENT)
            throw new AccessDeniedException("Only students can unsubmit assignments");

        // Check if the class entity exists
        ClassEntity classEntity = classEntityRepository.findById(currentUser .getStudentDetails().getClassEntity())
                .orElseThrow(() -> new EntityNotFoundException("Class not found"));

        // Verify the assignment belongs to the student
        if (!classEntity.getCourses().contains(assignment.getCourse()))
            throw new AccessDeniedException("You can only unsubmit your own assignments");

        // Find the student's submission
        StudentSubmission studentSubmission = assignment.getStudentSubmissions().stream()
                .filter(submission -> submission.getStudent().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Student didn't submit assignment"));

        // Validate submission status
        if (studentSubmission.getStatus() != AssignmentStatus.SUBMITTED) {
            throw new IllegalStateException("Can only unsubmit assignments that have been submitted.");
        }

        // Validate that assignment is in SUBMITTED status and not yet graded
        if (studentSubmission.getGrade() != null)
            throw new IllegalStateException("Cannot unsubmit graded assignments");

        // Reset to pending status
        studentSubmission.setStatus(AssignmentStatus.PENDING);
        studentSubmission.setDocument(null);

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Assignment submitAssignment(Long assignmentId, SubmitAssignmentDTO submitDTO, AppUser currentUser)
            throws IllegalStateException, IOException {
        Assignment assignment = findById(assignmentId);

        // Check deadline
        if (LocalDate.now().isAfter(assignment.getDueDate()))
            throw new IllegalStateException("Assignment deadline has passed");

        Optional<StudentSubmission> optionalSubmission = assignment.getStudentSubmissions().stream()
                .filter(studentSubmission -> studentSubmission.getStudent().getId().equals(currentUser .getId()))
                .findFirst();

        if (optionalSubmission.isPresent()) {
            StudentSubmission theStudentSubmission = optionalSubmission.get();

            if (theStudentSubmission.getStatus() == AssignmentStatus.SUBMITTED)
                throw new IllegalStateException("You have already submitted the assignment");
            else if (theStudentSubmission.getGrade() != null)
                throw new IllegalStateException("The assignment has already been graded");
            else if (theStudentSubmission.getStatus() == AssignmentStatus.GRADED)
                throw new IllegalStateException("The assignment has already been graded");
            else {
                // Delete the old file
                Files.deleteIfExists(Paths.get(theStudentSubmission.getDocument().getFilePath()));
                theStudentSubmission.setDocument(null);
                assignmentRepository.save(assignment);
            }
        }

        Optional<ClassEntity> classEntityOpt = classEntityRepository.findById(currentUser.getStudentDetails().getClassEntity());
        if (classEntityOpt.isEmpty())
            throw new EntityNotFoundException("Class not found");
        ClassEntity classEntity = classEntityOpt.get();

        // Verify the assignment belongs to the student
        if (!classEntity.getCourses().contains(assignment.getCourse())) {
            throw new AccessDeniedException("You can only submit your own assignments");
        }

        // Upload document
        StudentSubmission studentSubmission = studentSubmissionService.submitAssignment(
                assignmentId,
                submitDTO,
                currentUser
        );

        return assignmentRepository.save(assignment);
    }

    public Assignment findById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found"));
    }

    @Cacheable(value = "assignments", key = "#courseId")
    public List<AssignmentDTO> getAssignmentsByCourse(Long courseId, AppUser loggedInUser)
            throws AccessDeniedException {
        AppUser user = appUserRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Assignment> assignments;
        if (user.getRole() == Role.ROLE_TEACHER) {
            assignments = assignmentRepository.findByCourseIdAndAssignedBy(courseId, loggedInUser);
        } else {
            assignments = assignmentRepository.findByCourseIdOrderByDueDateDesc(courseId);
        }

        return assignments.stream()
                .map(assignment -> new AssignmentDTO(assignment, ""))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Assignment> createBatchAssignments(List<AssignmentRequestDTO> dtos, Long loggedInUserId)
            throws AccessDeniedException {
        return dtos.stream()
                .map(dto -> {
                    try {
                        return createAssignment(dto, loggedInUserId);
                    } catch (AccessDeniedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void validateAssignmentRequest(AssignmentRequestDTO dto) {
        if (dto.getTitle().length() > maxTitleLength) {
            throw new IllegalArgumentException("Title exceeds maximum length");
        }

        if (dto.getDueDate().isBefore(LocalDate.now().plusDays(minDueDateDays))) {
            throw new IllegalArgumentException("Due date must be at least " + minDueDateDays + " days in the future");
        }
    }

    private void validateTeacherAccess(AppUser teacher, ClassEntity classEntity)
            throws AccessDeniedException {
        if (teacher.getRole() == Role.ROLE_STUDENT) {
            throw new AccessDeniedException("Only teachers, admins, coordinators can create assignments");
        }

        if (teacher.getRole() == Role.ROLE_TEACHER &&
                !teacher.getTeacherDetails().getClasses().contains(classEntity.getId())) {
            throw new AccessDeniedException("Teachers can create assignments only for their assigned classes");
        }
    }

    private Assignment createAssignmentEntity(
            AssignmentRequestDTO dto,
            AppUser teacher,
            ClassEntity classEntity,
            Course course) {
        Assignment assignment = new Assignment();
        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setDueDate(dto.getDueDate());
        assignment.setAssignedBy(teacher);
        if(dto.getDocument() != null)
            assignment.setTeacherDocument(dto.getDocument().DTOtoDocument(assignmentRepository, appUserRepository));
        assignment.setClassEntity(classEntity);
        assignment.setCourse(course);
        assignment.setDate(LocalDate.now());

        return assignment;
    }

    /*
    private void validateUniqueTitle(String title, ClassEntity classEntity, Long... existingAssignmentId) {
        log.debug("Validating title uniqueness: {} for class: {}", title, classEntity.getName());

        // Normalize the title for comparison (trim whitespace and convert to lowercase)
        String normalizedTitle = title.trim().toLowerCase();

        // Check if any assignment with the same normalized title exists in the class
        Optional<Assignment> existingAssignment = assignmentRepository
                .findByTitleIgnoreCaseAndClassEntity(normalizedTitle, classEntity);

        if (existingAssignment.isPresent()) {
            // For update scenario, check if the found assignment is the same as the one being updated
            if (existingAssignmentId != null && existingAssignmentId.length > 0) {
                if (!existingAssignment.get().getId().equals(existingAssignmentId[0])) {
                    log.warn("Duplicate title found: {} for class: {}", title, classEntity.getName());
                    throw new IllegalArgumentException(
                            String.format("An assignment with title '%s' already exists in class %s",
                                    title, classEntity.getName())
                    );
                }
            } else {
                // For create scenario, any existing assignment with same title is a conflict
                log.warn("Attempted to create assignment with duplicate title: {} in class: {}",
                        title, classEntity.getName());
                throw new IllegalArgumentException(
                        String.format("An assignment with title '%s' already exists in class %s",
                                title, classEntity.getName())
                );
            }
        }
        log.debug("Title validation passed for: {} in class: {}", title, classEntity.getName());
    }
     */
}