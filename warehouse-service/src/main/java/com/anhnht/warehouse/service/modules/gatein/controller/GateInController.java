package com.anhnht.warehouse.service.modules.gatein.controller;

import com.anhnht.warehouse.service.common.dto.response.ApiResponse;
import com.anhnht.warehouse.service.common.dto.response.PageResponse;
import com.anhnht.warehouse.service.common.util.PageableUtils;
import com.anhnht.warehouse.service.common.util.SecurityUtils;
import com.anhnht.warehouse.service.modules.gatein.dto.request.ContainerPositionRequest;
import com.anhnht.warehouse.service.modules.gatein.dto.request.GateInRequest;
import com.anhnht.warehouse.service.modules.gatein.dto.response.ContainerPositionResponse;
import com.anhnht.warehouse.service.modules.gatein.dto.response.GateInReceiptResponse;
import com.anhnht.warehouse.service.modules.gatein.dto.response.YardStorageResponse;
import com.anhnht.warehouse.service.modules.gatein.mapper.GateInMapper;
import com.anhnht.warehouse.service.modules.gatein.service.GateInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class GateInController {

    private final GateInService gateInService;
    private final GateInMapper  gateInMapper;

    // ============================================================
    // Gate-In receipts
    // ============================================================

    @PostMapping("/gate-in")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<GateInReceiptResponse>> processGateIn(
            @Valid @RequestBody GateInRequest request) {
        Integer operatorId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(ApiResponse.created(
                gateInMapper.toGateInResponse(gateInService.processGateIn(operatorId, request))));
    }

    @GetMapping("/gate-in")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<PageResponse<GateInReceiptResponse>>> getGateInReceipts(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "gateInTime") String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        Pageable pageable = PageableUtils.of(page, size, sortBy, direction);
        org.springframework.data.domain.Page<GateInReceiptResponse> pageResult = gateInService.findAll(pageable)
                .map(gateInMapper::toGateInResponse);

        pageResult.getContent().forEach(g -> {
            // cargoTypeName & containerTypeName already set by mapper (eager-loaded via @EntityGraph)

            // blockName, rowNo, bayNo, tier, zoneName — from ContainerPosition
            try {
                var pos = gateInService.getPosition(g.getContainerId());
                if (pos != null && pos.getSlot() != null) {
                    var slot = pos.getSlot();
                    g.setRowNo(slot.getRowNo());
                    g.setBayNo(slot.getBayNo());
                    g.setTier(pos.getTier());
                    if (slot.getBlock() != null) {
                        g.setBlockName(slot.getBlock().getBlockName());
                        if (slot.getBlock().getZone() != null) {
                            g.setZoneName(slot.getBlock().getZone().getZoneName());
                        }
                    }
                }
            } catch (Exception ignored) {}

            // yardName — from most recent YardStorage
            try {
                var storageList = gateInService.getStorageHistory(g.getContainerId());
                if (storageList != null && !storageList.isEmpty()) {
                    var storage = storageList.get(0);
                    if (storage.getYard() != null) {
                        g.setYardName(storage.getYard().getYardName());
                    }
                }
            } catch (Exception ignored) {}
        });

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(pageResult)));
    }

    @GetMapping("/gate-in/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<GateInReceiptResponse>> getGateInReceipt(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                gateInMapper.toGateInResponse(gateInService.findById(id))));
    }

    // ============================================================
    // Container position (manual assignment)
    // ============================================================

    @PostMapping("/containers/{containerId}/position")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<ContainerPositionResponse>> assignPosition(
            @PathVariable String containerId,
            @Valid @RequestBody ContainerPositionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                gateInMapper.toPositionResponse(gateInService.assignPosition(containerId, request))));
    }

    @GetMapping("/containers/{containerId}/position")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<ContainerPositionResponse>> getPosition(
            @PathVariable String containerId) {
        return ResponseEntity.ok(ApiResponse.success(
                gateInMapper.toPositionResponse(gateInService.getPosition(containerId))));
    }

    // ============================================================
    // Yard storage history
    // ============================================================

    @GetMapping("/containers/{containerId}/storage")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<List<YardStorageResponse>>> getStorageHistory(
            @PathVariable String containerId) {
        return ResponseEntity.ok(ApiResponse.success(
                gateInMapper.toYardStorageResponses(gateInService.getStorageHistory(containerId))));
    }
}
