package com.sms.studentmanagement.service;

import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.TeacherRepository;
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
public class TeacherPhotoService {

    private final TeacherRepository teacherRepository;
    private final PhotoStorage storage;
    private final AuditService auditService;

    @Transactional
    public void upload(Long teacherId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ছবি বাছাই করা হয়নি");
        }
        if (file.getSize() > SafeImage.MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ছবি সর্বোচ্চ ২ MB হতে পারে");
        }
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));

        byte[] jpeg;
        try {
            jpeg = SafeImage.toSafeJpeg(file.getBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ছবি পড়া যায়নি");
        }

        String oldName = teacher.getPhotoPath();
        teacher.setPhotoPath(storage.save(jpeg));
        teacherRepository.save(teacher);
        deleteAfterCommit(oldName);

        auditService.log("TEACHER_PHOTO_CHANGED", "TEACHER", teacherId, teacher.getFullName());
    }

    @Transactional(readOnly = true)
    public byte[] read(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));
        if (teacher.getPhotoPath() == null) {
            throw new NotFoundException("এই শিক্ষকের ছবি নেই");
        }
        return storage.read(teacher.getPhotoPath())
                .orElseThrow(() -> new NotFoundException("ছবির ফাইল পাওয়া যায়নি"));
    }

    @Transactional
    public void remove(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("শিক্ষক পাওয়া যায়নি"));
        String oldName = teacher.getPhotoPath();
        if (oldName == null) {
            return;
        }
        teacher.setPhotoPath(null);
        teacherRepository.save(teacher);
        deleteAfterCommit(oldName);

        auditService.log("TEACHER_PHOTO_REMOVED", "TEACHER", teacherId, teacher.getFullName());
    }

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
}
