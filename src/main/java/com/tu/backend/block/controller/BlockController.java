package com.tu.backend.block.controller;

import com.tu.backend.block.dto.BlockMetaDto;
import com.tu.backend.block.dto.SyncBlocksRequest;
import com.tu.backend.block.dto.UpdateBlockRequest;
import com.tu.backend.block.dto.UpdateBlockContentRequest;
import com.tu.backend.block.dto.UpdateBlockGraphRequest;
import com.tu.backend.block.service.BlockService;
import com.tu.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping
    public ApiResponse<List<BlockMetaDto>> listBlocks() {
        return ApiResponse.success(blockService.listBlocks());
    }

    @PatchMapping("/{id}/content")
    public ApiResponse<Void> updateBlockContent(
        @PathVariable("id") String blockId,
        @Valid @RequestBody UpdateBlockContentRequest request
    ) {
        blockService.updateBlockContent(blockId, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/graph")
    public ApiResponse<Void> updateBlockGraph(
        @PathVariable("id") String blockId,
        @Valid @RequestBody UpdateBlockGraphRequest request
    ) {
        blockService.updateBlockGraph(blockId, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> updateBlock(
        @PathVariable("id") String blockId,
        @Valid @RequestBody UpdateBlockRequest request
    ) {
        blockService.updateBlock(blockId, request);
        return ApiResponse.success();
    }

    @PostMapping("/sync")
    public ApiResponse<Void> syncBlocks(@Valid @RequestBody SyncBlocksRequest request) {
        blockService.syncBlocks(request);
        return ApiResponse.success();
    }
}
