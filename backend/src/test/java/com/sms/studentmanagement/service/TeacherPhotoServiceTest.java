package com.sms.studentmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.studentmanagement.entity.Teacher;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.TeacherRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class TeacherPhotoServiceTest {

    @Mock
    TeacherRepository teacherRepository;

    @Mock
    PhotoStorage photoStorage;

    @Mock
    AuditService auditService;

    TeacherPhotoService service;

    @BeforeEach
    void setUp() {
        service = new TeacherPhotoService(teacherRepository, photoStorage, auditService);
    }

    @Test
    void upload_emptyFile_throwsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        ApiException ex = assertThrows(ApiException.class, () -> service.upload(1L, emptyFile));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void upload_teacherNotFound_throwsNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.upload(999L, file));
    }

    @Test
    void read_teacherWithoutPhoto_throwsNotFound() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setPhotoPath(null);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        assertThrows(NotFoundException.class, () -> service.read(1L));
    }

    @Test
    void read_success() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setPhotoPath("abc-123.jpg");

        byte[] photoData = new byte[] {10, 20, 30};
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(photoStorage.read("abc-123.jpg")).thenReturn(Optional.of(photoData));

        byte[] result = service.read(1L);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void remove_success() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setFullName("Rahim Sir");
        teacher.setPhotoPath("old-photo.jpg");

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        service.remove(1L);

        assertNull(teacher.getPhotoPath());
        verify(teacherRepository).save(teacher);
        verify(auditService).log("TEACHER_PHOTO_REMOVED", "TEACHER", 1L, "Rahim Sir");
    }
}
