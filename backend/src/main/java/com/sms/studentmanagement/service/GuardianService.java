package com.sms.studentmanagement.service;

import com.sms.studentmanagement.dto.PeopleDtos.ChildLink;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianDetail;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianRequest;
import com.sms.studentmanagement.dto.PeopleDtos.GuardianResponse;
import com.sms.studentmanagement.entity.Guardian;
import com.sms.studentmanagement.exception.ApiException;
import com.sms.studentmanagement.exception.NotFoundException;
import com.sms.studentmanagement.repository.GuardianRepository;
import com.sms.studentmanagement.repository.StudentGuardianRepository;
import com.sms.studentmanagement.util.Changes;
import com.sms.studentmanagement.util.PhoneNumbers;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<GuardianResponse> search(String q) {
        String term = q == null ? "" : q.trim();
        if (term.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "খুঁজতে কমপক্ষে ২ অক্ষর লিখুন");
        }
        return guardianRepository.search(term, PageRequest.of(0, 20)).stream()
                .map(GuardianResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuardianDetail get(Long id) {
        Guardian guardian = find(id);
        List<ChildLink> children = studentGuardianRepository.findAllForGuardian(id).stream()
                .map(link -> new ChildLink(
                        link.getStudent().getId(),
                        link.getStudent().getStudentCode(),
                        link.getStudent().getFullName(),
                        link.getRelation(),
                        link.isPrimaryContact()))
                .toList();
        return new GuardianDetail(GuardianResponse.from(guardian), children);
    }

    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        Guardian guardian = new Guardian();
        guardian.setFullName(request.fullName().trim());
        guardian.setPhone(PhoneNumbers.normalize(request.phone()));
        guardian.setAltPhone(PhoneNumbers.normalize(request.altPhone()));
        guardian.setEmail(blankToNull(request.email()));
        guardian.setAddress(blankToNull(request.address()));
        guardianRepository.saveAndFlush(guardian);

        auditService.log("GUARDIAN_CREATED", "GUARDIAN", guardian.getId(), guardian.getFullName());
        return GuardianResponse.from(guardian);
    }

    @Transactional
    public GuardianResponse update(Long id, GuardianRequest request) {
        Guardian guardian = find(id);
        String fullName = request.fullName().trim();
        String phone = PhoneNumbers.normalize(request.phone());
        String altPhone = PhoneNumbers.normalize(request.altPhone());
        String email = blankToNull(request.email());
        String address = blankToNull(request.address());

        Changes changes = new Changes()
                .track("নাম", guardian.getFullName(), fullName)
                .track("ফোন", guardian.getPhone(), phone)
                .track("বিকল্প ফোন", guardian.getAltPhone(), altPhone)
                .track("ইমেইল", guardian.getEmail(), email)
                .track("ঠিকানা", guardian.getAddress(), address);

        guardian.setFullName(fullName);
        guardian.setPhone(phone);
        guardian.setAltPhone(altPhone);
        guardian.setEmail(email);
        guardian.setAddress(address);
        guardianRepository.flush();

        if (!changes.isEmpty()) {
            auditService.log("GUARDIAN_UPDATED", "GUARDIAN", id, changes.describe());
        }
        return GuardianResponse.from(guardian);
    }

    private Guardian find(Long id) {
        return guardianRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("অভিভাবক পাওয়া যায়নি"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}