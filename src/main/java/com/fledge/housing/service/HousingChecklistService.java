package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.checklist.HousingChecklistTemplates;
import com.fledge.housing.domain.ChecklistTemplateType;
import com.fledge.housing.domain.HousingChecklist;
import com.fledge.housing.domain.HousingChecklistItem;
import com.fledge.housing.dto.CreateHousingChecklistItemRequest;
import com.fledge.housing.dto.CreateHousingChecklistRequest;
import com.fledge.housing.dto.HousingChecklistResponse;
import com.fledge.housing.dto.UpdateHousingChecklistItemRequest;
import com.fledge.housing.repository.HousingChecklistItemRepository;
import com.fledge.housing.repository.HousingChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HousingChecklistService {

    private final HousingChecklistRepository checklistRepository;
    private final HousingChecklistItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<HousingChecklistResponse> list(Long memberId) {
        return checklistRepository.findByMemberIdOrderByCreatedAtAsc(memberId).stream()
                .map(HousingChecklistResponse::from)
                .toList();
    }

    @Transactional
    public HousingChecklistResponse create(Long memberId, CreateHousingChecklistRequest request) {
        ChecklistTemplateType type = request.templateType();
        if (checklistRepository.existsByMemberIdAndTemplateType(memberId, type)) {
            throw new ApiException(ErrorCode.HOUSING_CHECKLIST_ALREADY_EXISTS);
        }

        HousingChecklist checklist = new HousingChecklist(memberId, type);
        int order = 0;
        for (HousingChecklistTemplates.SeedItem seed : HousingChecklistTemplates.itemsOf(type)) {
            checklist.addItem(new HousingChecklistItem(seed.content(), seed.memo(), order++));
        }

        try {
            checklistRepository.save(checklist);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.HOUSING_CHECKLIST_ALREADY_EXISTS);
        }
        return HousingChecklistResponse.from(checklist);
    }

    @Transactional
    public void delete(Long memberId, Long checklistId) {
        HousingChecklist checklist = requireChecklist(memberId, checklistId);
        checklistRepository.delete(checklist);
    }

    @Transactional
    public HousingChecklistResponse.HousingChecklistItemResponse addItem(
            Long memberId, Long checklistId, CreateHousingChecklistItemRequest request) {
        HousingChecklist checklist = requireChecklist(memberId, checklistId);
        int nextOrder = itemRepository.findMaxSortOrder(checklistId) + 1;
        HousingChecklistItem item = new HousingChecklistItem(request.content(), request.memo(), nextOrder);
        if (request.dueDate() != null) {
            item.applyPatch(null, true, request.dueDate(), false, null, null, null);
        }
        checklist.addItem(item);
        // cascade 만으로는 IDENTITY id 가 응답 직전에 안 붙는 경우가 있어 명시적으로 flush 한다
        HousingChecklistItem saved = itemRepository.saveAndFlush(item);
        return HousingChecklistResponse.HousingChecklistItemResponse.from(saved);
    }

    @Transactional
    public HousingChecklistResponse.HousingChecklistItemResponse updateItem(
            Long memberId, Long checklistId, Long itemId, UpdateHousingChecklistItemRequest request) {
        HousingChecklist checklist = requireChecklist(memberId, checklistId);
        HousingChecklistItem item = requireItem(checklist, itemId);

        if (request.content() != null && request.content().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "항목 내용을 비울 수 없습니다");
        }

        item.applyPatch(
                request.content(),
                request.dueDatePresent(),
                request.dueDateValue(),
                request.memoPresent(),
                request.memoValue(),
                request.done(),
                request.sortOrder()
        );
        return HousingChecklistResponse.HousingChecklistItemResponse.from(item);
    }

    @Transactional
    public void deleteItem(Long memberId, Long checklistId, Long itemId) {
        HousingChecklist checklist = requireChecklist(memberId, checklistId);
        checklist.removeItem(requireItem(checklist, itemId));
    }

    private static HousingChecklistItem requireItem(HousingChecklist checklist, Long itemId) {
        return checklist.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.HOUSING_CHECKLIST_ITEM_NOT_FOUND));
    }

    private HousingChecklist requireChecklist(Long memberId, Long checklistId) {
        return checklistRepository.findByIdAndMemberId(checklistId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.HOUSING_CHECKLIST_NOT_FOUND));
    }
}
