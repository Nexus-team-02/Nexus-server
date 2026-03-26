package pingpong.backend.domain.notion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pingpong.backend.domain.notion.dto.common.NotionDateRange;
import pingpong.backend.domain.notion.dto.request.NotionPageUpdateRequest;
import pingpong.backend.domain.notion.dto.response.ChildDatabaseWithPagesResponse;
import pingpong.backend.domain.notion.dto.response.DatabaseWithPagesResponse;
import pingpong.backend.domain.notion.dto.response.PageDetailResponse;
import pingpong.backend.domain.task.Task;
import pingpong.backend.domain.task.repository.TaskRepository;
import pingpong.backend.domain.task.service.TaskSyncService;
import pingpong.backend.global.rag.indexing.dto.IndexJob;
import pingpong.backend.global.rag.indexing.enums.IndexSourceType;
import pingpong.backend.global.rag.indexing.job.IndexJobPublisher;
import pingpong.backend.global.rag.indexing.repository.VectorStoreGateway;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Notion 웹훅 이벤트 발생 시 VectorDB를 동기화합니다.
 *
 * - 페이지 변경 이벤트(page.content_updated 등): 해당 페이지 + (child DB page이면 child DB + task page) + primary database 재인덱싱
 * - page.deleted 이벤트: 해당 페이지 청크 삭제 + primary database 재인덱싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotionWebhookIndexingService {

    private final NotionPageService notionPageService;
    private final NotionDatabaseQueryService notionDatabaseQueryService;
    private final NotionConnectionService notionConnectionService;
    private final IndexJobPublisher indexJobPublisher;
    private final VectorStoreGateway vectorStoreGateway;
    private final TaskSyncService taskSyncService;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    private static final String STATUS_COMPLETED = "완료";

    @Async("indexExecutor")
    public void triggerPageIndexing(Long teamId, String pageId) {
        log.info("WEBHOOK_INDEX: 페이지 재인덱싱 시작 teamId={} pageId={}", teamId, pageId);
        PageDetailResponse pageResponse = null;
        try {
            pageResponse = indexPage(teamId, pageId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: 페이지 인덱싱 실패 teamId={} pageId={}", teamId, pageId, e);
        }
        if (pageResponse != null) {
            try {
                String primaryDbId = resolveCompactPrimaryDatabaseId(teamId);
                String parentDatabaseId = pageResponse.parentDatabaseId();
                if (parentDatabaseId != null && !parentDatabaseId.isBlank()
                        && !parentDatabaseId.equals(primaryDbId)) {
                    indexChildDatabaseAndTaskPage(teamId, parentDatabaseId);
                }
            } catch (Exception e) {
                log.error("WEBHOOK_INDEX: child database 인덱싱 실패 teamId={} pageId={}", teamId, pageId, e);
            }
            try {
                pageResponse = autoSetCompletedDateIfNeeded(teamId, pageId, pageResponse);
            } catch (Exception e) {
                log.error("WEBHOOK_INDEX: 완료일 자동 설정 실패 teamId={} pageId={}", teamId, pageId, e);
            }
            try {
                taskSyncService.upsert(teamId, pageResponse);
            } catch (Exception e) {
                log.error("WEBHOOK_INDEX: Task upsert 실패 teamId={} pageId={}", teamId, pageId, e);
            }
        }
        try {
            indexPrimaryDatabase(teamId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: primary database 재인덱싱 실패 teamId={} pageId={}", teamId, pageId, e);
        }
    }

    @Async("indexExecutor")
    public void triggerPageDeletion(Long teamId, String pageId) {
        log.info("WEBHOOK_INDEX: 페이지 삭제 처리 시작 teamId={} pageId={}", teamId, pageId);
        try {
            vectorStoreGateway.deleteByPageId(teamId, pageId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: 페이지 벡터 삭제 실패 teamId={} pageId={}", teamId, pageId, e);
        }
        try {
            taskSyncService.delete(pageId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: Task 삭제 실패 teamId={} pageId={}", teamId, pageId, e);
        }
        try {
            indexPrimaryDatabase(teamId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: primary database 재인덱싱 실패 teamId={} pageId={}", teamId, pageId, e);
        }
    }

    @Async("indexExecutor")
    public void triggerAfterPageCreate(Long teamId, PageDetailResponse pageResponse) {
        log.info("WEBHOOK_INDEX: 페이지 생성 후 인덱싱 시작 teamId={} pageId={}", teamId, pageResponse.id());
        try {
            String apiPath = "GET /api/v1/teams/" + teamId + "/notion/pages/" + pageResponse.id();
            JsonNode payload = objectMapper.valueToTree(pageResponse);
            indexJobPublisher.publish(new IndexJob(IndexSourceType.NOTION, teamId, apiPath, pageResponse.id(), payload));
            taskSyncService.upsert(teamId, pageResponse);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: 페이지 인덱싱 실패 teamId={} pageId={}", teamId, pageResponse.id(), e);
        }
        try {
            indexPrimaryDatabase(teamId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: primary database 재인덱싱 실패 teamId={}", teamId, e);
        }
    }

    @Async("indexExecutor")
    public void triggerAfterDatabaseCreate(Long teamId, String databaseId, String parentPageId) {
        log.info("WEBHOOK_INDEX: 데이터베이스 생성 후 인덱싱 시작 teamId={} databaseId={}", teamId, databaseId);
        try {
            indexChildDatabaseAndTaskPage(teamId, databaseId);
        } catch (Exception e) {
            log.error("WEBHOOK_INDEX: child database 인덱싱 실패 teamId={} databaseId={}", teamId, databaseId, e);
        }
    }

    private PageDetailResponse indexPage(Long teamId, String pageId) {
        PageDetailResponse pageResponse = notionPageService.getPageBlocks(teamId, pageId);
        String apiPath = "GET /api/v1/teams/" + teamId + "/notion/pages/" + pageId;
        JsonNode payload = objectMapper.valueToTree(pageResponse);
        indexJobPublisher.publish(new IndexJob(IndexSourceType.NOTION, teamId, apiPath, pageId, payload));
        return pageResponse;
    }

    private void indexPrimaryDatabase(Long teamId) {
        DatabaseWithPagesResponse dbResponse = notionDatabaseQueryService.queryPrimaryDatabase(teamId);
        String apiPath = "GET /api/v1/teams/" + teamId + "/notion/databases/primary";
        JsonNode payload = objectMapper.valueToTree(dbResponse);
        indexJobPublisher.publish(new IndexJob(IndexSourceType.NOTION, teamId, apiPath, null, payload));
    }

    private void indexChildDatabaseAndTaskPage(Long teamId, String childDatabaseId) {
        ChildDatabaseWithPagesResponse dbResponse = notionDatabaseQueryService.queryChildDatabase(teamId, childDatabaseId);
        String apiPath = "GET /api/v1/teams/" + teamId + "/notion/pages/" + dbResponse.parentPageId() + "/databases";
        JsonNode payload = objectMapper.valueToTree(dbResponse);
        indexJobPublisher.publish(new IndexJob(IndexSourceType.NOTION, teamId, apiPath, childDatabaseId, payload));
        indexPage(teamId, dbResponse.parentPageId());
    }

    /**
     * 상태가 "완료"이고 completedDate가 미설정(null)인 경우 자동으로 완료일을 설정하고 Notion에 write-back합니다.
     * completedDate가 이미 존재하면 사용자가 수동 설정한 것으로 간주하여 덮어쓰지 않습니다.
     */
    private PageDetailResponse autoSetCompletedDateIfNeeded(Long teamId, String pageId, PageDetailResponse pageResponse) {
        if (!STATUS_COMPLETED.equals(pageResponse.status())) {
            return pageResponse;
        }
        if (pageResponse.completedDate() != null) {
            log.info("WEBHOOK_INDEX: 완료일 이미 존재 — 자동 설정 건너뜀 pageId={}", pageId);
            return pageResponse;
        }

        String start = resolveCompletedDateStart(pageResponse, pageId);
        String end = LocalDate.now().toString();
        log.info("WEBHOOK_INDEX: 완료일 자동 설정 pageId={} start={} end={}", pageId, start, end);

        NotionDateRange completedDate = new NotionDateRange(start, end);
        NotionPageUpdateRequest updateRequest = new NotionPageUpdateRequest(null, null, completedDate, null);
        return notionPageService.updatePage(teamId, pageId, updateRequest);
    }

    /**
     * 완료일 start 값을 결정합니다.
     * 우선순위: 계획일(date).start → 기존 Task의 createdAt → 오늘 날짜
     */
    private String resolveCompletedDateStart(PageDetailResponse pageResponse, String pageId) {
        if (pageResponse.date() != null && pageResponse.date().start() != null) {
            return pageResponse.date().start();
        }
        Optional<Task> existingTask = taskRepository.findById(pageId);
        if (existingTask.isPresent() && existingTask.get().getCreatedAt() != null) {
            return existingTask.get().getCreatedAt().toString();
        }
        return LocalDate.now().toString();
    }

    private String resolveCompactPrimaryDatabaseId(Long teamId) {
        return notionConnectionService.resolveConnectedDatabaseId(teamId);
    }
}
