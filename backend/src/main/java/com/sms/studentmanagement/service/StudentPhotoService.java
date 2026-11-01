package com.sms.studentmanagement.service;

import com.sms.studentmanagement.entity.Student;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.StudentRepository;
import com.sms.studentmanagement.util.SafeImage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentPhotoService {

    private final StudentRepository studentRepository;
    private final PhotoStorage storage;
    private final AuditService auditService;

    @Transactional
    public void upload(Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw bad("ছবি বাছাই করা হয়নি");
        }
        if (file.getSize() > SafeImage.MAX_BYTES) {
            throw bad("ছবি সর্বোচ্চ ২ MB হতে পারে");
        }
        Student student = find(studentId);

        byte[] jpeg;
        try {
            jpeg = SafeImage.toSafeJpeg(file.getBytes());
        } catch (IOException e) {
            throw bad("ছবি পড়া যায়নি");
        }

        String oldName = student.getPhotoPath();
        student.setPhotoPath(storage.save(jpeg));
        studentRepository.save(student);
        deleteAfterCommit(oldName);

        auditService.log("STUDENT_PHOTO_CHANGED", "STUDENT", studentId, student.getStudentCode());
    }

    @Transactional(readOnly = true)
    public byte[] read(Long studentId) {
        Student student = find(studentId);
        if (student.getPhotoPath() == null) {
            throw new NotFoundException("এই শিক্ষার্থীর ছবি নেই");
        }
        return storage.read(student.getPhotoPath())
                .orElseThrow(() -> new NotFoundException("ছবির ফাইল পাওয়া যায়নি"));
    }

    @Transactional
    public void remove(Long studentId) {
        Student student = find(studentId);
        String oldName = student.getPhotoPath();
        if (oldName == null) {
            return;
        }
        student.setPhotoPath(null);
        studentRepository.save(student);
        deleteAfterCommit(oldName);

        auditService.log("STUDENT_PHOTO_REMOVED", "STUDENT", studentId, student.getStudentCode());
    }

    /** ডেটাবেসের কাজ নিশ্চিতভাবে শেষ হলে তবেই পুরনো ফাইল মোছা হয়। */
    private void deleteAfterCommit(String filename) {
        if (filename == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storage.delete(filename);
            }
        });
    }

    private Student find(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("শিক্ষার্থী পাওয়া যায়নি"));
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}