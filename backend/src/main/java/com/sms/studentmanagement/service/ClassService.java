package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.AcademicDtos.ClassResponse;
import com.sms.studentmanagement.dto.AcademicDtos.SectionResponse;
import com.sms.studentmanagement.dto.AcademicDtos.SubjectResponse;
import com.sms.studentmanagement.entity.ClassSubject;
import com.sms.studentmanagement.entity.SchoolClass;
import com.sms.studentmanagement.entity.Section;
import com.sms.studentmanagement.entity.Subject;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.ClassSubjectRepository;
import com.sms.studentmanagement.repository.SchoolClassRepository;
import com.sms.studentmanagement.repository.SectionRepository;
import com.sms.studentmanagement.repository.SubjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    // ---------- Class ----------
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses() {
        return classRepository.findAll(Sort.by("displayOrder", "name")).stream()
                .map(ClassResponse::from)
                .toList();
    }

    @Transactional
    public ClassResponse createClass(String name, int displayOrder) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName(name.trim());
        schoolClass.setDisplayOrder(displayOrder);
        schoolClass.setActive(true);
        return ClassResponse.from(classRepository.saveAndFlush(schoolClass));
    }

    @Transactional
    public ClassResponse updateClass(Long id, String name, int displayOrder) {
        SchoolClass schoolClass = findClass(id);
        schoolClass.setName(name.trim());
        schoolClass.setDisplayOrder(displayOrder);
        classRepository.flush();
        return ClassResponse.from(schoolClass);
    }

    @Transactional
    public ClassResponse setClassActive(Long id, boolean active) {
        SchoolClass schoolClass = findClass(id);
        schoolClass.setActive(active);
        classRepository.flush();
        return ClassResponse.from(schoolClass);
    }

    // ---------- Section ----------
    @Transactional(readOnly = true)
    public List<SectionResponse> listSections(Long classId) {
        findClass(classId);
        return sectionRepository.findBySchoolClassIdOrderByName(classId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional
    public SectionResponse createSection(Long classId, String name) {
        SchoolClass schoolClass = findClass(classId);
        if (!schoolClass.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "নিষ্ক্রিয় Class-এ Section যোগ করা যাবে না");
        }
        Section section = new Section();
        section.setSchoolClass(schoolClass);
        section.setName(name.trim());
        section.setActive(true);
        return SectionResponse.from(sectionRepository.saveAndFlush(section));
    }

    @Transactional
    public SectionResponse updateSection(Long sectionId, String name) {
        Section section = findSection(sectionId);
        section.setName(name.trim());
        sectionRepository.flush();
        return SectionResponse.from(section);
    }

    @Transactional
    public SectionResponse setSectionActive(Long sectionId, boolean active) {
        Section section = findSection(sectionId);
        section.setActive(active);
        sectionRepository.flush();
        return SectionResponse.from(section);
    }

    // ---------- Class-Subject ----------
    @Transactional(readOnly = true)
    public List<SubjectResponse> listClassSubjects(Long classId) {
        findClass(classId);
        return classSubjectRepository.findAllWithSubject(classId).stream()
                .map(cs -> SubjectResponse.from(cs.getSubject()))
                .toList();
    }

    @Transactional
    public SubjectResponse assignSubject(Long classId, Long subjectId) {
        SchoolClass schoolClass = findClass(classId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject পাওয়া যায়নি"));
        if (!schoolClass.isActive() || !subject.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "নিষ্ক্রিয় Class বা Subject-এর সাথে যুক্ত করা যাবে না");
        }
        ClassSubject assignment = new ClassSubject();
        assignment.setSchoolClass(schoolClass);
        assignment.setSubject(subject);
        classSubjectRepository.saveAndFlush(assignment);
        return SubjectResponse.from(subject);
    }

    @Transactional
    public void removeSubject(Long classId, Long subjectId) {
        int deleted = classSubjectRepository.deleteAssignment(classId, subjectId);
        if (deleted == 0) {
            throw new NotFoundException("এই Class-এ এই Subject যুক্ত নেই");
        }
    }

    private SchoolClass findClass(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class পাওয়া যায়নি"));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Section পাওয়া যায়নি"));
    }
}